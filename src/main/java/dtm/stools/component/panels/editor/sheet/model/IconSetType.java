package dtm.stools.component.panels.editor.sheet.model;

public enum IconSetType {
    ARROWS_3("3Arrows", 3), ARROWS_GRAY_3("3ArrowsGray", 3), FLAGS_3("3Flags", 3), TRAFFIC_LIGHTS_3("3TrafficLights1", 3), TRAFFIC_LIGHTS_RIMMED_3("3TrafficLights2", 3),
    SIGNS_3("3Signs", 3), SYMBOLS_3("3Symbols", 3), SYMBOLS_CIRCLED_3("3Symbols2", 3), STARS_3("3Stars", 3), TRIANGLES_3("3Triangles", 3),
    ARROWS_4("4Arrows", 4), ARROWS_GRAY_4("4ArrowsGray", 4), RED_TO_BLACK_4("4RedToBlack", 4), RATING_4("4Rating", 4), TRAFFIC_LIGHTS_4("4TrafficLights", 4),
    ARROWS_5("5Arrows", 5), ARROWS_GRAY_5("5ArrowsGray", 5), RATING_5("5Rating", 5), QUARTERS_5("5Quarters", 5), BOXES_5("5Boxes", 5);

    private final String xml;
    private final int size;
    IconSetType(String xml, int size) { this.xml = xml; this.size = size; }
    public String xml() { return xml; }
    public int size() { return size; }
    public static IconSetType fromXml(String v) { for (IconSetType t : values()) if (t.xml.equals(v)) return t; return TRAFFIC_LIGHTS_3; }
}
