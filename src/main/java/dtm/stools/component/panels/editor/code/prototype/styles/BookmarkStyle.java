package dtm.stools.component.panels.editor.code.prototype.styles;

import lombok.Builder;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
@Builder
public class BookmarkStyle {

    @Builder.Default
    private final Color color = Color.decode("#4C8DFF");

    @Builder.Default
    private final Icon icon = null;
}
