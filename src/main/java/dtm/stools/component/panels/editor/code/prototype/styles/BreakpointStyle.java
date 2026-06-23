package dtm.stools.component.panels.editor.code.prototype.styles;

import lombok.Builder;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
@Builder
public class BreakpointStyle {

    @Builder.Default
    private final Color color = Color.decode("#E05555");

    @Builder.Default
    private final Icon icon = null;
}
