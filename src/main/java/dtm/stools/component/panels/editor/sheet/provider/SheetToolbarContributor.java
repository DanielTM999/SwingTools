package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import javax.swing.JComponent;

public interface SheetToolbarContributor extends SheetProvider {
    JComponent createToolbar(SheetEditor editor);
}
