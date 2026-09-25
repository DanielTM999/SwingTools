package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

@FunctionalInterface
public interface RangeMapper {
    CellRange map(String sheet, CellRange range);
}
