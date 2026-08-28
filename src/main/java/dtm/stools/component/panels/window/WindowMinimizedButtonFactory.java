package dtm.stools.component.panels.window;

import javax.swing.*;

@FunctionalInterface
public interface WindowMinimizedButtonFactory {
    AbstractButton createButton(WindowMinimizedBar bar, WindowPanel window);
}
