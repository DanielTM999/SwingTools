package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DependencyIndex {
    private static final int SMALL = 16;
    private static final int BLOCK_SHIFT = 6;
    private static final int WIDE_BLOCKS = 16;

    private record RangeEntry(CellRange range, FormulaCell cell) {}

    private final Map<Integer, Map<Long, List<FormulaCell>>> cells = new HashMap<>();
    private final Map<Integer, Map<Integer, List<RangeEntry>>> blocks = new HashMap<>();
    private final Map<Integer, List<RangeEntry>> wide = new HashMap<>();

    void clear() { cells.clear(); blocks.clear(); wide.clear(); }

    void add(FormulaCell f) {
        for (Dependency d : f.dependencies) for (int s = d.sheet(); s <= d.sheetEnd(); s++) add(s, d.range(), f);
    }

    void remove(FormulaCell f) {
        for (Dependency d : f.dependencies) for (int s = d.sheet(); s <= d.sheetEnd(); s++) remove(s, d.range(), f);
    }

    private void add(int sheet, CellRange r, FormulaCell f) {
        if (r.cellCount() <= SMALL) {
            Map<Long, List<FormulaCell>> m = cells.computeIfAbsent(sheet, k -> new HashMap<>());
            for (int row = r.firstRow(); row <= r.lastRow(); row++)
                for (int c = r.firstColumn(); c <= r.lastColumn(); c++) m.computeIfAbsent(CellAddress.key(row, c), k -> new ArrayList<>(2)).add(f);
            return;
        }
        int b1 = r.firstColumn() >> BLOCK_SHIFT, b2 = r.lastColumn() >> BLOCK_SHIFT;
        RangeEntry e = new RangeEntry(r, f);
        if (b2 - b1 + 1 > WIDE_BLOCKS) { wide.computeIfAbsent(sheet, k -> new ArrayList<>()).add(e); return; }
        Map<Integer, List<RangeEntry>> m = blocks.computeIfAbsent(sheet, k -> new HashMap<>());
        for (int b = b1; b <= b2; b++) m.computeIfAbsent(b, k -> new ArrayList<>()).add(e);
    }

    private void remove(int sheet, CellRange r, FormulaCell f) {
        if (r.cellCount() <= SMALL) {
            Map<Long, List<FormulaCell>> m = cells.get(sheet);
            if (m == null) return;
            for (int row = r.firstRow(); row <= r.lastRow(); row++)
                for (int c = r.firstColumn(); c <= r.lastColumn(); c++) {
                    long k = CellAddress.key(row, c);
                    List<FormulaCell> l = m.get(k);
                    if (l != null) { l.remove(f); if (l.isEmpty()) m.remove(k); }
                }
            return;
        }
        int b1 = r.firstColumn() >> BLOCK_SHIFT, b2 = r.lastColumn() >> BLOCK_SHIFT;
        if (b2 - b1 + 1 > WIDE_BLOCKS) { List<RangeEntry> l = wide.get(sheet); if (l != null) l.removeIf(e -> e.cell == f && e.range.equals(r)); return; }
        Map<Integer, List<RangeEntry>> m = blocks.get(sheet);
        if (m == null) return;
        for (int b = b1; b <= b2; b++) { List<RangeEntry> l = m.get(b); if (l != null) { l.removeIf(e -> e.cell == f && e.range.equals(r)); if (l.isEmpty()) m.remove(b); } }
    }

    void dependents(int sheet, int row, int column, Set<FormulaCell> out) {
        Map<Long, List<FormulaCell>> m = cells.get(sheet);
        if (m != null) { List<FormulaCell> l = m.get(CellAddress.key(row, column)); if (l != null) out.addAll(l); }
        Map<Integer, List<RangeEntry>> bm = blocks.get(sheet);
        if (bm != null) { List<RangeEntry> l = bm.get(column >> BLOCK_SHIFT); if (l != null) for (RangeEntry e : l) if (e.range.contains(row, column)) out.add(e.cell); }
        List<RangeEntry> w = wide.get(sheet);
        if (w != null) for (RangeEntry e : w) if (e.range.contains(row, column)) out.add(e.cell);
    }

    void dependents(int sheet, CellRange range, Set<FormulaCell> out) {
        if (range.cellCount() <= 256) {
            for (int r = range.firstRow(); r <= range.lastRow(); r++) for (int c = range.firstColumn(); c <= range.lastColumn(); c++) dependents(sheet, r, c, out);
            return;
        }
        Map<Long, List<FormulaCell>> m = cells.get(sheet);
        if (m != null) for (Map.Entry<Long, List<FormulaCell>> e : m.entrySet()) if (range.contains(CellAddress.keyRow(e.getKey()), CellAddress.keyColumn(e.getKey()))) out.addAll(e.getValue());
        Map<Integer, List<RangeEntry>> bm = blocks.get(sheet);
        if (bm != null) for (int b = range.firstColumn() >> BLOCK_SHIFT; b <= range.lastColumn() >> BLOCK_SHIFT; b++) {
            List<RangeEntry> l = bm.get(b);
            if (l != null) for (RangeEntry e : l) if (e.range.intersects(range)) out.add(e.cell);
        }
        List<RangeEntry> w = wide.get(sheet);
        if (w != null) for (RangeEntry e : w) if (e.range.intersects(range)) out.add(e.cell);
    }

    Set<FormulaCell> dependents(int sheet, CellRange range) { Set<FormulaCell> s = new LinkedHashSet<>(); dependents(sheet, range, s); return s; }
}
