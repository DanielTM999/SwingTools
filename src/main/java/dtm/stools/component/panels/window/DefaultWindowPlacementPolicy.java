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
        Rectangle area = desktop.getAvailableDesktopBounds();
        Dimension preferred = window.getPreferredSize();
        int requestedWidth = requestedBounds == null ? 0 : requestedBounds.width;
        int requestedHeight = requestedBounds == null ? 0 : requestedBounds.height;
        int width = Math.min(area.width, Math.max(window.getMinimumSize().width,
                requestedWidth > 0 ? requestedWidth : preferred.width > 0 ? preferred.width : 520));
        int height = Math.min(area.height, Math.max(window.getMinimumSize().height,
                requestedHeight > 0 ? requestedHeight : preferred.height > 0 ? preferred.height : 340));
        WindowPosition position = window.getInitialPosition();
        if (position == WindowPosition.CASCADE) {
            if (requestedBounds != null) return new Rectangle(requestedBounds);
            return cascade(desktop, area, width, height);
        }
        return align(area, width, height, position);
    }

    protected Rectangle cascade(WindowDesktopPanel desktop, Rectangle area, int width, int height) {
        int index = desktop.getWindows().size();
        int rangeX = Math.max(1, area.width - width);
        int rangeY = Math.max(1, area.height - height);
        int x = area.x + (index * cascadeOffset) % rangeX;
        int y = area.y + (index * cascadeOffset) % rangeY;
        return new Rectangle(x, y, width, height);
    }

    protected Rectangle align(Rectangle area, int width, int height, WindowPosition position) {
        int centerX = area.x + (area.width - width) / 2;
        int centerY = area.y + (area.height - height) / 2;
        int right = area.x + area.width - width;
        int bottom = area.y + area.height - height;
        return switch (position) {
            case CENTER -> new Rectangle(centerX, centerY, width, height);
            case LEFT -> new Rectangle(area.x, centerY, width, height);
            case RIGHT -> new Rectangle(right, centerY, width, height);
            case TOP -> new Rectangle(centerX, area.y, width, height);
            case BOTTOM -> new Rectangle(centerX, bottom, width, height);
            case TOP_LEFT -> new Rectangle(area.x, area.y, width, height);
            case TOP_RIGHT -> new Rectangle(right, area.y, width, height);
            case BOTTOM_LEFT -> new Rectangle(area.x, bottom, width, height);
            case BOTTOM_RIGHT -> new Rectangle(right, bottom, width, height);
            case CASCADE -> throw new IllegalArgumentException("CASCADE must be resolved separately");
        };
    }
}
