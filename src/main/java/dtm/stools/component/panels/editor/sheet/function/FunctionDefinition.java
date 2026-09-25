package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class FunctionDefinition implements SheetFunction {
    private final String name;
    private final FunctionCategory category;
    private final int min, max;
    private final RawBody raw;
    private final ScalarBody scalar;
    private final long liftMask;
    private final boolean volatileFlag, returnsReference;
    private final FunctionOrigin origin;
    private final String description;
    private final List<String> parameters;

    private FunctionDefinition(String name, FunctionCategory category, int min, int max, RawBody raw, ScalarBody scalar, long liftMask, boolean volatileFlag,
                               boolean returnsReference, FunctionOrigin origin, String description, List<String> parameters) {
        this.name = name.toUpperCase(Locale.ROOT);
        this.category = Objects.requireNonNull(category);
        this.min = min; this.max = max < 0 ? 255 : max;
        this.raw = raw; this.scalar = scalar; this.liftMask = liftMask;
        this.volatileFlag = volatileFlag; this.returnsReference = returnsReference;
        this.origin = origin; this.description = description == null ? "" : description; this.parameters = List.copyOf(parameters);
    }

    public static Builder raw(String name, FunctionCategory category, int min, int max, RawBody body) { return new Builder(name, category, min, max).raw(body); }
    public static Builder scalar(String name, FunctionCategory category, int min, int max, ScalarBody body) { return new Builder(name, category, min, max).scalar(body); }

    @Override public String name() { return name; }
    @Override public FunctionCategory category() { return category; }
    @Override public int minArgs() { return min; }
    @Override public int maxArgs() { return max; }
    @Override public boolean isVolatile() { return volatileFlag; }
    @Override public boolean returnsReference() { return returnsReference; }
    @Override public FunctionOrigin origin() { return origin; }
    @Override public String description() { return description; }
    @Override public List<String> parameters() { return parameters; }

    @Override
    public CellValue call(FunctionContext context, FunctionArgs args) {
        if (raw != null) return raw.apply(context, args);
        int n = args.size();
        CellValue[] values = new CellValue[n];
        int rows = 1, cols = 1;
        boolean lifted = false;
        for (int i = 0; i < n; i++) {
            CellValue v = args.value(i);
            boolean lift = (liftMask & (1L << Math.min(i, 63))) != 0;
            if (lift) {
                v = context.deref(v);
                if (v instanceof ArrayValue a) {
                    if (a.size() == 1) v = a.get(0, 0);
                    else { lifted = true; rows = Math.max(rows, a.rows()); cols = Math.max(cols, a.columns()); }
                }
            }
            values[i] = v;
        }
        if (!lifted) return invoke(context, values);
        ArrayValue out = ArrayValue.of(rows, cols);
        CellValue[] element = new CellValue[n];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                for (int i = 0; i < n; i++) element[i] = values[i] instanceof ArrayValue a && (liftMask & (1L << Math.min(i, 63))) != 0 ? a.broadcast(r, c) : values[i];
                out.set(r, c, invoke(context, element.clone()));
            }
        return out;
    }

    private CellValue invoke(FunctionContext context, CellValue[] values) {
        try {
            for (CellValue v : values) if (v instanceof ErrorValue e && propagate()) return e;
            CellValue result = scalar.apply(context, values);
            return result == null ? CellValue.EMPTY : result;
        } catch (EvalError e) {
            return e.toValue();
        } catch (ArithmeticException e) {
            return CellValue.error(CellError.NUM);
        }
    }

    private boolean propagate() { return !name.startsWith("IS") && !name.equals("IFERROR") && !name.equals("IFNA") && !name.equals("ERROR.TYPE") && !name.equals("TYPE") && !name.equals("N") && !name.equals("T"); }

    public static CellValue arg(CellValue[] a, int i) { return i < a.length ? a[i] : OmittedValue.INSTANCE; }
    public static boolean given(CellValue[] a, int i) { return i < a.length && !(a[i] instanceof OmittedValue); }

    public static final class Builder {
        private final String name;
        private final FunctionCategory category;
        private final int min, max;
        private RawBody raw;
        private ScalarBody scalar;
        private long liftMask = -1L;
        private boolean volatileFlag, returnsReference;
        private FunctionOrigin origin = FunctionOrigin.EXCEL;
        private String description = "";
        private List<String> parameters = List.of();

        private Builder(String name, FunctionCategory category, int min, int max) { this.name = name; this.category = category; this.min = min; this.max = max; }

        public Builder raw(RawBody b) { raw = b; return this; }
        public Builder scalar(ScalarBody b) { scalar = b; return this; }
        public Builder lift(int... indexes) { liftMask = 0; for (int i : indexes) liftMask |= 1L << i; return this; }
        public Builder liftNone() { liftMask = 0; return this; }
        public Builder volatileFunction() { volatileFlag = true; return this; }
        public Builder reference() { returnsReference = true; return this; }
        public Builder origin(FunctionOrigin o) { origin = o; return this; }
        public Builder google() { origin = FunctionOrigin.GOOGLE; return this; }
        public Builder modern() { origin = FunctionOrigin.EXCEL_365; return this; }
        public Builder describe(String d, String... params) { description = d; parameters = List.of(params); return this; }
        public FunctionDefinition build() { return new FunctionDefinition(name, category, min, max, raw, scalar, liftMask, volatileFlag, returnsReference, origin, description, parameters); }
    }
}
