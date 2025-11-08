package dtm.stools.component.inputfields.events;

import java.util.function.Consumer;

public interface EventListenerComponent {
    void addEventListner(String eventType, Consumer<EventComponent> event);
}
