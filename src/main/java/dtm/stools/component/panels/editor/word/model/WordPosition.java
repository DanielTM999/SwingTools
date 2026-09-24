package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;
import java.util.UUID;

public record WordPosition(UUID paragraphId, int offset) {
    public WordPosition { Objects.requireNonNull(paragraphId); if (offset < 0) throw new IllegalArgumentException("Negative offset"); }
}
