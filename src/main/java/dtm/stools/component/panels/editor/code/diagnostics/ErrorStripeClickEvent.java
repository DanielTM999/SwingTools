package dtm.stools.component.panels.editor.code.diagnostics;

import java.awt.event.MouseEvent;

public record ErrorStripeClickEvent(
        Diagnostic diagnostic,
        int line,
        MouseEvent mouseEvent
) {
}
