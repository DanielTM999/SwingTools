package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.WorkbookProperties;

public record WorkbookEdit(WorkbookProperties before, WorkbookProperties after) implements SheetEdit {
    @Override public void undo(SheetWorkbook wb) { wb.setProperties(before); }
    @Override public void redo(SheetWorkbook wb) { wb.setProperties(after); }
    @Override public boolean structural() { return !before.names().equals(after.names()) || before.date1904() != after.date1904(); }
}
