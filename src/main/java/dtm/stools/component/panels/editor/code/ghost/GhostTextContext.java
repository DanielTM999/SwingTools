package dtm.stools.component.panels.editor.code.ghost;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record GhostTextContext(
        TextBuffer buffer,
        int caretOffset,
        int caretLine,
        int caretCol,
        TriggerKind triggerKind
) {
    public enum TriggerKind {
        EXPLICIT,
        TYPING,
        CARET_IDLE
    }

    public String currentLine() {
        return buffer.lineAt(caretLine);
    }
}
