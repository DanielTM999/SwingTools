package dtm.stools.component.panels.tab;

import java.awt.*;

@FunctionalInterface
public interface TabWindowFactory {
    Window createWindow(TabbedPanel source, TabWindowRequest request);
}
