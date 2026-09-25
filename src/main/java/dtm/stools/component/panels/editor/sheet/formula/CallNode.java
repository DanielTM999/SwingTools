package dtm.stools.component.panels.editor.sheet.formula;

import java.util.List;

public record CallNode(FormulaNode target, List<FormulaNode> args) implements FormulaNode {
    public CallNode { args = List.copyOf(args); }
}
