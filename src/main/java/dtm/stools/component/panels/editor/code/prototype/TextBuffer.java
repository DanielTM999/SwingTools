package dtm.stools.component.panels.editor.code.prototype;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class TextBuffer {

    private final StringBuilder content;

    @Getter
    private int version;

    private int[] lineStarts = new int[16];
    private int lineCount;
    private boolean lineIndexReady;

    private final List<TextBufferLineListener> lineListeners = new ArrayList<>();

    private final Deque<Transaction> undoStack = new ArrayDeque<>();
    private final Deque<Transaction> redoStack = new ArrayDeque<>();

    private int compoundDepth = 0;
    private List<UndoEntry> pending = null;
    private Object pendingBeforeState = null;

    private record UndoEntry(int offset, String oldText, String newText) {}
    private record Transaction(List<UndoEntry> entries, Object beforeState, Object afterState) {}

    public record AppliedChange(int offset, String removedText, String insertedText) {}

    public record EditResult(int caretOffset, Object state, List<AppliedChange> changes) {

        public EditResult {
            changes = changes == null ? List.of() : List.copyOf(changes);
        }

        public EditResult(int caretOffset, Object state) {
            this(caretOffset, state, List.of());
        }
    }

    public TextBuffer() {
        this.content = new StringBuilder();
    }

    public TextBuffer(String initial) {
        this.content = new StringBuilder(Objects.toString(initial, ""));
    }

    public void addLineListener(TextBufferLineListener listener) {
        if (listener != null) lineListeners.add(listener);
    }

    public void removeLineListener(TextBufferLineListener listener) {
        lineListeners.remove(listener);
    }

    public String getText() {
        return content.toString();
    }

    public void setText(String text) {
        boolean notify = !lineListeners.isEmpty();
        int oldLineCount = notify ? lineCount() : 0;
        content.setLength(0);
        content.append(Objects.toString(text, ""));
        lineIndexReady = false;
        clearHistory();
        version++;
        if (notify) fireLinesChanged(0, oldLineCount - 1, lineCount() - 1);
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        compoundDepth = 0;
        pending = null;
        pendingBeforeState = null;
    }

    public int length() {
        return content.length();
    }

    public boolean isEmpty(){
        return length() == 0;
    }

    public char charAt(int offset) {
        return content.charAt(offset);
    }

    public String substring(int start, int end) {
        return content.substring(start, end);
    }

    public void insert(int offset, String text) {
        if (offset < 0 || offset > content.length()) throw new StringIndexOutOfBoundsException(offset);
        pushEntry(new UndoEntry(offset, "", text));
        applyEdit(offset, "", text);
        version++;
    }

    public void delete(int start, int end) {
        String removed = content.substring(start, end);
        pushEntry(new UndoEntry(start, removed, ""));
        applyEdit(start, removed, "");
        version++;
    }

    public void replace(int start, int end, String text) {
        String removed = content.substring(start, end);
        pushEntry(new UndoEntry(start, removed, text));
        applyEdit(start, removed, text);
        version++;
    }

    public void beginCompound() {
        beginCompound(null);
    }

    public void beginCompound(Object beforeState) {
        if (compoundDepth == 0) {
            pending = new ArrayList<>();
            pendingBeforeState = beforeState;
        }
        compoundDepth++;
    }

    public void endCompound() {
        endCompound(null);
    }

    public void endCompound(Object afterState) {
        if (compoundDepth == 0) return;
        compoundDepth--;
        if (compoundDepth == 0 && pending != null && !pending.isEmpty()) {
            undoStack.push(new Transaction(pending, pendingBeforeState, afterState));
            redoStack.clear();
            pending = null;
            pendingBeforeState = null;
        } else if (compoundDepth == 0) {
            pending = null;
            pendingBeforeState = null;
        }
    }

    public void updateNextUndoAfterState(Object afterState) {
        if (undoStack.isEmpty()) return;
        Transaction tx = undoStack.pop();
        undoStack.push(new Transaction(tx.entries(), tx.beforeState(), afterState));
    }

    private void pushEntry(UndoEntry entry) {
        if (compoundDepth > 0) {
            if (pending == null) pending = new ArrayList<>();
            pending.add(entry);
        } else {
            List<UndoEntry> tx = new ArrayList<>(1);
            tx.add(entry);
            undoStack.push(new Transaction(tx, null, null));
            redoStack.clear();
        }
    }

    public int undo() {
        return undoEdit().caretOffset();
    }

    public EditResult undoEdit() {
        if (undoStack.isEmpty()) return new EditResult(-1, null);
        Transaction tx = undoStack.pop();
        redoStack.push(tx);
        int caret = -1;
        List<UndoEntry> entries = tx.entries();
        List<AppliedChange> applied = new ArrayList<>(entries.size() * 2);
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoEntry entry = entries.get(i);
            if (!entry.newText.isEmpty()) {
                applyEdit(entry.offset, entry.newText, "");
                applied.add(new AppliedChange(entry.offset, entry.newText, ""));
            }
            if (!entry.oldText.isEmpty()) {
                applyEdit(entry.offset, "", entry.oldText);
                applied.add(new AppliedChange(entry.offset, "", entry.oldText));
            }
            caret = entry.offset + entry.oldText.length();
        }
        version++;
        return new EditResult(caret, tx.beforeState(), applied);
    }

    public int redo() {
        return redoEdit().caretOffset();
    }

    public EditResult redoEdit() {
        if (redoStack.isEmpty()) return new EditResult(-1, null);
        Transaction tx = redoStack.pop();
        undoStack.push(tx);
        int caret = -1;
        List<UndoEntry> entries = tx.entries();
        List<AppliedChange> applied = new ArrayList<>(entries.size() * 2);
        for (UndoEntry entry : entries) {
            if (!entry.oldText.isEmpty()) {
                applyEdit(entry.offset, entry.oldText, "");
                applied.add(new AppliedChange(entry.offset, entry.oldText, ""));
            }
            if (!entry.newText.isEmpty()) {
                applyEdit(entry.offset, "", entry.newText);
                applied.add(new AppliedChange(entry.offset, "", entry.newText));
            }
            caret = entry.offset + entry.newText.length();
        }
        version++;
        return new EditResult(caret, tx.afterState(), applied);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private void applyEdit(int offset, String removed, String inserted) {
        int startLine = lineOfOffset(offset);
        int removedBreaks = countLineBreaks(removed);
        int insertedBreaks = countLineBreaks(inserted);
        content.replace(offset, offset + removed.length(), inserted);

        int oldTailFrom = startLine + 1 + removedBreaks;
        int newTailFrom = startLine + 1 + insertedBreaks;
        int newLineCount = lineCount - removedBreaks + insertedBreaks;
        ensureLineCapacity(newLineCount);
        System.arraycopy(lineStarts, oldTailFrom, lineStarts, newTailFrom, lineCount - oldTailFrom);
        int delta = inserted.length() - removed.length();
        if (delta != 0) {
            for (int i = newTailFrom; i < newLineCount; i++) {
                lineStarts[i] += delta;
            }
        }
        int at = startLine + 1;
        for (int i = 0; at < newTailFrom; i++) {
            if (inserted.charAt(i) == '\n') {
                lineStarts[at++] = offset + i + 1;
            }
        }
        lineCount = newLineCount;
        fireLinesChanged(startLine, removedBreaks, insertedBreaks);
    }

    private void ensureLineIndex() {
        if (!lineIndexReady) rebuildLineIndex();
    }

    private void rebuildLineIndex() {
        int len = content.length();
        ensureLineCapacity(countLineBreaks(content) + 1);
        lineStarts[0] = 0;
        int idx = 1;
        for (int i = 0; i < len; i++) {
            if (content.charAt(i) == '\n') {
                lineStarts[idx++] = i + 1;
            }
        }
        lineCount = idx;
        lineIndexReady = true;
    }

    private void ensureLineCapacity(int required) {
        if (lineStarts.length >= required) return;
        int capacity = Math.max(required, lineStarts.length + (lineStarts.length >> 1));
        lineStarts = Arrays.copyOf(lineStarts, capacity);
    }

    private static int countLineBreaks(CharSequence text) {
        int count = 0;
        for (int i = 0, len = text.length(); i < len; i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }

    private void fireLinesChanged(int startLine, int removedLineBreaks, int insertedLineBreaks) {
        for (int i = 0; i < lineListeners.size(); i++) {
            lineListeners.get(i).linesChanged(startLine, removedLineBreaks, insertedLineBreaks);
        }
    }

    public int lineCount() {
        ensureLineIndex();
        return lineCount;
    }

    public String lineAt(int lineIndex) {
        ensureLineIndex();
        if (lineIndex < 0 || lineIndex >= lineCount) return "";
        int start = lineStarts[lineIndex];
        int end;
        if (lineIndex + 1 < lineCount) {
            end = lineStarts[lineIndex + 1] - 1;
        } else {
            end = content.length();
        }
        if (end < start) end = start;
        return content.substring(start, end);
    }

    public int offsetOfLine(int lineIndex) {
        ensureLineIndex();
        if (lineIndex < 0) return 0;
        if (lineIndex >= lineCount) return content.length();
        return lineStarts[lineIndex];
    }

    public int lineOfOffset(int offset) {
        ensureLineIndex();
        if (offset <= 0) return 0;
        int n = lineCount;
        if (offset >= content.length()) return n - 1;
        int lo = 0, hi = n - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStarts[mid] <= offset) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }
}
