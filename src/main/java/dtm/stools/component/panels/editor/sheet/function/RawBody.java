package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

@FunctionalInterface
public interface RawBody {
    CellValue apply(FunctionContext context, FunctionArgs args);
}
