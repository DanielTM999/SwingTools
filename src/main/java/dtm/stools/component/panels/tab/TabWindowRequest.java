package dtm.stools.component.panels.tab;

import javax.swing.*;
import java.awt.*;

public class TabWindowRequest {
    private final String key;
    private final String title;
    private final Icon icon;
    private final Component component;
    private final Window owner;
    private final Dimension preferredWindowSize;
    private final Point screenLocation;
    private final boolean alwaysOnTop;

    TabWindowRequest(String key, String title, Icon icon, Component component, Window owner,
                     Dimension preferredWindowSize, Point screenLocation, boolean alwaysOnTop) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.component = component;
        this.owner = owner;
        this.preferredWindowSize = preferredWindowSize;
        this.screenLocation = screenLocation;
        this.alwaysOnTop = alwaysOnTop;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public Icon getIcon() {
        return icon;
    }

    public Component getComponent() {
        return component;
    }

    public Window getOwner() {
        return owner;
    }

    public Dimension getPreferredWindowSize() {
        return preferredWindowSize;
    }

    public Point getScreenLocation() {
        return screenLocation;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }
}
