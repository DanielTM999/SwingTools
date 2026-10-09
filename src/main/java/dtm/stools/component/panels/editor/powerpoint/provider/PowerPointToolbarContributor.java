package dtm.stools.component.panels.editor.powerpoint.provider;

import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;
import javax.swing.JComponent;

public interface PowerPointToolbarContributor extends PowerPointProvider {
    JComponent createToolbar(PowerPointEditor editor);
}
