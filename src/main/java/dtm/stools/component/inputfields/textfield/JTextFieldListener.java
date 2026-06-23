package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JTextFieldListener extends JTextField implements EventListenerComponent {

    private final Set<String> validEvents = new HashSet<>();
    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    public JTextFieldListener(){
        init();
    }

    public JTextFieldListener(int columns){
        super(columns);
        init();
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return () -> {};

        if(!validEvents.contains(eventType)) return () -> {};

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        listeners.get(eventType).remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            dispachEvent(EventType.LOAD, this, this);
        });
    }

    protected void registerValidEvents(Set<String> events){
        events.add(EventType.LOAD);
        events.add(EventType.CHANGE);
        events.add(EventType.INPUT);
        events.add(EventType.RESIZE);
        events.add(EventType.CLEAR);
        events.add(EventType.SUBMIT);
    }

    protected void dispachEvent(String eventType, Object value){
        dispachEvent(eventType, JTextFieldListener.this, value);
    }

    protected <T> void dispachEvent(String eventType, Supplier<T> value){
        dispachEvent(eventType, JTextFieldListener.this, value);
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

    protected <T> void dispachEvent(String eventType, Component component, Supplier<T> value){
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
                    public T tryGetValue() {
                        try {
                            return value.get();
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

    private void init(){
        registerValidEvents(validEvents);
    }

}
