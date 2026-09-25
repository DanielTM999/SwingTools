package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.configs.UiTokens;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class SheetIcon implements Icon {
    public static final Color GREEN = new Color(0x107C41), BLUE = new Color(0x2B78D0), RED = new Color(0xD13438), ORANGE = new Color(0xE3830A), PURPLE = new Color(0x8661C5);

    private final String name;
    private final int size;
    private final Color accent;

    public SheetIcon(String name, int size) { this(name, size, null); }
    public SheetIcon(String name, int size, Color accent) { this.name = name; this.size = size; this.accent = accent; }

    public static SheetIcon small(String name) { return new SheetIcon(name, 16); }
    public static SheetIcon large(String name) { return new SheetIcon(name, 28); }

    public String name() { return name; }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double s = size / 16.0;
            g.scale(s, s);
            boolean enabled = c == null || c.isEnabled();
            Color fg = enabled ? (UiTokens.isDarkTheme() ? new Color(0xE6E6E6) : new Color(0x3B3B3B)) : new Color(0x9E9E9E);
            Color acc = enabled ? (accent != null ? accent : GREEN) : new Color(0xB0B0B0);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(fg);
            draw(g, fg, acc, enabled);
        } finally {
            g.dispose();
        }
    }

    private void text(Graphics2D g, String t, int style, double size, Color color, double cx, double baseline) {
        g.setFont(new Font(Font.SANS_SERIF, style, 1).deriveFont((float) size));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(color);
        g.drawString(t, (float) (cx - fm.stringWidth(t) / 2.0), (float) baseline);
    }

    private void grid(Graphics2D g, Color fg, double x, double y, double w, double h, int rows, int cols) {
        g.setColor(fg);
        g.draw(new Rectangle2D.Double(x, y, w, h));
        for (int r = 1; r < rows; r++) g.draw(new Line2D.Double(x, y + h * r / rows, x + w, y + h * r / rows));
        for (int c = 1; c < cols; c++) g.draw(new Line2D.Double(x + w * c / cols, y, x + w * c / cols, y + h));
    }

    private void lines(Graphics2D g, double x, double y, double[] widths, double gap) {
        for (int k = 0; k < widths.length; k++) g.draw(new Line2D.Double(x, y + k * gap, x + widths[k], y + k * gap));
    }

    private void draw(Graphics2D g, Color fg, Color acc, boolean enabled) {
        switch (name) {
            case "bold" -> text(g, "N", Font.BOLD, 13, fg, 8, 13);
            case "italic" -> text(g, "I", Font.ITALIC | Font.BOLD, 13, fg, 8, 13);
            case "underline" -> { text(g, "S", Font.PLAIN, 12, fg, 8, 11.5); g.draw(new Line2D.Double(3.5, 14, 12.5, 14)); }
            case "strike" -> { text(g, "ab", Font.PLAIN, 10, fg, 8, 12); g.setColor(RED); g.draw(new Line2D.Double(2, 9, 14, 9)); }
            case "font-color" -> { text(g, "A", Font.BOLD, 12, fg, 8, 11); g.setColor(enabled ? RED : acc); g.fill(new Rectangle2D.Double(2, 13, 12, 2.5)); }
            case "fill" -> {
                Path2D p = new Path2D.Double(); p.moveTo(3, 8); p.lineTo(8, 3); p.lineTo(13, 8); p.lineTo(8, 12); p.closePath();
                g.draw(p); g.setColor(enabled ? new Color(0xFFD700) : acc); g.fill(new Rectangle2D.Double(2, 13, 12, 2.5));
            }
            case "borders" -> { grid(g, fg, 2, 2, 12, 12, 2, 2); g.setStroke(new BasicStroke(2f)); g.setColor(fg); g.draw(new Line2D.Double(2, 14, 14, 14)); }
            case "align-left" -> lines(g, 2, 3.5, new double[]{12, 8, 12, 6}, 3);
            case "align-center" -> { lines(g, 2, 3.5, new double[]{12}, 3); lines(g, 4, 6.5, new double[]{8}, 3); lines(g, 2, 9.5, new double[]{12}, 3); lines(g, 5, 12.5, new double[]{6}, 3); }
            case "align-right" -> { g.draw(new Line2D.Double(2, 3.5, 14, 3.5)); g.draw(new Line2D.Double(6, 6.5, 14, 6.5)); g.draw(new Line2D.Double(2, 9.5, 14, 9.5)); g.draw(new Line2D.Double(8, 12.5, 14, 12.5)); }
            case "align-top" -> { g.draw(new Line2D.Double(2, 2.5, 14, 2.5)); g.setColor(acc); g.fill(new Rectangle2D.Double(5, 4, 6, 6)); }
            case "align-middle" -> { g.draw(new Line2D.Double(2, 8, 14, 8)); g.setColor(acc); g.fill(new Rectangle2D.Double(5, 5, 6, 6)); }
            case "align-bottom" -> { g.draw(new Line2D.Double(2, 13.5, 14, 13.5)); g.setColor(acc); g.fill(new Rectangle2D.Double(5, 6, 6, 6)); }
            case "wrap" -> { g.draw(new Line2D.Double(2, 3.5, 14, 3.5)); Path2D p = new Path2D.Double(); p.moveTo(2, 7.5); p.lineTo(12, 7.5); p.quadTo(14.5, 9.5, 12, 11.5); p.lineTo(8, 11.5); g.draw(p); g.draw(new Line2D.Double(8, 11.5, 10, 10)); g.draw(new Line2D.Double(8, 11.5, 10, 13)); g.draw(new Line2D.Double(2, 13.5, 5, 13.5)); }
            case "merge" -> { g.draw(new Rectangle2D.Double(1.5, 4, 13, 8)); g.setColor(acc); g.draw(new Line2D.Double(4, 8, 12, 8)); g.draw(new Line2D.Double(4, 8, 6, 6)); g.draw(new Line2D.Double(4, 8, 6, 10)); g.draw(new Line2D.Double(12, 8, 10, 6)); g.draw(new Line2D.Double(12, 8, 10, 10)); }
            case "indent" -> { lines(g, 7, 3.5, new double[]{7, 7, 7, 7}, 3); g.setColor(acc); g.fill(new java.awt.Polygon(new int[]{2, 5, 2}, new int[]{6, 8, 10}, 3)); }
            case "outdent" -> { lines(g, 7, 3.5, new double[]{7, 7, 7, 7}, 3); g.setColor(acc); g.fill(new java.awt.Polygon(new int[]{5, 2, 5}, new int[]{6, 8, 10}, 3)); }
            case "orientation" -> { text(g, "ab", Font.PLAIN, 9, fg, 7, 12); g.setColor(acc); g.draw(new Line2D.Double(3, 14, 13, 3)); }
            case "currency" -> text(g, "R$", Font.BOLD, 9, fg, 8, 12);
            case "percent" -> text(g, "%", Font.BOLD, 12, fg, 8, 12.5);
            case "comma" -> text(g, "000", Font.BOLD, 8, fg, 8, 11);
            case "dec-inc" -> { text(g, ",0", Font.PLAIN, 8, fg, 8, 7); text(g, ",00", Font.PLAIN, 8, acc, 9, 14.5); }
            case "dec-dec" -> { text(g, ",00", Font.PLAIN, 8, fg, 8, 7); text(g, ",0", Font.PLAIN, 8, acc, 9, 14.5); }
            case "number-format" -> text(g, "123", Font.BOLD, 8, fg, 8, 11);
            case "cond-format" -> { grid(g, fg, 2, 2, 12, 12, 3, 1); g.setColor(RED); g.fill(new Rectangle2D.Double(2.5, 2.5, 11, 3.5)); g.setColor(new Color(0xFFC000)); g.fill(new Rectangle2D.Double(2.5, 6.5, 7, 3.5)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(2.5, 10.5, 4, 3)); }
            case "table" -> { g.setColor(acc); g.fill(new Rectangle2D.Double(2, 2, 12, 3)); grid(g, fg, 2, 2, 12, 12, 4, 3); }
            case "cell-styles" -> { g.setColor(new Color(0xC6EFCE)); g.fill(new Rectangle2D.Double(2, 2, 6, 6)); g.setColor(new Color(0xFFC7CE)); g.fill(new Rectangle2D.Double(8, 2, 6, 6)); g.setColor(new Color(0xFFEB9C)); g.fill(new Rectangle2D.Double(2, 8, 6, 6)); g.setColor(new Color(0xDDEBF7)); g.fill(new Rectangle2D.Double(8, 8, 6, 6)); grid(g, fg, 2, 2, 12, 12, 2, 2); }
            case "insert-cells" -> { grid(g, fg, 2, 5, 12, 9, 2, 2); g.setColor(acc); g.setStroke(new BasicStroke(1.8f)); g.draw(new Line2D.Double(8, 0.5, 8, 4.5)); g.draw(new Line2D.Double(6, 2.5, 10, 2.5)); }
            case "delete-cells" -> { grid(g, fg, 2, 5, 12, 9, 2, 2); g.setColor(RED); g.setStroke(new BasicStroke(1.8f)); g.draw(new Line2D.Double(5.5, 0.5, 10.5, 4.5)); g.draw(new Line2D.Double(10.5, 0.5, 5.5, 4.5)); }
            case "format-cells" -> { grid(g, fg, 2, 2, 12, 12, 2, 2); g.setColor(acc); g.fill(new Rectangle2D.Double(8.5, 8.5, 5, 5)); }
            case "autosum" -> text(g, "Σ", Font.BOLD, 14, fg, 8, 13.5);
            case "fill-down" -> { g.draw(new Rectangle2D.Double(3, 1.5, 10, 4)); g.setColor(acc); g.setStroke(new BasicStroke(1.8f)); g.draw(new Line2D.Double(8, 6, 8, 14)); g.draw(new Line2D.Double(5, 11, 8, 14)); g.draw(new Line2D.Double(11, 11, 8, 14)); }
            case "clear" -> { Path2D p = new Path2D.Double(); p.moveTo(6, 2); p.lineTo(14, 8); p.lineTo(9, 14); p.lineTo(4, 14); p.lineTo(1.5, 10.5); p.closePath(); g.draw(p); g.setColor(new Color(0xF4B6C2)); g.fill(new java.awt.Polygon(new int[]{2, 4, 9, 7}, new int[]{10, 14, 14, 9}, 4)); }
            case "sort-filter" -> { text(g, "A", Font.BOLD, 7, BLUE, 4, 7); text(g, "Z", Font.BOLD, 7, BLUE, 4, 14.5); g.setColor(fg); g.fill(new java.awt.Polygon(new int[]{8, 15, 12, 12, 11, 11}, new int[]{3, 3, 7, 13, 12, 7}, 6)); }
            case "sort-asc" -> { text(g, "A", Font.BOLD, 7, BLUE, 5, 7); text(g, "Z", Font.BOLD, 7, BLUE, 5, 14.5); g.setColor(fg); g.draw(new Line2D.Double(12, 2, 12, 14)); g.draw(new Line2D.Double(10, 12, 12, 14)); g.draw(new Line2D.Double(14, 12, 12, 14)); }
            case "sort-desc" -> { text(g, "Z", Font.BOLD, 7, BLUE, 5, 7); text(g, "A", Font.BOLD, 7, BLUE, 5, 14.5); g.setColor(fg); g.draw(new Line2D.Double(12, 2, 12, 14)); g.draw(new Line2D.Double(10, 12, 12, 14)); g.draw(new Line2D.Double(14, 12, 12, 14)); }
            case "sort" -> { text(g, "AZ", Font.BOLD, 7, BLUE, 6, 7); text(g, "ZA", Font.BOLD, 7, RED, 6, 14.5); g.setColor(fg); g.draw(new Line2D.Double(13.5, 2, 13.5, 14)); }
            case "filter" -> { g.setColor(fg); g.fill(new java.awt.Polygon(new int[]{2, 14, 9, 9, 7, 7}, new int[]{3, 3, 8, 14, 13, 8}, 6)); }
            case "clear-filter" -> { g.fill(new java.awt.Polygon(new int[]{1, 11, 7, 7, 5, 5}, new int[]{2, 2, 6, 11, 10, 6}, 6)); g.setColor(RED); g.setStroke(new BasicStroke(1.6f)); g.draw(new Line2D.Double(10, 10, 15, 15)); g.draw(new Line2D.Double(15, 10, 10, 15)); }
            case "find" -> { g.setStroke(new BasicStroke(1.8f)); g.draw(new Ellipse2D.Double(2, 2, 8, 8)); g.draw(new Line2D.Double(9, 9, 14, 14)); }
            case "replace" -> { text(g, "ab", Font.PLAIN, 7, fg, 5, 7); text(g, "cd", Font.PLAIN, 7, acc, 11, 14.5); g.draw(new Line2D.Double(9, 3, 13, 3)); g.draw(new Line2D.Double(13, 3, 13, 8)); }
            case "pivot" -> { g.setColor(acc); g.fill(new Rectangle2D.Double(2, 2, 12, 3)); g.fill(new Rectangle2D.Double(2, 2, 3, 12)); grid(g, fg, 2, 2, 12, 12, 3, 3); }
            case "chart-column", "chart" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(2, 8, 3, 6)); g.setColor(ORANGE); g.fill(new Rectangle2D.Double(6.5, 3, 3, 11)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(11, 6, 3, 8)); g.setColor(fg); g.draw(new Line2D.Double(1, 14.5, 15, 14.5)); }
            case "chart-bar" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(2, 2, 8, 3)); g.setColor(ORANGE); g.fill(new Rectangle2D.Double(2, 6.5, 12, 3)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(2, 11, 6, 3)); g.setColor(fg); g.draw(new Line2D.Double(1.5, 1, 1.5, 15)); }
            case "chart-line" -> { g.setColor(BLUE); g.setStroke(new BasicStroke(1.8f)); Path2D p = new Path2D.Double(); p.moveTo(1.5, 12); p.lineTo(5.5, 7); p.lineTo(9.5, 10); p.lineTo(14.5, 3); g.draw(p); g.setColor(fg); g.setStroke(new BasicStroke(1f)); g.draw(new Line2D.Double(1, 14.5, 15, 14.5)); }
            case "chart-pie" -> { g.setColor(BLUE); g.fill(new java.awt.geom.Arc2D.Double(1.5, 1.5, 13, 13, 90, 250, java.awt.geom.Arc2D.PIE)); g.setColor(ORANGE); g.fill(new java.awt.geom.Arc2D.Double(2.5, 1, 13, 13, 340, 110, java.awt.geom.Arc2D.PIE)); }
            case "chart-area" -> { g.setColor(BLUE); Path2D p = new Path2D.Double(); p.moveTo(1.5, 14); p.lineTo(1.5, 9); p.lineTo(6, 5); p.lineTo(10, 8); p.lineTo(14.5, 3); p.lineTo(14.5, 14); p.closePath(); g.fill(p); }
            case "chart-scatter" -> { g.setColor(BLUE); for (double[] pt : new double[][]{{3, 11}, {5, 8}, {8, 9}, {9, 5}, {12, 6}, {13, 3}}) g.fill(new Ellipse2D.Double(pt[0] - 1.3, pt[1] - 1.3, 2.6, 2.6)); g.setColor(fg); g.draw(new Line2D.Double(1.5, 1, 1.5, 14.5)); g.draw(new Line2D.Double(1.5, 14.5, 15, 14.5)); }
            case "chart-combo" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(2, 8, 3, 6)); g.fill(new Rectangle2D.Double(6.5, 6, 3, 8)); g.fill(new Rectangle2D.Double(11, 9, 3, 5)); g.setColor(ORANGE); g.setStroke(new BasicStroke(1.6f)); Path2D p = new Path2D.Double(); p.moveTo(2, 6); p.lineTo(8, 2.5); p.lineTo(14, 5); g.draw(p); }
            case "chart-other" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(2, 2, 7, 7)); g.setColor(ORANGE); g.fill(new Rectangle2D.Double(9.5, 2, 4.5, 4)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(9.5, 6.5, 4.5, 2.5)); g.setColor(PURPLE); g.fill(new Rectangle2D.Double(2, 9.5, 12, 4.5)); }
            case "chart-radar" -> { Path2D p = new Path2D.Double(); for (int k = 0; k <= 5; k++) { double a = Math.PI / 2 + 2 * Math.PI * k / 5, r = 6.5; double x = 8 + r * Math.cos(a), y = 8.5 - r * Math.sin(a); if (k == 0) p.moveTo(x, y); else p.lineTo(x, y); } g.draw(p); g.setColor(BLUE); g.fill(new Ellipse2D.Double(5, 5, 6, 7)); }
            case "chart-histogram" -> { g.setColor(BLUE); for (int k = 0; k < 5; k++) { double h = new double[]{4, 8, 12, 7, 3}[k]; g.fill(new Rectangle2D.Double(1.5 + k * 2.7, 14 - h, 2.5, h)); } }
            case "chart-waterfall" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(1.5, 6, 3, 8)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(5, 3, 3, 3)); g.setColor(RED); g.fill(new Rectangle2D.Double(8.5, 3, 3, 5)); g.setColor(BLUE); g.fill(new Rectangle2D.Double(12, 8, 3, 6)); }
            case "chart-treemap" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(1.5, 1.5, 7, 13)); g.setColor(ORANGE); g.fill(new Rectangle2D.Double(9, 1.5, 5.5, 7)); g.setColor(GREEN); g.fill(new Rectangle2D.Double(9, 9, 5.5, 5.5)); }
            case "chart-funnel" -> { g.setColor(BLUE); g.fill(new Rectangle2D.Double(1.5, 2, 13, 3)); g.fill(new Rectangle2D.Double(3.5, 6.5, 9, 3)); g.fill(new Rectangle2D.Double(5.5, 11, 5, 3)); }
            case "image" -> { g.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 2, 2)); g.setColor(acc); Path2D p = new Path2D.Double(); p.moveTo(2, 13); p.lineTo(6, 8); p.lineTo(9, 11); p.lineTo(11, 9); p.lineTo(14, 13); p.closePath(); g.fill(p); g.setColor(new Color(0xFFC000)); g.fill(new Ellipse2D.Double(10, 4, 3, 3)); }
            case "shapes" -> { g.setColor(BLUE); g.fill(new Ellipse2D.Double(1.5, 1.5, 7, 7)); g.setColor(ORANGE); g.fill(new Rectangle2D.Double(7, 7, 7.5, 7.5)); }
            case "textbox" -> { g.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13)); text(g, "A", Font.BOLD, 9, acc, 8, 11.5); }
            case "link" -> { g.setStroke(new BasicStroke(1.6f)); g.draw(new RoundRectangle2D.Double(1.5, 5.5, 7, 5, 5, 5)); g.draw(new RoundRectangle2D.Double(7.5, 5.5, 7, 5, 5, 5)); }
            case "note" -> { g.setColor(new Color(0xFFF2B3)); g.fill(new Rectangle2D.Double(2, 2, 12, 12)); g.setColor(fg); g.draw(new Rectangle2D.Double(2, 2, 12, 12)); lines(g, 4, 5.5, new double[]{8, 8, 5}, 3); }
            case "comment" -> { Path2D p = new Path2D.Double(); p.moveTo(2, 2.5); p.lineTo(14, 2.5); p.lineTo(14, 10.5); p.lineTo(7, 10.5); p.lineTo(4, 13.5); p.lineTo(4, 10.5); p.lineTo(2, 10.5); p.closePath(); g.setColor(acc); g.fill(p); }
            case "sparkline" -> { grid(g, fg, 1.5, 3, 13, 10, 1, 1); g.setColor(BLUE); Path2D p = new Path2D.Double(); p.moveTo(3, 10); p.lineTo(5.5, 6); p.lineTo(8, 8.5); p.lineTo(10.5, 5); p.lineTo(13, 7); g.draw(p); }
            case "slicer" -> { g.draw(new Rectangle2D.Double(2, 1.5, 12, 13)); g.setColor(acc); g.fill(new Rectangle2D.Double(4, 4, 8, 2.5)); g.setColor(new Color(0xBFBFBF)); g.fill(new Rectangle2D.Double(4, 8, 8, 2.5)); }
            case "function" -> text(g, "fx", Font.ITALIC | Font.BOLD, 11, fg, 8, 12);
            case "names" -> { g.draw(new RoundRectangle2D.Double(1.5, 4, 13, 8, 3, 3)); text(g, "abc", Font.PLAIN, 6.5, fg, 8, 10); }
            case "trace-precedents" -> { g.setColor(BLUE); g.fill(new Ellipse2D.Double(1.5, 5.5, 4, 4)); g.draw(new Line2D.Double(5, 7.5, 12, 7.5)); g.fill(new java.awt.Polygon(new int[]{11, 15, 11}, new int[]{5, 7, 10}, 3)); g.setColor(fg); g.draw(new Rectangle2D.Double(11.5, 3, 3, 3)); }
            case "trace-dependents" -> { g.setColor(fg); g.draw(new Rectangle2D.Double(1.5, 5, 3.5, 4)); g.setColor(BLUE); g.draw(new Line2D.Double(5, 7.5, 12, 7.5)); g.fill(new java.awt.Polygon(new int[]{11, 15, 11}, new int[]{5, 7, 10}, 3)); }
            case "remove-arrows" -> { g.setColor(BLUE); g.draw(new Line2D.Double(2, 7, 11, 7)); g.setColor(RED); g.setStroke(new BasicStroke(1.8f)); g.draw(new Line2D.Double(9, 9, 14, 14)); g.draw(new Line2D.Double(14, 9, 9, 14)); }
            case "show-formulas" -> { grid(g, fg, 1.5, 1.5, 13, 13, 2, 1); text(g, "fx", Font.ITALIC, 7, acc, 8, 7); }
            case "error-check" -> { g.setColor(new Color(0xFFC000)); g.fill(new java.awt.Polygon(new int[]{8, 15, 1}, new int[]{1, 14, 14}, 3)); text(g, "!", Font.BOLD, 9, Color.BLACK, 8, 13); }
            case "evaluate" -> { text(g, "(fx)", Font.PLAIN, 7.5, fg, 8, 7); g.setColor(acc); g.draw(new Line2D.Double(4, 11, 12, 11)); g.draw(new Line2D.Double(10, 9, 12, 11)); g.draw(new Line2D.Double(10, 13, 12, 11)); }
            case "watch" -> { g.draw(new Ellipse2D.Double(1.5, 5, 5.5, 5.5)); g.draw(new Ellipse2D.Double(9, 5, 5.5, 5.5)); g.draw(new Line2D.Double(7, 7, 9, 7)); }
            case "calc-now" -> { g.setColor(acc); g.setStroke(new BasicStroke(1.6f)); g.draw(new java.awt.geom.Arc2D.Double(2, 2, 12, 12, 30, 280, java.awt.geom.Arc2D.OPEN)); g.fill(new java.awt.Polygon(new int[]{13, 15, 11}, new int[]{2, 6, 6}, 3)); }
            case "calc-options" -> { grid(g, fg, 2, 2, 12, 12, 2, 2); text(g, "=", Font.BOLD, 9, acc, 8, 11); }
            case "text-to-columns" -> { g.draw(new Rectangle2D.Double(1.5, 4, 6, 8)); g.draw(new Rectangle2D.Double(9, 4, 5.5, 8)); g.setColor(acc); g.draw(new Line2D.Double(8, 2, 8, 14)); }
            case "remove-duplicates" -> { grid(g, fg, 2, 2, 12, 12, 3, 1); g.setColor(RED); g.setStroke(new BasicStroke(1.6f)); g.draw(new Line2D.Double(9, 9, 14, 14)); g.draw(new Line2D.Double(14, 9, 9, 14)); }
            case "validation" -> { g.draw(new Rectangle2D.Double(1.5, 3, 13, 10)); g.setColor(GREEN); g.setStroke(new BasicStroke(1.8f)); Path2D p = new Path2D.Double(); p.moveTo(4, 8); p.lineTo(7, 11); p.lineTo(12, 5); g.draw(p); }
            case "what-if" -> { text(g, "?", Font.BOLD, 13, acc, 8, 13); }
            case "group" -> { g.draw(new Line2D.Double(3, 2, 3, 14)); g.draw(new Line2D.Double(3, 2, 6, 2)); g.draw(new Line2D.Double(3, 14, 6, 14)); lines(g, 7, 5, new double[]{7, 7, 7}, 3); text(g, "+", Font.BOLD, 8, acc, 3, 10); }
            case "ungroup" -> { lines(g, 7, 5, new double[]{7, 7, 7}, 3); text(g, "−", Font.BOLD, 10, RED, 3, 11); }
            case "subtotal" -> { grid(g, fg, 2, 2, 12, 12, 3, 1); text(g, "Σ", Font.BOLD, 7, acc, 11, 14); }
            case "flash-fill" -> { g.setColor(new Color(0xFFC000)); g.fill(new java.awt.Polygon(new int[]{9, 3, 8, 6, 13, 8}, new int[]{1, 9, 9, 15, 6, 6}, 6)); }
            case "consolidate" -> { g.draw(new Rectangle2D.Double(1.5, 1.5, 5, 5)); g.draw(new Rectangle2D.Double(1.5, 9.5, 5, 5)); g.setColor(acc); g.fill(new Rectangle2D.Double(10, 5.5, 5, 5)); g.draw(new Line2D.Double(6.5, 4, 10, 8)); g.draw(new Line2D.Double(6.5, 12, 10, 8)); }
            case "forecast" -> { g.setColor(BLUE); Path2D p = new Path2D.Double(); p.moveTo(1.5, 12); p.lineTo(5, 9); p.lineTo(8, 10); g.draw(p); g.setColor(ORANGE); g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 1.5f}, 0)); g.draw(new Line2D.Double(8, 10, 14.5, 4)); }
            case "spelling" -> { text(g, "ABC", Font.BOLD, 6.5, fg, 8, 7); g.setColor(GREEN); g.setStroke(new BasicStroke(1.6f)); Path2D p = new Path2D.Double(); p.moveTo(4, 11); p.lineTo(7, 14); p.lineTo(13, 8); g.draw(p); }
            case "protect" -> { g.draw(new java.awt.geom.Arc2D.Double(4.5, 1.5, 7, 8, 0, 180, java.awt.geom.Arc2D.OPEN)); g.draw(new Line2D.Double(4.5, 5.5, 4.5, 7)); g.draw(new Line2D.Double(11.5, 5.5, 11.5, 7)); g.setColor(new Color(0xFFC000)); g.fill(new RoundRectangle2D.Double(3, 7, 10, 7.5, 2, 2)); }
            case "freeze" -> { grid(g, fg, 1.5, 1.5, 13, 13, 3, 3); g.setColor(BLUE); g.setStroke(new BasicStroke(2f)); g.draw(new Line2D.Double(1.5, 6, 14.5, 6)); g.draw(new Line2D.Double(6, 1.5, 6, 14.5)); }
            case "zoom" -> { g.setStroke(new BasicStroke(1.6f)); g.draw(new Ellipse2D.Double(2, 2, 8.5, 8.5)); g.draw(new Line2D.Double(9.5, 9.5, 14, 14)); text(g, "+", Font.BOLD, 8, fg, 6.2, 9.2); }
            case "zoom-100" -> text(g, "100", Font.BOLD, 7, fg, 8, 11);
            case "zoom-selection" -> { g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 1.5f}, 0)); g.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13)); g.setStroke(new BasicStroke(1.6f)); g.draw(new Ellipse2D.Double(4, 4, 6, 6)); g.draw(new Line2D.Double(9, 9, 12.5, 12.5)); }
            case "gridlines" -> grid(g, fg, 1.5, 1.5, 13, 13, 3, 3);
            case "headers" -> { g.setColor(new Color(0xD0D0D0)); g.fill(new Rectangle2D.Double(1.5, 1.5, 13, 3.5)); g.fill(new Rectangle2D.Double(1.5, 1.5, 3.5, 13)); grid(g, fg, 1.5, 1.5, 13, 13, 1, 1); }
            case "page-layout" -> { g.draw(new Rectangle2D.Double(3, 1.5, 10, 13)); lines(g, 5, 5, new double[]{6, 6, 4}, 3); }
            case "page-break" -> { g.draw(new Rectangle2D.Double(3, 1.5, 10, 5)); g.draw(new Rectangle2D.Double(3, 9.5, 10, 5)); g.setColor(BLUE); g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 1.5f}, 0)); g.draw(new Line2D.Double(1, 8, 15, 8)); }
            case "normal-view" -> grid(g, fg, 1.5, 3, 13, 10, 3, 3);
            case "margins" -> { g.draw(new Rectangle2D.Double(2.5, 1.5, 11, 13)); g.setColor(BLUE); g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1.5f, 1.5f}, 0)); g.draw(new Rectangle2D.Double(4.5, 3.5, 7, 9)); }
            case "page-orientation" -> { g.draw(new Rectangle2D.Double(1.5, 4, 9, 7)); g.draw(new Rectangle2D.Double(9, 1.5, 5.5, 10)); }
            case "page-size" -> { g.draw(new Rectangle2D.Double(3, 1.5, 9, 13)); g.draw(new Rectangle2D.Double(6, 5.5, 8.5, 9)); }
            case "print-area" -> { g.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13)); g.setColor(BLUE); g.fill(new Rectangle2D.Double(4, 4, 8, 8)); }
            case "print-titles" -> { grid(g, fg, 1.5, 1.5, 13, 13, 3, 1); g.setColor(BLUE); g.fill(new Rectangle2D.Double(2, 2, 12, 3.8)); }
            case "print", "pdf" -> { g.draw(new Rectangle2D.Double(4, 1.5, 8, 5)); g.draw(new RoundRectangle2D.Double(1.5, 6, 13, 6, 2, 2)); g.setColor(Color.WHITE); g.fill(new Rectangle2D.Double(4, 10, 8, 4.5)); g.setColor(fg); g.draw(new Rectangle2D.Double(4, 10, 8, 4.5)); if (name.equals("pdf")) text(g, "PDF", Font.BOLD, 4.5, RED, 8, 14); }
            case "undo" -> { g.setStroke(new BasicStroke(1.6f)); Path2D p = new Path2D.Double(); p.moveTo(4, 5); p.lineTo(10, 5); p.quadTo(14.5, 5, 14.5, 9.5); p.quadTo(14.5, 14, 10, 14); p.lineTo(6, 14); g.draw(p); g.fill(new java.awt.Polygon(new int[]{1, 5, 5}, new int[]{5, 2, 8}, 3)); }
            case "redo" -> { g.setStroke(new BasicStroke(1.6f)); Path2D p = new Path2D.Double(); p.moveTo(12, 5); p.lineTo(6, 5); p.quadTo(1.5, 5, 1.5, 9.5); p.quadTo(1.5, 14, 6, 14); p.lineTo(10, 14); g.draw(p); g.fill(new java.awt.Polygon(new int[]{15, 11, 11}, new int[]{5, 2, 8}, 3)); }
            case "save" -> { g.draw(new RoundRectangle2D.Double(1.5, 1.5, 13, 13, 2, 2)); g.setColor(acc); g.fill(new Rectangle2D.Double(4, 1.5, 8, 5)); g.setColor(fg); g.draw(new Rectangle2D.Double(4, 9, 8, 5.5)); }
            case "open" -> { Path2D p = new Path2D.Double(); p.moveTo(1.5, 3); p.lineTo(6, 3); p.lineTo(7.5, 4.5); p.lineTo(13, 4.5); p.lineTo(13, 13.5); p.lineTo(1.5, 13.5); p.closePath(); g.setColor(new Color(0xFFC857)); g.fill(p); g.setColor(fg); g.draw(p); }
            case "new" -> { Path2D p = new Path2D.Double(); p.moveTo(3, 1.5); p.lineTo(10, 1.5); p.lineTo(13, 4.5); p.lineTo(13, 14.5); p.lineTo(3, 14.5); p.closePath(); g.draw(p); g.setColor(acc); g.fill(new Rectangle2D.Double(5, 7, 6, 1.5)); g.fill(new Rectangle2D.Double(7.25, 4.75, 1.5, 6)); }
            case "paste" -> { g.setColor(new Color(0xC19A6B)); g.fill(new RoundRectangle2D.Double(2, 3, 10, 12, 2, 2)); g.setColor(fg); g.draw(new RoundRectangle2D.Double(2, 3, 10, 12, 2, 2)); g.setColor(Color.WHITE); g.fill(new Rectangle2D.Double(7, 7, 7.5, 8)); g.setColor(fg); g.draw(new Rectangle2D.Double(7, 7, 7.5, 8)); g.fill(new Rectangle2D.Double(4.5, 1.5, 5, 2.5)); }
            case "cut" -> { g.draw(new Ellipse2D.Double(2, 10, 4, 4)); g.draw(new Ellipse2D.Double(10, 10, 4, 4)); g.draw(new Line2D.Double(5, 10.5, 11.5, 2)); g.draw(new Line2D.Double(11, 10.5, 4.5, 2)); }
            case "copy" -> { g.draw(new RoundRectangle2D.Double(1.5, 1.5, 8.5, 10, 1.5, 1.5)); g.setColor(UiTokens.isDarkTheme() ? new Color(0x2B2B2B) : Color.WHITE); g.fill(new RoundRectangle2D.Double(5.5, 4.5, 9, 10, 1.5, 1.5)); g.setColor(fg); g.draw(new RoundRectangle2D.Double(5.5, 4.5, 9, 10, 1.5, 1.5)); }
            case "format-painter" -> { g.setColor(new Color(0xFFC000)); g.fill(new RoundRectangle2D.Double(2, 2, 11, 5, 2, 2)); g.setColor(fg); g.draw(new RoundRectangle2D.Double(2, 2, 11, 5, 2, 2)); g.draw(new Line2D.Double(13, 4.5, 14.5, 4.5)); g.draw(new Line2D.Double(14.5, 4.5, 14.5, 8.5)); g.draw(new Line2D.Double(14.5, 8.5, 8, 8.5)); g.fill(new Rectangle2D.Double(7, 8.5, 2, 6)); }
            case "checkbox" -> { g.setColor(GREEN); g.fill(new RoundRectangle2D.Double(2, 2, 12, 12, 3, 3)); g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.8f)); Path2D p = new Path2D.Double(); p.moveTo(4.5, 8); p.lineTo(7, 10.5); p.lineTo(11.5, 5.5); g.draw(p); }
            case "dropdown" -> { g.draw(new Rectangle2D.Double(1.5, 4, 13, 8)); g.fill(new java.awt.Polygon(new int[]{9, 13, 11}, new int[]{7, 7, 10}, 3)); }
            case "ai" -> { g.setColor(PURPLE); Path2D p = new Path2D.Double(); p.moveTo(8, 1); p.quadTo(9, 7, 15, 8); p.quadTo(9, 9, 8, 15); p.quadTo(7, 9, 1, 8); p.quadTo(7, 7, 8, 1); g.fill(p); }
            case "palette" -> { g.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 3, 3)); text(g, ">_", Font.BOLD, 7, acc, 7, 10.5); }
            case "sheet" -> { grid(g, fg, 1.5, 1.5, 13, 13, 3, 3); g.setColor(acc); g.fill(new Rectangle2D.Double(1.5, 1.5, 13, 4.3)); }
            case "sheet-add" -> { text(g, "+", Font.BOLD, 14, fg, 8, 13); }
            case "more" -> { for (int k = 0; k < 3; k++) g.fill(new Ellipse2D.Double(2.5 + k * 4.5, 7, 2.2, 2.2)); }
            case "close" -> { g.setStroke(new BasicStroke(1.5f)); g.draw(new Line2D.Double(3.5, 3.5, 12.5, 12.5)); g.draw(new Line2D.Double(12.5, 3.5, 3.5, 12.5)); }
            case "check" -> { g.setColor(GREEN); g.setStroke(new BasicStroke(1.8f)); Path2D p = new Path2D.Double(); p.moveTo(3, 8.5); p.lineTo(6.5, 12); p.lineTo(13, 4.5); g.draw(p); }
            case "cancel" -> { g.setColor(RED); g.setStroke(new BasicStroke(1.8f)); g.draw(new Line2D.Double(3.5, 3.5, 12.5, 12.5)); g.draw(new Line2D.Double(12.5, 3.5, 3.5, 12.5)); }
            case "expand" -> { g.fill(new java.awt.Polygon(new int[]{4, 12, 8}, new int[]{6, 6, 11}, 3)); }
            case "collapse" -> { g.fill(new java.awt.Polygon(new int[]{4, 12, 8}, new int[]{10, 10, 5}, 3)); }
            case "hide" -> { g.draw(new java.awt.geom.Arc2D.Double(1.5, 4, 13, 8, 0, 180, java.awt.geom.Arc2D.OPEN)); g.draw(new java.awt.geom.Arc2D.Double(1.5, 4, 13, 8, 180, 180, java.awt.geom.Arc2D.OPEN)); g.setColor(RED); g.draw(new Line2D.Double(2, 14, 14, 2)); }
            case "data" -> { g.draw(new Ellipse2D.Double(2.5, 1.5, 11, 4)); g.draw(new Line2D.Double(2.5, 3.5, 2.5, 12.5)); g.draw(new Line2D.Double(13.5, 3.5, 13.5, 12.5)); g.draw(new java.awt.geom.Arc2D.Double(2.5, 10.5, 11, 4, 180, 180, java.awt.geom.Arc2D.OPEN)); }
            case "refresh" -> { g.setColor(acc); g.setStroke(new BasicStroke(1.6f)); g.draw(new java.awt.geom.Arc2D.Double(2, 2, 12, 12, 60, 270, java.awt.geom.Arc2D.OPEN)); g.fill(new java.awt.Polygon(new int[]{11, 15, 14}, new int[]{1, 3, 7}, 3)); }
            default -> {
                String t = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                g.draw(new RoundRectangle2D.Double(1.5, 1.5, 13, 13, 3, 3));
                text(g, t, Font.BOLD, 9, acc, 8, 11.5);
            }
        }
    }
}
