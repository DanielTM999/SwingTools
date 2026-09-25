package dtm.stools.component.panels.editor.sheet.model;

public interface CellValue {
    CellValue EMPTY = EmptyValue.INSTANCE;
    CellValue TRUE = new BoolValue(true);
    CellValue FALSE = new BoolValue(false);
    CellValue ZERO = new NumberValue(0);

    static CellValue of(double value) { return Double.isFinite(value) ? new NumberValue(value) : new ErrorValue(CellError.NUM); }
    static CellValue of(String value) { return value == null ? EMPTY : new TextValue(value); }
    static CellValue of(boolean value) { return value ? TRUE : FALSE; }
    static CellValue error(CellError error) { return ErrorValue.of(error); }

    static CellValue from(Object value) {
        if (value == null) return EMPTY;
        if (value instanceof CellValue v) return v;
        if (value instanceof Number n) return of(n.doubleValue());
        if (value instanceof Boolean b) return of(b.booleanValue());
        if (value instanceof CellError e) return error(e);
        return of(String.valueOf(value));
    }

    default boolean isEmpty() { return this instanceof EmptyValue; }
    default boolean isNumber() { return this instanceof NumberValue; }
    default boolean isText() { return this instanceof TextValue; }
    default boolean isBoolean() { return this instanceof BoolValue; }
    default boolean isError() { return this instanceof ErrorValue; }
    default boolean isArray() { return this instanceof ArrayValue; }

    default String display() {
        return switch (this) {
            case EmptyValue e -> "";
            case NumberValue n -> NumberValue.general(n.value());
            case TextValue t -> t.value();
            case BoolValue b -> b.value() ? "TRUE" : "FALSE";
            case ErrorValue e -> e.error().text();
            case ArrayValue a -> a.get(0, 0).display();
            default -> toString();
        };
    }

    default Object raw() {
        return switch (this) {
            case EmptyValue e -> null;
            case NumberValue n -> n.value();
            case TextValue t -> t.value();
            case BoolValue b -> b.value();
            case ErrorValue e -> e.error();
            default -> this;
        };
    }
}
