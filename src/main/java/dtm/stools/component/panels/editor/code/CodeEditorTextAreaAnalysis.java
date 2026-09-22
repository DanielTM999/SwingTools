package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.DocumentSymbol;
import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.component.panels.editor.code.ghost.GhostTextProvider;
import dtm.stools.component.panels.editor.code.codelens.CodeLens;
import dtm.stools.component.panels.editor.code.codelens.CodeLensContext;
import dtm.stools.component.panels.editor.code.codelens.CodeLensProvider;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsChange;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsContext;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlight;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightContext;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightPalette;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightProvider;
import dtm.stools.component.panels.editor.code.format.CodeFormatter;
import dtm.stools.component.panels.editor.code.format.FormatContext;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.inlay.InlayHintContext;
import dtm.stools.component.panels.editor.code.inlay.InlayHintProvider;
import dtm.stools.component.panels.editor.code.listeners.DiagnosticsChangeListener;
import dtm.stools.component.panels.editor.code.listeners.SearchRequestListener;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.search.SearchMatch;
import dtm.stools.component.panels.editor.code.search.SearchOptions;
import dtm.stools.component.panels.editor.code.search.SearchPanel;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class CodeEditorTextAreaAnalysis extends CodeEditorTextAreaCompletion {

    protected CodeEditorTextAreaAnalysis(TextBuffer buffer) {
        super(buffer);
    }

    protected boolean isSyntaxHighlightStale() {
        return syntaxHighlightEnabled
                && tokenizerProvider != null
                && !buffer.getText().equals(lastHighlightText);
    }

    protected void refreshSearchOnTextChange() {
        if (searchQuery == null || searchQuery.isEmpty()) return;
        int previousIndex = searchCurrentIndex;
        int anchorOffset = -1;
        if (previousIndex >= 0 && previousIndex < searchMatches.size()) {
            anchorOffset = searchMatches.get(previousIndex).startOffset();
        }
        searchMatches.clear();
        searchMatches.addAll(searchEngine.findAll(buffer, searchQuery, searchOptions));
        if (searchMatches.isEmpty()) {
            searchCurrentIndex = -1;
        } else {
            int from = anchorOffset >= 0 ? anchorOffset : caretOffset();
            SearchMatch next = searchEngine.findNext(searchMatches, from, searchOptions.isWrapAround());
            searchCurrentIndex = (next != null) ? searchMatches.indexOf(next) : 0;
        }
        updateSearchPanelCount();
        repaint();
    }















































    protected void scheduleSelectedTextOccurrencesRefresh() {
        selectedTextOccurrencesVersion.incrementAndGet();
        selectedTextOccurrenceOffsets = new int[0];
        cancelFuture(currentSelectedTextOccurrencesTask);
        if (selectedTextOccurrencesTimer != null) {
            selectedTextOccurrencesTimer.stop();
        }
        if (!highlightSelectedTextOccurrences || !hasSelection()) {
            repaint();
            return;
        }
        if (selectedTextOccurrencesTimer == null) {
            selectedTextOccurrencesTimer = new Timer(
                    SELECTED_TEXT_OCCURRENCES_DELAY,
                    e -> submitSelectedTextOccurrencesRefresh());
            selectedTextOccurrencesTimer.setRepeats(false);
        }
        selectedTextOccurrencesTimer.restart();
        repaint();
    }

    protected void submitSelectedTextOccurrencesRefresh() {
        if (!highlightSelectedTextOccurrences || !hasSelection()) return;
        int version = selectedTextOccurrencesVersion.get();
        int bufferVersion = buffer.getVersion();
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        String selectedText = buffer.substring(selectionStart, selectionEnd);
        if (selectedText.isBlank()) return;
        String textSnapshot = buffer.getText();
        currentSelectedTextOccurrencesTask = getSelectedTextOccurrencesExecutor().submit(() -> {
            int[] matches = findSelectedTextOccurrences(
                    textSnapshot,
                    selectedText,
                    selectionStart,
                    selectionEnd);
            if (Thread.currentThread().isInterrupted()) return;
            SwingUtilities.invokeLater(() -> {
                if (version != selectedTextOccurrencesVersion.get()) return;
                if (!highlightSelectedTextOccurrences || bufferVersion != buffer.getVersion()) return;
                if (!hasSelection()
                        || selectionStart != getSelectionStart()
                        || selectionEnd != getSelectionEnd()) {
                    return;
                }
                selectedTextOccurrenceOffsets = matches;
                repaint();
            });
        });
    }

    protected List<int[]> captureFoldedAnchorOffsets() {
        List<int[]> list = new ArrayList<>();
        for (FoldRegion r : foldRegions) {
            if (r.folded()) {
                list.add(new int[]{buffer.offsetOfLine(r.startLine()), r.endLine() - r.startLine()});
            }
        }
        return list;
    }

    protected void restoreFoldedByOffsets(List<int[]> oldAnchors, int editStart, int removedLen, int insertedLen) {
        if (oldAnchors == null || oldAnchors.isEmpty()) return;
        int delta = insertedLen - removedLen;
        for (int[] anchor : oldAnchors) {
            int oldOff = anchor[0];
            int span = anchor[1];
            int newOff;
            if (oldOff < editStart) {
                newOff = oldOff;
            } else if (oldOff >= editStart + removedLen) {
                newOff = oldOff + delta;
            } else {

                continue;
            }
            newOff = Math.max(0, Math.min(newOff, buffer.length()));
            int newLine = buffer.lineOfOffset(newOff);

            for (int i = 0; i < foldRegions.size(); i++) {
                FoldRegion r = foldRegions.get(i);
                if (r.startLine() == newLine && r.endLine() - r.startLine() == span) {
                    if (!r.folded()) foldRegions.set(i, r.withFolded(true));
                    break;
                }
            }
        }
    }

    protected int[] getBlockRange(int line) {
        if (foldingEnabled) {
            for (FoldRegion r : foldRegions) {
                if (r.startLine() == line && r.folded()) {
                    return new int[]{r.startLine(), r.endLine()};
                }
            }
        }
        return new int[]{line, line};
    }

    protected int[] getMoveBlockRange(int line) {
        int[] range = getBlockRange(line);
        if (foldingEnabled) {
            int end = range[1];
            while (end + 1 < buffer.lineCount() && isLineHidden(end + 1)) end++;
            range[1] = end;
        }
        return range;
    }

    protected List<int[]> foldedRegionsWithin(int startLine, int endLine) {
        List<int[]> list = new ArrayList<>();
        if (!foldingEnabled) return list;
        for (FoldRegion r : foldRegions) {
            if (r.folded() && r.startLine() >= startLine && r.endLine() <= endLine) {
                list.add(new int[]{r.startLine() - startLine, r.endLine() - startLine});
            }
        }
        return list;
    }

    protected void refoldRelative(List<int[]> spans, int newStart) {
        for (int[] s : spans) {
            refoldAt(newStart + s[0], newStart + s[1]);
        }
    }

    protected int offsetOfLineEnd(int line) {
        return buffer.offsetOfLine(line) + buffer.lineAt(line).length();
    }

    protected void refoldAt(int startLine, int endLine) {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.startLine() == startLine && r.endLine() == endLine) {
                if (!r.folded()) foldRegions.set(i, r.withFolded(true));
                return;
            }
        }
    }

    public void setFoldingEnabled(boolean enabled) {
        this.foldingEnabled = enabled;
        if (!enabled) {
            foldRegions = new ArrayList<>();
        } else {
            recomputeFoldRegions();
        }
        invalidateGeometry();
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    public void addFoldStateListener(Runnable listener) {
        if (listener != null) foldStateListeners.add(listener);
    }

    public void removeFoldStateListener(Runnable listener) {
        foldStateListeners.remove(listener);
    }

    protected void fireFoldStateChanged() {
        invalidateGeometry();
        for (Runnable r : foldStateListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    public void addFoldRule(FoldRule rule) {
        if (rule == null) return;
        foldRules.add(rule);
        refreshFoldRegionsAfterRuleChange();
    }

    public boolean removeFoldRule(FoldRule rule) {
        if (rule == null) return false;
        boolean removed = foldRules.remove(rule);
        if (removed) refreshFoldRegionsAfterRuleChange();
        return removed;
    }

    public void setFoldRules(Collection<? extends FoldRule> rules) {
        foldRules.clear();
        if (rules != null) {
            for (FoldRule rule : rules) {
                if (rule != null) foldRules.add(rule);
            }
        }
        refreshFoldRegionsAfterRuleChange();
    }

    public void clearFoldRules() {
        foldRules.clear();
        foldRegions = new ArrayList<>();
        invalidateGeometry();
        revalidate();
        repaint();
    }

    public List<FoldRule> getFoldRules() {
        return Collections.unmodifiableList(foldRules);
    }

    public List<FoldRegion> getFoldRegions() {
        return Collections.unmodifiableList(foldRegions);
    }

    protected void refreshFoldRegionsAfterRuleChange() {
        recomputeFoldRegions();
        invalidateGeometry();
        revalidate();
        repaint();
    }

    public void setToggleFoldKeyStroke(KeyStroke ks) {
        this.toggleFoldKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void toggleFoldAtCaret() {
        FoldRegion target = findEnclosingRegion(caretLine);
        if (target != null) toggleFoldRegion(target);
    }

    public void toggleFold(int bufferLine) {
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) {
                toggleFoldRegion(r);
                return;
            }
        }
    }

    public void foldAll() {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (!r.folded()) foldRegions.set(i, r.withFolded(true));
        }
        ensureCaretVisible();
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    public void unfoldAll() {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.folded()) foldRegions.set(i, r.withFolded(false));
        }
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    protected FoldRegion findEnclosingRegion(int line) {
        FoldRegion best = null;
        for (FoldRegion r : foldRegions) {
            if (r.contains(line)) {
                if (best == null || (r.endLine() - r.startLine()) < (best.endLine() - best.startLine())) {
                    best = r;
                }
            }
        }
        return best;
    }

    protected void toggleFoldRegion(FoldRegion target) {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.startLine() == target.startLine() && r.endLine() == target.endLine()) {
                boolean newFolded = !r.folded();
                foldRegions.set(i, r.withFolded(newFolded));
                if (newFolded && caretLine > r.startLine() && caretLine <= r.endLine()) {
                    caretLine = r.startLine();
                    caretCol = Math.min(caretCol, buffer.lineAt(caretLine).length());
                    clearSelection();
                }
                fireFoldStateChanged();
                revalidate();
                repaint();
                return;
            }
        }
    }

    protected void unfoldToRevealCaret() {
        if (!foldingEnabled) return;
        boolean changed = false;
        FoldRegion hiding;
        while ((hiding = findFoldingRegionHiding(caretLine)) != null) {
            boolean unfolded = false;
            for (int i = 0; i < foldRegions.size(); i++) {
                FoldRegion r = foldRegions.get(i);
                if (r.startLine() == hiding.startLine() && r.endLine() == hiding.endLine() && r.folded()) {
                    foldRegions.set(i, r.withFolded(false));
                    unfolded = true;
                    changed = true;
                    break;
                }
            }
            if (!unfolded) break;
        }
        if (changed) fireFoldStateChanged();
    }

    protected void ensureCaretVisible() {
        if (!foldingEnabled) return;
        while (isLineHidden(caretLine)) {
            FoldRegion r = findFoldingRegionHiding(caretLine);
            if (r == null) break;
            caretLine = r.startLine();
        }
        caretCol = Math.min(caretCol, buffer.lineAt(caretLine).length());
    }

    protected FoldRegion findFoldingRegionHiding(int line) {
        for (FoldRegion r : foldRegions) {
            if (r.folded() && line > r.startLine() && line <= r.endLine()) return r;
        }
        return null;
    }

    protected void recomputeFoldRegions() {
        recomputeFoldRegions(true);
    }

    protected void recomputeFoldRegions(boolean preserveFoldedByLine) {
        if (!foldingEnabled || foldRules.isEmpty()) {
            foldRegions = new ArrayList<>();
            return;
        }
        List<FoldRegion> newRegions = new ArrayList<>();
        int lineCount = buffer.lineCount();
        for (FoldRule rule : foldRules) {
            computeRegionsForRule(rule, lineCount, newRegions);
        }
        if (preserveFoldedByLine) {
            for (int i = 0; i < newRegions.size(); i++) {
                FoldRegion nr = newRegions.get(i);
                for (FoldRegion old : foldRegions) {
                    if (old.startLine() == nr.startLine() && old.endLine() == nr.endLine() && old.folded()) {
                        newRegions.set(i, nr.withFolded(true));
                        break;
                    }
                }
            }
        }
        foldRegions = newRegions;
    }

    protected void scheduleFoldRefresh() {
        if (!foldingEnabled) return;
        foldingDebounceTimer = restartDebounce(foldingDebounceTimer, foldingDebounceMs, () -> {
            recomputeFoldRegions(!suppressFoldRestore);
            invalidateGeometry();
            revalidate();
            repaint();
        });
    }

    protected void computeRegionsForRule(FoldRule rule, int lineCount, List<FoldRegion> out) {
        if (rule instanceof FoldRule.Pair(char open, char close)) {
            computePairRegions(String.valueOf(open), String.valueOf(close), lineCount, out);
        } else if (rule instanceof FoldRule.StringPair(String open, String close)) {
            computePairRegions(open, close, lineCount, out);
        } else if (rule instanceof FoldRule.Section s) {
            String prefix = s.markerPrefix();
            if (prefix == null || prefix.isEmpty()) return;
            List<Integer> markers = new ArrayList<>();
            for (int line = 0; line < lineCount; line++) {
                if (buffer.lineAt(line).trim().startsWith(prefix)) markers.add(line);
            }
            for (int i = 0; i < markers.size(); i++) {
                int start = markers.get(i);
                int end = (i + 1 < markers.size()) ? markers.get(i + 1) - 1 : lineCount - 1;
                while (end > start && buffer.lineAt(end).trim().isEmpty()) end--;
                if (end > start) out.add(new FoldRegion(start, end, false));
            }
        } else if (rule instanceof FoldRule.Custom custom) {
            List<FoldRegion> regions = custom.provider().compute(new FoldRule.Context(lineCount, buffer::lineAt));
            if (regions == null) return;
            for (FoldRegion region : regions) {
                if (region == null) continue;
                int start = region.startLine();
                int end = region.endLine();
                if (start >= 0 && start < lineCount && end > start && end < lineCount) {
                    out.add(new FoldRegion(start, end, false));
                }
            }
        }
    }

    protected void computePairRegions(String open, String close, int lineCount, List<FoldRegion> out) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (int line = 0; line < lineCount; line++) {
            String text = buffer.lineAt(line);
            int col = 0;
            while (col < text.length()) {
                int openAt = text.indexOf(open, col);
                int closeAt = text.indexOf(close, col);
                if (openAt < 0 && closeAt < 0) break;
                if (openAt >= 0 && (closeAt < 0 || openAt <= closeAt)) {
                    stack.push(line);
                    col = openAt + open.length();
                } else {
                    if (!stack.isEmpty()) {
                        int sLine = stack.pop();
                        if (line > sLine) out.add(new FoldRegion(sLine, line, false));
                    }
                    col = closeAt + close.length();
                }
            }
        }
    }

    public boolean isLineHidden(int bufferLine) {
        if (!foldingEnabled) return false;
        if (!geometryCacheDirty && cachedHiddenLines != null
                && bufferLine >= 0 && bufferLine < cachedLineCount) {
            return cachedHiddenLines.get(bufferLine);
        }
        for (FoldRegion r : foldRegions) {
            if (r.folded() && bufferLine > r.startLine() && bufferLine <= r.endLine()) return true;
        }
        return false;
    }

    public boolean isFoldAnchor(int bufferLine) {
        if (!foldingEnabled) return false;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine && r.folded()) return true;
        }
        return false;
    }

    protected String[] findFoldSeparatorsForRegion(FoldRegion region) {
        if (region == null || foldRules.isEmpty()) return null;
        String startLineText = buffer.lineAt(region.startLine());
        String endLineText = buffer.lineAt(region.endLine());
        for (FoldRule rule : foldRules) {
            if (rule instanceof FoldRule.Pair(char openChar, char closeChar)) {
                String open = String.valueOf(openChar);
                String close = String.valueOf(closeChar);
                if (startLineText.contains(open) && endLineText.contains(close)) {
                    return new String[]{open, close};
                }
            } else if (rule instanceof FoldRule.StringPair(String open, String close)) {
                if (startLineText.contains(open) && endLineText.contains(close)) {
                    return new String[]{open, close};
                }
            }
        }
        return null;
    }

    protected boolean shouldHideTrailingOpenForFold(int bufferLine) {
        if (!foldPlaceholderWithSeparators || !isFoldAnchor(bufferLine)) return false;
        String[] sep = findFoldSeparatorsForRegion(getFoldRegionStartingAt(bufferLine));
        if (sep == null || sep[0].isEmpty()) return false;
        String text = buffer.lineAt(bufferLine).stripTrailing();
        return text.endsWith(sep[0]);
    }

    public boolean isFoldAnchorLine(int bufferLine) {
        if (!foldingEnabled) return false;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) return true;
        }
        return false;
    }

    public FoldRegion getFoldRegionStartingAt(int bufferLine) {
        if (!foldingEnabled) return null;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) return r;
        }
        return null;
    }

    public void formatDocument() {
        if (readOnly) return;
        if (codeFormatter == null) return;
        int caretOff = caretOffset();
        String sourceText = buffer.getText();
        CodeFormatter formatter = codeFormatter;
        FormatContext ctx = new FormatContext(new TextBuffer(sourceText), 0, sourceText.length(),
                tabSize, useSpacesForTab, FormatContext.Scope.DOCUMENT);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return formatter.format(ctx);
                    } catch (Exception ignored) {
                        return null;
                    }
                }, getProviderExecutor())
                .thenAccept(formatted -> SwingUtilities.invokeLater(() ->
                        applyFormattedDocument(sourceText, caretOff, formatted)));
    }

    protected void applyFormattedDocument(String sourceText, int caretOff, String formatted) {
        if (formatted == null) return;
        formatted = formatted.replace("\r\n", "\n").replace("\r", "\n");
        if (!buffer.getText().equals(sourceText)) return;
        if (formatted.equals(buffer.getText())) return;
        beginCompoundEdit();
        try {
            deleteText(0, buffer.length());
            insertText(0, formatted);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(Math.min(caretOff, buffer.length()));
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void formatSelection() {
        if (readOnly) return;
        if (codeFormatter == null || !hasSelection()) return;
        int start = getSelectionStart();
        int end = getSelectionEnd();
        String sourceText = buffer.getText();
        CodeFormatter formatter = codeFormatter;
        FormatContext ctx = new FormatContext(new TextBuffer(sourceText), start, end,
                tabSize, useSpacesForTab, FormatContext.Scope.SELECTION);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return formatter.format(ctx);
                    } catch (Exception ignored) {
                        return null;
                    }
                }, getProviderExecutor())
                .thenAccept(formatted -> SwingUtilities.invokeLater(() ->
                        applyFormattedSelection(sourceText, start, end, formatted)));
    }

    protected void applyFormattedSelection(String sourceText, int start, int end, String formatted) {
        if (formatted == null) return;
        formatted = formatted.replace("\r\n", "\n").replace("\r", "\n");
        if (!buffer.getText().equals(sourceText)) return;
        beginCompoundEdit();
        try {
            deleteText(start, end);
            insertText(start, formatted);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(start + formatted.length());
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void format(){
        if(hasSelection()){
            formatSelection();
            return;
        }
        formatDocument();
    }

    public void refreshDiagnostics() {
        if (diagnosticsDebounceTimer != null) diagnosticsDebounceTimer.stop();
        refreshDiagnosticsAsync();
    }

    public void addDiagnosticsChangeListener(DiagnosticsChangeListener listener) {
        if (listener != null) diagnosticsChangeListeners.add(listener);
    }

    public void removeDiagnosticsChangeListener(DiagnosticsChangeListener listener) {
        if (listener != null) diagnosticsChangeListeners.remove(listener);
    }

    protected void fireDiagnosticsChanged() {
        if (diagnosticsChangeListeners.isEmpty()) return;
        List<Diagnostic> snapshot = List.copyOf(diagnostics);
        for (DiagnosticsChangeListener listener : diagnosticsChangeListeners) {
            listener.onDiagnosticsChanged(snapshot);
        }
    }

    static void stopDebounce(Timer timer) {
        if (timer != null) timer.stop();
    }

    static Timer restartDebounce(Timer timer, int delayMs, Runnable action) {
        if (delayMs <= 0) {
            action.run();
            return timer;
        }
        Timer target = timer;
        if (target == null) {
            target = new Timer(delayMs, e -> action.run());
            target.setRepeats(false);
        } else {
            for (ActionListener listener : target.getActionListeners()) {
                target.removeActionListener(listener);
            }
            target.addActionListener(e -> action.run());
            target.setInitialDelay(delayMs);
            target.setDelay(delayMs);
        }
        target.restart();
        return target;
    }

    protected void scheduleDiagnosticsRefresh() {
        if (diagnosticsDebounceMs <= 0) {
            refreshDiagnosticsAsync();
            return;
        }
        if (diagnosticsDebounceTimer == null) {
            diagnosticsDebounceTimer = new Timer(diagnosticsDebounceMs, e -> refreshDiagnosticsAsync());
            diagnosticsDebounceTimer.setRepeats(false);
        } else {
            diagnosticsDebounceTimer.setInitialDelay(diagnosticsDebounceMs);
            diagnosticsDebounceTimer.setDelay(diagnosticsDebounceMs);
        }
        diagnosticsDebounceTimer.restart();
    }

    public void refreshDiagnosticsAsync() {
        if (diagnosticsProvider == null) {
            diagnostics.clear();
            lastDiagnostics = null;
            lastDiagnosticsText = null;
            pendingDiagnosticsEdit = null;
            fireDiagnosticsChanged();
            repaint();
            return;
        }

        int version = diagnosticsVersion.incrementAndGet();

        if (currentDiagnosticsTask != null && !currentDiagnosticsTask.isDone()) {
            currentDiagnosticsTask.cancel(true);
        }

        final DiagnosticsProvider provider = diagnosticsProvider;
        final String textSnapshot = buffer.getText();
        final TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        final PendingHighlightEdit edit = pendingDiagnosticsEdit;
        pendingDiagnosticsEdit = null;
        final List<Diagnostic> prevDiagnostics = lastDiagnostics;
        final String prevText = lastDiagnosticsText;

        currentDiagnosticsTask = getDiagnosticsExecutor().submit(() -> {
            try {
                List<Diagnostic> list;
                if (provider.supportsIncremental()
                        && isConsistentEdit(edit, prevText, textSnapshot)
                        && prevDiagnostics != null) {
                    DiagnosticsChange change = new DiagnosticsChange(
                            bufferSnapshot, prevText, textSnapshot,
                            edit.offset(), edit.removedLength(), edit.insertedText(),
                            prevDiagnostics);
                    list = provider.getDiagnostics(change);
                } else {
                    list = provider.getDiagnostics(new DiagnosticsContext(bufferSnapshot));
                }

                if (version != diagnosticsVersion.get()) return;

                final List<Diagnostic> snapshot = list != null
                        ? List.copyOf(list)
                        : List.of();
                lastDiagnostics = snapshot;
                lastDiagnosticsText = textSnapshot;

                SwingUtilities.invokeLater(() -> {
                    if (version != diagnosticsVersion.get()) return;

                    diagnostics.clear();
                    diagnostics.addAll(snapshot);
                    fireDiagnosticsChanged();
                    repaint();
                });

            } catch (Exception ignored) {}
        });
    }

    public void refreshCodeLenses() {
        refreshCodeLensesAsync();
    }

    protected void scheduleCodeLensesRefresh() {
        codeLensesDebounceTimer = restartDebounce(codeLensesDebounceTimer, codeLensesDebounceMs,
                this::refreshCodeLensesAsync);
    }

    public void refreshCodeLensesAsync() {
        stopDebounce(codeLensesDebounceTimer);
        if (codeLensProvider == null) {
            codeLenses.clear();
            revalidate();
            repaint();
            return;
        }

        int version = codeLensVersion.incrementAndGet();

        if (currentCodeLensTask != null && !currentCodeLensTask.isDone()) {
            currentCodeLensTask.cancel(true);
        }

        final CodeLensProvider provider = codeLensProvider;
        final String textSnapshot = buffer.getText();
        final CodeLensContext ctx = new CodeLensContext(new TextBuffer(textSnapshot));

        currentCodeLensTask = getCodeLensExecutor().submit(() -> {
            try {
                List<CodeLens> list = provider.getCodeLenses(ctx);

                if (version != codeLensVersion.get()) return;

                SwingUtilities.invokeLater(() -> {
                    if (version != codeLensVersion.get()) return;

                    codeLenses.clear();
                    if (list != null) codeLenses.addAll(list);
                    invalidateGeometry();
                    revalidate();
                    repaint();
                });

            } catch (Exception failure) {
                CODE_LENS_LOG.log(Level.WARNING, "code lens provider failed", failure);
            }
        });
    }

    public void applySyntaxHighlight() {
        if (!syntaxHighlightEnabled) return;
        if (tokenizerProvider == null || tokenClassifierProvider == null
                || tokenColorProvider == null || tokenRenderProvider == null) {
            return;
        }
        stopDebounce(syntaxHighlightDebounceTimer);
        applySyntaxHighlight(highlightVersion.incrementAndGet());
        scheduleSyntaxHighlightWatchdog();
    }

    public void reapplySyntaxHighlightStyles() {
        if (!syntaxHighlightEnabled) return;
        if (tokenColorProvider == null || tokenRenderProvider == null) {
            return;
        }
        if (!(tokenRenderProvider instanceof PreparedTokenRenderCodeEditorProvider preparedRenderer)) {
            applySyntaxHighlight();
            return;
        }

        final Collection<Token> tokens = lastHighlightTokens;
        final String textSnapshot = lastHighlightText;
        if (tokens == null || textSnapshot == null || !buffer.getText().equals(textSnapshot)) {
            applySyntaxHighlight();
            return;
        }

        final int version = highlightVersion.incrementAndGet();
        final TokenColorProvider colorProvider = tokenColorProvider;
        final TokenRenderSnapshot renderSnapshot = new TokenRenderSnapshot(textSnapshot, defaultStyle);

        getHighlightExecutor().submit(() -> {
            try {
                Collection<StyledRange> ranges =
                        preparedRenderer.prepare(tokens, colorProvider, renderSnapshot);
                final Collection<StyledRange> preparedRanges =
                        ranges == null ? List.of() : List.copyOf(ranges);
                SwingUtilities.invokeLater(() -> {
                    if (version != highlightVersion.get()) return;
                    if (!buffer.getText().equals(textSnapshot)) return;
                    replaceStyledRanges(preparedRanges);
                });
            } catch (Exception failure) {
                CODE_LENS_LOG.log(Level.WARNING, "syntax highlight restyle failed", failure);
            }
        });
    }

    protected void scheduleSyntaxHighlight() {
        final int version = highlightVersion.incrementAndGet();
        syntaxHighlightDebounceTimer = restartDebounce(syntaxHighlightDebounceTimer,
                syntaxHighlightDebounceMs, () -> applySyntaxHighlight(version));
        scheduleSyntaxHighlightWatchdog();
    }

    protected void scheduleSyntaxHighlightWatchdog() {
        int delay = Math.max(400, syntaxHighlightDebounceMs * 4);
        syntaxHighlightWatchdogTimer = restartDebounce(syntaxHighlightWatchdogTimer,
                delay, this::runSyntaxHighlightWatchdog);
    }

    protected void runSyntaxHighlightWatchdog() {
        if (!isSyntaxHighlightStale()) {
            syntaxHighlightRescueAttempts = 0;
            return;
        }
        if (syntaxHighlightDebounceTimer != null && syntaxHighlightDebounceTimer.isRunning()) {
            return;
        }
        if (currentHighlightTask != null && !currentHighlightTask.isDone()) {
            scheduleSyntaxHighlightWatchdog();
            return;
        }
        if (syntaxHighlightRescueAttempts >= MAX_SYNTAX_HIGHLIGHT_RESCUES) {
            return;
        }
        syntaxHighlightRescueAttempts++;
        applySyntaxHighlight();
    }

    private void applySyntaxHighlight(int version) {
        if (!syntaxHighlightEnabled || version != highlightVersion.get()) return;
        if (tokenizerProvider == null || tokenClassifierProvider == null
                || tokenColorProvider == null || tokenRenderProvider == null) {
            return;
        }
        final String textSnapshot = buffer.getText();
        final TokenizerCodeEditorProvider tokenizer = tokenizerProvider;
        final TokenClassifierCodeEditorProvider classifier = tokenClassifierProvider;
        final TokenColorProvider colorProvider = tokenColorProvider;
        final TokenRenderCodeEditorProvider renderer = tokenRenderProvider;

        final Collection<Token> prevTokens = lastHighlightTokens;
        final String prevText = lastHighlightText;
        final TokenizeChange change = coalescedHighlightChange(prevText, textSnapshot, prevTokens);
        final TokenRenderSnapshot renderSnapshot = new TokenRenderSnapshot(textSnapshot, defaultStyle);

        if (currentHighlightTask != null && !currentHighlightTask.isDone()) {
            currentHighlightTask.cancel(true);
        }
        currentHighlightTask = getHighlightExecutor().submit(() -> {
            try {
                Collection<Token> tokens;
                if (tokenizer.supportsIncremental()
                        && change != null) {
                    tokens = tokenizer.tokenize(change, classifier);
                } else {
                    tokens = tokenizer.tokenize(textSnapshot, classifier);
                }
                if (tokens == null) tokens = Collections.emptyList();
                final Collection<Token> snapshot = List.copyOf(tokens);
                final PreparedTokenRenderCodeEditorProvider preparedRenderer =
                        renderer instanceof PreparedTokenRenderCodeEditorProvider prepared
                                ? prepared : null;
                final Collection<StyledRange> preparedRanges;
                if (preparedRenderer == null) {
                    preparedRanges = null;
                } else {
                    Collection<StyledRange> ranges =
                            preparedRenderer.prepare(snapshot, colorProvider, renderSnapshot);
                    preparedRanges = ranges == null ? List.of() : List.copyOf(ranges);
                }
                SwingUtilities.invokeLater(() -> {
                    if (version != highlightVersion.get()) return;
                    if (!buffer.getText().equals(textSnapshot)) return;
                    lastHighlightTokens = snapshot;
                    lastHighlightText = textSnapshot;
                    syntaxHighlightRescueAttempts = 0;
                    if (preparedRenderer != null) {
                        replaceStyledRanges(preparedRanges);
                        preparedRenderer.afterApply((CodeEditorTextArea) this);
                    } else {
                        renderer.render(snapshot, colorProvider, (CodeEditorTextArea) this);
                    }
                });
            } catch (Exception failure) {
                CODE_LENS_LOG.log(Level.WARNING, "syntax highlight provider failed", failure);
            }
        });
    }

    private static TokenizeChange coalescedHighlightChange(
            String oldText, String newText, Collection<Token> previousTokens) {
        if (oldText == null || newText == null || previousTokens == null
                || oldText.equals(newText)) {
            return null;
        }
        int prefix = 0;
        int commonLength = Math.min(oldText.length(), newText.length());
        while (prefix < commonLength && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        int oldRemaining = oldText.length() - prefix;
        int newRemaining = newText.length() - prefix;
        int maxSuffix = Math.min(oldRemaining, newRemaining);
        while (suffix < maxSuffix
                && oldText.charAt(oldText.length() - 1 - suffix)
                == newText.charAt(newText.length() - 1 - suffix)) {
            suffix++;
        }
        int removedLength = oldText.length() - prefix - suffix;
        String insertedText = newText.substring(prefix, newText.length() - suffix);
        return new TokenizeChange(
                oldText, newText, prefix, removedLength, insertedText, previousTokens);
    }

    private static boolean isConsistentEdit(PendingHighlightEdit edit, String prevText, String newText) {
        if (edit == null || prevText == null) return false;
        int offset = edit.offset();
        int removedEnd = offset + edit.removedLength();
        if (offset < 0 || removedEnd > prevText.length()) return false;
        String reconstructed = prevText.substring(0, offset)
                + edit.insertedText()
                + prevText.substring(removedEnd);
        return reconstructed.equals(newText);
    }

    public void addProvider(CodeEditorProvider provider) {
        if (provider == null) return;
        if (provider instanceof TokenizerCodeEditorProvider p) setTokenizerProvider(p);
        if (provider instanceof TokenClassifierCodeEditorProvider p) setTokenClassifierProvider(p);
        if (provider instanceof TokenColorProvider p) setTokenColorProvider(p);
        if (provider instanceof TokenRenderCodeEditorProvider p) setTokenRenderProvider(p);
        if (provider instanceof AutoCompleteProvider p) setAutoCompleteProvider(p);
        if (provider instanceof CodeFormatter p) setCodeFormatter(p);
        if (provider instanceof DiagnosticsProvider p) setDiagnosticsProvider(p);
        if (provider instanceof DefinitionProvider p) setDefinitionProvider(p);
        if (provider instanceof InlayHintProvider p) {
            setInlayHintProvider(p);
            if (inlayHintsEnabled) refreshInlayHints();
        }
        if (provider instanceof CodeLensProvider p) {
            setCodeLensProvider(p);
            if (codeLensesAutoRunEnabled) refreshCodeLensesAsync();
        }
        if (provider instanceof HoverDocumentationProvider p) setHoverDocumentationProvider(p);
        if (provider instanceof SignatureHelpProvider p) setSignatureHelpProvider(p);
        if (provider instanceof DefinitionLocationProvider p) setDefinitionLocationProvider(p);
        if (provider instanceof ReferencesProvider p) setReferencesProvider(p);
        if (provider instanceof DocumentSymbolProvider p) {
            setDocumentSymbolProvider(p);
            refreshDocumentSymbolsAsync();
        }
        if (provider instanceof RenameProvider p) setRenameProvider(p);
        if (provider instanceof CodeActionProvider p) setCodeActionProvider(p);
        if (provider instanceof SelectionRangeProvider p) setSelectionRangeProvider(p);
        if (provider instanceof CommentProvider p) setCommentProvider(p);
        if (provider instanceof ContextMenuProvider p) setContextMenuProvider(p);
        if (provider instanceof BracketMatcher p) setBracketMatcher(p);
        if (provider instanceof WordDetector p) setWordDetector(p);
        if (provider instanceof GhostTextProvider p) setGhostTextProvider(p);
        if (provider instanceof DocumentHighlightProvider p) setDocumentHighlightProvider(p);
    }

    public void setDocumentHighlightProvider(DocumentHighlightProvider provider) {
        if (documentHighlightProvider == provider) return;
        documentHighlightProvider = provider;
        scheduleDocumentHighlightsRefresh();
    }

    public void setDocumentHighlightsEnabled(boolean enabled) {
        if (documentHighlightsEnabled == enabled) return;
        documentHighlightsEnabled = enabled;
        scheduleDocumentHighlightsRefresh();
    }

    public void setDocumentHighlightDebounceMs(int debounceMs) {
        documentHighlightDebounceMs = Math.max(0, debounceMs);
        scheduleDocumentHighlightsRefresh();
    }

    public void setDocumentHighlightPalette(DocumentHighlightPalette palette) {
        documentHighlightPalette = palette == null ? DocumentHighlightPalette.defaults() : palette;
        repaint();
    }

    protected void scheduleDocumentHighlightsRefresh() {
        documentHighlightVersion.incrementAndGet();
        cancelFuture(currentDocumentHighlightTask);
        currentDocumentHighlightTask = null;
        stopDebounce(documentHighlightDebounceTimer);

        if (!documentHighlightsEnabled || documentHighlightProvider == null
                || findWordSpan(caretLine, caretCol) == null) {
            clearDocumentHighlights();
            return;
        }

        documentHighlightDebounceTimer = restartDebounce(
                documentHighlightDebounceTimer,
                documentHighlightDebounceMs,
                this::refreshDocumentHighlights);
    }

    public void refreshDocumentHighlights() {
        stopDebounce(documentHighlightDebounceTimer);
        int version = documentHighlightVersion.incrementAndGet();
        cancelFuture(currentDocumentHighlightTask);
        currentDocumentHighlightTask = null;

        DocumentHighlightProvider provider = documentHighlightProvider;
        if (!documentHighlightsEnabled || provider == null
                || findWordSpan(caretLine, caretCol) == null) {
            clearDocumentHighlights();
            return;
        }

        int line = caretLine;
        int col = caretCol;
        int offset = caretOffset();
        String textSnapshot = buffer.getText();
        TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        DocumentHighlightContext context = new DocumentHighlightContext(
                bufferSnapshot,
                line,
                col,
                offset);

        currentDocumentHighlightTask = getProviderExecutor().submit(() -> {
            List<DocumentHighlight> provided;
            try {
                provided = provider.getDocumentHighlights(context);
            } catch (Exception ignored) {
                provided = List.of();
            }
            List<ResolvedDocumentHighlight> resolved = resolveDocumentHighlights(provided, bufferSnapshot);
            SwingUtilities.invokeLater(() -> {
                if (version != documentHighlightVersion.get()) return;
                if (!documentHighlightsEnabled || provider != documentHighlightProvider) return;
                if (offset != caretOffset() || !textSnapshot.equals(buffer.getText())) return;
                resolvedDocumentHighlights = resolved;
                repaint();
            });
        });
    }

    protected List<ResolvedDocumentHighlight> resolveDocumentHighlights(
            List<DocumentHighlight> highlights,
            TextBuffer snapshot
    ) {
        if (highlights == null || highlights.isEmpty() || snapshot == null) return List.of();
        List<ResolvedDocumentHighlight> resolved = new ArrayList<>(highlights.size());
        for (DocumentHighlight highlight : highlights) {
            if (highlight == null || highlight.range() == null
                    || highlight.range().start() == null || highlight.range().end() == null) {
                continue;
            }
            int start = documentHighlightOffset(snapshot, highlight.range().start());
            int end = documentHighlightOffset(snapshot, highlight.range().end());
            if (start < 0 || end <= start) continue;
            resolved.add(new ResolvedDocumentHighlight(start, end, highlight.kind()));
        }
        resolved.sort(Comparator.comparingInt(ResolvedDocumentHighlight::startOffset));
        return List.copyOf(resolved);
    }

    private static int documentHighlightOffset(TextBuffer snapshot, Position position) {
        int line = position.line();
        int col = position.col();
        if (line < 0 || line >= snapshot.lineCount() || col < 0) return -1;
        String lineText = snapshot.lineAt(line);
        if (col > lineText.length()) return -1;
        return snapshot.offsetOfLine(line) + col;
    }

    private void clearDocumentHighlights() {
        if (resolvedDocumentHighlights.isEmpty()) return;
        resolvedDocumentHighlights = List.of();
        repaint();
    }

    protected void scheduleInlayHintsRefresh() {
        inlayHintsDebounceTimer = restartDebounce(inlayHintsDebounceTimer, inlayHintsDebounceMs,
                this::refreshInlayHints);
    }

    public void refreshInlayHints() {
        stopDebounce(inlayHintsDebounceTimer);
        int version = inlayHintVersion.incrementAndGet();
        if (currentInlayHintTask != null && !currentInlayHintTask.isDone()) {
            currentInlayHintTask.cancel(true);
        }
        if (inlayHintProvider == null) {
            inlayHints.clear();
            invalidateGeometry();
            revalidate();
            repaint();
            return;
        }

        final InlayHintProvider provider = inlayHintProvider;
        final String textSnapshot = buffer.getText();
        final TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        final InlayHintContext ctx = new InlayHintContext(
                bufferSnapshot,
                0,
                Math.max(0, bufferSnapshot.lineCount() - 1));

        currentInlayHintTask = getProviderExecutor().submit(() -> {
            try {
                List<InlayHint> list = provider.getInlayHints(ctx);
                final List<InlayHint> snapshot = list != null ? List.copyOf(list) : List.of();
                SwingUtilities.invokeLater(() -> {
                    if (version != inlayHintVersion.get()) return;
                    if (!buffer.getText().equals(textSnapshot)) return;
                    inlayHints.clear();
                    inlayHints.addAll(snapshot);
                    invalidateGeometry();
                    revalidate();
                    repaint();
                });
            } catch (Exception ignored) {
            }
        });
    }



























    public void searchUpdateQuery(String query, SearchOptions opts) {
        this.searchQuery = query == null ? "" : query;
        if (opts != null) this.searchOptions = opts;
        searchMatches.clear();
        searchCurrentIndex = -1;
        if (!searchQuery.isEmpty()) {
            searchMatches.addAll(searchEngine.findAll(buffer, searchQuery, searchOptions));
            if (!searchMatches.isEmpty()) {
                SearchMatch next = searchEngine.findNext(searchMatches, caretOffset(), searchOptions.isWrapAround());
                if (next != null) searchCurrentIndex = searchMatches.indexOf(next);
                else searchCurrentIndex = 0;
            }
        }
        updateSearchPanelCount();
        repaint();
    }

    protected void updateSearchPanelCount() {
        if (searchPanel != null) searchPanel.updateMatchCount(searchCurrentIndex, searchMatches.size());
    }

    public void searchFindNext() {
        if (searchMatches.isEmpty()) return;
        int from = (searchCurrentIndex >= 0)
                ? searchMatches.get(searchCurrentIndex).endOffset() : caretOffset();
        SearchMatch m = searchEngine.findNext(searchMatches, from, searchOptions.isWrapAround());
        if (m == null) return;
        searchCurrentIndex = searchMatches.indexOf(m);
        selectSearchMatch(m);
    }

    public void searchFindPrev() {
        if (searchMatches.isEmpty()) return;
        int from = (searchCurrentIndex >= 0)
                ? searchMatches.get(searchCurrentIndex).startOffset() : caretOffset();
        SearchMatch m = searchEngine.findPrev(searchMatches, from, searchOptions.isWrapAround());
        if (m == null) return;
        searchCurrentIndex = searchMatches.indexOf(m);
        selectSearchMatch(m);
    }

    protected void selectSearchMatch(SearchMatch m) {
        int startLine = buffer.lineOfOffset(m.startOffset());
        int endLine = buffer.lineOfOffset(m.endOffset());
        selectionStartLine = startLine;
        selectionStartCol = m.startOffset() - buffer.offsetOfLine(startLine);
        caretLine = endLine;
        caretCol = m.endOffset() - buffer.offsetOfLine(endLine);
        updateSearchPanelCount();
        scrollToCaret();
        resetCaretBlink();
        repaint();
    }

    public void searchReplaceCurrent(String replacement) {
        if (readOnly) return;
        if (searchMatches.isEmpty() || searchCurrentIndex < 0) return;
        SearchMatch m = searchMatches.get(searchCurrentIndex);
        String repl = replacement == null ? "" : replacement;
        beginCompoundEdit();
        try {
            deleteText(m.startOffset(), m.endOffset());
            insertText(m.startOffset(), repl);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(m.startOffset() + repl.length());
        clearSelection();
        updateLastEditState();
        searchUpdateQuery(searchQuery, searchOptions);
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void searchReplaceAll(String replacement) {
        if (readOnly) return;
        if (searchMatches.isEmpty()) return;
        String repl = replacement == null ? "" : replacement;
        List<SearchMatch> snapshot = new ArrayList<>(searchMatches);
        beginCompoundEdit();
        try {
            for (int i = snapshot.size() - 1; i >= 0; i--) {
                SearchMatch m = snapshot.get(i);
                deleteText(m.startOffset(), m.endOffset());
                insertText(m.startOffset(), repl);
            }
        } finally {
            endCompoundEdit();
        }
        searchUpdateQuery(searchQuery, searchOptions);
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void addSearchRequestListener(SearchRequestListener listener) {
        if (listener != null) searchRequestListeners.add(listener);
    }

    public void removeSearchRequestListener(SearchRequestListener listener) {
        searchRequestListeners.remove(listener);
    }

    protected void fireSearchRequested(boolean replaceMode) {
        replaceMode = replaceMode && !readOnly;
        String selected = hasSelection() ? getSelectedText() : "";
        if (selected.contains("\n")) selected = "";
        for (SearchRequestListener l : searchRequestListeners) {
            try {
                l.onSearchRequested(selected, replaceMode);
            } catch (Exception ignored) {
            }
        }
    }

    protected SearchPanel createSearchPanel() {
        return new SearchPanel((CodeEditorTextArea) this);
    }

    protected SearchPanel getOrCreateSearchPanel() {
        if (searchPanel == null) searchPanel = createSearchPanel();
        return searchPanel;
    }

    public void showSearchPanel(boolean replaceMode) {
        SearchPanel panel = getOrCreateSearchPanel();
        panel.setReplaceVisible(replaceMode && !readOnly);
        String initial = hasSelection() ? getSelectedText() : "";
        if (!initial.isEmpty() && !initial.contains("\n")) {
            panel.setQuery(initial);
        }
        panel.setVisible(true);
        panel.focusFindField();
        searchUpdateQuery(panel.getFindField().getText(), panel.getOptions());
    }

    public void hideSearchPanel() {
        if (searchPanel != null) searchPanel.setVisible(false);
        searchMatches.clear();
        searchCurrentIndex = -1;
        searchQuery = "";
        requestFocusInWindow();
        repaint();
    }

    public boolean isSearchPanelVisible() {
        return searchPanel != null && searchPanel.isVisible();
    }

    public abstract boolean canShowPopups();

    protected abstract void rebindMoveLineKeys();

    protected abstract WordSpan findWordSpan(int line, int col);

    public abstract void setBracketMatcher(BracketMatcher matcher);

    public abstract CompletableFuture<List<DocumentSymbol>> refreshDocumentSymbolsAsync();
}
