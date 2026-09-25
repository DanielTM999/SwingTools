package dtm.stools.component.panels.editor.sheet.model;

public enum SheetVisibility {
    VISIBLE("visible"), HIDDEN("hidden"), VERY_HIDDEN("veryHidden");

    private final String xml;
    SheetVisibility(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static SheetVisibility fromXml(String v) { for (SheetVisibility s : values()) if (s.xml.equals(v)) return s; return VISIBLE; }
}
