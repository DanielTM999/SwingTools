package dtm.stools.component.icon;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public record VectorIcon(Kind kind, int size, Color color) implements Icon {
    public enum Kind {
        CALENDAR, LEFT, RIGHT, UP, DOWN, CLOSE, MENU, PLUS, MINUS, MORE,
        WARNING, INFO, AUDIO, PLAY, DOT, CHECK, FUNCTION, RANGE
    }

    public VectorIcon(Kind kind, int size) {
        this(kind, size, null);
    }

    public VectorIcon {
        java.util.Objects.requireNonNull(kind);
        if (size < 1) throw new IllegalArgumentException("size must be positive");
    }

    public static JButton button(Kind kind, String description) {
        JButton button = new JButton(new VectorIcon(kind, dtm.stools.configs.UiTokens.scale(16)));
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleName(description);
        button.setActionCommand(description);
        return button;
    }

    public static Icon withDropdown(Icon source) {
        Icon arrow = new VectorIcon(Kind.DOWN, dtm.stools.configs.UiTokens.scale(10));
        if (source == null) return arrow;
        return new Icon() {
            @Override
            public int getIconWidth() { return source.getIconWidth() + arrow.getIconWidth() + 3; }
            @Override
            public int getIconHeight() { return Math.max(source.getIconHeight(), arrow.getIconHeight()); }
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                source.paintIcon(c, g, x, y + (getIconHeight() - source.getIconHeight()) / 2);
                arrow.paintIcon(c, g, x + source.getIconWidth() + 3, y + (getIconHeight() - arrow.getIconHeight()) / 2);
            }
        };
    }

    @Override
    public int getIconWidth() { return size; }

    @Override
    public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(size / 16.0, size / 16.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            Color ink = color != null ? color : c == null ? null : c.getForeground();
            if (c != null && !c.isEnabled()) ink = UIManager.getColor("Label.disabledForeground");
            if (ink == null) ink = UIManager.getColor("Label.foreground");
            g.setColor(ink != null ? ink : Color.GRAY);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (kind) {
                case CALENDAR -> {
                    g.draw(new RoundRectangle2D.Double(2, 3.5, 12, 11, 2, 2));
                    line(g, 2, 6.5, 14, 6.5); line(g, 5, 1.5, 5, 4.5); line(g, 11, 1.5, 11, 4.5);
                    for (int row = 0; row < 2; row++) for (int col = 0; col < 3; col++)
                        g.fill(new Ellipse2D.Double(4 + col * 3, 8.5 + row * 3, 1.4, 1.4));
                }
                case LEFT -> path(g, 10, 3, 5, 8, 10, 13);
                case RIGHT -> path(g, 6, 3, 11, 8, 6, 13);
                case UP -> path(g, 3, 10, 8, 5, 13, 10);
                case DOWN -> path(g, 3, 6, 8, 11, 13, 6);
                case CLOSE -> { line(g, 4, 4, 12, 12); line(g, 12, 4, 4, 12); }
                case MENU -> { for (int y0 = 4; y0 <= 12; y0 += 4) line(g, 2, y0, 14, y0); }
                case PLUS -> { line(g, 3, 8, 13, 8); line(g, 8, 3, 8, 13); }
                case MINUS -> line(g, 3, 8, 13, 8);
                case MORE -> { for (int x0 = 3; x0 <= 13; x0 += 5) g.fill(new Ellipse2D.Double(x0 - 1, 7, 2, 2)); }
                case DOT -> g.fill(new Ellipse2D.Double(5, 5, 6, 6));
                case CHECK -> path(g, 3, 8, 6.5, 11.5, 13, 4.5);
                case WARNING -> {
                    Path2D p = new Path2D.Double(); p.moveTo(8, 1.5); p.lineTo(15, 14); p.lineTo(1, 14); p.closePath(); g.draw(p);
                    line(g, 8, 6, 8, 9); g.fill(new Ellipse2D.Double(7.3, 11, 1.4, 1.4));
                }
                case INFO -> {
                    g.draw(new Ellipse2D.Double(1.5, 1.5, 13, 13));
                    line(g, 8, 7, 8, 11.5); g.fill(new Ellipse2D.Double(7.2, 4, 1.6, 1.6));
                }
                case AUDIO -> {
                    path(g, 6, 11, 6, 3, 13, 1.5, 13, 9.5); line(g, 6, 5.5, 13, 4);
                    g.fill(new Ellipse2D.Double(2, 10, 4, 3)); g.fill(new Ellipse2D.Double(9, 8.5, 4, 3));
                }
                case PLAY -> { Path2D p = new Path2D.Double(); p.moveTo(4, 2); p.lineTo(14, 8); p.lineTo(4, 14); p.closePath(); g.fill(p); }
                case FUNCTION -> IconPaths.draw(g, "fx", 1, 12, 13, 2);
                case RANGE -> { g.draw(new RoundRectangle2D.Double(1.5, 3, 13, 10, 1, 1)); line(g, 1.5, 6, 14.5, 6); line(g, 6, 3, 6, 13); }
            }
        } finally {
            g.dispose();
        }
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }

    private static void path(Graphics2D g, double... points) {
        Path2D p = new Path2D.Double(); p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) p.lineTo(points[i], points[i + 1]);
        g.draw(p);
    }
}
