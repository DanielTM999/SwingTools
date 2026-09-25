package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.DATABASE;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class DatabaseFunctions {
    private DatabaseFunctions() {}

    static void register(FunctionRegistry r) {
        raw(r, "DSUM", DATABASE, 3, 3, (c, a) -> num(values(c, a).sum()));
        raw(r, "DAVERAGE", DATABASE, 3, 3, (c, a) -> num(values(c, a).mean()));
        raw(r, "DCOUNT", DATABASE, 2, 3, (c, a) -> num(values(c, a).size()));
        raw(r, "DCOUNTA", DATABASE, 2, 3, (c, a) -> num(rows(c, a, true).size()));
        raw(r, "DMAX", DATABASE, 3, 3, (c, a) -> { DoubleList l = values(c, a); return num(l.size() == 0 ? 0 : MathFunctions.max(l)); });
        raw(r, "DMIN", DATABASE, 3, 3, (c, a) -> { DoubleList l = values(c, a); return num(l.size() == 0 ? 0 : MathFunctions.min(l)); });
        raw(r, "DPRODUCT", DATABASE, 3, 3, (c, a) -> { DoubleList l = values(c, a); double p = l.size() == 0 ? 0 : 1; for (int k = 0; k < l.size(); k++) p *= l.get(k); return num(p); });
        raw(r, "DSTDEV", DATABASE, 3, 3, (c, a) -> num(Math.sqrt(variance(values(c, a), true))));
        raw(r, "DSTDEVP", DATABASE, 3, 3, (c, a) -> num(Math.sqrt(variance(values(c, a), false))));
        raw(r, "DVAR", DATABASE, 3, 3, (c, a) -> num(variance(values(c, a), true)));
        raw(r, "DVARP", DATABASE, 3, 3, (c, a) -> num(variance(values(c, a), false)));
        raw(r, "DGET", DATABASE, 3, 3, (c, a) -> {
            List<CellValue> v = rows(c, a, false);
            if (v.isEmpty()) throw EvalError.value();
            if (v.size() > 1) throw EvalError.num();
            return v.getFirst();
        });
    }

    private static DoubleList values(FunctionContext c, FunctionArgs a) {
        DoubleList out = new DoubleList();
        for (CellValue v : rows(c, a, false)) {
            if (v instanceof ErrorValue e) throw EvalError.of(e.error());
            if (v instanceof NumberValue n) out.add(n.value());
        }
        return out;
    }

    private static List<CellValue> rows(FunctionContext c, FunctionArgs a, boolean nonEmpty) {
        ArrayValue db = c.toArray(a.value(0));
        boolean hasField = a.size() > 2 && a.has(1);
        ArrayValue criteria = c.toArray(a.value(a.size() > 2 ? 2 : 1));
        int field = hasField ? field(db, a.scalar(1)) : -1;
        List<List<Predicate<CellValue>>> alternatives = new ArrayList<>();
        List<List<Integer>> columns = new ArrayList<>();
        for (int row = 1; row < criteria.rows(); row++) {
            List<Predicate<CellValue>> preds = new ArrayList<>();
            List<Integer> cols = new ArrayList<>();
            for (int col = 0; col < criteria.columns(); col++) {
                CellValue cond = criteria.get(row, col);
                if (cond.isEmpty()) continue;
                int dbCol = field(db, criteria.get(0, col));
                preds.add(criteria(cond));
                cols.add(dbCol);
            }
            alternatives.add(preds);
            columns.add(cols);
        }
        List<CellValue> out = new ArrayList<>();
        for (int row = 1; row < db.rows(); row++) {
            boolean match = alternatives.isEmpty();
            for (int k = 0; k < alternatives.size() && !match; k++) {
                boolean all = true;
                for (int j = 0; j < alternatives.get(k).size(); j++) if (!alternatives.get(k).get(j).test(db.get(row, columns.get(k).get(j)))) { all = false; break; }
                if (all) match = true;
            }
            if (!match) continue;
            if (field < 0) { out.add(CellValue.of(row)); continue; }
            CellValue v = db.get(row, field);
            if (nonEmpty && v.isEmpty()) continue;
            out.add(v);
        }
        return out;
    }

    private static int field(ArrayValue db, CellValue f) {
        if (f instanceof NumberValue n) {
            int k = (int) n.value();
            if (k < 1 || k > db.columns()) throw EvalError.value();
            return k - 1;
        }
        String name = Coerce.text(f);
        for (int col = 0; col < db.columns(); col++) if (db.get(0, col) instanceof TextValue t && t.value().equalsIgnoreCase(name)) return col;
        throw EvalError.value();
    }
}
