package dtm.stools.component.panels.editor.sheet.model;

public enum ConditionalRuleType {
    CELL_VALUE("cellIs"), EXPRESSION("expression"), COLOR_SCALE("colorScale"), DATA_BAR("dataBar"), ICON_SET("iconSet"), TOP_BOTTOM("top10"),
    ABOVE_AVERAGE("aboveAverage"), DUPLICATE("duplicateValues"), UNIQUE("uniqueValues"), CONTAINS_TEXT("containsText"), NOT_CONTAINS_TEXT("notContainsText"),
    BEGINS_WITH("beginsWith"), ENDS_WITH("endsWith"), BLANKS("containsBlanks"), NO_BLANKS("notContainsBlanks"), ERRORS("containsErrors"),
    NO_ERRORS("notContainsErrors"), TIME_PERIOD("timePeriod");

    private final String xml;
    ConditionalRuleType(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static ConditionalRuleType fromXml(String v) { for (ConditionalRuleType t : values()) if (t.xml.equals(v)) return t; return EXPRESSION; }
}
