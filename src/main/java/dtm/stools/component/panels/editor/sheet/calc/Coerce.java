package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.Locale;

public final class Coerce {
    private Coerce() {}

    public static double number(CellValue v) {
        return switch (v) {
            case NumberValue n -> n.value();
            case EmptyValue e -> 0;
            case OmittedValue o -> 0;
            case BoolValue b -> b.value() ? 1 : 0;
            case TextValue t -> parseNumber(t.value());
            case ErrorValue e -> throw EvalError.of(e.error());
            case ArrayValue a -> number(a.get(0, 0));
            default -> throw EvalError.value();
        };
    }

    public static double parseNumber(String text) {
        Double d = ValueParser.parseNumberLenient(text);
        if (d == null) throw EvalError.value();
        return d;
    }

    public static Double tryNumber(CellValue v) {
        try { return number(v); } catch (EvalError e) { return null; }
    }

    public static String text(CellValue v) {
        return switch (v) {
            case TextValue t -> t.value();
            case NumberValue n -> NumberValue.general(n.value());
            case EmptyValue e -> "";
            case OmittedValue o -> "";
            case BoolValue b -> b.value() ? "TRUE" : "FALSE";
            case ErrorValue e -> throw EvalError.of(e.error());
            case ArrayValue a -> text(a.get(0, 0));
            default -> throw EvalError.value();
        };
    }

    public static boolean bool(CellValue v) {
        return switch (v) {
            case BoolValue b -> b.value();
            case NumberValue n -> n.value() != 0;
            case EmptyValue e -> false;
            case OmittedValue o -> false;
            case TextValue t -> {
                String u = t.value().strip().toUpperCase(Locale.ROOT);
                if (u.equals("TRUE") || u.equals("VERDADEIRO")) yield true;
                if (u.equals("FALSE") || u.equals("FALSO")) yield false;
                throw EvalError.value();
            }
            case ErrorValue e -> throw EvalError.of(e.error());
            case ArrayValue a -> bool(a.get(0, 0));
            default -> throw EvalError.value();
        };
    }

    public static void check(CellValue v) { if (v instanceof ErrorValue e) throw EvalError.of(e.error()); }

    public static int typeRank(CellValue v) {
        return switch (v) {
            case NumberValue n -> 1;
            case TextValue t -> 2;
            case BoolValue b -> 3;
            case ErrorValue e -> 4;
            default -> 0;
        };
    }

    public static int compare(CellValue a, CellValue b) { return compare(a, b, false); }

    public static int compare(CellValue a, CellValue b, boolean caseSensitive) {
        if (a instanceof ErrorValue e) throw EvalError.of(e.error());
        if (b instanceof ErrorValue e) throw EvalError.of(e.error());
        if (a.isEmpty() || a instanceof OmittedValue) a = emptyAs(b);
        if (b.isEmpty() || b instanceof OmittedValue) b = emptyAs(a);
        int ra = typeRank(a), rb = typeRank(b);
        if (ra != rb) return Integer.compare(ra, rb);
        return switch (a) {
            case NumberValue n -> Double.compare(NumberValue.round15(n.value()), NumberValue.round15(((NumberValue) b).value()));
            case TextValue t -> caseSensitive ? t.value().compareTo(((TextValue) b).value()) : compareText(t.value(), ((TextValue) b).value());
            case BoolValue bo -> Boolean.compare(bo.value(), ((BoolValue) b).value());
            default -> 0;
        };
    }

    public static int compareText(String a, String b) {
        return java.text.Collator.getInstance(Locale.ROOT).compare(a.toLowerCase(Locale.ROOT), b.toLowerCase(Locale.ROOT)) ;
    }

    private static CellValue emptyAs(CellValue other) {
        return switch (other) {
            case TextValue t -> new TextValue("");
            case BoolValue b -> CellValue.FALSE;
            default -> CellValue.ZERO;
        };
    }

    public static boolean equalsValue(CellValue a, CellValue b) {
        try { return compare(a, b) == 0; } catch (EvalError e) { return false; }
    }

    public static CellValue result(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return CellValue.error(dtm.stools.component.panels.editor.sheet.model.CellError.NUM);
        return new NumberValue(v);
    }

    public static double checked(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw EvalError.num();
        return v;
    }

    public static int integer(CellValue v) {
        double d = number(v);
        if (d >= Integer.MAX_VALUE || d <= Integer.MIN_VALUE) throw EvalError.num();
        return (int) Math.floor(d);
    }

    public static int truncate(CellValue v) {
        double d = number(v);
        if (d >= Integer.MAX_VALUE || d <= Integer.MIN_VALUE) throw EvalError.num();
        return (int) d;
    }
}
