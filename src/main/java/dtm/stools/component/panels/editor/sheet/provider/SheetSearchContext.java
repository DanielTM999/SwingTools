package dtm.stools.component.panels.editor.sheet.provider;

import java.awt.Component;
import java.util.List;

public interface SheetSearchContext {
    Component owner();
    List<SheetSearchHit> findAll(String query, SheetSearchOptions options);
    boolean select(SheetSearchHit hit);
    int replace(SheetSearchHit hit, String replacement, SheetSearchOptions options);
    int replaceAll(String query, String replacement, SheetSearchOptions options);
}
