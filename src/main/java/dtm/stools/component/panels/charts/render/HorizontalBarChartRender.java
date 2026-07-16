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

public class HorizontalBarChartRender extends ChartBaseRender {

    private volatile float cornerRadius = 5f;
    private volatile float groupGapRatio = 0.3f;
    private volatile float barGapRatio = 0.12f;
    private volatile float staggerAmount = 0.3f;
    private volatile boolean gradientEnabled = true;
    private volatile boolean hoverHighlightEnabled = true;
    private volatile boolean gridVisible = true;
    private volatile boolean valueLabelsVisible = true;
    private volatile boolean categoryLabelsVisible = true;
    private volatile boolean includeZero = true;
    private volatile int targetTickCount = 5;
    private volatile Double fixedMinValue;
    private volatile Double fixedMaxValue;
    private volatile DoubleFunction<String> valueFormatter = ChartBaseRender::formatValue;

    public float getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(float cornerRadius) { this.cornerRadius = Math.max(0f, cornerRadius); }

    public float getGroupGapRatio() { return groupGapRatio; }
    public void setGroupGapRatio(float groupGapRatio) {
        this.groupGapRatio = Math.max(0f, Math.min(0.9f, groupGapRatio));
    }

    public float getBarGapRatio() { return barGapRatio; }
    public void setBarGapRatio(float barGapRatio) {
        this.barGapRatio = Math.max(0f, Math.min(0.9f, barGapRatio));
    }

    public float getStaggerAmount() { return staggerAmount; }
    public void setStaggerAmount(float staggerAmount) {
        this.staggerAmount = Math.max(0f, Math.min(0.9f, staggerAmount));
    }

    public boolean isGradientEnabled() { return gradientEnabled; }
    public void setGradientEnabled(boolean gradientEnabled) { this.gradientEnabled = gradientEnabled; }

    public boolean isHoverHighlightEnabled() { return hoverHighlightEnabled; }
    public void setHoverHighlightEnabled(boolean hoverHighlightEnabled) { this.hoverHighlightEnabled = hoverHighlightEnabled; }

    public boolean isGridVisible() { return gridVisible; }
    public void setGridVisible(boolean gridVisible) { this.gridVisible = gridVisible; }

    public boolean isValueLabelsVisible() { return valueLabelsVisible; }
    public void setValueLabelsVisible(boolean valueLabelsVisible) { this.valueLabelsVisible = valueLabelsVisible; }

    public boolean isCategoryLabelsVisible() { return categoryLabelsVisible; }
    public void setCategoryLabelsVisible(boolean categoryLabelsVisible) { this.categoryLabelsVisible = categoryLabelsVisible; }

    public boolean isIncludeZero() { return includeZero; }
    public void setIncludeZero(boolean includeZero) { this.includeZero = includeZero; }

    public int getTargetTickCount() { return targetTickCount; }
    public void setTargetTickCount(int targetTickCount) { this.targetTickCount = Math.max(2, targetTickCount); }

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
        List<ChartSeries> seriesList = ds.getSeriesList();
        int categoryCount = ds.maxPointCount();
        int seriesCount = seriesList.size();
        if (categoryCount == 0 || seriesCount == 0) return;

        List<String> categories = computeCategories(ds);
        double[] range = computeAxisRange(ds);
        double axisMin = range[0];
        double axisMax = range[1];
        double tickStep = range[2];

        Font labelFont = getTheme().getLabelFont();
        GlTextRenderer text = textRenderer();

        float categoryLabelWidth = 0f;
        if (categoryLabelsVisible) {
            for (String label : categories) {
                categoryLabelWidth = Math.max(categoryLabelWidth, text.measureWidth(label, labelFont));
            }
            categoryLabelWidth += 10f;
        }
        float valueLabelHeight = valueLabelsVisible ? text.lineHeight(labelFont) + 8f : 0f;

        ChartRect plot = area.inset(categoryLabelWidth, 4f, 4f, valueLabelHeight);
        if (plot.width < 8f || plot.height < 8f) return;

        ChartColor gridColor = getTheme().getGridColor();
        ChartColor textColor = getTheme().getAxisTextColor();
        for (double tick = axisMin; tick <= axisMax + tickStep * 0.5; tick += tickStep) {
            float x = valueToX(tick, axisMin, axisMax, plot);
            if (gridVisible) {
                boolean isZero = Math.abs(tick) < tickStep * 1e-6;
                graphics().strokeLine(x, plot.y, x, plot.bottom(), 1f,
                        isZero ? gridColor.brighter(0.18f) : gridColor);
            }
            if (valueLabelsVisible) {
                drawText(valueFormatter.apply(tick), labelFont, x, plot.bottom() + 6f,
                        textColor, GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.TOP);
            }
        }

