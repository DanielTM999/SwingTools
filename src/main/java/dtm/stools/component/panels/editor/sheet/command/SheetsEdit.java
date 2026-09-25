package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

import java.util.List;

public record SheetsEdit(List<SheetWorksheet> before, List<SheetWorksheet> after) implements SheetEdit {
    public SheetsEdit { before = List.copyOf(before); after = List.copyOf(after); }
    @Override public void undo(SheetWorkbook wb) { wb.replaceSheets(before); }
    @Override public void redo(SheetWorkbook wb) { wb.replaceSheets(after); }
    @Override public boolean structural() { return true; }
}
