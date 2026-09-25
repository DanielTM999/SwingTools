package dtm.stools.component.panels.editor.sheet.formula;

import java.util.List;

public record ArrayNode(List<List<FormulaNode>> rows) implements FormulaNode {
    public ArrayNode { rows = rows.stream().map(List::copyOf).toList(); }

    public int rowCount() { return rows.size(); }
    public int columnCount() { return rows.stream().mapToInt(List::size).max().orElse(0); }
}
