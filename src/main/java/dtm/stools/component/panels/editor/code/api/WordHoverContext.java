package dtm.stools.component.panels.editor.code.api;

import java.awt.Color;
import java.awt.Font;

public record WordHoverContext(
        int line,
        int startCol,
        int endCol,
        String word,
        int startOffset,
        int endOffset,
        int xStart,
        int xEnd,
        int yTop,
        int lineHeight,
        Font baseFont,
        Color defaultForeground,
        Color defaultBackground,
        WordHoverStyle style
) {
}
