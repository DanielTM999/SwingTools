package dtm.stools.component.panels.editor.sheet.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class History {
    private final Deque<SheetChange> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private int limit;

    public History(int limit) { this.limit = Math.max(0, limit); }

    public synchronized void push(SheetChange change) { undo.addLast(change); redo.clear(); trim(); }
    public synchronized SheetChange popUndo() { SheetChange c = undo.pollLast(); if (c != null) redo.addLast(c); return c; }
    public synchronized SheetChange popRedo() { SheetChange c = redo.pollLast(); if (c != null) undo.addLast(c); return c; }
    public synchronized boolean canUndo() { return !undo.isEmpty(); }
    public synchronized boolean canRedo() { return !redo.isEmpty(); }
    public synchronized long currentId() { return undo.isEmpty() ? 0 : undo.peekLast().id(); }
    public synchronized String undoLabel() { return undo.isEmpty() ? "" : undo.peekLast().label(); }
    public synchronized String redoLabel() { return redo.isEmpty() ? "" : redo.peekLast().label(); }
    public synchronized List<String> undoLabels() { return undo.stream().map(SheetChange::label).toList().reversed(); }
    public synchronized void clear() { undo.clear(); redo.clear(); }
    public synchronized void setLimit(int value) { limit = Math.max(0, value); trim(); }
    public synchronized int size() { return undo.size(); }

    private void trim() { while (undo.size() > limit) undo.removeFirst(); while (redo.size() > limit) redo.removeFirst(); }
}
