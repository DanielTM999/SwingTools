package dtm.stools.component.panels.editor.code.prototype;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class TextBuffer {

    private final StringBuilder content;

    @Getter
    private int version;

    private int[] lineStarts;
    private int cachedLineIndexVersion = -1;

    private final Deque<Transaction> undoStack = new ArrayDeque<>();
    private final Deque<Transaction> redoStack = new ArrayDeque<>();

    private int compoundDepth = 0;
    private List<UndoEntry> pending = null;
    private Object pendingBeforeState = null;

    private record UndoEntry(int offset, String oldText, String newText) {}
    private record Transaction(List<UndoEntry> entries, Object beforeState, Object afterState) {}
    public record EditResult(int caretOffset, Object state) {}

    public TextBuffer() {
        this.content = new StringBuilder();
    }

    public TextBuffer(String initial) {
        this.content = new StringBuilder(Objects.toString(initial, ""));
    }

    public String getText() {
        return content.toString();
    }

    public void setText(String text) {
        content.setLength(0);
        content.append(Objects.toString(text, ""));
        clearHistory();
        version++;
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
        pushEntry(new UndoEntry(offset, "", text));
        content.insert(offset, text);
        version++;
    }

    public void delete(int start, int end) {
        String removed = content.substring(start, end);
        pushEntry(new UndoEntry(start, removed, ""));
        content.delete(start, end);
        version++;
    }

    public void replace(int start, int end, String text) {
        String removed = content.substring(start, end);
        pushEntry(new UndoEntry(start, removed, text));
        content.replace(start, end, text);
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
        for (int i = entries.size() - 1; i >= 0; i--) {
            UndoEntry entry = entries.get(i);
            if (!entry.newText.isEmpty()) {
                content.delete(entry.offset, entry.offset + entry.newText.length());
            }
            if (!entry.oldText.isEmpty()) {
                content.insert(entry.offset, entry.oldText);
            }
            caret = entry.offset + entry.oldText.length();
        }
        version++;
        return new EditResult(caret, tx.beforeState());
    }

    public int redo() {
        return redoEdit().caretOffset();
    }

    public EditResult redoEdit() {
        if (redoStack.isEmpty()) return new EditResult(-1, null);
        Transaction tx = redoStack.pop();
        undoStack.push(tx);
        int caret = -1;
        for (UndoEntry entry : tx.entries()) {
            if (!entry.oldText.isEmpty()) {
                content.delete(entry.offset, entry.offset + entry.oldText.length());
            }
            if (!entry.newText.isEmpty()) {
                content.insert(entry.offset, entry.newText);
            }
            caret = entry.offset + entry.newText.length();
        }
        version++;
        return new EditResult(caret, tx.afterState());
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private void ensureLineIndex() {
        if (cachedLineIndexVersion == version && lineStarts != null) return;
        int len = content.length();
        int newlines = 0;
        for (int i = 0; i < len; i++) {
            if (content.charAt(i) == '\n') newlines++;
        }
        int[] starts = new int[newlines + 1];
        starts[0] = 0;
        int idx = 1;
        for (int i = 0; i < len; i++) {
            if (content.charAt(i) == '\n') {
                starts[idx++] = i + 1;
            }
        }
        lineStarts = starts;
        cachedLineIndexVersion = version;
    }

    public int lineCount() {
        ensureLineIndex();
        return lineStarts.length;
    }

    public String lineAt(int lineIndex) {
        ensureLineIndex();
        if (lineIndex < 0 || lineIndex >= lineStarts.length) return "";
        int start = lineStarts[lineIndex];
        int end;
        if (lineIndex + 1 < lineStarts.length) {
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
        if (lineIndex >= lineStarts.length) return content.length();
        return lineStarts[lineIndex];
    }

    public int lineOfOffset(int offset) {
        ensureLineIndex();
        if (offset <= 0) return 0;
        int n = lineStarts.length;
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
