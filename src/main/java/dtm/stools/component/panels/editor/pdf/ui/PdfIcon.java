package dtm.stools.component.panels.editor.pdf.ui;

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
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class PdfIcon implements Icon {
    public static final Color RED = new Color(0xC8323C), BLUE = new Color(0x2B78D0), GREEN = new Color(0x2E8B57),
            ORANGE = new Color(0xE3830A), YELLOW = new Color(0xF2C230), PURPLE = new Color(0x7E5BC2);

    private final String name;
    private final int size;
    private final Color swatch;

    public PdfIcon(String name, int size) { this(name, size, null); }
    public PdfIcon(String name, int size, Color swatch) { this.name = name == null ? "" : name; this.size = size; this.swatch = swatch; }

    public static PdfIcon small(String name) { return new PdfIcon(name, 16); }
    public static PdfIcon large(String name) { return new PdfIcon(name, 28); }

    public String name() { return name; }
    @Override
    public int getIconWidth() { return size; }
    @Override
    public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component component, Graphics original, int x, int y) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(size / 16.0, size / 16.0);
            boolean enabled = component == null || component.isEnabled();
            Color fg = enabled ? (UiTokens.isDarkTheme() ? new Color(0xE4E6EA) : new Color(0x3A3D42)) : new Color(0x9E9E9E);
            Color accent = enabled ? RED : new Color(0xB0B0B0);
            Color blue = enabled ? BLUE : new Color(0xB0B0B0);
            g.setStroke(stroke(1.2f));
            g.setColor(fg);
            draw(g, fg, accent, blue, enabled);
        } finally { g.dispose(); }
    }

    private void draw(Graphics2D g, Color fg, Color accent, Color blue, boolean enabled) {
        switch (name) {
            case "new" -> { page(g, fg); g.setColor(accent); plus(g, 11, 11, 3); }
            case "open" -> {
                Path2D p = new Path2D.Double(); p.moveTo(1.5, 4); p.lineTo(6, 4); p.lineTo(7.5, 5.5); p.lineTo(13.5, 5.5);
                p.lineTo(13.5, 13.5); p.lineTo(1.5, 13.5); p.closePath(); g.setColor(new Color(0xF4C861)); g.fill(p); g.setColor(fg); g.draw(p);
            }
            case "save", "save-as" -> {
                g.setColor(blue); g.fill(new RoundRectangle2D.Double(2, 2, 12, 12, 2, 2));
                g.setColor(Color.WHITE); g.fill(new Rectangle2D.Double(4.5, 2.5, 7, 4)); g.fill(new Rectangle2D.Double(4, 9, 8, 4.5));
                if (name.equals("save-as")) { g.setColor(accent); g.setStroke(stroke(1.6f)); g.draw(new Line2D.Double(9, 15, 15, 9)); }
            }
            case "print" -> {
                g.draw(new Rectangle2D.Double(4.5, 1.5, 7, 4)); g.setColor(fg); g.fill(new RoundRectangle2D.Double(1.5, 5.5, 13, 6, 2, 2));
                g.setColor(Color.WHITE); g.fill(new Rectangle2D.Double(4.5, 9.5, 7, 5)); g.setColor(fg); g.draw(new Rectangle2D.Double(4.5, 9.5, 7, 5));
            }
            case "properties" -> { page(g, fg); lines(g, fg, 5, 6, 3); g.setColor(blue); g.fill(new Ellipse2D.Double(9.5, 9.5, 5, 5)); }
            case "extract" -> { page(g, fg); g.setColor(accent); arrowRight(g, 7, 9, 15); }
            case "paste" -> {
                g.setColor(new Color(0xB98A4E)); g.fill(new RoundRectangle2D.Double(2, 2.5, 10, 12, 2, 2));
                g.setColor(fg); g.fill(new RoundRectangle2D.Double(4.5, 1, 5, 3, 1, 1));
                g.setColor(Color.WHITE); g.fill(new Rectangle2D.Double(6.5, 6.5, 8, 8.5)); g.setColor(fg); g.draw(new Rectangle2D.Double(6.5, 6.5, 8, 8.5));
            }
            case "cut" -> {
                g.draw(new Ellipse2D.Double(2, 10, 4, 4)); g.draw(new Ellipse2D.Double(10, 10, 4, 4));
                g.draw(new Line2D.Double(5.5, 10.5, 11, 1.5)); g.draw(new Line2D.Double(10.5, 10.5, 5, 1.5));
            }
            case "copy", "duplicate" -> {
                g.draw(new RoundRectangle2D.Double(1.5, 1.5, 8.5, 10, 1.5, 1.5));
                g.setColor(enabled ? Color.WHITE : fg); g.fill(new RoundRectangle2D.Double(5.5, 4.5, 9, 10, 1.5, 1.5));
                g.setColor(fg); g.draw(new RoundRectangle2D.Double(5.5, 4.5, 9, 10, 1.5, 1.5));
                if (name.equals("duplicate")) { g.setColor(accent); plus(g, 10, 9.5, 2.5); }
            }
            case "delete" -> {
                g.setColor(accent); g.draw(new Line2D.Double(2.5, 4, 13.5, 4)); g.draw(new Line2D.Double(6, 4, 6.5, 2)); g.draw(new Line2D.Double(10, 4, 9.5, 2));
                Path2D p = new Path2D.Double(); p.moveTo(3.5, 4); p.lineTo(4.5, 14.5); p.lineTo(11.5, 14.5); p.lineTo(12.5, 4); g.draw(p);
                g.draw(new Line2D.Double(6.5, 6.5, 6.8, 12)); g.draw(new Line2D.Double(9.5, 6.5, 9.2, 12));
            }
            case "view" -> {
                page(g, fg);
                g.setColor(blue);
                g.draw(new Ellipse2D.Double(4, 6, 8, 5));
                g.fill(new Ellipse2D.Double(7, 7, 2, 2));
            }
            case "cursor" -> {
                Path2D p = new Path2D.Double(); p.moveTo(3.5, 1.5); p.lineTo(3.5, 13); p.lineTo(6.5, 10); p.lineTo(9, 14.8);
                p.lineTo(10.8, 14); p.lineTo(8.4, 9.3); p.lineTo(12.5, 9.3); p.closePath();
                g.setColor(enabled ? (UiTokens.isDarkTheme() ? new Color(0x2A2D33) : Color.WHITE) : fg); g.fill(p); g.setColor(fg); g.draw(p);
            }
            case "area" -> { dashed(g, fg, 1.5, 1.5, 13, 13); g.setColor(blue); corners(g); }
            case "eraser" -> {
                Path2D p = new Path2D.Double(); p.moveTo(9.5, 1.5); p.lineTo(15, 7); p.lineTo(8, 14); p.lineTo(4, 14); p.lineTo(1, 11); p.closePath();
                g.setColor(new Color(0xF2A7B5)); g.fill(p); g.setColor(fg); g.draw(p); g.draw(new Line2D.Double(5, 7, 11, 13)); g.draw(new Line2D.Double(4, 14.5, 15, 14.5));
            }
            case "select-all" -> { dashed(g, fg, 1.5, 1.5, 13, 13); g.setColor(blue); g.fill(new Rectangle2D.Double(4, 4, 8, 8)); }
            case "zoom-in", "zoom-out" -> {
                g.setStroke(stroke(1.6f)); g.draw(new Ellipse2D.Double(1.5, 1.5, 9, 9)); g.draw(new Line2D.Double(9.5, 9.5, 14.5, 14.5));
                g.setStroke(stroke(1.4f)); g.draw(new Line2D.Double(4, 6, 8, 6)); if (name.equals("zoom-in")) g.draw(new Line2D.Double(6, 4, 6, 8));
            }
            case "fit-width" -> { g.draw(new Rectangle2D.Double(3.5, 1.5, 9, 13)); g.setColor(blue); arrowBoth(g); }
            case "fit-page" -> { g.draw(new Rectangle2D.Double(3.5, 1.5, 9, 13)); g.setColor(blue); corners(g); }
            case "undo", "redo" -> {
                boolean undo = name.equals("undo");
                if (!undo) { g.translate(16, 0); g.scale(-1, 1); }
                g.setColor(undo ? blue : GREEN); g.setStroke(stroke(1.8f));
                g.draw(new Arc2D.Double(3, 4, 11, 9, 90, -200, Arc2D.OPEN));
                Path2D p = new Path2D.Double(); p.moveTo(1.5, 4.5); p.lineTo(6.5, 1.5); p.lineTo(6.5, 7.5); p.closePath(); g.fill(p);
            }
            case "text" -> {
                g.setColor(blue); text(g, "T", Font.BOLD, 13, 8, 13);
                g.setColor(fg); g.draw(new Line2D.Double(13.5, 3, 13.5, 14));
            }
            case "image" -> {
                g.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 2, 2));
                Path2D p = new Path2D.Double(); p.moveTo(2, 13); p.lineTo(6, 8); p.lineTo(9, 11); p.lineTo(11, 9); p.lineTo(14, 13); p.closePath();
                g.setColor(enabled ? GREEN : fg); g.fill(p); g.setColor(enabled ? YELLOW : fg); g.fill(new Ellipse2D.Double(10, 4, 3, 3));
            }
            case "rectangle" -> { g.setColor(blue); g.setStroke(stroke(1.6f)); g.draw(new Rectangle2D.Double(2, 3.5, 12, 9)); }
            case "ellipse" -> { g.setColor(blue); g.setStroke(stroke(1.6f)); g.draw(new Ellipse2D.Double(1.5, 3, 13, 10)); }
            case "line" -> { g.setColor(blue); g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(2.5, 13.5, 13.5, 2.5)); }
            case "arrow" -> {
                g.setColor(blue); g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(2.5, 13.5, 12, 4));
                Path2D p = new Path2D.Double(); p.moveTo(14.5, 1.5); p.lineTo(13.5, 7.5); p.lineTo(8.5, 2.5); p.closePath(); g.fill(p);
            }
            case "shapes", "shape" -> {
                g.setColor(blue); g.fill(new Ellipse2D.Double(1.5, 1.5, 7.5, 7.5));
                g.setColor(enabled ? ORANGE : fg); g.fill(new Rectangle2D.Double(7, 7, 7.5, 7.5));
            }
            case "pen" -> {
                Path2D p = new Path2D.Double(); p.moveTo(11.5, 1.5); p.lineTo(14.5, 4.5); p.lineTo(5.5, 13.5); p.lineTo(1.5, 14.5); p.lineTo(2.5, 10.5); p.closePath();
                g.setColor(enabled ? ORANGE : fg); g.fill(p); g.setColor(fg); g.draw(p); g.draw(new Line2D.Double(9.5, 3.5, 12.5, 6.5));
            }
            case "note" -> {
                g.setColor(enabled ? new Color(0xFFE38A) : new Color(0xDDDDDD)); Path2D p = new Path2D.Double();
                p.moveTo(2, 2); p.lineTo(14, 2); p.lineTo(14, 10.5); p.lineTo(10.5, 14); p.lineTo(2, 14); p.closePath(); g.fill(p);
                g.setColor(fg); g.draw(p); lines(g, fg, 4.5, 5.5, 3);
            }
            case "highlight" -> {
                g.setColor(enabled ? YELLOW : new Color(0xDDDDDD)); g.fill(new Rectangle2D.Double(1, 9.5, 14, 5));
                g.setColor(fg); text(g, "ab", Font.BOLD, 9, 8, 13);
            }
            case "field" -> { g.draw(new RoundRectangle2D.Double(1.5, 4.5, 13, 7, 2, 2)); g.setColor(blue); g.draw(new Line2D.Double(4, 6.5, 4, 9.5)); }
            case "checkbox" -> {
                g.draw(new RoundRectangle2D.Double(2, 2, 12, 12, 2, 2)); g.setColor(blue); g.setStroke(stroke(1.8f));
                Path2D p = new Path2D.Double(); p.moveTo(4.5, 8); p.lineTo(7, 10.5); p.lineTo(11.5, 5); g.draw(p);
            }
            case "choice" -> {
                g.draw(new RoundRectangle2D.Double(1.5, 4.5, 13, 7, 2, 2)); g.draw(new Line2D.Double(10.5, 4.5, 10.5, 11.5));
                Path2D p = new Path2D.Double(); p.moveTo(11.7, 7); p.lineTo(13.5, 7); p.lineTo(12.6, 8.8); p.closePath(); g.fill(p);
            }
            case "radio" -> { g.draw(new Ellipse2D.Double(2, 2, 12, 12)); g.setColor(blue); g.fill(new Ellipse2D.Double(5, 5, 6, 6)); }
            case "signature" -> {
                g.setColor(blue); g.setStroke(stroke(1.4f)); Path2D p = new Path2D.Double();
                p.moveTo(1.5, 11); p.curveTo(3, 4, 5, 3, 5.5, 7); p.curveTo(6, 11, 8, 4, 9.5, 8); p.curveTo(10.5, 10.5, 12, 7, 14.5, 8); g.draw(p);
                g.setColor(fg); g.draw(new Line2D.Double(1.5, 13.8, 14.5, 13.8));
            }
            case "sign" -> {
                page(g, fg); g.setColor(enabled ? GREEN : fg); g.fill(new Ellipse2D.Double(8.5, 8.5, 6.5, 6.5));
                g.setColor(Color.WHITE); g.setStroke(stroke(1.2f)); Path2D p = new Path2D.Double(); p.moveTo(10, 11.8); p.lineTo(11.4, 13.2); p.lineTo(13.6, 10.3); g.draw(p);
            }
            case "validate" -> {
                Path2D p = new Path2D.Double(); p.moveTo(8, 1.5); p.lineTo(14, 4); p.curveTo(14, 10, 11, 13, 8, 14.8); p.curveTo(5, 13, 2, 10, 2, 4); p.closePath();
                g.setColor(enabled ? GREEN : fg); g.fill(p); g.setColor(Color.WHITE); g.setStroke(stroke(1.6f));
                Path2D c = new Path2D.Double(); c.moveTo(5, 8); c.lineTo(7.2, 10.2); c.lineTo(11, 5.8); g.draw(c);
            }
            case "ocr" -> { corners(g); g.setColor(blue); text(g, "Aa", Font.BOLD, 8, 8, 11); }
            case "find" -> { g.setStroke(stroke(1.8f)); g.draw(new Ellipse2D.Double(2, 2, 8, 8)); g.draw(new Line2D.Double(9, 9, 14, 14)); }
            case "replace" -> {
                g.setColor(fg); text(g, "ab", Font.PLAIN, 7, 5, 7); g.setColor(blue); text(g, "cd", Font.PLAIN, 7, 11, 14.5);
                g.setColor(fg); g.draw(new Line2D.Double(9, 3, 13, 3)); g.draw(new Line2D.Double(13, 3, 13, 8));
            }
            case "forward", "backward" -> {
                boolean forward = name.equals("forward");
                g.setColor(forward ? fg : blue); g.fill(new Rectangle2D.Double(1.5, 1.5, 8, 8));
                g.setColor(enabled ? Color.WHITE : fg); g.fill(new Rectangle2D.Double(6.5, 6.5, 8, 8));
                g.setColor(forward ? blue : fg); if (forward) g.fill(new Rectangle2D.Double(6.5, 6.5, 8, 8));
                g.setColor(fg); g.draw(new Rectangle2D.Double(6.5, 6.5, 8, 8)); g.draw(new Rectangle2D.Double(1.5, 1.5, 8, 8));
            }
            case "rotate-right", "rotate-left", "rotate-free" -> {
                boolean left = name.equals("rotate-left");
                if (left) { g.translate(16, 0); g.scale(-1, 1); }
                g.setColor(name.equals("rotate-free") ? (enabled ? PURPLE : fg) : blue); g.setStroke(stroke(1.7f));
                g.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 120, -270, Arc2D.OPEN));
                Path2D p = new Path2D.Double(); p.moveTo(11.5, 0.5); p.lineTo(14.8, 4.5); p.lineTo(9.8, 5); p.closePath(); g.fill(p);
            }
            case "align" , "align-left", "align-center", "align-right" -> {
                double[] widths = {10, 6, 8};
                for (int i = 0; i < 3; i++) {
                    double w = widths[i], x = switch (name) { case "align-center" -> 8 - w / 2; case "align-right" -> 14 - w; default -> 2; };
                    g.setColor(i == 1 ? blue : fg); g.fill(new Rectangle2D.Double(x, 2.5 + i * 4, w, 2.5));
                }
                if (!name.equals("align")) { g.setColor(accent); double x = name.equals("align-left") ? 1 : name.equals("align-right") ? 15 : 8; g.draw(new Line2D.Double(x, 1, x, 15)); }
            }
            case "align-top", "align-middle", "align-bottom" -> {
                double[] heights = {10, 6, 8};
                for (int i = 0; i < 3; i++) {
                    double h = heights[i], y = switch (name) { case "align-middle" -> 8 - h / 2; case "align-bottom" -> 14 - h; default -> 2; };
                    g.setColor(i == 1 ? blue : fg); g.fill(new Rectangle2D.Double(2.5 + i * 4, y, 2.5, h));
                }
                g.setColor(accent); double y = name.equals("align-top") ? 1 : name.equals("align-bottom") ? 15 : 8; g.draw(new Line2D.Double(1, y, 15, y));
            }
            case "page-blank" -> { page(g, fg); g.setColor(accent); plus(g, 8, 9, 3); }
            case "page-insert" -> { page(g, fg); g.setColor(blue); arrowRight(g, 3, 9, 11); }
            case "page-up", "previous" -> chevron(g, blue, true);
            case "page-down", "next" -> chevron(g, blue, false);
            case "page-delete" -> {
                page(g, fg); g.setColor(accent); g.setStroke(stroke(1.6f));
                g.draw(new Line2D.Double(9, 9, 14, 14)); g.draw(new Line2D.Double(14, 9, 9, 14));
            }
            case "view-single" -> { g.draw(new Rectangle2D.Double(4, 1.5, 8, 13)); lines(g, fg, 6, 4.5, 4); }
            case "view-continuous" -> { g.draw(new Rectangle2D.Double(4, 0.5, 8, 7)); g.draw(new Rectangle2D.Double(4, 8.5, 8, 7)); }
            case "sidebar" -> { g.draw(new Rectangle2D.Double(1.5, 2.5, 13, 11)); g.setColor(blue); g.fill(new Rectangle2D.Double(2, 3, 4, 10)); }
            case "status" -> { g.draw(new Rectangle2D.Double(1.5, 2.5, 13, 11)); g.setColor(blue); g.fill(new Rectangle2D.Double(2, 10.5, 12, 2.5)); }
            case "text-panel" -> { g.draw(new Rectangle2D.Double(1.5, 2.5, 13, 11)); lines(g, fg, 3.5, 5, 4); }
            case "fill-form" -> { g.draw(new RoundRectangle2D.Double(1.5, 5, 13, 6.5, 2, 2)); g.setColor(blue); text(g, "Ab", Font.PLAIN, 6.5, 6.5, 10); }
            case "bold" -> text(g, "B", Font.BOLD, 13, 8, 13);
            case "italic" -> text(g, "I", Font.ITALIC, 13, 8, 13);
            case "color-text", "color-stroke", "color-fill" -> {
                Color chosen = swatch == null ? (name.equals("color-fill") ? YELLOW : RED) : swatch;
                switch (name) {
                    case "color-text" -> text(g, "A", Font.BOLD, 11, 8, 10.5);
                    case "color-stroke" -> { g.setStroke(stroke(1.6f)); g.draw(new Rectangle2D.Double(3, 1.5, 10, 8)); }
                    default -> { g.fill(new Rectangle2D.Double(3, 1.5, 10, 8)); }
                }
                g.setColor(enabled ? chosen : fg); g.fill(new Rectangle2D.Double(1.5, 11.5, 13, 3.5));
                if (chosen.getAlpha() == 0 || swatch != null && swatch.equals(Color.WHITE)) { g.setColor(fg); g.draw(new Rectangle2D.Double(1.5, 11.5, 13, 3.5)); }
            }
            case "line-width" -> {
                for (int i = 0; i < 3; i++) { g.setStroke(new BasicStroke(1 + i, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)); g.draw(new Line2D.Double(2, 3.5 + i * 4.5, 14, 3.5 + i * 4.5)); }
            }
            case "pages" -> { g.draw(new Rectangle2D.Double(1.5, 1.5, 8, 11)); g.setColor(enabled ? Color.WHITE : fg); g.fill(new Rectangle2D.Double(6.5, 4.5, 8, 11)); g.setColor(fg); g.draw(new Rectangle2D.Double(6.5, 4.5, 8, 11)); }
            case "tools" -> {
                g.setColor(blue); g.fill(new Rectangle2D.Double(2, 2, 5, 5)); g.fill(new Rectangle2D.Double(9, 9, 5, 5));
                g.setColor(fg); g.draw(new Rectangle2D.Double(9, 2, 5, 5)); g.draw(new Rectangle2D.Double(2, 9, 5, 5));
            }
            case "search" -> { g.setStroke(stroke(1.6f)); g.draw(new Ellipse2D.Double(2, 2, 8, 8)); g.draw(new Line2D.Double(9, 9, 14, 14)); }
            default -> {
                g.fill(new Ellipse2D.Double(2, 7, 2.5, 2.5)); g.fill(new Ellipse2D.Double(6.75, 7, 2.5, 2.5)); g.fill(new Ellipse2D.Double(11.5, 7, 2.5, 2.5));
            }
        }
    }

    private static BasicStroke stroke(float width) { return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }

    private static void page(Graphics2D g, Color fg) {
        Path2D p = new Path2D.Double(); p.moveTo(3, 1.5); p.lineTo(10, 1.5); p.lineTo(13, 4.5); p.lineTo(13, 14.5); p.lineTo(3, 14.5); p.closePath();
        g.setColor(UiTokens.isDarkTheme() ? new Color(0x3A3D42) : Color.WHITE); g.fill(p); g.setColor(fg); g.draw(p);
        g.draw(new Line2D.Double(10, 1.5, 10, 4.5)); g.draw(new Line2D.Double(10, 4.5, 13, 4.5));
    }
    private static void lines(Graphics2D g, Color fg, double x, double y, int count) {
        g.setColor(fg);
        for (int i = 0; i < count; i++) g.draw(new Line2D.Double(x, y + i * 2.5, x + (i == count - 1 ? 4 : 6), y + i * 2.5));
    }
    private static void plus(Graphics2D g, double cx, double cy, double r) {
        g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(cx - r, cy, cx + r, cy)); g.draw(new Line2D.Double(cx, cy - r, cx, cy + r));
    }
    private static void arrowRight(Graphics2D g, double x1, double y, double x2) {
        g.setStroke(stroke(1.6f)); g.draw(new Line2D.Double(x1, y, x2, y));
        g.draw(new Line2D.Double(x2 - 3, y - 3, x2, y)); g.draw(new Line2D.Double(x2 - 3, y + 3, x2, y));
    }
    private static void arrowBoth(Graphics2D g) {
        g.setStroke(stroke(1.4f)); g.draw(new Line2D.Double(1, 8, 15, 8));
        g.draw(new Line2D.Double(1, 8, 3.5, 5.5)); g.draw(new Line2D.Double(1, 8, 3.5, 10.5));
        g.draw(new Line2D.Double(15, 8, 12.5, 5.5)); g.draw(new Line2D.Double(15, 8, 12.5, 10.5));
    }
    private static void corners(Graphics2D g) {
        g.setStroke(stroke(1.5f));
        g.draw(new Line2D.Double(1, 4.5, 1, 1)); g.draw(new Line2D.Double(1, 1, 4.5, 1));
        g.draw(new Line2D.Double(11.5, 1, 15, 1)); g.draw(new Line2D.Double(15, 1, 15, 4.5));
        g.draw(new Line2D.Double(1, 11.5, 1, 15)); g.draw(new Line2D.Double(1, 15, 4.5, 15));
        g.draw(new Line2D.Double(15, 11.5, 15, 15)); g.draw(new Line2D.Double(11.5, 15, 15, 15));
    }
    private static void dashed(Graphics2D g, Color fg, double x, double y, double w, double h) {
        g.setColor(fg);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4, new float[]{2, 2}, 0));
        g.draw(new Rectangle2D.Double(x, y, w, h));
        g.setStroke(stroke(1.2f));
    }
    private static void chevron(Graphics2D g, Color color, boolean up) {
        g.setColor(color); g.setStroke(stroke(2f));
        Path2D p = new Path2D.Double();
        if (up) { p.moveTo(3, 11); p.lineTo(8, 5); p.lineTo(13, 11); } else { p.moveTo(3, 5); p.lineTo(8, 11); p.lineTo(13, 5); }
        g.draw(p);
    }
    private static void text(Graphics2D g, String value, int style, double size, double cx, double baseline) {
        g.setFont(new Font(Font.SANS_SERIF, style, 1).deriveFont((float) size));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(value, (float) (cx - metrics.stringWidth(value) / 2.0), (float) baseline);
    }
}
