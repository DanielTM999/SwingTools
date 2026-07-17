package dtm.stools.component.panels.editor.code.gutter;

import java.awt.event.MouseEvent;

public record GutterRightClickEvent(
        int line,
        MouseEvent mouseEvent
) {
}
