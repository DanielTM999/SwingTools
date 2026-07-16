package dtm.stools.component.panels.charts.data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ChartDataSource {

    private final CopyOnWriteArrayList<ChartSeries> series = new CopyOnWriteArrayList<>();
    private final AtomicLong version = new AtomicLong();

    public static ChartDataSource of(ChartSeries... series) {
        ChartDataSource dataSource = new ChartDataSource();
        for (ChartSeries s : series) {
            dataSource.addSeries(s);
        }
        return dataSource;
    }

    public ChartDataSource addSeries(ChartSeries newSeries) {
        Objects.requireNonNull(newSeries, "series cannot be null");
        newSeries.setChangeListener(this::touch);
        series.add(newSeries);
        touch();
        return this;
    }

    public ChartSeries addSeries(String name, Map<String, ? extends Number> values) {
        return series(name).setData(values);
    }

    public ChartSeries series(String name) {
        for (ChartSeries s : series) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        ChartSeries created = new ChartSeries(name);
        addSeries(created);
        return created;
    }

    public ChartSeries getSeries(int index) {
        return series.get(index);
    }

    public List<ChartSeries> getSeriesList() {
        return List.copyOf(series);
    }

    public boolean removeSeries(ChartSeries toRemove) {
        boolean removed = series.remove(toRemove);
        if (removed) {
            toRemove.setChangeListener(null);
            touch();
        }
        return removed;
    }

    public boolean removeSeries(String name) {
        for (ChartSeries s : series) {
            if (s.getName().equals(name)) {
                return removeSeries(s);
            }
        }
        return false;
    }

    public ChartDataSource clear() {
        for (ChartSeries s : series) {
            s.setChangeListener(null);
        }
        series.clear();
        touch();
        return this;
    }

    public int seriesCount() {
        return series.size();
    }

    public boolean isEmpty() {
        if (series.isEmpty()) return true;
        for (ChartSeries s : series) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    public int maxPointCount() {
        int max = 0;
        for (ChartSeries s : series) {
            max = Math.max(max, s.size());
        }
        return max;
    }

    public long getVersion() {
        return version.get();
    }

    public void touch() {
        version.incrementAndGet();
    }
}
