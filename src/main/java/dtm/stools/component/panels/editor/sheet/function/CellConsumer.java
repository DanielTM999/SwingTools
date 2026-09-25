package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

@FunctionalInterface
public interface CellConsumer {
    void accept(int sheet, int row, int column, CellValue value);
}
