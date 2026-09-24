package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.WordEditor;
import javax.swing.JComponent;

public interface WordToolbarContributor extends WordProvider {
    JComponent createToolbar(WordEditor editor);
}
