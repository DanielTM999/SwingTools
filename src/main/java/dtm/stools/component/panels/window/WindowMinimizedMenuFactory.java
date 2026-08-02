package dtm.stools.component.panels.window;

import javax.swing.*;

@FunctionalInterface
public interface WindowMinimizedMenuFactory {
    JPopupMenu createMenu(WindowDesktopPanel desktop, WindowPanel window);
}
