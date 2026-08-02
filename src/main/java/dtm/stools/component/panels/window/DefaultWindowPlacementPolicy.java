package dtm.stools.component.panels.window;

import java.awt.*;

public class DefaultWindowPlacementPolicy implements WindowPlacementPolicy {
    private int cascadeOffset = 28;

    public DefaultWindowPlacementPolicy cascadeOffset(int value) {
        cascadeOffset = Math.max(0, value);
        return this;
    }

    @Override
    public Rectangle resolveInitialBounds(WindowDesktopPanel desktop, WindowPanel window, Rectangle requestedBounds) {
        if (requestedBounds != null) return new Rectangle(requestedBounds);
        Rectangle area = desktop.getAvailableDesktopBounds();
        Dimension preferred = window.getPreferredSize();
        int width = Math.min(area.width, Math.max(window.getMinimumSize().width,
                preferred.width > 0 ? preferred.width : 520));
        int height = Math.min(area.height, Math.max(window.getMinimumSize().height,
                preferred.height > 0 ? preferred.height : 340));
        int index = desktop.getWindows().size();
        int rangeX = Math.max(1, area.width - width);
        int rangeY = Math.max(1, area.height - height);
        int x = area.x + (index * cascadeOffset) % rangeX;
        int y = area.y + (index * cascadeOffset) % rangeY;
        return new Rectangle(x, y, width, height);
    }
}
