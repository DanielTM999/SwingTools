package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;

public interface SheetFunction {
    String name();
    FunctionCategory category();
    int minArgs();
    int maxArgs();
    CellValue call(FunctionContext context, FunctionArgs args);

    default boolean isVolatile() { return false; }
    default FunctionOrigin origin() { return FunctionOrigin.EXCEL; }
    default String description() { return ""; }
    default List<String> parameters() { return List.of(); }
    default boolean returnsReference() { return false; }
}
