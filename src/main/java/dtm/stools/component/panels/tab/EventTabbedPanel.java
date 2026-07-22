package dtm.stools.component.panels.tab;

public final class EventTabbedPanel {
    public static final String TAB_ADD = "tabAdd";
    public static final String BEFORE_TAB_REMOVE = "beforeTabRemove";
    public static final String TAB_CLOSE = "tabClose";
    public static final String TAB_REMOVE = "tabRemove";
    public static final String BEFORE_TAB_MOVE = "beforeTabMove";
    public static final String TAB_MOVE = "tabMove";
    public static final String TAB_DRAG = "tabDrag";
    public static final String BEFORE_TAB_SPLIT = "beforeTabSplit";
    public static final String TAB_SPLIT = "tabSplit";
    public static final String BEFORE_TAB_TRANSFER = "beforeTabTransfer";
    public static final String TAB_TRANSFER = "tabTransfer";
    public static final String TAB_MENU = "tabMenu";
    public static final String TAB_TITLE_CHANGE = "tabTitleChange";
    public static final String TAB_ICON_CHANGE = "tabIconChange";
    public static final String TAB_TOOLTIP_CHANGE = "tabTooltipChange";
    public static final String TAB_CLOSABLE_CHANGE = "tabClosableChange";
    public static final String TAB_HEADER_FACTORY_CHANGE = "tabHeaderFactoryChange";
    public static final String TAB_BADGE_CHANGE = "tabBadgeChange";
    public static final String TAB_PINNED_CHANGE = "tabPinnedChange";
    public static final String TAB_DIRTY_CHANGE = "tabDirtyChange";
    public static final String BEFORE_TAB_CLOSE = "beforeTabClose";
    public static final String TAB_HEADER_HOVER = "tabHeaderHover";
    public static final String TAB_HEADER_HOVER_EXIT = "tabHeaderHoverExit";

    private EventTabbedPanel() { throw new IllegalArgumentException("EventTabbedPanel cannot be instantiated"); }
}
