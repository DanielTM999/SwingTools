package dtm.stools.component.panels.editor.code.format;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record FormatContext(
        TextBuffer buffer,
        int startOffset,
        int endOffset,
        int tabSize,
        boolean useSpacesForTab,
        Scope scope,
        int indentSize
) {

    public FormatContext(TextBuffer buffer, int startOffset, int endOffset, int tabSize, boolean useSpacesForTab, Scope scope) {
        this(buffer, startOffset, endOffset, tabSize, useSpacesForTab, scope, tabSize);
    }

    public enum Scope { DOCUMENT, SELECTION }

    public String text() {
        return buffer.substring(startOffset, endOffset);
    }
}
