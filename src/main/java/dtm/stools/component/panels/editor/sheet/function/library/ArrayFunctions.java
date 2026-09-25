package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.LambdaValue;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.ARRAY;
import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.LAMBDA;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class ArrayFunctions {
    private ArrayFunctions() {}

    static void register(FunctionRegistry r) {
        raw365(r, "FILTER", ARRAY, 2, 3, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            ArrayValue include = c.toArray(a.value(1));
            boolean byRows = include.columns() == 1 && include.rows() == data.rows();
            boolean byColumns = include.rows() == 1 && include.columns() == data.columns() && !byRows;
            if (!byRows && !byColumns) throw EvalError.value();
            List<Integer> keep = new ArrayList<>();
            int size = byRows ? data.rows() : data.columns();
            for (int k = 0; k < size; k++) {
                CellValue v = include.at(k);
                if (v instanceof ErrorValue e) throw EvalError.of(e.error());
                if (Coerce.bool(v.isEmpty() ? CellValue.FALSE : v)) keep.add(k);
            }
            if (keep.isEmpty()) return a.has(2) ? a.value(2) : err(CellError.CALC);
            ArrayValue out = byRows ? ArrayValue.of(keep.size(), data.columns()) : ArrayValue.of(data.rows(), keep.size());
            for (int k = 0; k < keep.size(); k++) {
                int src = keep.get(k);
                if (byRows) for (int col = 0; col < data.columns(); col++) out.set(k, col, data.get(src, col));
                else for (int row = 0; row < data.rows(); row++) out.set(row, k, data.get(row, src));
            }
            return out;
        });
        raw365(r, "SORT", ARRAY, 1, 4, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            boolean byCol = a.bool(3, false);
            ArrayValue indexes = c.toArray(a.has(1) ? a.value(1) : CellValue.of(1));
            ArrayValue orders = c.toArray(a.has(2) ? a.value(2) : CellValue.of(1));
            List<int[]> keys = new ArrayList<>();
            for (int k = 0; k < indexes.size(); k++) {
                int idx = (int) Coerce.number(indexes.at(k));
                int order = (int) Coerce.number(orders.broadcast(k % orders.rows(), 0));
                if (orders.size() > 1 && k < orders.size()) order = (int) Coerce.number(orders.at(k));
                if (idx < 1 || idx > (byCol ? data.rows() : data.columns()) || order != 1 && order != -1) throw EvalError.value();
                keys.add(new int[]{idx - 1, order});
            }
            return sortArray(data, byCol, keys, null);
        });
        raw365(r, "SORTBY", ARRAY, 2, 254, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            List<ArrayValue> by = new ArrayList<>();
            List<Integer> orders = new ArrayList<>();
            for (int k = 1; k < a.size(); k += 2) {
                ArrayValue key = c.toArray(a.value(k));
                by.add(key);
                orders.add(k + 1 < a.size() && a.has(k + 1) ? a.integer(k + 1) : 1);
            }
            boolean byCol = by.getFirst().rows() == 1 && by.getFirst().columns() == data.columns() && data.rows() != by.getFirst().size();
            int n = byCol ? data.columns() : data.rows();
            for (ArrayValue key : by) if (key.size() != n) throw EvalError.value();
            Integer[] idx = new Integer[n];
            for (int k = 0; k < n; k++) idx[k] = k;
            java.util.Arrays.sort(idx, (x, y) -> {
                for (int k = 0; k < by.size(); k++) {
                    int cmp = compareSort(by.get(k).at(x), by.get(k).at(y));
                    if (cmp != 0) return orders.get(k) < 0 ? -cmp : cmp;
                }
                return 0;
            });
            return reorder(data, byCol, idx);
        });
        raw365(r, "UNIQUE", ARRAY, 1, 3, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            boolean byCol = a.bool(1, false), once = a.bool(2, false);
            int n = byCol ? data.columns() : data.rows();
            Map<String, List<Integer>> groups = new LinkedHashMap<>();
            for (int k = 0; k < n; k++) groups.computeIfAbsent(key(data, k, byCol), x -> new ArrayList<>()).add(k);
            List<Integer> keep = new ArrayList<>();
            for (List<Integer> g : groups.values()) if (!once || g.size() == 1) keep.add(g.getFirst());
            if (keep.isEmpty()) throw EvalError.calc();
            return reorder(data, byCol, keep.toArray(Integer[]::new));
        });
        raw365(r, "TAKE", ARRAY, 2, 3, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            int rows = a.has(1) ? a.integer(1) : data.rows(), cols = a.has(2) ? a.integer(2) : data.columns();
            if (rows == 0 || cols == 0) throw EvalError.calc();
            int r0 = rows > 0 ? 0 : Math.max(0, data.rows() + rows), r1 = rows > 0 ? Math.min(rows, data.rows()) : data.rows();
            int c0 = cols > 0 ? 0 : Math.max(0, data.columns() + cols), c1 = cols > 0 ? Math.min(cols, data.columns()) : data.columns();
            return slice(data, r0, r1, c0, c1);
        });
        raw365(r, "DROP", ARRAY, 2, 3, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            int rows = a.integer(1, 0), cols = a.integer(2, 0);
            int r0 = rows > 0 ? rows : 0, r1 = rows < 0 ? data.rows() + rows : data.rows();
            int c0 = cols > 0 ? cols : 0, c1 = cols < 0 ? data.columns() + cols : data.columns();
            if (r0 >= r1 || c0 >= c1) throw EvalError.calc();
            return slice(data, r0, r1, c0, c1);
        });
        raw365(r, "CHOOSEROWS", ARRAY, 2, 254, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            List<Integer> idx = new ArrayList<>();
            for (CellValue v : flattenArgs(c, a, 1)) {
                int k = (int) Coerce.number(v);
                if (k == 0 || Math.abs(k) > data.rows()) throw EvalError.value();
                idx.add(k > 0 ? k - 1 : data.rows() + k);
            }
            return reorder(data, false, idx.toArray(Integer[]::new));
        });
        raw365(r, "CHOOSECOLS", ARRAY, 2, 254, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            List<Integer> idx = new ArrayList<>();
            for (CellValue v : flattenArgs(c, a, 1)) {
                int k = (int) Coerce.number(v);
                if (k == 0 || Math.abs(k) > data.columns()) throw EvalError.value();
                idx.add(k > 0 ? k - 1 : data.columns() + k);
            }
            return reorder(data, true, idx.toArray(Integer[]::new));
        });
        raw365(r, "VSTACK", ARRAY, 1, 254, (c, a) -> {
            List<ArrayValue> parts = new ArrayList<>();
            int rows = 0, cols = 0;
            for (int k = 0; k < a.size(); k++) { ArrayValue p = c.toArray(a.value(k)); parts.add(p); rows += p.rows(); cols = Math.max(cols, p.columns()); }
            ArrayValue out = ArrayValue.of(rows, cols);
            int r0 = 0;
            for (ArrayValue p : parts) { for (int i = 0; i < p.rows(); i++) for (int j = 0; j < cols; j++) out.set(r0 + i, j, j < p.columns() ? p.get(i, j) : err(CellError.NA)); r0 += p.rows(); }
            return out;
        });
        raw365(r, "HSTACK", ARRAY, 1, 254, (c, a) -> {
            List<ArrayValue> parts = new ArrayList<>();
            int rows = 0, cols = 0;
            for (int k = 0; k < a.size(); k++) { ArrayValue p = c.toArray(a.value(k)); parts.add(p); cols += p.columns(); rows = Math.max(rows, p.rows()); }
            ArrayValue out = ArrayValue.of(rows, cols);
            int c0 = 0;
            for (ArrayValue p : parts) { for (int i = 0; i < rows; i++) for (int j = 0; j < p.columns(); j++) out.set(i, c0 + j, i < p.rows() ? p.get(i, j) : err(CellError.NA)); c0 += p.columns(); }
            return out;
        });
        raw365(r, "TOCOL", ARRAY, 1, 3, (c, a) -> ArrayValue.column(linear(c.toArray(a.value(0)), a.integer(1, 0), a.bool(2, false))));
        raw365(r, "TOROW", ARRAY, 1, 3, (c, a) -> ArrayValue.row(linear(c.toArray(a.value(0)), a.integer(1, 0), a.bool(2, false))));
        raw365(r, "WRAPROWS", ARRAY, 2, 3, (c, a) -> wrap(c, a, true));
        raw365(r, "WRAPCOLS", ARRAY, 2, 3, (c, a) -> wrap(c, a, false));
        raw365(r, "EXPAND", ARRAY, 2, 4, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            int rows = a.has(1) ? a.integer(1) : data.rows(), cols = a.has(2) ? a.integer(2) : data.columns();
            if (rows < data.rows() || cols < data.columns()) throw EvalError.value();
            CellValue pad = a.has(3) ? a.scalar(3) : err(CellError.NA);
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) out.set(i, j, i < data.rows() && j < data.columns() ? data.get(i, j) : pad);
            return out;
        });
        r.register(FunctionDefinition.raw("TRIMRANGE", ARRAY, 1, 3, (c, a) -> {
            ReferenceValue ref = a.reference(0);
            CellRange range = ref.range();
            int trimRows = a.integer(1, 3), trimCols = a.integer(2, 3);
            int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1};
            c.forEachCell(ref, (s, row, col, v) -> { bounds[0] = Math.min(bounds[0], row); bounds[1] = Math.min(bounds[1], col); bounds[2] = Math.max(bounds[2], row); bounds[3] = Math.max(bounds[3], col); });
            if (bounds[2] < 0) return err(CellError.REF);
            int r1 = (trimRows & 1) != 0 ? bounds[0] : range.firstRow(), r2 = (trimRows & 2) != 0 ? bounds[2] : range.lastRow();
            int c1 = (trimCols & 1) != 0 ? bounds[1] : range.firstColumn(), c2 = (trimCols & 2) != 0 ? bounds[3] : range.lastColumn();
            return ReferenceValue.of(ref.sheet(), new CellRange(r1, c1, r2, c2));
        }).modern().reference().build());
        raw365(r, "GROUPBY", ARRAY, 3, 8, ArrayFunctions::groupBy);
        raw365(r, "PIVOTBY", ARRAY, 4, 11, ArrayFunctions::pivotBy);
        raw365(r, "MAP", LAMBDA, 2, 254, (c, a) -> {
            LambdaValue fn = lambda(a.value(a.size() - 1));
            List<ArrayValue> arrays = new ArrayList<>();
            int rows = 1, cols = 1;
            for (int k = 0; k < a.size() - 1; k++) { ArrayValue arr = c.toArray(a.value(k)); arrays.add(arr); rows = Math.max(rows, arr.rows()); cols = Math.max(cols, arr.columns()); }
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
                List<CellValue> args = new ArrayList<>();
                for (ArrayValue arr : arrays) args.add(arr.broadcast(i, j));
                out.set(i, j, scalarResult(c, c.callLambda(fn, args)));
            }
            return out;
        });
        raw365(r, "REDUCE", LAMBDA, 3, 3, (c, a) -> {
            CellValue acc = a.has(0) ? c.scalar(a.value(0)) : CellValue.ZERO;
            LambdaValue fn = lambda(a.value(2));
            for (CellValue v : c.toArray(a.value(1)).list()) acc = c.callLambda(fn, List.of(acc, v));
            return acc;
        });
        raw365(r, "SCAN", LAMBDA, 3, 3, (c, a) -> {
            CellValue acc = a.has(0) ? c.scalar(a.value(0)) : CellValue.ZERO;
            LambdaValue fn = lambda(a.value(2));
            ArrayValue arr = c.toArray(a.value(1));
            ArrayValue out = ArrayValue.of(arr.rows(), arr.columns());
            for (int k = 0; k < arr.size(); k++) { acc = scalarResult(c, c.callLambda(fn, List.of(acc, arr.at(k)))); out.set(k / arr.columns(), k % arr.columns(), acc); }
            return out;
        });
        raw365(r, "BYROW", LAMBDA, 2, 2, (c, a) -> {
            ArrayValue arr = c.toArray(a.value(0));
            LambdaValue fn = lambda(a.value(1));
            ArrayValue out = ArrayValue.of(arr.rows(), 1);
            for (int i = 0; i < arr.rows(); i++) out.set(i, 0, scalarResult(c, c.callLambda(fn, List.of(LookupFunctions.sliceRow(arr, i)))));
            return out;
        });
        raw365(r, "BYCOL", LAMBDA, 2, 2, (c, a) -> {
            ArrayValue arr = c.toArray(a.value(0));
            LambdaValue fn = lambda(a.value(1));
            ArrayValue out = ArrayValue.of(1, arr.columns());
            for (int j = 0; j < arr.columns(); j++) out.set(0, j, scalarResult(c, c.callLambda(fn, List.of(LookupFunctions.sliceColumn(arr, j)))));
            return out;
        });
        raw365(r, "MAKEARRAY", LAMBDA, 3, 3, (c, a) -> {
            int rows = a.integer(0), cols = a.integer(1);
            if (rows < 1 || cols < 1 || (long) rows * cols > 5_000_000L) throw EvalError.value();
            LambdaValue fn = lambda(a.value(2));
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) out.set(i, j, scalarResult(c, c.callLambda(fn, List.of(num(i + 1), num(j + 1)))));
            return out;
        });
    }

    static CellValue scalarResult(FunctionContext c, CellValue v) {
        CellValue x = c.deref(v);
        if (x instanceof ArrayValue arr) { if (arr.size() == 1) return arr.get(0, 0); return err(CellError.CALC); }
        if (x instanceof LambdaValue) return err(CellError.CALC);
        return x instanceof OmittedValue ? CellValue.ZERO : x;
    }

    static int compareSort(CellValue x, CellValue y) {
        if (x.isEmpty() && y.isEmpty()) return 0;
        if (x.isEmpty()) return 1;
        if (y.isEmpty()) return -1;
        int rx = Coerce.typeRank(x), ry = Coerce.typeRank(y);
        if (rx != ry) return Integer.compare(rx, ry);
        if (x instanceof ErrorValue) return 0;
        return Coerce.compare(x, y);
    }

    static ArrayValue sortArray(ArrayValue data, boolean byCol, List<int[]> keys, Comparator<Integer> extra) {
        int n = byCol ? data.columns() : data.rows();
        Integer[] idx = new Integer[n];
        for (int k = 0; k < n; k++) idx[k] = k;
        java.util.Arrays.sort(idx, (x, y) -> {
            for (int[] key : keys) {
                CellValue vx = byCol ? data.get(key[0], x) : data.get(x, key[0]);
                CellValue vy = byCol ? data.get(key[0], y) : data.get(y, key[0]);
                int cmp = compareSort(vx, vy);
                if (vx.isEmpty() != vy.isEmpty()) return cmp;
                if (cmp != 0) return key[1] < 0 ? -cmp : cmp;
            }
            return 0;
        });
        return reorder(data, byCol, idx);
    }

    static ArrayValue reorder(ArrayValue data, boolean byCol, Integer[] idx) {
        ArrayValue out = byCol ? ArrayValue.of(data.rows(), idx.length) : ArrayValue.of(idx.length, data.columns());
        for (int k = 0; k < idx.length; k++) {
            if (byCol) for (int i = 0; i < data.rows(); i++) out.set(i, k, data.get(i, idx[k]));
            else for (int j = 0; j < data.columns(); j++) out.set(k, j, data.get(idx[k], j));
        }
        return out;
    }

    static ArrayValue slice(ArrayValue data, int r0, int r1, int c0, int c1) {
        ArrayValue out = ArrayValue.of(r1 - r0, c1 - c0);
        for (int i = r0; i < r1; i++) for (int j = c0; j < c1; j++) out.set(i - r0, j - c0, data.get(i, j));
        return out;
    }

    static String key(ArrayValue data, int k, boolean byCol) {
        StringBuilder b = new StringBuilder();
        int n = byCol ? data.rows() : data.columns();
        for (int i = 0; i < n; i++) {
            CellValue v = byCol ? data.get(i, k) : data.get(k, i);
            b.append(Coerce.typeRank(v)).append(':').append(v instanceof TextValue t ? t.value().toLowerCase(java.util.Locale.ROOT) : v instanceof NumberValue nv ? String.valueOf(NumberValue.round15(nv.value())) : v.display()).append('\u0001');
        }
        return b.toString();
    }

    private static List<CellValue> linear(ArrayValue data, int ignore, boolean byColumn) {
        List<CellValue> out = new ArrayList<>();
        int rows = data.rows(), cols = data.columns();
        for (int outer = 0; outer < (byColumn ? cols : rows); outer++)
            for (int inner = 0; inner < (byColumn ? rows : cols); inner++) {
                CellValue v = byColumn ? data.get(inner, outer) : data.get(outer, inner);
                if ((ignore == 1 || ignore == 3) && v.isEmpty()) continue;
                if ((ignore == 2 || ignore == 3) && v instanceof ErrorValue) continue;
                out.add(v);
            }
        if (out.isEmpty()) throw EvalError.calc();
        return out;
    }

    private static CellValue wrap(FunctionContext c, FunctionArgs a, boolean rows) {
        ArrayValue data = c.toArray(a.value(0));
        if (data.rows() > 1 && data.columns() > 1) throw EvalError.value();
        int count = a.integer(1);
        if (count < 1) throw EvalError.num();
        CellValue pad = a.has(2) ? a.scalar(2) : err(CellError.NA);
        int n = data.size(), other = (n + count - 1) / count;
        ArrayValue out = rows ? ArrayValue.of(other, count) : ArrayValue.of(count, other);
        for (int k = 0; k < other * count; k++) {
            CellValue v = k < n ? data.at(k) : pad;
            if (rows) out.set(k / count, k % count, v); else out.set(k % count, k / count, v);
        }
        return out;
    }

    private static LambdaValue aggregator(FunctionContext c, CellValue v) { return lambda(v); }

    private static CellValue groupBy(FunctionContext c, FunctionArgs a) {
        ArrayValue keys = c.toArray(a.value(0)), values = c.toArray(a.value(1));
        LambdaValue fn = aggregator(c, a.value(2));
        int headersMode = a.has(3) ? a.integer(3) : (Coerce.typeRank(keys.get(0, 0)) == 2 && Coerce.typeRank(values.get(0, 0)) == 2 ? 3 : 0);
        int totalDepth = a.integer(4, 1);
        int sortOrder = a.integer(5, 1);
        ArrayValue filter = a.has(6) ? c.toArray(a.value(6)) : null;
        if (keys.rows() != values.rows()) throw EvalError.value();
        boolean hasHeaders = headersMode == 1 || headersMode == 3, showHeaders = headersMode == 2 || headersMode == 3;
        int start = hasHeaders ? 1 : 0;
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        Map<String, Integer> firstRow = new LinkedHashMap<>();
        for (int row = start; row < keys.rows(); row++) {
            if (filter != null && !Coerce.bool(filter.at(row - (filter.rows() == keys.rows() ? 0 : start)))) continue;
            String k = key(keys, row, false);
            groups.computeIfAbsent(k, x -> new ArrayList<>()).add(row);
            firstRow.putIfAbsent(k, row);
        }
        List<String> order = new ArrayList<>(groups.keySet());
        int keyCols = keys.columns(), valCols = values.columns();
        Comparator<String> cmp = (x, y) -> {
            int col = Math.abs(sortOrder) - 1;
            int cmpv;
            if (col < keyCols) cmpv = compareSort(keys.get(firstRow.get(x), col), keys.get(firstRow.get(y), col));
            else cmpv = compareSort(aggregate(c, fn, values, groups.get(x), col - keyCols), aggregate(c, fn, values, groups.get(y), col - keyCols));
            return sortOrder < 0 ? -cmpv : cmpv;
        };
        if (sortOrder != 0) order.sort(cmp);
        List<List<CellValue>> rows = new ArrayList<>();
        if (showHeaders) {
            List<CellValue> h = new ArrayList<>();
            for (int j = 0; j < keyCols; j++) h.add(hasHeaders ? keys.get(0, j) : text("Campo " + (j + 1)));
            for (int j = 0; j < valCols; j++) h.add(hasHeaders ? values.get(0, j) : text("Valor " + (j + 1)));
            rows.add(h);
        }
        for (String k : order) {
            List<CellValue> line = new ArrayList<>();
            int first = firstRow.get(k);
            for (int j = 0; j < keyCols; j++) line.add(keys.get(first, j));
            for (int j = 0; j < valCols; j++) line.add(aggregate(c, fn, values, groups.get(k), j));
            rows.add(line);
        }
        if (totalDepth != 0) {
            List<Integer> all = new ArrayList<>();
            for (List<Integer> g : groups.values()) all.addAll(g);
            List<CellValue> total = new ArrayList<>();
            total.add(text("Total"));
            for (int j = 1; j < keyCols; j++) total.add(CellValue.EMPTY);
            for (int j = 0; j < valCols; j++) total.add(aggregate(c, fn, values, all, j));
            if (totalDepth > 0) rows.add(total); else rows.add(showHeaders ? 1 : 0, total);
        }
        if (rows.isEmpty()) throw EvalError.calc();
        return ArrayValue.of(rows);
    }

    private static CellValue aggregate(FunctionContext c, LambdaValue fn, ArrayValue values, List<Integer> rows, int col) {
        List<CellValue> subset = new ArrayList<>();
        for (int row : rows) subset.add(values.get(row, col));
        if (subset.isEmpty()) return CellValue.EMPTY;
        return scalarResult(c, c.callLambda(fn, List.of(ArrayValue.column(subset))));
    }

    private static CellValue pivotBy(FunctionContext c, FunctionArgs a) {
        ArrayValue rowKeys = c.toArray(a.value(0)), colKeys = c.toArray(a.value(1)), values = c.toArray(a.value(2));
        LambdaValue fn = aggregator(c, a.value(3));
        int headersMode = a.integer(4, 0);
        boolean hasHeaders = headersMode == 1 || headersMode == 3;
        int start = hasHeaders ? 1 : 0;
        int rowTotals = a.integer(5, 1), colTotals = a.integer(7, 1);
        int rowSort = a.integer(6, 1), colSort = a.integer(8, 1);
        ArrayValue filter = a.has(9) ? c.toArray(a.value(9)) : null;
        if (rowKeys.rows() != values.rows() || colKeys.rows() != values.rows()) throw EvalError.value();
        Map<String, Integer> rowFirst = new LinkedHashMap<>(), colFirst = new LinkedHashMap<>();
        Map<String, List<Integer>> cells = new LinkedHashMap<>();
        for (int row = start; row < values.rows(); row++) {
            if (filter != null && !Coerce.bool(filter.at(row))) continue;
            String rk = key(rowKeys, row, false), ck = key(colKeys, row, false);
            rowFirst.putIfAbsent(rk, row);
            colFirst.putIfAbsent(ck, row);
            cells.computeIfAbsent(rk + "\u0002" + ck, x -> new ArrayList<>()).add(row);
        }
        List<String> rowOrder = new ArrayList<>(rowFirst.keySet()), colOrder = new ArrayList<>(colFirst.keySet());
        if (rowSort != 0) rowOrder.sort((x, y) -> { int v = compareSort(rowKeys.get(rowFirst.get(x), 0), rowKeys.get(rowFirst.get(y), 0)); return rowSort < 0 ? -v : v; });
        if (colSort != 0) colOrder.sort((x, y) -> { int v = compareSort(colKeys.get(colFirst.get(x), 0), colKeys.get(colFirst.get(y), 0)); return colSort < 0 ? -v : v; });
        int keyCols = rowKeys.columns();
        List<List<CellValue>> out = new ArrayList<>();
        List<CellValue> header = new ArrayList<>();
        for (int j = 0; j < keyCols; j++) header.add(CellValue.EMPTY);
        for (String ck : colOrder) header.add(colKeys.get(colFirst.get(ck), 0));
        if (colTotals != 0) header.add(text("Total"));
        out.add(header);
        for (String rk : rowOrder) {
            List<CellValue> line = new ArrayList<>();
            for (int j = 0; j < keyCols; j++) line.add(rowKeys.get(rowFirst.get(rk), j));
            List<Integer> rowAll = new ArrayList<>();
            for (String ck : colOrder) {
                List<Integer> rows = cells.getOrDefault(rk + "\u0002" + ck, List.of());
                rowAll.addAll(rows);
                line.add(rows.isEmpty() ? CellValue.EMPTY : aggregate(c, fn, values, rows, 0));
            }
            if (colTotals != 0) line.add(aggregate(c, fn, values, rowAll, 0));
            out.add(line);
        }
        if (rowTotals != 0) {
            List<CellValue> total = new ArrayList<>();
            total.add(text("Total"));
            for (int j = 1; j < keyCols; j++) total.add(CellValue.EMPTY);
            List<Integer> all = new ArrayList<>();
            for (String ck : colOrder) {
                List<Integer> colRows = new ArrayList<>();
                for (String rk : rowOrder) colRows.addAll(cells.getOrDefault(rk + "\u0002" + ck, List.of()));
                all.addAll(colRows);
                total.add(aggregate(c, fn, values, colRows, 0));
            }
            if (colTotals != 0) total.add(aggregate(c, fn, values, all, 0));
            out.add(total);
        }
        return ArrayValue.of(out);
    }

    static boolean same(Object a, Object b) { return Objects.equals(a, b); }
}
