package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record SheetTable(int id, String name, CellRange range, boolean headerRow, boolean totalsRow, String style, List<TableColumn> columns,
                         boolean bandedRows, boolean bandedColumns, boolean firstColumn, boolean lastColumn, boolean filterButton) {
    public SheetTable {
        Objects.requireNonNull(name); Objects.requireNonNull(range);
        style = Objects.requireNonNullElse(style, "TableStyleMedium2");
        columns = List.copyOf(columns);
        if (columns.size() != range.columnCount()) throw new IllegalArgumentException("Table columns do not match range");
    }

    public static SheetTable create(int id, String name, CellRange range, List<String> headers) {
        List<TableColumn> cols = new ArrayList<>();
        for (String h : headers) cols.add(TableColumn.of(h));
        return new SheetTable(id, name, range, true, false, "TableStyleMedium2", cols, true, false, false, false, true);
    }

    public CellRange dataRange() {
        int first = range.firstRow() + (headerRow ? 1 : 0), last = range.lastRow() - (totalsRow ? 1 : 0);
        if (last < first) last = first;
        return new CellRange(first, range.firstColumn(), last, range.lastColumn());
    }

    public CellRange headerRange() { return headerRow ? new CellRange(range.firstRow(), range.firstColumn(), range.firstRow(), range.lastColumn()) : null; }
    public CellRange totalsRange() { return totalsRow ? new CellRange(range.lastRow(), range.firstColumn(), range.lastRow(), range.lastColumn()) : null; }

    public int columnIndex(String columnName) {
        for (int i = 0; i < columns.size(); i++) if (columns.get(i).name().equalsIgnoreCase(columnName)) return i;
        return -1;
    }

    public boolean nameMatches(String n) { return name.toLowerCase(Locale.ROOT).equals(n.toLowerCase(Locale.ROOT)); }
}
