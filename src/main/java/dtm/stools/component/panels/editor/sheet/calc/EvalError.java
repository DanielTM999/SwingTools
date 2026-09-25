package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;

import java.util.EnumMap;
import java.util.Map;

public final class EvalError extends RuntimeException {
    private static final Map<CellError, EvalError> CACHE = new EnumMap<>(CellError.class);
    static { for (CellError e : CellError.values()) CACHE.put(e, new EvalError(e)); }

    private final CellError error;

    private EvalError(CellError error) { super(error.text(), null, false, false); this.error = error; }

    public static EvalError of(CellError error) { return CACHE.get(error); }
    public static EvalError value() { return CACHE.get(CellError.VALUE); }
    public static EvalError na() { return CACHE.get(CellError.NA); }
    public static EvalError num() { return CACHE.get(CellError.NUM); }
    public static EvalError div0() { return CACHE.get(CellError.DIV0); }
    public static EvalError ref() { return CACHE.get(CellError.REF); }
    public static EvalError name() { return CACHE.get(CellError.NAME); }
    public static EvalError calc() { return CACHE.get(CellError.CALC); }

    public CellError error() { return error; }
    public CellValue toValue() { return ErrorValue.of(error); }
}
