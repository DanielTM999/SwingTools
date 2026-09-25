package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;

import java.awt.Rectangle;

public final class SheetGeometry {
    private final SheetWorksheet sheet;
    private final double zoom;
    private final int headerWidth, headerHeight, frozenRows, frozenColumns, width, height;
    private final long scrollX, scrollY;

    public SheetGeometry(SheetWorksheet sheet, double zoom, int headerWidth, int headerHeight, long scrollX, long scrollY, int width, int height) {
        this.sheet = sheet;
        this.zoom = zoom;
        this.headerWidth = headerWidth;
        this.headerHeight = headerHeight;
        this.frozenRows = sheet.properties().freeze().rows();
        this.frozenColumns = sheet.properties().freeze().columns();
        this.scrollX = Math.max(0, scrollX);
        this.scrollY = Math.max(0, scrollY);
        this.width = width;
        this.height = height;
    }

    public SheetWorksheet sheet() { return sheet; }
    public double zoom() { return zoom; }
    public int headerWidth() { return headerWidth; }
    public int headerHeight() { return headerHeight; }
    public int frozenRows() { return frozenRows; }
    public int frozenColumns() { return frozenColumns; }
    public long scrollX() { return scrollX; }
    public long scrollY() { return scrollY; }
    public int width() { return width; }
    public int height() { return height; }
    public AxisIndex rows() { return sheet.rows(); }
    public AxisIndex columns() { return sheet.columns(); }

    public int frozenWidth() { return (int) Math.round(sheet.columns().position(frozenColumns) * zoom); }
    public int frozenHeight() { return (int) Math.round(sheet.rows().position(frozenRows) * zoom); }
    public int bodyX() { return headerWidth + frozenWidth(); }
    public int bodyY() { return headerHeight + frozenHeight(); }

    public int columnX(int column) {
        AxisIndex cols = sheet.columns();
        if (column < frozenColumns) return headerWidth + (int) Math.round(cols.position(column) * zoom);
        return headerWidth + frozenWidth() + (int) Math.round((cols.position(column) - cols.position(frozenColumns) - scrollX) * zoom);
    }

    public int rowY(int row) {
        AxisIndex rows = sheet.rows();
        if (row < frozenRows) return headerHeight + (int) Math.round(rows.position(row) * zoom);
        return headerHeight + frozenHeight() + (int) Math.round((rows.position(row) - rows.position(frozenRows) - scrollY) * zoom);
    }

    public int columnWidth(int column) { return columnX(column + 1) - columnX(column); }
    public int rowHeight(int row) { return rowY(row + 1) - rowY(row); }

    public Rectangle cellRect(int row, int column) { return new Rectangle(columnX(column), rowY(row), columnWidth(column), rowHeight(row)); }

    public Rectangle rangeRect(CellRange r) {
        int x1 = columnX(r.firstColumn()), y1 = rowY(r.firstRow());
        int x2 = columnX(Math.min(CellAddress.MAX_COLUMNS - 1, r.lastColumn()) + 1), y2 = rowY(Math.min(CellAddress.MAX_ROWS - 1, r.lastRow()) + 1);
        if (r.lastColumn() == CellAddress.MAX_COLUMNS - 1) x2 = Math.max(x2, width);
        if (r.lastRow() == CellAddress.MAX_ROWS - 1) y2 = Math.max(y2, height);
        return new Rectangle(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
    }

    public int columnAt(int x) {
        AxisIndex cols = sheet.columns();
        if (x < headerWidth) return -1;
        if (x < headerWidth + frozenWidth()) return cols.indexAt((long) Math.floor((x - headerWidth) / zoom));
        long pos = cols.position(frozenColumns) + scrollX + (long) Math.floor((x - headerWidth - frozenWidth()) / zoom);
        return Math.min(CellAddress.MAX_COLUMNS - 1, cols.indexAt(pos));
    }

    public int rowAt(int y) {
        AxisIndex rows = sheet.rows();
        if (y < headerHeight) return -1;
        if (y < headerHeight + frozenHeight()) return rows.indexAt((long) Math.floor((y - headerHeight) / zoom));
        long pos = rows.position(frozenRows) + scrollY + (long) Math.floor((y - headerHeight - frozenHeight()) / zoom);
        return Math.min(CellAddress.MAX_ROWS - 1, rows.indexAt(pos));
    }

    public int columnAtClamped(int x) { return columnAt(Math.max(headerWidth, Math.min(width - 1, x))); }
    public int rowAtClamped(int y) { return rowAt(Math.max(headerHeight, Math.min(height - 1, y))); }

    public int firstScrollColumn() { return sheet.columns().indexAt(sheet.columns().position(frozenColumns) + scrollX); }
    public int firstScrollRow() { return sheet.rows().indexAt(sheet.rows().position(frozenRows) + scrollY); }
    public int lastVisibleColumn() { return columnAtClamped(width - 1); }
    public int lastVisibleRow() { return rowAtClamped(height - 1); }

    public boolean isColumnVisible(int column) {
        if (column < frozenColumns) return true;
        int x = columnX(column);
        return x + columnWidth(column) > bodyX() && x < width;
    }

    public boolean isRowVisible(int row) {
        if (row < frozenRows) return true;
        int y = rowY(row);
        return y + rowHeight(row) > bodyY() && y < height;
    }

    public long scrollXFor(int column, boolean alignRight) {
        AxisIndex cols = sheet.columns();
        long base = cols.position(frozenColumns);
        if (column < frozenColumns) return scrollX;
        long left = cols.position(column) - base, right = cols.position(column + 1) - base;
        long view = (long) Math.floor((width - bodyX()) / zoom);
        if (left < scrollX) return left;
        if (right > scrollX + view) return alignRight ? right - view : left;
        return scrollX;
    }

    public long scrollYFor(int row) {
        AxisIndex rows = sheet.rows();
        long base = rows.position(frozenRows);
        if (row < frozenRows) return scrollY;
        long top = rows.position(row) - base, bottom = rows.position(row + 1) - base;
        long view = (long) Math.floor((height - bodyY()) / zoom);
        if (top < scrollY) return top;
        if (bottom > scrollY + view) return bottom - view;
        return scrollY;
    }

    public Rectangle objectRect(ObjectAnchor a) {
        int x = columnX(a.column()) + (int) Math.round(a.offsetX() * zoom), y = rowY(a.row()) + (int) Math.round(a.offsetY() * zoom);
        return new Rectangle(x, y, (int) Math.round(a.width() * zoom), (int) Math.round(a.height() * zoom));
    }

    public ObjectAnchor anchorAt(int x, int y, int w, int h) {
        int col = Math.max(0, columnAt(Math.max(headerWidth, x))), row = Math.max(0, rowAt(Math.max(headerHeight, y)));
        int ox = (int) Math.round((x - columnX(col)) / zoom), oy = (int) Math.round((y - rowY(row)) / zoom);
        return new ObjectAnchor(row, col, Math.max(0, ox), Math.max(0, oy), (int) Math.round(w / zoom), (int) Math.round(h / zoom));
    }
}
