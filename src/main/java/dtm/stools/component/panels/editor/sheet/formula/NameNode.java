package dtm.stools.component.panels.editor.sheet.formula;

public record NameNode(String sheet, String name) implements FormulaNode {
    @Override public boolean isReference() { return true; }
}
