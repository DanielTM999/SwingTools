package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.Optional;

public interface SheetExternalDataProvider extends SheetProvider {
    boolean supports(String function);
    Optional<CellValue> fetch(ExternalDataRequest request);
}
