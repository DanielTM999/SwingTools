package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.util.ArrayList;
import java.util.List;

public class BarChartRender extends CartesianChartRender {

    private volatile float cornerRadius = 5f;
    private volatile float groupGapRatio = 0.3f;
    private volatile float barGapRatio = 0.12f;
    private volatile float staggerAmount = 0.35f;
    private volatile boolean gradientEnabled = true;
    private volatile boolean hoverHighlightEnabled = true;

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

    @Override
    protected void renderPlot(GraphicsGlContext context, ChartRect plot, float progress) {
        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        int categoryCount = getDataSource().maxPointCount();
        int seriesCount = seriesList.size();
        if (categoryCount == 0 || seriesCount == 0) return;

        float slotWidth = plot.width / categoryCount;
        float groupWidth = slotWidth * (1f - groupGapRatio);
        float barSlotWidth = groupWidth / seriesCount;
        float barWidth = Math.max(1f, barSlotWidth * (1f - barGapRatio));
        float baseY = baselineY();

        GraphicsInput input = input(context);
        boolean mouseInside = input.isMouseInside() && plot.contains(input.getMouseX(), input.getMouseY());
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();

        int hoveredCategory = -1;
        int hoveredSeries = -1;

        for (int i = 0; i < categoryCount; i++) {
            float localProgress = staggeredProgress(progress, i, categoryCount);
            if (localProgress <= 0f) continue;
            float groupStart = plot.x + i * slotWidth + (slotWidth - groupWidth) * 0.5f;

            for (int s = 0; s < seriesCount; s++) {
                ChartSeries series = seriesList.get(s);
                if (i >= series.size()) continue;
                double value = series.get(i).getValue();
                if (value == 0d) continue;

                float valueY = valueToY(value);
                float animatedY = baseY + (valueY - baseY) * localProgress;
                float top = Math.min(baseY, animatedY);
                float barHeight = Math.abs(baseY - animatedY);
                if (barHeight < 0.5f) continue;
                float barX = groupStart + s * barSlotWidth + (barSlotWidth - barWidth) * 0.5f;

                boolean hovered = hoverHighlightEnabled && mouseInside
                        && mouseX >= barX && mouseX <= barX + barWidth
                        && mouseY >= top && mouseY <= top + barHeight;
                if (hovered) {
                    hoveredCategory = i;
                    hoveredSeries = s;
                }

                drawBar(barX, top, barWidth, barHeight, value >= 0, seriesColor(s, series), hovered);
            }
        }

        if (hoveredCategory >= 0) {
            requestBarTooltip(context, seriesList, hoveredCategory, hoveredSeries);
        }
    }

    protected void drawBar(float x, float y, float width, float height,
                           boolean positive, ChartColor color, boolean hovered) {
        ChartColor base = hovered ? color.brighter(0.18f) : color;
        float radius = Math.min(cornerRadius, Math.min(width, height) * 0.5f);
        float topRadius = positive ? radius : 0f;
        float bottomRadius = positive ? 0f : radius;

        if (gradientEnabled) {
            ChartColor light = base.brighter(0.10f);
            ChartColor darkColor = base.darker(0.10f);
            float top = y;
            float span = Math.max(1f, height);
            graphics().fillRoundRect(x, y, width, height, topRadius, topRadius, bottomRadius, bottomRadius,
                    (px, py) -> light.lerp(darkColor, Math.max(0f, Math.min(1f, (py - top) / span))));
        } else {
            graphics().fillRoundRect(x, y, width, height, topRadius, topRadius, bottomRadius, bottomRadius,
                    (px, py) -> base);
        }
    }

    protected void requestBarTooltip(GraphicsGlContext context, List<ChartSeries> seriesList,
                                     int categoryIndex, int seriesIndex) {
        List<TooltipLine> lines = new ArrayList<>();
        List<String> categories = getCategories();
        if (categoryIndex < categories.size()) {
            lines.add(new TooltipLine(categories.get(categoryIndex)));
        }
        ChartSeries series = seriesList.get(seriesIndex);
        double value = series.get(categoryIndex).getValue();
        lines.add(new TooltipLine(series.getName() + ": " + getValueFormatter().apply(value),
                seriesColor(seriesIndex, series)));
        GraphicsInput input = input(context);
        requestTooltip(input.getMouseX(), input.getMouseY(), lines);
    }

    private float staggeredProgress(float progress, int index, int count) {
        float stagger = staggerAmount;
        if (stagger <= 0f || count <= 1) return progress;
        float delay = stagger * index / (count - 1);
        float local = (progress - delay) / (1f - stagger);
        return Math.max(0f, Math.min(1f, local));
    }
}
