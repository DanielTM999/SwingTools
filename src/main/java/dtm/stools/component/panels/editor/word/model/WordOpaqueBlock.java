package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;
import java.util.UUID;

public record WordOpaqueBlock(UUID id, String label, String xml, String previewText) implements WordBlock {
    public WordOpaqueBlock {
        Objects.requireNonNull(id); Objects.requireNonNull(label); Objects.requireNonNull(xml);
        previewText = previewText == null ? "" : previewText;
    }
    @Override public String plainText() { return previewText; }
}
