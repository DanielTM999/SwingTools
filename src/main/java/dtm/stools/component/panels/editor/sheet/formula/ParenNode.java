package dtm.stools.component.panels.editor.sheet.formula;

public record ParenNode(FormulaNode inner) implements FormulaNode {
    @Override public boolean isReference() { return inner.isReference(); }
}
