package dtm.stools.component.panels.base;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.BlockingPanel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PanelEventListener extends BlockingPanel implements EventListenerComponent {

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    protected PanelEventListener(){}

    protected PanelEventListener(boolean opaque){
        setOpaque(opaque);
    }

    protected PanelEventListener(LayoutManager layout){
        this(layout, false);
    }

    protected PanelEventListener(LayoutManager layout, boolean opaque){
        super(layout);
        setOpaque(opaque);
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return () -> {};

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
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
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            dispatchEvent(EventType.LOAD, this, this, new HashMap<>());
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    protected void dispatchEvent(String eventType, Object value){
        dispatchEvent(eventType, PanelEventListener.this, value, new HashMap<>());
    }

    protected <T> void dispatchEvent(String eventType, Supplier<T> value){
        dispatchEvent(eventType, PanelEventListener.this, value);
    }

    protected void dispatchEvent(String eventType, Object value, Map<String, Object> props){
        dispatchEvent(eventType, PanelEventListener.this, value, props);
    }

    protected <T> void dispatchEvent(String eventType, Supplier<T> value, Map<String, Object> props){
        dispatchEvent(eventType, PanelEventListener.this, value, props);
    }

    protected void dispatchEvent(String eventType, Component component, Object value, Map<String, Object> props){
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

                    @Override
                    public Map<String, Object> getProperties() {
                        return props;
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

    protected <T> void dispatchEvent(String eventType, Component component, Supplier<T> value){
        dispatchEvent(eventType, component, value, new HashMap<>());
    }

    protected <T> void dispatchEvent(String eventType, Component component, Supplier<T> value, Map<String, Object> props){
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

                    @Override
                    public Map<String, Object> getProperties() {
                        return props;
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

}
