package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;

public record SheetContextMenuContext(Target target, int sheet, CellAddress cell, int index, SheetObject object) {
    public enum Target { CELL, ROW_HEADER, COLUMN_HEADER, SHEET_TAB, OBJECT }
}
