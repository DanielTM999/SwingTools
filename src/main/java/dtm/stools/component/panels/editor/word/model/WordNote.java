package dtm.stools.component.panels.editor.word.model;

import java.util.List;
import java.util.Objects;

public record WordNote(String id, Kind kind, List<WordParagraph> paragraphs) {
    public enum Kind { FOOTNOTE, ENDNOTE }
    public WordNote {
        Objects.requireNonNull(id); Objects.requireNonNull(kind);
        paragraphs = List.copyOf(paragraphs);
        if (paragraphs.isEmpty()) paragraphs = List.of(WordParagraph.of(""));
    }
    public String text() { return String.join("\n",paragraphs.stream().map(WordParagraph::plainText).toList()); }
}
