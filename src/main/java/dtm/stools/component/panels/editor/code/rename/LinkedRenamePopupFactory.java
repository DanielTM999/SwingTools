package dtm.stools.component.panels.editor.code.rename;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

@FunctionalInterface
public interface LinkedRenamePopupFactory {

    JComponent createContent(LinkedRenamePopupContext context);

    default Point locate(Rectangle anchorOnScreen, Dimension popupSize) {
        return new Point(anchorOnScreen.x, anchorOnScreen.y + anchorOnScreen.height + 2);
    }

    static LinkedRenamePopupFactory none() {
        return context -> null;
    }
}
