package dtm.stools.component.panels.tab;

@FunctionalInterface
public interface TabGroupFactory {
    TabbedPanel createGroup(TabbedPanel source, TabEntry entry, TabSplitPlacement placement);
}
