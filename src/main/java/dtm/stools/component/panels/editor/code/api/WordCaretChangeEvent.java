package dtm.stools.component.panels.editor.code.api;

public record WordCaretChangeEvent(
        String word,
        int line,
        int caretCol,
        int startCol,
        int endCol,
        int caretOffset,
        int startOffset,
        int endOffset,
        int mouseX,
        int mouseY
) {
}
