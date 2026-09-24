package dtm.stools.component.panels.editor.word.model;

import java.util.UUID;

public interface WordBlock {
    UUID id();
    default String label() { return getClass().getSimpleName(); }
    default String plainText() { return ""; }
}
