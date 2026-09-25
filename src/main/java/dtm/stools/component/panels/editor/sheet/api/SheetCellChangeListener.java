package dtm.stools.component.panels.editor.sheet.api;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.List;

@FunctionalInterface
public interface SheetCellChangeListener {
    void cellsChanged(String sheet, List<CellRange> ranges, boolean valuesOnly);
}
