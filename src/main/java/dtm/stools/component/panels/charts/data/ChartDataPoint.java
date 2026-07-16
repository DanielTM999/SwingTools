package dtm.stools.component.panels.charts.data;

public final class ChartDataPoint {

    private final String label;
    private final double value;

    public ChartDataPoint(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public static ChartDataPoint of(String label, double value) {
        return new ChartDataPoint(label, value);
    }

    public static ChartDataPoint of(double value) {
        return new ChartDataPoint(null, value);
    }

    public String getLabel() {
        return label;
    }

    public double getValue() {
        return value;
    }

    public ChartDataPoint withValue(double newValue) {
        return new ChartDataPoint(label, newValue);
    }

    @Override
    public String toString() {
        return "ChartDataPoint(" + label + " = " + value + ")";
    }
}
