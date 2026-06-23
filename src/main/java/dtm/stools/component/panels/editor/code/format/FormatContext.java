package dtm.stools.component.panels.editor.code.format;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record FormatContext(
        TextBuffer buffer,
        int startOffset,
        int endOffset,
        int tabSize,
        boolean useSpacesForTab,
        Scope scope
) {
    public enum Scope { DOCUMENT, SELECTION }

    public String text() {
        return buffer.substring(startOffset, endOffset);
    }
}
