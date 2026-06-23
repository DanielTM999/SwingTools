package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record CompletionContext(
        TextBuffer buffer,
        int caretOffset,
        int caretLine,
        int caretCol,
        String prefix,
        int prefixOffset,
        TriggerKind triggerKind
) {
    public enum TriggerKind {
        EXPLICIT,
        TYPING
    }

    public String currentLine() {
        return buffer.lineAt(caretLine);
    }
}
