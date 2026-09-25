package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.PivotAggregation;
import dtm.stools.component.panels.editor.sheet.model.PivotCalculatedField;
import dtm.stools.component.panels.editor.sheet.model.PivotField;
import dtm.stools.component.panels.editor.sheet.model.PivotGrouping;
import dtm.stools.component.panels.editor.sheet.model.PivotShowAs;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PivotValueField;

import java.util.List;
import java.util.Set;

public record PivotJson(String name, int sourceSheet, String source, String target, List<Field> rows, List<Field> columns, List<Value> values, List<Field> filters,
                        List<Calc> calculated, boolean rowTotals, boolean columnTotals, boolean compact, String output) {
    public record Field(String name, String grouping, double size, boolean descending, Set<String> hidden) {}
    public record Value(String field, String aggregation, String showAs, String caption, String format) {}
    public record Calc(String name, String formula) {}

    public static PivotJson from(PivotTable p) {
        return new PivotJson(p.name(), p.sourceSheet(), p.source().toA1(), p.target().toA1(), fields(p.rows()), fields(p.columns()),
                p.values().stream().map(v -> new Value(v.field(), v.aggregation().name(), v.showAs().name(), v.caption(), v.numberFormat())).toList(),
                fields(p.filters()), p.calculated().stream().map(c -> new Calc(c.name(), c.formula())).toList(), p.rowGrandTotals(), p.columnGrandTotals(), p.compact(),
                p.output() == null ? null : p.output().toA1());
    }

    private static List<Field> fields(List<PivotField> list) {
        return list.stream().map(f -> new Field(f.name(), f.grouping().name(), f.groupSize(), f.descending(), f.hiddenItems())).toList();
    }

    public PivotTable toModel() {
        return new PivotTable(name, sourceSheet, CellRange.parse(source), CellAddress.parse(target), model(rows), model(columns),
                values == null ? List.of() : values.stream().map(v -> new PivotValueField(v.field(), PivotAggregation.valueOf(v.aggregation()), PivotShowAs.valueOf(v.showAs()), v.caption(), v.format())).toList(),
                model(filters), calculated == null ? List.of() : calculated.stream().map(c -> new PivotCalculatedField(c.name(), c.formula())).toList(),
                rowTotals, columnTotals, compact, output == null ? null : CellRange.parse(output));
    }

    private static List<PivotField> model(List<Field> list) {
        if (list == null) return List.of();
        return list.stream().map(f -> new PivotField(f.name(), PivotGrouping.valueOf(f.grouping()), f.size(), f.descending(), f.hidden())).toList();
    }
}
