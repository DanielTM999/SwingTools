package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record SheetCell(CellValue value, String formula, int style) {
    public static final SheetCell BLANK = new SheetCell(CellValue.EMPTY, null, 0);

    public SheetCell {
        value = Objects.requireNonNullElse(value, CellValue.EMPTY);
        if (formula != null && formula.isBlank()) formula = null;
        if (style < 0) style = 0;
    }

    public static SheetCell of(CellValue value) { return new SheetCell(value, null, 0); }
    public static SheetCell formula(String formula) { return new SheetCell(CellValue.EMPTY, formula, 0); }

    public boolean hasFormula() { return formula != null; }
    public boolean isBlank() { return formula == null && value.isEmpty() && style == 0; }
    public boolean hasContent() { return formula != null || !value.isEmpty(); }
    public SheetCell withValue(CellValue v) { return new SheetCell(v, null, style); }
    public SheetCell withFormula(String f, CellValue cached) { return new SheetCell(cached, f, style); }
    public SheetCell withStyle(int s) { return new SheetCell(value, formula, s); }
    public SheetCell cleared() { return new SheetCell(CellValue.EMPTY, null, style); }
}
