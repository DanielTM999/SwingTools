package dtm.stools.component.panels.editor.sheet.model;

public enum FillPattern {
    NONE("none"), SOLID("solid"), GRAY_50("mediumGray"), GRAY_75("darkGray"), GRAY_25("lightGray"), GRAY_125("gray125"), GRAY_0625("gray0625"),
    HORIZONTAL("darkHorizontal"), VERTICAL("darkVertical"), DOWN("darkDown"), UP("darkUp"), GRID("darkGrid"), TRELLIS("darkTrellis"),
    THIN_HORIZONTAL("lightHorizontal"), THIN_VERTICAL("lightVertical"), THIN_DOWN("lightDown"), THIN_UP("lightUp"), THIN_GRID("lightGrid"), THIN_TRELLIS("lightTrellis");

    private final String xml;
    FillPattern(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static FillPattern fromXml(String v) { if (v == null) return NONE; for (FillPattern p : values()) if (p.xml.equals(v)) return p; return SOLID; }
}
