package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record FilterView(String id, String name, AutoFilter filter) {
    public FilterView { id = Objects.requireNonNullElseGet(id, () -> java.util.UUID.randomUUID().toString()); Objects.requireNonNull(name); Objects.requireNonNull(filter); }
}
