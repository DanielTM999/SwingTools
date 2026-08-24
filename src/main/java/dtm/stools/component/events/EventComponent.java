package dtm.stools.component.events;

import dtm.stools.exceptions.EventCastComponentException;
import dtm.stools.exceptions.EventComponentException;
import dtm.stools.exceptions.NullEventComponentException;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public interface EventComponent {
    Component getComponent();
    Object getValue();
    <T> T tryGetValue();
    String getEventType();
    default Map<String, Object> getProperties(){return new HashMap<>();}

    default <T> T getValueOrThrow(){
        try{
            T event =  tryGetValue();
            if (event != null) return event;
            throw new NullEventComponentException("null event component");
        }catch(ClassCastException e){
            throw new EventCastComponentException("Event component does not implement this type", e);
        }
    }

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
