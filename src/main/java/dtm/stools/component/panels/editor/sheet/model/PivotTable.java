package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record PivotTable(String name, int sourceSheet, CellRange source, CellAddress target, List<PivotField> rows, List<PivotField> columns,
                         List<PivotValueField> values, List<PivotField> filters, List<PivotCalculatedField> calculated, boolean rowGrandTotals,
                         boolean columnGrandTotals, boolean compact, CellRange output) {
    public PivotTable {
        Objects.requireNonNull(name); Objects.requireNonNull(source); Objects.requireNonNull(target);
        rows = rows == null ? List.of() : List.copyOf(rows);
        columns = columns == null ? List.of() : List.copyOf(columns);
        values = values == null ? List.of() : List.copyOf(values);
        filters = filters == null ? List.of() : List.copyOf(filters);
        calculated = calculated == null ? List.of() : List.copyOf(calculated);
    }

    public static PivotTable create(String name, int sourceSheet, CellRange source, CellAddress target) {
        return new PivotTable(name, sourceSheet, source, target, List.of(), List.of(), List.of(), List.of(), List.of(), true, true, true, null);
    }
}
