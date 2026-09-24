package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordListRef(String listId, int level) {
    public WordListRef {
        Objects.requireNonNull(listId);
        if (listId.isBlank() || level < 0 || level > 8) throw new IllegalArgumentException("Invalid list reference");
    }
    public WordListRef withLevel(int value) { return new WordListRef(listId,Math.max(0,Math.min(8,value))); }
}
