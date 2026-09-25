package dtm.stools.component.panels.editor.sheet.model;

public enum BorderStyle {
    NONE("none", 0f), HAIR("hair", .5f), THIN("thin", 1f), DOTTED("dotted", 1f), DASHED("dashed", 1f), DASH_DOT("dashDot", 1f), DASH_DOT_DOT("dashDotDot", 1f),
    MEDIUM("medium", 2f), MEDIUM_DASHED("mediumDashed", 2f), MEDIUM_DASH_DOT("mediumDashDot", 2f), MEDIUM_DASH_DOT_DOT("mediumDashDotDot", 2f),
    SLANT_DASH_DOT("slantDashDot", 2f), THICK("thick", 3f), DOUBLE("double", 3f);

    private final String xml;
    private final float width;
    BorderStyle(String xml, float width) { this.xml = xml; this.width = width; }
    public String xml() { return xml; }
    public float width() { return width; }
    public static BorderStyle fromXml(String v) { if (v == null) return NONE; for (BorderStyle b : values()) if (b.xml.equals(v)) return b; return THIN; }
}
