package dtm.stools.component.panels.editor.word.api;

import dtm.stools.component.panels.editor.word.model.WordDocument;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record WordVersion(String id, Instant savedAt, Path file, String label, WordDocument snapshot) {
    public WordVersion { Objects.requireNonNull(id); Objects.requireNonNull(savedAt); Objects.requireNonNull(snapshot); label = label == null ? "" : label; }
}
