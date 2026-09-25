package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

public record PropertiesEdit(String sheetId, SheetProperties before, SheetProperties after) implements SheetEdit {
    @Override public void undo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.setProperties(before); }
    @Override public void redo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.setProperties(after); }
    @Override public boolean structural() { return !before.tables().equals(after.tables()) || !before.name().equals(after.name()) || !before.pivots().equals(after.pivots()); }
}
