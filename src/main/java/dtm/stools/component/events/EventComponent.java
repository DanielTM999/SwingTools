package dtm.stools.component.events;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public interface EventComponent {
    Component getComponent();
    Object getValue();
    <T> T tryGetValue();
    String getEventType();
    default Map<String, Object> getProperties(){return new HashMap<>();}

    default Object getProperty(String name) {
        return getProperties().get(name);
    }

    default <T> T getProperty(String name, Class<T> type) {
        Object value = getProperty(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    default String getStringProperty(String name) {
        Object value = getProperty(name);
        return value == null ? null : value.toString();
    }

    default boolean getBooleanProperty(String name) {
        Object value = getProperty(name);
        return value instanceof Boolean bool && bool;
    }

    default int getIntProperty(String name, int fallback) {
        Object value = getProperty(name);
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
