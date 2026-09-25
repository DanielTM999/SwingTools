package dtm.stools.component.panels.editor.sheet.ui;

import java.util.List;

public record RibbonGroup(String id, String title, int priority, String icon, List<RibbonItem> items) {
    public RibbonGroup { items = List.copyOf(items); }
}
