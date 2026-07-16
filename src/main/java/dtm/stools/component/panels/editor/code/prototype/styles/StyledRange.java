package dtm.stools.component.panels.editor.code.prototype.styles;

import lombok.Getter;

@Getter
public class StyledRange {

    private final TextStyle style;
    private final int startOffset;
    private final int endOffset;

    public StyledRange(TextStyle style, int startOffset, int endOffset) {
        this.style = style;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }
}