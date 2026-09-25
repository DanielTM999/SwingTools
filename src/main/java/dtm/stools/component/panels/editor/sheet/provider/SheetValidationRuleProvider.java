package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.Optional;

public interface SheetValidationRuleProvider extends SheetProvider {
    Optional<String> validate(SheetEditor editor, int sheet, CellAddress cell, CellValue value);
}
