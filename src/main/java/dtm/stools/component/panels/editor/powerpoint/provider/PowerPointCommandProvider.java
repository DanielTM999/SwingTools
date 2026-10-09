package dtm.stools.component.panels.editor.powerpoint.provider;

import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;
import javax.swing.Action;
import java.util.Map;

public interface PowerPointCommandProvider extends PowerPointProvider {
    Map<String,Action> commands(PowerPointEditor editor);
}
