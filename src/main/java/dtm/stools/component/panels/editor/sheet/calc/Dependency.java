package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

public record Dependency(int sheet, int sheetEnd, CellRange range) {
    public boolean covers(int s, int row, int column) { return s >= sheet && s <= sheetEnd && range.contains(row, column); }
    public boolean intersects(int s, CellRange r) { return s >= sheet && s <= sheetEnd && range.intersects(r); }
}
