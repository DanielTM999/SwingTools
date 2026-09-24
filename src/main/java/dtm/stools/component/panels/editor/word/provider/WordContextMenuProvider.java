package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.WordEditor;
import javax.swing.JPopupMenu;

public interface WordContextMenuProvider extends WordProvider {
    void contribute(WordEditor editor,JPopupMenu menu);
}
