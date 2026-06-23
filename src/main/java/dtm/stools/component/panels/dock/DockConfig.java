package dtm.stools.component.panels.dock;

import dtm.stools.component.panels.tab.TabHeaderFactory;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Predicate;

@Getter
public class DockConfig {
    private final String key;
    private final String title;
    private final Component component;
    private DockRegion region = DockRegion.CENTER;
    private Icon icon;
    private Icon selectedIcon;
    private boolean closable = true;
    private String tooltip;
    private String badge;
    private TabHeaderFactory headerFactory;
    private Component dragHandle;
    private String dragHandleName;
    private boolean pinned;
    private boolean dirty;
    private boolean tabHeaderVisible = true;
    private boolean draggable = true;
    private Dimension preferredSize;
    private EnumSet<DockRegion> allowedDropRegions = EnumSet.allOf(DockRegion.class);
    private Predicate<DockDropContext> dropPredicate;

    public DockConfig(String title, Component component) {
        this(null, title, component);
    }

    public DockConfig(String key, String title, Component component) {
        this.key = key;
        this.title = title;
        this.component = component;
    }

    public DockConfig region(DockRegion region) {
        this.region = region == null ? DockRegion.CENTER : region;
        return this;
    }

    public DockConfig center() {
        return region(DockRegion.CENTER);
    }

    public DockConfig left() {
        return region(DockRegion.LEFT);
    }

    public DockConfig right() {
        return region(DockRegion.RIGHT);
    }

    public DockConfig top() {
        return region(DockRegion.TOP);
    }

    public DockConfig bottom() {
        return region(DockRegion.BOTTOM);
    }

    public DockConfig topLeft() {
        return region(DockRegion.TOP_LEFT);
    }

    public DockConfig bottomLeft() {
        return region(DockRegion.BOTTOM_LEFT);
    }

    public DockConfig topRight() {
        return region(DockRegion.TOP_RIGHT);
    }

    public DockConfig bottomRight() {
        return region(DockRegion.BOTTOM_RIGHT);
    }

    public DockConfig icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public DockConfig selectedIcon(Icon selectedIcon) {
        this.selectedIcon = selectedIcon;
        return this;
    }

    public DockConfig icons(Icon icon, Icon selectedIcon) {
        this.icon = icon;
        this.selectedIcon = selectedIcon;
        return this;
    }

    public DockConfig closable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public DockConfig tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public DockConfig badge(String badge) {
        this.badge = badge;
        return this;
    }

    public DockConfig headerFactory(TabHeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
        return this;
    }

    public DockConfig dragHandle(Component dragHandle) {
        this.dragHandle = dragHandle;
        return this;
    }

    public DockConfig dragHandleName(String dragHandleName) {
        this.dragHandleName = dragHandleName;
        return this;
    }

    public DockConfig pinned(boolean pinned) {
        this.pinned = pinned;
        return this;
    }

    public DockConfig dirty(boolean dirty) {
        this.dirty = dirty;
        return this;
    }

    public DockConfig tabHeaderVisible(boolean tabHeaderVisible) {
        this.tabHeaderVisible = tabHeaderVisible;
        return this;
    }

    public DockConfig draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public DockConfig preferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize == null ? null : new Dimension(preferredSize);
        return this;
    }

    public DockConfig allowedDropRegions(DockRegion first, DockRegion... regions) {
        Objects.requireNonNull(first, "first");
        this.allowedDropRegions = EnumSet.of(first, regions);
        return this;
    }

    public Dimension getPreferredSize() {
        return preferredSize == null ? null : new Dimension(preferredSize);
    }

    public DockConfig allowedDropRegions(Collection<DockRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            this.allowedDropRegions = EnumSet.noneOf(DockRegion.class);
            return this;
        }
        this.allowedDropRegions = EnumSet.copyOf(regions);
        return this;
    }

    public DockConfig dropPredicate(Predicate<DockDropContext> dropPredicate) {
        this.dropPredicate = dropPredicate;
        return this;
    }

    public EnumSet<DockRegion> getAllowedDropRegions() {
        return allowedDropRegions.clone();
    }
}
