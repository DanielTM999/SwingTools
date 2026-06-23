package dtm.stools.component.panels.editor.code.prototype.styles;

import lombok.Builder;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
@Builder
public class TextStyle {

    @Builder.Default
    private final Color foreground = UIManager.getColor("TextArea.foreground");

    @Builder.Default
    private final Color background = UIManager.getColor("TextArea.background");

    private final boolean bold;

    private final boolean italic;

    private final boolean underline;
}
