package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.LambdaValue;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.RawBody;
import dtm.stools.component.panels.editor.sheet.function.ScalarBody;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class Lib {
    private Lib() {}

    static final class DoubleList {
        double[] data = new double[16];
        int size;
        void add(double v) { if (size == data.length) data = Arrays.copyOf(data, size * 2); data[size++] = v; }
        double[] toArray() { return Arrays.copyOf(data, size); }
        double[] sorted() { double[] d = toArray(); Arrays.sort(d); return d; }
        int size() { return size; }
        double get(int i) { return data[i]; }
        double sum() { double s = 0, c = 0; for (int i = 0; i < size; i++) { double y = data[i] - c, t = s + y; c = (t - s) - y; s = t; } return s; }
        double mean() { if (size == 0) throw EvalError.div0(); return sum() / size; }
    }

    static CellValue arg(CellValue[] a, int i) { return FunctionDefinition.arg(a, i); }
    static boolean given(CellValue[] a, int i) { return FunctionDefinition.given(a, i); }
    static double n(CellValue[] a, int i) { return Coerce.number(arg(a, i)); }
    static double n(CellValue[] a, int i, double d) { return given(a, i) ? n(a, i) : d; }
    static int i(CellValue[] a, int i) { double d = n(a, i); if (Math.abs(d) >= Integer.MAX_VALUE) throw EvalError.num(); return (int) d; }
    static int i(CellValue[] a, int idx, int d) { return given(a, idx) ? i(a, idx) : d; }
    static String t(CellValue[] a, int i) { return Coerce.text(arg(a, i)); }
    static String t(CellValue[] a, int i, String d) { return given(a, i) ? t(a, i) : d; }
    static boolean b(CellValue[] a, int i) { return Coerce.bool(arg(a, i)); }
    static boolean b(CellValue[] a, int i, boolean d) { return given(a, i) ? b(a, i) : d; }
    static CellValue num(double v) { return Coerce.result(v); }
    static CellValue text(String s) { return new TextValue(s); }
    static CellValue bool(boolean v) { return CellValue.of(v); }
    static CellValue err(CellError e) { return CellValue.error(e); }
    static EvalError fail(CellError e) { return EvalError.of(e); }

    static void scalar(FunctionRegistry r, String name, FunctionCategory c, int min, int max, ScalarBody body) {
        r.register(FunctionDefinition.scalar(name, c, min, max, body).build());
    }

    static void scalar365(FunctionRegistry r, String name, FunctionCategory c, int min, int max, ScalarBody body) {
        r.register(FunctionDefinition.scalar(name, c, min, max, body).modern().build());
    }

    static void raw(FunctionRegistry r, String name, FunctionCategory c, int min, int max, RawBody body) {
        r.register(FunctionDefinition.raw(name, c, min, max, body).build());
    }

    static void raw365(FunctionRegistry r, String name, FunctionCategory c, int min, int max, RawBody body) {
        r.register(FunctionDefinition.raw(name, c, min, max, body).modern().build());
    }

    static void google(FunctionRegistry r, String name, FunctionCategory c, int min, int max, RawBody body) {
        r.register(FunctionDefinition.raw(name, c, min, max, body).google().build());
    }

    static void googleScalar(FunctionRegistry r, String name, FunctionCategory c, int min, int max, ScalarBody body) {
        r.register(FunctionDefinition.scalar(name, c, min, max, body).google().build());
    }

    static void math1(FunctionRegistry r, String name, FunctionCategory c, UnaryDouble f) { scalar(r, name, c, 1, 1, (ctx, a) -> num(f.f(n(a, 0)))); }
    static void math2(FunctionRegistry r, String name, FunctionCategory c, BinaryDouble f) { scalar(r, name, c, 2, 2, (ctx, a) -> num(f.f(n(a, 0), n(a, 1)))); }

    static CellValue scalarOf(FunctionContext ctx, CellValue v) { return ctx.scalar(v); }

    static DoubleList numbers(FunctionContext ctx, FunctionArgs args, int from) { return numbers(ctx, args, from, args.size(), false); }

    static DoubleList numbers(FunctionContext ctx, FunctionArgs args, int from, int to, boolean aMode) {
        DoubleList out = new DoubleList();
        for (int i = from; i < to; i++) collect(ctx, args.value(i), out, aMode, true);
        return out;
    }

    static DoubleList numbersOf(FunctionContext ctx, CellValue v, boolean aMode) {
        DoubleList out = new DoubleList();
        collect(ctx, v, out, aMode, true);
        return out;
    }

    static void collect(FunctionContext ctx, CellValue v, DoubleList out, boolean aMode, boolean direct) {
        switch (v) {
            case ReferenceValue r -> ctx.forEachCell(r, (s, row, col, value) -> {
                switch (value) {
                    case NumberValue n -> out.add(n.value());
                    case ErrorValue e -> throw EvalError.of(e.error());
                    case BoolValue b -> { if (aMode) out.add(b.value() ? 1 : 0); }
                    case TextValue t -> { if (aMode) out.add(0); }
                    default -> { }
                }
            });
            case ArrayValue a -> {
                for (int k = 0; k < a.size(); k++) {
                    CellValue x = a.at(k);
                    switch (x) {
                        case NumberValue n -> out.add(n.value());
                        case ErrorValue e -> throw EvalError.of(e.error());
                        case BoolValue b -> { if (aMode) out.add(b.value() ? 1 : 0); }
                        case TextValue t -> { if (aMode) out.add(0); }
                        default -> { }
                    }
                }
            }
            case NumberValue n -> out.add(n.value());
            case BoolValue b -> out.add(b.value() ? 1 : 0);
            case TextValue t -> out.add(Coerce.parseNumber(t.value()));
            case ErrorValue e -> throw EvalError.of(e.error());
            case OmittedValue o -> out.add(0);
            case EmptyValue e -> { if (direct) out.add(0); }
            default -> throw EvalError.value();
        }
    }

    static List<CellValue> flatten(FunctionContext ctx, CellValue v) {
        return ctx.toArray(v).list();
    }

    static List<CellValue> flattenArgs(FunctionContext ctx, FunctionArgs args, int from) {
        List<CellValue> out = new ArrayList<>();
        for (int i = from; i < args.size(); i++) {
            CellValue v = args.value(i);
            if (v instanceof OmittedValue) continue;
            out.addAll(ctx.toArray(v).list());
        }
        return out;
    }

    static ArrayValue array(FunctionContext ctx, CellValue v) { return ctx.toArray(v); }

    static LambdaValue lambda(CellValue v) {
        if (v instanceof LambdaValue l) return l;
        if (v instanceof ErrorValue e) throw EvalError.of(e.error());
        throw EvalError.value();
    }

    static Predicate<CellValue> criteria(CellValue criterion) {
        switch (criterion) {
            case NumberValue n -> { double d = n.value(); return v -> numeric(v) != null && numeric(v) == d; }
            case BoolValue b -> { return v -> v instanceof BoolValue x && x.value() == b.value(); }
            case ErrorValue e -> { return v -> v instanceof ErrorValue x && x.error() == e.error(); }
            case EmptyValue e -> { return v -> v.isEmpty() || v instanceof TextValue t && t.value().isEmpty(); }
            case OmittedValue o -> { return v -> v.isEmpty(); }
            default -> { }
        }
        String c = Coerce.text(criterion);
        String op = "=";
        for (String candidate : new String[]{">=", "<=", "<>", "=", ">", "<"}) if (c.startsWith(candidate)) { op = candidate; c = c.substring(candidate.length()); break; }
        String operand = c;
        if (operand.isEmpty()) {
            if (op.equals("=")) return v -> v.isEmpty() || v instanceof TextValue t && t.value().isEmpty();
            if (op.equals("<>")) return v -> !(v.isEmpty() || v instanceof TextValue t && t.value().isEmpty());
        }
        Double number = operand.isBlank() ? null : ValueParser.parseNumberLenient(operand);
        String upper = operand.toUpperCase(Locale.ROOT);
        Boolean boolOperand = upper.equals("TRUE") || upper.equals("VERDADEIRO") ? Boolean.TRUE : upper.equals("FALSE") || upper.equals("FALSO") ? Boolean.FALSE : null;
        CellError errorOperand = CellError.parse(operand).orElse(null);
        String finalOp = op;
        if (errorOperand != null) return v -> (v instanceof ErrorValue e && e.error() == errorOperand) == finalOp.equals("=");
        if (boolOperand != null) {
            boolean bo = boolOperand;
            return v -> {
                boolean eq = v instanceof BoolValue b && b.value() == bo;
                return switch (finalOp) { case "=" -> eq; case "<>" -> !eq; default -> v instanceof BoolValue b && compareOp(Boolean.compare(b.value(), bo), finalOp); };
            };
        }
        if (number != null) {
            double d = number;
            return v -> {
                Double x = v instanceof NumberValue n ? Double.valueOf(n.value()) : null;
                if (x == null && v instanceof TextValue t && (finalOp.equals("=") || finalOp.equals("<>"))) x = ValueParser.parseNumberLenient(t.value());
                if (x == null) return finalOp.equals("<>");
                return compareOp(Double.compare(x, d), finalOp);
            };
        }
        Pattern pattern = wildcard(operand);
        return v -> {
            if (finalOp.equals("=") || finalOp.equals("<>")) {
                boolean match = !(v instanceof ErrorValue) && !(v instanceof NumberValue) && !(v instanceof BoolValue) && pattern.matcher(v instanceof TextValue t ? t.value() : "").matches();
                return finalOp.equals("=") == match;
            }
            if (!(v instanceof TextValue t)) return false;
            return compareOp(Coerce.compareText(t.value(), operand), finalOp);
        };
    }

    private static Double numeric(CellValue v) {
        if (v instanceof NumberValue n) return n.value();
        if (v instanceof TextValue t) return ValueParser.parseNumberLenient(t.value());
        return null;
    }

    static boolean compareOp(int c, String op) {
        return switch (op) {
            case ">" -> c > 0;
            case ">=" -> c >= 0;
            case "<" -> c < 0;
            case "<=" -> c <= 0;
            case "<>" -> c != 0;
            default -> c == 0;
        };
    }

    static Pattern wildcard(String pattern) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '~' && i + 1 < pattern.length() && "*?~".indexOf(pattern.charAt(i + 1)) >= 0) { b.append(Pattern.quote(String.valueOf(pattern.charAt(++i)))); continue; }
            if (c == '*') b.append(".*");
            else if (c == '?') b.append('.');
            else b.append(Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(b.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);
    }

    static boolean hasWildcard(String s) { return s.indexOf('*') >= 0 || s.indexOf('?') >= 0 || s.indexOf('~') >= 0; }

    static ArrayValue column(List<CellValue> values) {
        if (values.isEmpty()) throw EvalError.calc();
        return ArrayValue.column(values);
    }

    static CellValue first(CellValue v) { return v instanceof ArrayValue a ? a.get(0, 0) : v; }

    static int[] sortedIndexes(double[] values) {
        Integer[] idx = new Integer[values.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (x, y) -> Double.compare(values[x], values[y]));
        int[] out = new int[idx.length];
        for (int i = 0; i < idx.length; i++) out[i] = idx[i];
        return out;
    }

    static double percentile(double[] sorted, double p, boolean exclusive) {
        int n = sorted.length;
        if (n == 0) throw EvalError.num();
        if (exclusive) {
            double rank = p * (n + 1);
            if (rank < 1 || rank > n) throw EvalError.num();
            int k = (int) Math.floor(rank);
            double f = rank - k;
            return k >= n ? sorted[n - 1] : sorted[k - 1] + f * (sorted[k] - sorted[k - 1]);
        }
        if (p < 0 || p > 1) throw EvalError.num();
        double rank = p * (n - 1);
        int k = (int) Math.floor(rank);
        double f = rank - k;
        return k + 1 >= n ? sorted[n - 1] : sorted[k] + f * (sorted[k + 1] - sorted[k]);
    }

    static double variance(DoubleList l, boolean sample) {
        int n = l.size();
        if (n < (sample ? 2 : 1)) throw EvalError.div0();
        double mean = l.mean(), s = 0;
        for (int i = 0; i < n; i++) { double d = l.get(i) - mean; s += d * d; }
        return s / (sample ? n - 1 : n);
    }

    static boolean isText(CellValue v) { return v instanceof TextValue; }
}
