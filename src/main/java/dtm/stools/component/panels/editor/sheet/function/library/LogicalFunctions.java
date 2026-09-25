package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.LambdaValue;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.NameNode;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.LAMBDA;
import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.LOGICAL;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class LogicalFunctions {
    private LogicalFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "TRUE", LOGICAL, 0, 0, (c, a) -> CellValue.TRUE);
        scalar(r, "FALSE", LOGICAL, 0, 0, (c, a) -> CellValue.FALSE);
        raw(r, "IF", LOGICAL, 1, 3, LogicalFunctions::ifFunction);
        raw(r, "IFS", LOGICAL, 2, 254, (c, a) -> {
            if (a.size() % 2 != 0) throw EvalError.value();
            CellValue first = c.deref(a.value(0));
            if (first instanceof ArrayValue) return liftIfs(c, a);
            for (int i = 0; i + 1 < a.size(); i += 2) {
                CellValue cond = a.scalar(i);
                if (cond instanceof ErrorValue) return cond;
                if (Coerce.bool(cond)) return a.value(i + 1);
            }
            return err(CellError.NA);
        });
        raw(r, "IFERROR", LOGICAL, 2, 2, (c, a) -> onError(c, a, false));
        raw(r, "IFNA", LOGICAL, 2, 2, (c, a) -> onError(c, a, true));
        raw(r, "AND", LOGICAL, 1, 255, (c, a) -> logical(c, a, 0));
        raw(r, "OR", LOGICAL, 1, 255, (c, a) -> logical(c, a, 1));
        raw(r, "XOR", LOGICAL, 1, 255, (c, a) -> logical(c, a, 2));
        scalar(r, "NOT", LOGICAL, 1, 1, (c, a) -> bool(!b(a, 0)));
        raw(r, "SWITCH", LOGICAL, 3, 254, (c, a) -> {
            CellValue expr = a.scalar(0);
            if (expr instanceof ErrorValue) return expr;
            int n = a.size();
            int pairs = (n - 1) / 2;
            for (int i = 0; i < pairs; i++) {
                CellValue v = a.scalar(1 + 2 * i);
                if (Coerce.equalsValue(expr, v) && Coerce.typeRank(expr) == Coerce.typeRank(v)) return a.value(2 + 2 * i);
            }
            return (n - 1) % 2 == 1 ? a.value(n - 1) : err(CellError.NA);
        });
        raw365(r, "LET", LAMBDA, 3, 253, LogicalFunctions::let);
        raw365(r, "LAMBDA", LAMBDA, 1, 254, (c, a) -> {
            List<String> params = new ArrayList<>();
            for (int i = 0; i < a.size() - 1; i++) {
                FormulaNode node = a.node(i);
                if (!(node instanceof NameNode n) || n.sheet() != null) throw EvalError.value();
                params.add(n.name().toUpperCase(Locale.ROOT));
            }
            return new LambdaValue(params, a.node(a.size() - 1), c.bindings());
        });
        raw365(r, "ISOMITTED", LAMBDA, 1, 1, (c, a) -> bool(a.value(0) instanceof OmittedValue));
    }

    private static CellValue ifFunction(FunctionContext c, FunctionArgs a) {
        CellValue cond = c.deref(a.value(0));
        if (cond instanceof ArrayValue arr) {
            CellValue whenTrue = a.has(1) ? c.deref(a.value(1)) : CellValue.ZERO, whenFalse = a.size() > 2 ? (a.has(2) ? c.deref(a.value(2)) : CellValue.ZERO) : CellValue.FALSE;
            int rows = arr.rows(), cols = arr.columns();
            if (whenTrue instanceof ArrayValue t) { rows = Math.max(rows, t.rows()); cols = Math.max(cols, t.columns()); }
            if (whenFalse instanceof ArrayValue f) { rows = Math.max(rows, f.rows()); cols = Math.max(cols, f.columns()); }
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
                CellValue x = arr.broadcast(i, j);
                CellValue v;
                if (x instanceof ErrorValue) v = x;
                else {
                    try {
                        boolean b = Coerce.bool(x);
                        CellValue pick = b ? whenTrue : whenFalse;
                        v = pick instanceof ArrayValue p ? p.broadcast(i, j) : pick;
                    } catch (EvalError e) { v = e.toValue(); }
                }
                out.set(i, j, v);
            }
            return out;
        }
        if (cond instanceof ErrorValue) return cond;
        boolean b = Coerce.bool(cond);
        if (b) return a.size() > 1 ? (a.has(1) ? a.value(1) : CellValue.ZERO) : CellValue.TRUE;
        if (a.size() > 2) return a.has(2) ? a.value(2) : CellValue.ZERO;
        return CellValue.FALSE;
    }

    private static CellValue liftIfs(FunctionContext c, FunctionArgs a) {
        ArrayValue shape = c.toArray(a.value(0));
        ArrayValue out = ArrayValue.of(shape.rows(), shape.columns());
        List<ArrayValue> conds = new ArrayList<>(), values = new ArrayList<>();
        for (int i = 0; i + 1 < a.size(); i += 2) { conds.add(c.toArray(a.value(i))); values.add(c.toArray(a.value(i + 1))); }
        for (int r = 0; r < shape.rows(); r++) for (int col = 0; col < shape.columns(); col++) {
            CellValue v = err(CellError.NA);
            for (int k = 0; k < conds.size(); k++) {
                CellValue cond = conds.get(k).broadcast(r, col);
                if (cond instanceof ErrorValue) { v = cond; break; }
                try { if (Coerce.bool(cond)) { v = values.get(k).broadcast(r, col); break; } } catch (EvalError e) { v = e.toValue(); break; }
            }
            out.set(r, col, v);
        }
        return out;
    }

    private static CellValue onError(FunctionContext c, FunctionArgs a, boolean onlyNa) {
        CellValue v = c.deref(a.value(0));
        if (v instanceof ArrayValue arr) {
            CellValue fallback = null;
            ArrayValue out = ArrayValue.of(arr.rows(), arr.columns());
            for (int k = 0; k < arr.size(); k++) {
                CellValue x = arr.at(k);
                if (x instanceof ErrorValue e && (!onlyNa || e.error() == CellError.NA)) {
                    if (fallback == null) fallback = a.has(1) ? c.scalar(a.value(1)) : CellValue.ZERO;
                    x = fallback.isEmpty() ? CellValue.ZERO : fallback;
                }
                out.set(k / arr.columns(), k % arr.columns(), x);
            }
            return out;
        }
        if (v instanceof ErrorValue e && (!onlyNa || e.error() == CellError.NA)) return a.has(1) ? a.value(1) : CellValue.ZERO;
        return a.value(0);
    }

    private static CellValue logical(FunctionContext c, FunctionArgs a, int mode) {
        int trues = 0, count = 0;
        for (int i = 0; i < a.size(); i++) {
            CellValue v = a.value(i);
            if (v instanceof ReferenceValue || v instanceof ArrayValue) {
                ArrayValue arr = c.toArray(v);
                for (int k = 0; k < arr.size(); k++) {
                    CellValue x = arr.at(k);
                    switch (x) {
                        case ErrorValue e -> throw EvalError.of(e.error());
                        case BoolValue b -> { count++; if (b.value()) trues++; }
                        case NumberValue n -> { count++; if (n.value() != 0) trues++; }
                        default -> { }
                    }
                }
                continue;
            }
            if (v instanceof OmittedValue) { count++; continue; }
            if (v instanceof TextValue t) {
                String u = t.value().toUpperCase(Locale.ROOT);
                if (u.equals("TRUE") || u.equals("VERDADEIRO")) { count++; trues++; continue; }
                if (u.equals("FALSE") || u.equals("FALSO")) { count++; continue; }
                throw EvalError.value();
            }
            count++;
            if (Coerce.bool(v)) trues++;
        }
        if (count == 0) throw EvalError.value();
        return switch (mode) {
            case 0 -> bool(trues == count);
            case 1 -> bool(trues > 0);
            default -> bool(trues % 2 == 1);
        };
    }

    private static CellValue let(FunctionContext c, FunctionArgs a) {
        if (a.size() % 2 == 0) throw EvalError.value();
        Map<String, CellValue> scope = new HashMap<>();
        for (int i = 0; i + 1 < a.size(); i += 2) {
            if (!(a.node(i) instanceof NameNode n) || n.sheet() != null) throw EvalError.value();
            CellValue v = c.evaluate(a.node(i + 1), scope);
            scope.put(n.name().toUpperCase(Locale.ROOT), v);
        }
        return c.evaluate(a.node(a.size() - 1), scope);
    }
}
