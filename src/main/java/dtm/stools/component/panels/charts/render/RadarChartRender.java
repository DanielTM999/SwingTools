package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartDataPoint;
import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.render.gl.GlTextRenderer;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

public class RadarChartRender extends ChartBaseRender {

    private volatile int ringCount = 4;
    private volatile float fillOpacity = 0.22f;
    private volatile float lineWidth = 2.2f;
    private volatile boolean showPoints = true;
    private volatile float pointRadius = 3.2f;
    private volatile boolean axisLabelsVisible = true;
    private volatile boolean ringLabelsVisible = true;
    private volatile Double fixedMaxValue;
    private volatile DoubleFunction<String> valueFormatter = ChartBaseRender::formatValue;

    public int getRingCount() { return ringCount; }
    public void setRingCount(int ringCount) { this.ringCount = Math.max(1, Math.min(10, ringCount)); }

    public float getFillOpacity() { return fillOpacity; }
    public void setFillOpacity(float fillOpacity) {
        this.fillOpacity = Math.max(0f, Math.min(1f, fillOpacity));
    }

    public float getLineWidth() { return lineWidth; }
    public void setLineWidth(float lineWidth) { this.lineWidth = Math.max(0.5f, lineWidth); }

    public boolean isShowPoints() { return showPoints; }
    public void setShowPoints(boolean showPoints) { this.showPoints = showPoints; }

    public float getPointRadius() { return pointRadius; }
    public void setPointRadius(float pointRadius) { this.pointRadius = Math.max(1f, pointRadius); }

    public boolean isAxisLabelsVisible() { return axisLabelsVisible; }
    public void setAxisLabelsVisible(boolean axisLabelsVisible) { this.axisLabelsVisible = axisLabelsVisible; }

    public boolean isRingLabelsVisible() { return ringLabelsVisible; }
    public void setRingLabelsVisible(boolean ringLabelsVisible) { this.ringLabelsVisible = ringLabelsVisible; }

    public Double getFixedMaxValue() { return fixedMaxValue; }
    public void setFixedMaxValue(Double fixedMaxValue) { this.fixedMaxValue = fixedMaxValue; }

    public DoubleFunction<String> getValueFormatter() { return valueFormatter; }
    public void setValueFormatter(DoubleFunction<String> valueFormatter) {
        this.valueFormatter = valueFormatter != null ? valueFormatter : ChartBaseRender::formatValue;
    }

    @Override
    protected void renderChart(GraphicsGlContext context, ChartRect area, float progress) {
        ChartDataSource ds = getDataSource();
        List<ChartSeries> seriesList = ds.getSeriesList();
        List<String> axes = computeAxes(ds);
        int axisCount = axes.size();
        if (axisCount < 3 || seriesList.isEmpty()) return;

        double maxValue = computeMaxValue(ds);
        if (maxValue <= 0) return;
        double step = niceNumber(maxValue / ringCount, true);
        double scaleMax = step * ringCount;
        if (scaleMax < maxValue) scaleMax = step * (ringCount + 1);

        Font labelFont = getTheme().getLabelFont();
        GlTextRenderer text = textRenderer();
        float maxLabelWidth = 0f;
        for (String axis : axes) {
            maxLabelWidth = Math.max(maxLabelWidth, text.measureWidth(axis, labelFont));
        }
        float labelHeight = text.lineHeight(labelFont);

        float cx = area.centerX();
        float cy = area.centerY();
        float radius = Math.min(area.width * 0.5f - (axisLabelsVisible ? maxLabelWidth + 12f : 4f),
                area.height * 0.5f - (axisLabelsVisible ? labelHeight + 10f : 4f));
        if (radius < 16f) return;

        float[] cos = new float[axisCount];
        float[] sin = new float[axisCount];
        for (int i = 0; i < axisCount; i++) {
            double angle = -Math.PI / 2 + Math.PI * 2 * i / axisCount;
            cos[i] = (float) Math.cos(angle);
            sin[i] = (float) Math.sin(angle);
        }

        drawGrid(cx, cy, radius, cos, sin, scaleMax);

        GraphicsInput input = input(context);
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();
        boolean mouseInside = input.isMouseInside();
        int hoveredSeries = -1;
        int hoveredAxis = -1;
        float bestDistance = 144f;

        float[] xs = new float[axisCount];
        float[] ys = new float[axisCount];
        float[] outlineX = new float[axisCount + 1];
        float[] outlineY = new float[axisCount + 1];

        for (int s = 0; s < seriesList.size(); s++) {
            ChartSeries series = seriesList.get(s);
            if (series.size() < axisCount) continue;
            ChartColor color = seriesColor(s, series);

            for (int i = 0; i < axisCount; i++) {
                double value = Math.max(0, series.get(i).getValue());
                float r = (float) (value / scaleMax) * radius * progress;
                xs[i] = cx + cos[i] * r;
                ys[i] = cy + sin[i] * r;

                if (mouseInside) {
                    float dx = mouseX - xs[i];
                    float dy = mouseY - ys[i];
                    float distance = dx * dx + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        hoveredSeries = s;
                        hoveredAxis = i;
                    }
                }
            }

            if (fillOpacity > 0f) {
                ChartColor fill = color.withAlpha(color.a() * fillOpacity);
                for (int i = 0; i < axisCount; i++) {
                    int j = (i + 1) % axisCount;
                    graphics().fillTriangle(cx, cy, xs[i], ys[i], xs[j], ys[j], fill);
                }
            }

            System.arraycopy(xs, 0, outlineX, 0, axisCount);
            System.arraycopy(ys, 0, outlineY, 0, axisCount);
            outlineX[axisCount] = xs[0];
            outlineY[axisCount] = ys[0];
            graphics().strokePolyline(outlineX, outlineY, axisCount + 1, lineWidth, color);

            if (showPoints) {
                for (int i = 0; i < axisCount; i++) {
                    boolean hovered = s == hoveredSeries && i == hoveredAxis;
                    float r = hovered ? pointRadius + 1.5f : pointRadius;
                    if (hovered) {
                        graphics().fillCircle(xs[i], ys[i], r * 2.4f, color.withAlpha(0.22f));
                    }
                    graphics().fillCircle(xs[i], ys[i], r, color);
                    graphics().fillCircle(xs[i], ys[i], Math.max(1f, r - 1.8f), getTheme().getBackground());
                }
            }
        }

