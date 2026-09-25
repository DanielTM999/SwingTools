package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record Threshold(ThresholdType type, String value, boolean greaterOrEqual) {
    public Threshold { Objects.requireNonNull(type); value = Objects.requireNonNullElse(value, ""); }

    public static Threshold min() { return new Threshold(ThresholdType.MIN, "", true); }
    public static Threshold max() { return new Threshold(ThresholdType.MAX, "", true); }
    public static Threshold percentile(double p) { return new Threshold(ThresholdType.PERCENTILE, trim(p), true); }
    public static Threshold percent(double p) { return new Threshold(ThresholdType.PERCENT, trim(p), true); }
    public static Threshold number(double v) { return new Threshold(ThresholdType.NUMBER, trim(v), true); }

    private static String trim(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v); }
}
