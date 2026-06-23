package dtm.stools.component.panels.dock;

import dtm.stools.component.panels.tab.TabbedPanel;

@FunctionalInterface
public interface DockGroupFactory {
    TabbedPanel createGroup(DockPanel dockPanel, DockRegion region);
}
