package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartDataPoint;
import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.render.gl.GlTextRenderer;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

public abstract class CartesianChartRender extends ChartBaseRender {

    private volatile boolean gridVisible = true;
    private volatile boolean valueLabelsVisible = true;
    private volatile boolean categoryLabelsVisible = true;
    private volatile int targetTickCount = 5;
    private volatile boolean includeZero = true;
    private volatile Double fixedMinValue;
    private volatile Double fixedMaxValue;
    private volatile DoubleFunction<String> valueFormatter = ChartBaseRender::formatValue;

    private ChartRect plotRect = new ChartRect(0, 0, 0, 0);
    private double axisMin;
    private double axisMax;
    private double tickStep;
    private List<String> categories = List.of();

    public boolean isGridVisible() { return gridVisible; }
    public void setGridVisible(boolean gridVisible) { this.gridVisible = gridVisible; }

    public boolean isValueLabelsVisible() { return valueLabelsVisible; }
    public void setValueLabelsVisible(boolean valueLabelsVisible) { this.valueLabelsVisible = valueLabelsVisible; }

    public boolean isCategoryLabelsVisible() { return categoryLabelsVisible; }
    public void setCategoryLabelsVisible(boolean categoryLabelsVisible) { this.categoryLabelsVisible = categoryLabelsVisible; }

    public int getTargetTickCount() { return targetTickCount; }
    public void setTargetTickCount(int targetTickCount) { this.targetTickCount = Math.max(2, targetTickCount); }

    public boolean isIncludeZero() { return includeZero; }
    public void setIncludeZero(boolean includeZero) { this.includeZero = includeZero; }

    public Double getFixedMinValue() { return fixedMinValue; }
    public void setFixedMinValue(Double fixedMinValue) { this.fixedMinValue = fixedMinValue; }

    public Double getFixedMaxValue() { return fixedMaxValue; }
    public void setFixedMaxValue(Double fixedMaxValue) { this.fixedMaxValue = fixedMaxValue; }

    public DoubleFunction<String> getValueFormatter() { return valueFormatter; }

    public void setValueFormatter(DoubleFunction<String> valueFormatter) {
        this.valueFormatter = valueFormatter != null ? valueFormatter : ChartBaseRender::formatValue;
    }

    @Override
    protected void renderChart(GraphicsGlContext context, ChartRect area, float progress) {
        ChartDataSource ds = getDataSource();
        computeValueRange(ds);
        categories = computeCategories(ds);

        Font labelFont = getTheme().getLabelFont();
        GlTextRenderer text = textRenderer();

        float axisLeftWidth = 0f;
        if (valueLabelsVisible) {
            for (double tick = axisMin; tick <= axisMax + tickStep * 0.5; tick += tickStep) {
                axisLeftWidth = Math.max(axisLeftWidth, text.measureWidth(valueFormatter.apply(tick), labelFont));
            }
            axisLeftWidth += 10f;
        }
        float axisBottomHeight = categoryLabelsVisible && !categories.isEmpty()
                ? text.lineHeight(labelFont) + 8f
                : 0f;

        plotRect = area.inset(axisLeftWidth, 4f, 0f, axisBottomHeight);
        if (plotRect.width < 8f || plotRect.height < 8f) return;

        ChartColor plotBackground = getTheme().getPlotBackground();
        if (plotBackground != null && plotBackground.a() > 0f) {
            graphics().fillRoundRect(plotRect.x, plotRect.y, plotRect.width, plotRect.height, 6f, plotBackground);
        }

        drawGridAndAxes(context);
        renderPlot(context, plotRect, progress);
    }

    protected abstract void renderPlot(GraphicsGlContext context, ChartRect plot, float progress);

    protected final ChartRect getPlotRect() { return plotRect; }
    protected final double getAxisMin() { return axisMin; }
    protected final double getAxisMax() { return axisMax; }
    protected final double getTickStep() { return tickStep; }

    protected final List<String> getCategories() { return categories; }

    protected final float valueToY(double value) {
        double range = axisMax - axisMin;
        if (range <= 0) return plotRect.bottom();
        return (float) (plotRect.bottom() - (value - axisMin) / range * plotRect.height);
    }

