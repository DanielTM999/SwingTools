package dtm.stools.component.panels.editor.word.model;

import java.util.List;
import java.util.Objects;

public record WordChartSeries(String name, List<Double> values, Integer color) {
    public WordChartSeries {
        Objects.requireNonNull(name);
        values = List.copyOf(values);
        for (Double v : values) if (Double.isInfinite(v)) throw new IllegalArgumentException("Chart values must be finite");
        if (color != null) color &= 0xffffff;
    }
    public WordChartSeries withName(String value) { return new WordChartSeries(value,values,color); }
    public WordChartSeries withValues(List<Double> value) { return new WordChartSeries(name,value,color); }
    public WordChartSeries withColor(Integer value) { return new WordChartSeries(name,values,value); }
    public double value(int index) { return index < values.size() ? values.get(index) : Double.NaN; }
}
