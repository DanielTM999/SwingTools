package dtm.stools.component.panels.editor.sheet.formula;

public record UnaryNode(String operator, FormulaNode operand) implements FormulaNode {
    @Override public boolean isReference() { return operator.equals("#") || operator.equals("@") && operand.isReference(); }
}
