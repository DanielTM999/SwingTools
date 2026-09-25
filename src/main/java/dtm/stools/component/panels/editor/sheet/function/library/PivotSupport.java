package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.PivotEngine;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PivotValueField;

import java.util.LinkedHashMap;
import java.util.Map;

final class PivotSupport {
    private PivotSupport() {}

    static CellValue getPivotData(FunctionContext c, FunctionArgs a) {
        String dataField = a.text(0);
        ReferenceValue ref = a.reference(1);
        PivotTable pivot = null;
        for (PivotTable p : c.workbook().sheet(ref.sheet()).properties().pivots()) {
            CellRange out = p.output() != null ? p.output() : CellRange.of(p.target());
            if (out.contains(ref.range().first())) { pivot = p; break; }
        }
        if (pivot == null) throw EvalError.ref();
        ArrayValue source = c.toArray(ReferenceValue.of(pivot.sourceSheet(), pivot.source()));
        PivotEngine.Result result = new PivotEngine(c.date1904(), c.locale()).compute(pivot, source);
        Map<String, String> items = new LinkedHashMap<>();
        for (int k = 2; k + 1 < a.size(); k += 2) items.put(a.text(k), Coerce.text(a.scalar(k + 1)));
        String caption = dataField;
        for (PivotValueField v : pivot.values()) if (v.field().equalsIgnoreCase(dataField) || v.caption().equalsIgnoreCase(dataField)) caption = v.caption();
        CellValue v = result.index().get(PivotEngine.fieldIndexKey(caption, items));
        return v == null ? CellValue.error(CellError.REF) : v;
    }
}
