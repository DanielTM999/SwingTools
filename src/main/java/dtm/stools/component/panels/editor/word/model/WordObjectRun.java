package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordObjectRun(WordInlineObject object, WordTextStyle style) implements WordInline {
    public static final char PLACEHOLDER = '￼';
    public static final String TEXT = String.valueOf(PLACEHOLDER);
    public WordObjectRun { Objects.requireNonNull(object); Objects.requireNonNull(style); }
    public WordObjectRun(WordInlineObject object) { this(object,WordTextStyle.DEFAULT); }
    @Override public String text() { return TEXT; }
    @Override public WordObjectRun withStyle(WordTextStyle value) { return new WordObjectRun(object,value); }
    public WordObjectRun withObject(WordInlineObject value) { return new WordObjectRun(value,style); }
}
