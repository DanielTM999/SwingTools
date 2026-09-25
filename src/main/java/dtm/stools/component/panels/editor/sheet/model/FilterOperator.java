package dtm.stools.component.panels.editor.sheet.model;

public enum FilterOperator {
    EQUAL("equal"), NOT_EQUAL("notEqual"), GREATER("greaterThan"), GREATER_OR_EQUAL("greaterThanOrEqual"), LESS("lessThan"), LESS_OR_EQUAL("lessThanOrEqual"),
    BEGINS_WITH("beginsWith"), ENDS_WITH("endsWith"), CONTAINS("contains"), NOT_CONTAINS("notContains");

    private final String xml;
    FilterOperator(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static FilterOperator fromXml(String v) { for (FilterOperator o : values()) if (o.xml.equals(v)) return o; return EQUAL; }
}
