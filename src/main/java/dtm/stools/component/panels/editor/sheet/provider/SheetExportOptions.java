package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.Locale;

public record SheetExportOptions(int sheet, CellRange range, boolean allSheets, Locale locale, double dpi) {
    public static SheetExportOptions activeSheet(int sheet, Locale locale) { return new SheetExportOptions(sheet, null, false, locale, 150); }
    public static SheetExportOptions workbook(Locale locale) { return new SheetExportOptions(0, null, true, locale, 150); }
}
