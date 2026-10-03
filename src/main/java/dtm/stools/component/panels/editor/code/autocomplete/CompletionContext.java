package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record CompletionContext(TextBuffer buffer, int caretOffset, int caretLine, int caretCol, String prefix, int prefixOffset, TriggerKind triggerKind, long documentVersion) {
    public enum TriggerKind {
        EXPLICIT,
        TYPING
    }

    public CompletionContext(TextBuffer buffer, int caretOffset, int caretLine, int caretCol, String prefix, int prefixOffset, TriggerKind triggerKind) {
        this(buffer, caretOffset, caretLine, caretCol, prefix, prefixOffset, triggerKind, -1);
    }

    public String currentLine() {
        return buffer.lineAt(caretLine);
    }
}
