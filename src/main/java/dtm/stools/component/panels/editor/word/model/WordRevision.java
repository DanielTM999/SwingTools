package dtm.stools.component.panels.editor.word.model;

import java.time.Instant;
import java.util.Objects;

public record WordRevision(Type type, String author, Instant date) {
    public enum Type { INSERT, DELETE }
    public WordRevision { Objects.requireNonNull(type); Objects.requireNonNull(author); Objects.requireNonNull(date); }
}
