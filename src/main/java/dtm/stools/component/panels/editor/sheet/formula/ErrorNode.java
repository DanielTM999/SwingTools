package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellError;

public record ErrorNode(CellError error) implements FormulaNode {
    @Override public boolean isReference() { return error == CellError.REF; }
}
