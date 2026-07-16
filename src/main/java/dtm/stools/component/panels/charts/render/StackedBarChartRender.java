package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.util.ArrayList;
import java.util.List;

public class StackedBarChartRender extends CartesianChartRender {

    private volatile float cornerRadius = 5f;
    private volatile float barWidthRatio = 0.55f;
    private volatile float segmentGap = 1f;
    private volatile float staggerAmount = 0.3f;
    private volatile boolean gradientEnabled = true;
    private volatile boolean hoverHighlightEnabled = true;
    private volatile boolean totalInTooltip = true;

    public float getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(float cornerRadius) { this.cornerRadius = Math.max(0f, cornerRadius); }

    public float getBarWidthRatio() { return barWidthRatio; }
    public void setBarWidthRatio(float barWidthRatio) {
        this.barWidthRatio = Math.max(0.05f, Math.min(1f, barWidthRatio));
    }

    public float getSegmentGap() { return segmentGap; }
    public void setSegmentGap(float segmentGap) { this.segmentGap = Math.max(0f, segmentGap); }

    public float getStaggerAmount() { return staggerAmount; }
    public void setStaggerAmount(float staggerAmount) {
        this.staggerAmount = Math.max(0f, Math.min(0.9f, staggerAmount));
    }

    public boolean isGradientEnabled() { return gradientEnabled; }
    public void setGradientEnabled(boolean gradientEnabled) { this.gradientEnabled = gradientEnabled; }

    public boolean isHoverHighlightEnabled() { return hoverHighlightEnabled; }
    public void setHoverHighlightEnabled(boolean hoverHighlightEnabled) { this.hoverHighlightEnabled = hoverHighlightEnabled; }

    public boolean isTotalInTooltip() { return totalInTooltip; }
    public void setTotalInTooltip(boolean totalInTooltip) { this.totalInTooltip = totalInTooltip; }

    @Override
    protected double[] computeDataRange(ChartDataSource ds) {
        List<ChartSeries> seriesList = ds.getSeriesList();
        int categoryCount = ds.maxPointCount();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < categoryCount; i++) {
            double positiveSum = 0;
            double negativeSum = 0;
            for (ChartSeries series : seriesList) {
                if (i >= series.size()) continue;
                double value = series.get(i).getValue();
                if (value >= 0) positiveSum += value;
                else negativeSum += value;
            }
            max = Math.max(max, positiveSum);
            min = Math.min(min, negativeSum);
        }
        return new double[]{min, max};
    }

    @Override
    protected void renderPlot(GraphicsGlContext context, ChartRect plot, float progress) {
        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        int categoryCount = getDataSource().maxPointCount();
        int seriesCount = seriesList.size();
        if (categoryCount == 0 || seriesCount == 0) return;

        float slotWidth = plot.width / categoryCount;
        float barWidth = Math.max(1f, slotWidth * barWidthRatio);
        float baseY = baselineY();

        GraphicsInput input = input(context);
        boolean mouseInside = hoverHighlightEnabled && input.isMouseInside()
                && plot.contains(input.getMouseX(), input.getMouseY());
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();

        int hoveredCategory = -1;
        int hoveredSeries = -1;

        for (int i = 0; i < categoryCount; i++) {
            float localProgress = staggeredProgress(progress, i, categoryCount);
            if (localProgress <= 0f) continue;
            float barX = plot.x + i * slotWidth + (slotWidth - barWidth) * 0.5f;

            int lastPositive = -1;
            int lastNegative = -1;
            for (int s = 0; s < seriesCount; s++) {
                if (i >= seriesList.get(s).size()) continue;
                double value = seriesList.get(s).get(i).getValue();
                if (value > 0) lastPositive = s;
                else if (value < 0) lastNegative = s;
            }

            double positiveCursor = 0;
            double negativeCursor = 0;
            for (int s = 0; s < seriesCount; s++) {
                ChartSeries series = seriesList.get(s);
                if (i >= series.size()) continue;
                double value = series.get(i).getValue();
                if (value == 0d) continue;

                boolean positive = value > 0;
                double from = positive ? positiveCursor : negativeCursor;
                double to = from + value;
                if (positive) positiveCursor = to;
                else negativeCursor = to;

                float yFrom = baseY + (valueToY(from) - baseY) * localProgress;
                float yTo = baseY + (valueToY(to) - baseY) * localProgress;
                float top = Math.min(yFrom, yTo);
                float height = Math.abs(yFrom - yTo);

                boolean outermost = positive ? s == lastPositive : s == lastNegative;
                if (!outermost && height > segmentGap + 1f) {
                    if (positive) {
                        top += segmentGap;
                        height -= segmentGap;
                    } else {
                        height -= segmentGap;
                    }
                }
                if (height < 0.5f) continue;

                boolean hovered = mouseInside
                        && mouseX >= barX && mouseX <= barX + barWidth
                        && mouseY >= top && mouseY <= top + height;
                if (hovered) {
                    hoveredCategory = i;
                    hoveredSeries = s;
                }

                float radius = outermost ? Math.min(cornerRadius, Math.min(barWidth, height) * 0.5f) : 0f;
                float topRadius = positive ? radius : 0f;
                float bottomRadius = positive ? 0f : radius;
                drawSegment(barX, top, barWidth, height, topRadius, bottomRadius,
                        seriesColor(s, series), hovered);
            }
        }

        if (hoveredCategory >= 0) {
            requestStackTooltip(context, seriesList, hoveredCategory, hoveredSeries);
        }
    }

    protected void drawSegment(float x, float y, float width, float height,
                               float topRadius, float bottomRadius,
                               ChartColor color, boolean hovered) {
        ChartColor base = hovered ? color.brighter(0.18f) : color;
        if (gradientEnabled) {
            ChartColor light = base.brighter(0.08f);
            ChartColor darkColor = base.darker(0.08f);
            float top = y;
            float span = Math.max(1f, height);
            graphics().fillRoundRect(x, y, width, height, topRadius, topRadius, bottomRadius, bottomRadius,
                    (px, py) -> light.lerp(darkColor, Math.max(0f, Math.min(1f, (py - top) / span))));
        } else {
            graphics().fillRoundRect(x, y, width, height, topRadius, topRadius, bottomRadius, bottomRadius,
                    (px, py) -> base);
        }
    }

    protected void requestStackTooltip(GraphicsGlContext context, List<ChartSeries> seriesList,
                                       int categoryIndex, int seriesIndex) {
        List<TooltipLine> lines = new ArrayList<>();
        List<String> categories = getCategories();
        if (categoryIndex < categories.size()) {
            lines.add(new TooltipLine(categories.get(categoryIndex)));
        }
        ChartSeries hovered = seriesList.get(seriesIndex);
        lines.add(new TooltipLine(hovered.getName() + ": "
                + getValueFormatter().apply(hovered.get(categoryIndex).getValue()),
                seriesColor(seriesIndex, hovered)));
        if (totalInTooltip) {
            double total = 0;
            for (ChartSeries series : seriesList) {
                if (categoryIndex < series.size()) {
                    total += series.get(categoryIndex).getValue();
                }
            }
            lines.add(new TooltipLine("Total: " + getValueFormatter().apply(total)));
        }
        GraphicsInput input = input(context);
        requestTooltip(input.getMouseX(), input.getMouseY(), lines);
    }

    private float staggeredProgress(float progress, int index, int count) {
        float stagger = staggerAmount;
        if (stagger <= 0f || count <= 1) return progress;
        float delay = stagger * index / (count - 1);
        return Math.max(0f, Math.min(1f, (progress - delay) / (1f - stagger)));
    }
}
