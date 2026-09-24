package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.api.WordCellSelection;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.model.WordInlineObject;
import dtm.stools.component.panels.editor.word.model.WordTable;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import java.awt.Component;
import java.util.Locale;
import java.util.Optional;

public interface WordObjectPropertiesContext {
    Component owner();
    Locale locale();
    WordDocument document();
    boolean readOnly();
    Optional<WordInlineObject> object();
    Optional<WordTable> table();
    Optional<WordCellSelection> cells();
    WordObjectRegistry registry();
    void applyObject(WordInlineObject updated);
    void applyTable(WordTable updated);
    void closed();
}
