package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordRun(String text, WordTextStyle style) implements WordInline {
    public WordRun {
        Objects.requireNonNull(text); Objects.requireNonNull(style);
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) throw new IllegalArgumentException("Paragraph breaks belong to the document");
        if (text.indexOf(WordObjectRun.PLACEHOLDER) >= 0) throw new IllegalArgumentException("Object placeholders belong to object runs");
    }
    @Override public WordRun withStyle(WordTextStyle value) { return new WordRun(text,value); }
}
