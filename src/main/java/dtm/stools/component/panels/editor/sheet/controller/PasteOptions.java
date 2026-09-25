package dtm.stools.component.panels.editor.sheet.controller;

public record PasteOptions(What what, Operation operation, boolean skipBlanks, boolean transpose, boolean link) {
    public enum What { ALL, FORMULAS, VALUES, FORMATS, COMMENTS, VALIDATION, ALL_EXCEPT_BORDERS, COLUMN_WIDTHS, FORMULAS_AND_NUMBER_FORMATS, VALUES_AND_NUMBER_FORMATS }
    public enum Operation { NONE, ADD, SUBTRACT, MULTIPLY, DIVIDE }

    public static final PasteOptions ALL = new PasteOptions(What.ALL, Operation.NONE, false, false, false);
    public static PasteOptions of(What what) { return new PasteOptions(what, Operation.NONE, false, false, false); }
    public PasteOptions withTranspose(boolean value) { return new PasteOptions(what, operation, skipBlanks, value, link); }
}
