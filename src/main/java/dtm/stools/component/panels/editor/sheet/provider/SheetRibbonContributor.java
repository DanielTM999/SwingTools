package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import java.util.List;

public interface SheetRibbonContributor extends SheetProvider {
    String tab();
    String group();
    List<String> commandIds(SheetEditor editor);
}
