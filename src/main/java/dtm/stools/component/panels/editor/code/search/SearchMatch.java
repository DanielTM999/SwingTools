package dtm.stools.component.panels.editor.code.search;

public record SearchMatch(int startOffset, int endOffset) {
    public int length() {
        return endOffset - startOffset;
    }
}
