package dtm.stools.component.tree;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TreeViewListener extends JTree implements EventListenerComponent {

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    public TreeViewListener() {
        super();
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if (eventType == null || eventType.isEmpty()) return () -> {};
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> dispachEvent(EventType.LOAD, this, this));
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        List<Consumer<EventComponent>> list = listeners.get(eventType);
        if (list != null) list.remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    protected void dispachEvent(String eventType, Object value) {
        dispachEvent(eventType, this, value);
    }

    protected <T> void dispachEvent(String eventType, Supplier<T> value) {
        dispachEvent(eventType, this, value);
    }

    protected void dispachEvent(String eventType, Component component, Object value) {
        if (listeners.isEmpty()) return;
        List<Consumer<EventComponent>> list = listeners.get(eventType);
        if (list == null || list.isEmpty()) return;

        EventComponent event = new EventComponent() {
            @Override public Component getComponent() { return component; }
            @Override public Object getValue() { return value; }
            @SuppressWarnings("unchecked")
            @Override public <X> X tryGetValue() {
                try { return (X) value; } catch (Exception e) { return null; }
            }
            @Override public String getEventType() { return eventType; }
        };

        for (Consumer<EventComponent> l : list) {
            try { l.accept(event); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    protected <T> void dispachEvent(String eventType, Component component, Supplier<T> value) {
        if (listeners.isEmpty()) return;
        List<Consumer<EventComponent>> list = listeners.get(eventType);
        if (list == null || list.isEmpty()) return;

        EventComponent event = new EventComponent() {
            @Override public Component getComponent() { return component; }
            @Override public Object getValue() { return value.get(); }
            @SuppressWarnings("unchecked")
            @Override public <X> X tryGetValue() {
                try { return (X) value.get(); } catch (Exception e) { return null; }
            }
            @Override public String getEventType() { return eventType; }
        };

        for (Consumer<EventComponent> l : list) {
            try { l.accept(event); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
