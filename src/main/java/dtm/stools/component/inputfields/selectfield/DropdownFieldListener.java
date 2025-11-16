package dtm.stools.component.inputfields.selectfield;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.JTextFieldListener;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DropdownFieldListener<T> extends JComboBox<T> implements EventListenerComponent {

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return;

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            dispachEvent(EventType.LOAD, this, this);
        });
    }

    protected void dispachEvent(String eventType, Object value){
        dispachEvent(eventType, DropdownFieldListener.this, value);
    }

    protected <S> void dispachEvent(String eventType, Supplier<S> value){
        dispachEvent(eventType, DropdownFieldListener.this, value);
    }

    protected void dispachEvent(String eventType, Component component, Object value){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <S> S tryGetValue() {
                        try {
                            return (S) value;
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    protected <S> void dispachEvent(String eventType, Component component, Supplier<S> value){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value.get();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public S tryGetValue() {
                        try {
                            return (S) value.get();
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public Object getValue() {
        return getSelectedItem();
    }

    public <S> S getValue(@NonNull Class<S> type) {
        Object value = getSelectedItem();
        return (type.isInstance(value)) ? type.cast(value) : null;
    }

    @SuppressWarnings("unchecked")
    public <S> S getValueAs() {
        try{
            Object value = getSelectedItem();
            return (S) value;
        }catch (Exception e){
            return null;
        }
    }


}
