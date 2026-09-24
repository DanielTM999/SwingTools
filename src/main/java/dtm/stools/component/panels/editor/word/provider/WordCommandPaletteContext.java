package dtm.stools.component.panels.editor.word.provider;

import java.awt.Component;
import java.util.List;
import java.util.Locale;

public interface WordCommandPaletteContext {
    Component owner();
    Locale locale();
    List<WordCommandEntry> commands();
    boolean execute(String commandId);
    void closed();
}
