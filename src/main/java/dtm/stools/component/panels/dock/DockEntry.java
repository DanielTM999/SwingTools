package dtm.stools.component.panels.dock;

import dtm.stools.component.panels.tab.TabEntry;
import dtm.stools.component.panels.tab.TabHeaderFactory;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.function.Predicate;

public class DockEntry {
    private final String key;
    private final Component component;
    private String title;
    private DockRegion region;
    private Icon icon;
    private Icon selectedIcon;
    private boolean closable;
    private String tooltip;
    private String badge;
    private TabHeaderFactory headerFactory;
    private Component dragHandle;
    private String dragHandleName;
    private boolean pinned;
    private boolean dirty;
    private boolean tabHeaderVisible;
    private boolean draggable;
    private Dimension preferredSize;
    private EnumSet<DockRegion> allowedDropRegions;
    private Predicate<DockDropContext> dropPredicate;

    DockEntry(String key, DockConfig config) {
        this.key = key;
        this.title = config.getTitle();
        this.component = config.getComponent();
        this.region = config.getRegion();
        this.icon = config.getIcon();
        this.selectedIcon = config.getSelectedIcon();
        this.closable = config.isClosable();
        this.tooltip = config.getTooltip();
        this.badge = config.getBadge();
        this.headerFactory = config.getHeaderFactory();
        this.dragHandle = config.getDragHandle();
        this.dragHandleName = config.getDragHandleName();
        this.pinned = config.isPinned();
        this.dirty = config.isDirty();
        this.tabHeaderVisible = config.isTabHeaderVisible();
        this.draggable = config.isDraggable();
        this.preferredSize = config.getPreferredSize();
        this.allowedDropRegions = config.getAllowedDropRegions();
        this.dropPredicate = config.getDropPredicate();
    }

    void updateFrom(TabEntry tab, DockRegion region) {
        if (tab == null) return;
        this.title = tab.getTitle();
        this.icon = tab.getIcon();
        this.selectedIcon = tab.getSelectedIcon();
        this.closable = tab.isClosable();
        this.tooltip = tab.getTooltip();
        this.badge = tab.getBadge();
        this.pinned = tab.isPinned();
        this.dirty = tab.isDirty();
        this.region = region;
    }

    void setRegion(DockRegion region) {
        this.region = region;
    }

    void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize == null ? null : new Dimension(preferredSize);
    }

    public String getKey() {
        return key;
    }

    public Component getComponent() {
        return component;
    }

    public String getTitle() {
        return title;
    }

    public DockRegion getRegion() {
        return region;
    }

    public Icon getIcon() {
        return icon;
    }

    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    public boolean isClosable() {
        return closable;
    }

    public String getTooltip() {
        return tooltip;
    }

    public String getBadge() {
        return badge;
    }

    public TabHeaderFactory getHeaderFactory() {
        return headerFactory;
    }

    public Component getDragHandle() {
        return dragHandle;
    }

    public String getDragHandleName() {
        return dragHandleName;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isTabHeaderVisible() {
        return tabHeaderVisible;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public Dimension getPreferredSize() {
        return preferredSize == null ? null : new Dimension(preferredSize);
    }

    public boolean canDropTo(DockRegion region) {
        return region != null && allowedDropRegions.contains(region);
    }

    public EnumSet<DockRegion> getAllowedDropRegions() {
        return allowedDropRegions.clone();
    }

    public Predicate<DockDropContext> getDropPredicate() {
        return dropPredicate;
    }
}
