package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Slicer(String id, String name, String tableName, String pivotName, String field, Set<String> selected, ObjectAnchor anchor) implements SheetObject {
    public Slicer {
        id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        Objects.requireNonNull(name); Objects.requireNonNull(field); Objects.requireNonNull(anchor);
        selected = selected == null ? Set.of() : Set.copyOf(selected);
    }

    @Override public SheetObject withAnchor(ObjectAnchor a) { return new Slicer(id, name, tableName, pivotName, field, selected, a); }
    public Slicer withSelected(Set<String> s) { return new Slicer(id, name, tableName, pivotName, field, s, anchor); }
    @Override public String description() { return name; }
}
