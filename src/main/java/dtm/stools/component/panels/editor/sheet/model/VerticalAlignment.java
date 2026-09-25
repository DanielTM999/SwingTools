package dtm.stools.component.panels.editor.sheet.model;

public enum VerticalAlignment {
    TOP("top"), CENTER("center"), BOTTOM("bottom"), JUSTIFY("justify"), DISTRIBUTED("distributed");

    private final String xml;
    VerticalAlignment(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static VerticalAlignment fromXml(String v) { for (VerticalAlignment a : values()) if (a.xml.equals(v)) return a; return BOTTOM; }
}
