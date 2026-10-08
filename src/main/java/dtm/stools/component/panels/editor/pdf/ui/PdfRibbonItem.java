package dtm.stools.component.panels.editor.pdf.ui;

import javax.swing.JComponent;
import java.util.function.Supplier;

public record PdfRibbonItem(String command, boolean large, Supplier<JComponent> custom) {
    public static PdfRibbonItem large(String command) { return new PdfRibbonItem(command, true, null); }
    public static PdfRibbonItem small(String command) { return new PdfRibbonItem(command, false, null); }
    public static PdfRibbonItem component(Supplier<JComponent> factory) { return new PdfRibbonItem(null, false, factory); }
}
