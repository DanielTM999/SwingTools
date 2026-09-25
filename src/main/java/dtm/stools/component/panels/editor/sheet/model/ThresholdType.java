package dtm.stools.component.panels.editor.sheet.model;

public enum ThresholdType {
    MIN("min"), MAX("max"), NUMBER("num"), PERCENT("percent"), PERCENTILE("percentile"), FORMULA("formula"), AUTO_MIN("autoMin"), AUTO_MAX("autoMax");

    private final String xml;
    ThresholdType(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static ThresholdType fromXml(String v) { for (ThresholdType t : values()) if (t.xml.equals(v)) return t; return NUMBER; }
}
