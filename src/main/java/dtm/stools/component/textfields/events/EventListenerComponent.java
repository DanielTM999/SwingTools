package dtm.stools.component.textfields.events;

import java.util.function.Consumer;

public interface EventListenerComponent {
    void addEventListner(String eventType, Consumer<EventComponent> event);
}
