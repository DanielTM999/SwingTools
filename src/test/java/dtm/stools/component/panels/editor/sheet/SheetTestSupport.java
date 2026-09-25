package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

import java.util.Locale;

final class SheetTestSupport {
    static final FunctionRegistry REGISTRY = FunctionRegistry.defaults();

    final SheetWorkbook workbook = SheetWorkbook.create();
    final CalcEngine engine = new CalcEngine(REGISTRY);

    SheetTestSupport() {
        engine.setLocale(Locale.US);
        engine.setClock(() -> 1_780_000_000_000L);
        engine.attach(workbook);
    }

    SheetTestSupport set(String address, Object value) {
        CellAddress a = CellAddress.parse(address);
        SheetCell cell;
        if (value instanceof String s && s.startsWith("=")) cell = SheetCell.formula(Formulas.toCanonical(s, FormulaLocale.EN));
        else if (value instanceof String s) cell = SheetCell.of(new ValueParser(Locale.US, false).parse(s).value());
        else cell = SheetCell.of(CellValue.from(value));
        workbook.sheet(0).put(a.row(), a.column(), cell);
        return this;
    }

    SheetTestSupport recalc() { engine.rebuild(); return this; }

    CellValue value(String address) { return engine.valueAt(0, CellAddress.parse(address)); }

    CellValue eval(String formula) {
        set("ZZ1000", formula);
        engine.rebuild();
        return value("ZZ1000");
    }
}
