package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlight;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightContext;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightProvider;
import dtm.stools.component.panels.editor.code.provider.RenameContext;
import dtm.stools.component.panels.editor.code.provider.RenameProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.rename.DefaultLinkedRenamePopupFactory;
import dtm.stools.component.panels.editor.code.rename.InlineRenamePresenter;
import dtm.stools.component.panels.editor.code.rename.LinkedRenamePopupContext;
import dtm.stools.component.panels.editor.code.rename.LinkedRenamePopupFactory;
import dtm.stools.component.panels.editor.code.rename.RenamePrepareContext;
import dtm.stools.component.panels.editor.code.rename.RenamePreparation;
import dtm.stools.component.panels.editor.code.rename.RenamePresenter;
import dtm.stools.component.panels.editor.code.rename.RenameSession;
import dtm.stools.component.panels.editor.code.rename.RenameStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CodeEditorTextAreaRename extends CodeEditorTextAreaActions {

    private static final int RENAME_HINT_DURATION_MS = 2600;

    protected List<int[]> linkedRanges;
    protected int linkedPrimary = -1;
    protected String linkedOriginalName;
    protected RenameSession linkedSession;
    protected boolean linkedSyncing;
    protected boolean linkedSyncScheduled;
    protected boolean linkedBroken;
    protected int linkedCaretRel;
    protected final Map<String, Boolean> linkedOptionValues = new LinkedHashMap<>();
    protected Position pendingRenamePosition;
    protected int pendingRenameOffset = -1;
    protected List<TextEdit> pendingRenameFallback = List.of();
    protected JWindow linkedRenameWindow;
    protected JWindow renameHintWindow;
    protected Timer renameHintTimer;
    protected boolean linkedRepositionScheduled;
    protected LinkedRenamePopupFactory linkedRenamePopupFactory = new DefaultLinkedRenamePopupFactory();

    protected CodeEditorTextAreaRename(TextBuffer buffer) {
        super(buffer);
        installLinkedRenameTracking();
    }

    public void setRenameStyle(RenameStyle style) {
        setRenamePresenter(style == null ? null : style.createPresenter());
    }

    @Override
    public void triggerRename() {
        if (readOnly) return;
        RenameProvider provider = renameProvider;
        if (provider == null) return;
        if (hasActiveLinkedRename()) return;

        int[] word = wordBoundsAtCaret();
        String current = word == null ? "" : buffer.substring(word[0], word[1]);
        String textSnapshot = buffer.getText();
        int line = caretLine;
        int col = caretCol;
        int offset = caretOffset();
        Position position = new Position(line, col);
        Range wordRange = word == null ? null : new Range(positionOf(word[0]), positionOf(word[1]));
        RenamePrepareContext prepareContext = new RenamePrepareContext(textSnapshot, position, offset, current);
        DocumentHighlightProvider highlightProvider = documentHighlightProvider;

        getProviderExecutor().submit(() -> {
            RenamePreparation preparation;
            try {
                preparation = provider.prepareRename(prepareContext);
            } catch (Exception ex) {
                preparation = null;
            }
            if (preparation == null) {
                preparation = wordRange == null
                        ? RenamePreparation.rejected("")
                        : RenamePreparation.of(wordRange, current);
            }
            if (!preparation.isRejected() && preparation.range() == null && wordRange != null) {
                preparation = preparation.withRange(wordRange);
            }
            if (!preparation.isRejected() && preparation.occurrences().isEmpty() && highlightProvider != null) {
                preparation = preparation.withOccurrences(
                        highlightOccurrences(highlightProvider, textSnapshot, line, col, offset));
            }
            final RenamePreparation resolved = preparation;
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                if (resolved.isRejected() || resolved.range() == null) {
                    String message = resolved.rejection();
                    showRenameHint(message == null || message.isBlank()
                            ? text("rename.rejected.default", "This element can't be renamed")
                            : message);
                    return;
                }
                presentRename(provider, prepareContext, resolved);
            });
        });
    }

    protected List<Range> highlightOccurrences(
            DocumentHighlightProvider provider,
            String textSnapshot,
            int line,
            int col,
            int offset
    ) {
        try {
            List<DocumentHighlight> highlights = provider.getDocumentHighlights(
                    new DocumentHighlightContext(new TextBuffer(textSnapshot), line, col, offset));
            if (highlights == null || highlights.isEmpty()) return List.of();
            List<Range> ranges = new ArrayList<>(highlights.size());
            for (DocumentHighlight highlight : highlights) {
                if (highlight != null && highlight.range() != null) ranges.add(highlight.range());
            }
            return ranges;
        } catch (Exception ex) {
            return List.of();
        }
    }

    protected void presentRename(RenameProvider provider, RenamePrepareContext prepareContext, RenamePreparation preparation) {
        Range range = preparation.range();
        int start = offsetOf(range.start());
        int end = offsetOf(range.end());
        String currentName = preparation.placeholder() != null && !preparation.placeholder().isEmpty()
                ? preparation.placeholder()
                : buffer.substring(Math.min(start, end), Math.max(start, end));

        Point anchor = caretScreenPoint();
        boolean localFallback = preparation.localFallback();
        RenameSession session = new RenameSession(
                this,
                anchor,
                currentName,
                range,
                preparation.occurrences(),
                preparation.options(),
                name -> validateRenameName(provider, prepareContext, currentName, name),
                (name, options) -> executeRename(provider, prepareContext, name, options, localFallback),
                this::onRenameSessionCancelled,
                this::startLinkedRename,
                preparation.kind()
        );

        RenamePresenter presenter = preparation.presenter() != null
                ? preparation.presenter()
                : renamePresenter != null ? renamePresenter : new InlineRenamePresenter();
        try {
            presenter.present(session);
        } catch (Exception ex) {
            if (!session.isFinished() && session != linkedSession) session.cancel();
        }
    }

    protected String validateRenameName(RenameProvider provider, RenamePrepareContext context, String currentName, String newName) {
        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty()) return text("rename.error.empty", "The new name can't be empty");
        if (name.equals(currentName)) return text("rename.error.same", "The new name is the same as the current one");
        try {
            String error = provider.validateNewName(context, name);
            return error == null || error.isBlank() ? null : error;
        } catch (Exception ex) {
            return null;
        }
    }

    protected void onRenameSessionCancelled() {
        requestFocusInWindow();
    }

    protected void executeRename(RenameProvider provider, RenamePrepareContext prepareContext, String newName, Map<String, Boolean> options, boolean localFallback) {
        Position position = pendingRenamePosition != null ? pendingRenamePosition : prepareContext.position();
        int offset = pendingRenameOffset >= 0 ? pendingRenameOffset : prepareContext.offset();
        List<TextEdit> fallback = pendingRenameFallback == null || !localFallback ? List.of() : pendingRenameFallback;
        pendingRenamePosition = null;
        pendingRenameOffset = -1;
        pendingRenameFallback = List.of();

        String textSnapshot = buffer.getText();
        RenameContext context = new RenameContext(textSnapshot, position, offset, newName, options);
        requestFocusInWindow();
        getProviderExecutor().submit(() -> {
            List<TextEdit> edits;
            try {
                edits = provider.computeRenameEdits(context);
            } catch (Exception ex) {
                edits = Collections.emptyList();
            }
            final List<TextEdit> snapshot = edits != null ? List.copyOf(edits) : List.of();
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                List<TextEdit> applied = List.of();
                if (!snapshot.isEmpty()) {
                    applyEdits(snapshot);
                    applied = snapshot;
                } else if (!fallback.isEmpty()) {
                    applyEdits(fallback);
                    applied = fallback;
                }
                try {
                    provider.onRenameApplied(context, applied);
                } catch (Exception ignored) {
                }
                if (applied.isEmpty() && !localFallback) {
                    showRenameHint(text("rename.error.noEdits", "Rename produced no changes"));
                }
            });
        });
    }

    protected int[] wordBoundsAtCaret() {
        int off = caretOffset();
        int start = off;
        int end = off;
        while (start > 0 && wordDetector.isWordChar(buffer.charAt(start - 1))) start--;
        while (end < buffer.length() && wordDetector.isWordChar(buffer.charAt(end))) end++;
        if (end <= start) return null;
        return new int[]{start, end};
    }

    public boolean hasActiveLinkedRename() {
        return linkedSession != null && linkedRanges != null;
    }

    protected void startLinkedRename(RenameSession session) {
        if (session == null || session.isFinished() || readOnly) return;
        if (hasActiveLinkedRename()) abortLinkedRename();
        Range primaryRange = session.range();
        if (primaryRange == null) return;

        int primaryStart = offsetOf(primaryRange.start());
        int primaryEnd = offsetOf(primaryRange.end());
        if (primaryEnd < primaryStart) {
            int tmp = primaryStart;
            primaryStart = primaryEnd;
            primaryEnd = tmp;
        }
        String original = buffer.substring(primaryStart, primaryEnd);

        List<int[]> ranges = new ArrayList<>();
        ranges.add(new int[]{primaryStart, primaryEnd - primaryStart});
        for (Range occurrence : session.occurrences()) {
            int s = offsetOf(occurrence.start());
            int e = offsetOf(occurrence.end());
            if (e < s) {
                int tmp = s;
                s = e;
                e = tmp;
            }
            if (e - s != original.length()) continue;
            if (!buffer.substring(s, e).equals(original)) continue;
            boolean overlaps = false;
            for (int[] existing : ranges) {
                if (s < existing[0] + existing[1] && existing[0] < e) {
                    overlaps = true;
                    break;
                }
                if (s == existing[0] && e == existing[0] + existing[1]) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) ranges.add(new int[]{s, e - s});
        }
        int[] primary = ranges.get(0);
        ranges.sort(Comparator.comparingInt(r -> r[0]));

        int caret = caretOffset();
        linkedCaretRel = Math.max(0, Math.min(caret - primaryStart, original.length()));
        linkedRanges = ranges;
        linkedPrimary = ranges.indexOf(primary);
        linkedOriginalName = original;
        linkedSession = session;
        linkedBroken = false;
        linkedSyncScheduled = false;
        linkedOptionValues.clear();
        linkedOptionValues.putAll(session.defaultOptionValues());

        hideAutoCompletePopup();
        clearGhostText();
        linkedSyncing = true;
        try {
            Position ps = positionOf(primaryStart);
            Position pe = positionOf(primaryEnd);
            setSelection(ps.line(), ps.col(), pe.line(), pe.col());
        } finally {
            linkedSyncing = false;
        }
        showLinkedRenameWindow();
        requestFocusInWindow();
        repaint();
    }

    public boolean commitLinkedRename() {
        return finishLinkedRename(true, true);
    }

    public void cancelLinkedRename() {
        finishLinkedRename(false, true);
    }

    protected void abortLinkedRename() {
        if (!hasActiveLinkedRename()) return;
        RenameSession session = linkedSession;
        clearLinkedRenameState();
        if (session != null && !session.isFinished()) session.cancel();
    }

    protected boolean finishLinkedRename(boolean commit, boolean keepOnInvalid) {
        if (!hasActiveLinkedRename()) return false;
        if (linkedBroken) {
            abortLinkedRename();
            return false;
        }
        RenameSession session = linkedSession;
        int[] primary = linkedRanges.get(linkedPrimary);
        String newName = buffer.substring(primary[0], primary[0] + primary[1]);
        String original = linkedOriginalName;

        if (commit && !newName.equals(original)) {
            String error = session.validate(newName);
            if (error != null) {
                if (keepOnInvalid) {
                    showRenameHint(error);
                    return false;
                }
                commit = false;
            }
        } else {
            commit = false;
        }

        List<int[]> ranges = linkedRanges;
        int primaryIndex = linkedPrimary;
        Map<String, Boolean> options = new LinkedHashMap<>(linkedOptionValues);
        int caretRel = Math.max(0, Math.min(caretOffset() - primary[0], newName.length()));

        linkedSyncing = true;
        try {
            replaceLinkedRanges(ranges, original, -1);
        } finally {
            linkedSyncing = false;
        }
        int primaryStart = ranges.get(primaryIndex)[0];
        clearLinkedRenameState();

        if (!commit) {
            setCaretFromOffset(primaryStart + Math.min(caretRel, original.length()));
            clearSelection();
            session.cancel();
            repaint();
            return false;
        }

        List<TextEdit> fallback = new ArrayList<>(ranges.size());
        for (int[] r : ranges) {
            fallback.add(TextEdit.replace(new Range(positionOf(r[0]), positionOf(r[0] + r[1])), newName));
        }
        int position = primaryStart + Math.min(linkedCaretRel, original.length());
        setCaretFromOffset(position);
        clearSelection();
        pendingRenamePosition = positionOf(position);
        pendingRenameOffset = position;
        pendingRenameFallback = List.copyOf(fallback);
        boolean committed = session.commit(newName, options);
        if (!committed) {
            pendingRenamePosition = null;
            pendingRenameOffset = -1;
            pendingRenameFallback = List.of();
        }
        repaint();
        return committed;
    }

    protected void replaceLinkedRanges(List<int[]> ranges, String name, int skipIndex) {
        beginCompoundEdit();
        try {
            for (int i = ranges.size() - 1; i >= 0; i--) {
                if (i == skipIndex) continue;
                int[] r = ranges.get(i);
                if (buffer.substring(r[0], r[0] + r[1]).equals(name)) continue;
                if (r[1] > 0) deleteText(r[0], r[0] + r[1]);
                if (!name.isEmpty()) insertText(r[0], name);
            }
        } finally {
            endCompoundEdit();
        }
        int delta = 0;
        for (int i = 0; i < ranges.size(); i++) {
            int[] r = ranges.get(i);
            r[0] += delta;
            if (i == skipIndex) continue;
            int change = name.length() - r[1];
            r[1] = name.length();
            delta += change;
        }
    }

    protected void clearLinkedRenameState() {
        linkedRanges = null;
        linkedPrimary = -1;
        linkedOriginalName = null;
        linkedSession = null;
        linkedBroken = false;
        linkedSyncScheduled = false;
        linkedOptionValues.clear();
        hideLinkedRenameWindow();
        repaint();
    }

    protected void syncLinkedRanges() {
        linkedSyncScheduled = false;
        if (!hasActiveLinkedRename() || linkedBroken) return;
        int[] primary = linkedRanges.get(linkedPrimary);
        String name = buffer.substring(primary[0], primary[0] + primary[1]);
        int caretRel = caretOffset() - primary[0];
        boolean caretInside = caretRel >= 0 && caretRel <= primary[1];
        boolean selectionActive = hasSelection();
        int selStartRel = selectionActive ? getSelectionStart() - primary[0] : 0;
        int selEndRel = selectionActive ? getSelectionEnd() - primary[0] : 0;

        linkedSyncing = true;
        try {
            replaceLinkedRanges(linkedRanges, name, linkedPrimary);
            int start = linkedRanges.get(linkedPrimary)[0];
            if (selectionActive && selStartRel >= 0 && selEndRel <= name.length()) {
                Position ps = positionOf(start + selStartRel);
                Position pe = positionOf(start + selEndRel);
                setSelection(ps.line(), ps.col(), pe.line(), pe.col());
            } else if (caretInside) {
                setCaretFromOffset(start + caretRel);
                clearSelection();
            }
        } finally {
            linkedSyncing = false;
        }
        showLinkedRenameWindow();
        repaint();
    }

    @Override
    protected void onLinkedRenameInsert(int offset, int insertedLen) {
        if (!hasActiveLinkedRename() || linkedSyncing || insertedLen <= 0) return;
        int[] primary = linkedRanges.get(linkedPrimary);
        boolean insidePrimary = offset >= primary[0] && offset <= primary[0] + primary[1];
        for (int i = 0; i < linkedRanges.size(); i++) {
            int[] r = linkedRanges.get(i);
            if (i == linkedPrimary) {
                if (offset < r[0]) r[0] += insertedLen;
                else if (offset <= r[0] + r[1]) r[1] += insertedLen;
                continue;
            }
            if (offset <= r[0]) {
                r[0] += insertedLen;
            } else if (offset < r[0] + r[1]) {
                linkedBroken = true;
            }
        }
        if (insidePrimary) scheduleLinkedSync();
        else linkedBroken = true;
        if (linkedBroken) scheduleLinkedAbort();
    }

    @Override
    protected void onLinkedRenameDelete(int start, int end) {
        if (!hasActiveLinkedRename() || linkedSyncing || end <= start) return;
        int delta = end - start;
        int[] primary = linkedRanges.get(linkedPrimary);
        boolean insidePrimary = start >= primary[0] && end <= primary[0] + primary[1];
        for (int i = 0; i < linkedRanges.size(); i++) {
            int[] r = linkedRanges.get(i);
            int rs = r[0];
            int re = rs + r[1];
            if (end <= rs) {
                r[0] = rs - delta;
            } else if (start >= re) {
                continue;
            } else if (i == linkedPrimary && insidePrimary) {
                r[1] = r[1] - delta;
            } else {
                linkedBroken = true;
            }
        }
        if (insidePrimary) scheduleLinkedSync();
        else linkedBroken = true;
        if (linkedBroken) scheduleLinkedAbort();
    }

    protected void scheduleLinkedSync() {
        if (linkedSyncScheduled) return;
        linkedSyncScheduled = true;
        SwingUtilities.invokeLater(this::syncLinkedRanges);
    }

    protected void scheduleLinkedAbort() {
        SwingUtilities.invokeLater(() -> {
            if (hasActiveLinkedRename() && linkedBroken) abortLinkedRename();
        });
    }

    @Override
    protected void onLinkedRenameCaretMoved(int offset) {
        if (!hasActiveLinkedRename() || linkedSyncing) return;
        int[] primary = linkedRanges.get(linkedPrimary);
        if (offset >= primary[0] && offset <= primary[0] + primary[1]) return;
        RenameSession session = linkedSession;
        SwingUtilities.invokeLater(() -> {
            if (!hasActiveLinkedRename() || linkedSession != session) return;
            int[] current = linkedRanges.get(linkedPrimary);
            int caret = caretOffset();
            if (caret >= current[0] && caret <= current[0] + current[1]) return;
            int keep = caret;
            finishLinkedRename(true, false);
            if (!hasActiveLinkedRename()) {
                setCaretFromOffset(Math.min(keep, buffer.length()));
            }
        });
    }

    @Override
    protected boolean handleLinkedRenameKey(KeyEvent e) {
        if (!hasActiveLinkedRename()) return false;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER -> {
                if (isAutoCompleteVisible()) return false;
                commitLinkedRename();
                e.consume();
                return true;
            }
            case KeyEvent.VK_ESCAPE -> {
                if (isAutoCompleteVisible()) return false;
                cancelLinkedRename();
                e.consume();
                return true;
            }
            case KeyEvent.VK_TAB -> {
                e.consume();
                return true;
            }
            default -> {
                if (isAutoCompleteVisible()) return false;
                return handleLinkedRenameNavigation(e);
            }
        }
    }

    protected boolean handleLinkedRenameNavigation(KeyEvent e) {
        if (e.isAltDown()) return false;
        int[] primary = linkedRanges.get(linkedPrimary);
        int start = primary[0];
        int end = primary[0] + primary[1];
        int caret = caretOffset();
        if (caret < start || caret > end) return false;
        boolean ctrl = e.isControlDown() || e.isMetaDown();
        boolean shift = e.isShiftDown();
        boolean selection = hasSelection();
        int selectionStart = selection ? getSelectionStart() : caret;
        int selectionEnd = selection ? getSelectionEnd() : caret;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> {
                int target;
                if (ctrl) target = start;
                else if (selection && !shift) target = selectionStart;
                else target = caret - 1;
                moveLinkedCaret(target, shift, start, end);
            }
            case KeyEvent.VK_RIGHT -> {
                int target;
                if (ctrl) target = end;
                else if (selection && !shift) target = selectionEnd;
                else target = caret + 1;
                moveLinkedCaret(target, shift, start, end);
            }
            case KeyEvent.VK_HOME -> {
                if (ctrl) return false;
                moveLinkedCaret(start, shift, start, end);
            }
            case KeyEvent.VK_END -> {
                if (ctrl) return false;
                moveLinkedCaret(end, shift, start, end);
            }
            case KeyEvent.VK_BACK_SPACE -> {
                if (selection) return false;
                if (caret <= start) break;
                if (!ctrl) return false;
                deleteText(start, caret);
                setCaretFromOffset(start);
            }
            case KeyEvent.VK_DELETE -> {
                if (selection) return false;
                if (caret >= end) break;
                if (!ctrl) return false;
                deleteText(caret, end);
                setCaretFromOffset(caret);
            }
            case KeyEvent.VK_A -> {
                if (!ctrl || shift) return false;
                moveLinkedCaret(start, false, start, end);
                moveLinkedCaret(end, true, start, end);
            }
            default -> {
                return false;
            }
        }
        e.consume();
        fireStateChangedIfNeeded();
        scrollToCaret();
        resetCaretBlink();
        repaint();
        return true;
    }

    protected void moveLinkedCaret(int target, boolean extend, int start, int end) {
        int clamped = Math.max(start, Math.min(end, target));
        int caret = caretOffset();
        int anchor = caret;
        if (hasSelection()) {
            anchor = caret == getSelectionStart() ? getSelectionEnd() : getSelectionStart();
        }
        if (extend && anchor != clamped) {
            Position anchorPosition = positionOf(anchor);
            Position targetPosition = positionOf(clamped);
            setSelection(anchorPosition.line(), anchorPosition.col(), targetPosition.line(), targetPosition.col());
            return;
        }
        setCaretFromOffset(clamped);
        clearSelection();
    }

    @Override
    protected void onLinkedRenameFocusLost(FocusEvent e) {
        if (!hasActiveLinkedRename()) return;
        Component opposite = e.getOppositeComponent();
        if (opposite != null && linkedRenameWindow != null
                && SwingUtilities.isDescendingFrom(opposite, linkedRenameWindow)) {
            return;
        }
        if (e.isTemporary()) return;
        SwingUtilities.invokeLater(() -> {
            if (hasActiveLinkedRename() && !isFocusOwner()) finishLinkedRename(true, false);
        });
    }

    @Override
    protected void paintLinkedRename(Graphics2D g2, FontMetrics fm, int lineHeight) {
        if (!hasActiveLinkedRename()) return;
        Color accent = linkedRenameAccent();
        Color fill = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40);
        Stroke previous = g2.getStroke();
        Object previousAa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < linkedRanges.size(); i++) {
            int[] r = linkedRanges.get(i);
            int start = clampOffset(r[0]);
            int end = clampOffset(r[0] + r[1]);
            int line = buffer.lineOfOffset(start);
            if (isLineHidden(line)) continue;
            String lineText = buffer.lineAt(line);
            int lineOffset = buffer.offsetOfLine(line);
            int endLine = buffer.lineOfOffset(end);
            int endCol = endLine == line ? end - lineOffset : lineText.length();
            int x1 = baseVisualXForColumn(line, lineText, start - lineOffset, fm);
            int x2 = baseVisualXForColumn(line, lineText, endCol, fm);
            int width = Math.max(2, x2 - x1);
            int y = yOfBufferLine(line);
            if (i == linkedPrimary) {
                g2.setColor(fill);
                g2.fillRect(x1 - 1, y, width + 2, lineHeight);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x1 - 2, y, width + 3, lineHeight - 1, 4, 4);
            } else {
                g2.setColor(fill);
                g2.fillRect(x1, y, width, lineHeight);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(x1, y + lineHeight - 1, x1 + width, y + lineHeight - 1);
            }
        }
        g2.setStroke(previous);
        if (previousAa != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
    }

    protected Color linkedRenameAccent() {
        Color color = UIManager.getColor("Component.focusColor");
        if (color == null) color = UIManager.getColor("Component.accentColor");
        if (color == null) color = new Color(0x3B82F6);
        return color;
    }

    public LinkedRenamePopupFactory getLinkedRenamePopupFactory() {
        return linkedRenamePopupFactory;
    }

    public void setLinkedRenamePopupFactory(LinkedRenamePopupFactory factory) {
        this.linkedRenamePopupFactory = factory == null ? new DefaultLinkedRenamePopupFactory() : factory;
        if (hasActiveLinkedRename()) {
            hideLinkedRenameWindow();
            showLinkedRenameWindow();
        }
    }

    protected void installLinkedRenameTracking() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                onLinkedRenameGeometryChanged();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                onLinkedRenameGeometryChanged();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                hideLinkedRenameWindowTemporarily();
            }
        });
        addHierarchyBoundsListener(new HierarchyBoundsListener() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                onLinkedRenameGeometryChanged();
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                onLinkedRenameGeometryChanged();
            }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing()) onLinkedRenameGeometryChanged();
            else hideLinkedRenameWindowTemporarily();
        });
    }

    protected void onLinkedRenameGeometryChanged() {
        hideRenameHint();
        if (!hasActiveLinkedRename()) return;
        if (linkedRepositionScheduled) return;
        linkedRepositionScheduled = true;
        SwingUtilities.invokeLater(() -> {
            linkedRepositionScheduled = false;
            if (hasActiveLinkedRename()) positionLinkedRenameWindow();
        });
    }

    protected void hideLinkedRenameWindowTemporarily() {
        if (linkedRenameWindow != null) linkedRenameWindow.setVisible(false);
    }

    protected void showLinkedRenameWindow() {
        if (!hasActiveLinkedRename() || !canShowPopups()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;
        if (linkedRenameWindow == null || linkedRenameWindow.getOwner() != owner) {
            hideLinkedRenameWindow();
            JComponent content = buildLinkedRenamePanel();
            if (content == null) return;
            linkedRenameWindow = new JWindow(owner);
            linkedRenameWindow.setFocusableWindowState(false);
            linkedRenameWindow.setContentPane(content);
            linkedRenameWindow.pack();
        }
        positionLinkedRenameWindow();
    }

    protected void positionLinkedRenameWindow() {
        JWindow window = linkedRenameWindow;
        if (window == null || !hasActiveLinkedRename()) return;
        if (!isShowing() || !canShowPopups()) {
            window.setVisible(false);
            return;
        }
        int[] primary = linkedRanges.get(linkedPrimary);
        FontMetrics fm = fontMetricsFor(getFont());
        int offset = clampOffset(primary[0]);
        int line = buffer.lineOfOffset(offset);
        if (isLineHidden(line)) {
            window.setVisible(false);
            return;
        }
        String lineText = buffer.lineAt(line);
        int x = baseVisualXForColumn(line, lineText, offset - buffer.offsetOfLine(line), fm);
        int endOffset = clampOffset(primary[0] + primary[1]);
        int endX = buffer.lineOfOffset(endOffset) == line
                ? baseVisualXForColumn(line, lineText, endOffset - buffer.offsetOfLine(line), fm)
                : x;
        int y = yOfBufferLine(line);
        int height = fm.getHeight();
        Rectangle visible = getVisibleRect();
        if (y + height <= visible.y || y >= visible.y + visible.height
                || x > visible.x + visible.width || Math.max(endX, x + 1) < visible.x) {
            window.setVisible(false);
            return;
        }
        Point screen;
        try {
            screen = getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            window.setVisible(false);
            return;
        }
        Rectangle anchor = new Rectangle(screen.x + x, screen.y + y, Math.max(1, endX - x), height);
        LinkedRenamePopupFactory factory = linkedRenamePopupFactory != null
                ? linkedRenamePopupFactory
                : new DefaultLinkedRenamePopupFactory();
        Point location = factory.locate(anchor, window.getSize());
        if (location == null) location = new Point(anchor.x, anchor.y + anchor.height + 2);
        window.setLocation(location);
        if (!window.isVisible()) window.setVisible(true);
    }

    protected JComponent buildLinkedRenamePanel() {
        LinkedRenamePopupFactory factory = linkedRenamePopupFactory != null
                ? linkedRenamePopupFactory
                : new DefaultLinkedRenamePopupFactory();
        LinkedRenamePopupContext context = new LinkedRenamePopupContext(
                this,
                linkedSession,
                new LinkedHashMap<>(linkedOptionValues),
                (id, value) -> linkedOptionValues.put(id, value),
                this::requestFocusInWindow,
                text("rename.hint.inline", "Enter to rename, Esc to cancel")
        );
        try {
            return factory.createContent(context);
        } catch (Exception ex) {
            return new DefaultLinkedRenamePopupFactory().createContent(context);
        }
    }

    protected void hideLinkedRenameWindow() {
        if (linkedRenameWindow != null) {
            linkedRenameWindow.setVisible(false);
            linkedRenameWindow.dispose();
            linkedRenameWindow = null;
        }
    }

    public void showRenameHint(String message) {
        if (message == null || message.isBlank() || !canShowPopups()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;
        hideRenameHint();
        Color bg = UIManager.getColor("ToolTip.background");
        if (bg == null) bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("ToolTip.foreground");
        if (fg == null) fg = UIManager.getColor("Label.foreground");
        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = Color.GRAY;

        JLabel label = new JLabel(message);
        label.setForeground(fg);
        label.setOpaque(true);
        label.setBackground(bg);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        renameHintWindow = new JWindow(owner);
        renameHintWindow.setFocusableWindowState(false);
        renameHintWindow.setContentPane(label);
        renameHintWindow.pack();
        Point p = caretScreenPoint();
        Point screen;
        try {
            screen = getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            renameHintWindow.dispose();
            renameHintWindow = null;
            return;
        }
        int offsetY = linkedRenameWindow != null && linkedRenameWindow.isVisible()
                ? linkedRenameWindow.getHeight() + 4
                : 2;
        renameHintWindow.setLocation(screen.x + p.x, screen.y + p.y + offsetY);
        renameHintWindow.setVisible(true);
        renameHintTimer = new Timer(RENAME_HINT_DURATION_MS, ev -> hideRenameHint());
        renameHintTimer.setRepeats(false);
        renameHintTimer.start();
    }

    protected void hideRenameHint() {
        if (renameHintTimer != null) {
            renameHintTimer.stop();
            renameHintTimer = null;
        }
        if (renameHintWindow != null) {
            renameHintWindow.setVisible(false);
            renameHintWindow.dispose();
            renameHintWindow = null;
        }
    }

    @Override
    protected void disposeTransientWindows() {
        super.disposeTransientWindows();
        if (hasActiveLinkedRename()) abortLinkedRename();
        hideLinkedRenameWindow();
        hideRenameHint();
    }
}
