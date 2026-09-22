package dtm.stools.component.panels.editor.code.documenthighlight;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record DocumentHighlightContext(
        TextBuffer buffer,
        int line,
        int col,
        int offset
) {
}
