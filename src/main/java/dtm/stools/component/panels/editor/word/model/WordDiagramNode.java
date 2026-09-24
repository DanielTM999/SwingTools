package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordDiagramNode(String text, int level) {
    public WordDiagramNode {
        Objects.requireNonNull(text);
        if (level < 0 || level > 8) throw new IllegalArgumentException("Invalid diagram level");
    }
}
