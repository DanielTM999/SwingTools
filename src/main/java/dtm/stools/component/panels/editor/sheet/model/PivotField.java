package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;
import java.util.Set;

public record PivotField(String name, PivotGrouping grouping, double groupSize, boolean descending, Set<String> hiddenItems) {
    public PivotField {
        Objects.requireNonNull(name);
        grouping = Objects.requireNonNullElse(grouping, PivotGrouping.NONE);
        hiddenItems = hiddenItems == null ? Set.of() : Set.copyOf(hiddenItems);
    }

    public static PivotField of(String name) { return new PivotField(name, PivotGrouping.NONE, 0, false, Set.of()); }
    public PivotField withHidden(Set<String> items) { return new PivotField(name, grouping, groupSize, descending, items); }
    public PivotField withGrouping(PivotGrouping g, double size) { return new PivotField(name, g, size, descending, hiddenItems); }
}
