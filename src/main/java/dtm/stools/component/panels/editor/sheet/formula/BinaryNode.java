package dtm.stools.component.panels.editor.sheet.formula;

public record BinaryNode(String operator, FormulaNode left, FormulaNode right) implements FormulaNode {
    @Override public boolean isReference() { return operator.equals(":") || operator.equals(",") || operator.equals(" "); }
}
