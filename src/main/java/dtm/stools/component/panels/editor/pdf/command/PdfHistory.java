package dtm.stools.component.panels.editor.pdf.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class PdfHistory {
    private final Deque<PdfHistoryEntry> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();

    public void record(PdfHistoryEntry entry, int limit) {
        undo.addLast(entry);
        while (undo.size() > Math.max(0, limit)) undo.removeFirst();
        redo.clear();
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public Optional<String> undoLabel() { return Optional.ofNullable(undo.peekLast()).map(PdfHistoryEntry::label); }
    public Optional<String> redoLabel() { return Optional.ofNullable(redo.peekLast()).map(PdfHistoryEntry::label); }

    public PdfHistoryEntry popUndo() { return undo.removeLast(); }
    public PdfHistoryEntry popRedo() { return redo.removeLast(); }
    public void pushUndo(PdfHistoryEntry entry) { undo.addLast(entry); }
    public void pushRedo(PdfHistoryEntry entry) { redo.addLast(entry); }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}
