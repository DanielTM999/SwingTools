package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;

/** Immutable editor state used while token styles are prepared off the EDT. */
public record TokenRenderSnapshot(String text, TextStyle defaultStyle) {
}