    protected final float baselineY() {
        return valueToY(Math.max(axisMin, Math.min(0d, axisMax)));
    }

    protected final float slotCenterX(int index, int count) {
        if (count <= 0) return plotRect.centerX();
        return plotRect.x + (index + 0.5f) / count * plotRect.width;
    }

    protected final float pointX(int index, int count) {
        if (count <= 1) return plotRect.centerX();
        return plotRect.x + (float) index / (count - 1) * plotRect.width;
    }

    protected double[] computeDataRange(ChartDataSource ds) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (ChartSeries series : ds.getSeriesList()) {
            for (ChartDataPoint point : series.getPoints()) {
                min = Math.min(min, point.getValue());
                max = Math.max(max, point.getValue());
            }
        }
        return new double[]{min, max};
    }

    private void computeValueRange(ChartDataSource ds) {
        double[] dataRange = computeDataRange(ds);
        double min = dataRange[0];
        double max = dataRange[1];
        if (min > max) {
            min = 0;
            max = 1;
        }
        if (includeZero) {
            min = Math.min(0, min);
            max = Math.max(0, max);
        }
        Double fixedMin = fixedMinValue;
        Double fixedMax = fixedMaxValue;
        if (fixedMin != null) min = fixedMin;
        if (fixedMax != null) max = fixedMax;
        if (max - min < 1e-9) {
            double pad = Math.max(1, Math.abs(max) * 0.1);
            min -= pad;
            max += pad;
        }

        int ticks = targetTickCount;
        double step = niceNumber((max - min) / Math.max(1, ticks - 1), true);
        axisMin = fixedMin != null ? fixedMin : Math.floor(min / step) * step;
        axisMax = fixedMax != null ? fixedMax : Math.ceil(max / step) * step;
        tickStep = step;
    }

    private List<String> computeCategories(ChartDataSource ds) {
        ChartSeries longest = null;
        for (ChartSeries series : ds.getSeriesList()) {
            if (longest == null || series.size() > longest.size()) {
                longest = series;
            }
        }
        if (longest == null || longest.isEmpty()) return List.of();
        List<ChartDataPoint> points = longest.getPoints();
        List<String> labels = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            String label = points.get(i).getLabel();
            labels.add(label != null ? label : String.valueOf(i + 1));
        }
        return labels;
    }

    protected void drawGridAndAxes(GraphicsGlContext context) {
        Font labelFont = getTheme().getLabelFont();
        ChartColor gridColor = getTheme().getGridColor();
        ChartColor textColor = getTheme().getAxisTextColor();

        for (double tick = axisMin; tick <= axisMax + tickStep * 0.5; tick += tickStep) {
            float y = valueToY(tick);
            if (gridVisible) {
                boolean isZero = Math.abs(tick) < tickStep * 1e-6;
                graphics().strokeLine(plotRect.x, y, plotRect.right(), y, 1f,
                        isZero ? gridColor.brighter(0.18f) : gridColor);
            }
            if (valueLabelsVisible) {
                drawText(getValueFormatter().apply(tick), labelFont, plotRect.x - 8f, y,
                        textColor, GlTextRenderer.HAlign.RIGHT, GlTextRenderer.VAlign.MIDDLE);
            }
        }

        if (categoryLabelsVisible && !categories.isEmpty()) {
            int count = categories.size();
            float labelY = plotRect.bottom() + 6f;
            float maxLabelWidth = 0f;
            for (String label : categories) {
                maxLabelWidth = Math.max(maxLabelWidth, textRenderer().measureWidth(label, labelFont));
            }
            int stride = Math.max(1, (int) Math.ceil((maxLabelWidth + 12f) * count / Math.max(1f, plotRect.width)));
            for (int i = 0; i < count; i += stride) {
                float x = categoryLabelX(i, count);
                drawText(categories.get(i), labelFont, x, labelY,
                        textColor, GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.TOP);
            }
        }
    }

    protected float categoryLabelX(int index, int count) {
        return slotCenterX(index, count);
    }
}
