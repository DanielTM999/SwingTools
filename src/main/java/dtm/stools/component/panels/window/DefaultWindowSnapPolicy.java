package dtm.stools.component.panels.window;

import java.awt.*;

public class DefaultWindowSnapPolicy implements WindowSnapPolicy {
    private int activationDistance = 24;

    public DefaultWindowSnapPolicy activationDistance(int value) {
        activationDistance = Math.max(4, value);
        return this;
    }

    @Override
    public WindowSnap resolveSnap(WindowDesktopPanel desktop, WindowPanel window, Point location) {
        if (location == null) return WindowSnap.NONE;
        int width = desktop.getAvailableDesktopBounds().width;
        int height = desktop.getAvailableDesktopBounds().height;
        boolean left = location.x <= activationDistance;
        boolean right = location.x >= width - activationDistance;
        boolean top = location.y <= activationDistance;
        boolean bottom = location.y >= height - activationDistance;
        if (left && top) return WindowSnap.TOP_LEFT;
        if (right && top) return WindowSnap.TOP_RIGHT;
        if (left && bottom) return WindowSnap.BOTTOM_LEFT;
        if (right && bottom) return WindowSnap.BOTTOM_RIGHT;
        if (left) return WindowSnap.LEFT;
        if (right) return WindowSnap.RIGHT;
        return WindowSnap.NONE;
    }

    @Override
    public Rectangle resolveBounds(WindowDesktopPanel desktop, WindowPanel window, WindowSnap snap) {
        Rectangle area = desktop.getAvailableDesktopBounds();
        int halfWidth = area.width / 2;
        int halfHeight = area.height / 2;
        int thirdWidth = area.width / 3;
        return switch (snap) {
            case LEFT -> new Rectangle(area.x, area.y, halfWidth, area.height);
            case RIGHT -> new Rectangle(area.x + halfWidth, area.y, area.width - halfWidth, area.height);
            case THIRD_LEFT -> new Rectangle(area.x, area.y, thirdWidth, area.height);
            case THIRD_CENTER -> new Rectangle(area.x + thirdWidth, area.y, thirdWidth, area.height);
            case THIRD_RIGHT -> new Rectangle(area.x + thirdWidth * 2, area.y,
                    area.width - thirdWidth * 2, area.height);
            case TWO_THIRDS_LEFT -> new Rectangle(area.x, area.y, thirdWidth * 2, area.height);
            case TWO_THIRDS_RIGHT -> new Rectangle(area.x + thirdWidth, area.y,
                    area.width - thirdWidth, area.height);
            case TOP_LEFT -> new Rectangle(area.x, area.y, halfWidth, halfHeight);
            case TOP_RIGHT -> new Rectangle(area.x + halfWidth, area.y, area.width - halfWidth, halfHeight);
            case BOTTOM_LEFT -> new Rectangle(area.x, area.y + halfHeight, halfWidth, area.height - halfHeight);
            case BOTTOM_RIGHT -> new Rectangle(area.x + halfWidth, area.y + halfHeight,
                    area.width - halfWidth, area.height - halfHeight);
            case NONE -> window.getNormalBounds();
        };
    }
}
