package dtm.stools.component.panels.tab;

import dtm.stools.component.events.EventComponent;

import java.awt.*;
import java.util.Map;

public class TabEvent implements EventComponent {
    private final Component source;
    private final TabEntry entry;
    private final String eventType;
    private final Map<String, Object> properties;
    private boolean canceled;

    public TabEvent(Component source, TabEntry entry, String eventType, Map<String, Object> properties) {
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

    public String getKey() {
        return entry == null ? null : entry.getKey();
    }

    public String getTitle() {
        return entry == null ? null : entry.getTitle();
    }

    public TabEntry getEntry() {
        return entry;
    }

    public Component getTabComponent() {
        return entry == null ? null : entry.getComponent();
    }

    public boolean isClosable() {
        return entry != null && entry.isClosable();
    }

    public boolean isPinned() {
        return entry != null && entry.isPinned();
    }

    public boolean isDirty() {
        return entry != null && entry.isDirty();
    }

    public int getIndex() {
        return getNumberProperty("index");
    }

    public int getOldIndex() {
        return getNumberProperty("oldIndex");
    }

    public int getNewIndex() {
        return getNumberProperty("newIndex");
    }

    public String getOldKey() {
        return getStringProperty("oldKey");
    }

    public String getNewKey() {
        return getStringProperty("newKey");
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
        return entry == null ? null : entry.getComponent();
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

    private int getNumberProperty(String name) {
        Object value = properties.get(name);
        return value instanceof Number number ? number.intValue() : -1;
    }

    @Override
    public String getStringProperty(String name) {
        Object value = properties.get(name);
        return value == null ? null : value.toString();
    }
}
