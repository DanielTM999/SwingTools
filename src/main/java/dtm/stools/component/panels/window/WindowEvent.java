package dtm.stools.component.panels.window;

import dtm.stools.component.events.EventComponent;

import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WindowEvent implements EventComponent {
    private final Component source;
    private final WindowPanel window;
    private final String eventType;
    private final Map<String, Object> properties;
    private boolean canceled;

    public WindowEvent(Component source, WindowPanel window, String eventType, Map<String, Object> properties) {
        this.source = source;
        this.window = window;
        this.eventType = eventType;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(
                properties == null ? Map.of() : properties
        ));
    }

    public void cancel() { canceled = true; }
    public boolean isCanceled() { return canceled; }
    public WindowPanel getWindow() { return window; }
    public String getKey() { return window == null ? getStringProperty("key") : window.getWindowKey(); }
    public WindowState getOldState() { return getProperty("oldState", WindowState.class); }
    public WindowState getNewState() { return getProperty("newState", WindowState.class); }
    public Rectangle getOldBounds() { return copy(getProperty("oldBounds", Rectangle.class)); }
    public Rectangle getNewBounds() { return copy(getProperty("newBounds", Rectangle.class)); }
    public WindowSnap getSnap() { return getProperty("snap", WindowSnap.class); }
    public String getTitle() { return window == null ? getStringProperty("title") : window.getTitle(); }
    public String getOldTitle() { return getStringProperty("oldTitle"); }
    public String getCapability() { return getStringProperty("capability"); }
    public Object getOldValue() { return properties.get("oldValue"); }
    public Object getNewValue() { return properties.get("value"); }
    public int getZOrder() {
        Object value = properties.get("zOrder");
        return value instanceof Number number ? number.intValue() : -1;
    }
    public WindowLayoutSnapshot getLayoutSnapshot() {
        return getProperty("snapshot", WindowLayoutSnapshot.class);
    }
    public Object getProperty(String name) { return properties.get(name); }

    public <T> T getProperty(String name, Class<T> type) {
        Object value = properties.get(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Override public Component getComponent() { return source; }
    @Override public Object getValue() { return window; }

    @SuppressWarnings("unchecked")
    @Override public <T> T tryGetValue() {
        try { return (T) window; } catch (Exception ignored) { return null; }
    }

    @Override public String getEventType() { return eventType; }
    @Override public Map<String, Object> getProperties() { return properties; }

    private static Rectangle copy(Rectangle rectangle) {
        return rectangle == null ? null : new Rectangle(rectangle);
    }
}
