package dtm.stools.component.panels.dock;

/**
 * Defines how docks that target the same region are presented.
 */
public enum DockRegionLayout {
    /** Keeps only the most recently added dock in the region. */
    SINGLE,
    /** Places all docks in one tab group. */
    TABS,
    /** Shows every dock at the same time, separated by resizable splitters. */
    SPLIT
}
