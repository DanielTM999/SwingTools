package dtm.stools.component.menu.bar.event;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.menu.bar.MenuBar;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.util.Map;

public class MenuBarEvent implements EventComponent {
    private final MenuBar source;
    private final Component component;
    private final MenuBar.Menu menu;
    private final JMenuItem item;
    private final Object value;
    private final String eventType;
    private final Map<String, Object> properties;
    private final boolean cancelable;
    private boolean canceled;

    public MenuBarEvent(MenuBar source,
                        Component component,
                        MenuBar.Menu menu,
                        JMenuItem item,
                        Object value,
                        String eventType,
                        Map<String, Object> properties) {
        this(source, component, menu, item, value, eventType, properties, false);
    }

    public MenuBarEvent(MenuBar source,
                        Component component,
                        MenuBar.Menu menu,
                        JMenuItem item,
                        Object value,
                        String eventType,
                        Map<String, Object> properties,
                        boolean cancelable) {
        this.source = source;
        this.component = component;
        this.menu = menu;
        this.item = item;
        this.value = value;
        this.eventType = eventType;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        this.cancelable = cancelable;
    }

    public MenuBar getSource() {
        return source;
    }

    @Override
    public Component getComponent() {
        return component;
    }

    public MenuBar.Menu getMenu() {
        return menu;
    }

    public JMenuItem getItem() {
        return item;
    }

    public String getMenuId() {
        return getStringProperty("menuId");
    }

    public String getItemId() {
        return getStringProperty("itemId");
    }

    public String getMenuText() {
        return getStringProperty("menuText");
    }

    public String getItemText() {
        return getStringProperty("itemText");
    }

    public boolean isSelected() {
        return getBooleanProperty("selected");
    }

    public boolean isCancelable() {
        return cancelable;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        if (cancelable) {
            canceled = true;
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T tryGetValue() {
        try {
            return (T) value;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
