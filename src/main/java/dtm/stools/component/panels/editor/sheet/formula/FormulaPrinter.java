package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class FormulaPrinter {
    private static final Pattern PLAIN_SHEET = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_.]*");

    private final FormulaLocale locale;
    private final CellAddress host;
    private final boolean r1c1;
    private java.util.function.UnaryOperator<String> functionMapper = java.util.function.UnaryOperator.identity();
    private java.util.function.BiFunction<String, java.util.Set<String>, String> nameMapper = (n, scope) -> n;
    private final java.util.Deque<java.util.Set<String>> lambdaScopes = new java.util.ArrayDeque<>();

    public FormulaPrinter(FormulaLocale locale) { this(locale, null, false); }
    public FormulaPrinter(FormulaLocale locale, CellAddress host, boolean r1c1) { this.locale = locale; this.host = host; this.r1c1 = r1c1 && host != null; }

    public FormulaPrinter withFunctionMapper(java.util.function.UnaryOperator<String> mapper) { functionMapper = mapper; return this; }
    private java.util.function.Function<RefNode, String> referenceMapper;

    public FormulaPrinter withReferenceMapper(java.util.function.Function<RefNode, String> mapper) { referenceMapper = mapper; return this; }
    public FormulaPrinter withNameMapper(java.util.function.BiFunction<String, java.util.Set<String>, String> mapper) { nameMapper = mapper; return this; }

    public static String canonical(FormulaNode node) { return new FormulaPrinter(FormulaLocale.EN).print(node); }

    public String print(FormulaNode node) {
        StringBuilder b = new StringBuilder();
        write(b, node);
        return b.toString();
    }

    private void write(StringBuilder b, FormulaNode node) {
        switch (node) {
            case NumberNode n -> b.append(number(n.value()));
            case StringNode s -> b.append('"').append(s.value().replace("\"", "\"\"")).append('"');
            case BoolNode v -> b.append(v.value() ? locale.trueText() : locale.falseText());
            case ErrorNode e -> b.append(locale.localizedErrors() ? e.error().localized() : e.error().text());
            case MissingNode m -> { }
            case RefNode r -> { if (referenceMapper != null) b.append(referenceMapper.apply(r)); else reference(b, r); }
            case NameNode n -> {
                if (n.sheet() != null) b.append(sheet(n.sheet())).append('!');
                java.util.Set<String> scope = new java.util.HashSet<>();
                for (java.util.Set<String> x : lambdaScopes) scope.addAll(x);
                b.append(nameMapper.apply(n.name(), scope));
            }
            case StructuredRefNode s -> b.append(StructuredReferences.print(s));
            case FunctionNode f -> {
                b.append(functionMapper.apply(locale.localizeFunction(f.name()))).append('(');
                boolean scoped = f.name().equals("LET") || f.name().equals("LAMBDA");
                if (scoped) {
                    java.util.Set<String> params = new java.util.HashSet<>();
                    for (int i = 0; i < f.args().size() - 1; i++) if (f.args().get(i) instanceof NameNode p && (f.name().equals("LAMBDA") || i % 2 == 0)) params.add(p.name().toUpperCase(Locale.ROOT));
                    lambdaScopes.push(params);
                }
                arguments(b, f.args());
                if (scoped) lambdaScopes.pop();
                b.append(')');
            }
            case CallNode c -> { write(b, c.target()); b.append('('); arguments(b, c.args()); b.append(')'); }
            case UnaryNode u -> {
                if (u.operator().equals("%") || u.operator().equals("#")) { write(b, u.operand()); b.append(u.operator()); }
                else { b.append(u.operator()); write(b, u.operand()); }
            }
            case BinaryNode bin -> {
                write(b, bin.left());
                String op = bin.operator().equals(",") ? String.valueOf(locale.argumentSeparator()) : bin.operator();
                b.append(op);
                write(b, bin.right());
            }
            case ParenNode p -> { b.append('('); write(b, p.inner()); b.append(')'); }
            case ArrayNode a -> {
                b.append('{');
                char col = locale.arrayColumnSeparator(), row = locale.arrayRowSeparator();
                for (int i = 0; i < a.rows().size(); i++) {
                    if (i > 0) b.append(row);
                    List<FormulaNode> r = a.rows().get(i);
                    for (int j = 0; j < r.size(); j++) { if (j > 0) b.append(col); write(b, r.get(j)); }
                }
                b.append('}');
            }
            default -> throw new IllegalArgumentException("Unknown node " + node);
        }
    }

    private void arguments(StringBuilder b, List<FormulaNode> args) {
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) b.append(locale.argumentSeparator());
            write(b, args.get(i));
        }
    }

    public String number(double v) {
        String s;
        if (v == Math.rint(v) && Math.abs(v) < 1e15) s = String.valueOf((long) v);
        else {
            s = BigDecimal.valueOf(v).stripTrailingZeros().toString();
            if (s.contains("E")) s = s.replace("E+", "E");
        }
        return locale.decimalSeparator() == '.' ? s : s.replace('.', locale.decimalSeparator());
    }

    private void reference(StringBuilder b, RefNode r) {
        if (r.external() != null) b.append('[').append(r.external()).append(']');
        if (r.sheet() != null) {
            if (r.sheetEnd() != null) {
                String both = r.sheet() + ":" + r.sheetEnd();
                boolean plain = PLAIN_SHEET.matcher(r.sheet()).matches() && PLAIN_SHEET.matcher(r.sheetEnd()).matches() && !looksLikeRef(r.sheet()) && !looksLikeRef(r.sheetEnd());
                b.append(plain ? both : "'" + both.replace("'", "''") + "'").append('!');
            } else b.append(sheet(r.sheet())).append('!');
        } else if (r.external() != null) b.append('!');
        if (r1c1) { r1c1(b, r); return; }
        RefPart a = r.first(), c = r.second();
        if (c == null) { cell(b, a); return; }
        if (a.wholeColumn() && c.wholeColumn()) {
            b.append(a.columnAbsolute() ? "$" : "").append(CellAddress.columnName(a.column())).append(':').append(c.columnAbsolute() ? "$" : "").append(CellAddress.columnName(c.column()));
            return;
        }
        if (a.wholeRow() && c.wholeRow()) {
            b.append(a.rowAbsolute() ? "$" : "").append(a.row() + 1).append(':').append(c.rowAbsolute() ? "$" : "").append(c.row() + 1);
            return;
        }
        cell(b, a);
        b.append(':');
        cell(b, c);
    }

    private void r1c1(StringBuilder b, RefNode r) {
        RefPart a = r.first(), c = r.second();
        if (c != null && a.wholeColumn()) { b.append(r1c1Col(a)).append(':').append(r1c1Col(c)); return; }
        if (c != null && a.wholeRow()) { b.append(r1c1Row(a)).append(':').append(r1c1Row(c)); return; }
        b.append(r1c1Row(a)).append(r1c1Col(a));
        if (c != null) b.append(':').append(r1c1Row(c)).append(r1c1Col(c));
    }

    private String r1c1Row(RefPart p) {
        if (p.rowAbsolute()) return "R" + (p.row() + 1);
        int d = p.row() - host.row();
        return d == 0 ? "R" : "R[" + d + "]";
    }

    private String r1c1Col(RefPart p) {
        if (p.columnAbsolute()) return "C" + (p.column() + 1);
        int d = p.column() - host.column();
        return d == 0 ? "C" : "C[" + d + "]";
    }

    private static void cell(StringBuilder b, RefPart p) {
        if (p.columnAbsolute()) b.append('$');
        b.append(CellAddress.columnName(p.column()));
        if (p.rowAbsolute()) b.append('$');
        b.append(p.row() + 1);
    }

    public static String sheet(String name) {
        if (PLAIN_SHEET.matcher(name).matches() && !looksLikeRef(name)) return name;
        return "'" + name.replace("'", "''") + "'";
    }

    private static boolean looksLikeRef(String name) {
        String u = name.toUpperCase(Locale.ROOT);
        return u.matches("[A-Z]{1,3}[0-9]+") || u.matches("R[0-9]*C[0-9]*") || u.equals("TRUE") || u.equals("FALSE");
    }

    static String format(double v) { return NumberValue.general(v); }
}
