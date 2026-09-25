package dtm.stools.component.panels.editor.sheet.render;

import dtm.stools.component.panels.editor.sheet.calc.SparklineValue;
import dtm.stools.component.panels.editor.sheet.model.SparklineType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Locale;

public final class SparklinePainter {
    private SparklinePainter() {}

    public static void paint(Graphics2D g, SparklineValue v, Rectangle r, double zoom) {
        String type = v.option("charttype", "line").toLowerCase(Locale.ROOT);
        SparklineType t = switch (type) { case "column" -> SparklineType.COLUMN; case "winloss" -> SparklineType.WIN_LOSS; case "bar" -> null; default -> SparklineType.LINE; };
        Color color = parse(v.option("color", v.option("color1", "#376092")));
        if (t == null) { bar(g, v.values(), r, color, parse(v.option("color2", "#AAAAAA"))); return; }
        double[] data = v.values().stream().mapToDouble(Double::doubleValue).toArray();
        paint(g, data, t, r, color, true, false, false, zoom);
    }

    public static void paint(Graphics2D g, double[] data, SparklineType type, Rectangle r, Color color, boolean negatives, boolean markers, boolean highLow, double zoom) {
        if (data.length == 0) return;
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = (int) Math.max(2, 3 * zoom);
        Rectangle a = new Rectangle(r.x + pad, r.y + pad, r.width - 2 * pad, r.height - 2 * pad);
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        int minI = 0, maxI = 0;
        for (int k = 0; k < data.length; k++) { if (data[k] < min) { min = data[k]; minI = k; } if (data[k] > max) { max = data[k]; maxI = k; } }
        if (type == SparklineType.LINE) {
            if (max == min) { max += 1; min -= 1; }
            Path2D p = new Path2D.Double();
            for (int k = 0; k < data.length; k++) {
                double x = a.x + (data.length == 1 ? a.width / 2.0 : a.width * k / (data.length - 1.0));
                double y = a.y + a.height - (data[k] - min) / (max - min) * a.height;
                if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            g.setColor(color);
            g.setStroke(new BasicStroke((float) Math.max(1, 1.5 * zoom)));
            g.draw(p);
            if (markers || highLow) {
                for (int k = 0; k < data.length; k++) {
                    if (!markers && k != minI && k != maxI) continue;
                    double x = a.x + (data.length == 1 ? a.width / 2.0 : a.width * k / (data.length - 1.0));
                    double y = a.y + a.height - (data[k] - min) / (max - min) * a.height;
                    g.setColor(k == maxI && highLow ? new Color(0x3AA35B) : k == minI && highLow ? new Color(0xE0463C) : color);
                    g.fillOval((int) x - 2, (int) y - 2, 4, 4);
                }
            }
        } else {
            double lo = Math.min(0, min), hi = Math.max(0, max);
            if (hi == lo) hi = lo + 1;
            double zeroY = type == SparklineType.WIN_LOSS ? a.y + a.height / 2.0 : a.y + a.height - (0 - lo) / (hi - lo) * a.height;
            double w = a.width / (double) data.length;
            for (int k = 0; k < data.length; k++) {
                double v = data[k];
                g.setColor(v < 0 && negatives ? new Color(0xD00000) : color);
                double x = a.x + k * w + w * 0.1;
                if (type == SparklineType.WIN_LOSS) {
                    double h = a.height / 2.0 - 1;
                    g.fillRect((int) x, (int) (v >= 0 ? zeroY - h : zeroY + 1), (int) Math.max(1, w * 0.8), (int) h);
                } else {
                    double y = a.y + a.height - (v - lo) / (hi - lo) * a.height;
                    g.fillRect((int) x, (int) Math.min(y, zeroY), (int) Math.max(1, w * 0.8), (int) Math.max(1, Math.abs(zeroY - y)));
                }
            }
        }
        g.setStroke(new BasicStroke(1f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    private static void bar(Graphics2D g, List<Double> values, Rectangle r, Color c1, Color c2) {
        double total = 0;
        for (double d : values) total += Math.max(0, d);
        if (total == 0) return;
        int x = r.x + 2;
        int w = r.width - 4;
        for (int k = 0; k < values.size(); k++) {
            int seg = (int) Math.round(w * Math.max(0, values.get(k)) / total);
            g.setColor(k % 2 == 0 ? c1 : c2);
            g.fillRect(x, r.y + 3, seg, r.height - 6);
            x += seg;
        }
    }

    static Color parse(String s) {
        String v = s.strip().toLowerCase(Locale.ROOT);
        try {
            if (v.startsWith("#")) return new Color(Integer.parseInt(v.substring(1), 16));
            return switch (v) { case "red", "vermelho" -> Color.RED; case "green", "verde" -> new Color(0x3AA35B); case "blue", "azul" -> new Color(0x4472C4); case "black", "preto" -> Color.BLACK; case "orange", "laranja" -> new Color(0xED7D31); case "gray", "grey", "cinza" -> Color.GRAY; default -> new Color(0x376092); };
        } catch (NumberFormatException e) { return new Color(0x376092); }
    }
}
