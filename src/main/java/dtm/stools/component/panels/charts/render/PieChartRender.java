package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartDataPoint;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.render.gl.GlTextRenderer;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

public class PieChartRender extends ChartBaseRender {

    private volatile float innerRadiusRatio = 0.6f;
    private volatile float startAngleDegrees = -90f;
    private volatile float sliceGapDegrees = 1.6f;
    private volatile boolean percentLabelsVisible = true;
    private volatile float minPercentForLabel = 0.055f;
    private volatile float hoverExpand = 7f;
    private volatile boolean centerTextVisible = true;
    private volatile String centerText;
    private volatile String centerSubText;
    private volatile DoubleFunction<String> valueFormatter = ChartBaseRender::formatValue;

    public float getInnerRadiusRatio() { return innerRadiusRatio; }
    public void setInnerRadiusRatio(float innerRadiusRatio) {
        this.innerRadiusRatio = Math.max(0f, Math.min(0.95f, innerRadiusRatio));
    }

    public float getStartAngleDegrees() { return startAngleDegrees; }
    public void setStartAngleDegrees(float startAngleDegrees) { this.startAngleDegrees = startAngleDegrees; }

    public float getSliceGapDegrees() { return sliceGapDegrees; }
    public void setSliceGapDegrees(float sliceGapDegrees) {
        this.sliceGapDegrees = Math.max(0f, Math.min(12f, sliceGapDegrees));
    }

    public boolean isPercentLabelsVisible() { return percentLabelsVisible; }
    public void setPercentLabelsVisible(boolean percentLabelsVisible) { this.percentLabelsVisible = percentLabelsVisible; }

    public float getMinPercentForLabel() { return minPercentForLabel; }
    public void setMinPercentForLabel(float minPercentForLabel) {
        this.minPercentForLabel = Math.max(0f, Math.min(1f, minPercentForLabel));
    }

    public float getHoverExpand() { return hoverExpand; }
    public void setHoverExpand(float hoverExpand) { this.hoverExpand = Math.max(0f, hoverExpand); }

    public boolean isCenterTextVisible() { return centerTextVisible; }
    public void setCenterTextVisible(boolean centerTextVisible) { this.centerTextVisible = centerTextVisible; }

    public String getCenterText() { return centerText; }
    public void setCenterText(String centerText) { this.centerText = centerText; }

    public String getCenterSubText() { return centerSubText; }
    public void setCenterSubText(String centerSubText) { this.centerSubText = centerSubText; }

    public DoubleFunction<String> getValueFormatter() { return valueFormatter; }
    public void setValueFormatter(DoubleFunction<String> valueFormatter) {
        this.valueFormatter = valueFormatter != null ? valueFormatter : ChartBaseRender::formatValue;
    }

    @Override
    protected List<LegendEntry> legendEntries() {
        List<LegendEntry> entries = new ArrayList<>();
        List<ChartDataPoint> slices = slices();
        for (int i = 0; i < slices.size(); i++) {
            String label = slices.get(i).getLabel();
            entries.add(new LegendEntry(label != null ? label : "Item " + (i + 1),
                    sliceColor(i)));
        }
        return entries;
    }

    @Override
    protected void renderChart(GraphicsGlContext context, ChartRect area, float progress) {
        List<ChartDataPoint> slices = slices();
        if (slices.isEmpty()) return;
        double total = 0;
        for (ChartDataPoint slice : slices) total += slice.getValue();
        if (total <= 0) return;

        float cx = area.centerX();
        float cy = area.centerY();
        float outerRadius = Math.min(area.width, area.height) * 0.5f - hoverExpand - 2f;
        if (outerRadius < 8f) return;
        float innerRadius = outerRadius * innerRadiusRatio;

        GraphicsInput input = input(context);
        int hoveredSlice = hoveredSlice(input, slices, total, cx, cy, innerRadius, outerRadius);

        float gap = (float) Math.toRadians(sliceGapDegrees);
        float startBase = (float) Math.toRadians(startAngleDegrees);
        float revealSweep = (float) (Math.PI * 2.0) * progress;

        float cursor = 0f;
        for (int i = 0; i < slices.size(); i++) {
            double value = slices.get(i).getValue();
            float sweep = (float) (value / total * Math.PI * 2.0);

            float visibleStart = cursor;
            float visibleEnd = Math.min(cursor + sweep, revealSweep);
            cursor += sweep;
            float visibleSweep = visibleEnd - visibleStart;
            if (visibleSweep <= 0f) continue;

            float sliceGap = sweep > gap * 3f ? gap : 0f;
            float drawStart = startBase + visibleStart + sliceGap * 0.5f;
            float drawSweep = Math.max(0.004f, visibleSweep - sliceGap);

            boolean hovered = i == hoveredSlice;
            float radius = hovered ? outerRadius + hoverExpand : outerRadius;
            ChartColor color = hovered ? sliceColor(i).brighter(0.12f) : sliceColor(i);
            graphics().fillArc(cx, cy, innerRadius, radius, drawStart, drawSweep, color);

            if (percentLabelsVisible && progress >= 1f && sweep / (Math.PI * 2f) >= minPercentForLabel) {
                drawPercentLabel(cx, cy, innerRadius, radius, drawStart + drawSweep * 0.5f,
                        (float) (value / total));
            }
        }

        if (centerTextVisible && innerRadiusRatio > 0.25f) {
            drawCenterText(cx, cy, total);
        }

        if (hoveredSlice >= 0) {
            requestSliceTooltip(context, slices.get(hoveredSlice), hoveredSlice, total);
        }
    }

