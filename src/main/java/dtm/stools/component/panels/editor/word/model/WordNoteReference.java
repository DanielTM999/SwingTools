package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordNoteReference(String id, String noteId) implements WordInlineObject {
    public static final String TYPE = "note";
    public WordNoteReference { WordInlineObject.requireId(id); Objects.requireNonNull(noteId); }
    @Override public String type() { return TYPE; }
    @Override public float width() { return 0; }
    @Override public float height() { return 0; }
    @Override public boolean resizable() { return false; }
    @Override public boolean textual() { return true; }
    @Override public WordNoteReference withId(String value) { return new WordNoteReference(value,noteId); }
}
