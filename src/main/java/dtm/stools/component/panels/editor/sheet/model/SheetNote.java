package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record SheetNote(String author, String text, boolean visible) {
    public SheetNote {
        author = Objects.requireNonNullElse(author, "");
        text = Objects.requireNonNullElse(text, "");
    }
}
