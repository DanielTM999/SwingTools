package dtm.stools.component.panels.editor.sheet.format;

import dtm.stools.component.panels.editor.sheet.model.CellValue;

public record ParsedInput(CellValue value, String format, String formula) {
    public boolean isFormula() { return formula != null; }
    public static ParsedInput of(CellValue v) { return new ParsedInput(v, null, null); }
}
