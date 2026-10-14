package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.inputfields.textarea.diagnostic.DiagnosticSeverity;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnostic;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticAction;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticProvider;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class TextAreaDiagnostics {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "SmartTextArea-diagnostics");
        thread.setDaemon(true);
        return thread;
    });

    private final SmartTextArea area;
    private final List<TextDiagnosticProvider> providers = new CopyOnWriteArrayList<>();
    private final List<TextAreaDiagnosticEntry> entries = new ArrayList<>();
    private final Map<DiagnosticSeverity, TextAreaDiagnosticPainter> painters = new EnumMap<>(DiagnosticSeverity.class);
    private final Timer timer;
    private long version;
    private Future<?> running;

    TextAreaDiagnostics(SmartTextArea area) {
        this.area = area;
        this.timer = new Timer(400, e -> run());
        this.timer.setRepeats(false);
        for (DiagnosticSeverity severity : DiagnosticSeverity.values()) {
            painters.put(severity, new TextAreaDiagnosticPainter(severity));
        }
    }

    void addProvider(TextDiagnosticProvider provider) {
        if (provider == null || providers.contains(provider)) return;
        providers.add(provider);
        schedule();
    }

    void removeProvider(TextDiagnosticProvider provider) {
        if (!providers.remove(provider)) return;
        if (providers.isEmpty()) {
            cancel();
            replace(false, List.of());
        } else {
            schedule();
        }
    }

    List<TextDiagnosticProvider> providers() {
        return List.copyOf(providers);
    }

    void setDelay(int delay) {
        timer.setInitialDelay(Math.max(0, delay));
        timer.setDelay(Math.max(0, delay));
    }

    int getDelay() {
        return timer.getInitialDelay();
    }

    void setManual(List<TextDiagnostic> diagnostics) {
        replace(true, diagnostics == null ? List.of() : diagnostics);
    }

    void clear() {
        cancel();
        version++;
        removeAll(entries);
        entries.clear();
        changed();
    }

    void documentChanged() {
        version++;
        if (!providers.isEmpty()) timer.restart();
    }

    void documentReplaced() {
        cancel();
        version++;
        removeAll(entries);
        entries.clear();
        changed();
        if (!providers.isEmpty()) timer.restart();
    }

    void runNow() {
        timer.stop();
        run();
    }

    void highlighterChanged() {
        Highlighter highlighter = area.getHighlighter();
        List<TextAreaDiagnosticEntry> rebuilt = new ArrayList<>(entries.size());
        for (TextAreaDiagnosticEntry entry : entries) {
            rebuilt.add(entry.withTag(addHighlight(highlighter, entry)));
        }
        entries.clear();
        entries.addAll(rebuilt);
    }

    Future<?> pending() {
        return running;
    }

    List<TextDiagnostic> current() {
        List<TextDiagnostic> result = new ArrayList<>(entries.size());
        for (TextAreaDiagnosticEntry entry : sortedEntries()) {
            TextDiagnostic live = entry.live();
            if (live != null) result.add(live);
        }
        return result;
    }

    List<TextAreaDiagnosticEntry> entriesAt(int offset) {
        List<TextAreaDiagnosticEntry> result = new ArrayList<>();
        for (TextAreaDiagnosticEntry entry : sortedEntries()) {
            TextDiagnostic live = entry.live();
            if (live != null && live.contains(offset)) result.add(entry);
        }
        result.sort(Comparator.comparingInt(e -> e.diagnostic().severity().ordinal()));
        return result;
    }

    List<TextDiagnostic> at(Point point) {
        int offset = area.viewToModel2D(point);
        List<TextDiagnostic> result = new ArrayList<>();
        if (offset < 0) return result;
        for (TextAreaDiagnosticEntry entry : entriesAt(offset)) {
            TextDiagnostic live = entry.live();
            if (live != null && covers(live, point)) result.add(live);
        }
        return result;
    }

    Map<Integer, DiagnosticSeverity> severityByLine() {
        Map<Integer, DiagnosticSeverity> lines = new HashMap<>();
        for (TextAreaDiagnosticEntry entry : entries) {
            TextDiagnostic live = entry.live();
            if (live == null) continue;
            int line = area.getLineOfOffsetSafe(live.start());
            DiagnosticSeverity previous = lines.get(line);
            if (live.severity().isMoreSevereThan(previous)) lines.put(line, live.severity());
        }
        return lines;
    }

    void apply(TextAreaDiagnosticEntry entry, TextDiagnosticAction action) {
        TextDiagnostic live = entry.live();
        if (live == null || action == null) return;
        if (entries.remove(entry)) {
            removeAll(List.of(entry));
            changed();
        }
        action.apply(area, live);
    }

    void select(boolean forward) {
        List<TextDiagnostic> all = current();
        if (all.isEmpty()) return;
        int anchor = area.getSelectionStart();
        boolean emptySelection = area.getSelectionStart() == area.getSelectionEnd();
        TextDiagnostic target = null;
        if (forward) {
            for (TextDiagnostic diagnostic : all) {
                if (diagnostic.start() > anchor || (diagnostic.start() == anchor && emptySelection)) {
                    target = diagnostic;
                    break;
                }
            }
            if (target == null) target = all.getFirst();
        } else {
            for (int i = all.size() - 1; i >= 0 && target == null; i--) {
                if (all.get(i).start() < anchor) target = all.get(i);
            }
            if (target == null) target = all.getLast();
        }
        area.setCaretPosition(target.start());
        area.moveCaretPosition(target.end());
    }

    private void schedule() {
        version++;
        timer.restart();
    }

    private void run() {
        if (providers.isEmpty()) return;
        cancel();
        long requested = version;
        String text = area.getText();
        List<TextDiagnosticProvider> snapshot = List.copyOf(providers);
        running = EXECUTOR.submit(() -> {
            List<TextDiagnostic> found = new ArrayList<>();
            for (TextDiagnosticProvider provider : snapshot) {
                if (Thread.currentThread().isInterrupted()) return;
                try {
                    List<TextDiagnostic> result = provider.diagnose(text);
                    if (result != null) found.addAll(result);
                } catch (Throwable failure) {
                    failure.printStackTrace();
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (requested == version) replace(false, found);
            });
        });
    }

    private void cancel() {
        timer.stop();
        if (running != null) {
            running.cancel(true);
            running = null;
        }
    }

    private void replace(boolean manual, List<TextDiagnostic> diagnostics) {
        List<TextAreaDiagnosticEntry> stale = entries.stream().filter(e -> e.manual() == manual).toList();
        removeAll(stale);
        entries.removeAll(stale);
        Document document = area.getDocument();
        Highlighter highlighter = area.getHighlighter();
        int length = document.getLength();
        for (TextDiagnostic diagnostic : diagnostics) {
            if (diagnostic == null) continue;
            int start = Math.min(diagnostic.start(), length);
            int end = Math.min(diagnostic.end(), length);
            if (end == start) {
                if (end < length) end++;
                else if (start > 0) start--;
                else continue;
            }
            try {
                Position startPosition = document.createPosition(start);
                Position endPosition = document.createPosition(end);
                TextAreaDiagnosticEntry entry = new TextAreaDiagnosticEntry(diagnostic, startPosition, endPosition, null, manual);
                entries.add(entry.withTag(addHighlight(highlighter, entry)));
            } catch (BadLocationException ignored) {
            }
        }
        changed();
    }

    private Object addHighlight(Highlighter highlighter, TextAreaDiagnosticEntry entry) {
        if (highlighter == null) return null;
        TextDiagnostic live = entry.live();
        if (live == null) return null;
        try {
            return highlighter.addHighlight(live.start(), live.end(), painters.get(live.severity()));
        } catch (BadLocationException ex) {
            return null;
        }
    }

    private void removeAll(List<TextAreaDiagnosticEntry> removed) {
        Highlighter highlighter = area.getHighlighter();
        if (highlighter == null) return;
        for (TextAreaDiagnosticEntry entry : removed) {
            if (entry.tag() != null) highlighter.removeHighlight(entry.tag());
        }
    }

    private List<TextAreaDiagnosticEntry> sortedEntries() {
        List<TextAreaDiagnosticEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((TextAreaDiagnosticEntry e) -> e.start().getOffset())
                .thenComparingInt(e -> e.end().getOffset()));
        return sorted;
    }

    private boolean covers(TextDiagnostic diagnostic, Point point) {
        try {
            Rectangle2D first = area.modelToView2D(diagnostic.start());
            Rectangle2D last = area.modelToView2D(diagnostic.end());
            if (first == null || last == null) return false;
            if (point.y < first.getY() || point.y > last.getMaxY()) return false;
            if (first.getY() == last.getY()) return point.x >= first.getX() && point.x <= last.getX();
            if (point.y < first.getMaxY()) return point.x >= first.getX();
            if (point.y >= last.getY()) return point.x <= last.getX();
            return true;
        } catch (BadLocationException ex) {
            return false;
        }
    }

    private void changed() {
        area.diagnosticsChanged();
    }
}
