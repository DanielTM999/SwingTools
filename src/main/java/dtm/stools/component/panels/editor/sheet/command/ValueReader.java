package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

@FunctionalInterface
public interface ValueReader {
    CellValue value(int row, int column);
}
