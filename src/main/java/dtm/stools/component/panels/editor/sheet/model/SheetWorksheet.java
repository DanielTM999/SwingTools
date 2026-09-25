package dtm.stools.component.panels.editor.sheet.model;

import dtm.stools.component.panels.editor.sheet.store.AxisIndex;
import dtm.stools.component.panels.editor.sheet.store.CellStore;

import java.util.Objects;
import java.util.UUID;

public final class SheetWorksheet {
    public static final int DEFAULT_ROW_HEIGHT = 20;
    public static final int DEFAULT_COLUMN_WIDTH = 72;

    private final String id;
    private CellStore cells;
    private AxisIndex rows;
    private AxisIndex columns;
    private SheetProperties properties;

    public SheetWorksheet(String name) {
        this(UUID.randomUUID().toString(), new CellStore(), new AxisIndex(CellAddress.MAX_ROWS, DEFAULT_ROW_HEIGHT), new AxisIndex(CellAddress.MAX_COLUMNS, DEFAULT_COLUMN_WIDTH), SheetProperties.named(name));
    }

    public SheetWorksheet(String id, CellStore cells, AxisIndex rows, AxisIndex columns, SheetProperties properties) {
        this.id = Objects.requireNonNull(id);
        this.cells = Objects.requireNonNull(cells);
        this.rows = Objects.requireNonNull(rows);
        this.columns = Objects.requireNonNull(columns);
        this.properties = Objects.requireNonNull(properties);
    }

    public String id() { return id; }
    public String name() { return properties.name(); }
    public CellStore cells() { return cells; }
    public AxisIndex rows() { return rows; }
    public AxisIndex columns() { return columns; }
    public SheetProperties properties() { return properties; }

    public void setCells(CellStore value) { cells = Objects.requireNonNull(value); }
    public void setRows(AxisIndex value) { rows = Objects.requireNonNull(value); }
    public void setColumns(AxisIndex value) { columns = Objects.requireNonNull(value); }
    public void setProperties(SheetProperties value) { properties = Objects.requireNonNull(value); }

    public SheetCell cell(int row, int column) { return cells.getOrBlank(row, column); }
    public SheetCell cell(CellAddress a) { return cells.getOrBlank(a.row(), a.column()); }
    public SheetCell put(int row, int column, SheetCell cell) { return cells.set(row, column, cell); }

    public CellRange usedRange() { return cells.usedRange(); }

    public SheetWorksheet snapshot() { return new SheetWorksheet(id, cells.snapshot(), rows.copy(), columns.copy(), properties); }

    public SheetWorksheet duplicate(String newName) { return new SheetWorksheet(UUID.randomUUID().toString(), cells.snapshot(), rows.copy(), columns.copy(), properties.withName(newName)); }

    @Override public String toString() { return name(); }
}
