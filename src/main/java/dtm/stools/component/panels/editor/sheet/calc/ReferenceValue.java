package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;

public record ReferenceValue(int sheet, int sheetEnd, List<CellRange> areas) implements CellValue {
    public ReferenceValue { areas = List.copyOf(areas); if (areas.isEmpty()) throw new IllegalArgumentException("Empty reference"); }

    public static ReferenceValue of(int sheet, CellRange range) { return new ReferenceValue(sheet, sheet, List.of(range)); }
    public static ReferenceValue cell(int sheet, int row, int column) { return of(sheet, CellRange.of(row, column)); }

    public boolean isSingleArea() { return areas.size() == 1 && sheet == sheetEnd; }
    public boolean isSingleCell() { return isSingleArea() && areas.getFirst().isSingleCell(); }
    public boolean is3D() { return sheet != sheetEnd; }
    public CellRange range() { return areas.getFirst(); }
    public int rows() { return range().rowCount(); }
    public int columns() { return range().columnCount(); }

    @Override public String toString() { return "Ref(" + sheet + (is3D() ? ":" + sheetEnd : "") + "!" + areas + ")"; }
}
