package dtm.stools.component.panels.tab;

import javax.swing.*;
import java.awt.event.MouseEvent;

@FunctionalInterface
public interface TabMenuProvider {
    JPopupMenu createMenu(TabbedPanel tabs, TabEntry entry, MouseEvent event);
}
