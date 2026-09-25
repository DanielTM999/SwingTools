package dtm.stools.component.panels.editor.sheet.model;

public enum HorizontalAlignment {
    GENERAL("general"), LEFT("left"), CENTER("center"), RIGHT("right"), FILL("fill"), JUSTIFY("justify"), CENTER_ACROSS("centerContinuous"), DISTRIBUTED("distributed");

    private final String xml;
    HorizontalAlignment(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static HorizontalAlignment fromXml(String v) { for (HorizontalAlignment a : values()) if (a.xml.equals(v)) return a; return GENERAL; }
}