        if (axisLabelsVisible) {
            drawAxisLabels(cx, cy, radius, cos, sin, axes, labelFont);
        }

        if (hoveredSeries >= 0) {
            ChartSeries series = seriesList.get(hoveredSeries);
            List<TooltipLine> lines = new ArrayList<>();
            lines.add(new TooltipLine(axes.get(hoveredAxis)));
            lines.add(new TooltipLine(series.getName() + ": "
                    + valueFormatter.apply(series.get(hoveredAxis).getValue()),
                    seriesColor(hoveredSeries, series)));
            requestTooltip(mouseX, mouseY, lines);
        }
    }

    protected void drawGrid(float cx, float cy, float radius, float[] cos, float[] sin, double scaleMax) {
        int axisCount = cos.length;
        ChartColor gridColor = getTheme().getGridColor();
        float[] ringX = new float[axisCount + 1];
        float[] ringY = new float[axisCount + 1];

        for (int ring = 1; ring <= ringCount; ring++) {
            float r = radius * ring / ringCount;
            for (int i = 0; i < axisCount; i++) {
                ringX[i] = cx + cos[i] * r;
                ringY[i] = cy + sin[i] * r;
            }
            ringX[axisCount] = ringX[0];
            ringY[axisCount] = ringY[0];
            graphics().strokePolyline(ringX, ringY, axisCount + 1, 1f, gridColor);

            if (ringLabelsVisible) {
                double value = scaleMax * ring / ringCount;
                drawText(valueFormatter.apply(value), getTheme().getLabelFont(),
                        cx + 5f, cy - r, getTheme().getAxisTextColor(),
                        GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.MIDDLE);
            }
        }
        for (int i = 0; i < axisCount; i++) {
            graphics().strokeLine(cx, cy, cx + cos[i] * radius, cy + sin[i] * radius, 1f, gridColor);
        }
    }

    protected void drawAxisLabels(float cx, float cy, float radius, float[] cos, float[] sin,
                                  List<String> axes, Font labelFont) {
        ChartColor textColor = getTheme().getAxisTextColor();
        for (int i = 0; i < axes.size(); i++) {
            float x = cx + cos[i] * (radius + 10f);
            float y = cy + sin[i] * (radius + 10f);
            GlTextRenderer.HAlign hAlign = cos[i] > 0.35f ? GlTextRenderer.HAlign.LEFT
                    : cos[i] < -0.35f ? GlTextRenderer.HAlign.RIGHT
                    : GlTextRenderer.HAlign.CENTER;
            GlTextRenderer.VAlign vAlign = sin[i] > 0.35f ? GlTextRenderer.VAlign.TOP
                    : sin[i] < -0.35f ? GlTextRenderer.VAlign.BOTTOM
                    : GlTextRenderer.VAlign.MIDDLE;
            drawText(axes.get(i), labelFont, x, y, textColor, hAlign, vAlign);
        }
    }

    private double computeMaxValue(ChartDataSource ds) {
        Double fixed = fixedMaxValue;
        if (fixed != null) return fixed;
        double max = 0;
        for (ChartSeries series : ds.getSeriesList()) {
            for (ChartDataPoint point : series.getPoints()) {
                max = Math.max(max, point.getValue());
            }
        }
        return max;
    }

    private List<String> computeAxes(ChartDataSource ds) {
        ChartSeries longest = null;
        for (ChartSeries series : ds.getSeriesList()) {
            if (longest == null || series.size() > longest.size()) {
                longest = series;
            }
        }
        if (longest == null) return List.of();
        List<ChartDataPoint> points = longest.getPoints();
        List<String> labels = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            String label = points.get(i).getLabel();
            labels.add(label != null ? label : String.valueOf(i + 1));
        }
        return labels;
    }
}
