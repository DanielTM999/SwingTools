package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;
import java.util.Map;

public record SparklineValue(List<Double> values, Map<String, String> options) implements CellValue {
    public SparklineValue { values = List.copyOf(values); options = Map.copyOf(options); }

    public String option(String key, String fallback) { return options.getOrDefault(key, fallback); }
    @Override public String toString() { return ""; }
}
