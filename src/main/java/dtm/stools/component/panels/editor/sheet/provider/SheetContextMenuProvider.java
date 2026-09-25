package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import javax.swing.JPopupMenu;

public interface SheetContextMenuProvider extends SheetProvider {
    void contribute(SheetEditor editor, SheetContextMenuContext context, JPopupMenu menu);
}
