package dtm.stools.component.panels.editor.sheet.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record ErrorValue(CellError error) implements CellValue {
    private static final Map<CellError, ErrorValue> CACHE = new EnumMap<>(CellError.class);
    static { for (CellError e : CellError.values()) CACHE.put(e, new ErrorValue(e)); }

    public ErrorValue { Objects.requireNonNull(error); }

    public static ErrorValue of(CellError error) { ErrorValue v = CACHE.get(error); return v != null ? v : new ErrorValue(error); }

    @Override public String toString() { return error.text(); }
}
