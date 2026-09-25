package dtm.stools.component.panels.editor.sheet.ui;

import javax.swing.JComponent;
import java.util.function.Supplier;

public record RibbonItem(String command, boolean large, Supplier<JComponent> custom) {
    public static RibbonItem large(String command) { return new RibbonItem(command, true, null); }
    public static RibbonItem small(String command) { return new RibbonItem(command, false, null); }
    public static RibbonItem component(Supplier<JComponent> factory) { return new RibbonItem(null, false, factory); }
}
