package dtm.stools.component.panels.window;

import java.awt.*;

public interface WindowPlacementPolicy {
    Rectangle resolveInitialBounds(WindowDesktopPanel desktop, WindowPanel window, Rectangle requestedBounds);
}
