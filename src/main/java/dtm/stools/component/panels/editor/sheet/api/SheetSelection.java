package dtm.stools.component.panels.editor.sheet.api;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record SheetSelection(CellAddress active, CellAddress anchor, List<CellRange> ranges) {
    public SheetSelection {
        Objects.requireNonNull(active);
        anchor = Objects.requireNonNullElse(anchor, active);
        ranges = ranges == null || ranges.isEmpty() ? List.of(CellRange.of(active)) : List.copyOf(ranges);
    }

    public static SheetSelection of(CellAddress a) { return new SheetSelection(a, a, List.of(CellRange.of(a))); }
    public static SheetSelection of(CellRange r) { return new SheetSelection(r.first(), r.first(), List.of(r)); }
    public static SheetSelection of(CellAddress active, CellRange r) { return new SheetSelection(active, r.first(), List.of(r)); }
    public static SheetSelection home() { return of(new CellAddress(0, 0)); }

    public CellRange range() { return ranges.getLast(); }
    public CellRange bounds() { CellRange b = ranges.getFirst(); for (CellRange r : ranges) b = b.union(r); return b; }
    public boolean isSingleCell() { return ranges.size() == 1 && ranges.getFirst().isSingleCell(); }
    public boolean isMulti() { return ranges.size() > 1; }
    public boolean contains(int row, int column) { for (CellRange r : ranges) if (r.contains(row, column)) return true; return false; }

    public SheetSelection extendTo(CellAddress focus) {
        List<CellRange> list = new ArrayList<>(ranges);
        list.set(list.size() - 1, CellRange.of(anchor, focus));
        return new SheetSelection(active, anchor, list);
    }

    public SheetSelection addRange(CellRange r) {
        List<CellRange> list = new ArrayList<>(ranges);
        list.add(r);
        return new SheetSelection(r.first(), r.first(), list);
    }

    public SheetSelection withActive(CellAddress a) { return new SheetSelection(a, anchor, ranges); }
}
