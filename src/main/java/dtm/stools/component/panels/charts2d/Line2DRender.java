package dtm.stools.component.panels.charts2d;

import dtm.stools.component.panels.charts.data.ChartDataPoint;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.charts.style.LegendPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class Line2DRender extends Chart2DRender {

    private static final Color[] FALLBACK_COLORS = {
            new Color(0x5E9BFF), new Color(0x4ADE80), new Color(0xF472B6),
            new Color(0xFACC15), new Color(0x38BDF8), new Color(0xA78BFA)
    };

    private boolean showPoints;
    private boolean fillArea;
    private float areaOpacity = 0.18f;
    private Double fixedMinValue;
    private Double fixedMaxValue;

    public boolean isShowPoints() {
        return showPoints;
    }

    public Line2DRender setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
        bumpRevision();
        return this;
    }

    public boolean isFillArea() {
        return fillArea;
    }

    public Line2DRender setFillArea(boolean fillArea) {
        this.fillArea = fillArea;
        bumpRevision();
        return this;
    }

    public float getAreaOpacity() {
        return areaOpacity;
    }

    public Line2DRender setAreaOpacity(float areaOpacity) {
        this.areaOpacity = Math.max(0f, Math.min(1f, areaOpacity));
        bumpRevision();
        return this;
    }

    public Line2DRender setFixedMinValue(Double fixedMinValue) {
        this.fixedMinValue = fixedMinValue;
        bumpRevision();
        return this;
    }

    public Line2DRender setFixedMaxValue(Double fixedMaxValue) {
        this.fixedMaxValue = fixedMaxValue;
        bumpRevision();
        return this;
    }

    @Override
    public void paint(Graphics2D g, int width, int height) {
        paintBackground(g, width, height);

        float pad = getPadding();
        int top = paintTitle(g, Math.round(pad));

        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        int legendHeight = isLegendVisible() && !seriesList.isEmpty() ? legendHeight(g) : 0;

        int left = Math.round(pad);
        int right = Math.round(width - pad);
        int plotTop = top + 2;
        int plotBottom = Math.round(height - pad) - legendHeight;
        if (right - left < 8 || plotBottom - plotTop < 8) {
            return;
        }

        double min = resolveMin(seriesList);
        double max = resolveMax(seriesList, min);
        double range = max - min;
        if (range <= 0) {
            range = 1;
        }

        Color grid = toAwt(getTheme().getGridColor(), new Color(0x26, 0x2B, 0x36));
        g.setStroke(new BasicStroke(1f));
        g.setColor(grid);
        for (int i = 0; i <= 4; i++) {
            int y = plotTop + (plotBottom - plotTop) * i / 4;
            g.drawLine(left, y, right, y);
        }

        Color axisText = toAwt(getTheme().getAxisTextColor(), Color.GRAY);
        g.setColor(axisText);
        g.setFont(axisFont(g));
        g.drawString(formatValue(max), left + 2, plotTop + g.getFontMetrics().getAscent());
        g.drawString(formatValue(min), left + 2, plotBottom - 2);

        int index = 0;
        for (ChartSeries series : seriesList) {
            paintSeries(g, series, index++, left, right, plotTop, plotBottom, min, range);
        }

        if (legendHeight > 0) {
            paintLegend(g, seriesList, left, plotBottom + legendHeight - 4);
        }

        paintHover(g, seriesList, left, right, plotTop, plotBottom, min, range);
    }

    private void paintHover(Graphics2D g, List<ChartSeries> seriesList,
                            int left, int right, int plotTop, int plotBottom,
                            double min, double range) {
        if (!isHovering()) {
            return;
        }
        int hoverX = getHoverX();
        int hoverY = getHoverY();
        if (hoverX < left || hoverX > right || hoverY < plotTop || hoverY > plotBottom) {
            return;
        }

        int maxCount = 0;
        for (ChartSeries series : seriesList) {
            maxCount = Math.max(maxCount, series.getPoints().size());
        }
        if (maxCount == 0) {
            return;
        }

        double spanX = right - left;
        double spanY = plotBottom - plotTop;
        double denominator = Math.max(1, maxCount - 1);
        int index = (int) Math.round((hoverX - left) / spanX * denominator);
        index = Math.max(0, Math.min(maxCount - 1, index));
        int x = (int) Math.round(left + spanX * index / denominator);

        g.setStroke(new BasicStroke(1f));
        g.setColor(toAwt(getTheme().getCrosshairColor(), new Color(0x88, 0x8F, 0x9C)));
        g.drawLine(x, plotTop, x, plotBottom);

        Font font = axisFont(g);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();

        List<String> lines = new ArrayList<>();
        List<Color> swatches = new ArrayList<>();
        String header = null;
        int seriesIndex = 0;
        for (ChartSeries series : seriesList) {
            List<ChartDataPoint> points = series.getPoints();
            if (index < points.size()) {
                ChartDataPoint point = points.get(index);
                if (header == null && point.getLabel() != null && !point.getLabel().isBlank()) {
                    header = point.getLabel();
                }
                double ratio = (point.getValue() - min) / range;
                ratio = Math.max(0d, Math.min(1d, ratio));
                int y = (int) Math.round(plotBottom - spanY * ratio);
                Color color = resolveColor(series.getColor(), seriesIndex);
                g.setColor(toAwt(getTheme().getBackground(), Color.DARK_GRAY));
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(color);
                g.fillOval(x - 3, y - 3, 6, 6);
                lines.add(series.getName() + "   " + formatValue(point.getValue()));
                swatches.add(color);
            }
            seriesIndex++;
        }
        if (lines.isEmpty()) {
            return;
        }

        int lineHeight = metrics.getHeight();
        int swatchSize = Math.max(6, metrics.getAscent() - 2);
        int boxPad = 8;
        int textWidth = header != null ? metrics.stringWidth(header) : 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, swatchSize + 6 + metrics.stringWidth(line));
        }
        int boxWidth = textWidth + boxPad * 2;
        int rows = lines.size() + (header != null ? 1 : 0);
        int boxHeight = rows * lineHeight + boxPad * 2;

        int boxX = x + 12;
        if (boxX + boxWidth > right) {
            boxX = x - 12 - boxWidth;
        }
        boxX = Math.max(left, boxX);
        int boxY = hoverY - boxHeight - 8;
        boxY = Math.max(plotTop, Math.min(boxY, plotBottom - boxHeight));

        g.setColor(toAwt(getTheme().getTooltipBackground(), new Color(0x1D, 0x22, 0x2D)));
        g.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);
        g.setColor(toAwt(getTheme().getTooltipBorderColor(), new Color(0x3A, 0x41, 0x50)));
        g.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 8, 8);

        Color tooltipText = toAwt(getTheme().getTooltipTextColor(), Color.WHITE);
        int textX = boxX + boxPad;
        int textY = boxY + boxPad + metrics.getAscent();
        if (header != null) {
            g.setColor(tooltipText);
            g.drawString(header, textX, textY);
            textY += lineHeight;
        }
        for (int i = 0; i < lines.size(); i++) {
            g.setColor(swatches.get(i));
            g.fillRect(textX, textY - swatchSize, swatchSize, swatchSize);
            g.setColor(tooltipText);
            g.drawString(lines.get(i), textX + swatchSize + 6, textY);
            textY += lineHeight;
        }
    }

    private void paintSeries(Graphics2D g, ChartSeries series, int index,
                             int left, int right, int plotTop, int plotBottom,
                             double min, double range) {
        List<ChartDataPoint> points = series.getPoints();
        if (points.isEmpty()) {
            return;
        }

        Color color = resolveColor(series.getColor(), index);
        int count = points.size();
        double spanX = right - left;
        double spanY = plotBottom - plotTop;
        double denominator = Math.max(1, count - 1);

        Path2D.Float line = new Path2D.Float();
        int firstX = left;
        int lastX = left;
        for (int i = 0; i < count; i++) {
            double ratio = (points.get(i).getValue() - min) / range;
            ratio = Math.max(0d, Math.min(1d, ratio));
            int x = (int) Math.round(left + spanX * i / denominator);
            int y = (int) Math.round(plotBottom - spanY * ratio);
            if (i == 0) {
                firstX = x;
                line.moveTo(x, y);
            } else {
                line.lineTo(x, y);
            }
            lastX = x;
        }

        if (fillArea) {
            Path2D.Float area = new Path2D.Float(line);
            area.lineTo(lastX, plotBottom);
            area.lineTo(firstX, plotBottom);
            area.closePath();
            g.setColor(withAlpha(color, Math.round(areaOpacity * 255)));
            g.fill(area);
        }

        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(line);

        if (showPoints) {
            for (int i = 0; i < count; i++) {
                double ratio = (points.get(i).getValue() - min) / range;
                ratio = Math.max(0d, Math.min(1d, ratio));
                int x = (int) Math.round(left + spanX * i / denominator);
                int y = (int) Math.round(plotBottom - spanY * ratio);
                g.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }

    private double resolveMin(List<ChartSeries> seriesList) {
        if (fixedMinValue != null) {
            return fixedMinValue;
        }
        double min = 0d;
        boolean any = false;
        for (ChartSeries series : seriesList) {
            for (ChartDataPoint point : series.getPoints()) {
                min = any ? Math.min(min, point.getValue()) : point.getValue();
                any = true;
            }
        }
        return Math.min(0d, min);
    }

    private double resolveMax(List<ChartSeries> seriesList, double min) {
        if (fixedMaxValue != null) {
            return fixedMaxValue;
        }
        double max = min;
        boolean any = false;
        for (ChartSeries series : seriesList) {
            for (ChartDataPoint point : series.getPoints()) {
                max = any ? Math.max(max, point.getValue()) : point.getValue();
                any = true;
            }
        }
        if (!any || max <= min) {
            return min + 1;
        }
        return max * 1.15;
    }

    private int legendHeight(Graphics2D g) {
        return axisFontMetrics(g).getHeight() + 6;
    }

    private void paintLegend(Graphics2D g, List<ChartSeries> seriesList, int left, int baselineY) {
        g.setFont(axisFont(g));
        FontMetrics metrics = g.getFontMetrics();
        int swatch = metrics.getAscent();
        int x = left;
        int index = 0;
        for (ChartSeries series : seriesList) {
            Color color = resolveColor(series.getColor(), index++);
            g.setColor(color);
            g.fillRect(x, baselineY - swatch, swatch, swatch);
            x += swatch + 4;
            g.setColor(toAwt(getTheme().getLegendTextColor(), Color.GRAY));
            String name = series.getName();
            g.drawString(name, x, baselineY);
            x += metrics.stringWidth(name) + 14;
        }
    }

    private static Color resolveColor(ChartColor color, int index) {
        if (color != null) {
            return color.toAwt();
        }
        return FALLBACK_COLORS[index % FALLBACK_COLORS.length];
    }

    private Font axisFont(Graphics2D g) {
        Font font = getTheme().getLabelFont();
        return font != null ? font : g.getFont().deriveFont(Font.PLAIN, 10f);
    }

    private FontMetrics axisFontMetrics(Graphics2D g) {
        return g.getFontMetrics(axisFont(g));
    }
}
