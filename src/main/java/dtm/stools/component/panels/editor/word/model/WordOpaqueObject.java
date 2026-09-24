package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordOpaqueObject(String id, String label, String xml, Level level, float width, float height, String previewText) implements WordInlineObject {
    public static final String TYPE = "opaque";
    public enum Level { RUN, PARAGRAPH }
    public WordOpaqueObject {
        WordInlineObject.requireId(id); Objects.requireNonNull(label); Objects.requireNonNull(xml); Objects.requireNonNull(level);
        WordInlineObject.checkSize(width,height);
        previewText = previewText == null ? "" : previewText;
    }
    @Override public String type() { return TYPE; }
    @Override public boolean resizable() { return false; }
    @Override public WordOpaqueObject withId(String value) { return new WordOpaqueObject(value,label,xml,level,width,height,previewText); }
    @Override public String plainText() { return previewText; }
}
