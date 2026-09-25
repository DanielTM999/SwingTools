package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;
import java.util.UUID;

public record SheetShape(String id, ShapeType type, ObjectAnchor anchor, String text, Integer fill, Integer line, float lineWidth) implements SheetObject {
    public SheetShape {
        id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        Objects.requireNonNull(type); Objects.requireNonNull(anchor);
        text = Objects.requireNonNullElse(text, "");
    }

    @Override public SheetObject withAnchor(ObjectAnchor a) { return new SheetShape(id, type, a, text, fill, line, lineWidth); }
    public SheetShape withText(String t) { return new SheetShape(id, type, anchor, t, fill, line, lineWidth); }
    @Override public String description() { return text.isBlank() ? type.name() : text; }
}
