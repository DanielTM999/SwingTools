package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;
import java.util.UUID;

public record WordTableOfContents(UUID id, String title, int maxLevel) implements WordBlock {
    public WordTableOfContents {
        Objects.requireNonNull(id);
        title = title == null ? "" : title;
        if (maxLevel < 1 || maxLevel > 9) throw new IllegalArgumentException("Invalid TOC depth");
    }
    public static WordTableOfContents create() { return new WordTableOfContents(UUID.randomUUID(),"Sumário",3); }
    @Override public String plainText() { return title; }
}
