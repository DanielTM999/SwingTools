package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.formula.RangeMapper;
import dtm.stools.component.panels.editor.sheet.model.AutoFilter;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.FilterView;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.ProtectedRange;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.SortKey;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.TableColumn;
import dtm.stools.component.panels.editor.sheet.store.CellStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class SheetOperations {
    public enum ClearMode { ALL, CONTENTS, FORMATS, NOTES, LINKS }

    private SheetOperations() {}

    public static void insertRows(SheetTransaction tx, int sheet, int at, int count) {
        SheetWorksheet ws = tx.sheet(sheet);
        tx.replaceCells(sheet, ws.cells().shiftRows(at, count));
        tx.updateAxis(sheet, true, a -> a.shift(at, count));
        remap(tx, sheet, ReferenceAdjuster.insertRows(ws.name(), at, count), (r, c) -> r >= at ? new int[]{r + count, c} : new int[]{r, c});
    }

    public static void deleteRows(SheetTransaction tx, int sheet, int at, int count) {
        SheetWorksheet ws = tx.sheet(sheet);
        tx.replaceCells(sheet, ws.cells().shiftRows(at, -count));
        tx.updateAxis(sheet, true, a -> a.shift(at, -count));
        remap(tx, sheet, ReferenceAdjuster.deleteRows(ws.name(), at, count), (r, c) -> r < at ? new int[]{r, c} : r < at + count ? null : new int[]{r - count, c});
    }

    public static void insertColumns(SheetTransaction tx, int sheet, int at, int count) {
        SheetWorksheet ws = tx.sheet(sheet);
        tx.replaceCells(sheet, ws.cells().shiftColumns(at, count));
        tx.updateAxis(sheet, false, a -> a.shift(at, count));
        remap(tx, sheet, ReferenceAdjuster.insertColumns(ws.name(), at, count), (r, c) -> c >= at ? new int[]{r, c + count} : new int[]{r, c});
    }

    public static void deleteColumns(SheetTransaction tx, int sheet, int at, int count) {
        SheetWorksheet ws = tx.sheet(sheet);
        tx.replaceCells(sheet, ws.cells().shiftColumns(at, -count));
        tx.updateAxis(sheet, false, a -> a.shift(at, -count));
        remap(tx, sheet, ReferenceAdjuster.deleteColumns(ws.name(), at, count), (r, c) -> c < at ? new int[]{r, c} : c < at + count ? null : new int[]{r, c - count});
    }

    public static void insertCells(SheetTransaction tx, int sheet, CellRange range, boolean shiftDown) {
        SheetWorksheet ws = tx.sheet(sheet);
        CellStore next = new CellStore();
        int rows = range.rowCount(), cols = range.columnCount();
        ws.cells().forEach((r, c, cell) -> {
            boolean band = shiftDown ? c >= range.firstColumn() && c <= range.lastColumn() && r >= range.firstRow() : r >= range.firstRow() && r <= range.lastRow() && c >= range.firstColumn();
            if (!band) { next.set(r, c, cell); return; }
            int nr = shiftDown ? r + rows : r, nc = shiftDown ? c : c + cols;
            if (nr < CellAddress.MAX_ROWS && nc < CellAddress.MAX_COLUMNS) next.set(nr, nc, cell);
        });
        tx.replaceCells(sheet, next);
        String name = ws.name();
        RangeMapper mapper = (s, r) -> {
            if (!s.equalsIgnoreCase(name)) return r;
            if (shiftDown) {
                if (r.firstColumn() < range.firstColumn() || r.lastColumn() > range.lastColumn() || r.lastRow() < range.firstRow()) return r;
                return new CellRange(r.firstRow() >= range.firstRow() ? r.firstRow() + rows : r.firstRow(), r.firstColumn(), Math.min(CellAddress.MAX_ROWS - 1, r.lastRow() + rows), r.lastColumn());
            }
            if (r.firstRow() < range.firstRow() || r.lastRow() > range.lastRow() || r.lastColumn() < range.firstColumn()) return r;
            return new CellRange(r.firstRow(), r.firstColumn() >= range.firstColumn() ? r.firstColumn() + cols : r.firstColumn(), r.lastRow(), Math.min(CellAddress.MAX_COLUMNS - 1, r.lastColumn() + cols));
        };
        remap(tx, sheet, mapper, (r, c) -> {
            boolean band = shiftDown ? c >= range.firstColumn() && c <= range.lastColumn() && r >= range.firstRow() : r >= range.firstRow() && r <= range.lastRow() && c >= range.firstColumn();
            return band ? (shiftDown ? new int[]{r + rows, c} : new int[]{r, c + cols}) : new int[]{r, c};
        });
    }

    public static void deleteCells(SheetTransaction tx, int sheet, CellRange range, boolean shiftUp) {
        SheetWorksheet ws = tx.sheet(sheet);
        CellStore next = new CellStore();
        int rows = range.rowCount(), cols = range.columnCount();
        ws.cells().forEach((r, c, cell) -> {
            if (range.contains(r, c)) return;
            boolean band = shiftUp ? c >= range.firstColumn() && c <= range.lastColumn() && r > range.lastRow() : r >= range.firstRow() && r <= range.lastRow() && c > range.lastColumn();
            if (!band) next.set(r, c, cell);
            else next.set(shiftUp ? r - rows : r, shiftUp ? c : c - cols, cell);
        });
        tx.replaceCells(sheet, next);
        String name = ws.name();
        RangeMapper mapper = (s, r) -> {
            if (!s.equalsIgnoreCase(name)) return r;
            if (range.contains(r)) return null;
            if (shiftUp) {
                if (r.firstColumn() < range.firstColumn() || r.lastColumn() > range.lastColumn() || r.firstRow() <= range.lastRow()) return r;
                return r.offset(-rows, 0);
            }
            if (r.firstRow() < range.firstRow() || r.lastRow() > range.lastRow() || r.firstColumn() <= range.lastColumn()) return r;
            return r.offset(0, -cols);
        };
        remap(tx, sheet, mapper, (r, c) -> {
            if (range.contains(r, c)) return null;
            boolean band = shiftUp ? c >= range.firstColumn() && c <= range.lastColumn() && r > range.lastRow() : r >= range.firstRow() && r <= range.lastRow() && c > range.lastColumn();
            return band ? (shiftUp ? new int[]{r - rows, c} : new int[]{r, c - cols}) : new int[]{r, c};
        });
    }

    private static void remap(SheetTransaction tx, int sheet, RangeMapper mapper, PointMapper points) {
        SheetWorkbook wb = tx.workbook();
        for (int s = 0; s < wb.sheetCount(); s++) {
            SheetWorksheet ws = wb.sheet(s);
            String host = ws.name();
            List<Object[]> updates = new ArrayList<>();
            ws.cells().forEach((r, c, cell) -> {
                if (!cell.hasFormula()) return;
                try {
                    String next = ReferenceAdjuster.map(cell.formula(), host, mapper);
                    if (!next.equals(cell.formula())) updates.add(new Object[]{r, c, cell.withFormula(next, cell.value())});
                } catch (RuntimeException ignored) { }
            });
            for (Object[] u : updates) tx.setCell(s, (int) u[0], (int) u[1], (SheetCell) u[2]);
            int sheetIndex = s;
            tx.updateProperties(s, p -> mapProperties(p, host, mapper, sheetIndex == sheet ? points : null));
        }
        tx.updateWorkbook(p -> p.withNames(p.names().stream().map(n -> mapName(n, wb, mapper)).toList()));
        tx.markStructural();
    }

    private static DefinedName mapName(DefinedName n, SheetWorkbook wb, RangeMapper mapper) {
        try {
            String host = n.sheetScope() != null && n.sheetScope() < wb.sheetCount() ? wb.sheet(n.sheetScope()).name() : wb.sheet(0).name();
            return n.withFormula(ReferenceAdjuster.map(n.formula(), host, mapper));
        } catch (RuntimeException e) { return n; }
    }

    public static SheetProperties mapProperties(SheetProperties p, String sheet, RangeMapper mapper, PointMapper points) {
        Function<CellRange, CellRange> m = r -> mapper.map(sheet, r);
        List<CellRange> merges = mapList(p.merges(), m);
        List<DataValidation> validations = new ArrayList<>();
        for (DataValidation v : p.validations()) { List<CellRange> rs = mapList(v.ranges(), m); if (!rs.isEmpty()) validations.add(v.withRanges(rs)); }
        List<ConditionalFormat> formats = new ArrayList<>();
        for (ConditionalFormat f : p.conditionalFormats()) { List<CellRange> rs = mapList(f.ranges(), m); if (!rs.isEmpty()) formats.add(new ConditionalFormat(rs, f.rules())); }
        AutoFilter filter = p.autoFilter() == null ? null : mapFilter(p.autoFilter(), m);
        List<FilterView> views = new ArrayList<>();
        for (FilterView v : p.filterViews()) { AutoFilter f = mapFilter(v.filter(), m); if (f != null) views.add(new FilterView(v.id(), v.name(), f)); }
        List<SheetTable> tables = new ArrayList<>();
        for (SheetTable t : p.tables()) {
            CellRange r = m.apply(t.range());
            if (r == null) continue;
            if (r.columnCount() != t.columns().size()) {
                List<TableColumn> cols = new ArrayList<>(t.columns());
                while (cols.size() < r.columnCount()) cols.add(TableColumn.of("Coluna" + (cols.size() + 1)));
                while (cols.size() > r.columnCount()) cols.removeLast();
                tables.add(t.toBuilder().range(r).columns(cols).build());
                continue;
            }
            tables.add(t.withRange(r));
        }
        List<SheetObject> objects = new ArrayList<>();
        for (SheetObject o : p.objects()) {
            ObjectAnchor a = o.anchor();
            if (points != null) { int[] np = points.map(a.row(), a.column()); if (np == null) np = new int[]{a.row(), a.column()}; a = a.moved(np[0], np[1], a.offsetX(), a.offsetY()); }
            SheetObject moved = o.withAnchor(a);
            if (moved instanceof SheetChart chart) moved = chart.withSeries(chart.series().stream().map(s -> mapSeries(s, sheet, mapper)).toList());
            objects.add(moved);
        }
        List<ProtectedRange> protectedRanges = new ArrayList<>();
        for (ProtectedRange pr : p.protectedRanges()) { List<CellRange> rs = mapList(pr.ranges(), m); if (!rs.isEmpty()) protectedRanges.add(new ProtectedRange(pr.name(), rs, pr.editors(), pr.passwordHash(), pr.description())); }
        List<Sparkline> sparklines = new ArrayList<>();
        for (Sparkline s : p.sparklines()) {
            CellAddress loc = s.location();
            if (points != null) { int[] np = points.map(loc.row(), loc.column()); if (np == null) continue; loc = new CellAddress(np[0], np[1]); }
            sparklines.add(new Sparkline(loc, mapRef(s.dataRef(), sheet, mapper), s.type(), s.color(), s.markers(), s.highPoint(), s.lowPoint(), s.negativePoints()));
        }
        List<PivotTable> pivots = new ArrayList<>();
        for (PivotTable pv : p.pivots()) {
            CellAddress target = pv.target();
            if (points != null) { int[] np = points.map(target.row(), target.column()); if (np == null) continue; target = new CellAddress(np[0], np[1]); }
            pivots.add(pv.withTarget(target).withOutput(pv.output() == null ? null : m.apply(pv.output())));
        }
        PrintSettings print = p.print();
        if (print.printArea() != null) print = print.withPrintArea(m.apply(print.printArea()));
        return p.withMerges(merges).withValidations(validations).withConditionalFormats(formats).withAutoFilter(filter).withFilterViews(views)
                .withTables(tables).withObjects(objects).withProtectedRanges(protectedRanges).withSparklines(sparklines).withPivots(pivots).withPrint(print)
                .withNotes(points == null ? p.notes() : mapKeys(p.notes(), points)).withThreads(points == null ? p.threads() : mapKeys(p.threads(), points))
                .withLinks(points == null ? p.links() : mapKeys(p.links(), points));
    }

    private static ChartSeries mapSeries(ChartSeries s, String sheet, RangeMapper mapper) {
        return new ChartSeries(s.name(), mapRef(s.nameRef(), sheet, mapper), mapRef(s.categoriesRef(), sheet, mapper), mapRef(s.valuesRef(), sheet, mapper), mapRef(s.sizesRef(), sheet, mapper), s.color(), s.type(), s.secondaryAxis());
    }

    private static String mapRef(String ref, String sheet, RangeMapper mapper) {
        if (ref == null || ref.isBlank()) return ref;
        try { return ReferenceAdjuster.map(ref, sheet, mapper); } catch (RuntimeException e) { return ref; }
    }

    private static AutoFilter mapFilter(AutoFilter f, Function<CellRange, CellRange> m) {
        CellRange r = m.apply(f.range());
        return r == null ? null : f.withRange(r);
    }

    private static <T> Map<CellAddress, T> mapKeys(Map<CellAddress, T> map, PointMapper points) {
        Map<CellAddress, T> out = new HashMap<>();
        for (Map.Entry<CellAddress, T> e : map.entrySet()) {
            int[] np = points.map(e.getKey().row(), e.getKey().column());
            if (np != null && np[0] >= 0 && np[1] >= 0 && np[0] < CellAddress.MAX_ROWS && np[1] < CellAddress.MAX_COLUMNS) out.put(new CellAddress(np[0], np[1]), e.getValue());
        }
        return out;
    }

    private static List<CellRange> mapList(List<CellRange> list, Function<CellRange, CellRange> m) {
        List<CellRange> out = new ArrayList<>();
        for (CellRange r : list) { CellRange x = m.apply(r); if (x != null) out.add(x); }
        return out;
    }

    public static void clear(SheetTransaction tx, int sheet, CellRange range, ClearMode mode) {
        SheetWorksheet ws = tx.sheet(sheet);
        List<CellAddress> cells = ws.cells().addresses(range);
        for (CellAddress a : cells) {
            SheetCell c = ws.cell(a);
            SheetCell next = switch (mode) {
                case ALL -> null;
                case CONTENTS -> c.cleared();
                case FORMATS -> c.withStyle(0);
                default -> c;
            };
            tx.setCell(sheet, a.row(), a.column(), next);
        }
        if (mode == ClearMode.ALL || mode == ClearMode.NOTES) tx.updateProperties(sheet, p -> p.withNotes(filterKeys(p.notes(), range)).withThreads(filterKeys(p.threads(), range)));
        if (mode == ClearMode.ALL || mode == ClearMode.LINKS) tx.updateProperties(sheet, p -> p.withLinks(filterKeys(p.links(), range)));
        if (mode == ClearMode.ALL || mode == ClearMode.FORMATS) tx.updateProperties(sheet, p -> {
            List<CellRange> merges = new ArrayList<>(p.merges());
            merges.removeIf(range::contains);
            return p.withMerges(merges);
        });
    }

    private static <T> Map<CellAddress, T> filterKeys(Map<CellAddress, T> map, CellRange range) {
        Map<CellAddress, T> out = new HashMap<>(map);
        out.keySet().removeIf(range::contains);
        return out;
    }

    public static void copy(SheetTransaction tx, int fromSheet, CellRange source, int toSheet, CellAddress target, boolean values, boolean formulas, boolean formats) {
        SheetWorksheet src = tx.sheet(fromSheet);
        Map<CellAddress, SheetCell> snapshot = new HashMap<>();
        src.cells().forEach(source, (r, c, cell) -> snapshot.put(new CellAddress(r, c), cell));
        int dr = target.row() - source.firstRow(), dc = target.column() - source.firstColumn();
        for (int r = source.firstRow(); r <= source.lastRow(); r++)
            for (int c = source.firstColumn(); c <= source.lastColumn(); c++) {
                int tr = r + dr, tc = c + dc;
                if (tr >= CellAddress.MAX_ROWS || tc >= CellAddress.MAX_COLUMNS) continue;
                SheetCell s = snapshot.getOrDefault(new CellAddress(r, c), SheetCell.BLANK);
                SheetCell existing = tx.cell(toSheet, tr, tc);
                CellValue value = existing.value();
                String formula = existing.formula();
                int style = existing.style();
                if (formats) style = s.style();
                if (formulas && s.hasFormula()) { formula = safeShift(s.formula(), dr, dc); value = CellValue.EMPTY; }
                else if (values || formulas) { value = s.value(); formula = null; }
                tx.setCell(toSheet, tr, tc, new SheetCell(value, formula, style));
            }
    }

    static String safeShift(String formula, int dr, int dc) {
        try { return ReferenceAdjuster.shift(formula, dr, dc); } catch (RuntimeException e) { return formula; }
    }

    public static void move(SheetTransaction tx, int sheet, CellRange source, CellAddress target) {
        SheetWorksheet ws = tx.sheet(sheet);
        int dr = target.row() - source.firstRow(), dc = target.column() - source.firstColumn();
        if (dr == 0 && dc == 0) return;
        CellRange dest = source.offset(dr, dc);
        Map<CellAddress, SheetCell> moving = new HashMap<>();
        ws.cells().forEach(source, (r, c, cell) -> moving.put(new CellAddress(r, c), cell));
        for (CellAddress a : moving.keySet()) tx.setCell(sheet, a.row(), a.column(), null);
        for (CellAddress a : ws.cells().addresses(dest)) if (!source.contains(a)) tx.setCell(sheet, a.row(), a.column(), null);
        for (Map.Entry<CellAddress, SheetCell> e : moving.entrySet()) tx.setCell(sheet, e.getKey().row() + dr, e.getKey().column() + dc, e.getValue());
        String name = ws.name();
        RangeMapper mapper = ReferenceAdjuster.move(name, source, name, dr, dc);
        SheetWorkbook wb = tx.workbook();
        for (int s = 0; s < wb.sheetCount(); s++) {
            SheetWorksheet other = wb.sheet(s);
            List<Object[]> updates = new ArrayList<>();
            other.cells().forEach((r, c, cell) -> {
                if (!cell.hasFormula()) return;
                try {
                    String next = ReferenceAdjuster.map(cell.formula(), other.name(), mapper);
                    if (!next.equals(cell.formula())) updates.add(new Object[]{r, c, cell.withFormula(next, cell.value())});
                } catch (RuntimeException ignored) { }
            });
            for (Object[] u : updates) tx.setCell(s, (int) u[0], (int) u[1], (SheetCell) u[2]);
        }
        tx.updateProperties(sheet, p -> {
            List<CellRange> merges = new ArrayList<>();
            for (CellRange m : p.merges()) if (source.contains(m)) merges.add(m.offset(dr, dc)); else if (!dest.intersects(m)) merges.add(m);
            return p.withMerges(merges).withNotes(moveKeys(p.notes(), source, dr, dc)).withLinks(moveKeys(p.links(), source, dr, dc)).withThreads(moveKeys(p.threads(), source, dr, dc));
        });
    }

    private static <T> Map<CellAddress, T> moveKeys(Map<CellAddress, T> map, CellRange source, int dr, int dc) {
        Map<CellAddress, T> out = new HashMap<>();
        for (Map.Entry<CellAddress, T> e : map.entrySet()) out.put(source.contains(e.getKey()) ? e.getKey().offset(dr, dc) : e.getKey(), e.getValue());
        return out;
    }

    public static void sort(SheetTransaction tx, int sheet, CellRange range, List<SortKey> keys, boolean hasHeader, boolean byColumns, ValueReader values, Comparator<CellValue> comparator, Map<Integer, Integer> colors) {
        SheetWorksheet ws = tx.sheet(sheet);
        int start = byColumns ? range.firstColumn() + (hasHeader ? 1 : 0) : range.firstRow() + (hasHeader ? 1 : 0);
        int end = byColumns ? range.lastColumn() : range.lastRow();
        if (end <= start) return;
        List<Integer> order = new ArrayList<>();
        for (int i = start; i <= end; i++) order.add(i);
        order.sort((x, y) -> {
            for (SortKey k : keys) {
                int kr = byColumns ? k.column() : -1;
                CellValue vx = byColumns ? values.value(k.column(), x) : values.value(x, k.column());
                CellValue vy = byColumns ? values.value(k.column(), y) : values.value(y, k.column());
                if (vx.isEmpty() != vy.isEmpty()) return vx.isEmpty() ? 1 : -1;
                int cmp;
                if (!k.customOrder().isEmpty()) cmp = Integer.compare(indexIn(k.customOrder(), vx), indexIn(k.customOrder(), vy));
                else if (k.on() != dtm.stools.component.panels.editor.sheet.model.SortOn.VALUES && colors != null) {
                    Integer cx = colors.get(x), cy = colors.get(y);
                    boolean mx = Objects.equals(cx, k.color()), my = Objects.equals(cy, k.color());
                    cmp = mx == my ? 0 : mx ? -1 : 1;
                    if (k.descending()) cmp = -cmp;
                } else cmp = comparator.compare(vx, vy);
                if (k.on() == dtm.stools.component.panels.editor.sheet.model.SortOn.VALUES && k.descending()) cmp = -cmp;
                if (cmp != 0) return cmp;
                if (kr > 0) return 0;
            }
            return Integer.compare(x, y);
        });
        Map<CellAddress, SheetCell> snapshot = new HashMap<>();
        CellRange body = byColumns ? new CellRange(range.firstRow(), start, range.lastRow(), end) : new CellRange(start, range.firstColumn(), end, range.lastColumn());
        ws.cells().forEach(body, (r, c, cell) -> snapshot.put(new CellAddress(r, c), cell));
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i), to = start + i;
            if (from == to) continue;
            for (int j = byColumns ? range.firstRow() : range.firstColumn(); j <= (byColumns ? range.lastRow() : range.lastColumn()); j++) {
                int fr = byColumns ? j : from, fc = byColumns ? from : j, tr = byColumns ? j : to, tc = byColumns ? to : j;
                SheetCell cell = snapshot.get(new CellAddress(fr, fc));
                if (cell != null && cell.hasFormula()) cell = cell.withFormula(safeShift(cell.formula(), tr - fr, tc - fc), cell.value());
                tx.setCell(sheet, tr, tc, cell);
            }
        }
        if (!byColumns) tx.updateAxis(sheet, true, axis -> {
            Map<Integer, Integer> heights = new HashMap<>();
            Map<Integer, Boolean> hidden = new HashMap<>();
            for (int i = start; i <= end; i++) { heights.put(i, axis.hasCustomSize(i) ? axis.rawSize(i) : -1); hidden.put(i, axis.isHidden(i)); }
            for (int i = 0; i < order.size(); i++) {
                int from = order.get(i), to = start + i;
                int h = heights.get(from);
                if (h < 0) axis.resetSize(to); else axis.setSize(to, h);
            }
        });
    }

    private static int indexIn(List<String> order, CellValue v) {
        String s = v.display();
        for (int i = 0; i < order.size(); i++) if (order.get(i).equalsIgnoreCase(s)) return i;
        return order.size();
    }

    public static void renameSheet(SheetTransaction tx, int sheet, String newName) {
        SheetWorkbook.validateSheetName(newName);
        SheetWorkbook wb = tx.workbook();
        int existing = wb.indexOf(newName);
        if (existing >= 0 && existing != sheet) throw new IllegalArgumentException("Já existe uma planilha com esse nome.");
        String old = wb.sheet(sheet).name();
        tx.updateProperties(sheet, p -> p.withName(newName));
        rewriteFormulas(tx, f -> ReferenceAdjuster.renameSheet(f, old, newName));
        tx.updateWorkbook(p -> p.withNames(p.names().stream().map(n -> { try { return n.withFormula(ReferenceAdjuster.renameSheet(n.formula(), old, newName)); } catch (RuntimeException e) { return n; } }).toList()));
        tx.markStructural();
    }

    public static void rewriteFormulas(SheetTransaction tx, Function<String, String> change) {
        SheetWorkbook wb = tx.workbook();
        for (int s = 0; s < wb.sheetCount(); s++) {
            List<Object[]> updates = new ArrayList<>();
            wb.sheet(s).cells().forEach((r, c, cell) -> {
                if (!cell.hasFormula()) return;
                try {
                    String next = change.apply(cell.formula());
                    if (!next.equals(cell.formula())) updates.add(new Object[]{r, c, cell.withFormula(next, cell.value())});
                } catch (RuntimeException ignored) { }
            });
            for (Object[] u : updates) tx.setCell(s, (int) u[0], (int) u[1], (SheetCell) u[2]);
        }
    }

    public static int addSheet(SheetTransaction tx, int index, String name) {
        SheetWorkbook wb = tx.workbook();
        String n = name == null ? wb.nextSheetName() : name;
        SheetWorkbook.validateSheetName(n);
        if (wb.indexOf(n) >= 0) throw new IllegalArgumentException("Já existe uma planilha com esse nome.");
        List<SheetWorksheet> list = new ArrayList<>(wb.sheets());
        int at = Math.max(0, Math.min(index, list.size()));
        list.add(at, new SheetWorksheet(n));
        tx.replaceSheets(list);
        return at;
    }

    public static int duplicateSheet(SheetTransaction tx, int index) {
        SheetWorkbook wb = tx.workbook();
        SheetWorksheet source = wb.sheet(index);
        String name = wb.uniqueSheetName(source.name().length() > 26 ? source.name().substring(0, 26) : source.name());
        SheetWorksheet copy = source.duplicate(name);
        List<SheetWorksheet> list = new ArrayList<>(wb.sheets());
        list.add(index + 1, copy);
        tx.replaceSheets(list);
        List<SheetTable> tables = new ArrayList<>();
        int nextId = wb.nextTableId();
        for (SheetTable t : copy.properties().tables()) tables.add(t.withId(nextId).withName(t.name() + "_" + (nextId++)));
        tx.updateProperties(index + 1, p -> p.withTables(tables).withPivots(List.of()));
        return index + 1;
    }

    public static void deleteSheet(SheetTransaction tx, int index) {
        SheetWorkbook wb = tx.workbook();
        if (wb.sheetCount() <= 1) throw new IllegalStateException("A pasta de trabalho precisa ter pelo menos uma planilha.");
        String name = wb.sheet(index).name();
        List<SheetWorksheet> list = new ArrayList<>(wb.sheets());
        list.remove(index);
        tx.replaceSheets(list);
        rewriteFormulas(tx, f -> ReferenceAdjuster.sheetDeleted(f, name));
        tx.updateWorkbook(p -> p.withNames(p.names().stream().filter(n -> !Objects.equals(n.sheetScope(), index)).map(n -> n.sheetScope() != null && n.sheetScope() > index ? new DefinedName(n.name(), n.formula(), n.sheetScope() - 1, n.hidden(), n.comment()) : n).toList()));
    }

    public static void moveSheet(SheetTransaction tx, int from, int to) {
        SheetWorkbook wb = tx.workbook();
        List<SheetWorksheet> list = new ArrayList<>(wb.sheets());
        SheetWorksheet s = list.remove(from);
        list.add(Math.max(0, Math.min(to, list.size())), s);
        tx.replaceSheets(list);
    }

    public static void merge(SheetTransaction tx, int sheet, CellRange range, boolean across) {
        if (range.isSingleCell()) return;
        List<CellRange> targets = new ArrayList<>();
        if (across) for (int r = range.firstRow(); r <= range.lastRow(); r++) targets.add(new CellRange(r, range.firstColumn(), r, range.lastColumn()));
        else targets.add(range);
        for (CellRange t : targets) {
            if (t.isSingleCell()) continue;
            for (CellAddress a : tx.sheet(sheet).cells().addresses(t)) if (!a.equals(t.first())) tx.updateCell(sheet, a.row(), a.column(), SheetCell::cleared);
        }
        tx.updateProperties(sheet, p -> {
            List<CellRange> merges = new ArrayList<>();
            for (CellRange m : p.merges()) if (!m.intersects(range)) merges.add(m);
            for (CellRange t : targets) if (!t.isSingleCell()) merges.add(t);
            return p.withMerges(merges);
        });
    }

    public static void unmerge(SheetTransaction tx, int sheet, CellRange range) {
        tx.updateProperties(sheet, p -> {
            List<CellRange> merges = new ArrayList<>();
            for (CellRange m : p.merges()) if (!m.intersects(range)) merges.add(m);
            return p.withMerges(merges);
        });
    }
}
