package dtm.stools.component.panels.editor.sheet.formula;

import java.util.List;
import java.util.Objects;

public record StructuredRefNode(String table, List<String> items, String firstColumn, String lastColumn, boolean thisRow) implements FormulaNode {
    public StructuredRefNode { items = items == null ? List.of() : List.copyOf(items); table = Objects.requireNonNullElse(table, ""); }

    @Override public boolean isReference() { return true; }
}
