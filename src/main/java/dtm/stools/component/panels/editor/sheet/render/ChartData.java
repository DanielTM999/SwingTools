package dtm.stools.component.panels.editor.sheet.render;

import java.util.List;

public record ChartData(List<String> categories, List<Series> series) {
    public record Series(String name, double[] values, double[] x, double[] sizes, Integer color, dtm.stools.component.panels.editor.sheet.model.ChartType type) {}

    public ChartData { categories = List.copyOf(categories); series = List.copyOf(series); }

    public int pointCount() {
        int n = categories.size();
        for (Series s : series) n = Math.max(n, s.values().length);
        return n;
    }
}
