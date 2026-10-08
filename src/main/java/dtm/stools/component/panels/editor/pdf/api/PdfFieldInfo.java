package dtm.stools.component.panels.editor.pdf.api;

import java.util.List;

public record PdfFieldInfo(String name, PdfFieldKind kind, String value, List<String> options, String widgetValue,
                           boolean required, boolean readOnly, boolean multiline, float fontSize) {
    public PdfFieldInfo {
        value = value == null ? "" : value;
        options = List.copyOf(options);
        widgetValue = widgetValue == null ? "" : widgetValue;
    }
}
