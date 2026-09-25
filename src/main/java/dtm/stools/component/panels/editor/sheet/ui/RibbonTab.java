package dtm.stools.component.panels.editor.sheet.ui;

import java.util.List;

public record RibbonTab(String id, String title, List<RibbonGroup> groups, boolean contextual) {
    public RibbonTab { groups = List.copyOf(groups); }
}