    protected ChartColor sliceColor(int index) {
        return getPalette().colorAt(index);
    }

    protected void drawPercentLabel(float cx, float cy, float innerRadius, float outerRadius,
                                    float midAngle, float fraction) {
        float labelRadius = innerRadius > 0f ? (innerRadius + outerRadius) * 0.5f : outerRadius * 0.62f;
        float x = cx + (float) Math.cos(midAngle) * labelRadius;
        float y = cy + (float) Math.sin(midAngle) * labelRadius;
        String label = Math.round(fraction * 100f) + "%";
        drawText(label, getTheme().getLabelFont(), x, y, ChartColor.WHITE.withAlpha(0.94f),
                GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.MIDDLE);
    }

    protected void drawCenterText(float cx, float cy, double total) {
        String mainText = centerText != null ? centerText : valueFormatter.apply(total);
        String subText = centerSubText != null ? centerSubText : "Total";
        Font mainFont = getTheme().getTitleFont();
        Font subFont = getTheme().getSubtitleFont();
        float mainHeight = textRenderer().lineHeight(mainFont);
        float subHeight = textRenderer().lineHeight(subFont);
        float blockHeight = mainHeight + 2f + subHeight;
        drawText(mainText, mainFont, cx, cy - blockHeight * 0.5f + mainHeight * 0.5f,
                getTheme().getTitleColor(), GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.MIDDLE);
        drawText(subText, subFont, cx, cy + blockHeight * 0.5f - subHeight * 0.5f,
                getTheme().getSubtitleColor(), GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.MIDDLE);
    }

    protected void requestSliceTooltip(GraphicsGlContext context, ChartDataPoint slice,
                                       int index, double total) {
        List<TooltipLine> lines = new ArrayList<>();
        String label = slice.getLabel() != null ? slice.getLabel() : "Item " + (index + 1);
        double percent = slice.getValue() / total * 100.0;
        lines.add(new TooltipLine(label, sliceColor(index)));
        lines.add(new TooltipLine(valueFormatter.apply(slice.getValue())
                + "  (" + String.format(java.util.Locale.US, "%.1f", percent) + "%)"));
        GraphicsInput input = input(context);
        requestTooltip(input.getMouseX(), input.getMouseY(), lines);
    }

    private List<ChartDataPoint> slices() {
        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        if (seriesList.isEmpty()) return List.of();
        List<ChartDataPoint> result = new ArrayList<>();
        for (ChartDataPoint point : seriesList.get(0).getPoints()) {
            if (point.getValue() > 0) {
                result.add(point);
            }
        }
        return result;
    }

    private int hoveredSlice(GraphicsInput input, List<ChartDataPoint> slices, double total,
                             float cx, float cy, float innerRadius, float outerRadius) {
        if (!input.isMouseInside()) return -1;
        float dx = input.getMouseX() - cx;
        float dy = input.getMouseY() - cy;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < innerRadius || distance > outerRadius + hoverExpand) return -1;

        double angle = Math.atan2(dy, dx) - Math.toRadians(startAngleDegrees);
        double twoPi = Math.PI * 2.0;
        angle = ((angle % twoPi) + twoPi) % twoPi;

        double cursor = 0;
        for (int i = 0; i < slices.size(); i++) {
            double sweep = slices.get(i).getValue() / total * twoPi;
            if (angle >= cursor && angle < cursor + sweep) return i;
            cursor += sweep;
        }
        return -1;
    }
}
