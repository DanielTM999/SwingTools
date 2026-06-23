package dtm.stools.component.panels.tab;

import javax.swing.*;
import java.awt.*;

public class TabEntry {
    private final String key;
    private final Component component;
    private String title;
    private Icon icon;
    private Icon selectedIcon;
    private Color titleForeground;
    private Color selectedTitleForeground;
    private Color diagnosticColor;
    private boolean closable;
    private String tooltip;
    private String badge;
    private TabHeaderFactory headerFactory;
    private boolean dragging;
    private boolean pinned;
    private boolean dirty;

    TabEntry(String key, String title, Icon icon, Component component, boolean closable, String tooltip, Color titleForeground, Color selectedTitleForeground) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.component = component;
        this.closable = closable;
        this.tooltip = tooltip;
        this.titleForeground = titleForeground;
        this.selectedTitleForeground = selectedTitleForeground;
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

    void setTitle(String title) {
        this.title = title;
    }

    public Icon getIcon() {
        return icon;
    }

    void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    void setSelectedIcon(Icon selectedIcon) {
        this.selectedIcon = selectedIcon;
    }

    public boolean isClosable() {
        return closable;
    }

    void setClosable(boolean closable) {
        this.closable = closable;
    }

    public String getTooltip() {
        return tooltip;
    }

    void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public String getBadge() {
        return badge;
    }

    void setBadge(String badge) {
        this.badge = badge;
    }

    public TabHeaderFactory getHeaderFactory() {
        return headerFactory;
    }

    void setHeaderFactory(TabHeaderFactory headerFactory) {
        this.headerFactory = headerFactory;
    }

    public boolean isDragging() {
        return dragging;
    }

    void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public boolean isPinned() {
        return pinned;
    }

    void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isDirty() {
        return dirty;
    }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public Color getTitleForeground() {
        return titleForeground;
    }

    void setTitleForeground(Color titleForeground) {
        this.titleForeground = titleForeground;
    }

    public Color getSelectedTitleForeground() {
        return selectedTitleForeground;
    }

    void setSelectedTitleForeground(Color selectedTitleForeground) {
        this.selectedTitleForeground = selectedTitleForeground;
    }

    public Color getDiagnosticColor() {
        return diagnosticColor;
    }

    void setDiagnosticColor(Color diagnosticColor) {
        this.diagnosticColor = diagnosticColor;
    }
}
