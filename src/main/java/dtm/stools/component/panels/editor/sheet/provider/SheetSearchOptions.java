package dtm.stools.component.panels.editor.sheet.provider;

public record SheetSearchOptions(boolean matchCase, boolean entireCell, boolean regex, boolean formulas, boolean workbook, boolean byColumns) {
    public static final SheetSearchOptions DEFAULT = new SheetSearchOptions(false, false, false, true, false, false);
}
