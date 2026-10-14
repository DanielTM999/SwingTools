package dtm.stools.component.inputfields.textarea;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

final class TextAreaUndoManager extends UndoManager {

    private static final long MERGE_WINDOW_MS = 1000L;

    private CompoundEdit typing;
    private DocumentEvent.EventType typingType;
    private int typingNextOffset;
    private boolean typingEndsWithWhitespace;
    private long typingTime;

    private CompoundEdit group;
    private int groupDepth;
    private boolean groupHasEdits;

    synchronized void record(UndoableEdit edit) {
        if (edit == null) return;
        if (group != null) {
            group.addEdit(edit);
            groupHasEdits = true;
            return;
        }
        if (edit instanceof DocumentEvent event) {
            Character ch = singleChar(event);
            if (ch != null && canMerge(event, ch)) {
                typing.addEdit(edit);
                track(event, ch);
                return;
            }
            closeTyping();
            if (ch != null && ch != '\n') {
                typing = new CompoundEdit();
                typing.addEdit(edit);
                super.addEdit(typing);
                track(event, ch);
                return;
            }
        } else {
            closeTyping();
        }
        super.addEdit(edit);
    }

    synchronized void beginGroup() {
        if (groupDepth++ > 0) return;
        closeTyping();
        group = new CompoundEdit();
        groupHasEdits = false;
    }

    synchronized void endGroup() {
        if (groupDepth == 0) return;
        if (--groupDepth > 0) return;
        CompoundEdit finished = group;
        group = null;
        finished.end();
        if (groupHasEdits) super.addEdit(finished);
    }

    synchronized void closeTyping() {
        if (typing != null) {
            typing.end();
            typing = null;
        }
        typingType = null;
    }

    @Override
    public synchronized boolean canUndo() {
        return isTypingOpen() || super.canUndo();
    }

    @Override
    public synchronized boolean canRedo() {
        return !isTypingOpen() && super.canRedo();
    }

    @Override
    public synchronized void undo() {
        closeTyping();
        super.undo();
    }

    @Override
    public synchronized void redo() {
        closeTyping();
        super.redo();
    }

    @Override
    public synchronized void discardAllEdits() {
        closeTyping();
        super.discardAllEdits();
    }

    private boolean isTypingOpen() {
        return typing != null && typing.isInProgress();
    }

    private boolean canMerge(DocumentEvent event, char ch) {
        if (!isTypingOpen() || typingType != event.getType()) return false;
        if (ch == '\n') return false;
        if (System.currentTimeMillis() - typingTime > MERGE_WINDOW_MS) return false;
        int offset = event.getOffset();
        if (event.getType() == DocumentEvent.EventType.INSERT) {
            if (offset != typingNextOffset) return false;
            return !(Character.isWhitespace(ch) && !typingEndsWithWhitespace);
        }
        return offset == typingNextOffset - 1 || offset == typingNextOffset;
    }

    private void track(DocumentEvent event, char ch) {
        typingType = event.getType();
        typingTime = System.currentTimeMillis();
        typingEndsWithWhitespace = Character.isWhitespace(ch);
        typingNextOffset = event.getType() == DocumentEvent.EventType.INSERT ? event.getOffset() + 1 : event.getOffset();
    }

    private Character singleChar(DocumentEvent event) {
        if (event.getLength() != 1) return null;
        if (event.getType() == DocumentEvent.EventType.REMOVE) return '\0';
        if (event.getType() != DocumentEvent.EventType.INSERT) return null;
        try {
            return event.getDocument().getText(event.getOffset(), 1).charAt(0);
        } catch (BadLocationException ex) {
            return null;
        }
    }
}
