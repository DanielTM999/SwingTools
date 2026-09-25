package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;
import java.util.UUID;

public record SheetImage(String id, byte[] data, String format, ObjectAnchor anchor, String altText) implements SheetObject {
    public SheetImage {
        id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        Objects.requireNonNull(data); Objects.requireNonNull(anchor);
        format = Objects.requireNonNullElse(format, "png");
        altText = Objects.requireNonNullElse(altText, "");
    }

    @Override public SheetObject withAnchor(ObjectAnchor a) { return new SheetImage(id, data, format, a, altText); }
    @Override public String description() { return altText.isBlank() ? "Imagem" : altText; }
}
