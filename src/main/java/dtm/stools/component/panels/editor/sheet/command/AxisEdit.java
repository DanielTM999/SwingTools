package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;

public record AxisEdit(String sheetId, boolean rows, AxisIndex before, AxisIndex after) implements SheetEdit {
    @Override public void undo(SheetWorkbook wb) { apply(wb, before); }
    @Override public void redo(SheetWorkbook wb) { apply(wb, after); }

    private void apply(SheetWorkbook wb, AxisIndex value) {
        SheetWorksheet s = wb.sheetById(sheetId);
        if (s == null) return;
        if (rows) s.setRows(value.copy()); else s.setColumns(value.copy());
    }
}
