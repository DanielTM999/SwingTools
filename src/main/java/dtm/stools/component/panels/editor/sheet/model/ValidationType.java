package dtm.stools.component.panels.editor.sheet.model;

public enum ValidationType {
    ANY("none"), WHOLE("whole"), DECIMAL("decimal"), LIST("list"), DATE("date"), TIME("time"), TEXT_LENGTH("textLength"), CUSTOM("custom"), CHECKBOX("checkbox");

    private final String xml;
    ValidationType(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static ValidationType fromXml(String v) { for (ValidationType t : values()) if (t.xml.equals(v)) return t; return ANY; }
}
