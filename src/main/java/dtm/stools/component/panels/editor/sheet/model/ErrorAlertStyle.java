package dtm.stools.component.panels.editor.sheet.model;

public enum ErrorAlertStyle {
    STOP("stop"), WARNING("warning"), INFORMATION("information");

    private final String xml;
    ErrorAlertStyle(String xml) { this.xml = xml; }
    public String xml() { return xml; }
    public static ErrorAlertStyle fromXml(String v) { for (ErrorAlertStyle s : values()) if (s.xml.equals(v)) return s; return STOP; }
}
