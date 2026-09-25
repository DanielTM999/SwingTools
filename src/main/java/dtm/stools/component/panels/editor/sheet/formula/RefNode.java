package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.Objects;

public record RefNode(String sheet, String sheetEnd, RefPart first, RefPart second, String external) implements FormulaNode {
    public RefNode { Objects.requireNonNull(first); }

    public static RefNode cell(String sheet, int row, int column) { return new RefNode(sheet, null, RefPart.cell(row, column, false, false), null, null); }
    public static RefNode area(String sheet, CellRange r) {
        return new RefNode(sheet, null, RefPart.cell(r.firstRow(), r.firstColumn(), false, false), r.isSingleCell() ? null : RefPart.cell(r.lastRow(), r.lastColumn(), false, false), null);
    }

    public boolean isArea() { return second != null; }
    public boolean is3D() { return sheetEnd != null; }
    @Override public boolean isReference() { return true; }

    public CellRange range() {
        RefPart a = first, b = second == null ? first : second;
        int r1 = a.wholeColumn() ? 0 : a.row(), r2 = b.wholeColumn() ? CellAddress.MAX_ROWS - 1 : b.row();
        int c1 = a.wholeRow() ? 0 : a.column(), c2 = b.wholeRow() ? CellAddress.MAX_COLUMNS - 1 : b.column();
        return new CellRange(r1, c1, r2, c2);
    }

    public RefNode withParts(RefPart a, RefPart b) { return new RefNode(sheet, sheetEnd, a, b, external); }
    public RefNode withSheet(String s, String e) { return new RefNode(s, e, first, second, external); }
}
