package dtm.stools.component.panels.editor.word.api;

import java.util.Objects;

public record WordObjectSelection(int offset, String objectId) implements WordContentSelection {
    public WordObjectSelection { if (offset < 0) throw new IllegalArgumentException("Negative offset"); Objects.requireNonNull(objectId); }
    @Override public WordSelection range() { return new WordSelection(offset,offset+1); }
}
