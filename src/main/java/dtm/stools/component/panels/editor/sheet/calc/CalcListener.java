package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.List;
import java.util.Map;

public interface CalcListener {
    default void calculationStarted(int formulas) {}
    default void valuesChanged(Map<String, List<CellRange>> changed) {}
    default void calculationFinished(CalcStatistics statistics) {}
}
