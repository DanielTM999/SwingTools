package dtm.stools.component.panels.editor.sheet.model;

public enum ComparisonOperator {
    BETWEEN("between"), NOT_BETWEEN("notBetween"), EQUAL("equal"), NOT_EQUAL("notEqual"), GREATER("greaterThan"), LESS("lessThan"),
    GREATER_OR_EQUAL("greaterThanOrEqual"), LESS_OR_EQUAL("lessThanOrEqual");

    private final String xml;
    ComparisonOperator(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public boolean twoOperands() { return this == BETWEEN || this == NOT_BETWEEN; }
    public static ComparisonOperator fromXml(String v) { for (ComparisonOperator o : values()) if (o.xml.equals(v)) return o; return BETWEEN; }

    public boolean test(double v, double a, double b) {
        return switch (this) {
            case BETWEEN -> v >= Math.min(a, b) && v <= Math.max(a, b);
            case NOT_BETWEEN -> v < Math.min(a, b) || v > Math.max(a, b);
            case EQUAL -> v == a;
            case NOT_EQUAL -> v != a;
            case GREATER -> v > a;
            case LESS -> v < a;
            case GREATER_OR_EQUAL -> v >= a;
            case LESS_OR_EQUAL -> v <= a;
        };
    }
}
