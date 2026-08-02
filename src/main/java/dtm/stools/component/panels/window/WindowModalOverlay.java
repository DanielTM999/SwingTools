package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

public class WindowModalOverlay extends JPanel {
    private Color overlayColor = new Color(0, 0, 0, 105);

    public WindowModalOverlay() {
        setOpaque(false);
        MouseAdapter blocker = new MouseAdapter() {};
        addMouseListener(blocker);
        addMouseMotionListener(blocker);
        setFocusable(true);
    }

    public WindowModalOverlay overlayColor(Color color) { overlayColor = color; repaint(); return this; }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.setColor(overlayColor);
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }
}
