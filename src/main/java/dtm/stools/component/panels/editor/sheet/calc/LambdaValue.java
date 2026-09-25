package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;
import java.util.Map;

public record LambdaValue(List<String> parameters, FormulaNode body, Map<String, CellValue> closure) implements CellValue {
    public LambdaValue { parameters = List.copyOf(parameters); closure = Map.copyOf(closure); }

    @Override public String toString() { return "LAMBDA(" + String.join(",", parameters) + ")"; }
}
