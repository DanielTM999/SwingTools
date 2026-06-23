package dtm.stools.component.panels.tab;

import javax.swing.*;
import java.awt.*;

public class TabConfig {
    private final String key;
    private final String title;
    private final Component component;
    private Icon icon;
    private Color titleForeground;
    private Color selectedTitleForeground;
    private Icon selectedIcon;
    private boolean closable = true;
    private String tooltip;
    private String badge;
    private TabHeaderFactory headerFactory;
    private boolean pinned;
    private boolean dirty;

    public TabConfig(String title, Component component) {
        this(null, title, component);
    }

    public TabConfig(String key, String title, Component component) {
        this.key = key;
        this.title = title;
        this.component = component;
    }

    public TabConfig titleForeground(Color titleForeground) {
        this.titleForeground = titleForeground;
        return this;
    }

    public TabConfig selectedTitleForeground(Color selectedTitleForeground) {
        this.selectedTitleForeground = selectedTitleForeground;
        return this;
    }

    public TabConfig icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public TabConfig selectedIcon(Icon selectedIcon) {
        this.selectedIcon = selectedIcon;
        return this;
    }

    public TabConfig icons(Icon icon, Icon selectedIcon) {
        this.icon = icon;
        this.selectedIcon = selectedIcon;
        return this;
    }

    public TabConfig closable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public Color getTitleForeground() {
        return titleForeground;
    }

    public Color getSelectedTitleForeground() {
        return selectedTitleForeground;
    }

    public TabConfig tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public TabConfig badge(String badge) {
        this.badge = badge;
        return this;
    }

    public TabConfig headerFactory(TabHeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
        return this;
    }

    public TabConfig pinned(boolean pinned) {
        this.pinned = pinned;
        return this;
    }

    public TabConfig dirty(boolean dirty) {
        this.dirty = dirty;
        return this;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public Component getComponent() {
        return component;
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

    public boolean isPinned() {
        return pinned;
    }

    public boolean isDirty() {
        return dirty;
    }
}
