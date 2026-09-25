package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;

import java.util.Optional;

public interface SheetConditionalRuleProvider extends SheetProvider {
    Optional<DifferentialStyle> style(SheetEditor editor, int sheet, int row, int column, CellValue value);
}
