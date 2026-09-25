package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

import java.util.List;

public record SheetImportResult(SheetWorkbook workbook, List<String> diagnostics, boolean editable, byte[] original, String format) {
    public SheetImportResult { diagnostics = List.copyOf(diagnostics); }
}
