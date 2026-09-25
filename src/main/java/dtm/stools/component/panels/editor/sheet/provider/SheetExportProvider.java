package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

import java.io.IOException;
import java.io.OutputStream;

public interface SheetExportProvider extends SheetProvider {
    String extension();
    default String description() { return extension().toUpperCase(java.util.Locale.ROOT); }
    void export(SheetWorkbook workbook, CalcEngine values, SheetExportOptions options, OutputStream output) throws IOException;
}
