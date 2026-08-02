package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Barra visual exibida no topo central enquanto uma janela e arrastada. */
public class WindowSnapDragSelector extends JComponent {
    protected WindowPanel window;
    protected Selection selection;
    private Color background = new Color(35, 38, 43, 242);
    private Color border = new Color(95, 100, 108, 215);
    private Color zone = new Color(68, 72, 78, 245);
    private Color zoneBorder = new Color(125, 130, 138, 230);
    private Color selected = new Color(55, 170, 235, 255);

    public WindowSnapDragSelector() {
        setOpaque(false);
        setVisible(false);
        setFocusable(false);
    }

    protected List<List<WindowSnap>> createLayouts() {
        return List.of(
                List.of(WindowSnap.LEFT, WindowSnap.RIGHT),
                List.of(WindowSnap.TWO_THIRDS_LEFT, WindowSnap.THIRD_RIGHT),
                List.of(WindowSnap.THIRD_LEFT, WindowSnap.THIRD_CENTER, WindowSnap.THIRD_RIGHT),
                List.of(WindowSnap.LEFT, WindowSnap.TOP_RIGHT, WindowSnap.BOTTOM_RIGHT),
                List.of(WindowSnap.TOP_LEFT, WindowSnap.TOP_RIGHT,
                        WindowSnap.BOTTOM_LEFT, WindowSnap.BOTTOM_RIGHT),
                List.of(WindowSnap.THIRD_LEFT, WindowSnap.TWO_THIRDS_RIGHT)
        );
    }

    public WindowSnapDragSelector open(WindowPanel value, Rectangle desktopBounds) {
        window = value;
        selection = null;
        int width = Math.max(280, Math.min(570, Math.max(280, desktopBounds.width - 16)));
        int height = 92;
        setBounds(desktopBounds.x + Math.max(0, (desktopBounds.width - width) / 2),
                desktopBounds.y + 8, width, height);
        setVisible(true);
        repaint();
        return this;
    }

    public Selection updateSelection(Point desktopPoint) {
        if (!isVisible() || desktopPoint == null) return null;
        Point local = new Point(desktopPoint.x - getX(), desktopPoint.y - getY());
        Selection nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        List<List<WindowSnap>> layouts = createLayouts();
        for (int layoutIndex = 0; layoutIndex < layouts.size(); layoutIndex++) {
            List<WindowSnap> layout = layouts.get(layoutIndex);
            Rectangle preview = resolveLayoutBounds(layoutIndex, layouts.size());
            for (WindowSnap snap : layout) {
                Rectangle zoneBounds = resolveZoneBounds(snap, preview);
                zoneBounds.grow(-2, -2);
                if (zoneBounds.contains(local)) {
                    setSelection(new Selection(layout, snap, zoneBounds));
                    return selection;
                }
                double distance = distanceSquared(local, zoneBounds);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = new Selection(layout, snap, zoneBounds);
                }
            }
        }
        setSelection(nearest);
        return selection;
    }

    protected void setSelection(Selection value) {
        if (selection != null && value != null
                && selection.snap == value.snap && selection.layout.equals(value.layout)) return;
        selection = value;
        repaint();
    }

    public WindowSnapDragSelector close() {
        setVisible(false);
        window = null;
        selection = null;
        repaint();
        return this;
    }

    protected Rectangle resolveLayoutBounds(int index, int count) {
        int horizontalPadding = 12;
        int gap = 8;
        int available = Math.max(count, getWidth() - horizontalPadding * 2 - gap * (count - 1));
        int width = Math.max(34, available / count);
        return new Rectangle(horizontalPadding + index * (width + gap), 14, width, 64);
    }

    protected Rectangle resolveZoneBounds(WindowSnap snap, Rectangle area) {
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
            case TOP_RIGHT -> new Rectangle(area.x + halfWidth, area.y,
                    area.width - halfWidth, halfHeight);
            case BOTTOM_LEFT -> new Rectangle(area.x, area.y + halfHeight,
                    halfWidth, area.height - halfHeight);
            case BOTTOM_RIGHT -> new Rectangle(area.x + halfWidth, area.y + halfHeight,
                    area.width - halfWidth, area.height - halfHeight);
            case NONE -> new Rectangle(area);
        };
    }

    private static double distanceSquared(Point point, Rectangle rectangle) {
        int x = Math.max(rectangle.x, Math.min(point.x, rectangle.x + rectangle.width));
        int y = Math.max(rectangle.y, Math.min(point.y, rectangle.y + rectangle.height));
        long dx = point.x - x;
        long dy = point.y - y;
        return dx * dx + dy * dy;
    }

    @Override public boolean contains(int x, int y) { return false; }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background);
            g.fillRoundRect(0, 0, Math.max(0, getWidth() - 1),
                    Math.max(0, getHeight() - 1), 16, 16);
            g.setColor(border);
            g.drawRoundRect(0, 0, Math.max(0, getWidth() - 1),
                    Math.max(0, getHeight() - 1), 16, 16);
            List<List<WindowSnap>> layouts = createLayouts();
            for (int index = 0; index < layouts.size(); index++) {
                List<WindowSnap> layout = layouts.get(index);
                Rectangle preview = resolveLayoutBounds(index, layouts.size());
                for (WindowSnap snap : layout) paintZone(g, layout, snap, preview);
            }
        } finally {
            g.dispose();
        }
    }

    protected void paintZone(Graphics2D graphics, List<WindowSnap> layout,
                             WindowSnap snap, Rectangle preview) {
        Rectangle bounds = resolveZoneBounds(snap, preview);
        bounds.grow(-2, -2);
        boolean active = selection != null && selection.snap == snap
                && selection.layout.equals(layout);
        graphics.setColor(active ? selected : zone);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
        graphics.setColor(active ? selected.brighter() : zoneBorder);
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
    }

    public WindowPanel getWindow() { return window; }
    public Selection getSelection() { return selection; }

    public static class Selection {
        private final List<WindowSnap> layout;
        private final WindowSnap snap;
        private final Rectangle previewBounds;

        public Selection(List<WindowSnap> layout, WindowSnap snap, Rectangle previewBounds) {
            this.layout = List.copyOf(layout);
            this.snap = snap;
            this.previewBounds = new Rectangle(previewBounds);
        }

        public List<WindowSnap> getLayout() { return layout; }
        public WindowSnap getSnap() { return snap; }
        public Rectangle getPreviewBounds() { return new Rectangle(previewBounds); }
    }
}
