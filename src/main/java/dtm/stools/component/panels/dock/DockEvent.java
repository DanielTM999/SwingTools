package dtm.stools.component.panels.dock;

import dtm.stools.component.events.EventComponent;

import java.awt.*;
import java.util.Map;

public class DockEvent implements EventComponent {
    private final Component source;
    private final DockEntry entry;
    private final String eventType;
    private final Map<String, Object> properties;
    private boolean canceled;

    public DockEvent(Component source, DockEntry entry, String eventType, Map<String, Object> properties) {
        this.source = source;
        this.entry = entry;
        this.eventType = eventType;
        this.properties = properties;
    }

    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public DockEntry getEntry() {
        return entry;
    }

    public String getKey() {
        return entry == null ? getStringProperty("key") : entry.getKey();
    }

    public String getTitle() {
        return entry == null ? getStringProperty("title") : entry.getTitle();
    }

    public Component getDockComponent() {
        return entry == null ? getProperty("component", Component.class) : entry.getComponent();
    }

    public DockRegion getRegion() {
        return entry == null ? getProperty("region", DockRegion.class) : entry.getRegion();
    }

    public DockRegion getOldRegion() {
        return getProperty("oldRegion", DockRegion.class);
    }

    public DockRegion getNewRegion() {
        return getProperty("newRegion", DockRegion.class);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public <T> T getProperty(String name, Class<T> type) {
        Object value = properties.get(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public Component getComponent() {
        return source;
    }

    @Override
    public Object getValue() {
        return getDockComponent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T tryGetValue() {
        try {
            return (T) getValue();
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

    @Override
    public String getStringProperty(String name) {
        Object value = properties.get(name);
        return value == null ? null : value.toString();
    }
}
