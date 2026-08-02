package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;

public class WindowControlButton extends JButton {
    private final WindowControl control;
    private WindowStyle style;
    private boolean restoreGlyph;

    public WindowControlButton(WindowControl control, WindowStyle style) {
        this.control = control;
        this.style = style;
        setFocusable(false);
        setCursor(Cursor.getDefaultCursor());
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setToolTipText(defaultTooltip(control));
    }

    public WindowControl getControl() { return control; }

    public void setWindowStyle(WindowStyle style) {
        this.style = style;
        repaint();
    }

    public void setRestoreGlyph(boolean restoreGlyph) {
        this.restoreGlyph = restoreGlyph;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || getModel().isPressed()) {
                Color hover = control == WindowControl.CLOSE
                        ? style.getCloseHoverBackground() : style.getControlHoverBackground();
                if (hover != null) {
                    g.setColor(hover);
                    g.fillRoundRect(2, 3, Math.max(0, getWidth() - 4), Math.max(0, getHeight() - 6), 8, 8);
                }
            }
            paintControl(g);
        } finally {
            g.dispose();
        }
    }

    protected void paintControl(Graphics2D g) {
        g.setColor(isEnabled() ? style.getForeground() : style.getInactiveForeground());
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        switch (control) {
            case MINIMIZE -> g.drawLine(cx - 5, cy + 3, cx + 5, cy + 3);
            case MAXIMIZE_RESTORE -> {
                if (restoreGlyph) {
                    g.drawRect(cx - 3, cy - 5, 8, 7);
                    g.drawRect(cx - 5, cy - 3, 8, 7);
                } else {
                    g.drawRect(cx - 5, cy - 5, 10, 9);
                }
            }
            case CLOSE -> {
                g.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                g.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
            }
        }
    }

    private static String defaultTooltip(WindowControl control) {
        return switch (control) {
            case MINIMIZE -> "Minimizar";
            case MAXIMIZE_RESTORE -> "Maximizar / restaurar";
            case CLOSE -> "Fechar";
        };
    }
}
