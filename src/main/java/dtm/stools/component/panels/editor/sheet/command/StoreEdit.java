package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.CellStore;

public record StoreEdit(String sheetId, CellStore before, CellStore after) implements SheetEdit {
    @Override public void undo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.setCells(before); }
    @Override public void redo(SheetWorkbook wb) { SheetWorksheet s = wb.sheetById(sheetId); if (s != null) s.setCells(after); }
    @Override public boolean structural() { return true; }
}
