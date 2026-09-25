package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaParser;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ValidationEvaluator {
    private final CalcEngine engine;

    public ValidationEvaluator(CalcEngine engine) { this.engine = engine; }

    public Optional<DataValidation> find(int sheet, int row, int column) {
        for (DataValidation v : engine.workbook().sheet(sheet).properties().validations()) if (v.appliesTo(row, column)) return Optional.of(v);
        return Optional.empty();
    }

    public List<String> listItems(int sheet, DataValidation v, int row, int column) {
        List<String> items = new ArrayList<>();
        if (v.type() != ValidationType.LIST || v.formula1() == null) return items;
        String f = v.formula1().strip();
        if (f.startsWith("\"") && f.endsWith("\"")) {
            for (String s : f.substring(1, f.length() - 1).split("[,;]")) if (!s.isBlank()) items.add(s.strip());
            return items;
        }
        boolean looksLikeFormula = f.startsWith("=") || f.matches(".*[!:$(].*") || isRef(f);
        if (!looksLikeFormula) {
            for (String s : f.split("[,;]")) if (!s.isBlank()) items.add(s.strip());
            return items;
        }
        CellValue value = evaluateRaw(sheet, v, f, row, column);
        ArrayValue arr;
        if (value instanceof ReferenceValue ref) {
            CellRange r = ref.range();
            arr = ArrayValue.of(r.rowCount() > 5000 ? 5000 : r.rowCount(), r.columnCount());
            for (int i = 0; i < arr.rows(); i++) for (int j = 0; j < arr.columns(); j++) arr.set(i, j, engine.valueAt(ref.sheet(), r.firstRow() + i, r.firstColumn() + j));
        } else if (value instanceof ArrayValue a) arr = a;
        else arr = new ArrayValue(1, 1, new CellValue[]{value});
        for (CellValue x : arr.list()) if (!x.isEmpty() && !(x instanceof ErrorValue)) { String s = engine.formatter().text(x, "General"); if (!items.contains(s)) items.add(s); }
        return items;
    }

    private static boolean isRef(String f) {
        try { return FormulaParser.parse(f, FormulaLocale.EN).isReference(); } catch (RuntimeException e) { return false; }
    }

    private CellValue evaluateRaw(int sheet, DataValidation v, String formula, int row, int column) {
        String f = formula.startsWith("=") ? formula.substring(1) : formula;
        CellRange anchor = v.ranges().getFirst();
        try {
            String shifted = ReferenceAdjuster.shift(f, row - anchor.firstRow(), column - anchor.firstColumn());
            return engine.evaluateRaw(sheet, new CellAddress(row, column), dtm.stools.component.panels.editor.sheet.formula.Formulas.parseCanonical(shifted));
        } catch (RuntimeException e) {
            return CellValue.error(dtm.stools.component.panels.editor.sheet.model.CellError.NAME);
        }
    }

    private CellValue evaluate(int sheet, DataValidation v, String formula, int row, int column) {
        if (formula == null || formula.isBlank()) return CellValue.EMPTY;
        String f = formula.startsWith("=") ? formula.substring(1) : formula;
        CellRange anchor = v.ranges().getFirst();
        try {
            return engine.evaluate(sheet, new CellAddress(row, column), ReferenceAdjuster.shift(f, row - anchor.firstRow(), column - anchor.firstColumn()));
        } catch (RuntimeException e) {
            return CellValue.error(dtm.stools.component.panels.editor.sheet.model.CellError.NAME);
        }
    }

    public boolean isValid(int sheet, DataValidation v, int row, int column, CellValue value) {
        if (value.isEmpty()) return v.allowBlank();
        return switch (v.type()) {
            case ANY -> true;
            case CHECKBOX -> value instanceof BoolValue;
            case LIST -> {
                String s = engine.formatter().text(value, "General");
                for (String item : listItems(sheet, v, row, column)) if (item.equalsIgnoreCase(s)) yield true;
                yield false;
            }
            case CUSTOM -> {
                CellValue r = evaluate(sheet, v, v.formula1(), row, column);
                try { yield !(r instanceof ErrorValue) && Coerce.bool(r); } catch (RuntimeException e) { yield false; }
            }
            case TEXT_LENGTH -> compare(sheet, v, row, column, value.display().length());
            case WHOLE -> value instanceof NumberValue n && n.value() == Math.rint(n.value()) && compare(sheet, v, row, column, n.value());
            case DECIMAL, DATE, TIME -> value instanceof NumberValue n && compare(sheet, v, row, column, v.type() == ValidationType.TIME ? n.value() - Math.floor(n.value()) : n.value());
        };
    }

    private boolean compare(int sheet, DataValidation v, int row, int column, double x) {
        try {
            double a = Coerce.number(evaluate(sheet, v, v.formula1(), row, column));
            double b = v.operator().twoOperands() ? Coerce.number(evaluate(sheet, v, v.formula2(), row, column)) : 0;
            return v.operator().test(x, a, b);
        } catch (RuntimeException e) { return false; }
    }

    public List<CellAddress> invalidCells(int sheet) {
        List<CellAddress> out = new ArrayList<>();
        for (DataValidation v : engine.workbook().sheet(sheet).properties().validations()) {
            for (CellRange r : v.ranges()) {
                CellRange used = engine.usedRange(sheet);
                if (used == null) continue;
                CellRange clipped = r.intersection(used);
                if (clipped == null) continue;
                for (CellAddress a : clipped) if (!isValid(sheet, v, a.row(), a.column(), engine.valueAt(sheet, a))) out.add(a);
            }
        }
        return out;
    }
}
