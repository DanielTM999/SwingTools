package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.model.WordDocument;
import java.util.Objects;

public record WordCollaborationEvent(long revision, String label, String author, WordDocument before, WordDocument after) {
    public WordCollaborationEvent { Objects.requireNonNull(label); Objects.requireNonNull(after); author = author == null ? "" : author; }
}
