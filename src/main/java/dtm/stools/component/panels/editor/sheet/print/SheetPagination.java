package dtm.stools.component.panels.editor.sheet.print;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;

import java.util.ArrayList;
import java.util.List;

public final class SheetPagination {
    public static final double PX_PER_POINT = 96.0 / 72.0;

    private SheetPagination() {}

    public static CellRange printRange(SheetWorkbook wb, CalcEngine engine, int sheet) {
        SheetWorksheet ws = wb.sheet(sheet);
        PrintSettings ps = ws.properties().print();
        if (ps.printArea() != null) return ps.printArea();
        CellRange used = engine == null ? ws.usedRange() : engine.usedRange(sheet);
        for (SheetObject o : ws.properties().objects()) {
            ObjectAnchor a = o.anchor();
            int lastCol = a.column(), lastRow = a.row();
            long x = ws.columns().position(a.column()) + a.offsetX() + a.width(), y = ws.rows().position(a.row()) + a.offsetY() + a.height();
            lastCol = Math.max(lastCol, ws.columns().indexAt(x));
            lastRow = Math.max(lastRow, ws.rows().indexAt(y));
            CellRange r = new CellRange(a.row(), a.column(), lastRow, lastCol);
            used = used == null ? r : used.union(r);
        }
        return used == null ? CellRange.of(0, 0) : new CellRange(0, 0, used.lastRow(), used.lastColumn());
    }

    public static SheetPageLayout layout(SheetWorkbook wb, CalcEngine engine, int sheet) {
        SheetWorksheet ws = wb.sheet(sheet);
        PrintSettings ps = ws.properties().print();
        CellRange range = printRange(wb, engine, sheet);
        double pageW = ps.pageWidth() * PX_PER_POINT, pageH = ps.pageHeight() * PX_PER_POINT;
        double availW = pageW - (ps.marginLeft() + ps.marginRight()) * 96, availH = pageH - (ps.marginTop() + ps.marginBottom()) * 96;
        CellRange repeatRows = ps.repeatRowFirst() == null ? null : new CellRange(ps.repeatRowFirst(), range.firstColumn(), ps.repeatRowLast() == null ? ps.repeatRowFirst() : ps.repeatRowLast(), range.lastColumn());
        CellRange repeatCols = ps.repeatColumnFirst() == null ? null : new CellRange(range.firstRow(), ps.repeatColumnFirst(), range.lastRow(), ps.repeatColumnLast() == null ? ps.repeatColumnFirst() : ps.repeatColumnLast());
        double totalW = span(ws.columns(), range.firstColumn(), range.lastColumn()), totalH = span(ws.rows(), range.firstRow(), range.lastRow());
        double scale = Math.max(0.1, Math.min(4, ps.scale() / 100.0));
        if (ps.fitWidth() > 0 || ps.fitHeight() > 0) {
            double s = 4;
            if (ps.fitWidth() > 0 && totalW > 0) s = Math.min(s, ps.fitWidth() * availW / totalW);
            if (ps.fitHeight() > 0 && totalH > 0) s = Math.min(s, ps.fitHeight() * availH / totalH);
            scale = Math.max(0.1, Math.min(1, s));
        }
        double titleH = repeatRows == null ? 0 : span(ws.rows(), repeatRows.firstRow(), repeatRows.lastRow()) * scale;
        double titleW = repeatCols == null ? 0 : span(ws.columns(), repeatCols.firstColumn(), repeatCols.lastColumn()) * scale;
        List<int[]> colBands = bands(ws.columns(), range.firstColumn(), range.lastColumn(), (availW - titleW) / scale, ps.columnBreaks());
        List<int[]> rowBands = bands(ws.rows(), range.firstRow(), range.lastRow(), (availH - titleH) / scale, ps.rowBreaks());
        List<SheetPage> pages = new ArrayList<>();
        int n = 1;
        if (ps.overThenDown()) {
            for (int[] rb : rowBands) for (int[] cb : colBands) pages.add(page(sheet, n++, rb, cb, repeatRows, repeatCols));
        } else {
            for (int[] cb : colBands) for (int[] rb : rowBands) pages.add(page(sheet, n++, rb, cb, repeatRows, repeatCols));
        }
        List<Integer> rowBreaks = new ArrayList<>(), colBreaks = new ArrayList<>();
        for (int i = 1; i < rowBands.size(); i++) rowBreaks.add(rowBands.get(i)[0]);
        for (int i = 1; i < colBands.size(); i++) colBreaks.add(colBands.get(i)[0]);
        return new SheetPageLayout(ps, scale, pageW, pageH, pages, rowBreaks, colBreaks);
    }

    private static SheetPage page(int sheet, int number, int[] rows, int[] cols, CellRange repeatRows, CellRange repeatCols) {
        CellRange cells = new CellRange(rows[0], cols[0], rows[1], cols[1]);
        CellRange rr = repeatRows != null && repeatRows.lastRow() < rows[0] ? new CellRange(repeatRows.firstRow(), cols[0], repeatRows.lastRow(), cols[1]) : null;
        CellRange rc = repeatCols != null && repeatCols.lastColumn() < cols[0] ? new CellRange(rows[0], repeatCols.firstColumn(), rows[1], repeatCols.lastColumn()) : null;
        return new SheetPage(sheet, number, cells, rr, rc);
    }

    static double span(AxisIndex axis, int from, int to) { return axis.position(to + 1) - axis.position(from); }

    private static List<int[]> bands(AxisIndex axis, int from, int to, double available, List<Integer> manual) {
        List<int[]> list = new ArrayList<>();
        int start = from;
        double used = 0;
        for (int i = from; i <= to; i++) {
            int size = axis.size(i);
            boolean forced = manual.contains(i) && i > start;
            if (i > start && (used + size > available || forced)) {
                list.add(new int[]{start, i - 1});
                start = i;
                used = 0;
            }
            used += size;
            if (i - start > 20_000) { list.add(new int[]{start, i}); start = i + 1; used = 0; }
        }
        if (start <= to) list.add(new int[]{start, to});
        if (list.isEmpty()) list.add(new int[]{from, from});
        return list;
    }
}
