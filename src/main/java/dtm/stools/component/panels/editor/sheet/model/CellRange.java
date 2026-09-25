package dtm.stools.component.panels.editor.sheet.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

public record CellRange(int firstRow, int firstColumn, int lastRow, int lastColumn) implements Iterable<CellAddress> {
    public CellRange {
        int r1 = Math.min(firstRow, lastRow), r2 = Math.max(firstRow, lastRow), c1 = Math.min(firstColumn, lastColumn), c2 = Math.max(firstColumn, lastColumn);
        if (r1 < 0 || c1 < 0 || r2 >= CellAddress.MAX_ROWS || c2 >= CellAddress.MAX_COLUMNS) throw new IllegalArgumentException("Invalid range");
        firstRow = r1; lastRow = r2; firstColumn = c1; lastColumn = c2;
    }

    public static CellRange of(CellAddress a) { return new CellRange(a.row(), a.column(), a.row(), a.column()); }
    public static CellRange of(CellAddress a, CellAddress b) { return new CellRange(a.row(), a.column(), b.row(), b.column()); }
    public static CellRange of(int row, int column) { return new CellRange(row, column, row, column); }
    public static CellRange columns(int first, int last) { return new CellRange(0, first, CellAddress.MAX_ROWS - 1, last); }
    public static CellRange rows(int first, int last) { return new CellRange(first, 0, last, CellAddress.MAX_COLUMNS - 1); }
    public static CellRange all() { return new CellRange(0, 0, CellAddress.MAX_ROWS - 1, CellAddress.MAX_COLUMNS - 1); }

    public static CellRange parse(String text) {
        String t = text.strip().replace("$", "");
        int colon = t.indexOf(':');
        if (colon < 0) return of(CellAddress.parse(t));
        String a = t.substring(0, colon), b = t.substring(colon + 1);
        if (a.chars().allMatch(Character::isLetter) && b.chars().allMatch(Character::isLetter)) return columns(CellAddress.columnIndex(a), CellAddress.columnIndex(b));
        if (a.chars().allMatch(Character::isDigit) && b.chars().allMatch(Character::isDigit)) return rows(Integer.parseInt(a) - 1, Integer.parseInt(b) - 1);
        return of(CellAddress.parse(a), CellAddress.parse(b));
    }

    public CellAddress first() { return new CellAddress(firstRow, firstColumn); }
    public CellAddress last() { return new CellAddress(lastRow, lastColumn); }
    public int rowCount() { return lastRow - firstRow + 1; }
    public int columnCount() { return lastColumn - firstColumn + 1; }
    public long cellCount() { return (long) rowCount() * columnCount(); }
    public boolean isSingleCell() { return firstRow == lastRow && firstColumn == lastColumn; }
    public boolean isWholeColumn() { return firstRow == 0 && lastRow == CellAddress.MAX_ROWS - 1; }
    public boolean isWholeRow() { return firstColumn == 0 && lastColumn == CellAddress.MAX_COLUMNS - 1; }
    public boolean contains(int row, int column) { return row >= firstRow && row <= lastRow && column >= firstColumn && column <= lastColumn; }
    public boolean contains(CellAddress a) { return contains(a.row(), a.column()); }
    public boolean contains(CellRange o) { return o.firstRow >= firstRow && o.lastRow <= lastRow && o.firstColumn >= firstColumn && o.lastColumn <= lastColumn; }
    public boolean intersects(CellRange o) { return o.firstRow <= lastRow && o.lastRow >= firstRow && o.firstColumn <= lastColumn && o.lastColumn >= firstColumn; }

    public CellRange intersection(CellRange o) {
        if (!intersects(o)) return null;
        return new CellRange(Math.max(firstRow, o.firstRow), Math.max(firstColumn, o.firstColumn), Math.min(lastRow, o.lastRow), Math.min(lastColumn, o.lastColumn));
    }

    public CellRange union(CellRange o) {
        return new CellRange(Math.min(firstRow, o.firstRow), Math.min(firstColumn, o.firstColumn), Math.max(lastRow, o.lastRow), Math.max(lastColumn, o.lastColumn));
    }

    public CellRange offset(int rows, int columns) { return new CellRange(firstRow + rows, firstColumn + columns, lastRow + rows, lastColumn + columns); }
    public CellRange resize(int rows, int columns) { return new CellRange(firstRow, firstColumn, firstRow + rows - 1, firstColumn + columns - 1); }

    public String toA1() {
        if (isWholeColumn() && !isWholeRow()) return CellAddress.columnName(firstColumn) + ":" + CellAddress.columnName(lastColumn);
        if (isWholeRow() && !isWholeColumn()) return (firstRow + 1) + ":" + (lastRow + 1);
        return isSingleCell() ? first().toA1() : first().toA1() + ":" + last().toA1();
    }

    @Override public String toString() { return toA1(); }

    @Override
    public Iterator<CellAddress> iterator() {
        return new Iterator<>() {
            int r = firstRow, c = firstColumn;
            @Override public boolean hasNext() { return r <= lastRow; }
            @Override public CellAddress next() {
                if (r > lastRow) throw new NoSuchElementException();
                CellAddress a = new CellAddress(r, c);
                if (++c > lastColumn) { c = firstColumn; r++; }
                return a;
            }
        };
    }
}
