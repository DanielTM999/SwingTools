package dtm.stools.component.panels.editor.sheet.provider;

import java.awt.Component;
import java.util.List;
import java.util.Locale;

public interface SheetCommandPaletteContext {
    Component owner();
    Locale locale();
    List<SheetCommandEntry> commands();
    boolean execute(String id);
}
