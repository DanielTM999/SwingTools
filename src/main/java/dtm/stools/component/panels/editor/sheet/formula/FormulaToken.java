package dtm.stools.component.panels.editor.sheet.formula;

public record FormulaToken(TokenType type, String text, int start, int end, Object value) {
    public boolean is(TokenType t) { return type == t; }
    public boolean isOperator(String op) { return type == TokenType.OPERATOR && text.equals(op); }
}
