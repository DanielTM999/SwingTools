package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class QueryLanguage {
    record Column(int index, String label) implements QueryExpr {}
    record Literal(CellValue value) implements QueryExpr {}
    record Aggregate(String function, QueryExpr argument) implements QueryExpr {}
    record Call(String function, List<QueryExpr> args) implements QueryExpr {}
    record Binary(String op, QueryExpr left, QueryExpr right) implements QueryExpr {}
    record Unary(String op, QueryExpr operand) implements QueryExpr {}
    record Sort(QueryExpr expr, boolean descending) {}

    private record Token(String type, String text) {
        boolean is(String t) { return type.equals("id") && text.equalsIgnoreCase(t) || type.equals("op") && text.equals(t); }
    }

    private final List<Token> tokens = new ArrayList<>();
    private final Map<String, Integer> columns;
    private final boolean date1904;
    private final NumberFormatter formatter;
    private int pos;

    private List<QueryExpr> select = null;
    private QueryExpr where;
    private final List<QueryExpr> groupBy = new ArrayList<>(), pivot = new ArrayList<>();
    private final List<Sort> orderBy = new ArrayList<>();
    private int limit = -1, offset;
    private final Map<QueryExpr, String> labels = new LinkedHashMap<>(), formats = new LinkedHashMap<>();

    private QueryLanguage(String query, Map<String, Integer> columns, boolean date1904, NumberFormatter formatter) {
        this.columns = columns;
        this.date1904 = date1904;
        this.formatter = formatter;
        tokenize(query);
        parse();
    }

    static ArrayValue run(ArrayValue data, String query, int headers, int firstColumn, boolean fromRange, boolean date1904, NumberFormatter formatter) {
        if (headers < 0) headers = detectHeaders(data);
        Map<String, Integer> columns = new HashMap<>();
        for (int k = 0; k < data.columns(); k++) {
            columns.put("COL" + (k + 1), k);
            if (fromRange) columns.put(CellAddress.columnName(firstColumn + k), k);
        }
        QueryLanguage q = new QueryLanguage(query, columns, date1904, formatter);
        return q.execute(data, headers);
    }

    private static int detectHeaders(ArrayValue data) {
        if (data.rows() < 2) return 0;
        boolean firstText = true, restText = true;
        for (int c = 0; c < data.columns(); c++) {
            if (!(data.get(0, c) instanceof TextValue)) firstText = false;
            if (!(data.get(1, c) instanceof TextValue)) restText = false;
        }
        return firstText && !restText ? 1 : 0;
    }

    private void tokenize(String q) {
        int i = 0;
        while (i < q.length()) {
            char c = q.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '\'' || c == '"') {
                int end = q.indexOf(c, i + 1);
                if (end < 0) throw EvalError.value();
                tokens.add(new Token("str", q.substring(i + 1, end)));
                i = end + 1;
                continue;
            }
            if (c == '`') {
                int end = q.indexOf('`', i + 1);
                if (end < 0) throw EvalError.value();
                tokens.add(new Token("id", q.substring(i + 1, end)));
                i = end + 1;
                continue;
            }
            if (Character.isDigit(c) || c == '.' && i + 1 < q.length() && Character.isDigit(q.charAt(i + 1))) {
                int j = i;
                while (j < q.length() && (Character.isDigit(q.charAt(j)) || q.charAt(j) == '.' || q.charAt(j) == 'e' || q.charAt(j) == 'E')) j++;
                tokens.add(new Token("num", q.substring(i, j)));
                i = j;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < q.length() && (Character.isLetterOrDigit(q.charAt(j)) || q.charAt(j) == '_')) j++;
                tokens.add(new Token("id", q.substring(i, j)));
                i = j;
                continue;
            }
            String two = i + 1 < q.length() ? q.substring(i, i + 2) : "";
            if (two.equals("<=") || two.equals(">=") || two.equals("!=") || two.equals("<>")) { tokens.add(new Token("op", two)); i += 2; continue; }
            if ("=<>+-*/(),".indexOf(c) >= 0) { tokens.add(new Token("op", String.valueOf(c))); i++; continue; }
            throw EvalError.value();
        }
    }

    private Token peek() { return pos < tokens.size() ? tokens.get(pos) : new Token("end", ""); }
    private Token next() { return tokens.get(pos++); }
    private boolean accept(String t) { if (peek().is(t)) { pos++; return true; } return false; }
    private void expect(String t) { if (!accept(t)) throw EvalError.value(); }
    private boolean clauseStart() {
        Token t = peek();
        if (t.type.equals("end")) return true;
        for (String k : new String[]{"where", "group", "pivot", "order", "limit", "offset", "label", "format", "options"}) if (t.is(k)) return true;
        return false;
    }

    private void parse() {
        if (accept("select")) {
            if (accept("*")) select = null;
            else { select = new ArrayList<>(); do select.add(expression()); while (accept(",")); }
        }
        while (!peek().type.equals("end")) {
            if (accept("where")) where = expression();
            else if (accept("group")) { expect("by"); do groupBy.add(expression()); while (accept(",")); }
            else if (accept("pivot")) { do pivot.add(expression()); while (accept(",")); }
            else if (accept("order")) { expect("by"); do { QueryExpr e = expression(); boolean desc = accept("desc"); if (!desc) accept("asc"); orderBy.add(new Sort(e, desc)); } while (accept(",")); }
            else if (accept("limit")) limit = (int) Double.parseDouble(next().text);
            else if (accept("offset")) offset = (int) Double.parseDouble(next().text);
            else if (accept("label")) { do { QueryExpr e = expression(); labels.put(e, next().text); } while (accept(",")); }
            else if (accept("format")) { do { QueryExpr e = expression(); formats.put(e, next().text); } while (accept(",")); }
            else if (accept("options")) { while (!clauseStart() || peek().is("options")) { if (peek().type.equals("end")) break; pos++; } }
            else throw EvalError.value();
        }
    }

    private QueryExpr expression() { return or(); }

    private QueryExpr or() { QueryExpr l = and(); while (accept("or")) l = new Binary("or", l, and()); return l; }
    private QueryExpr and() { QueryExpr l = not(); while (accept("and")) l = new Binary("and", l, not()); return l; }
    private QueryExpr not() { if (accept("not")) return new Unary("not", not()); return comparison(); }

    private QueryExpr comparison() {
        QueryExpr l = additive();
        while (true) {
            Token t = peek();
            if (t.type.equals("op") && Set.of("=", "!=", "<>", "<", ">", "<=", ">=").contains(t.text)) { pos++; l = new Binary(t.text.equals("<>") ? "!=" : t.text, l, additive()); continue; }
            if (t.is("is")) { pos++; boolean negate = accept("not"); expect("null"); l = new Unary(negate ? "notnull" : "isnull", l); continue; }
            if (t.is("contains")) { pos++; l = new Binary("contains", l, additive()); continue; }
            if (t.is("starts")) { pos++; expect("with"); l = new Binary("starts", l, additive()); continue; }
            if (t.is("ends")) { pos++; expect("with"); l = new Binary("ends", l, additive()); continue; }
            if (t.is("matches")) { pos++; l = new Binary("matches", l, additive()); continue; }
            if (t.is("like")) { pos++; l = new Binary("like", l, additive()); continue; }
            return l;
        }
    }

    private QueryExpr additive() {
        QueryExpr l = multiplicative();
        while (peek().is("+") || peek().is("-")) { String op = next().text; l = new Binary(op, l, multiplicative()); }
        return l;
    }

    private QueryExpr multiplicative() {
        QueryExpr l = unary();
        while (peek().is("*") || peek().is("/")) { String op = next().text; l = new Binary(op, l, unary()); }
        return l;
    }

    private QueryExpr unary() {
        if (accept("-")) return new Unary("neg", unary());
        return primary();
    }

    private QueryExpr primary() {
        Token t = next();
        switch (t.type) {
            case "num" -> { return new Literal(CellValue.of(Double.parseDouble(t.text))); }
            case "str" -> { return new Literal(CellValue.of(t.text)); }
            case "op" -> {
                if (t.text.equals("(")) { QueryExpr e = expression(); expect(")"); return e; }
                throw EvalError.value();
            }
            default -> { }
        }
        String id = t.text, upper = id.toUpperCase(Locale.ROOT);
        if (upper.equals("TRUE") || upper.equals("FALSE")) return new Literal(CellValue.of(upper.equals("TRUE")));
        if (upper.equals("NULL")) return new Literal(CellValue.EMPTY);
        if ((upper.equals("DATE") || upper.equals("DATETIME") || upper.equals("TIMEOFDAY")) && peek().type.equals("str")) {
            String s = next().text;
            return new Literal(CellValue.of(literalDate(upper, s)));
        }
        if (peek().is("(")) {
            pos++;
            List<QueryExpr> args = new ArrayList<>();
            if (!accept(")")) { do args.add(accept("*") ? new Literal(CellValue.of(1)) : expression()); while (accept(",")); expect(")"); }
            String fn = id.toLowerCase(Locale.ROOT);
            if (Set.of("sum", "avg", "count", "max", "min").contains(fn)) return new Aggregate(fn, args.isEmpty() ? new Literal(CellValue.of(1)) : args.getFirst());
            return new Call(fn, args);
        }
        Integer col = columns.get(upper);
        if (col == null) throw EvalError.value();
        return new Column(col, id);
    }

    private double literalDate(String type, String s) {
        try {
            return switch (type) {
                case "DATE" -> DateSerial.toSerial(LocalDate.parse(s.strip()), date1904);
                case "DATETIME" -> DateSerial.toSerial(LocalDateTime.parse(s.strip().replace(' ', 'T')), date1904);
                default -> DateSerial.toSerial(java.time.LocalTime.parse(s.strip()));
            };
        } catch (RuntimeException e) { throw EvalError.value(); }
    }

    private ArrayValue execute(ArrayValue data, int headers) {
        List<String> headerNames = new ArrayList<>();
        for (int c = 0; c < data.columns(); c++) {
            StringBuilder b = new StringBuilder();
            for (int h = 0; h < headers && h < data.rows(); h++) { String t = data.get(h, c).display(); if (!t.isEmpty()) b.append(b.length() > 0 ? " " : "").append(t); }
            headerNames.add(b.toString());
        }
        List<CellValue[]> rows = new ArrayList<>();
        for (int r = headers; r < data.rows(); r++) {
            CellValue[] row = new CellValue[data.columns()];
            for (int c = 0; c < data.columns(); c++) row[c] = data.get(r, c);
            if (where != null && !truthy(eval(where, row, null))) continue;
            rows.add(row);
        }
        List<QueryExpr> selected = select;
        if (selected == null) { selected = new ArrayList<>(); for (int c = 0; c < data.columns(); c++) selected.add(new Column(c, CellAddress.columnName(c))); }
        boolean aggregate = !groupBy.isEmpty() || !pivot.isEmpty() || selected.stream().anyMatch(this::hasAggregate);
        List<String> outHeaders = new ArrayList<>();
        List<List<CellValue>> out = new ArrayList<>();
        List<QueryExpr> finalSelect = selected;
        if (!aggregate) {
            for (QueryExpr e : finalSelect) outHeaders.add(header(e, headerNames));
            List<CellValue[]> sorted = new ArrayList<>(rows);
            if (!orderBy.isEmpty()) sorted.sort(comparator());
            for (CellValue[] row : sorted) { List<CellValue> line = new ArrayList<>(); for (QueryExpr e : finalSelect) line.add(format(e, eval(e, row, null))); out.add(line); }
        } else {
            Map<List<Object>, List<CellValue[]>> groups = new LinkedHashMap<>();
            for (CellValue[] row : rows) {
                List<Object> key = new ArrayList<>();
                for (QueryExpr g : groupBy) key.add(keyOf(eval(g, row, null)));
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
            if (groups.isEmpty() && groupBy.isEmpty()) groups.put(List.of(), List.of());
            Set<List<Object>> pivotKeys = new LinkedHashSet<>();
            if (!pivot.isEmpty()) {
                for (CellValue[] row : rows) { List<Object> k = new ArrayList<>(); for (QueryExpr p : pivot) k.add(keyOf(eval(p, row, null))); pivotKeys.add(k); }
                List<List<Object>> sortedKeys = new ArrayList<>(pivotKeys);
                sortedKeys.sort((x, y) -> compareKeys(x, y));
                pivotKeys = new LinkedHashSet<>(sortedKeys);
            }
            for (QueryExpr e : finalSelect) {
                if (!pivot.isEmpty() && hasAggregate(e)) for (List<Object> pk : pivotKeys) outHeaders.add(joinKey(pk) + " " + header(e, headerNames));
                else outHeaders.add(header(e, headerNames));
            }
            List<Map.Entry<List<Object>, List<CellValue[]>>> entries = new ArrayList<>(groups.entrySet());
            if (!orderBy.isEmpty()) entries.sort((x, y) -> compareGroups(x.getValue(), y.getValue()));
            else if (!groupBy.isEmpty()) entries.sort((x, y) -> compareKeys(x.getKey(), y.getKey()));
            for (Map.Entry<List<Object>, List<CellValue[]>> g : entries) {
                List<CellValue> line = new ArrayList<>();
                for (QueryExpr e : finalSelect) {
                    if (!pivot.isEmpty() && hasAggregate(e)) {
                        for (List<Object> pk : pivotKeys) {
                            List<CellValue[]> subset = new ArrayList<>();
                            for (CellValue[] row : g.getValue()) { List<Object> k = new ArrayList<>(); for (QueryExpr p : pivot) k.add(keyOf(eval(p, row, null))); if (k.equals(pk)) subset.add(row); }
                            line.add(format(e, subset.isEmpty() ? CellValue.EMPTY : eval(e, subset.getFirst(), subset)));
                        }
                    } else line.add(format(e, eval(e, g.getValue().isEmpty() ? new CellValue[data.columns()] : g.getValue().getFirst(), g.getValue())));
                }
                out.add(line);
            }
        }
        int from = Math.min(offset, out.size()), to = limit >= 0 ? Math.min(out.size(), from + limit) : out.size();
        List<List<CellValue>> page = new ArrayList<>(out.subList(from, to));
        boolean showHeaders = headers > 0 || aggregate || !labels.isEmpty();
        if (showHeaders) {
            List<CellValue> h = new ArrayList<>();
            for (String s : outHeaders) h.add(CellValue.of(s));
            page.addFirst(h);
        }
        if (page.isEmpty()) throw EvalError.na();
        for (List<CellValue> line : page) for (int k = 0; k < line.size(); k++) if (line.get(k) == null) line.set(k, CellValue.EMPTY);
        return ArrayValue.of(page);
    }

    private int compareGroups(List<CellValue[]> x, List<CellValue[]> y) {
        for (Sort s : orderBy) {
            CellValue a = x.isEmpty() ? CellValue.EMPTY : eval(s.expr(), x.getFirst(), x), b = y.isEmpty() ? CellValue.EMPTY : eval(s.expr(), y.getFirst(), y);
            int c = ArrayFunctions.compareSort(a, b);
            if (c != 0) return s.descending() ? -c : c;
        }
        return 0;
    }

    private Comparator<CellValue[]> comparator() {
        return (x, y) -> {
            for (Sort s : orderBy) {
                int c = ArrayFunctions.compareSort(eval(s.expr(), x, null), eval(s.expr(), y, null));
                if (c != 0) return s.descending() ? -c : c;
            }
            return 0;
        };
    }

    private static int compareKeys(List<Object> x, List<Object> y) {
        for (int k = 0; k < Math.min(x.size(), y.size()); k++) {
            Object a = x.get(k), b = y.get(k);
            int c;
            if (a instanceof Double da && b instanceof Double db) c = Double.compare(da, db);
            else c = String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
            if (c != 0) return c;
        }
        return Integer.compare(x.size(), y.size());
    }

    private static String joinKey(List<Object> k) {
        List<String> parts = new ArrayList<>();
        for (Object o : k) parts.add(o instanceof Double d ? NumberValue.general(d) : String.valueOf(o));
        return String.join(", ", parts);
    }

    private static Object keyOf(CellValue v) {
        return switch (v) {
            case NumberValue n -> n.value();
            case TextValue t -> t.value();
            case BoolValue b -> b.value() ? "TRUE" : "FALSE";
            default -> "";
        };
    }

    private boolean hasAggregate(QueryExpr e) {
        return switch (e) {
            case Aggregate a -> true;
            case Binary b -> hasAggregate(b.left()) || hasAggregate(b.right());
            case Unary u -> hasAggregate(u.operand());
            case Call c -> c.args().stream().anyMatch(this::hasAggregate);
            default -> false;
        };
    }

    private String header(QueryExpr e, List<String> names) {
        for (Map.Entry<QueryExpr, String> l : labels.entrySet()) if (l.getKey().equals(e)) return l.getValue();
        return switch (e) {
            case Column c -> names.get(c.index()).isEmpty() ? "" : names.get(c.index());
            case Aggregate a -> a.function() + " " + (a.argument() instanceof Column c ? (names.get(c.index()).isEmpty() ? c.label() : names.get(c.index())) : "");
            case Literal l -> l.value().display();
            default -> "";
        };
    }

    private CellValue format(QueryExpr e, CellValue v) {
        for (Map.Entry<QueryExpr, String> f : formats.entrySet()) {
            if (!f.getKey().equals(e)) continue;
            if (v instanceof NumberValue n) return CellValue.of(formatter.text(n, f.getValue().replace("MM", "mm").replace("HH", "hh")));
        }
        return v;
    }

    private CellValue eval(QueryExpr e, CellValue[] row, List<CellValue[]> group) {
        return switch (e) {
            case Column c -> c.index() < row.length && row[c.index()] != null ? row[c.index()] : CellValue.EMPTY;
            case Literal l -> l.value();
            case Aggregate a -> aggregate(a, group == null ? List.<CellValue[]>of(row) : group);
            case Unary u -> switch (u.op()) {
                case "not" -> CellValue.of(!truthy(eval(u.operand(), row, group)));
                case "neg" -> CellValue.of(-number(eval(u.operand(), row, group)));
                case "isnull" -> CellValue.of(eval(u.operand(), row, group).isEmpty());
                default -> CellValue.of(!eval(u.operand(), row, group).isEmpty());
            };
            case Binary b -> binary(b, row, group);
            case Call c -> call(c, row, group);
            default -> CellValue.EMPTY;
        };
    }

    private CellValue aggregate(Aggregate a, List<CellValue[]> rows) {
        double sum = 0, max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
        int count = 0;
        CellValue textMax = null, textMin = null;
        for (CellValue[] row : rows) {
            CellValue v = eval(a.argument(), row, null);
            if (v.isEmpty()) continue;
            count++;
            if (v instanceof NumberValue n) { sum += n.value(); max = Math.max(max, n.value()); min = Math.min(min, n.value()); }
            else {
                if (textMax == null || ArrayFunctions.compareSort(v, textMax) > 0) textMax = v;
                if (textMin == null || ArrayFunctions.compareSort(v, textMin) < 0) textMin = v;
            }
        }
        return switch (a.function()) {
            case "count" -> CellValue.of(count);
            case "sum" -> CellValue.of(sum);
            case "avg" -> count == 0 ? CellValue.EMPTY : CellValue.of(sum / count);
            case "max" -> max != Double.NEGATIVE_INFINITY ? CellValue.of(max) : textMax != null ? textMax : CellValue.EMPTY;
            default -> min != Double.POSITIVE_INFINITY ? CellValue.of(min) : textMin != null ? textMin : CellValue.EMPTY;
        };
    }

    private CellValue binary(Binary b, CellValue[] row, List<CellValue[]> group) {
        if (b.op().equals("and")) return CellValue.of(truthy(eval(b.left(), row, group)) && truthy(eval(b.right(), row, group)));
        if (b.op().equals("or")) return CellValue.of(truthy(eval(b.left(), row, group)) || truthy(eval(b.right(), row, group)));
        CellValue l = eval(b.left(), row, group), r = eval(b.right(), row, group);
        switch (b.op()) {
            case "+" -> { return CellValue.of(number(l) + number(r)); }
            case "-" -> { return CellValue.of(number(l) - number(r)); }
            case "*" -> { return CellValue.of(number(l) * number(r)); }
            case "/" -> { double d = number(r); return d == 0 ? CellValue.EMPTY : CellValue.of(number(l) / d); }
            case "contains" -> { return CellValue.of(l.display().contains(r.display())); }
            case "starts" -> { return CellValue.of(l.display().startsWith(r.display())); }
            case "ends" -> { return CellValue.of(l.display().endsWith(r.display())); }
            case "matches" -> { return CellValue.of(Pattern.compile(r.display()).matcher(l.display()).matches()); }
            case "like" -> {
                String p = r.display().replace("%", "\u0001").replace("_", "\u0002");
                String regex = Pattern.quote(p).replace("\u0001", "\\E.*\\Q").replace("\u0002", "\\E.\\Q");
                return CellValue.of(Pattern.compile(regex, Pattern.DOTALL).matcher(l.display()).matches());
            }
            default -> { }
        }
        if (l.isEmpty() || r.isEmpty()) {
            boolean eq = l.isEmpty() && r.isEmpty();
            return CellValue.of(b.op().equals("=") ? eq : b.op().equals("!=") && !eq);
        }
        int c;
        if (l instanceof NumberValue && r instanceof NumberValue || l instanceof BoolValue && r instanceof BoolValue) c = Coerce.compare(l, r);
        else if (l instanceof TextValue && r instanceof TextValue) c = l.display().compareTo(r.display());
        else return CellValue.of(b.op().equals("!="));
        return CellValue.of(switch (b.op()) { case "=" -> c == 0; case "!=" -> c != 0; case "<" -> c < 0; case ">" -> c > 0; case "<=" -> c <= 0; default -> c >= 0; });
    }

    private CellValue call(Call c, CellValue[] row, List<CellValue[]> group) {
        List<CellValue> args = new ArrayList<>();
        for (QueryExpr e : c.args()) args.add(eval(e, row, group));
        CellValue first = args.isEmpty() ? CellValue.EMPTY : args.getFirst();
        switch (c.function()) {
            case "upper" -> { return CellValue.of(first.display().toUpperCase(Locale.ROOT)); }
            case "lower" -> { return CellValue.of(first.display().toLowerCase(Locale.ROOT)); }
            case "now" -> { return CellValue.of(DateSerial.toSerial(LocalDateTime.now(), date1904)); }
            case "todate" -> { return CellValue.of(Math.floor(number(first))); }
            case "datediff" -> { return CellValue.of(Math.floor(number(first)) - Math.floor(number(args.get(1)))); }
            default -> { }
        }
        if (first.isEmpty()) return CellValue.EMPTY;
        int[] p = DateSerial.parts(number(first), date1904);
        return switch (c.function()) {
            case "year" -> CellValue.of(p[0]);
            case "month" -> CellValue.of(p[1] - 1);
            case "day" -> CellValue.of(p[2]);
            case "hour" -> CellValue.of(p[3]);
            case "minute" -> CellValue.of(p[4]);
            case "second" -> CellValue.of(p[5]);
            case "millisecond" -> CellValue.of(p[6]);
            case "quarter" -> CellValue.of((p[1] - 1) / 3 + 1);
            case "dayofweek" -> CellValue.of(p[7] + 1);
            default -> throw EvalError.value();
        };
    }

    private static boolean truthy(CellValue v) {
        return switch (v) {
            case BoolValue b -> b.value();
            case NumberValue n -> n.value() != 0;
            case TextValue t -> !t.value().isEmpty();
            default -> false;
        };
    }

    private static double number(CellValue v) {
        if (v instanceof NumberValue n) return n.value();
        if (v instanceof BoolValue b) return b.value() ? 1 : 0;
        if (v instanceof EmptyValue) return 0;
        if (v instanceof ErrorValue e) throw EvalError.of(e.error());
        Double d = Coerce.tryNumber(v);
        return d == null ? 0 : d;
    }

    static long daysBetween(LocalDate a, LocalDate b) { return ChronoUnit.DAYS.between(a, b); }
}
