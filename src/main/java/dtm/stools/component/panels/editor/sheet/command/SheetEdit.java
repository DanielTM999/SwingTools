package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

public interface SheetEdit {
    void undo(SheetWorkbook workbook);
    void redo(SheetWorkbook workbook);
    default boolean structural() { return false; }
}
