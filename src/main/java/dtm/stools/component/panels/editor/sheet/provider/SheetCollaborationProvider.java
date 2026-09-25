package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

public interface SheetCollaborationProvider extends SheetProvider {
    void localChange(SheetEditor editor, SheetCollaborationEvent event);

    default void selectionChanged(SheetEditor editor, String sheet, String range) {}
}
