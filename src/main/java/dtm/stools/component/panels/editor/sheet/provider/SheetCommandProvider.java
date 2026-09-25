package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import javax.swing.Action;
import java.util.Map;

public interface SheetCommandProvider extends SheetProvider {
    Map<String, Action> commands(SheetEditor editor);

    default String group() { return "Extensões"; }
}
