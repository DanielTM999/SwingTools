package dtm.stools.component.panels.window;

import java.awt.*;

public interface WindowSnapPolicy {
    WindowSnap resolveSnap(WindowDesktopPanel desktop, WindowPanel window, Point location);
    Rectangle resolveBounds(WindowDesktopPanel desktop, WindowPanel window, WindowSnap snap);
}
