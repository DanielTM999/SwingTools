package dtm.stools.component.panels.editor.code.diagnostics;

import java.awt.event.MouseEvent;
import java.util.List;

public record InspectionWidgetClickEvent(
        List<Diagnostic> diagnostics,
        MouseEvent mouseEvent
) {
}
