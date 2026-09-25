package dtm.stools.component.panels.editor.sheet.formula;

import java.util.List;
import java.util.Locale;

public record FunctionNode(String name, List<FormulaNode> args) implements FormulaNode {
    public FunctionNode { name = name.toUpperCase(Locale.ROOT); args = List.copyOf(args); }

    @Override public boolean isReference() {
        return switch (name) { case "INDEX", "OFFSET", "INDIRECT", "CHOOSE", "IF", "XLOOKUP", "SWITCH", "IFS", "LET", "TAKE", "DROP", "CHOOSEROWS", "CHOOSECOLS" -> true; default -> false; };
    }
}
