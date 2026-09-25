package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

interface BinaryOperation {
    CellValue apply(CellValue a, CellValue b);
}
