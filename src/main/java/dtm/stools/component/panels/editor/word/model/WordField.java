package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordField(String id, Kind kind, String argument, String cachedText) implements WordInlineObject {
    public static final String TYPE = "field";
    public enum Kind { PAGE, NUM_PAGES, DATE, REF, PAGE_REF, SEQ }
    public WordField {
        WordInlineObject.requireId(id); Objects.requireNonNull(kind);
        argument = argument == null ? "" : argument;
        cachedText = cachedText == null ? "" : cachedText;
        if ((kind == Kind.REF || kind == Kind.PAGE_REF || kind == Kind.SEQ) && argument.isBlank()) throw new IllegalArgumentException("Field argument required");
    }
    public static WordField of(Kind kind, String argument) { return new WordField(WordIds.next(),kind,argument,""); }
    @Override public String type() { return TYPE; }
    @Override public float width() { return 0; }
    @Override public float height() { return 0; }
    @Override public boolean resizable() { return false; }
    @Override public boolean textual() { return true; }
    @Override public WordField withId(String value) { return new WordField(value,kind,argument,cachedText); }
    public WordField withCachedText(String value) { return new WordField(id,kind,argument,value); }
    public String instruction() {
        return switch (kind) {
            case PAGE -> "PAGE"; case NUM_PAGES -> "NUMPAGES"; case DATE -> argument.isBlank() ? "DATE" : "DATE \\@ \"" + argument + "\"";
            case REF -> "REF " + argument + " \\h"; case PAGE_REF -> "PAGEREF " + argument + " \\h"; case SEQ -> "SEQ " + argument + " \\* ARABIC";
        };
    }
    @Override public String plainText() { return cachedText; }
}
