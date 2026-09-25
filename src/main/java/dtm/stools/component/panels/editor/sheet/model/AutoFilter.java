package dtm.stools.component.panels.editor.sheet.model;

import java.util.HashMap;
import java.util.Map;

public record AutoFilter(CellRange range, Map<Integer, FilterCriteria> criteria, SortSpec sort) {
    public AutoFilter { criteria = Map.copyOf(criteria); }

    public static AutoFilter of(CellRange range) { return new AutoFilter(range, Map.of(), null); }

    public AutoFilter withCriteria(int column, FilterCriteria c) {
        Map<Integer, FilterCriteria> m = new HashMap<>(criteria);
        if (c == null) m.remove(column); else m.put(column, c);
        return new AutoFilter(range, m, sort);
    }

    public AutoFilter withSort(SortSpec s) { return new AutoFilter(range, criteria, s); }
    public AutoFilter withRange(CellRange r) { return new AutoFilter(r, criteria, sort); }
    public boolean isFiltered() { return !criteria.isEmpty(); }
}