        float slotHeight = plot.height / categoryCount;
        float groupHeight = slotHeight * (1f - groupGapRatio);
        float barSlotHeight = groupHeight / seriesCount;
        float barHeight = Math.max(1f, barSlotHeight * (1f - barGapRatio));
        float baseX = valueToX(Math.max(axisMin, Math.min(0d, axisMax)), axisMin, axisMax, plot);

        GraphicsInput input = input(context);
        boolean mouseInside = hoverHighlightEnabled && input.isMouseInside()
                && plot.contains(input.getMouseX(), input.getMouseY());
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();

        int hoveredCategory = -1;
        int hoveredSeries = -1;

        for (int i = 0; i < categoryCount; i++) {
            float localProgress = staggeredProgress(progress, i, categoryCount);
            float slotY = plot.y + i * slotHeight;

            if (categoryLabelsVisible && i < categories.size()) {
                drawText(categories.get(i), labelFont, plot.x - 8f, slotY + slotHeight * 0.5f,
                        textColor, GlTextRenderer.HAlign.RIGHT, GlTextRenderer.VAlign.MIDDLE);
            }
            if (localProgress <= 0f) continue;

            float groupStart = slotY + (slotHeight - groupHeight) * 0.5f;
            for (int s = 0; s < seriesCount; s++) {
                ChartSeries series = seriesList.get(s);
                if (i >= series.size()) continue;
                double value = series.get(i).getValue();
                if (value == 0d) continue;

                float valueX = valueToX(value, axisMin, axisMax, plot);
                float animatedX = baseX + (valueX - baseX) * localProgress;
                float left = Math.min(baseX, animatedX);
                float barWidth = Math.abs(animatedX - baseX);
                if (barWidth < 0.5f) continue;
                float barY = groupStart + s * barSlotHeight + (barSlotHeight - barHeight) * 0.5f;

                boolean hovered = mouseInside
                        && mouseX >= left && mouseX <= left + barWidth
                        && mouseY >= barY && mouseY <= barY + barHeight;
                if (hovered) {
                    hoveredCategory = i;
                    hoveredSeries = s;
                }

                drawBar(left, barY, barWidth, barHeight, value >= 0, seriesColor(s, series), hovered);
            }
        }

        if (hoveredCategory >= 0) {
            List<TooltipLine> lines = new ArrayList<>();
            if (hoveredCategory < categories.size()) {
                lines.add(new TooltipLine(categories.get(hoveredCategory)));
            }
            ChartSeries series = seriesList.get(hoveredSeries);
            lines.add(new TooltipLine(series.getName() + ": "
                    + valueFormatter.apply(series.get(hoveredCategory).getValue()),
                    seriesColor(hoveredSeries, series)));
            requestTooltip(mouseX, mouseY, lines);
        }
    }

    protected void drawBar(float x, float y, float width, float height,
                           boolean positive, ChartColor color, boolean hovered) {
        ChartColor base = hovered ? color.brighter(0.18f) : color;
        float radius = Math.min(cornerRadius, Math.min(width, height) * 0.5f);
        float rightRadius = positive ? radius : 0f;
        float leftRadius = positive ? 0f : radius;

        if (gradientEnabled) {
            ChartColor light = base.brighter(0.10f);
            ChartColor darkColor = base.darker(0.10f);
            float top = y;
            float span = Math.max(1f, height);
            graphics().fillRoundRect(x, y, width, height, leftRadius, rightRadius, rightRadius, leftRadius,
                    (px, py) -> light.lerp(darkColor, Math.max(0f, Math.min(1f, (py - top) / span))));
        } else {
            graphics().fillRoundRect(x, y, width, height, leftRadius, rightRadius, rightRadius, leftRadius,
                    (px, py) -> base);
        }
    }

    private float valueToX(double value, double axisMin, double axisMax, ChartRect plot) {
        double range = axisMax - axisMin;
        if (range <= 0) return plot.x;
        return (float) (plot.x + (value - axisMin) / range * plot.width);
    }

    private double[] computeAxisRange(ChartDataSource ds) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (ChartSeries series : ds.getSeriesList()) {
            for (ChartDataPoint point : series.getPoints()) {
                min = Math.min(min, point.getValue());
                max = Math.max(max, point.getValue());
            }
        }
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
        double step = niceNumber((max - min) / Math.max(1, targetTickCount - 1), true);
        double axisMin = fixedMin != null ? fixedMin : Math.floor(min / step) * step;
        double axisMax = fixedMax != null ? fixedMax : Math.ceil(max / step) * step;
        return new double[]{axisMin, axisMax, step};
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

    private float staggeredProgress(float progress, int index, int count) {
        float stagger = staggerAmount;
        if (stagger <= 0f || count <= 1) return progress;
        float delay = stagger * index / (count - 1);
        return Math.max(0f, Math.min(1f, (progress - delay) / (1f - stagger)));
    }
}
