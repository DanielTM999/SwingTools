package dtm.stools.component.panels.charts2d;

import dtm.stools.component.panels.charts.style.ChartColor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Gauge2DRender extends Chart2DRender {

    public record ColorStop(float fraction, ChartColor color) {
    }

    private double minValue = 0d;
    private double maxValue = 100d;
    private double value;
    private String unit = "";
    private String valueLabel;
    private boolean valueTextVisible = true;
    private boolean rangeLabelsVisible = true;
    private float startAngleDegrees = 210f;
    private float sweepDegrees = 240f;
    private float thicknessRatio = 0.16f;
    private float sizeRatio = 1f;
    private ChartColor trackColor;
    private final List<ColorStop> colorStops = new ArrayList<>();

    public Gauge2DRender() {
        setLegendVisible(false);
        setValueFormatter(value -> String.format(Locale.ROOT, "%.1f", value));
    }

    public double getMinValue() {
        return minValue;
    }

    public Gauge2DRender setMinValue(double minValue) {
        this.minValue = minValue;
        bumpRevision();
        return this;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public Gauge2DRender setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        bumpRevision();
        return this;
    }

    public double getValue() {
        return value;
    }

    public Gauge2DRender setValue(double value) {
        this.value = value;
        bumpRevision();
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public Gauge2DRender setUnit(String unit) {
        this.unit = unit == null ? "" : unit;
        bumpRevision();
        return this;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public Gauge2DRender setValueLabel(String valueLabel) {
        this.valueLabel = valueLabel;
        bumpRevision();
        return this;
    }

    public Gauge2DRender setValueTextVisible(boolean valueTextVisible) {
        this.valueTextVisible = valueTextVisible;
        bumpRevision();
        return this;
    }

    public Gauge2DRender setRangeLabelsVisible(boolean rangeLabelsVisible) {
        this.rangeLabelsVisible = rangeLabelsVisible;
        bumpRevision();
        return this;
    }

    public Gauge2DRender setStartAngleDegrees(float startAngleDegrees) {
        this.startAngleDegrees = startAngleDegrees;
        bumpRevision();
        return this;
    }

    public Gauge2DRender setSweepDegrees(float sweepDegrees) {
        this.sweepDegrees = Math.max(10f, Math.min(360f, sweepDegrees));
        bumpRevision();
        return this;
    }

    public Gauge2DRender setThicknessRatio(float thicknessRatio) {
        this.thicknessRatio = Math.max(0.02f, Math.min(0.5f, thicknessRatio));
        bumpRevision();
        return this;
    }

    public float getSizeRatio() {
        return sizeRatio;
    }

    public Gauge2DRender setSizeRatio(float sizeRatio) {
        this.sizeRatio = Math.max(0.1f, Math.min(1f, sizeRatio));
        bumpRevision();
        return this;
    }

    public ChartColor getTrackColor() {
        return trackColor;
    }

    public Gauge2DRender setTrackColor(ChartColor trackColor) {
        this.trackColor = trackColor;
        bumpRevision();
        return this;
    }

    public Gauge2DRender addColorStop(float fraction, ChartColor color) {
        colorStops.add(new ColorStop(Math.max(0f, Math.min(1f, fraction)), color));
        colorStops.sort(Comparator.comparingDouble(ColorStop::fraction));
        bumpRevision();
        return this;
    }

    public Gauge2DRender clearColorStops() {
        colorStops.clear();
        bumpRevision();
        return this;
    }

    @Override
    public void paint(Graphics2D g, int width, int height) {
        paintBackground(g, width, height);

        int top = paintTitle(g, Math.round(getPadding()));
        float pad = getPadding();

        float availableWidth = width - 2 * pad;
        float availableHeight = height - top - pad;
        if (availableWidth <= 4 || availableHeight <= 4) {
            return;
        }

        float[] bounds = arcUnitBounds();
        float xExtent = Math.max(0.001f, bounds[2] - bounds[0]);
        float yExtent = Math.max(0.001f, bounds[3] - bounds[1]);
        float centerOffsetX = (bounds[0] + bounds[2]) / 2f;
        float centerOffsetY = (bounds[1] + bounds[3]) / 2f;

        float strokeSpan = 2f * thicknessRatio;
        float radius = Math.min(
                availableWidth / (xExtent + strokeSpan),
                availableHeight / (yExtent + strokeSpan)
        ) * sizeRatio;
        if (radius <= 2f) {
            return;
        }

        float centerX = pad + availableWidth / 2f - centerOffsetX * radius;
        float centerY = top + availableHeight / 2f - centerOffsetY * radius;
        float diameter = radius * 2f;
        float arcX = centerX - radius;
        float arcY = centerY - radius;

        float stroke = Math.max(6f, diameter * thicknessRatio);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Color track = toAwt(trackColor, toAwt(getTheme().getGridColor(), new Color(0x33, 0x38, 0x42)));
        g.setColor(track);
        g.draw(new Arc2D.Float(arcX, arcY, diameter, diameter, startAngleDegrees, -sweepDegrees, Arc2D.OPEN));

        double span = maxValue - minValue;
        double ratio = span <= 0 ? 0 : (value - minValue) / span;
        ratio = Math.max(0d, Math.min(1d, ratio));

        if (ratio > 0) {
            g.setColor(resolveColor((float) ratio));
            g.draw(new Arc2D.Float(arcX, arcY, diameter, diameter, startAngleDegrees, (float) (-sweepDegrees * ratio), Arc2D.OPEN));
        }

        if (valueTextVisible) {
            Color textColor = toAwt(getTheme().getTitleColor(), Color.LIGHT_GRAY);
            g.setColor(textColor);
            Font valueFont = deriveFont(g, Math.max(14f, radius * 0.34f), Font.BOLD);
            g.setFont(valueFont);
            String valueText = formatValue(value) + (unit == null ? "" : unit);
            FontMetrics metrics = g.getFontMetrics();
            int textX = Math.round(centerX - metrics.stringWidth(valueText) / 2f);
            int textY = Math.round(centerY + metrics.getAscent() / 2f - metrics.getDescent());
            g.drawString(valueText, textX, textY);

            if (valueLabel != null && !valueLabel.isBlank()) {
                g.setColor(toAwt(getTheme().getAxisTextColor(), Color.GRAY));
                g.setFont(deriveFont(g, Math.max(9f, radius * 0.16f), Font.PLAIN));
                FontMetrics labelMetrics = g.getFontMetrics();
                int labelX = Math.round(centerX - labelMetrics.stringWidth(valueLabel) / 2f);
                g.drawString(valueLabel, labelX, Math.round(centerY - radius * 0.32f));
            }
        }

        if (rangeLabelsVisible) {
            g.setColor(toAwt(getTheme().getAxisTextColor(), Color.GRAY));
            g.setFont(deriveFont(g, Math.max(9f, radius * 0.14f), Font.PLAIN));
            FontMetrics metrics = g.getFontMetrics();
            String minText = formatValue(minValue);
            String maxText = formatValue(maxValue);
            g.drawString(minText, Math.round(arcX), Math.round(centerY + radius * 0.55f + metrics.getAscent()));
            g.drawString(maxText, Math.round(arcX + diameter - metrics.stringWidth(maxText)),
                    Math.round(centerY + radius * 0.55f + metrics.getAscent()));
        }
    }

    private Color resolveColor(float ratio) {
        if (colorStops.isEmpty()) {
            return toAwt(getTheme().getTitleColor(), new Color(0x4A, 0xDE, 0x80));
        }
        ColorStop resolved = colorStops.get(0);
        for (ColorStop stop : colorStops) {
            if (ratio >= stop.fraction()) {
                resolved = stop;
            }
        }
        return resolved.color() == null ? Color.GRAY : resolved.color().toAwt();
    }

    private float[] arcUnitBounds() {
        float start = startAngleDegrees;
        float end = startAngleDegrees - sweepDegrees;
        float lo = Math.min(start, end);
        float hi = Math.max(start, end);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        List<Float> angles = new ArrayList<>();
        angles.add(lo);
        angles.add(hi);
        float first = (float) (Math.ceil(lo / 90f) * 90f);
        for (float angle = first; angle <= hi; angle += 90f) {
            angles.add(angle);
        }

        for (float angle : angles) {
            double radians = Math.toRadians(angle);
            float x = (float) Math.cos(radians);
            float y = (float) -Math.sin(radians);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        return new float[]{minX, minY, maxX, maxY};
    }

    private static Font deriveFont(Graphics2D g, float size, int style) {
        Font base = g.getFont();
        return base.deriveFont(style, size);
    }
}
