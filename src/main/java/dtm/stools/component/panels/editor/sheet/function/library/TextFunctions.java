package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.format.ParsedInput;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.TEXT;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class TextFunctions {
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private TextFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "LEN", TEXT, 1, 1, (c, a) -> num(t(a, 0).length()));
        scalar(r, "LENB", TEXT, 1, 1, (c, a) -> num(t(a, 0).length()));
        scalar(r, "LEFT", TEXT, 1, 2, (c, a) -> { String s = t(a, 0); int n = i(a, 1, 1); if (n < 0) throw EvalError.value(); return text(s.substring(0, Math.min(n, s.length()))); });
        scalar(r, "LEFTB", TEXT, 1, 2, (c, a) -> { String s = t(a, 0); int n = i(a, 1, 1); if (n < 0) throw EvalError.value(); return text(s.substring(0, Math.min(n, s.length()))); });
        scalar(r, "RIGHT", TEXT, 1, 2, (c, a) -> { String s = t(a, 0); int n = i(a, 1, 1); if (n < 0) throw EvalError.value(); return text(s.substring(Math.max(0, s.length() - n))); });
        scalar(r, "RIGHTB", TEXT, 1, 2, (c, a) -> { String s = t(a, 0); int n = i(a, 1, 1); if (n < 0) throw EvalError.value(); return text(s.substring(Math.max(0, s.length() - n))); });
        scalar(r, "MID", TEXT, 3, 3, (c, a) -> mid(t(a, 0), i(a, 1), i(a, 2)));
        scalar(r, "MIDB", TEXT, 3, 3, (c, a) -> mid(t(a, 0), i(a, 1), i(a, 2)));
        scalar(r, "LOWER", TEXT, 1, 1, (c, a) -> text(t(a, 0).toLowerCase(c.locale())));
        scalar(r, "UPPER", TEXT, 1, 1, (c, a) -> text(t(a, 0).toUpperCase(c.locale())));
        scalar(r, "PROPER", TEXT, 1, 1, (c, a) -> text(proper(t(a, 0), c.locale())));
        scalar(r, "TRIM", TEXT, 1, 1, (c, a) -> text(t(a, 0).replaceAll(" +", " ").replaceAll("^ | $", "")));
        scalar(r, "CLEAN", TEXT, 1, 1, (c, a) -> text(t(a, 0).replaceAll("[\\x00-\\x1F]", "")));
        scalar(r, "EXACT", TEXT, 2, 2, (c, a) -> bool(t(a, 0).equals(t(a, 1))));
        scalar(r, "REPT", TEXT, 2, 2, (c, a) -> {
            String s = t(a, 0);
            int n = i(a, 1);
            if (n < 0 || (long) n * s.length() > 32767) throw EvalError.value();
            return text(s.repeat(n));
        });
        scalar(r, "CHAR", TEXT, 1, 1, (c, a) -> {
            int n = i(a, 0);
            if (n < 1 || n > 255) throw EvalError.value();
            return text(CP1252.decode(ByteBuffer.wrap(new byte[]{(byte) n})).toString());
        });
        scalar(r, "CODE", TEXT, 1, 1, (c, a) -> {
            String s = t(a, 0);
            if (s.isEmpty()) throw EvalError.value();
            ByteBuffer b = CP1252.encode(CharBuffer.wrap(s.substring(0, 1)));
            return num(b.hasRemaining() ? b.get() & 0xFF : 63);
        });
        scalar(r, "UNICHAR", TEXT, 1, 1, (c, a) -> { int n = i(a, 0); if (n < 1 || n > 0x10FFFF || n >= 0xD800 && n <= 0xDFFF) throw EvalError.value(); return text(new String(Character.toChars(n))); });
        scalar(r, "UNICODE", TEXT, 1, 1, (c, a) -> { String s = t(a, 0); if (s.isEmpty()) throw EvalError.value(); return num(s.codePointAt(0)); });
        scalar(r, "ASC", TEXT, 1, 1, (c, a) -> text(toHalfWidth(t(a, 0))));
        scalar(r, "DBCS", TEXT, 1, 1, (c, a) -> text(toFullWidth(t(a, 0))));
        scalar(r, "JIS", TEXT, 1, 1, (c, a) -> text(toFullWidth(t(a, 0))));
        scalar(r, "PHONETIC", TEXT, 1, 1, (c, a) -> text(t(a, 0)));
        scalar(r, "BAHTTEXT", TEXT, 1, 1, (c, a) -> text(c.formatNumber(n(a, 0), "#,##0.00") + " บาท"));
        raw(r, "CONCATENATE", TEXT, 1, 255, (c, a) -> {
            boolean arrays = false;
            for (int k = 0; k < a.size(); k++) if (c.deref(a.value(k)) instanceof ArrayValue) arrays = true;
            if (arrays) return liftConcat(c, a);
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < a.size(); k++) b.append(Coerce.text(a.scalar(k)));
            return text(b.toString());
        });
        raw365(r, "CONCAT", TEXT, 1, 254, (c, a) -> {
            StringBuilder b = new StringBuilder();
            for (CellValue v : flattenArgs(c, a, 0)) { if (v instanceof ErrorValue e) throw EvalError.of(e.error()); b.append(Coerce.text(v)); }
            if (b.length() > 32767) throw EvalError.value();
            return text(b.toString());
        });
        raw365(r, "TEXTJOIN", TEXT, 3, 252, (c, a) -> {
            List<CellValue> delims = flatten(c, a.value(0));
            boolean ignore = a.bool(1);
            List<String> parts = new ArrayList<>();
            for (CellValue v : flattenArgs(c, a, 2)) {
                if (v instanceof ErrorValue e) throw EvalError.of(e.error());
                String s = Coerce.text(v);
                if (ignore && s.isEmpty()) continue;
                parts.add(s);
            }
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < parts.size(); k++) {
                if (k > 0) b.append(Coerce.text(delims.get((k - 1) % delims.size())));
                b.append(parts.get(k));
            }
            if (b.length() > 32767) throw EvalError.value();
            return text(b.toString());
        });
        scalar(r, "FIND", TEXT, 2, 3, (c, a) -> find(t(a, 0), t(a, 1), i(a, 2, 1), true));
        scalar(r, "FINDB", TEXT, 2, 3, (c, a) -> find(t(a, 0), t(a, 1), i(a, 2, 1), true));
        scalar(r, "SEARCH", TEXT, 2, 3, (c, a) -> find(t(a, 0), t(a, 1), i(a, 2, 1), false));
        scalar(r, "SEARCHB", TEXT, 2, 3, (c, a) -> find(t(a, 0), t(a, 1), i(a, 2, 1), false));
        scalar(r, "SUBSTITUTE", TEXT, 3, 4, (c, a) -> {
            String s = t(a, 0), old = t(a, 1), rep = t(a, 2);
            if (old.isEmpty()) return text(s);
            if (!given(a, 3)) return text(s.replace(old, rep));
            int instance = i(a, 3);
            if (instance < 1) throw EvalError.value();
            int idx = -1;
            for (int k = 0; k < instance; k++) { idx = s.indexOf(old, idx + 1); if (idx < 0) return text(s); }
            return text(s.substring(0, idx) + rep + s.substring(idx + old.length()));
        });
        scalar(r, "REPLACE", TEXT, 4, 4, (c, a) -> replace(t(a, 0), i(a, 1), i(a, 2), t(a, 3)));
        scalar(r, "REPLACEB", TEXT, 4, 4, (c, a) -> replace(t(a, 0), i(a, 1), i(a, 2), t(a, 3)));
        scalar(r, "TEXT", TEXT, 2, 2, (c, a) -> {
            CellValue v = arg(a, 0);
            String code = localizedFormat(t(a, 1), c);
            if (v instanceof TextValue tv) {
                Double d = ValueParser.parseNumberLenient(tv.value());
                if (d == null) return text(formatText(c, tv.value(), code));
                v = num(d);
            }
            if (v instanceof BoolValue bv) return text(bv.value() ? "TRUE" : "FALSE");
            return text(c.formatNumber(Coerce.number(v), code));
        });
        scalar(r, "FIXED", TEXT, 1, 3, (c, a) -> {
            int decimals = i(a, 1, 2);
            boolean noCommas = b(a, 2, false);
            double v = MathFunctions.round(n(a, 0), decimals, java.math.RoundingMode.HALF_UP);
            String code = (noCommas ? "0" : "#,##0") + (decimals > 0 ? "." + "0".repeat(Math.min(decimals, 127)) : "");
            return text(c.formatNumber(v, code));
        });
        scalar(r, "DOLLAR", TEXT, 1, 2, (c, a) -> {
            int decimals = i(a, 1, 2);
            double v = MathFunctions.round(n(a, 0), decimals, java.math.RoundingMode.HALF_UP);
            String sym = c.locale().getLanguage().equals("pt") ? "\"R$\" " : "\"$\"";
            String num = "#,##0" + (decimals > 0 ? "." + "0".repeat(Math.min(decimals, 127)) : "");
            return text(c.formatNumber(v, sym + num + ";-" + sym + num));
        });
        scalar(r, "VALUE", TEXT, 1, 1, (c, a) -> {
            CellValue v = arg(a, 0);
            if (v instanceof NumberValue) return v;
            if (v.isEmpty()) return num(0);
            if (v instanceof BoolValue) throw EvalError.value();
            ParsedInput p = new ValueParser(c.locale(), c.date1904()).parse(Coerce.text(v).strip());
            if (p.value() instanceof NumberValue n) return n;
            throw EvalError.value();
        });
        scalar(r, "NUMBERVALUE", TEXT, 1, 3, (c, a) -> {
            String s = t(a, 0).replaceAll("\\s", "");
            String dec = t(a, 1, String.valueOf(new java.text.DecimalFormatSymbols(c.locale()).getDecimalSeparator()));
            String grp = t(a, 2, String.valueOf(new java.text.DecimalFormatSymbols(c.locale()).getGroupingSeparator()));
            if (dec.isEmpty() || grp.isEmpty()) throw EvalError.value();
            char d = dec.charAt(0), g = grp.charAt(0);
            if (d == g) throw EvalError.value();
            if (s.isEmpty()) return num(0);
            int percents = 0;
            while (s.endsWith("%")) { percents++; s = s.substring(0, s.length() - 1); }
            int decIndex = s.indexOf(d);
            if (decIndex >= 0 && s.indexOf(g, decIndex) >= 0) throw EvalError.value();
            s = s.replace(String.valueOf(g), "").replace(d, '.');
            try { return num(Double.parseDouble(s) / Math.pow(100, percents)); } catch (NumberFormatException e) { throw EvalError.value(); }
        });
        raw365(r, "TEXTBEFORE", TEXT, 2, 6, (c, a) -> textAround(c, a, true));
        raw365(r, "TEXTAFTER", TEXT, 2, 6, (c, a) -> textAround(c, a, false));
        raw365(r, "TEXTSPLIT", TEXT, 2, 6, TextFunctions::textSplit);
        raw365(r, "VALUETOTEXT", TEXT, 1, 2, (c, a) -> {
            CellValue v = c.deref(a.value(0));
            boolean strict = a.integer(1, 0) == 1;
            if (v instanceof ArrayValue arr) return arr.map(x -> text(valueToText(x, strict)));
            return text(valueToText(v, strict));
        });
        raw365(r, "ARRAYTOTEXT", TEXT, 1, 2, (c, a) -> {
            ArrayValue arr = c.toArray(a.value(0));
            boolean strict = a.integer(1, 0) == 1;
            StringBuilder b = new StringBuilder(strict ? "{" : "");
            char col = strict ? c.formulaLocale().arrayColumnSeparator() : ',';
            for (int row = 0; row < arr.rows(); row++) {
                if (row > 0) b.append(strict ? String.valueOf(c.formulaLocale().arrayRowSeparator()) : ", ");
                for (int k = 0; k < arr.columns(); k++) {
                    if (k > 0) b.append(strict ? String.valueOf(col) : ", ");
                    b.append(valueToText(arr.get(row, k), strict));
                }
            }
            if (strict) b.append('}');
            return text(b.toString());
        });
        raw365(r, "REGEXTEST", TEXT, 2, 3, (c, a) -> liftText(c, a, 0, s -> bool(regex(c, a, 1, 2).matcher(s).find())));
        raw365(r, "REGEXEXTRACT", TEXT, 2, 4, (c, a) -> {
            Pattern p = regex(c, a, 1, 3);
            int mode = a.integer(2, 0);
            CellValue source = c.deref(a.value(0));
            if (source instanceof ArrayValue arr && mode == 0) return arr.map(x -> { try { return extractFirst(p, Coerce.text(x)); } catch (EvalError e) { return e.toValue(); } });
            String s = Coerce.text(c.scalar(source));
            Matcher m = p.matcher(s);
            return switch (mode) {
                case 0 -> extractFirst(p, s);
                case 1 -> { List<CellValue> all = new ArrayList<>(); while (m.find()) all.add(text(m.group())); if (all.isEmpty()) throw EvalError.na(); yield ArrayValue.column(all); }
                case 2 -> {
                    if (!m.find()) throw EvalError.na();
                    if (m.groupCount() == 0) yield text(m.group());
                    List<CellValue> groups = new ArrayList<>();
                    for (int g = 1; g <= m.groupCount(); g++) groups.add(text(m.group(g) == null ? "" : m.group(g)));
                    yield ArrayValue.row(groups);
                }
                default -> throw EvalError.value();
            };
        });
        raw365(r, "REGEXREPLACE", TEXT, 3, 5, (c, a) -> {
            Pattern p = regex(c, a, 1, 4);
            String replacement = a.text(2);
            int occurrence = a.integer(3, 0);
            return liftText(c, a, 0, s -> {
                Matcher m = p.matcher(s);
                if (occurrence == 0) return text(m.replaceAll(replacement));
                List<int[]> matches = new ArrayList<>();
                while (m.find()) matches.add(new int[]{m.start(), m.end()});
                int idx = occurrence > 0 ? occurrence - 1 : matches.size() + occurrence;
                if (idx < 0 || idx >= matches.size()) return text(s);
                Matcher single = p.matcher(s);
                StringBuilder out = new StringBuilder();
                int k = 0;
                while (single.find()) {
                    if (k++ == idx) { single.appendReplacement(out, replacement); break; }
                }
                single.appendTail(out);
                return text(out.toString());
            });
        });
    }

    private static CellValue liftText(FunctionContext c, FunctionArgs a, int index, TextOperation op) {
        CellValue v = c.deref(a.value(index));
        if (v instanceof ArrayValue arr) return arr.map(x -> { try { if (x instanceof ErrorValue) return x; return op.apply(Coerce.text(x)); } catch (EvalError e) { return e.toValue(); } });
        if (v instanceof ErrorValue) return v;
        return op.apply(Coerce.text(v));
    }

    private static CellValue extractFirst(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (!m.find()) throw EvalError.na();
        return text(m.group());
    }

    private static Pattern regex(FunctionContext c, FunctionArgs a, int patternIndex, int caseIndex) {
        int flags = a.integer(caseIndex, 0) == 1 ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        try { return Pattern.compile(a.text(patternIndex), flags); } catch (PatternSyntaxException e) { throw EvalError.value(); }
    }

    private static CellValue mid(String s, int start, int count) {
        if (start < 1 || count < 0) throw EvalError.value();
        if (start > s.length()) return text("");
        return text(s.substring(start - 1, Math.min(s.length(), start - 1 + count)));
    }

    private static CellValue replace(String s, int start, int count, String with) {
        if (start < 1 || count < 0) throw EvalError.value();
        int from = Math.min(start - 1, s.length()), to = Math.min(s.length(), from + count);
        return text(s.substring(0, from) + with + s.substring(to));
    }

    private static CellValue find(String needle, String haystack, int start, boolean caseSensitive) {
        if (start < 1 || start > haystack.length() + 1) throw EvalError.value();
        if (needle.isEmpty()) return num(start);
        if (caseSensitive) {
            int idx = haystack.indexOf(needle, start - 1);
            if (idx < 0) throw EvalError.value();
            return num(idx + 1);
        }
        Matcher m = Lib.wildcard(needle).matcher(haystack);
        if (!Lib.hasWildcard(needle)) {
            int idx = haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), start - 1);
            if (idx < 0) throw EvalError.value();
            return num(idx + 1);
        }
        for (int k = start - 1; k < haystack.length(); k++) {
            m.region(k, haystack.length());
            if (m.lookingAt()) return num(k + 1);
        }
        throw EvalError.value();
    }

    private static String proper(String s, Locale locale) {
        StringBuilder b = new StringBuilder(s.length());
        boolean upper = true;
        for (int k = 0; k < s.length(); k++) {
            char ch = s.charAt(k);
            if (Character.isLetter(ch)) { b.append(upper ? Character.toUpperCase(ch) : Character.toLowerCase(ch)); upper = false; }
            else { b.append(ch); upper = true; }
        }
        return b.toString();
    }

    private static String toHalfWidth(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= 0xFF01 && ch <= 0xFF5E ? (char) (ch - 0xFEE0) : ch == 0x3000 ? ' ' : ch);
        return b.toString();
    }

    private static String toFullWidth(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(ch >= 0x21 && ch <= 0x7E ? (char) (ch + 0xFEE0) : ch == ' ' ? (char) 0x3000 : ch);
        return b.toString();
    }

    private static CellValue liftConcat(FunctionContext c, FunctionArgs a) {
        int rows = 1, cols = 1;
        List<CellValue> values = new ArrayList<>();
        for (int k = 0; k < a.size(); k++) {
            CellValue v = c.deref(a.value(k));
            if (v instanceof ArrayValue arr) { rows = Math.max(rows, arr.rows()); cols = Math.max(cols, arr.columns()); }
            values.add(v);
        }
        ArrayValue out = ArrayValue.of(rows, cols);
        for (int r = 0; r < rows; r++) for (int col = 0; col < cols; col++) {
            StringBuilder b = new StringBuilder();
            CellValue result = null;
            for (CellValue v : values) {
                CellValue x = v instanceof ArrayValue arr ? arr.broadcast(r, col) : v;
                if (x instanceof ErrorValue) { result = x; break; }
                b.append(Coerce.text(x));
            }
            out.set(r, col, result != null ? result : text(b.toString()));
        }
        return out;
    }

    static String localizedFormat(String code, FunctionContext c) {
        if (c.formulaLocale().decimalSeparator() != ',') return code;
        StringBuilder b = new StringBuilder();
        boolean quoted = false, bracket = false;
        boolean dateLike = code.toLowerCase(Locale.ROOT).matches(".*[dmyahs].*") && !code.matches(".*[0#].*");
        for (int k = 0; k < code.length(); k++) {
            char ch = code.charAt(k);
            if (ch == '"') quoted = !quoted;
            else if (ch == '[' && !quoted) bracket = true;
            else if (ch == ']' && !quoted) bracket = false;
            if (!quoted && !bracket) {
                if (ch == ',') ch = '.';
                else if (ch == '.') ch = ',';
                else if ((ch == 'a' || ch == 'A') && c.locale().getLanguage().equals("pt") && dateLike) ch = 'y';
            }
            b.append(ch);
        }
        return b.toString();
    }

    private static String formatText(FunctionContext c, String value, String code) {
        return new dtm.stools.component.panels.editor.sheet.format.NumberFormatter(c.locale(), c.date1904()).formatText(value, code).text();
    }

    private static String valueToText(CellValue v, boolean strict) {
        return switch (v) {
            case TextValue t -> strict ? "\"" + t.value().replace("\"", "\"\"") + "\"" : t.value();
            case ErrorValue e -> e.error().text();
            case BoolValue b -> b.value() ? "TRUE" : "FALSE";
            case NumberValue n -> NumberValue.general(n.value());
            default -> "";
        };
    }

    private static CellValue textAround(FunctionContext c, FunctionArgs a, boolean before) {
        List<CellValue> delimiters = flatten(c, a.value(1));
        int instance = a.integer(2, 1);
        boolean ignoreCase = a.integer(3, 0) == 1;
        boolean matchEnd = a.integer(4, 0) == 1;
        CellValue notFound = a.has(5) ? a.scalar(5) : null;
        return liftText(c, a, 0, s -> {
            if (instance == 0 || Math.abs(instance) > s.length() + 1) throw EvalError.value();
            String hay = ignoreCase ? s.toLowerCase(Locale.ROOT) : s;
            List<int[]> hits = new ArrayList<>();
            for (int pos = 0; pos <= s.length(); pos++) {
                for (CellValue dv : delimiters) {
                    String d = Coerce.text(dv);
                    String dd = ignoreCase ? d.toLowerCase(Locale.ROOT) : d;
                    if (d.isEmpty()) { hits.add(new int[]{pos, pos}); break; }
                    if (hay.startsWith(dd, pos)) { hits.add(new int[]{pos, pos + d.length()}); break; }
                }
            }
            if (!delimiters.isEmpty() && Coerce.text(delimiters.getFirst()).isEmpty()) {
                return before ? text(instance > 0 ? "" : s) : text(instance > 0 ? s : "");
            }
            int[] hit;
            if (instance > 0) {
                if (instance > hits.size()) {
                    if (matchEnd && instance == hits.size() + 1) hit = new int[]{s.length(), s.length()};
                    else return notFound != null ? notFound : err(CellError.NA);
                } else hit = hits.get(instance - 1);
            } else {
                int idx = hits.size() + instance;
                if (idx < 0) {
                    if (matchEnd && idx == -1) hit = new int[]{0, 0};
                    else return notFound != null ? notFound : err(CellError.NA);
                } else hit = hits.get(idx);
            }
            return text(before ? s.substring(0, hit[0]) : s.substring(hit[1]));
        });
    }

    private static CellValue textSplit(FunctionContext c, FunctionArgs a) {
        String s = a.text(0);
        List<String> colDelims = a.has(1) ? flatten(c, a.value(1)).stream().map(Coerce::text).toList() : List.of();
        List<String> rowDelims = a.has(2) ? flatten(c, a.value(2)).stream().map(Coerce::text).toList() : List.of();
        boolean ignoreEmpty = a.bool(3, false);
        boolean ignoreCase = a.integer(4, 0) == 1;
        CellValue pad = a.has(5) ? a.scalar(5) : err(CellError.NA);
        List<String> rows = rowDelims.isEmpty() ? List.of(s) : split(s, rowDelims, ignoreCase, ignoreEmpty);
        List<List<String>> grid = new ArrayList<>();
        int width = 0;
        for (String row : rows) {
            List<String> cells = colDelims.isEmpty() ? List.of(row) : split(row, colDelims, ignoreCase, ignoreEmpty);
            grid.add(cells);
            width = Math.max(width, cells.size());
        }
        if (grid.isEmpty() || width == 0) throw EvalError.calc();
        ArrayValue out = ArrayValue.of(grid.size(), width);
        for (int r = 0; r < grid.size(); r++) for (int k = 0; k < width; k++) out.set(r, k, k < grid.get(r).size() ? text(grid.get(r).get(k)) : pad);
        return out;
    }

    static List<String> split(String s, List<String> delimiters, boolean ignoreCase, boolean ignoreEmpty) {
        List<String> out = new ArrayList<>();
        String hay = ignoreCase ? s.toLowerCase(Locale.ROOT) : s;
        int start = 0, pos = 0;
        while (pos <= s.length()) {
            int len = -1;
            for (String d : delimiters) {
                if (d.isEmpty()) continue;
                String dd = ignoreCase ? d.toLowerCase(Locale.ROOT) : d;
                if (hay.startsWith(dd, pos)) { len = d.length(); break; }
            }
            if (len > 0) {
                String part = s.substring(start, pos);
                if (!(ignoreEmpty && part.isEmpty())) out.add(part);
                pos += len;
                start = pos;
            } else pos++;
        }
        String last = s.substring(Math.min(start, s.length()));
        if (!(ignoreEmpty && last.isEmpty())) out.add(last);
        return out;
    }

    static boolean omitted(CellValue v) { return v instanceof OmittedValue; }
}
