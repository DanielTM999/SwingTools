package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.WordEditor;
import javax.swing.Action;
import java.util.Map;

public interface WordCommandProvider extends WordProvider {
    Map<String,Action> commands(WordEditor editor);
}
