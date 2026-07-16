package dtm.stools.component.panels.charts.data;

import dtm.stools.component.panels.charts.style.ChartColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChartSeries {

    private final String name;
    private final CopyOnWriteArrayList<ChartDataPoint> points = new CopyOnWriteArrayList<>();
    private volatile ChartColor color;
    private volatile Runnable changeListener;

    public ChartSeries(String name) {
        this.name = Objects.requireNonNull(name, "series name cannot be null");
    }

    public static ChartSeries of(String name, double... values) {
        ChartSeries series = new ChartSeries(name);
        for (double value : values) {
            series.points.add(ChartDataPoint.of(value));
        }
        return series;
    }

    public static ChartSeries of(String name, Map<String, ? extends Number> values) {
        ChartSeries series = new ChartSeries(name);
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            series.points.add(ChartDataPoint.of(entry.getKey(), entry.getValue().doubleValue()));
        }
        return series;
    }

    public String getName() {
        return name;
    }

    public ChartColor getColor() {
        return color;
    }

    public ChartSeries setColor(ChartColor color) {
        this.color = color;
        notifyChanged();
        return this;
    }

    public ChartSeries add(double value) {
        points.add(ChartDataPoint.of(value));
        notifyChanged();
        return this;
    }

    public ChartSeries add(String label, double value) {
        points.add(ChartDataPoint.of(label, value));
        notifyChanged();
        return this;
    }

    public ChartSeries add(ChartDataPoint point) {
        points.add(Objects.requireNonNull(point, "point cannot be null"));
        notifyChanged();
        return this;
    }

    public ChartSeries addAll(Collection<ChartDataPoint> newPoints) {
        points.addAll(newPoints);
        notifyChanged();
        return this;
    }

    public ChartSeries addAll(Map<String, ? extends Number> values) {
        List<ChartDataPoint> newPoints = new ArrayList<>(values.size());
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            newPoints.add(ChartDataPoint.of(entry.getKey(), entry.getValue().doubleValue()));
        }
        points.addAll(newPoints);
        notifyChanged();
        return this;
    }

    public ChartSeries setData(Map<String, ? extends Number> values) {
        List<ChartDataPoint> newPoints = new ArrayList<>(values.size());
        for (Map.Entry<String, ? extends Number> entry : values.entrySet()) {
            newPoints.add(ChartDataPoint.of(entry.getKey(), entry.getValue().doubleValue()));
        }
        points.clear();
        points.addAll(newPoints);
        notifyChanged();
        return this;
    }

    public ChartSeries set(int index, double value) {
        points.set(index, points.get(index).withValue(value));
        notifyChanged();
        return this;
    }

    public ChartSeries set(int index, ChartDataPoint point) {
        points.set(index, Objects.requireNonNull(point, "point cannot be null"));
        notifyChanged();
        return this;
    }

    public ChartSeries remove(int index) {
        points.remove(index);
        notifyChanged();
        return this;
    }

    public ChartSeries removeFirst() {
        if (!points.isEmpty()) {
            points.remove(0);
            notifyChanged();
        }
        return this;
    }

    public ChartSeries setValues(double... values) {
        List<ChartDataPoint> current = List.copyOf(points);
        points.clear();
        for (int i = 0; i < values.length; i++) {
            String label = i < current.size() ? current.get(i).getLabel() : null;
            points.add(ChartDataPoint.of(label, values[i]));
        }
        notifyChanged();
        return this;
    }

    public ChartSeries clear() {
        points.clear();
        notifyChanged();
        return this;
    }

    public ChartDataPoint get(int index) {
        return points.get(index);
    }

    public int size() {
        return points.size();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public List<ChartDataPoint> getPoints() {
        return List.copyOf(points);
    }

    public double sum() {
        double total = 0;
        for (ChartDataPoint point : points) {
            total += point.getValue();
        }
        return total;
    }

    void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    private void notifyChanged() {
        Runnable listener = changeListener;
        if (listener != null) {
            listener.run();
        }
    }
}
