package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;

public record SortKey(int column, boolean descending, SortOn on, Integer color, List<String> customOrder) {
    public SortKey { on = on == null ? SortOn.VALUES : on; customOrder = customOrder == null ? List.of() : List.copyOf(customOrder); }

    public static SortKey of(int column, boolean descending) { return new SortKey(column, descending, SortOn.VALUES, null, List.of()); }
}
