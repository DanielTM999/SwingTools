package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;

public record ConditionalFormat(List<CellRange> ranges, List<ConditionalRule> rules) {
    public ConditionalFormat { ranges = List.copyOf(ranges); rules = List.copyOf(rules); }

    public static ConditionalFormat of(CellRange range, ConditionalRule rule) { return new ConditionalFormat(List.of(range), List.of(rule)); }

    public boolean appliesTo(int row, int column) { for (CellRange r : ranges) if (r.contains(row, column)) return true; return false; }
    public CellRange bounds() { CellRange b = ranges.getFirst(); for (CellRange r : ranges) b = b.union(r); return b; }
}
