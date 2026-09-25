package dtm.stools.component.panels.editor.sheet.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public record NumberValue(double value) implements CellValue {
    public NumberValue {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non finite number");
        if (value == 0) value = 0;
    }

    public static double round15(double v) {
        if (v == 0 || !Double.isFinite(v)) return v;
        return new BigDecimal(v).round(new MathContext(15, RoundingMode.HALF_EVEN)).doubleValue();
    }

    public static String general(double v) {
        if (v == 0) return "0";
        double a = Math.abs(v);
        if (a >= 1e11 || a < 1e-9) {
            BigDecimal b = new BigDecimal(v).round(new MathContext(6, RoundingMode.HALF_UP));
            String s = String.format(java.util.Locale.ROOT, "%.5E", b.doubleValue());
            int e = s.indexOf('E');
            String mantissa = s.substring(0, e).replaceAll("0+$", "").replaceAll("\\.$", "");
            int exp = Integer.parseInt(s.substring(e + 1));
            return mantissa + "E" + (exp < 0 ? "-" : "+") + (Math.abs(exp) < 10 ? "0" : "") + Math.abs(exp);
        }
        BigDecimal b = new BigDecimal(v).round(new MathContext(a >= 1 ? 11 : 10, RoundingMode.HALF_UP)).stripTrailingZeros();
        String s = b.toPlainString();
        if (s.replace("-", "").replace(".", "").length() > 11 && s.contains(".")) {
            int intDigits = s.indexOf('.') - (s.startsWith("-") ? 1 : 0);
            int scale = Math.max(0, 10 - intDigits);
            s = b.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        return s;
    }

    @Override public String toString() { return general(value); }
}
