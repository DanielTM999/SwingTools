package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

interface ValueOperation {
    CellValue apply(CellValue v);
}
