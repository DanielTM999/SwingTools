package dtm.stools.component.panels.editor.code.signature;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record SignatureHelpContext(
        TextBuffer buffer,
        int caretOffset,
        int caretLine,
        int caretCol,
        TriggerKind triggerKind,
        char triggerCharacter,
        boolean retrigger,
        SignatureHelp activeSignatureHelp
) {

    public enum TriggerKind {

        INVOKED,

        TRIGGER_CHARACTER,

        CONTENT_CHANGE
    }

    public String currentLine() {
        return buffer.lineAt(caretLine);
    }

    public boolean hasTriggerCharacter() {
        return triggerCharacter != '\0';
    }

    public String textBeforeCaret() {
        String line = currentLine();
        return line.substring(0, Math.min(caretCol, line.length()));
    }
}
