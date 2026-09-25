package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.LOOKUP;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class LookupFunctions {
    private LookupFunctions() {}

    static void register(FunctionRegistry r) {
        raw(r, "VLOOKUP", LOOKUP, 3, 4, (c, a) -> tableLookup(c, a, true));
        raw(r, "HLOOKUP", LOOKUP, 3, 4, (c, a) -> tableLookup(c, a, false));
        raw(r, "MATCH", LOOKUP, 2, 3, (c, a) -> liftFirst(c, a, value -> {
            ArrayValue arr = c.toArray(a.value(1));
            if (arr.rows() > 1 && arr.columns() > 1) throw EvalError.na();
            List<CellValue> list = arr.list();
            int type = a.integer(2, 1);
            int idx = type == 0 ? exact(list, value, true, 1) : type > 0 ? approximate(list, value, true) : approximateDescending(list, value);
            if (idx < 0) throw EvalError.na();
            return num(idx + 1);
        }));
        raw365(r, "XMATCH", LOOKUP, 2, 4, (c, a) -> liftFirst(c, a, value -> {
            ArrayValue arr = c.toArray(a.value(1));
            if (arr.rows() > 1 && arr.columns() > 1) throw EvalError.value();
            int idx = xfind(arr.list(), value, a.integer(2, 0), a.integer(3, 1));
            if (idx < 0) throw EvalError.na();
            return num(idx + 1);
        }));
        r.register(FunctionDefinition.raw("XLOOKUP", LOOKUP, 3, 6, LookupFunctions::xlookup).modern().reference().build());
        raw(r, "LOOKUP", LOOKUP, 2, 3, (c, a) -> {
            CellValue value = a.scalar(0);
            ArrayValue vector = c.toArray(a.value(1));
            ArrayValue result;
            List<CellValue> keys;
            if (a.has(2)) { keys = vector.list(); result = c.toArray(a.value(2)); }
            else if (vector.columns() > vector.rows()) { keys = row(vector, 0); result = sliceRow(vector, vector.rows() - 1); }
            else { keys = column(vector, 0); result = sliceColumn(vector, vector.columns() - 1); }
            int idx = approximate(keys, value, false);
            if (idx < 0) throw EvalError.na();
            List<CellValue> out = result.list();
            if (idx >= out.size()) throw EvalError.na();
            return out.get(idx);
        });
        r.register(FunctionDefinition.raw("INDEX", LOOKUP, 1, 4, LookupFunctions::index).reference().build());
        r.register(FunctionDefinition.raw("OFFSET", LOOKUP, 3, 5, (c, a) -> {
            ReferenceValue ref = a.reference(0);
            if (!ref.isSingleArea()) throw EvalError.value();
            CellRange base = ref.range();
            int rows = a.integer(1), cols = a.integer(2);
            int height = a.has(3) ? a.integer(3) : base.rowCount(), width = a.has(4) ? a.integer(4) : base.columnCount();
            if (height == 0 || width == 0) throw EvalError.ref();
            int r1 = base.firstRow() + rows, c1 = base.firstColumn() + cols;
            int r2 = r1 + (height > 0 ? height - 1 : height + 1), c2 = c1 + (width > 0 ? width - 1 : width + 1);
            if (Math.min(r1, r2) < 0 || Math.min(c1, c2) < 0 || Math.max(r1, r2) >= CellAddress.MAX_ROWS || Math.max(c1, c2) >= CellAddress.MAX_COLUMNS) throw EvalError.ref();
            return ReferenceValue.of(ref.sheet(), new CellRange(r1, c1, r2, c2));
        }).volatileFunction().reference().build());
        r.register(FunctionDefinition.raw("INDIRECT", LOOKUP, 1, 2, (c, a) -> c.indirect(a.text(0), a.bool(1, true))).volatileFunction().reference().build());
        raw(r, "ROW", LOOKUP, 0, 1, (c, a) -> {
            if (!a.has(0)) return num(c.host().row() + 1);
            ReferenceValue ref = a.reference(0);
            CellRange range = ref.range();
            if (range.rowCount() == 1) return num(range.firstRow() + 1);
            CellRange clipped = range.isWholeColumn() ? c.clip(ref.sheet(), range) : range;
            ArrayValue out = ArrayValue.of(clipped.rowCount(), 1);
            for (int k = 0; k < clipped.rowCount(); k++) out.set(k, 0, num(clipped.firstRow() + k + 1));
            return out;
        });
        raw(r, "COLUMN", LOOKUP, 0, 1, (c, a) -> {
            if (!a.has(0)) return num(c.host().column() + 1);
            ReferenceValue ref = a.reference(0);
            CellRange range = ref.range();
            if (range.columnCount() == 1) return num(range.firstColumn() + 1);
            ArrayValue out = ArrayValue.of(1, range.columnCount());
            for (int k = 0; k < range.columnCount(); k++) out.set(0, k, num(range.firstColumn() + k + 1));
            return out;
        });
        raw(r, "ROWS", LOOKUP, 1, 1, (c, a) -> {
            CellValue v = a.value(0);
            if (v instanceof ReferenceValue ref && ref.isSingleArea()) return num(ref.range().rowCount());
            return num(c.toArray(v).rows());
        });
        raw(r, "COLUMNS", LOOKUP, 1, 1, (c, a) -> {
            CellValue v = a.value(0);
            if (v instanceof ReferenceValue ref && ref.isSingleArea()) return num(ref.range().columnCount());
            return num(c.toArray(v).columns());
        });
        raw(r, "AREAS", LOOKUP, 1, 1, (c, a) -> num(a.reference(0).areas().size() * (a.reference(0).sheetEnd() - a.reference(0).sheet() + 1)));
        scalar(r, "ADDRESS", LOOKUP, 2, 5, (c, a) -> {
            int row = i(a, 0), col = i(a, 1), abs = i(a, 2, 1);
            boolean a1 = b(a, 3, true);
            if (row < 1 || col < 1 || row > CellAddress.MAX_ROWS || col > CellAddress.MAX_COLUMNS || abs < 1 || abs > 4) throw EvalError.value();
            String out;
            if (a1) {
                String colText = (abs == 1 || abs == 3 ? "$" : "") + CellAddress.columnName(col - 1);
                String rowText = (abs == 1 || abs == 2 ? "$" : "") + row;
                out = colText + rowText;
            } else {
                String rowText = abs == 1 || abs == 2 ? "R" + row : "R[" + row + "]";
                String colText = abs == 1 || abs == 3 ? "C" + col : "C[" + col + "]";
                out = rowText + colText;
            }
            if (given(a, 4)) { String sheet = t(a, 4); if (!sheet.isEmpty()) out = FormulaPrinter.sheet(sheet) + "!" + out; }
            return text(out);
        });
        raw(r, "CHOOSE", LOOKUP, 2, 255, (c, a) -> {
            CellValue idxValue = c.deref(a.value(0));
            if (idxValue instanceof ArrayValue arr) {
                return arr.map(x -> {
                    try {
                        int k = (int) Coerce.number(x);
                        if (k < 1 || k >= a.size()) return err(CellError.VALUE);
                        return c.scalar(a.value(k));
                    } catch (EvalError e) { return e.toValue(); }
                });
            }
            int k = (int) Coerce.number(idxValue);
            if (k < 1 || k >= a.size()) throw EvalError.value();
            return a.value(k);
        });
        raw(r, "TRANSPOSE", LOOKUP, 1, 1, (c, a) -> c.toArray(a.value(0)).transpose());
        raw(r, "HYPERLINK", LOOKUP, 1, 2, (c, a) -> a.has(1) ? a.scalar(1) : a.scalar(0));
        raw(r, "RTD", LOOKUP, 3, 255, (c, a) -> err(CellError.NA));
        raw(r, "GETPIVOTDATA", LOOKUP, 2, 254, PivotSupport::getPivotData);
    }

    private static CellValue liftFirst(FunctionContext c, FunctionArgs a, ValueOperation op) {
        CellValue v = c.deref(a.value(0));
        if (v instanceof ArrayValue arr) return arr.map(x -> { try { return op.apply(x); } catch (EvalError e) { return e.toValue(); } });
        if (v instanceof ErrorValue) return v;
        return op.apply(v);
    }

    private static CellValue tableLookup(FunctionContext c, FunctionArgs a, boolean vertical) {
        ArrayValue table = c.toArray(a.value(1));
        boolean approx = !a.has(3) || a.bool(3);
        return liftFirst(c, a, value -> {
            if (value.isEmpty()) value = CellValue.ZERO;
            int col = (int) Coerce.number(c.scalar(a.value(2)));
            int size = vertical ? table.columns() : table.rows();
            if (col < 1) throw EvalError.value();
            if (col > size) throw EvalError.ref();
            List<CellValue> keys = vertical ? column(table, 0) : row(table, 0);
            int idx = approx ? approximate(keys, value, true) : exact(keys, value, true, 1);
            if (idx < 0) throw EvalError.na();
            return vertical ? table.get(idx, col - 1) : table.get(col - 1, idx);
        });
    }

    static List<CellValue> column(ArrayValue a, int col) { List<CellValue> l = new ArrayList<>(a.rows()); for (int k = 0; k < a.rows(); k++) l.add(a.get(k, col)); return l; }
    static List<CellValue> row(ArrayValue a, int row) { List<CellValue> l = new ArrayList<>(a.columns()); for (int k = 0; k < a.columns(); k++) l.add(a.get(row, k)); return l; }

    static ArrayValue sliceRow(ArrayValue a, int row) { return ArrayValue.row(row(a, row)); }
    static ArrayValue sliceColumn(ArrayValue a, int col) { return ArrayValue.column(column(a, col)); }

    static boolean matches(CellValue candidate, CellValue value, boolean wildcards) {
        if (candidate instanceof ErrorValue || value instanceof ErrorValue) return false;
        if (value instanceof TextValue t && wildcards && Lib.hasWildcard(t.value())) {
            if (!(candidate instanceof TextValue ct)) return false;
            return Lib.wildcard(t.value()).matcher(ct.value()).matches();
        }
        if (Coerce.typeRank(candidate) != Coerce.typeRank(value) && !(candidate.isEmpty() && value.isEmpty())) return false;
        return Coerce.equalsValue(candidate, value);
    }

    static int exact(List<CellValue> list, CellValue value, boolean wildcards, int direction) {
        Pattern p = value instanceof TextValue t && wildcards && Lib.hasWildcard(t.value()) ? Lib.wildcard(t.value()) : null;
        int n = list.size();
        for (int k = 0; k < n; k++) {
            int idx = direction > 0 ? k : n - 1 - k;
            CellValue cand = list.get(idx);
            if (p != null) { if (cand instanceof TextValue ct && p.matcher(ct.value()).matches()) return idx; continue; }
            if (matches(cand, value, false)) return idx;
        }
        return -1;
    }

    static int approximate(List<CellValue> list, CellValue value, boolean binary) {
        int lo = 0, hi = list.size() - 1, result = -1;
        int rank = Coerce.typeRank(value);
        if (!binary) {
            for (int k = 0; k < list.size(); k++) {
                CellValue v = list.get(k);
                if (Coerce.typeRank(v) != rank) continue;
                if (Coerce.compare(v, value) <= 0) result = k; else break;
            }
            return result;
        }
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            CellValue v = list.get(mid);
            int probe = mid;
            while (probe <= hi && (Coerce.typeRank(list.get(probe)) != rank)) probe++;
            if (probe > hi) { hi = mid - 1; continue; }
            v = list.get(probe);
            int cmp = Coerce.compare(v, value);
            if (cmp <= 0) { result = probe; lo = probe + 1; }
            else hi = mid - 1;
        }
        return result;
    }

    static int approximateDescending(List<CellValue> list, CellValue value) {
        int result = -1;
        for (int k = 0; k < list.size(); k++) {
            CellValue v = list.get(k);
            if (Coerce.typeRank(v) != Coerce.typeRank(value)) continue;
            if (Coerce.compare(v, value) >= 0) result = k; else break;
        }
        return result;
    }

    static int xfind(List<CellValue> list, CellValue value, int matchMode, int searchMode) {
        if (matchMode < -1 && matchMode != 2 || matchMode > 2 || !(searchMode == 1 || searchMode == -1 || searchMode == 2 || searchMode == -2)) throw EvalError.value();
        int n = list.size();
        if (Math.abs(searchMode) == 2) {
            int lo = 0, hi = n - 1, found = -1, best = -1;
            boolean asc = searchMode == 2;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int cmp = Coerce.typeRank(list.get(mid)) == Coerce.typeRank(value) ? Coerce.compare(list.get(mid), value) : Integer.compare(Coerce.typeRank(list.get(mid)), Coerce.typeRank(value));
                if (!asc) cmp = -cmp;
                if (cmp == 0) { found = mid; break; }
                if (cmp < 0) { if (matchMode == -1 && asc || matchMode == 1 && !asc) best = mid; lo = mid + 1; }
                else { if (matchMode == 1 && asc || matchMode == -1 && !asc) best = mid; hi = mid - 1; }
            }
            if (found >= 0) return found;
            return matchMode == 0 || matchMode == 2 ? -1 : best;
        }
        int direction = searchMode > 0 ? 1 : -1;
        if (matchMode == 0 || matchMode == 2) return exact(list, value, matchMode == 2, direction);
        int best = -1;
        CellValue bestValue = null;
        for (int k = 0; k < n; k++) {
            int idx = direction > 0 ? k : n - 1 - k;
            CellValue v = list.get(idx);
            if (v instanceof ErrorValue || Coerce.typeRank(v) != Coerce.typeRank(value)) continue;
            int cmp = Coerce.compare(v, value);
            if (cmp == 0) return idx;
            if (matchMode == -1 && cmp < 0 && (bestValue == null || Coerce.compare(v, bestValue) > 0)) { best = idx; bestValue = v; }
            if (matchMode == 1 && cmp > 0 && (bestValue == null || Coerce.compare(v, bestValue) < 0)) { best = idx; bestValue = v; }
        }
        return best;
    }

    private static CellValue xlookup(FunctionContext c, FunctionArgs a) {
        CellValue lookupRaw = a.value(1), returnRaw = a.value(2);
        ArrayValue lookup = c.toArray(lookupRaw);
        boolean vertical = lookup.columns() == 1;
        if (lookup.rows() > 1 && lookup.columns() > 1) throw EvalError.value();
        List<CellValue> keys = lookup.list();
        int matchMode = a.integer(4, 0), searchMode = a.integer(5, 1);
        CellValue notFound = a.has(3) ? a.value(3) : null;
        CellValue value = c.deref(a.value(0));
        if (value instanceof ArrayValue arr) {
            ArrayValue ret = c.toArray(returnRaw);
            return arr.map(x -> {
                try {
                    int idx = xfind(keys, x, matchMode, searchMode);
                    if (idx < 0) return notFound != null ? c.scalar(notFound) : err(CellError.NA);
                    return vertical ? ret.get(Math.min(idx, ret.rows() - 1), 0) : ret.get(0, Math.min(idx, ret.columns() - 1));
                } catch (EvalError e) { return e.toValue(); }
            });
        }
        if (value instanceof ErrorValue) return value;
        int idx = xfind(keys, value, matchMode, searchMode);
        if (idx < 0) return notFound != null ? notFound : err(CellError.NA);
        if (returnRaw instanceof ReferenceValue ref && ref.isSingleArea()) {
            CellRange rr = ref.range();
            if (vertical) {
                if (idx >= rr.rowCount() && !rr.isWholeColumn()) throw EvalError.value();
                return ReferenceValue.of(ref.sheet(), new CellRange(rr.firstRow() + idx, rr.firstColumn(), rr.firstRow() + idx, rr.lastColumn()));
            }
            if (idx >= rr.columnCount()) throw EvalError.value();
            return ReferenceValue.of(ref.sheet(), new CellRange(rr.firstRow(), rr.firstColumn() + idx, rr.lastRow(), rr.firstColumn() + idx));
        }
        ArrayValue ret = c.toArray(returnRaw);
        if (vertical) {
            if (idx >= ret.rows()) throw EvalError.value();
            return ret.columns() == 1 ? ret.get(idx, 0) : sliceRow(ret, idx);
        }
        if (idx >= ret.columns()) throw EvalError.value();
        return ret.rows() == 1 ? ret.get(0, idx) : sliceColumn(ret, idx);
    }

    private static CellValue index(FunctionContext c, FunctionArgs a) {
        CellValue source = a.value(0);
        CellValue rowArg = a.has(1) ? c.deref(a.value(1)) : CellValue.ZERO;
        CellValue colArg = a.has(2) ? c.deref(a.value(2)) : OmittedValue.INSTANCE;
        if (rowArg instanceof ArrayValue || colArg instanceof ArrayValue) {
            ArrayValue rows = c.toArray(rowArg), cols = c.toArray(colArg instanceof OmittedValue ? CellValue.ZERO : colArg);
            int h = Math.max(rows.rows(), cols.rows()), w = Math.max(rows.columns(), cols.columns());
            ArrayValue out = ArrayValue.of(h, w);
            ArrayValue arr = c.toArray(source);
            for (int i = 0; i < h; i++) for (int j = 0; j < w; j++) {
                try {
                    int rr = (int) Coerce.number(rows.broadcast(i, j)), cc = colArg instanceof OmittedValue ? 0 : (int) Coerce.number(cols.broadcast(i, j));
                    if (colArg instanceof OmittedValue && (arr.rows() == 1)) { cc = rr; rr = 1; }
                    else if (colArg instanceof OmittedValue) cc = 1;
                    out.set(i, j, arr.get(rr - 1, cc - 1));
                } catch (EvalError | IndexOutOfBoundsException e) { out.set(i, j, err(CellError.REF)); }
            }
            return out;
        }
        int row = (int) Coerce.number(rowArg);
        int col = colArg instanceof OmittedValue ? -1 : (int) Coerce.number(colArg);
        if (row < 0 || col < -1) throw EvalError.value();
        if (source instanceof ReferenceValue ref) {
            int area = a.integer(3, 1);
            if (area < 1 || area > ref.areas().size()) throw EvalError.ref();
            CellRange range = ref.areas().get(area - 1);
            if (col == -1) {
                if (range.rowCount() == 1 && range.columnCount() > 1) { col = row; row = 1; }
                else if (range.columnCount() == 1) col = 1;
                else col = 0;
            }
            if (row > range.rowCount() || col > range.columnCount()) throw EvalError.ref();
            int r1 = row == 0 ? range.firstRow() : range.firstRow() + row - 1, r2 = row == 0 ? range.lastRow() : r1;
            int c1 = col == 0 ? range.firstColumn() : range.firstColumn() + col - 1, c2 = col == 0 ? range.lastColumn() : c1;
            return ReferenceValue.of(ref.sheet(), new CellRange(r1, c1, r2, c2));
        }
        ArrayValue arr = c.toArray(source);
        if (col == -1) {
            if (arr.rows() == 1) { col = row; row = 1; }
            else if (arr.columns() == 1) col = 1;
            else col = 0;
        }
        if (row > arr.rows() || col > arr.columns()) throw EvalError.ref();
        if (row == 0 && col == 0) return arr;
        if (row == 0) return sliceColumn(arr, col - 1);
        if (col == 0) return sliceRow(arr, row - 1);
        return arr.get(row - 1, col - 1);
    }

    static boolean isNumber(CellValue v) { return v instanceof NumberValue; }
}
