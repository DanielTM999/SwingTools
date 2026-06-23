package dtm.stools.component.panels.editor.code.inlay;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

public record InlayHintContext(TextBuffer buffer, int firstLine, int lastLine) {
}
