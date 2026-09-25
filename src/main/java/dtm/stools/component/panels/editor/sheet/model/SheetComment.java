package dtm.stools.component.panels.editor.sheet.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SheetComment(String id, String author, String text, Instant created) {
    public SheetComment {
        id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        author = Objects.requireNonNullElse(author, "");
        text = Objects.requireNonNullElse(text, "");
        created = Objects.requireNonNullElseGet(created, Instant::now);
    }

    public static SheetComment of(String author, String text) { return new SheetComment(null, author, text, null); }
}
