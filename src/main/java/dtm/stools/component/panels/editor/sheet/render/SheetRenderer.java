package dtm.stools.component.panels.editor.sheet.render;

import dtm.stools.component.panels.editor.sheet.calc.SparklineValue;
import dtm.stools.component.panels.editor.sheet.data.ConditionalResult;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.FillPattern;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SheetRenderer {
    private final Map<String, Font> fonts = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Font> e) { return size() > 256; }
    };
    private final Map<String, Paint> patterns = new HashMap<>();
    private final ChartPainter charts = new ChartPainter();

    public ChartPainter charts() { return charts; }

    public Font font(CellStyle s, double zoom) {
        String key = s.fontFamily() + "|" + s.fontSize() + "|" + s.bold() + "|" + s.italic() + "|" + s.underline() + "|" + s.strikethrough() + "|" + zoom + "|" + s.superscript() + s.subscript();
        return fonts.computeIfAbsent(key, k -> {
            int style = (s.bold() ? Font.BOLD : 0) | (s.italic() ? Font.ITALIC : 0);
            float size = (float) (s.fontSize() * 96 / 72 * zoom * (s.superscript() || s.subscript() ? 0.7 : 1));
            Font f = new Font(s.fontFamily(), style, 1).deriveFont(size);
            Map<TextAttribute, Object> attrs = new HashMap<>();
            if (s.underline() != UnderlineStyle.NONE) attrs.put(TextAttribute.UNDERLINE, s.underline() == UnderlineStyle.DOUBLE || s.underline() == UnderlineStyle.DOUBLE_ACCOUNTING ? TextAttribute.UNDERLINE_LOW_TWO_PIXEL : TextAttribute.UNDERLINE_ON);
            if (s.strikethrough()) attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            if (s.superscript()) attrs.put(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
            if (s.subscript()) attrs.put(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
            return attrs.isEmpty() ? f : f.deriveFont(attrs);
        });
    }

    public void paintBackground(Graphics2D g, CellPaintContext c) {
        Rectangle r = c.bounds();
        ConditionalResult cf = c.conditional();
        SheetFill fill = c.style().fill();
        if (cf != null && cf.scaleColor() != null) { g.setColor(SheetPalette.color(cf.scaleColor())); g.fillRect(r.x, r.y, r.width, r.height); }
        else if (fill.visible()) paintFill(g, fill, r);
        if (cf != null && cf.barFraction() != null) paintDataBar(g, r, cf, c.zoom());
    }

    public void paintFill(Graphics2D g, SheetFill fill, Rectangle r) {
        if (fill.isGradient()) {
            double angle = Math.toRadians(fill.gradientAngle());
            float x2 = (float) (r.x + r.width * Math.cos(angle)), y2 = (float) (r.y + r.height * Math.sin(angle));
            g.setPaint(new GradientPaint(r.x, r.y, SheetPalette.color(fill.foreground()), x2 == r.x && y2 == r.y ? r.x + r.width : x2, y2, SheetPalette.color(fill.gradientEnd())));
            g.fillRect(r.x, r.y, r.width, r.height);
            return;
        }
        if (fill.pattern() == FillPattern.SOLID) {
            Integer c = fill.primaryColor();
            if (c == null) return;
            g.setColor(SheetPalette.color(c));
            g.fillRect(r.x, r.y, r.width, r.height);
            return;
        }
        Color fg = fill.foreground() == null ? Color.BLACK : SheetPalette.color(fill.foreground());
        Color bg = fill.background() == null ? Color.WHITE : SheetPalette.color(fill.background());
        g.setPaint(pattern(fill.pattern(), fg, bg));
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    private Paint pattern(FillPattern p, Color fg, Color bg) {
        String key = p + ":" + fg.getRGB() + ":" + bg.getRGB();
        return patterns.computeIfAbsent(key, k -> {
            BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
                boolean on = switch (p) {
                    case GRAY_50 -> (x + y) % 2 == 0;
                    case GRAY_75 -> (x + y) % 2 == 0 || x % 2 == 0;
                    case GRAY_25 -> x % 2 == 0 && y % 2 == 0;
                    case GRAY_125 -> x == 0 && y == 0;
                    case GRAY_0625 -> x == 0 && y == 0 && false;
                    case HORIZONTAL -> y < 2;
                    case VERTICAL -> x < 2;
                    case DOWN -> (x - y + 4) % 4 < 2;
                    case UP -> (x + y) % 4 < 2;
                    case GRID -> x < 2 && y < 2 || x >= 2 && y >= 2;
                    case TRELLIS -> (x + y) % 2 == 0 || y % 2 == 0;
                    case THIN_HORIZONTAL -> y == 0;
                    case THIN_VERTICAL -> x == 0;
                    case THIN_DOWN -> x == y;
                    case THIN_UP -> x + y == 3;
                    case THIN_GRID -> x == 0 || y == 0;
                    case THIN_TRELLIS -> (x + y) % 2 == 0 && y % 2 == 0;
                    default -> false;
                };
                img.setRGB(x, y, (on ? fg : bg).getRGB());
            }
            return new TexturePaint(img, new Rectangle(0, 0, 4, 4));
        });
    }

    private void paintDataBar(Graphics2D g, Rectangle r, ConditionalResult cf, double zoom) {
        int pad = (int) Math.max(1, 2 * zoom);
        int w = (int) Math.round((r.width - 2 * pad) * cf.barFraction());
        if (w <= 0) return;
        Color c = SheetPalette.color(cf.barColor() == null ? 0xFF638EC6 : cf.barColor());
        Rectangle bar = new Rectangle(r.x + pad, r.y + pad, w, r.height - 2 * pad);
        if (cf.barGradient()) g.setPaint(new GradientPaint(bar.x, 0, c, bar.x + Math.max(1, bar.width), 0, new Color(255, 255, 255, 0)));
        else g.setColor(c);
        g.fill(bar);
        g.setColor(c.darker());
        g.setStroke(new BasicStroke(1f));
        g.drawRect(bar.x, bar.y, bar.width - 1, bar.height - 1);
    }

    public void paintContent(Graphics2D g, CellPaintContext c) {
        Rectangle r = c.bounds();
        CellValue v = c.value();
        CellStyle s = c.style();
        if (v instanceof SparklineValue spark) { SparklinePainter.paint(g, spark, r, c.zoom()); return; }
        if (c.checkbox()) { paintCheckbox(g, r, v instanceof BoolValue b && b.value(), c.zoom(), c.dark()); return; }
        ConditionalResult cf = c.conditional();
        int iconSpace = 0;
        if (cf != null && cf.iconSet() != null && cf.iconIndex() >= 0) {
            int size = (int) Math.max(8, Math.min(r.height - 4, 16 * c.zoom()));
            paintIcon(g, cf.iconSet(), cf.iconIndex(), new Rectangle(r.x + 2, r.y + (r.height - size) / 2, size, size));
            iconSpace = size + 4;
        }
        if (cf != null && cf.hideValue()) return;
        String text = c.text();
        if (text == null || text.isEmpty()) return;
        Font font = font(s, c.zoom());
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        Color color = c.textColor() != null ? SheetPalette.color(c.textColor()) : s.fontColor() != null ? SheetPalette.color(s.fontColor()) : c.dark() ? new Color(0xE8E8E8) : Color.BLACK;
        if (c.hyperlink() && s.fontColor() == null && c.textColor() == null) color = c.dark() ? new Color(0x6CB4FF) : new Color(0x0563C1);
        g.setColor(color);
        Rectangle tb = c.textBounds() == null ? r : c.textBounds();
        int indent = (int) Math.round(s.indent() * 9 * c.zoom());
        int pad = (int) Math.max(2, Math.round(3 * c.zoom()));
        HorizontalAlignment h = s.horizontal();
        boolean numeric = v instanceof NumberValue || v instanceof BoolValue || v instanceof ErrorValue;
        if (h == HorizontalAlignment.GENERAL) h = c.showFormulas() ? HorizontalAlignment.LEFT : v instanceof NumberValue ? HorizontalAlignment.RIGHT : v instanceof BoolValue || v instanceof ErrorValue ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT;
        Shape oldClip = g.getClip();
        g.clip(new Rectangle(tb.x + 1, tb.y + 1, Math.max(0, tb.width - 2), Math.max(0, tb.height - 1)));
        if (s.rotation() != 0) {
            paintRotated(g, text, s, r, fm, pad);
            g.setClip(oldClip);
            return;
        }
        List<String> lines;
        int available = r.width - 2 * pad - indent - iconSpace;
        if (s.wrap() || h == HorizontalAlignment.JUSTIFY || h == HorizontalAlignment.DISTRIBUTED) lines = wrap(text, fm, Math.max(4, available));
        else lines = List.of(text.split("\n", -1));
        if (s.shrinkToFit() && !s.wrap()) {
            int widest = lines.stream().mapToInt(fm::stringWidth).max().orElse(0);
            if (widest > available && widest > 0) {
                g.setFont(font.deriveFont((float) (font.getSize2D() * Math.max(0.3, (double) available / widest))));
                fm = g.getFontMetrics();
            }
        }
        if (numeric && !s.wrap() && lines.size() == 1 && fm.stringWidth(text) > available && !c.showFormulas() && v instanceof NumberValue) {
            lines = List.of("#".repeat(Math.max(1, available / Math.max(1, fm.charWidth('#')))));
        }
        int lineHeight = fm.getHeight();
        int total = lineHeight * lines.size();
        VerticalAlignment va = s.vertical();
        int y = switch (va) {
            case TOP -> r.y + pad / 2;
            case CENTER, JUSTIFY, DISTRIBUTED -> r.y + (r.height - total) / 2;
            default -> r.y + r.height - total - Math.max(1, pad / 2);
        };
        for (String line : lines) {
            int w = fm.stringWidth(line);
            int x = switch (h) {
                case RIGHT -> tb.x + tb.width - pad - w - indent;
                case CENTER, CENTER_ACROSS -> tb.x + (tb.width - w) / 2;
                case FILL -> tb.x + pad;
                default -> tb.x + pad + indent + iconSpace;
            };
            if (h == HorizontalAlignment.FILL && w > 0 && !line.isEmpty()) {
                StringBuilder b = new StringBuilder(line);
                while (fm.stringWidth(b.toString() + line) <= r.width - 2 * pad) b.append(line);
                line = b.toString();
            }
            if (c.fillCharacter() && w < available) x = tb.x + tb.width - pad - w;
            g.drawString(line, x, y + fm.getAscent());
            y += lineHeight;
        }
        g.setClip(oldClip);
    }

    private void paintRotated(Graphics2D g, String text, CellStyle s, Rectangle r, FontMetrics fm, int pad) {
        AffineTransform old = g.getTransform();
        if (s.isVertical()) {
            int y = r.y + pad;
            for (char ch : text.toCharArray()) {
                String t = String.valueOf(ch);
                g.drawString(t, r.x + (r.width - fm.stringWidth(t)) / 2, y + fm.getAscent());
                y += fm.getHeight();
            }
            return;
        }
        double angle = -Math.toRadians(s.rotation());
        int w = fm.stringWidth(text);
        double cx = r.x + r.width / 2.0, cy = r.y + r.height / 2.0;
        g.rotate(angle, cx, cy);
        g.drawString(text, (float) (cx - w / 2.0), (float) (cy + fm.getAscent() / 2.0 - 1));
        g.setTransform(old);
    }

    public static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) { out.add(""); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("(?<= )")) {
                if (fm.stringWidth(line + word) <= width || line.isEmpty()) {
                    if (line.isEmpty() && fm.stringWidth(word) > width) {
                        for (char ch : word.toCharArray()) {
                            if (fm.stringWidth(line.toString() + ch) > width && !line.isEmpty()) { out.add(line.toString()); line.setLength(0); }
                            line.append(ch);
                        }
                    } else line.append(word);
                } else {
                    out.add(line.toString().stripTrailing());
                    line.setLength(0);
                    line.append(word);
                }
            }
            out.add(line.toString().stripTrailing());
        }
        return out;
    }

    public int measureWidth(Graphics2D g, CellStyle s, String text, double zoom) {
        g.setFont(font(s, zoom));
        FontMetrics fm = g.getFontMetrics();
        int w = 0;
        for (String line : text.split("\n")) w = Math.max(w, fm.stringWidth(line));
        return w + (int) Math.round(8 * zoom) + (int) Math.round(s.indent() * 9 * zoom);
    }

    public int measureHeight(Graphics2D g, CellStyle s, String text, int width, double zoom) {
        g.setFont(font(s, zoom));
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = s.wrap() ? wrap(text, fm, Math.max(4, width - 6)) : List.of(text.split("\n", -1));
        return lines.size() * fm.getHeight() + (int) Math.round(4 * zoom);
    }

    public void paintBorders(Graphics2D g, CellStyle s, Rectangle r) {
        side(g, s.top(), r.x, r.y, r.x + r.width, r.y, true);
        side(g, s.bottom(), r.x, r.y + r.height, r.x + r.width, r.y + r.height, true);
        side(g, s.left(), r.x, r.y, r.x, r.y + r.height, false);
        side(g, s.right(), r.x + r.width, r.y, r.x + r.width, r.y + r.height, false);
        if (s.diagonal().visible()) {
            if (s.diagonalDown()) side(g, s.diagonal(), r.x, r.y, r.x + r.width, r.y + r.height, false);
            if (s.diagonalUp()) side(g, s.diagonal(), r.x, r.y + r.height, r.x + r.width, r.y, false);
        }
    }

    private void side(Graphics2D g, SheetBorder b, int x1, int y1, int x2, int y2, boolean horizontal) {
        if (!b.visible()) return;
        g.setColor(b.color() == null ? Color.BLACK : SheetPalette.color(b.color()));
        Stroke old = g.getStroke();
        float w = b.style().width();
        g.setStroke(stroke(b.style()));
        if (b.style() == BorderStyle.DOUBLE) {
            g.setStroke(new BasicStroke(1f));
            if (horizontal) { g.drawLine(x1, y1 - 1, x2, y2 - 1); g.drawLine(x1, y1 + 1, x2, y2 + 1); }
            else { g.drawLine(x1 - 1, y1, x2 - 1, y2); g.drawLine(x1 + 1, y1, x2 + 1, y2); }
        } else g.draw(new Line2D.Float(x1, y1, x2, y2));
        g.setStroke(old);
    }

    private static Stroke stroke(BorderStyle s) {
        float w = Math.max(1f, s.width());
        return switch (s) {
            case DOTTED, HAIR -> new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{1f, 1f}, 0f);
            case DASHED, MEDIUM_DASHED -> new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 2f}, 0f);
            case DASH_DOT, MEDIUM_DASH_DOT, SLANT_DASH_DOT -> new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 2f, 1f, 2f}, 0f);
            case DASH_DOT_DOT, MEDIUM_DASH_DOT_DOT -> new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 2f, 1f, 2f, 1f, 2f}, 0f);
            default -> new BasicStroke(w, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
        };
    }

    public void paintIndicators(Graphics2D g, CellPaintContext c) {
        Rectangle r = c.bounds();
        int size = (int) Math.max(4, 6 * c.zoom());
        if (c.noteIndicator() || c.threadIndicator()) {
            g.setColor(c.threadIndicator() ? new Color(0x7B3FB0) : new Color(0xD83B01));
            g.fillPolygon(new Polygon(new int[]{r.x + r.width - size, r.x + r.width, r.x + r.width}, new int[]{r.y, r.y, r.y + size}, 3));
        }
        if (c.errorIndicator()) {
            g.setColor(new Color(0x107C41));
            g.fillPolygon(new Polygon(new int[]{r.x, r.x + size, r.x}, new int[]{r.y, r.y, r.y + size}, 3));
        }
    }

    public void paintCheckbox(Graphics2D g, Rectangle r, boolean checked, double zoom, boolean dark) {
        int size = (int) Math.max(10, Math.min(r.height - 4, 14 * zoom));
        int x = r.x + (r.width - size) / 2, y = r.y + (r.height - size) / 2;
        Color accent = new Color(0x107C41);
        if (checked) { g.setColor(accent); g.fill(new RoundRectangle2D.Float(x, y, size, size, 3, 3)); }
        else { g.setColor(dark ? new Color(0x2B2B2B) : Color.WHITE); g.fill(new RoundRectangle2D.Float(x, y, size, size, 3, 3)); g.setColor(dark ? new Color(0x9A9A9A) : new Color(0x6E6E6E)); g.draw(new RoundRectangle2D.Float(x, y, size, size, 3, 3)); }
        if (checked) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(Math.max(1.5f, size / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(x + size * .22, y + size * .52);
            p.lineTo(x + size * .42, y + size * .72);
            p.lineTo(x + size * .78, y + size * .3);
            g.draw(p);
            g.setStroke(new BasicStroke(1f));
        }
    }

    public void paintIcon(Graphics2D g, IconSetType set, int index, Rectangle r) {
        int n = set.size();
        double level = n <= 1 ? 1 : (double) index / (n - 1);
        Color red = new Color(0xE0463C), yellow = new Color(0xF2C200), green = new Color(0x3AA35B), gray = new Color(0x808080);
        Color c = level < 0.34 ? red : level < 0.67 ? yellow : green;
        String name = set.name();
        g.setStroke(new BasicStroke(1f));
        if (name.startsWith("ARROWS")) {
            boolean grayArrows = name.contains("GRAY");
            Color col = grayArrows ? gray : c;
            g.setColor(col);
            double angle = level >= 0.99 ? -90 : level <= 0.01 ? 90 : level > 0.6 ? -45 : level < 0.4 ? 45 : 0;
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(angle), r.getCenterX(), r.getCenterY());
            Path2D p = new Path2D.Float();
            double x = r.x, y = r.y, w = r.width, h = r.height;
            p.moveTo(x + w * .15, y + h * .38); p.lineTo(x + w * .55, y + h * .38); p.lineTo(x + w * .55, y + h * .15); p.lineTo(x + w * .95, y + h * .5);
            p.lineTo(x + w * .55, y + h * .85); p.lineTo(x + w * .55, y + h * .62); p.lineTo(x + w * .15, y + h * .62); p.closePath();
            g.fill(p);
            g.setTransform(old);
        } else if (name.startsWith("FLAGS")) {
            g.setColor(c);
            g.fillRect(r.x + r.width / 4, r.y + r.height / 6, r.width / 2, r.height / 3);
            g.setColor(Color.DARK_GRAY);
            g.drawLine(r.x + r.width / 4, r.y + r.height / 6, r.x + r.width / 4, r.y + r.height);
        } else if (name.startsWith("STARS")) {
            g.setColor(new Color(0xF2C200));
            Shape star = star(r);
            if (level >= 0.99) g.fill(star);
            else if (level > 0.01) { Shape clip = g.getClip(); g.clip(new Rectangle(r.x, r.y, r.width / 2, r.height)); g.fill(star); g.setClip(clip); g.draw(star); }
            else g.draw(star);
        } else if (name.startsWith("RATING") || name.startsWith("BOXES") || name.startsWith("QUARTERS")) {
            g.setColor(new Color(0x4472C4));
            int bars = n - 1;
            int filled = index;
            if (name.startsWith("QUARTERS")) {
                g.setColor(Color.DARK_GRAY);
                g.draw(new Ellipse2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
                g.fillArc(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 90, -(int) Math.round(360.0 * index / (n - 1)));
            } else {
                for (int k = 0; k < bars; k++) {
                    int bh = (int) (r.height * (k + 1.0) / bars);
                    Rectangle bar = new Rectangle(r.x + k * r.width / bars + 1, r.y + r.height - bh, Math.max(2, r.width / bars - 2), bh);
                    if (k < filled) g.fill(bar); else { g.setColor(new Color(0xBFBFBF)); g.fill(bar); g.setColor(new Color(0x4472C4)); }
                }
            }
        } else if (name.startsWith("SYMBOLS") || name.startsWith("SIGNS")) {
            g.setColor(c);
            g.fill(new Ellipse2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(Math.max(1.5f, r.width / 8f)));
            int m = r.width / 4;
            if (c == green) { g.drawLine(r.x + m, r.y + r.height / 2, r.x + r.width / 2 - 1, r.y + r.height - m); g.drawLine(r.x + r.width / 2 - 1, r.y + r.height - m, r.x + r.width - m, r.y + m); }
            else if (c == yellow) { g.drawLine(r.x + r.width / 2, r.y + m, r.x + r.width / 2, r.y + r.height / 2 + 1); g.fillRect(r.x + r.width / 2 - 1, r.y + r.height - m - 1, 2, 2); }
            else { g.drawLine(r.x + m, r.y + m, r.x + r.width - m, r.y + r.height - m); g.drawLine(r.x + r.width - m, r.y + m, r.x + m, r.y + r.height - m); }
            g.setStroke(new BasicStroke(1f));
        } else if (name.startsWith("TRIANGLES")) {
            g.setColor(c);
            if (c == yellow) g.fillRect(r.x + 2, r.y + r.height / 2 - 2, r.width - 4, 4);
            else if (c == green) g.fillPolygon(new int[]{r.x + r.width / 2, r.x + r.width - 1, r.x + 1}, new int[]{r.y + 2, r.y + r.height - 2, r.y + r.height - 2}, 3);
            else g.fillPolygon(new int[]{r.x + 1, r.x + r.width - 1, r.x + r.width / 2}, new int[]{r.y + 2, r.y + 2, r.y + r.height - 2}, 3);
        } else {
            if (name.equals("RED_TO_BLACK_4")) c = new Color(new int[]{0x000000, 0x808080, 0xFF9999, 0xE0463C}[Math.max(0, Math.min(3, 3 - index))]);
            g.setColor(c);
            g.fill(new Ellipse2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2));
            if (name.contains("RIMMED") || name.equals("TRAFFIC_LIGHTS_4")) { g.setColor(Color.DARK_GRAY); g.draw(new Ellipse2D.Float(r.x + 1, r.y + 1, r.width - 2, r.height - 2)); }
        }
    }

    private static Shape star(Rectangle r) {
        Path2D p = new Path2D.Float();
        double cx = r.getCenterX(), cy = r.getCenterY(), outer = r.width / 2.0, inner = outer * 0.45;
        for (int k = 0; k < 10; k++) {
            double a = Math.PI / 2 + k * Math.PI / 5, rad = k % 2 == 0 ? outer : inner;
            double x = cx + rad * Math.cos(a), y = cy - rad * Math.sin(a);
            if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    public void paintDropdownButton(Graphics2D g, Rectangle r, boolean filtered, boolean dark) {
        g.setColor(dark ? new Color(0x3C3C3C) : new Color(0xF3F3F3));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(dark ? new Color(0x707070) : new Color(0xA0A0A0));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setColor(dark ? new Color(0xDDDDDD) : new Color(0x404040));
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
        if (filtered) {
            g.fillPolygon(new int[]{cx - 4, cx + 4, cx + 1, cx + 1, cx - 1, cx - 1}, new int[]{cy - 4, cy - 4, cy, cy + 4, cy + 3, cy}, 6);
        } else g.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
    }
}
