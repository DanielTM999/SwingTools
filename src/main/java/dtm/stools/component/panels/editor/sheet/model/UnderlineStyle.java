package dtm.stools.component.panels.editor.sheet.model;

public enum UnderlineStyle {
    NONE("none"), SINGLE("single"), DOUBLE("double"), SINGLE_ACCOUNTING("singleAccounting"), DOUBLE_ACCOUNTING("doubleAccounting");

    private final String xml;
    UnderlineStyle(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static UnderlineStyle fromXml(String v) { if (v == null) return SINGLE; for (UnderlineStyle u : values()) if (u.xml.equals(v)) return u; return SINGLE; }
}
