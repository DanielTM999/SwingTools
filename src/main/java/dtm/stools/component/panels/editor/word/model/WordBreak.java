package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordBreak(String id, Kind kind) implements WordInlineObject {
    public static final String TYPE = "break";
    public enum Kind { LINE, PAGE, COLUMN }
    public WordBreak { WordInlineObject.requireId(id); Objects.requireNonNull(kind); }
    public static WordBreak of(Kind kind) { return new WordBreak(WordIds.next(),kind); }
    @Override public String type() { return TYPE; }
    @Override public float width() { return 0; }
    @Override public float height() { return 0; }
    @Override public boolean resizable() { return false; }
    @Override public boolean textual() { return true; }
    @Override public WordBreak withId(String value) { return new WordBreak(value,kind); }
    @Override public String plainText() { return kind == Kind.LINE ? " " : ""; }
}
