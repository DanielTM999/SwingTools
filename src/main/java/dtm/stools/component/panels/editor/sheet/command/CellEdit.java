package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

public record CellEdit(String sheetId, int row, int column, SheetCell before, SheetCell after) implements SheetEdit {
    @Override public void undo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.put(row, column, before); }
    @Override public void redo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.put(row, column, after); }
}
