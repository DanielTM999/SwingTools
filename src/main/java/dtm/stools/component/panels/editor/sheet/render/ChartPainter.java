package dtm.stools.component.panels.editor.sheet.render;

import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartPainter {
    private SheetTheme theme = SheetTheme.OFFICE;

    public void setTheme(SheetTheme value) { theme = value == null ? SheetTheme.OFFICE : value; }

    private Color seriesColor(ChartData.Series s, int index) { return s.color() != null ? SheetPalette.color(s.color()) : SheetPalette.series(index, theme); }

    public void paint(Graphics2D g0, SheetChart chart, ChartData data, Rectangle bounds, boolean dark) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color bg = dark ? new Color(0x2B2B2B) : Color.WHITE, fg = dark ? new Color(0xE0E0E0) : new Color(0x404040);
            g.setColor(bg);
            g.fill(bounds);
            g.setColor(dark ? new Color(0x505050) : new Color(0xD9D9D9));
            g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
            g.clip(bounds);
            Font base = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, Math.min(14, bounds.height / 26)));
            Rectangle area = new Rectangle(bounds.x + 10, bounds.y + 8, bounds.width - 20, bounds.height - 16);
            if (!chart.title().isEmpty()) {
                g.setFont(base.deriveFont(Font.PLAIN, base.getSize2D() * 1.35f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(fg);
                g.drawString(chart.title(), area.x + (area.width - fm.stringWidth(chart.title())) / 2, area.y + fm.getAscent());
                area.y += fm.getHeight() + 4;
                area.height -= fm.getHeight() + 4;
            }
            g.setFont(base);
            ChartType type = chart.type();
            boolean circular = type.circular() || type == ChartType.TREEMAP || type == ChartType.SUNBURST || type == ChartType.FUNNEL;
            List<String> legendItems = new ArrayList<>();
            List<Color> legendColors = new ArrayList<>();
            if (circular && type != ChartType.FUNNEL && !data.series().isEmpty()) {
                for (int k = 0; k < data.pointCount(); k++) { legendItems.add(k < data.categories().size() ? data.categories().get(k) : String.valueOf(k + 1)); legendColors.add(SheetPalette.series(k, theme)); }
            } else if (type != ChartType.FUNNEL && type != ChartType.WATERFALL && type != ChartType.HISTOGRAM) {
                for (int k = 0; k < data.series().size(); k++) { legendItems.add(name(data.series().get(k), k)); legendColors.add(seriesColor(data.series().get(k), k)); }
            }
            if (chart.legend() != LegendPosition.NONE && !legendItems.isEmpty()) area = legend(g, chart.legend(), legendItems, legendColors, area, fg);
            if (data.series().isEmpty() || data.pointCount() == 0) {
                g.setColor(dark ? new Color(0x909090) : new Color(0x808080));
                String msg = "Sem dados";
                g.drawString(msg, area.x + (area.width - g.getFontMetrics().stringWidth(msg)) / 2, area.y + area.height / 2);
                return;
            }
            switch (type) {
                case PIE, DOUGHNUT -> pie(g, data, area, type == ChartType.DOUGHNUT, chart.dataLabels(), fg);
                case RADAR, FILLED_RADAR -> radar(g, data, area, type == ChartType.FILLED_RADAR, fg, dark);
                case SCATTER, SCATTER_LINES, BUBBLE -> scatter(g, chart, data, area, fg, dark);
                case TREEMAP -> treemap(g, data, area, fg);
                case SUNBURST -> sunburst(g, data, area, fg, bg);
                case FUNNEL -> funnel(g, data, area, fg);
                case HISTOGRAM -> histogram(g, chart, data, area, fg, dark, false);
                case PARETO -> histogram(g, chart, data, area, fg, dark, true);
                case WATERFALL -> waterfall(g, chart, data, area, fg, dark);
                case BOX_WHISKER -> box(g, chart, data, area, fg, dark);
                default -> cartesian(g, chart, data, area, fg, dark);
            }
        } finally {
            g.dispose();
        }
    }

    private static String name(ChartData.Series s, int k) { return s.name() == null || s.name().isBlank() ? "Série " + (k + 1) : s.name(); }

    private Rectangle legend(Graphics2D g, LegendPosition pos, List<String> items, List<Color> colors, Rectangle area, Color fg) {
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.getAscent();
        if (pos == LegendPosition.RIGHT || pos == LegendPosition.LEFT) {
            int width = 0;
            for (String s : items) width = Math.max(width, fm.stringWidth(s));
            width = Math.min(area.width / 3, width + sw + 12);
            int x = pos == LegendPosition.RIGHT ? area.x + area.width - width : area.x;
            int y = area.y + Math.max(0, (area.height - items.size() * (fm.getHeight() + 2)) / 2);
            for (int k = 0; k < items.size(); k++) {
                g.setColor(colors.get(k));
                g.fillRect(x + 2, y + (fm.getHeight() - sw) / 2, sw, sw);
                g.setColor(fg);
                g.drawString(clip(items.get(k), fm, width - sw - 8), x + sw + 6, y + fm.getAscent());
                y += fm.getHeight() + 2;
            }
            return pos == LegendPosition.RIGHT ? new Rectangle(area.x, area.y, area.width - width - 6, area.height) : new Rectangle(area.x + width + 6, area.y, area.width - width - 6, area.height);
        }
        int total = 0;
        for (String s : items) total += fm.stringWidth(s) + sw + 16;
        int rows = Math.max(1, (int) Math.ceil(total / (double) Math.max(1, area.width)));
        int height = rows * (fm.getHeight() + 2);
        int y = pos == LegendPosition.BOTTOM ? area.y + area.height - height : area.y;
        int x = area.x + Math.max(0, (area.width - Math.min(total, area.width)) / 2);
        for (int k = 0; k < items.size(); k++) {
            int w = fm.stringWidth(items.get(k)) + sw + 16;
            if (x + w > area.x + area.width && x > area.x) { x = area.x; y += fm.getHeight() + 2; }
            g.setColor(colors.get(k));
            g.fillRect(x, y + (fm.getHeight() - sw) / 2, sw, sw);
            g.setColor(fg);
            g.drawString(items.get(k), x + sw + 4, y + fm.getAscent());
            x += w;
        }
        return pos == LegendPosition.BOTTOM ? new Rectangle(area.x, area.y, area.width, area.height - height - 6) : new Rectangle(area.x, area.y + height + 6, area.width, area.height - height - 6);
    }

    private static String clip(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) return s;
        String t = s;
        while (!t.isEmpty() && fm.stringWidth(t + "…") > width) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    static double[] niceScale(double min, double max, int ticks) {
        if (min == max) { max = min + 1; min = min - (min == 0 ? 0 : 1); }
        double range = nice(max - min, false);
        double step = nice(range / Math.max(1, ticks - 1), true);
        double lo = Math.floor(min / step) * step, hi = Math.ceil(max / step) * step;
        return new double[]{lo, hi, step};
    }

    private static double nice(double x, boolean round) {
        double exp = Math.floor(Math.log10(x)), f = x / Math.pow(10, exp), nf;
        if (round) nf = f < 1.5 ? 1 : f < 3 ? 2 : f < 7 ? 5 : 10;
        else nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nf * Math.pow(10, exp);
    }

    static String label(double v) { return NumberValue.general(Math.abs(v) < 1e-12 ? 0 : v); }

    private record Axis(double min, double max, double step) {
        double map(double v, int from, int length) { return from + (v - min) / (max - min) * length; }
    }

    private void cartesian(Graphics2D g, SheetChart chart, ChartData data, Rectangle area, Color fg, boolean dark) {
        ChartType type = chart.type();
        boolean horizontal = type.horizontal();
        boolean stacked = type.stacked(), percent = type == ChartType.PERCENT_COLUMN;
        int n = data.pointCount();
        double min = 0, max = 0;
        if (stacked) {
            for (int k = 0; k < n; k++) {
                double pos = 0, neg = 0, total = 0;
                for (ChartData.Series s : data.series()) { double v = k < s.values().length && Double.isFinite(s.values()[k]) ? s.values()[k] : 0; if (v >= 0) pos += v; else neg += v; total += Math.abs(v); }
                if (percent) { max = 1; min = 0; } else { max = Math.max(max, pos); min = Math.min(min, neg); }
            }
        } else for (ChartData.Series s : data.series()) for (double v : s.values()) if (Double.isFinite(v)) { max = Math.max(max, v); min = Math.min(min, v); }
        if (chart.minimum() != null) min = chart.minimum();
        if (chart.maximum() != null) max = chart.maximum();
        double[] scale = niceScale(min, max, 6);
        Axis axis = new Axis(scale[0], scale[1], scale[2]);
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = 0;
        for (double v = axis.min; v <= axis.max + axis.step / 2; v += axis.step) labelWidth = Math.max(labelWidth, fm.stringWidth(percent ? Math.round(v * 100) + "%" : label(v)));
        Rectangle plot;
        int catLabelHeight = fm.getHeight() + 4;
        if (horizontal) {
            int catWidth = 0;
            for (String c : data.categories()) catWidth = Math.max(catWidth, fm.stringWidth(c));
            catWidth = Math.min(area.width / 3, catWidth + 8);
            plot = new Rectangle(area.x + catWidth, area.y + 4, area.width - catWidth - 8, area.height - catLabelHeight - 4);
        } else plot = new Rectangle(area.x + labelWidth + 8, area.y + 4, area.width - labelWidth - 12, area.height - catLabelHeight - 6);
        if (!chart.yAxisTitle().isEmpty()) { plot.x += fm.getHeight(); plot.width -= fm.getHeight(); }
        if (!chart.xAxisTitle().isEmpty()) plot.height -= fm.getHeight();
        if (plot.width < 10 || plot.height < 10) return;
        Color grid = dark ? new Color(0x444444) : new Color(0xE3E3E3);
        g.setStroke(new BasicStroke(1f));
        for (double v = axis.min; v <= axis.max + axis.step / 2; v += axis.step) {
            String text = percent ? Math.round(v * 100) + "%" : label(v);
            if (horizontal) {
                int x = (int) axis.map(v, plot.x, plot.width);
                if (chart.gridlines()) { g.setColor(grid); g.drawLine(x, plot.y, x, plot.y + plot.height); }
                g.setColor(fg);
                g.drawString(text, x - fm.stringWidth(text) / 2, plot.y + plot.height + fm.getAscent() + 2);
            } else {
                int y = (int) (plot.y + plot.height - axis.map(v, 0, plot.height));
                if (chart.gridlines()) { g.setColor(grid); g.drawLine(plot.x, y, plot.x + plot.width, y); }
                g.setColor(fg);
                g.drawString(text, plot.x - fm.stringWidth(text) - 4, y + fm.getAscent() / 2 - 1);
            }
        }
        axisTitles(g, chart, plot, area, fg);
        double slot = (horizontal ? plot.height : plot.width) / (double) Math.max(1, n);
        g.setColor(dark ? new Color(0x777777) : new Color(0xBFBFBF));
        double zero = horizontal ? axis.map(Math.max(axis.min, Math.min(axis.max, 0)), plot.x, plot.width) : plot.y + plot.height - axis.map(Math.max(axis.min, Math.min(axis.max, 0)), 0, plot.height);
        if (horizontal) g.draw(new Line2D.Double(zero, plot.y, zero, plot.y + plot.height)); else g.draw(new Line2D.Double(plot.x, zero, plot.x + plot.width, zero));
        g.setColor(fg);
        int every = Math.max(1, (int) Math.ceil(n / Math.max(1.0, (horizontal ? plot.height / (double) fm.getHeight() : plot.width / 60.0))));
        for (int k = 0; k < n; k += every) {
            String c = k < data.categories().size() ? data.categories().get(k) : String.valueOf(k + 1);
            if (horizontal) g.drawString(clip(c, fm, plot.x - area.x - 6), area.x, (int) (plot.y + slot * k + slot / 2 + fm.getAscent() / 2.0));
            else { String t = clip(c, fm, (int) Math.max(20, slot * every - 4)); g.drawString(t, (int) (plot.x + slot * k + slot / 2 - fm.stringWidth(t) / 2.0), plot.y + plot.height + fm.getAscent() + 2); }
        }
        List<ChartData.Series> bars = new ArrayList<>(), lines = new ArrayList<>(), areas = new ArrayList<>();
        for (ChartData.Series s : data.series()) {
            ChartType st = s.type() == null || type != ChartType.COMBO ? (type == ChartType.COMBO ? ChartType.COLUMN : type) : s.type();
            switch (st) {
                case LINE, LINE_MARKERS, STACKED_LINE, STOCK -> lines.add(s);
                case AREA, STACKED_AREA -> areas.add(s);
                default -> bars.add(s);
            }
        }
        if (!areas.isEmpty()) {
            double[] base = new double[n];
            for (ChartData.Series s : areas) {
                int idx = data.series().indexOf(s);
                Path2D p = new Path2D.Double();
                double[] top = new double[n];
                for (int k = 0; k < n; k++) top[k] = (stacked ? base[k] : 0) + value(s, k);
                for (int k = 0; k < n; k++) { double x = plot.x + slot * k + slot / 2, y = plot.y + plot.height - axis.map(top[k], 0, plot.height); if (k == 0) p.moveTo(x, y); else p.lineTo(x, y); }
                for (int k = n - 1; k >= 0; k--) { double x = plot.x + slot * k + slot / 2, y = plot.y + plot.height - axis.map(stacked ? base[k] : Math.max(axis.min, 0), 0, plot.height); p.lineTo(x, y); }
                p.closePath();
                Color c = seriesColor(s, idx);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
                g.fill(p);
                if (stacked) base = top;
            }
        }
        if (!bars.isEmpty()) {
            double groupWidth = slot * 0.7, barWidth = stacked ? groupWidth : groupWidth / bars.size();
            for (int k = 0; k < n; k++) {
                double pos = 0, neg = 0, total = 0;
                if (percent) for (ChartData.Series s : bars) total += Math.abs(value(s, k));
                for (int j = 0; j < bars.size(); j++) {
                    ChartData.Series s = bars.get(j);
                    double v = value(s, k);
                    if (percent && total > 0) v /= total;
                    double from, to;
                    if (stacked) { if (v >= 0) { from = pos; pos += v; to = pos; } else { from = neg; neg += v; to = neg; } }
                    else { from = 0; to = v; }
                    from = Math.max(axis.min, Math.min(axis.max, from)); to = Math.max(axis.min, Math.min(axis.max, to));
                    double offset = slot * k + (slot - groupWidth) / 2 + (stacked ? 0 : j * barWidth);
                    Rectangle2D rect;
                    if (horizontal) {
                        double x1 = axis.map(from, plot.x, plot.width), x2 = axis.map(to, plot.x, plot.width);
                        rect = new Rectangle2D.Double(Math.min(x1, x2), plot.y + offset, Math.abs(x2 - x1), barWidth - 1);
                    } else {
                        double y1 = plot.y + plot.height - axis.map(from, 0, plot.height), y2 = plot.y + plot.height - axis.map(to, 0, plot.height);
                        rect = new Rectangle2D.Double(plot.x + offset, Math.min(y1, y2), barWidth - 1, Math.abs(y2 - y1));
                    }
                    g.setColor(seriesColor(s, data.series().indexOf(s)));
                    g.fill(rect);
                    if (chart.dataLabels()) { g.setColor(fg); String t = percent ? Math.round(v * 100) + "%" : label(value(s, k)); g.drawString(t, (int) (rect.getCenterX() - fm.stringWidth(t) / 2.0), (int) (horizontal ? rect.getCenterY() + fm.getAscent() / 2.0 : rect.getY() - 2)); }
                }
            }
        }
        double[] base = new double[n];
        for (ChartData.Series s : lines) {
            int idx = data.series().indexOf(s);
            Color c = seriesColor(s, idx);
            Path2D p = new Path2D.Double();
            boolean started = false;
            List<double[]> points = new ArrayList<>();
            for (int k = 0; k < n; k++) {
                if (k >= s.values().length || !Double.isFinite(s.values()[k])) { started = false; continue; }
                double v = (type == ChartType.STACKED_LINE ? base[k] : 0) + s.values()[k];
                if (type == ChartType.STACKED_LINE) base[k] = v;
                double x = plot.x + slot * k + slot / 2, y = plot.y + plot.height - axis.map(Math.max(axis.min, Math.min(axis.max, v)), 0, plot.height);
                if (!started) { p.moveTo(x, y); started = true; } else p.lineTo(x, y);
                points.add(new double[]{x, y, s.values()[k]});
            }
            g.setColor(c);
            if (type != ChartType.STOCK) { g.setStroke(new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(p); }
            g.setStroke(new BasicStroke(1f));
            if (type == ChartType.LINE_MARKERS || type == ChartType.STOCK || type == ChartType.COMBO) for (double[] pt : points) g.fill(new Ellipse2D.Double(pt[0] - 3, pt[1] - 3, 6, 6));
            if (chart.dataLabels()) { g.setColor(fg); for (double[] pt : points) g.drawString(label(pt[2]), (int) pt[0] + 3, (int) pt[1] - 4); }
        }
        if (type == ChartType.STOCK && data.series().size() >= 3) {
            g.setColor(fg);
            ChartData.Series hi = data.series().get(data.series().size() - 3), lo = data.series().get(data.series().size() - 2);
            for (int k = 0; k < n; k++) {
                double x = plot.x + slot * k + slot / 2;
                g.draw(new Line2D.Double(x, plot.y + plot.height - axis.map(value(hi, k), 0, plot.height), x, plot.y + plot.height - axis.map(value(lo, k), 0, plot.height)));
            }
        }
    }

    private void axisTitles(Graphics2D g, SheetChart chart, Rectangle plot, Rectangle area, Color fg) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(fg);
        if (!chart.xAxisTitle().isEmpty()) g.drawString(chart.xAxisTitle(), plot.x + (plot.width - fm.stringWidth(chart.xAxisTitle())) / 2, area.y + area.height - fm.getDescent());
        if (!chart.yAxisTitle().isEmpty()) {
            var old = g.getTransform();
            g.rotate(-Math.PI / 2, area.x + fm.getAscent(), plot.y + plot.height / 2.0);
            g.drawString(chart.yAxisTitle(), area.x + fm.getAscent() - fm.stringWidth(chart.yAxisTitle()) / 2, plot.y + plot.height / 2 + fm.getAscent() / 2);
            g.setTransform(old);
        }
    }

    private static double value(ChartData.Series s, int k) { return k < s.values().length && Double.isFinite(s.values()[k]) ? s.values()[k] : 0; }

    private void pie(Graphics2D g, ChartData data, Rectangle area, boolean doughnut, boolean labels, Color fg) {
        ChartData.Series s = data.series().getFirst();
        double total = 0;
        for (double v : s.values()) if (Double.isFinite(v) && v > 0) total += v;
        if (total <= 0) return;
        int size = Math.min(area.width, area.height) - 10;
        Rectangle circle = new Rectangle(area.x + (area.width - size) / 2, area.y + (area.height - size) / 2, size, size);
        double start = 90;
        FontMetrics fm = g.getFontMetrics();
        for (int k = 0; k < s.values().length; k++) {
            double v = s.values()[k];
            if (!Double.isFinite(v) || v <= 0) continue;
            double extent = -v / total * 360;
            g.setColor(SheetPalette.series(k, theme));
            g.fill(new Arc2D.Double(circle, start, extent, Arc2D.PIE));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Arc2D.Double(circle, start, extent, Arc2D.PIE));
            if (labels) {
                double mid = Math.toRadians(start + extent / 2), rr = size * (doughnut ? 0.38 : 0.33);
                String t = Math.round(v / total * 100) + "%";
                g.setColor(SheetPalette.contrast(SheetPalette.series(k, theme)));
                g.drawString(t, (int) (circle.getCenterX() + rr * Math.cos(mid) - fm.stringWidth(t) / 2.0), (int) (circle.getCenterY() - rr * Math.sin(mid) + fm.getAscent() / 2.0));
            }
            start += extent;
        }
        if (doughnut) {
            int hole = size / 2;
            g.setColor(g.getBackground() == null ? Color.WHITE : new Color(0xFFFFFF));
            Color bg = fg.getRed() > 128 ? new Color(0x2B2B2B) : Color.WHITE;
            g.setColor(bg);
            g.fill(new Ellipse2D.Double(circle.getCenterX() - hole / 2.0, circle.getCenterY() - hole / 2.0, hole, hole));
        }
    }

    private void radar(Graphics2D g, ChartData data, Rectangle area, boolean filled, Color fg, boolean dark) {
        int n = data.pointCount();
        if (n < 3) return;
        double max = 0;
        for (ChartData.Series s : data.series()) for (double v : s.values()) if (Double.isFinite(v)) max = Math.max(max, v);
        double[] scale = niceScale(0, max, 5);
        double cx = area.getCenterX(), cy = area.getCenterY(), radius = Math.min(area.width, area.height) / 2.0 - 20;
        g.setColor(dark ? new Color(0x505050) : new Color(0xD9D9D9));
        for (double v = scale[2]; v <= scale[1] + scale[2] / 2; v += scale[2]) {
            Path2D ring = new Path2D.Double();
            for (int k = 0; k <= n; k++) { double a = Math.PI / 2 - 2 * Math.PI * (k % n) / n, r = radius * v / scale[1]; double x = cx + r * Math.cos(a), y = cy - r * Math.sin(a); if (k == 0) ring.moveTo(x, y); else ring.lineTo(x, y); }
            g.draw(ring);
        }
        FontMetrics fm = g.getFontMetrics();
        for (int k = 0; k < n; k++) {
            double a = Math.PI / 2 - 2 * Math.PI * k / n;
            g.setColor(dark ? new Color(0x505050) : new Color(0xD9D9D9));
            g.draw(new Line2D.Double(cx, cy, cx + radius * Math.cos(a), cy - radius * Math.sin(a)));
            String c = k < data.categories().size() ? data.categories().get(k) : String.valueOf(k + 1);
            g.setColor(fg);
            g.drawString(c, (int) (cx + (radius + 8) * Math.cos(a) - fm.stringWidth(c) / 2.0), (int) (cy - (radius + 8) * Math.sin(a) + fm.getAscent() / 2.0));
        }
        for (int j = 0; j < data.series().size(); j++) {
            ChartData.Series s = data.series().get(j);
            Path2D p = new Path2D.Double();
            for (int k = 0; k <= n; k++) {
                double v = value(s, k % n), a = Math.PI / 2 - 2 * Math.PI * (k % n) / n, r = radius * v / scale[1];
                double x = cx + r * Math.cos(a), y = cy - r * Math.sin(a);
                if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            Color c = seriesColor(s, j);
            if (filled) { g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 110)); g.fill(p); }
            g.setColor(c);
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void scatter(Graphics2D g, SheetChart chart, ChartData data, Rectangle area, Color fg, boolean dark) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxSize = 0;
        for (ChartData.Series s : data.series()) {
            for (int k = 0; k < s.values().length; k++) {
                double x = s.x() != null && k < s.x().length ? s.x()[k] : k + 1, y = s.values()[k];
                if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                if (s.sizes() != null && k < s.sizes().length) maxSize = Math.max(maxSize, s.sizes()[k]);
            }
        }
        if (minX == Double.MAX_VALUE) return;
        double[] sx = niceScale(Math.min(0, minX), maxX, 6), sy = niceScale(Math.min(0, minY), maxY, 6);
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = Math.max(fm.stringWidth(label(sy[1])), fm.stringWidth(label(sy[0])));
        Rectangle plot = new Rectangle(area.x + labelWidth + 8, area.y + 4, area.width - labelWidth - 12, area.height - fm.getHeight() - 8);
        if (!chart.xAxisTitle().isEmpty()) plot.height -= fm.getHeight();
        Axis ax = new Axis(sx[0], sx[1], sx[2]), ay = new Axis(sy[0], sy[1], sy[2]);
        Color grid = dark ? new Color(0x444444) : new Color(0xE3E3E3);
        for (double v = ay.min; v <= ay.max + ay.step / 2; v += ay.step) {
            int y = (int) (plot.y + plot.height - ay.map(v, 0, plot.height));
            if (chart.gridlines()) { g.setColor(grid); g.drawLine(plot.x, y, plot.x + plot.width, y); }
            g.setColor(fg);
            g.drawString(label(v), plot.x - fm.stringWidth(label(v)) - 4, y + fm.getAscent() / 2);
        }
        for (double v = ax.min; v <= ax.max + ax.step / 2; v += ax.step) {
            int x = (int) ax.map(v, plot.x, plot.width);
            g.setColor(fg);
            g.drawString(label(v), x - fm.stringWidth(label(v)) / 2, plot.y + plot.height + fm.getAscent() + 2);
        }
        axisTitles(g, chart, plot, area, fg);
        for (int j = 0; j < data.series().size(); j++) {
            ChartData.Series s = data.series().get(j);
            Color c = seriesColor(s, j);
            Path2D p = new Path2D.Double();
            boolean started = false;
            for (int k = 0; k < s.values().length; k++) {
                double x = s.x() != null && k < s.x().length ? s.x()[k] : k + 1, y = s.values()[k];
                if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
                double px = ax.map(x, plot.x, plot.width), py = plot.y + plot.height - ay.map(y, 0, plot.height);
                if (!started) { p.moveTo(px, py); started = true; } else p.lineTo(px, py);
                double r = 4;
                if (chart.type() == ChartType.BUBBLE && s.sizes() != null && k < s.sizes().length && maxSize > 0) r = 4 + 22 * Math.sqrt(s.sizes()[k] / maxSize);
                g.setColor(chart.type() == ChartType.BUBBLE ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 170) : c);
                g.fill(new Ellipse2D.Double(px - r, py - r, 2 * r, 2 * r));
            }
            if (chart.type() == ChartType.SCATTER_LINES) { g.setColor(c); g.setStroke(new BasicStroke(2f)); g.draw(p); g.setStroke(new BasicStroke(1f)); }
        }
    }

    private void treemap(Graphics2D g, ChartData data, Rectangle area, Color fg) {
        ChartData.Series s = data.series().getFirst();
        List<double[]> items = new ArrayList<>();
        for (int k = 0; k < s.values().length; k++) if (Double.isFinite(s.values()[k]) && s.values()[k] > 0) items.add(new double[]{s.values()[k], k});
        items.sort((a, b) -> Double.compare(b[0], a[0]));
        double total = items.stream().mapToDouble(a -> a[0]).sum();
        if (total <= 0) return;
        squarify(g, data, items, new Rectangle2D.Double(area.x, area.y, area.width, area.height), total);
    }

    private void squarify(Graphics2D g, ChartData data, List<double[]> items, Rectangle2D rect, double total) {
        if (items.isEmpty() || rect.getWidth() < 1 || rect.getHeight() < 1) return;
        if (items.size() == 1) { cell(g, data, items.getFirst(), rect); return; }
        boolean wide = rect.getWidth() >= rect.getHeight();
        double side = wide ? rect.getHeight() : rect.getWidth();
        List<double[]> row = new ArrayList<>();
        double rowSum = 0, worst = Double.MAX_VALUE;
        int k = 0;
        for (; k < items.size(); k++) {
            double v = items.get(k)[0], sum = rowSum + v;
            double area = rect.getWidth() * rect.getHeight() * sum / total;
            double length = area / side;
            double w = 0;
            for (double[] it : row) w = Math.max(w, ratio(rect.getWidth() * rect.getHeight() * it[0] / total, length));
            w = Math.max(w, ratio(rect.getWidth() * rect.getHeight() * v / total, length));
            if (!row.isEmpty() && w > worst) break;
            worst = w;
            row.add(items.get(k));
            rowSum = sum;
        }
        double rowArea = rect.getWidth() * rect.getHeight() * rowSum / total, thickness = rowArea / side, offset = 0;
        for (double[] it : row) {
            double len = side * it[0] / rowSum;
            Rectangle2D r = wide ? new Rectangle2D.Double(rect.getX(), rect.getY() + offset, thickness, len) : new Rectangle2D.Double(rect.getX() + offset, rect.getY(), len, thickness);
            cell(g, data, it, r);
            offset += len;
        }
        Rectangle2D rest = wide ? new Rectangle2D.Double(rect.getX() + thickness, rect.getY(), rect.getWidth() - thickness, rect.getHeight()) : new Rectangle2D.Double(rect.getX(), rect.getY() + thickness, rect.getWidth(), rect.getHeight() - thickness);
        squarify(g, data, items.subList(k, items.size()), rest, total - rowSum);
    }

    private static double ratio(double area, double length) { double other = area / length; return Math.max(length / other, other / length); }

    private void cell(Graphics2D g, ChartData data, double[] item, Rectangle2D r) {
        int k = (int) item[1];
        Color c = SheetPalette.series(k, theme);
        g.setColor(c);
        g.fill(r);
        g.setColor(Color.WHITE);
        g.draw(r);
        String label = k < data.categories().size() ? data.categories().get(k) : String.valueOf(k + 1);
        g.setColor(SheetPalette.contrast(c));
        FontMetrics fm = g.getFontMetrics();
        if (r.getWidth() > fm.stringWidth(label) + 4 && r.getHeight() > fm.getHeight()) g.drawString(label, (int) r.getX() + 4, (int) r.getY() + fm.getAscent() + 2);
    }

    private void sunburst(Graphics2D g, ChartData data, Rectangle area, Color fg, Color bg) {
        ChartData.Series s = data.series().getFirst();
        double total = Arrays.stream(s.values()).filter(v -> Double.isFinite(v) && v > 0).sum();
        if (total <= 0) return;
        int size = Math.min(area.width, area.height) - 10;
        Rectangle outer = new Rectangle(area.x + (area.width - size) / 2, area.y + (area.height - size) / 2, size, size);
        double start = 90;
        for (int k = 0; k < s.values().length; k++) {
            double v = s.values()[k];
            if (!Double.isFinite(v) || v <= 0) continue;
            double extent = -v / total * 360;
            g.setColor(SheetPalette.series(k, theme));
            g.fill(new Arc2D.Double(outer, start, extent, Arc2D.PIE));
            g.setColor(bg);
            g.draw(new Arc2D.Double(outer, start, extent, Arc2D.PIE));
            start += extent;
        }
        int hole = size / 3;
        g.setColor(bg);
        g.fill(new Ellipse2D.Double(outer.getCenterX() - hole / 2.0, outer.getCenterY() - hole / 2.0, hole, hole));
    }

    private void funnel(Graphics2D g, ChartData data, Rectangle area, Color fg) {
        ChartData.Series s = data.series().getFirst();
        int n = s.values().length;
        double max = Arrays.stream(s.values()).filter(Double::isFinite).max().orElse(0);
        if (max <= 0 || n == 0) return;
        FontMetrics fm = g.getFontMetrics();
        int labelW = 0;
        for (String c : data.categories()) labelW = Math.max(labelW, fm.stringWidth(c));
        labelW = Math.min(area.width / 3, labelW + 8);
        double h = area.height / (double) n;
        int plotW = area.width - labelW;
        for (int k = 0; k < n; k++) {
            double v = value(s, k), w = plotW * v / max;
            Rectangle2D r = new Rectangle2D.Double(area.x + labelW + (plotW - w) / 2, area.y + k * h + 1, w, h - 2);
            Color c = seriesColor(s, 0);
            g.setColor(c);
            g.fill(r);
            g.setColor(SheetPalette.contrast(c));
            String t = label(v);
            if (r.getWidth() > fm.stringWidth(t)) g.drawString(t, (int) (r.getCenterX() - fm.stringWidth(t) / 2.0), (int) (r.getCenterY() + fm.getAscent() / 2.0 - 1));
            g.setColor(fg);
            String c2 = k < data.categories().size() ? data.categories().get(k) : "";
            g.drawString(clip(c2, fm, labelW - 4), area.x, (int) (area.y + k * h + h / 2 + fm.getAscent() / 2.0));
        }
    }

    private void histogram(Graphics2D g, SheetChart chart, ChartData data, Rectangle area, Color fg, boolean dark, boolean pareto) {
        double[] values = data.series().getFirst().values();
        double[] clean = Arrays.stream(values).filter(Double::isFinite).sorted().toArray();
        if (clean.length == 0) return;
        List<String> cats = new ArrayList<>();
        List<Double> counts = new ArrayList<>();
        if (pareto) {
            List<double[]> items = new ArrayList<>();
            for (int k = 0; k < values.length; k++) if (Double.isFinite(values[k])) items.add(new double[]{values[k], k});
            items.sort((a, b) -> Double.compare(b[0], a[0]));
            for (double[] it : items) { cats.add((int) it[1] < data.categories().size() ? data.categories().get((int) it[1]) : String.valueOf((int) it[1] + 1)); counts.add(it[0]); }
        } else {
            int bins = (int) Math.max(1, Math.ceil(Math.sqrt(clean.length)));
            double min = clean[0], max = clean[clean.length - 1], width = max == min ? 1 : (max - min) / bins;
            double[] c = new double[bins];
            for (double v : clean) c[Math.min(bins - 1, (int) ((v - min) / width))]++;
            for (int k = 0; k < bins; k++) { cats.add("[" + label(min + k * width) + ", " + label(min + (k + 1) * width) + (k == bins - 1 ? "]" : ")")); counts.add(c[k]); }
        }
        ChartData.Series s = new ChartData.Series(pareto ? name(data.series().getFirst(), 0) : "Frequência", counts.stream().mapToDouble(Double::doubleValue).toArray(), null, null, data.series().getFirst().color(), ChartType.COLUMN);
        SheetChart column = chart.toBuilder().type(ChartType.COLUMN).build();
        cartesian(g, column, new ChartData(cats, List.of(s)), area, fg, dark);
        if (pareto) {
            double total = counts.stream().mapToDouble(Double::doubleValue).sum(), acc = 0;
            FontMetrics fm = g.getFontMetrics();
            Rectangle plot = new Rectangle(area.x + 40, area.y + 4, area.width - 44, area.height - fm.getHeight() - 10);
            double slot = plot.width / (double) counts.size();
            Path2D p = new Path2D.Double();
            for (int k = 0; k < counts.size(); k++) {
                acc += counts.get(k);
                double x = plot.x + slot * k + slot / 2, y = plot.y + plot.height - plot.height * acc / total;
                if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            g.setColor(new Color(0xED7D31));
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void waterfall(Graphics2D g, SheetChart chart, ChartData data, Rectangle area, Color fg, boolean dark) {
        double[] values = data.series().getFirst().values();
        int n = values.length;
        double running = 0, min = 0, max = 0;
        double[] starts = new double[n], ends = new double[n];
        for (int k = 0; k < n; k++) { double v = Double.isFinite(values[k]) ? values[k] : 0; starts[k] = running; running += v; ends[k] = running; min = Math.min(min, Math.min(starts[k], ends[k])); max = Math.max(max, Math.max(starts[k], ends[k])); }
        double[] scale = niceScale(min, max, 6);
        Axis axis = new Axis(scale[0], scale[1], scale[2]);
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label(scale[1])) + 8;
        Rectangle plot = new Rectangle(area.x + lw, area.y + 4, area.width - lw - 4, area.height - fm.getHeight() - 8);
        Color grid = dark ? new Color(0x444444) : new Color(0xE3E3E3);
        for (double v = axis.min; v <= axis.max + axis.step / 2; v += axis.step) {
            int y = (int) (plot.y + plot.height - axis.map(v, 0, plot.height));
            g.setColor(grid); g.drawLine(plot.x, y, plot.x + plot.width, y);
            g.setColor(fg); g.drawString(label(v), plot.x - fm.stringWidth(label(v)) - 4, y + fm.getAscent() / 2);
        }
        double slot = plot.width / (double) n;
        for (int k = 0; k < n; k++) {
            double y1 = plot.y + plot.height - axis.map(starts[k], 0, plot.height), y2 = plot.y + plot.height - axis.map(ends[k], 0, plot.height);
            g.setColor(ends[k] >= starts[k] ? new Color(0x4472C4) : new Color(0xED7D31));
            g.fill(new Rectangle2D.Double(plot.x + slot * k + slot * 0.15, Math.min(y1, y2), slot * 0.7, Math.max(1, Math.abs(y2 - y1))));
            g.setColor(fg);
            String c = k < data.categories().size() ? data.categories().get(k) : String.valueOf(k + 1);
            g.drawString(clip(c, fm, (int) slot), (int) (plot.x + slot * k + slot / 2 - fm.stringWidth(clip(c, fm, (int) slot)) / 2.0), plot.y + plot.height + fm.getAscent() + 2);
        }
    }

    private void box(Graphics2D g, SheetChart chart, ChartData data, Rectangle area, Color fg, boolean dark) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (ChartData.Series s : data.series()) for (double v : s.values()) if (Double.isFinite(v)) { min = Math.min(min, v); max = Math.max(max, v); }
        if (min == Double.MAX_VALUE) return;
        double[] scale = niceScale(min, max, 6);
        Axis axis = new Axis(scale[0], scale[1], scale[2]);
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label(scale[1])) + 8;
        Rectangle plot = new Rectangle(area.x + lw, area.y + 4, area.width - lw - 4, area.height - fm.getHeight() - 8);
        for (double v = axis.min; v <= axis.max + axis.step / 2; v += axis.step) {
            int y = (int) (plot.y + plot.height - axis.map(v, 0, plot.height));
            g.setColor(dark ? new Color(0x444444) : new Color(0xE3E3E3)); g.drawLine(plot.x, y, plot.x + plot.width, y);
            g.setColor(fg); g.drawString(label(v), plot.x - fm.stringWidth(label(v)) - 4, y + fm.getAscent() / 2);
        }
        double slot = plot.width / (double) data.series().size();
        for (int j = 0; j < data.series().size(); j++) {
            double[] v = Arrays.stream(data.series().get(j).values()).filter(Double::isFinite).sorted().toArray();
            if (v.length == 0) continue;
            double q1 = quantile(v, .25), q2 = quantile(v, .5), q3 = quantile(v, .75), lo = v[0], hi = v[v.length - 1];
            double cx = plot.x + slot * j + slot / 2, w = slot * 0.4;
            Color c = seriesColor(data.series().get(j), j);
            double yq1 = plot.y + plot.height - axis.map(q1, 0, plot.height), yq3 = plot.y + plot.height - axis.map(q3, 0, plot.height), ym = plot.y + plot.height - axis.map(q2, 0, plot.height);
            g.setColor(c);
            g.fill(new Rectangle2D.Double(cx - w / 2, yq3, w, Math.max(1, yq1 - yq3)));
            g.setColor(fg);
            g.draw(new Line2D.Double(cx - w / 2, ym, cx + w / 2, ym));
            double ylo = plot.y + plot.height - axis.map(lo, 0, plot.height), yhi = plot.y + plot.height - axis.map(hi, 0, plot.height);
            g.draw(new Line2D.Double(cx, yq3, cx, yhi)); g.draw(new Line2D.Double(cx, yq1, cx, ylo));
            g.draw(new Line2D.Double(cx - w / 4, yhi, cx + w / 4, yhi)); g.draw(new Line2D.Double(cx - w / 4, ylo, cx + w / 4, ylo));
            String n = name(data.series().get(j), j);
            g.drawString(clip(n, fm, (int) slot), (int) (cx - fm.stringWidth(clip(n, fm, (int) slot)) / 2.0), plot.y + plot.height + fm.getAscent() + 2);
        }
    }

    private static double quantile(double[] sorted, double p) {
        double rank = p * (sorted.length - 1);
        int k = (int) Math.floor(rank);
        return k + 1 < sorted.length ? sorted[k] + (rank - k) * (sorted[k + 1] - sorted[k]) : sorted[k];
    }

    public static Shape boundsShape(Rectangle r) { return r; }
}
