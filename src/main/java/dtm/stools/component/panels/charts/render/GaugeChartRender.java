package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.render.gl.GlTextRenderer;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.awt.Font;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleFunction;

public class GaugeChartRender extends ChartBaseRender {

    public static class ColorStop {
        private final float fraction;
        private final ChartColor color;

        public ColorStop(float fraction, ChartColor color) {
            this.fraction = Math.max(0f, Math.min(1f, fraction));
            this.color = color;
        }

        public float getFraction() { return fraction; }
        public ChartColor getColor() { return color; }
    }

    private final CopyOnWriteArrayList<ColorStop> colorStops = new CopyOnWriteArrayList<>();

    private volatile double minValue = 0d;
    private volatile double maxValue = 100d;
    private volatile float startAngleDegrees = 150f;
    private volatile float sweepDegrees = 240f;
    private volatile float thicknessRatio = 0.16f;
    private volatile boolean roundedCaps = true;
    private volatile boolean valueTextVisible = true;
    private volatile boolean rangeLabelsVisible = true;
    private volatile String unit = "";
    private volatile String valueLabel;
    private volatile ChartColor trackColor;
    private volatile DoubleFunction<String> valueFormatter = ChartBaseRender::formatValue;

    public GaugeChartRender() {
        setLegendVisible(false);
    }

    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }

    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }

    public float getStartAngleDegrees() { return startAngleDegrees; }
    public void setStartAngleDegrees(float startAngleDegrees) { this.startAngleDegrees = startAngleDegrees; }

    public float getSweepDegrees() { return sweepDegrees; }
    public void setSweepDegrees(float sweepDegrees) {
        this.sweepDegrees = Math.max(10f, Math.min(360f, sweepDegrees));
    }

    public float getThicknessRatio() { return thicknessRatio; }
    public void setThicknessRatio(float thicknessRatio) {
        this.thicknessRatio = Math.max(0.02f, Math.min(0.5f, thicknessRatio));
    }

    public boolean isRoundedCaps() { return roundedCaps; }
    public void setRoundedCaps(boolean roundedCaps) { this.roundedCaps = roundedCaps; }

    public boolean isValueTextVisible() { return valueTextVisible; }
    public void setValueTextVisible(boolean valueTextVisible) { this.valueTextVisible = valueTextVisible; }

    public boolean isRangeLabelsVisible() { return rangeLabelsVisible; }
    public void setRangeLabelsVisible(boolean rangeLabelsVisible) { this.rangeLabelsVisible = rangeLabelsVisible; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit != null ? unit : ""; }

    public String getValueLabel() { return valueLabel; }
    public void setValueLabel(String valueLabel) { this.valueLabel = valueLabel; }

    public ChartColor getTrackColor() { return trackColor; }
    public void setTrackColor(ChartColor trackColor) { this.trackColor = trackColor; }

    public DoubleFunction<String> getValueFormatter() { return valueFormatter; }
    public void setValueFormatter(DoubleFunction<String> valueFormatter) {
        this.valueFormatter = valueFormatter != null ? valueFormatter : ChartBaseRender::formatValue;
    }

    public GaugeChartRender addColorStop(float fraction, ChartColor color) {
        colorStops.add(new ColorStop(fraction, color));
        return this;
    }

    public GaugeChartRender clearColorStops() {
        colorStops.clear();
        return this;
    }

    public void setValue(double value) {
        ChartSeries series = getDataSource().series("value");
        if (series.isEmpty()) {
            series.add(value);
        } else {
            series.set(series.size() - 1, value);
        }
    }

    public double getValue() {
        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        if (seriesList.isEmpty() || seriesList.get(0).isEmpty()) return minValue;
        ChartSeries series = seriesList.get(0);
        return series.get(series.size() - 1).getValue();
    }

    @Override
    protected List<LegendEntry> legendEntries() {
        return List.of();
    }

    @Override
    protected void renderChart(GraphicsGlContext context, ChartRect area, float progress) {
        double min = minValue;
        double max = maxValue;
        if (max <= min) return;

        double value = Math.max(min, Math.min(max, getValue()));
        double displayValue = min + (value - min) * progress;
        float fraction = (float) ((displayValue - min) / (max - min));

        float radius = Math.min(area.width, area.height) * 0.5f - 4f;
        if (radius < 12f) return;
        float cx = area.centerX();
        float cy = area.centerY();
        float thickness = Math.max(6f, radius * thicknessRatio);
        float outerRadius = radius;
        float innerRadius = radius - thickness;
        float midRadius = (outerRadius + innerRadius) * 0.5f;
        float capRadius = thickness * 0.5f;

        float start = (float) Math.toRadians(startAngleDegrees);
        float sweep = (float) Math.toRadians(sweepDegrees);
        float valueSweep = sweep * fraction;

        ChartColor track = trackColor != null ? trackColor : getTheme().getGridColor();
        ChartColor color = colorForFraction(fraction);

        graphics().fillArc(cx, cy, innerRadius, outerRadius, start, sweep, track);
        if (roundedCaps) {
            capCircle(cx, cy, midRadius, start, capRadius, track);
            capCircle(cx, cy, midRadius, start + sweep, capRadius, track);
        }

        if (valueSweep > 0.001f) {
            graphics().fillArc(cx, cy, innerRadius, outerRadius, start, valueSweep, color);
            if (roundedCaps) {
                capCircle(cx, cy, midRadius, start, capRadius, color);
                capCircle(cx, cy, midRadius, start + valueSweep, capRadius, color);
            }
        }

        if (valueTextVisible) {
            drawValueText(cx, cy, displayValue);
        }
        if (rangeLabelsVisible) {
            drawRangeLabels(cx, cy, midRadius, capRadius, start, sweep, min, max);
        }
    }

    protected ChartColor colorForFraction(float fraction) {
        ChartColor result = null;
        float bestFraction = -1f;
        for (ColorStop stop : colorStops) {
            if (stop.getFraction() <= fraction && stop.getFraction() > bestFraction) {
                bestFraction = stop.getFraction();
                result = stop.getColor();
            }
        }
        return result != null ? result : getPalette().colorAt(0);
    }

    protected void drawValueText(float cx, float cy, double displayValue) {
        String mainText = valueFormatter.apply(displayValue) + unit;
        Font mainFont = getTheme().getTitleFont();
        drawText(mainText, mainFont, cx, cy, getTheme().getTitleColor(),
                GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.MIDDLE);
        String subText = valueLabel;
        if (subText == null || subText.isEmpty()) return;
        float offset = textRenderer().lineHeight(mainFont) * 0.5f
                + textRenderer().lineHeight(getTheme().getSubtitleFont()) * 0.5f + 2f;
        drawText(subText, getTheme().getSubtitleFont(), cx, cy + offset, getTheme().getSubtitleColor(),
                GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.MIDDLE);
    }

    protected void drawRangeLabels(float cx, float cy, float midRadius, float capRadius,
                                   float start, float sweep, double min, double max) {
        Font labelFont = getTheme().getLabelFont();
        ChartColor textColor = getTheme().getAxisTextColor();
        float labelRadius = midRadius;
        float offset = capRadius + 6f;

        float startX = cx + (float) Math.cos(start) * labelRadius;
        float startY = cy + (float) Math.sin(start) * labelRadius + offset;
        float endX = cx + (float) Math.cos(start + sweep) * labelRadius;
        float endY = cy + (float) Math.sin(start + sweep) * labelRadius + offset;

        drawText(valueFormatter.apply(min), labelFont, startX, startY, textColor,
                GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.TOP);
        drawText(valueFormatter.apply(max), labelFont, endX, endY, textColor,
                GlTextRenderer.HAlign.CENTER, GlTextRenderer.VAlign.TOP);
    }

    private void capCircle(float cx, float cy, float midRadius, float angle,
                           float capRadius, ChartColor color) {
        float x = cx + (float) Math.cos(angle) * midRadius;
        float y = cy + (float) Math.sin(angle) * midRadius;
        graphics().fillCircle(x, y, capRadius, color);
    }
}
