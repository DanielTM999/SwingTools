package dtm.stools.component.panels.editor.code.codelens;

import java.awt.event.MouseEvent;

public record CodeLensClickEvent(
        CodeLens lens,
        CodeLensItem item,
        int line,
        MouseEvent mouseEvent
) {
}
