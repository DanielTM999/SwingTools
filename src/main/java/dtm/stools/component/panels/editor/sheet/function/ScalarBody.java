package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

@FunctionalInterface
public interface ScalarBody {
    CellValue apply(FunctionContext context, CellValue[] args);
}
