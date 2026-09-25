package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.formula.ArrayNode;
import dtm.stools.component.panels.editor.sheet.formula.BinaryNode;
import dtm.stools.component.panels.editor.sheet.formula.BoolNode;
import dtm.stools.component.panels.editor.sheet.formula.CallNode;
import dtm.stools.component.panels.editor.sheet.formula.ErrorNode;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.FormulaParser;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.formula.FunctionNode;
import dtm.stools.component.panels.editor.sheet.formula.MissingNode;
import dtm.stools.component.panels.editor.sheet.formula.NameNode;
import dtm.stools.component.panels.editor.sheet.formula.NumberNode;
import dtm.stools.component.panels.editor.sheet.formula.ParenNode;
import dtm.stools.component.panels.editor.sheet.formula.RefNode;
import dtm.stools.component.panels.editor.sheet.formula.StringNode;
import dtm.stools.component.panels.editor.sheet.formula.StructuredRefNode;
import dtm.stools.component.panels.editor.sheet.formula.UnaryNode;
import dtm.stools.component.panels.editor.sheet.function.CellConsumer;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.provider.SheetExternalDataProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class Evaluator implements FunctionContext {
    private static final int MAX_DEPTH = 256;

    private final CalcEngine engine;
    private final int sheet, row, column;
    private final Map<String, CellValue> bindings;
    private final int depth;

    Evaluator(CalcEngine engine, int sheet, int row, int column) { this(engine, sheet, row, column, Map.of(), 0); }

    private Evaluator(CalcEngine engine, int sheet, int row, int column, Map<String, CellValue> bindings, int depth) {
        this.engine = engine; this.sheet = sheet; this.row = row; this.column = column; this.bindings = bindings; this.depth = depth;
        if (depth > MAX_DEPTH) throw EvalError.of(CellError.NUM);
    }

    public CellValue evaluateTop(FormulaNode node) {
        CellValue v;
        try { v = evaluate(node); } catch (EvalError e) { return e.toValue(); } catch (StackOverflowError e) { return CellValue.error(CellError.NUM); }
        v = derefTop(v);
        if (v instanceof LambdaValue) return CellValue.error(CellError.CALC);
        if (v instanceof OmittedValue) return CellValue.ZERO;
        if (v instanceof EmptyValue) return CellValue.ZERO;
        if (v instanceof ArrayValue a && a.size() == 1) { CellValue x = a.get(0, 0); return x.isEmpty() ? CellValue.ZERO : x; }
        return v;
    }

    private CellValue derefTop(CellValue v) {
        try {
            if (v instanceof ReferenceValue r) {
                if (!r.isSingleArea()) return CellValue.error(CellError.VALUE);
                if (r.isSingleCell()) { CellValue x = cell(r.sheet(), r.range().firstRow(), r.range().firstColumn()); return x.isEmpty() ? CellValue.ZERO : x; }
                ArrayValue a = toArray(r);
                return a.map(x -> x.isEmpty() ? CellValue.ZERO : x);
            }
            if (v instanceof ArrayValue a) return a.map(x -> x instanceof ReferenceValue || x instanceof OmittedValue ? scalar(x) : x.isEmpty() ? CellValue.ZERO : x);
            return v;
        } catch (EvalError e) { return e.toValue(); }
    }

    @Override public CellValue evaluate(FormulaNode node) {
        return switch (node) {
            case NumberNode n -> new NumberValue(n.value());
            case StringNode s -> new TextValue(s.value());
            case BoolNode b -> CellValue.of(b.value());
            case ErrorNode e -> ErrorValue.of(e.error());
            case MissingNode m -> OmittedValue.INSTANCE;
            case RefNode r -> reference(r);
            case NameNode n -> name(n);
            case StructuredRefNode s -> structured(s);
            case FunctionNode f -> function(f);
            case CallNode c -> call(c);
            case UnaryNode u -> unary(u);
            case BinaryNode b -> binary(b);
            case ParenNode p -> evaluate(p.inner());
            case ArrayNode a -> array(a);
            default -> throw EvalError.value();
        };
    }

    @Override public CellValue evaluate(FormulaNode node, Map<String, CellValue> extra) {
        Map<String, CellValue> merged = new HashMap<>(bindings);
        extra.forEach((k, v) -> merged.put(k.toUpperCase(Locale.ROOT), v));
        return new Evaluator(engine, sheet, row, column, merged, depth + 1).evaluate(node);
    }

    private CellValue reference(RefNode r) {
        if (r.external() != null) return CellValue.error(CellError.REF);
        int s1 = r.sheet() == null ? sheet : engine.workbook().indexOf(r.sheet());
        int s2 = r.sheetEnd() == null ? s1 : engine.workbook().indexOf(r.sheetEnd());
        if (s1 < 0 || s2 < 0) return CellValue.error(CellError.REF);
        return new ReferenceValue(Math.min(s1, s2), Math.max(s1, s2), List.of(r.range()));
    }

    private CellValue name(NameNode n) {
        String upper = n.name().toUpperCase(Locale.ROOT);
        if (n.sheet() == null) {
            CellValue bound = bindings.get(upper);
            if (bound != null) return bound;
        }
        SheetWorkbook wb = engine.workbook();
        Integer scope = n.sheet() == null ? sheet : wb.indexOf(n.sheet());
        Optional<DefinedName> dn = wb.name(n.name(), scope);
        if (dn.isPresent()) {
            FormulaNode ast;
            try { ast = Formulas.parseCanonical(dn.get().formula()); } catch (RuntimeException e) { return CellValue.error(CellError.NAME); }
            return new Evaluator(engine, sheet, row, column, Map.of(), depth + 1).evaluate(ast);
        }
        Optional<SheetTable> t = wb.table(n.name());
        if (t.isPresent()) return ReferenceValue.of(wb.sheetOfTable(n.name()), t.get().dataRange());
        String canonical = engine.formulaLocale().canonicalFunction(n.name());
        if (n.sheet() == null && engine.functions().find(canonical).isPresent())
            return new LambdaValue(List.of("_ETA1"), new FunctionNode(canonical, List.of(new NameNode(null, "_ETA1"))), Map.of());
        return CellValue.error(CellError.NAME);
    }

    private CellValue structured(StructuredRefNode s) {
        SheetWorkbook wb = engine.workbook();
        SheetTable table = null;
        int tableSheet = sheet;
        if (s.table().isEmpty()) {
            for (SheetTable t : wb.sheet(sheet).properties().tables()) if (t.range().contains(row, column)) table = t;
        } else {
            table = wb.table(s.table()).orElse(null);
            tableSheet = wb.sheetOfTable(s.table());
        }
        if (table == null) return CellValue.error(CellError.REF);
        CellRange full = table.range();
        int firstRow, lastRow;
        List<String> items = s.items();
        boolean all = items.contains("#All"), data = items.contains("#Data") || items.isEmpty() && !s.thisRow(), headers = items.contains("#Headers"), totals = items.contains("#Totals");
        if (s.thisRow()) {
            if (row < full.firstRow() || row > full.lastRow()) return CellValue.error(CellError.VALUE);
            firstRow = lastRow = row;
        } else if (all) { firstRow = full.firstRow(); lastRow = full.lastRow(); }
        else {
            firstRow = Integer.MAX_VALUE; lastRow = -1;
            if (headers && table.headerRow()) { firstRow = Math.min(firstRow, full.firstRow()); lastRow = Math.max(lastRow, full.firstRow()); }
            if (data) { CellRange d = table.dataRange(); firstRow = Math.min(firstRow, d.firstRow()); lastRow = Math.max(lastRow, d.lastRow()); }
            if (totals && table.totalsRow()) { firstRow = Math.min(firstRow, full.lastRow()); lastRow = Math.max(lastRow, full.lastRow()); }
            if (lastRow < 0) return CellValue.error(CellError.REF);
        }
        int c1 = full.firstColumn(), c2 = full.lastColumn();
        if (s.firstColumn() != null) {
            int i = table.columnIndex(s.firstColumn());
            if (i < 0) return CellValue.error(CellError.REF);
            c1 = c2 = full.firstColumn() + i;
            if (s.lastColumn() != null) {
                int j = table.columnIndex(s.lastColumn());
                if (j < 0) return CellValue.error(CellError.REF);
                c2 = full.firstColumn() + j;
            }
        }
        return ReferenceValue.of(tableSheet, new CellRange(firstRow, c1, lastRow, c2));
    }

    private CellValue function(FunctionNode f) {
        String name = f.name();
        CellValue bound = bindings.get(name);
        if (bound instanceof LambdaValue l) return callLambda(l, evaluateArgs(f.args()));
        Optional<SheetFunction> fn = engine.functions().find(name);
        if (fn.isEmpty()) {
            Integer scope = sheet;
            Optional<DefinedName> dn = engine.workbook().name(name, scope);
            if (dn.isPresent()) {
                CellValue target = name(new NameNode(null, name));
                if (target instanceof LambdaValue l) return callLambda(l, evaluateArgs(f.args()));
                return CellValue.error(CellError.VALUE);
            }
            return CellValue.error(CellError.NAME);
        }
        SheetFunction function = fn.get();
        int n = f.args().size();
        if (n < function.minArgs() || n > function.maxArgs()) return CellValue.error(CellError.VALUE);
        try {
            CellValue v = function.call(this, new FunctionArgs(this, f.args()));
            return v == null ? CellValue.EMPTY : v;
        } catch (EvalError e) {
            return e.toValue();
        } catch (ArithmeticException | IndexOutOfBoundsException e) {
            return CellValue.error(CellError.NUM);
        }
    }

    private List<CellValue> evaluateArgs(List<FormulaNode> args) {
        List<CellValue> list = new ArrayList<>();
        for (FormulaNode a : args) list.add(evaluate(a));
        return list;
    }

    private CellValue call(CallNode c) {
        CellValue target = evaluate(c.target());
        if (target instanceof LambdaValue l) return callLambda(l, evaluateArgs(c.args()));
        if (target instanceof ErrorValue) return target;
        return CellValue.error(CellError.VALUE);
    }

    @Override public CellValue callLambda(LambdaValue lambda, List<CellValue> args) {
        if (args.size() > lambda.parameters().size()) return CellValue.error(CellError.VALUE);
        Map<String, CellValue> scope = new HashMap<>(lambda.closure());
        for (int i = 0; i < lambda.parameters().size(); i++) scope.put(lambda.parameters().get(i).toUpperCase(Locale.ROOT), i < args.size() ? args.get(i) : OmittedValue.INSTANCE);
        return new Evaluator(engine, sheet, row, column, scope, depth + 1).evaluate(lambda.body());
    }

    private CellValue unary(UnaryNode u) {
        switch (u.operator()) {
            case "@" -> {
                CellValue v = evaluate(u.operand());
                if (v instanceof ReferenceValue r) return implicitIntersection(r);
                if (v instanceof ArrayValue a) return a.get(0, 0);
                return v;
            }
            case "#" -> {
                CellValue v = evaluate(u.operand());
                if (!(v instanceof ReferenceValue r) || !r.isSingleCell()) return CellValue.error(CellError.REF);
                CellRange spill = engine.spillRange(r.sheet(), r.range().firstRow(), r.range().firstColumn());
                if (spill == null) return CellValue.error(CellError.REF);
                return ReferenceValue.of(r.sheet(), spill);
            }
            default -> { }
        }
        CellValue v = deref(evaluate(u.operand()));
        return switch (u.operator()) {
            case "-" -> mapNumeric(v, d -> -d);
            case "%" -> mapNumeric(v, d -> d / 100);
            default -> v instanceof OmittedValue ? CellValue.ZERO : v;
        };
    }

    private CellValue implicitIntersection(ReferenceValue r) {
        if (r.isSingleCell()) return cell(r.sheet(), r.range().firstRow(), r.range().firstColumn());
        if (!r.isSingleArea()) return CellValue.error(CellError.VALUE);
        CellRange a = r.range();
        if (a.columnCount() == 1 && row >= a.firstRow() && row <= a.lastRow()) return cell(r.sheet(), row, a.firstColumn());
        if (a.rowCount() == 1 && column >= a.firstColumn() && column <= a.lastColumn()) return cell(r.sheet(), a.firstRow(), column);
        return CellValue.error(CellError.VALUE);
    }

    private CellValue mapNumeric(CellValue v, NumericOperation op) {
        if (v instanceof ArrayValue a) return a.map(x -> { try { return Coerce.result(op.apply(Coerce.number(x))); } catch (EvalError e) { return e.toValue(); } });
        try { return Coerce.result(op.apply(Coerce.number(v))); } catch (EvalError e) { return e.toValue(); }
    }

    private CellValue binary(BinaryNode b) {
        switch (b.operator()) {
            case ":" -> { return rangeOperator(evaluate(b.left()), evaluate(b.right())); }
            case "," -> { return union(evaluate(b.left()), evaluate(b.right())); }
            case " " -> { return intersection(evaluate(b.left()), evaluate(b.right())); }
            default -> { }
        }
        CellValue left = deref(evaluate(b.left()));
        CellValue right = deref(evaluate(b.right()));
        BinaryOperation op = switch (b.operator()) {
            case "+" -> (x, y) -> arithmetic(x, y, '+');
            case "-" -> (x, y) -> arithmetic(x, y, '-');
            case "*" -> (x, y) -> arithmetic(x, y, '*');
            case "/" -> (x, y) -> arithmetic(x, y, '/');
            case "^" -> (x, y) -> arithmetic(x, y, '^');
            case "&" -> (x, y) -> { if (x instanceof ErrorValue) return x; if (y instanceof ErrorValue) return y; return new TextValue(Coerce.text(x) + Coerce.text(y)); };
            case "=", "<>", "<", ">", "<=", ">=" -> (x, y) -> comparison(x, y, b.operator());
            default -> throw EvalError.value();
        };
        return broadcast(left, right, op);
    }

    private CellValue broadcast(CellValue left, CellValue right, BinaryOperation op) {
        if (!(left instanceof ArrayValue) && !(right instanceof ArrayValue)) return safe(op, left, right);
        ArrayValue la = left instanceof ArrayValue a ? a : null, ra = right instanceof ArrayValue a ? a : null;
        int rows = Math.max(la == null ? 1 : la.rows(), ra == null ? 1 : ra.rows());
        int cols = Math.max(la == null ? 1 : la.columns(), ra == null ? 1 : ra.columns());
        ArrayValue out = ArrayValue.of(rows, cols);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                CellValue x = la == null ? left : la.broadcast(r, c), y = ra == null ? right : ra.broadcast(r, c);
                out.set(r, c, safe(op, x, y));
            }
        return out;
    }

    private static CellValue safe(BinaryOperation op, CellValue a, CellValue b) {
        try { return op.apply(a, b); } catch (EvalError e) { return e.toValue(); }
    }

    private static CellValue arithmetic(CellValue x, CellValue y, char op) {
        if (x instanceof ErrorValue) return x;
        if (y instanceof ErrorValue) return y;
        double a = Coerce.number(x), b = Coerce.number(y);
        double r = switch (op) {
            case '+' -> snap(a + b, a, b);
            case '-' -> snap(a - b, a, b);
            case '*' -> a * b;
            case '/' -> { if (b == 0) throw EvalError.div0(); yield a / b; }
            default -> {
                if (a == 0 && b == 0) throw EvalError.num();
                if (a == 0 && b < 0) throw EvalError.div0();
                double p = Math.pow(a, b);
                if (Double.isNaN(p) && a < 0) {
                    double inv = 1 / b;
                    if (inv == Math.rint(inv) && ((long) inv) % 2 != 0) p = -Math.pow(-a, b);
                }
                yield p;
            }
        };
        return Coerce.result(r);
    }

    private static double snap(double r, double a, double b) {
        double m = Math.max(Math.abs(a), Math.abs(b));
        return m != 0 && Math.abs(r) < m * 1e-15 ? 0 : r;
    }

    private static CellValue comparison(CellValue x, CellValue y, String op) {
        if (x instanceof ErrorValue) return x;
        if (y instanceof ErrorValue) return y;
        int c = Coerce.compare(x, y);
        boolean result = switch (op) {
            case "=" -> c == 0;
            case "<>" -> c != 0;
            case "<" -> c < 0;
            case ">" -> c > 0;
            case "<=" -> c <= 0;
            default -> c >= 0;
        };
        return CellValue.of(result);
    }

    private CellValue rangeOperator(CellValue a, CellValue b) {
        if (a instanceof ErrorValue) return a;
        if (b instanceof ErrorValue) return b;
        if (!(a instanceof ReferenceValue ra) || !(b instanceof ReferenceValue rb)) return CellValue.error(CellError.VALUE);
        if (ra.sheet() != rb.sheet()) return CellValue.error(CellError.REF);
        CellRange u = ra.range();
        for (CellRange r : ra.areas()) u = u.union(r);
        for (CellRange r : rb.areas()) u = u.union(r);
        return ReferenceValue.of(ra.sheet(), u);
    }

    private CellValue union(CellValue a, CellValue b) {
        if (a instanceof ErrorValue) return a;
        if (b instanceof ErrorValue) return b;
        if (!(a instanceof ReferenceValue ra) || !(b instanceof ReferenceValue rb)) return CellValue.error(CellError.VALUE);
        if (ra.sheet() != rb.sheet()) return CellValue.error(CellError.VALUE);
        List<CellRange> areas = new ArrayList<>(ra.areas());
        areas.addAll(rb.areas());
        return new ReferenceValue(ra.sheet(), ra.sheet(), areas);
    }

    private CellValue intersection(CellValue a, CellValue b) {
        if (a instanceof ErrorValue) return a;
        if (b instanceof ErrorValue) return b;
        if (!(a instanceof ReferenceValue ra) || !(b instanceof ReferenceValue rb)) return CellValue.error(CellError.VALUE);
        if (ra.sheet() != rb.sheet()) return CellValue.error(CellError.NULL);
        List<CellRange> out = new ArrayList<>();
        for (CellRange x : ra.areas()) for (CellRange y : rb.areas()) { CellRange i = x.intersection(y); if (i != null) out.add(i); }
        if (out.isEmpty()) return CellValue.error(CellError.NULL);
        return new ReferenceValue(ra.sheet(), ra.sheet(), out);
    }

    private CellValue array(ArrayNode a) {
        int rows = a.rowCount(), cols = a.columnCount();
        ArrayValue out = ArrayValue.of(rows, cols);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < a.rows().get(r).size(); c++) {
                CellValue v;
                try { v = scalar(evaluate(a.rows().get(r).get(c))); } catch (EvalError e) { v = e.toValue(); }
                out.set(r, c, v);
            }
        if (rows == 1 && cols == 1) return out;
        boolean nested = false;
        for (int r = 0; r < rows && !nested; r++) for (int c = 0; c < cols; c++) {
            FormulaNode n = a.rows().get(r).get(c);
            if (!(n instanceof NumberNode || n instanceof StringNode || n instanceof BoolNode || n instanceof ErrorNode || n instanceof UnaryNode u && u.operand() instanceof NumberNode)) { nested = true; break; }
        }
        if (nested) return stack(a);
        return out;
    }

    private CellValue stack(ArrayNode a) {
        List<List<ArrayValue>> blocks = new ArrayList<>();
        for (List<FormulaNode> rowNodes : a.rows()) {
            List<ArrayValue> row = new ArrayList<>();
            for (FormulaNode n : rowNodes) row.add(toArray(evaluate(n)));
            blocks.add(row);
        }
        int totalRows = 0, width = 0;
        for (List<ArrayValue> row : blocks) {
            int h = 0, w = 0;
            for (ArrayValue v : row) { h = Math.max(h, v.rows()); w += v.columns(); }
            totalRows += h; width = Math.max(width, w);
        }
        ArrayValue out = ArrayValue.of(totalRows, width);
        int r0 = 0;
        for (List<ArrayValue> row : blocks) {
            int h = 0, c0 = 0;
            for (ArrayValue v : row) h = Math.max(h, v.rows());
            for (ArrayValue v : row) {
                for (int r = 0; r < h; r++) for (int c = 0; c < v.columns(); c++) out.set(r0 + r, c0 + c, r < v.rows() ? v.get(r, c) : CellValue.error(CellError.NA));
                c0 += v.columns();
            }
            for (int r = 0; r < h; r++) for (int c = c0; c < width; c++) out.set(r0 + r, c, CellValue.error(CellError.NA));
            r0 += h;
        }
        return out;
    }

    @Override public CellValue deref(CellValue value) {
        if (value instanceof ReferenceValue r) {
            if (!r.isSingleArea()) {
                if (r.is3D()) throw EvalError.value();
                return toArray(r);
            }
            CellRange a = r.range();
            if (a.isSingleCell()) return cell(r.sheet(), a.firstRow(), a.firstColumn());
            return toArray(r);
        }
        return value;
    }

    @Override public CellValue scalar(CellValue value) {
        CellValue v = deref(value);
        if (v instanceof ArrayValue a) return a.get(0, 0);
        return v;
    }

    @Override public ArrayValue toArray(CellValue value) {
        if (value instanceof ArrayValue a) return a;
        if (value instanceof ReferenceValue r) {
            if (r.is3D()) throw EvalError.value();
            if (r.areas().size() > 1) {
                List<CellValue> all = new ArrayList<>();
                for (CellRange area : r.areas()) all.addAll(toArray(ReferenceValue.of(r.sheet(), area)).list());
                return ArrayValue.column(all);
            }
            CellRange a = clip(r.sheet(), r.range());
            ArrayValue out = ArrayValue.of(a.rowCount(), a.columnCount());
            engine.fill(r.sheet(), a, out);
            return out;
        }
        if (value instanceof OmittedValue) return new ArrayValue(1, 1, new CellValue[]{CellValue.EMPTY});
        return new ArrayValue(1, 1, new CellValue[]{value});
    }

    @Override public CellRange clip(int sheetIndex, CellRange range) {
        if (range.rowCount() * (long) range.columnCount() <= 1_000_000L && !range.isWholeColumn() && !range.isWholeRow()) return range;
        CellRange used = engine.extent(sheetIndex);
        if (used == null) return new CellRange(range.firstRow(), range.firstColumn(), range.firstRow(), range.firstColumn());
        int lastRow = Math.max(range.firstRow(), Math.min(range.lastRow(), used.lastRow()));
        int lastCol = Math.max(range.firstColumn(), Math.min(range.lastColumn(), used.lastColumn()));
        return new CellRange(range.firstRow(), range.firstColumn(), lastRow, lastCol);
    }

    @Override public CellValue cell(int sheetIndex, int r, int c) { return engine.valueForEvaluation(sheetIndex, r, c); }

    @Override public void forEachCell(ReferenceValue reference, CellConsumer consumer) {
        for (int s = reference.sheet(); s <= reference.sheetEnd(); s++)
            for (CellRange area : reference.areas()) engine.forEachValue(s, area, consumer);
    }

    @Override public CellRange usedRange(int sheetIndex) { return engine.extent(sheetIndex); }
    @Override public int hostSheet() { return sheet; }
    @Override public CellAddress host() { return new CellAddress(row, column); }
    @Override public SheetWorkbook workbook() { return engine.workbook(); }
    @Override public boolean date1904() { return engine.workbook().properties().date1904(); }
    @Override public FormulaLocale formulaLocale() { return engine.formulaLocale(); }
    @Override public Locale locale() { return engine.locale(); }
    @Override public FunctionRegistry functions() { return engine.functions(); }
    @Override public double random() { return engine.random(); }
    @Override public long now() { return engine.now(); }
    @Override public Map<String, CellValue> bindings() { return bindings; }
    @Override public int sheetIndex(String name) { return engine.workbook().indexOf(name); }
    @Override public String sheetName(int index) { return engine.workbook().sheet(index).name(); }
    @Override public SheetExternalDataProvider external() { return engine.external(); }
    @Override public String formatNumber(double value, String format) { return engine.formatter().text(CellValue.of(value), format); }

    @Override public String numberFormatAt(int s, int r, int c) {
        SheetCell cell = engine.workbook().sheet(s).cell(r, c);
        return engine.workbook().style(cell.style()).numberFormat();
    }

    @Override public Optional<String> formulaAt(int s, int r, int c) {
        SheetCell cell = engine.workbook().sheet(s).cells().get(r, c);
        return cell != null && cell.hasFormula() ? Optional.of(cell.formula()) : Optional.empty();
    }

    @Override public boolean isHiddenRow(int s, int r) { return engine.workbook().sheet(s).rows().isHidden(r); }

    @Override public boolean isSubtotalCell(int s, int r, int c) {
        Optional<String> f = formulaAt(s, r, c);
        if (f.isEmpty()) return false;
        String u = f.get().toUpperCase(Locale.ROOT);
        return u.contains("SUBTOTAL(") || u.contains("AGGREGATE(");
    }

    @Override public ReferenceValue indirect(String text, boolean a1) {
        String t = text.strip();
        if (t.startsWith("=")) t = t.substring(1);
        FormulaNode node;
        try { node = FormulaParser.parse(t, FormulaLocale.EN, new CellAddress(row, column), !a1); }
        catch (RuntimeException e) {
            try { node = FormulaParser.parse(t, engine.formulaLocale(), new CellAddress(row, column), !a1); } catch (RuntimeException e2) { throw EvalError.ref(); }
        }
        CellValue v = evaluate(node);
        if (v instanceof ReferenceValue r) return r;
        throw EvalError.ref();
    }

    static double serialNow(long millis, boolean date1904) {
        java.time.LocalDateTime now = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), java.time.ZoneId.systemDefault());
        return DateSerial.toSerial(now, date1904);
    }
}
