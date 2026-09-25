package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@With
@Builder(toBuilder = true)
public record SheetChart(String id, ChartType type, ObjectAnchor anchor, String title, List<ChartSeries> series, LegendPosition legend,
                         String xAxisTitle, String yAxisTitle, boolean dataLabels, boolean gridlines, int styleIndex, Double minimum, Double maximum) implements SheetObject {
    public SheetChart {
        id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        type = Objects.requireNonNullElse(type, ChartType.COLUMN);
        Objects.requireNonNull(anchor);
        title = Objects.requireNonNullElse(title, "");
        series = List.copyOf(series);
        legend = Objects.requireNonNullElse(legend, LegendPosition.BOTTOM);
        xAxisTitle = Objects.requireNonNullElse(xAxisTitle, "");
        yAxisTitle = Objects.requireNonNullElse(yAxisTitle, "");
    }

    @Override public SheetChart withAnchor(ObjectAnchor a) { return toBuilder().anchor(a).build(); }
    @Override public String description() { return title.isBlank() ? type.label() : title; }
}
