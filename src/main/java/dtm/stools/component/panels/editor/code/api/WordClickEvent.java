package dtm.stools.component.panels.editor.code.api;

import java.awt.event.MouseEvent;

public record WordClickEvent(
        String word,
        int line,
        int col,
        int startOffset,
        int endOffset,
        MouseEvent mouseEvent
) {
}
