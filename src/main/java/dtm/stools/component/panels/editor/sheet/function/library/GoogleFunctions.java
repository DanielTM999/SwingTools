package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.calc.SparklineValue;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.GOOGLE;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class GoogleFunctions {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.!#$%&'*+/=?^`{|}~-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern URL = Pattern.compile("^(https?://|ftp://)?([\\w-]+\\.)+[a-z]{2,}(:\\d+)?(/\\S*)?$", Pattern.CASE_INSENSITIVE);

    private GoogleFunctions() {}

    static void register(FunctionRegistry r) {
        google(r, "QUERY", GOOGLE, 2, 3, (c, a) -> {
            CellValue source = a.value(0);
            ArrayValue data = c.toArray(source);
            boolean fromRange = source instanceof ReferenceValue;
            int firstColumn = fromRange ? ((ReferenceValue) source).range().firstColumn() : 0;
            int headers = a.has(2) ? a.integer(2) : -1;
            return QueryLanguage.run(data, a.text(1), headers, firstColumn, fromRange, c.date1904(), new NumberFormatter(c.locale(), c.date1904()));
        });
        google(r, "ARRAYFORMULA", GOOGLE, 1, 1, (c, a) -> c.deref(a.value(0)));
        google(r, "SPLIT", GOOGLE, 2, 4, (c, a) -> {
            String text = a.text(0), delimiter = a.text(1);
            boolean each = a.bool(2, true), removeEmpty = a.bool(3, true);
            List<String> delims = new ArrayList<>();
            if (each) for (char ch : delimiter.toCharArray()) delims.add(String.valueOf(ch)); else delims.add(delimiter);
            List<String> parts = TextFunctions.split(text, delims, false, removeEmpty);
            if (parts.isEmpty()) throw EvalError.value();
            List<CellValue> out = new ArrayList<>();
            for (String p : parts) { Double d = dtm.stools.component.panels.editor.sheet.format.ValueParser.parseNumberLenient(p); out.add(d != null && !p.isBlank() ? num(d) : text(p)); }
            return ArrayValue.row(out);
        });
        google(r, "JOIN", GOOGLE, 2, 255, (c, a) -> {
            String d = a.text(0);
            List<String> parts = new ArrayList<>();
            for (CellValue v : flattenArgs(c, a, 1)) { if (v instanceof ErrorValue e) throw EvalError.of(e.error()); parts.add(Coerce.text(v)); }
            return text(String.join(d, parts));
        });
        google(r, "FLATTEN", GOOGLE, 1, 255, (c, a) -> {
            List<CellValue> out = new ArrayList<>();
            for (int k = 0; k < a.size(); k++) out.addAll(c.toArray(a.value(k)).list());
            return ArrayValue.column(out);
        });
        google(r, "SORTN", GOOGLE, 1, 255, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            int n = a.integer(1, 1), tieMode = a.integer(2, 0);
            List<int[]> keys = new ArrayList<>();
            for (int k = 3; k + 1 < a.size() + 1; k += 2) {
                if (!a.has(k)) break;
                keys.add(new int[]{a.integer(k) - 1, a.bool(k + 1, true) ? 1 : -1});
            }
            if (keys.isEmpty()) keys.add(new int[]{0, 1});
            ArrayValue sorted = ArrayFunctions.sortArray(data, false, keys, null);
            List<List<CellValue>> rows = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int row = 0; row < sorted.rows(); row++) {
                String key = ArrayFunctions.key(sorted, row, false);
                if (tieMode == 2 && !seen.add(key)) continue;
                if (rows.size() >= n && tieMode != 1) break;
                if (rows.size() >= n && tieMode == 1) {
                    String prev = ArrayFunctions.key(sorted, row - 1, false);
                    if (!prev.equals(key)) break;
                }
                List<CellValue> line = new ArrayList<>();
                for (int col = 0; col < sorted.columns(); col++) line.add(sorted.get(row, col));
                rows.add(line);
            }
            if (rows.isEmpty()) throw EvalError.na();
            return ArrayValue.of(rows);
        });
        google(r, "COUNTUNIQUE", GOOGLE, 1, 255, (c, a) -> {
            Set<String> set = new HashSet<>();
            for (CellValue v : flattenArgs(c, a, 0)) if (!v.isEmpty()) set.add(Coerce.typeRank(v) + ":" + v.display().toLowerCase(Locale.ROOT));
            return num(set.size());
        });
        google(r, "COUNTUNIQUEIFS", GOOGLE, 3, 255, (c, a) -> {
            ArrayValue values = c.toArray(a.value(0));
            boolean[] mask = MathFunctions.ifsMask(c, a, 1, values.rows(), values.columns());
            Set<String> set = new HashSet<>();
            for (int k = 0; k < values.size(); k++) if (mask[k] && !values.at(k).isEmpty()) set.add(values.at(k).display().toLowerCase(Locale.ROOT));
            return num(set.size());
        });
        googleScalar(r, "REGEXMATCH", GOOGLE, 2, 2, (c, a) -> bool(Pattern.compile(t(a, 1)).matcher(t(a, 0)).find()));
        googleScalar(r, "ISEMAIL", GOOGLE, 1, 1, (c, a) -> bool(arg(a, 0) instanceof TextValue tv && EMAIL.matcher(tv.value().strip()).matches()));
        googleScalar(r, "ISURL", GOOGLE, 1, 1, (c, a) -> bool(arg(a, 0) instanceof TextValue tv && URL.matcher(tv.value().strip()).matches()));
        googleScalar(r, "ISDATE", GOOGLE, 1, 1, (c, a) -> bool(arg(a, 0) instanceof NumberValue));
        googleScalar(r, "ISBETWEEN", GOOGLE, 3, 5, (c, a) -> {
            double v = n(a, 0), lo = n(a, 1), hi = n(a, 2);
            boolean li = b(a, 3, true), hiIncl = b(a, 4, true);
            return bool((li ? v >= lo : v > lo) && (hiIncl ? v <= hi : v < hi));
        });
        googleScalar(r, "TO_DATE", GOOGLE, 1, 1, (c, a) -> arg(a, 0) instanceof NumberValue n ? n : arg(a, 0));
        googleScalar(r, "TO_DOLLARS", GOOGLE, 1, 1, (c, a) -> arg(a, 0) instanceof NumberValue n ? n : arg(a, 0));
        googleScalar(r, "TO_PERCENT", GOOGLE, 1, 1, (c, a) -> arg(a, 0) instanceof NumberValue n ? n : arg(a, 0));
        googleScalar(r, "TO_PURE_NUMBER", GOOGLE, 1, 1, (c, a) -> num(n(a, 0)));
        googleScalar(r, "TO_TEXT", GOOGLE, 1, 1, (c, a) -> text(arg(a, 0) instanceof NumberValue n ? NumberValue.general(n.value()) : t(a, 0)));
        googleScalar(r, "UNARY_PERCENT", GOOGLE, 1, 1, (c, a) -> num(n(a, 0) / 100));
        googleScalar(r, "ADD", GOOGLE, 2, 2, (c, a) -> num(n(a, 0) + n(a, 1)));
        googleScalar(r, "MINUS", GOOGLE, 2, 2, (c, a) -> num(n(a, 0) - n(a, 1)));
        googleScalar(r, "MULTIPLY", GOOGLE, 2, 2, (c, a) -> num(n(a, 0) * n(a, 1)));
        googleScalar(r, "DIVIDE", GOOGLE, 2, 2, (c, a) -> { double d = n(a, 1); if (d == 0) throw EvalError.div0(); return num(n(a, 0) / d); });
        googleScalar(r, "POW", GOOGLE, 2, 2, (c, a) -> num(Math.pow(n(a, 0), n(a, 1))));
        googleScalar(r, "UMINUS", GOOGLE, 1, 1, (c, a) -> num(-n(a, 0)));
        googleScalar(r, "UPLUS", GOOGLE, 1, 1, (c, a) -> arg(a, 0));
        googleScalar(r, "EQ", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) == 0));
        googleScalar(r, "NE", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) != 0));
        googleScalar(r, "GT", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) > 0));
        googleScalar(r, "GTE", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) >= 0));
        googleScalar(r, "LT", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) < 0));
        googleScalar(r, "LTE", GOOGLE, 2, 2, (c, a) -> bool(Coerce.compare(arg(a, 0), arg(a, 1)) <= 0));
        googleScalar(r, "EPOCHTODATE", GOOGLE, 1, 2, (c, a) -> {
            double v = n(a, 0);
            int unit = i(a, 1, 1);
            long millis = switch (unit) { case 1 -> (long) (v * 1000); case 2 -> (long) v; case 3 -> (long) (v / 1000); default -> throw EvalError.num(); };
            return num(DateSerial.toSerial(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC), c.date1904()));
        });
        google(r, "MARGINOFERROR", GOOGLE, 2, 2, (c, a) -> {
            DoubleList l = numbersOf(c, a.value(0), false);
            double confidence = a.number(1);
            if (l.size() < 2 || confidence <= 0 || confidence >= 1) throw EvalError.num();
            double sd = Math.sqrt(variance(l, true));
            return num(Math.abs(StatisticalFunctions.tInverse((1 - confidence) / 2, l.size() - 1)) * sd / Math.sqrt(l.size()));
        });
        google(r, "AVERAGE.WEIGHTED", GOOGLE, 2, 254, (c, a) -> {
            double sum = 0, weights = 0;
            for (int k = 0; k + 1 < a.size(); k += 2) {
                ArrayValue v = c.toArray(a.value(k)), w = c.toArray(a.value(k + 1));
                if (v.size() != w.size()) throw EvalError.value();
                for (int j = 0; j < v.size(); j++) { double x = Coerce.number(v.at(j)), y = Coerce.number(w.at(j)); sum += x * y; weights += y; }
            }
            if (weights == 0) throw EvalError.div0();
            return num(sum / weights);
        });
        google(r, "ARRAY_CONSTRAIN", GOOGLE, 3, 3, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            int rows = Math.min(data.rows(), a.integer(1)), cols = Math.min(data.columns(), a.integer(2));
            if (rows < 1 || cols < 1) throw EvalError.num();
            return ArrayFunctions.slice(data, 0, rows, 0, cols);
        });
        google(r, "SPARKLINE", GOOGLE, 1, 2, (c, a) -> {
            ArrayValue data = c.toArray(a.value(0));
            List<Double> values = new ArrayList<>();
            for (CellValue v : data.list()) if (v instanceof NumberValue n) values.add(n.value());
            Map<String, String> options = new LinkedHashMap<>();
            if (a.has(1)) {
                ArrayValue opts = c.toArray(a.value(1));
                for (int row = 0; row < opts.rows(); row++) if (opts.columns() >= 2) options.put(opts.get(row, 0).display().toLowerCase(Locale.ROOT), opts.get(row, 1).display());
            }
            return new SparklineValue(values, options);
        });
        for (String name : new String[]{"IMPORTRANGE", "IMPORTDATA", "IMPORTXML", "IMPORTHTML", "IMPORTFEED", "GOOGLEFINANCE", "GOOGLETRANSLATE", "DETECTLANGUAGE", "GOOGLECLOCK"}) {
            r.register(FunctionDefinition.raw(name, GOOGLE, 1, 10, (c, a) -> googleExternal(c, a, name)).google().volatileFunction().build());
        }
    }

    private static CellValue googleExternal(FunctionContext c, FunctionArgs a, String name) {
        if (name.equals("DETECTLANGUAGE") && c.external() == null) return text(detectLanguage(a.text(0)));
        if (name.equals("GOOGLETRANSLATE") && c.external() == null) return a.has(0) ? text(a.text(0)) : err(CellError.NA);
        return WebCubeFunctions.fetch(c, a, name);
    }

    private static String detectLanguage(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.matches(".*[ãõçáéíóúâêô].*") || t.matches(".*\\b(que|não|você|para|com|uma)\\b.*")) return "pt";
        if (t.matches(".*[ñ¿¡].*") || t.matches(".*\\b(el|los|las|por|una|está)\\b.*")) return "es";
        if (t.matches(".*[äöüß].*")) return "de";
        if (t.matches(".*[àèùëïœ].*")) return "fr";
        return "en";
    }

    static boolean isBool(CellValue v) { return v instanceof BoolValue; }
}
