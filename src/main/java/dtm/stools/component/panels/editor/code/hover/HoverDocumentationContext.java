package dtm.stools.component.panels.editor.code.hover;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record HoverDocumentationContext(
        TextBuffer buffer,
        int line,
        int col,
        int offset
) {
}
