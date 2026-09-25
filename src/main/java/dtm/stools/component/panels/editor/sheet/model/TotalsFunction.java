package dtm.stools.component.panels.editor.sheet.model;

public enum TotalsFunction {
    NONE("none", 0), AVERAGE("average", 101), COUNT("countNums", 102), COUNTA("count", 103), MAX("max", 104), MIN("min", 105), STDDEV("stdDev", 107),
    SUM("sum", 109), VAR("var", 110), CUSTOM("custom", 0);

    private final String xml;
    private final int subtotal;
    TotalsFunction(String xml, int subtotal) { this.xml = xml; this.subtotal = subtotal; }
    public String xml() { return xml; }
    public int subtotal() { return subtotal; }
    public static TotalsFunction fromXml(String v) { for (TotalsFunction t : values()) if (t.xml.equals(v)) return t; return NONE; }
}
