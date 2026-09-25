package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.MATH;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class MathFunctions {
    private MathFunctions() {}

    static void register(FunctionRegistry r) {
        math1(r, "ABS", MATH, Math::abs);
        math1(r, "ACOS", MATH, x -> { if (x < -1 || x > 1) throw EvalError.num(); return Math.acos(x); });
        math1(r, "ACOSH", MATH, x -> { if (x < 1) throw EvalError.num(); return Math.log(x + Math.sqrt(x * x - 1)); });
        math1(r, "ACOT", MATH, x -> Math.PI / 2 - Math.atan(x));
        math1(r, "ACOTH", MATH, x -> { if (Math.abs(x) <= 1) throw EvalError.num(); return 0.5 * Math.log((x + 1) / (x - 1)); });
        math1(r, "ASIN", MATH, x -> { if (x < -1 || x > 1) throw EvalError.num(); return Math.asin(x); });
        math1(r, "ASINH", MATH, x -> Math.log(x + Math.sqrt(x * x + 1)));
        math1(r, "ATAN", MATH, Math::atan);
        math2(r, "ATAN2", MATH, (x, y) -> { if (x == 0 && y == 0) throw EvalError.div0(); return Math.atan2(y, x); });
        math1(r, "ATANH", MATH, x -> { if (Math.abs(x) >= 1) throw EvalError.num(); return 0.5 * Math.log((1 + x) / (1 - x)); });
        math1(r, "COS", MATH, x -> { if (Math.abs(x) >= 134217728) throw EvalError.num(); return Math.cos(x); });
        math1(r, "COSH", MATH, Math::cosh);
        math1(r, "COT", MATH, x -> { if (x == 0) throw EvalError.div0(); return 1 / Math.tan(x); });
        math1(r, "COTH", MATH, x -> { if (x == 0) throw EvalError.div0(); return 1 / Math.tanh(x); });
        math1(r, "CSC", MATH, x -> { if (x == 0) throw EvalError.div0(); return 1 / Math.sin(x); });
        math1(r, "CSCH", MATH, x -> { if (x == 0) throw EvalError.div0(); return 1 / Math.sinh(x); });
        math1(r, "SEC", MATH, x -> 1 / Math.cos(x));
        math1(r, "SECH", MATH, x -> 1 / Math.cosh(x));
        math1(r, "SIN", MATH, x -> { if (Math.abs(x) >= 134217728) throw EvalError.num(); return Math.sin(x); });
        math1(r, "SINH", MATH, Math::sinh);
        math1(r, "TAN", MATH, x -> { if (Math.abs(x) >= 134217728) throw EvalError.num(); return Math.tan(x); });
        math1(r, "TANH", MATH, Math::tanh);
        math1(r, "DEGREES", MATH, Math::toDegrees);
        math1(r, "RADIANS", MATH, Math::toRadians);
        math1(r, "EXP", MATH, Math::exp);
        math1(r, "LN", MATH, x -> { if (x <= 0) throw EvalError.num(); return Math.log(x); });
        math1(r, "LOG10", MATH, x -> { if (x <= 0) throw EvalError.num(); return Math.log10(x); });
        scalar(r, "LOG", MATH, 1, 2, (c, a) -> {
            double x = n(a, 0), base = n(a, 1, 10);
            if (x <= 0 || base <= 0) throw EvalError.num();
            if (base == 1) throw EvalError.div0();
            return num(Math.log(x) / Math.log(base));
        });
        math1(r, "SQRT", MATH, x -> { if (x < 0) throw EvalError.num(); return Math.sqrt(x); });
        math1(r, "SQRTPI", MATH, x -> { if (x < 0) throw EvalError.num(); return Math.sqrt(x * Math.PI); });
        math1(r, "INT", MATH, Math::floor);
        math1(r, "SIGN", MATH, Math::signum);
        math1(r, "FACT", MATH, SpecialFunctions::factorial);
        math1(r, "FACTDOUBLE", MATH, x -> { if (x < -1) throw EvalError.num(); double v = 1; for (long i = (long) Math.floor(x); i > 1; i -= 2) v *= i; return v; });
        math1(r, "EVEN", MATH, x -> { double v = Math.ceil(Math.abs(x)); if (v % 2 != 0) v++; return Math.copySign(v, x); });
        math1(r, "ODD", MATH, x -> { double v = Math.ceil(Math.abs(x)); if (v % 2 == 0) v++; return Math.copySign(v, x); });
        scalar(r, "PI", MATH, 0, 0, (c, a) -> num(Math.PI));
        math2(r, "POWER", MATH, (x, y) -> {
            if (x == 0 && y == 0) throw EvalError.num();
            if (x == 0 && y < 0) throw EvalError.div0();
            double p = Math.pow(x, y);
            if (Double.isNaN(p)) throw EvalError.num();
            return p;
        });
        math2(r, "MOD", MATH, (x, y) -> { if (y == 0) throw EvalError.div0(); double m = x - y * Math.floor(x / y); return Math.abs(m) < 1e-15 * Math.abs(y) ? 0 : m; });
        math2(r, "QUOTIENT", MATH, (x, y) -> { if (y == 0) throw EvalError.div0(); double q = x / y; return q < 0 ? Math.ceil(q) : Math.floor(q); });
        math2(r, "COMBIN", MATH, (x, y) -> SpecialFunctions.combin(Math.floor(x), Math.floor(y)));
        math2(r, "COMBINA", MATH, (x, y) -> { x = Math.floor(x); y = Math.floor(y); if (x < 0 || y < 0) throw EvalError.num(); return x == 0 && y == 0 ? 1 : SpecialFunctions.combin(x + y - 1, y); });
        scalar(r, "ROUND", MATH, 2, 2, (c, a) -> num(round(n(a, 0), (int) n(a, 1), RoundingMode.HALF_UP)));
        scalar(r, "ROUNDUP", MATH, 2, 2, (c, a) -> num(round(n(a, 0), (int) n(a, 1), RoundingMode.UP)));
        scalar(r, "ROUNDDOWN", MATH, 2, 2, (c, a) -> num(round(n(a, 0), (int) n(a, 1), RoundingMode.DOWN)));
        scalar(r, "TRUNC", MATH, 1, 2, (c, a) -> num(round(n(a, 0), (int) n(a, 1, 0), RoundingMode.DOWN)));
        scalar(r, "MROUND", MATH, 2, 2, (c, a) -> {
            double x = n(a, 0), m = n(a, 1);
            if (m == 0) return num(0);
            if (x * m < 0) throw EvalError.num();
            return num(round(Math.round(x / m) * m, 12, RoundingMode.HALF_UP));
        });
        scalar(r, "CEILING", MATH, 1, 2, (c, a) -> {
            double x = n(a, 0), s = n(a, 1, 1);
            if (s == 0) return num(0);
            if (x > 0 && s < 0) throw EvalError.num();
            return num(clean(Math.ceil(x / s - 1e-12) * s));
        });
        scalar(r, "CEILING.MATH", MATH, 1, 3, (c, a) -> {
            double x = n(a, 0), s = Math.abs(n(a, 1, 1));
            boolean away = n(a, 2, 0) != 0;
            if (s == 0) return num(0);
            double q = x / s;
            return num(clean((x < 0 && away ? Math.floor(q + 1e-12) : Math.ceil(q - 1e-12)) * s));
        });
        scalar(r, "CEILING.PRECISE", MATH, 1, 2, (c, a) -> { double s = Math.abs(n(a, 1, 1)); return num(s == 0 ? 0 : clean(Math.ceil(n(a, 0) / s - 1e-12) * s)); });
        scalar(r, "ISO.CEILING", MATH, 1, 2, (c, a) -> { double s = Math.abs(n(a, 1, 1)); return num(s == 0 ? 0 : clean(Math.ceil(n(a, 0) / s - 1e-12) * s)); });
        scalar(r, "FLOOR", MATH, 1, 2, (c, a) -> {
            double x = n(a, 0), s = n(a, 1, 1);
            if (s == 0) { if (x == 0) return num(0); throw EvalError.div0(); }
            if (x > 0 && s < 0) throw EvalError.num();
            return num(clean(Math.floor(x / s + 1e-12) * s));
        });
        scalar(r, "FLOOR.MATH", MATH, 1, 3, (c, a) -> {
            double x = n(a, 0), s = Math.abs(n(a, 1, 1));
            boolean toward = n(a, 2, 0) != 0;
            if (s == 0) return num(0);
            double q = x / s;
            return num(clean((x < 0 && toward ? Math.ceil(q - 1e-12) : Math.floor(q + 1e-12)) * s));
        });
        scalar(r, "FLOOR.PRECISE", MATH, 1, 2, (c, a) -> { double s = Math.abs(n(a, 1, 1)); return num(s == 0 ? 0 : clean(Math.floor(n(a, 0) / s + 1e-12) * s)); });
        raw(r, "GCD", MATH, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            long g = 0;
            for (int k = 0; k < l.size(); k++) { double v = l.get(k); if (v < 0) throw EvalError.num(); g = gcd(g, (long) Math.floor(v)); }
            return num(g);
        });
        raw(r, "LCM", MATH, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            long m = 1;
            for (int k = 0; k < l.size(); k++) { double v = Math.floor(l.get(k)); if (v < 0) throw EvalError.num(); if (v == 0) return num(0); m = m / gcd(m, (long) v) * (long) v; }
            return num(m);
        });
        raw(r, "MULTINOMIAL", MATH, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            double sum = 0, denominator = 0;
            for (int k = 0; k < l.size(); k++) { double v = Math.floor(l.get(k)); if (v < 0) throw EvalError.num(); sum += v; denominator += SpecialFunctions.logGamma(v + 1); }
            return num(Math.rint(Math.exp(SpecialFunctions.logGamma(sum + 1) - denominator)));
        });
        raw(r, "SUM", MATH, 1, 255, (c, a) -> num(numbers(c, a, 0).sum()));
        raw(r, "PRODUCT", MATH, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); double p = l.size() == 0 ? 0 : 1; for (int k = 0; k < l.size(); k++) p *= l.get(k); return num(p); });
        raw(r, "SUMSQ", MATH, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); double s = 0; for (int k = 0; k < l.size(); k++) s += l.get(k) * l.get(k); return num(s); });
        raw(r, "SUMIF", MATH, 2, 3, (c, a) -> {
            ReferenceValue range = a.isReference(0) ? a.reference(0) : null;
            ArrayValue values = c.toArray(a.value(0));
            ArrayValue sums = a.has(2) ? sumRange(c, a.value(2), values) : values;
            Predicate<CellValue> p = criteria(a.scalar(1));
            double s = 0;
            for (int row = 0; row < values.rows(); row++)
                for (int col = 0; col < values.columns(); col++) {
                    if (!p.test(values.get(row, col))) continue;
                    CellValue v = row < sums.rows() && col < sums.columns() ? sums.get(row, col) : CellValue.EMPTY;
                    if (v instanceof ErrorValue e) throw EvalError.of(e.error());
                    if (v instanceof NumberValue n) s += n.value();
                }
            return num(s);
        });
        raw(r, "SUMIFS", MATH, 3, 255, (c, a) -> {
            ArrayValue sums = c.toArray(a.value(0));
            boolean[] mask = ifsMask(c, a, 1, sums.rows(), sums.columns());
            double s = 0;
            for (int k = 0; k < sums.size(); k++) if (mask[k]) { CellValue v = sums.at(k); if (v instanceof ErrorValue e) throw EvalError.of(e.error()); if (v instanceof NumberValue n) s += n.value(); }
            return num(s);
        });
        raw(r, "SUMPRODUCT", MATH, 1, 255, (c, a) -> {
            ArrayValue first = c.toArray(a.value(0));
            double[] product = new double[first.size()];
            java.util.Arrays.fill(product, 1);
            for (int i = 0; i < a.size(); i++) {
                ArrayValue x = c.toArray(a.value(i));
                if (x.rows() != first.rows() || x.columns() != first.columns()) throw EvalError.value();
                for (int k = 0; k < x.size(); k++) {
                    CellValue v = x.at(k);
                    if (v instanceof ErrorValue e) throw EvalError.of(e.error());
                    product[k] *= v instanceof NumberValue nv ? nv.value() : 0;
                }
            }
            double s = 0;
            for (double d : product) s += d;
            return num(s);
        });
        raw(r, "SUMX2MY2", MATH, 2, 2, (c, a) -> pairSum(c, a, (x, y) -> x * x - y * y));
        raw(r, "SUMX2PY2", MATH, 2, 2, (c, a) -> pairSum(c, a, (x, y) -> x * x + y * y));
        raw(r, "SUMXMY2", MATH, 2, 2, (c, a) -> pairSum(c, a, (x, y) -> (x - y) * (x - y)));
        raw(r, "SERIESSUM", MATH, 4, 4, (c, a) -> {
            double x = a.number(0), n0 = a.number(1), m = a.number(2);
            DoubleList coeffs = numbersOf(c, a.value(3), false);
            double s = 0;
            for (int k = 0; k < coeffs.size(); k++) s += coeffs.get(k) * Math.pow(x, n0 + k * m);
            return num(s);
        });
        r.register(FunctionDefinition.scalar("RAND", MATH, 0, 0, (c, a) -> num(c.random())).volatileFunction().build());
        r.register(FunctionDefinition.scalar("RANDBETWEEN", MATH, 2, 2, (c, a) -> {
            double lo = Math.ceil(n(a, 0)), hi = Math.floor(n(a, 1));
            if (lo > hi) throw EvalError.num();
            return num(lo + Math.floor(c.random() * (hi - lo + 1)));
        }).volatileFunction().build());
        r.register(FunctionDefinition.raw("RANDARRAY", MATH, 0, 5, (c, a) -> {
            int rows = a.integer(0, 1), cols = a.integer(1, 1);
            double min = a.number(2, 0), max = a.number(3, 1);
            boolean whole = a.bool(4, false);
            if (rows < 1 || cols < 1 || min > max) throw EvalError.value();
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
                double v = whole ? Math.ceil(min) + Math.floor(c.random() * (Math.floor(max) - Math.ceil(min) + 1)) : min + c.random() * (max - min);
                out.set(i, j, num(v));
            }
            return out;
        }).volatileFunction().modern().build());
        raw365(r, "SEQUENCE", MATH, 1, 4, (c, a) -> {
            int rows = a.integer(0), cols = a.integer(1, 1);
            double start = a.number(2, 1), step = a.number(3, 1);
            if (rows < 1 || cols < 1) throw EvalError.calc();
            if ((long) rows * cols > 10_000_000L) throw EvalError.num();
            ArrayValue out = ArrayValue.of(rows, cols);
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) out.set(i, j, num(start + (i * cols + j) * step));
            return out;
        });
        raw(r, "MMULT", MATH, 2, 2, (c, a) -> {
            double[][] x = matrix(c, a.value(0)), y = matrix(c, a.value(1));
            if (x[0].length != y.length) throw EvalError.value();
            ArrayValue out = ArrayValue.of(x.length, y[0].length);
            for (int i = 0; i < x.length; i++) for (int j = 0; j < y[0].length; j++) { double s = 0; for (int k = 0; k < y.length; k++) s += x[i][k] * y[k][j]; out.set(i, j, num(s)); }
            return out;
        });
        raw(r, "MDETERM", MATH, 1, 1, (c, a) -> {
            double[][] m = matrix(c, a.value(0));
            if (m.length != m[0].length) throw EvalError.value();
            return num(determinant(m));
        });
        raw(r, "MINVERSE", MATH, 1, 1, (c, a) -> {
            double[][] m = matrix(c, a.value(0));
            if (m.length != m[0].length) throw EvalError.value();
            double[][] inv = inverse(m);
            ArrayValue out = ArrayValue.of(m.length, m.length);
            for (int i = 0; i < m.length; i++) for (int j = 0; j < m.length; j++) out.set(i, j, num(clean(inv[i][j])));
            return out;
        });
        raw365(r, "MUNIT", MATH, 1, 1, (c, a) -> {
            int size = a.integer(0);
            if (size < 1) throw EvalError.value();
            ArrayValue out = ArrayValue.of(size, size);
            for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) out.set(i, j, num(i == j ? 1 : 0));
            return out;
        });
        scalar(r, "ROMAN", MATH, 1, 2, (c, a) -> {
            double v = Math.floor(n(a, 0));
            if (v < 0 || v > 3999) throw EvalError.value();
            return text(roman((int) v, given(a, 1) ? (arg(a, 1).isBoolean() ? (b(a, 1) ? 0 : 4) : (int) n(a, 1)) : 0));
        });
        scalar(r, "ARABIC", MATH, 1, 1, (c, a) -> num(arabic(t(a, 0))));
        scalar(r, "BASE", MATH, 2, 3, (c, a) -> {
            double v = Math.floor(n(a, 0));
            int radix = i(a, 1), len = i(a, 2, 0);
            if (v < 0 || v >= 9.007199254740992E15 || radix < 2 || radix > 36 || len < 0 || len > 255) throw EvalError.num();
            String s = Long.toString((long) v, radix).toUpperCase(java.util.Locale.ROOT);
            while (s.length() < len) s = "0" + s;
            return text(s);
        });
        scalar(r, "DECIMAL", MATH, 2, 2, (c, a) -> {
            int radix = i(a, 1);
            if (radix < 2 || radix > 36) throw EvalError.num();
            try { return num(Long.parseLong(t(a, 0).strip(), radix)); } catch (NumberFormatException e) { throw EvalError.num(); }
        });
        raw(r, "SUBTOTAL", MATH, 2, 255, (c, a) -> {
            int code = a.integer(0);
            boolean ignoreHidden = code > 100;
            int fn = code % 100;
            if (fn < 1 || fn > 11) throw EvalError.value();
            DoubleList l = new DoubleList();
            int[] counts = {0};
            for (int i = 1; i < a.size(); i++) {
                CellValue v = a.value(i);
                if (v instanceof ReferenceValue ref) {
                    c.forEachCell(ref, (s, row, col, value) -> {
                        if (c.isSubtotalCell(s, row, col)) return;
                        if (ignoreHidden && c.isHiddenRow(s, row)) return;
                        if (value instanceof ErrorValue e) throw EvalError.of(e.error());
                        if (fn == 3) { counts[0]++; return; }
                        if (value instanceof NumberValue nv) l.add(nv.value());
                    });
                } else {
                    if (fn == 3) { if (!v.isEmpty()) counts[0]++; continue; }
                    collect(c, v, l, false, true);
                }
            }
            if (fn == 3) return num(counts[0]);
            return aggregate(fn, l);
        });
        raw(r, "AGGREGATE", MATH, 3, 255, (c, a) -> {
            int fn = a.integer(0), options = a.integer(1);
            if (fn < 1 || fn > 19 || options < 0 || options > 7) throw EvalError.value();
            boolean ignoreHidden = options == 1 || options == 3 || options == 5 || options == 7;
            boolean ignoreErrors = options == 2 || options == 3 || options == 6 || options == 7;
            boolean ignoreNested = options <= 3;
            DoubleList l = new DoubleList();
            int end = fn >= 14 ? 2 : a.size() - 1;
            int[] counts = {0};
            for (int i = 2; i <= Math.min(end, a.size() - 1); i++) {
                CellValue v = a.value(i);
                if (v instanceof ReferenceValue ref) {
                    c.forEachCell(ref, (s, row, col, value) -> {
                        if (ignoreNested && c.isSubtotalCell(s, row, col)) return;
                        if (ignoreHidden && c.isHiddenRow(s, row)) return;
                        if (value instanceof ErrorValue e) { if (ignoreErrors) return; throw EvalError.of(e.error()); }
                        counts[0]++;
                        if (value instanceof NumberValue nv) l.add(nv.value());
                    });
                } else {
                    ArrayValue arr = c.toArray(v);
                    for (int k = 0; k < arr.size(); k++) {
                        CellValue x = arr.at(k);
                        if (x instanceof ErrorValue e) { if (ignoreErrors) continue; throw EvalError.of(e.error()); }
                        if (!x.isEmpty()) counts[0]++;
                        if (x instanceof NumberValue nv) l.add(nv.value());
                    }
                }
            }
            if (fn >= 14) {
                double k = a.number(3);
                double[] sorted = l.sorted();
                if (sorted.length == 0) throw EvalError.num();
                return switch (fn) {
                    case 14 -> { int idx = (int) Math.ceil(k); if (idx < 1 || idx > sorted.length) throw EvalError.num(); yield num(sorted[sorted.length - idx]); }
                    case 15 -> { int idx = (int) Math.ceil(k); if (idx < 1 || idx > sorted.length) throw EvalError.num(); yield num(sorted[idx - 1]); }
                    case 16 -> num(percentile(sorted, k, false));
                    case 17 -> num(percentile(sorted, k / 4, false));
                    case 18 -> num(percentile(sorted, k, true));
                    default -> num(percentile(sorted, k / 4, true));
                };
            }
            return switch (fn) {
                case 3 -> num(counts[0]);
                case 12 -> num(StatisticalFunctions.median(l.sorted()));
                case 13 -> StatisticalFunctions.mode(l.toArray());
                default -> aggregate(fn, l);
            };
        });
    }

    static CellValue aggregate(int fn, DoubleList l) {
        return switch (fn) {
            case 1 -> num(l.mean());
            case 2 -> num(l.size());
            case 4 -> num(l.size() == 0 ? 0 : max(l));
            case 5 -> num(l.size() == 0 ? 0 : min(l));
            case 6 -> { double p = l.size() == 0 ? 0 : 1; for (int k = 0; k < l.size(); k++) p *= l.get(k); yield num(p); }
            case 7 -> num(Math.sqrt(variance(l, true)));
            case 8 -> num(Math.sqrt(variance(l, false)));
            case 9 -> num(l.sum());
            case 10 -> num(variance(l, true));
            case 11 -> num(variance(l, false));
            default -> throw EvalError.value();
        };
    }

    static double max(DoubleList l) { double m = Double.NEGATIVE_INFINITY; for (int k = 0; k < l.size(); k++) m = Math.max(m, l.get(k)); return m; }
    static double min(DoubleList l) { double m = Double.POSITIVE_INFINITY; for (int k = 0; k < l.size(); k++) m = Math.min(m, l.get(k)); return m; }

    static ArrayValue sumRange(FunctionContext c, CellValue sumArg, ArrayValue shape) {
        if (sumArg instanceof ReferenceValue ref && ref.isSingleArea()) {
            var r = ref.range();
            return c.toArray(ReferenceValue.of(ref.sheet(), new dtm.stools.component.panels.editor.sheet.model.CellRange(r.firstRow(), r.firstColumn(),
                    Math.min(dtm.stools.component.panels.editor.sheet.model.CellAddress.MAX_ROWS - 1, r.firstRow() + shape.rows() - 1),
                    Math.min(dtm.stools.component.panels.editor.sheet.model.CellAddress.MAX_COLUMNS - 1, r.firstColumn() + shape.columns() - 1))));
        }
        return c.toArray(sumArg);
    }

    static boolean[] ifsMask(FunctionContext c, FunctionArgs a, int from, int rows, int cols) {
        boolean[] mask = new boolean[rows * cols];
        java.util.Arrays.fill(mask, true);
        if ((a.size() - from) % 2 != 0) throw EvalError.value();
        for (int i = from; i + 1 < a.size(); i += 2) {
            ArrayValue range = c.toArray(a.value(i));
            if (range.rows() != rows || range.columns() != cols) {
                if (a.value(i) instanceof ReferenceValue ref && ref.isSingleArea() && ref.range().rowCount() == rows && ref.range().columnCount() == cols) {
                    ArrayValue full = ArrayValue.of(rows, cols);
                    for (int r = 0; r < Math.min(rows, range.rows()); r++) for (int col = 0; col < Math.min(cols, range.columns()); col++) full.set(r, col, range.get(r, col));
                    range = full;
                } else throw EvalError.value();
            }
            Predicate<CellValue> p = criteria(a.scalar(i + 1));
            for (int k = 0; k < mask.length; k++) if (mask[k] && !p.test(range.at(k))) mask[k] = false;
        }
        return mask;
    }

    private static CellValue pairSum(FunctionContext c, FunctionArgs a, BinaryDouble f) {
        ArrayValue x = c.toArray(a.value(0)), y = c.toArray(a.value(1));
        if (x.size() != y.size()) throw EvalError.na();
        double s = 0;
        for (int k = 0; k < x.size(); k++) {
            CellValue u = x.at(k), v = y.at(k);
            if (u instanceof ErrorValue e) throw EvalError.of(e.error());
            if (v instanceof ErrorValue e) throw EvalError.of(e.error());
            if (u instanceof NumberValue nu && v instanceof NumberValue nv) s += f.f(nu.value(), nv.value());
        }
        return num(s);
    }

    static double[][] matrix(FunctionContext c, CellValue v) {
        ArrayValue a = c.toArray(v);
        double[][] m = new double[a.rows()][a.columns()];
        for (int i = 0; i < a.rows(); i++) for (int j = 0; j < a.columns(); j++) {
            CellValue x = a.get(i, j);
            if (x instanceof ErrorValue e) throw EvalError.of(e.error());
            if (!(x instanceof NumberValue n)) throw EvalError.value();
            m[i][j] = n.value();
        }
        return m;
    }

    static double determinant(double[][] input) {
        int n = input.length;
        double[][] m = new double[n][];
        for (int i = 0; i < n; i++) m[i] = input[i].clone();
        double det = 1;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            if (Math.abs(m[pivot][col]) < 1e-300) return 0;
            if (pivot != col) { double[] t = m[pivot]; m[pivot] = m[col]; m[col] = t; det = -det; }
            det *= m[col][col];
            for (int r = col + 1; r < n; r++) {
                double f = m[r][col] / m[col][col];
                for (int k = col; k < n; k++) m[r][k] -= f * m[col][k];
            }
        }
        return det;
    }

    static double[][] inverse(double[][] input) {
        int n = input.length;
        double[][] m = new double[n][2 * n];
        for (int i = 0; i < n; i++) { System.arraycopy(input[i], 0, m[i], 0, n); m[i][n + i] = 1; }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            if (Math.abs(m[pivot][col]) < 1e-14) throw EvalError.num();
            double[] t = m[pivot]; m[pivot] = m[col]; m[col] = t;
            double p = m[col][col];
            for (int k = 0; k < 2 * n; k++) m[col][k] /= p;
            for (int r = 0; r < n; r++) {
                if (r == col) continue;
                double f = m[r][col];
                for (int k = 0; k < 2 * n; k++) m[r][k] -= f * m[col][k];
            }
        }
        double[][] out = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(m[i], n, out[i], 0, n);
        return out;
    }

    static double round(double value, int digits, RoundingMode mode) {
        if (!Double.isFinite(value)) throw EvalError.num();
        if (digits > 15) digits = 15;
        BigDecimal bd = new BigDecimal(Double.toString(NumberValue.round15(value)));
        return bd.setScale(digits, mode).doubleValue();
    }

    static double clean(double v) { return Math.abs(v) < 1e-14 ? 0 : NumberValue.round15(v); }

    static long gcd(long a, long b) { while (b != 0) { long t = a % b; a = b; b = t; } return Math.abs(a); }

    private static String roman(int n, int form) {
        if (n == 0) return "";
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) while (n >= values[i]) { b.append(symbols[i]); n -= values[i]; }
        String s = b.toString();
        if (form >= 1) s = s.replace("XLV", "VL").replace("XCV", "VC").replace("CDL", "LD").replace("CML", "LM");
        if (form >= 2) s = s.replace("XLIX", "IL").replace("XCIX", "IC").replace("CDXC", "XD").replace("CMXC", "XM");
        if (form >= 3) s = s.replace("CDXCV", "VD").replace("CMXCV", "VM");
        if (form >= 4) s = s.replace("CDXCIX", "ID").replace("CMXCIX", "IM").replace("XDIX", "ID").replace("XMIX", "IM");
        return s;
    }

    private static double arabic(String text) {
        String s = text.strip().toUpperCase(java.util.Locale.ROOT);
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);
        if (s.length() > 255) throw EvalError.value();
        int total = 0, prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (s.charAt(i)) { case 'I' -> 1; case 'V' -> 5; case 'X' -> 10; case 'L' -> 50; case 'C' -> 100; case 'D' -> 500; case 'M' -> 1000; default -> throw EvalError.value(); };
            if (v < prev) total -= v; else { total += v; prev = v; }
        }
        return negative ? -total : total;
    }

    static boolean isOmitted(CellValue v) { return v instanceof OmittedValue; }
    static double coerce(CellValue v) { return Coerce.number(v); }
    static List<CellValue> none() { return List.of(); }
    static CellValue error(CellError e) { return CellValue.error(e); }
}
