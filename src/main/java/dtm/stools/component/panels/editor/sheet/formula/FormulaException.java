package dtm.stools.component.panels.editor.sheet.formula;

public class FormulaException extends IllegalArgumentException {
    private final int position;

    public FormulaException(String message, int position) { super(message); this.position = position; }

    public int position() { return position; }
}
