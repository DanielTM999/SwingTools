package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Destaca no desktop a area real ocupada por um layout antes da selecao. */
public class WindowSnapPreviewOverlay extends JComponent {
    private final List<Rectangle> zones = new ArrayList<>();
    private Rectangle selectedBounds;
    private Color zoneColor = new Color(150, 170, 190, 38);
    private Color zoneBorderColor = new Color(185, 205, 225, 135);
    private Color selectedColor = new Color(45, 145, 225, 105);
    private Color selectedBorderColor = new Color(95, 190, 255, 230);

    public WindowSnapPreviewOverlay() {
        setOpaque(false);
        setVisible(false);
        setFocusable(false);
    }

    public WindowSnapPreviewOverlay preview(List<Rectangle> layoutZones, Rectangle selected) {
        zones.clear();
        if (layoutZones != null) {
            for (Rectangle zone : layoutZones) {
                if (zone != null) zones.add(new Rectangle(zone));
            }
        }
        selectedBounds = selected == null ? null : new Rectangle(selected);
        setVisible(selectedBounds != null);
        repaint();
        return this;
    }

    public WindowSnapPreviewOverlay clearPreview() {
        zones.clear();
        selectedBounds = null;
        setVisible(false);
        repaint();
        return this;
    }

    public WindowSnapPreviewOverlay zoneColor(Color value) { zoneColor = value; repaint(); return this; }
    public WindowSnapPreviewOverlay zoneBorderColor(Color value) { zoneBorderColor = value; repaint(); return this; }
    public WindowSnapPreviewOverlay selectedColor(Color value) { selectedColor = value; repaint(); return this; }
    public WindowSnapPreviewOverlay selectedBorderColor(Color value) { selectedBorderColor = value; repaint(); return this; }

    public Rectangle getSelectedBounds() {
        return selectedBounds == null ? null : new Rectangle(selectedBounds);
    }

    public List<Rectangle> getZoneBounds() {
        return zones.stream().map(Rectangle::new).toList();
    }

    @Override public boolean contains(int x, int y) { return false; }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Rectangle zone : zones) paintZone(g, zone, false);
            if (selectedBounds != null) paintZone(g, selectedBounds, true);
        } finally {
            g.dispose();
        }
    }

    protected void paintZone(Graphics2D graphics, Rectangle bounds, boolean selected) {
        Rectangle painted = new Rectangle(bounds);
        painted.grow(-6, -6);
        if (painted.width <= 0 || painted.height <= 0) return;
        graphics.setColor(selected ? selectedColor : zoneColor);
        graphics.fillRoundRect(painted.x, painted.y, painted.width, painted.height, 12, 12);
        graphics.setColor(selected ? selectedBorderColor : zoneBorderColor);
        graphics.setStroke(new BasicStroke(selected ? 2f : 1f));
        graphics.drawRoundRect(painted.x, painted.y, painted.width, painted.height, 12, 12);
    }
}
