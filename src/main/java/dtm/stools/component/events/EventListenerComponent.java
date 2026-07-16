package dtm.stools.component.events;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface EventListenerComponent {
    EventSubscription addEventListener(String eventType, Consumer<EventComponent> event);

    default <T extends EventComponent> EventSubscription addEventListener(String eventType,
                                                                          Class<T> eventClass,
                                                                          Consumer<T> event) {
        Consumer<EventComponent> wrapper = value -> {
            if (eventClass.isInstance(value)) {
                event.accept(eventClass.cast(value));
            }
        };
        addEventListener(eventType, wrapper);
        return () -> removeEventListner(eventType, wrapper);
    }

    default EventSubscription addEventListenerOnce(String eventType, Consumer<EventComponent> event) {
        final EventSubscription[] subscription = new EventSubscription[1];
        Consumer<EventComponent> wrapper = value -> {
            subscription[0].unsubscribe();
            event.accept(value);
        };
        addEventListener(eventType, wrapper);
        subscription[0] = () -> removeEventListner(eventType, wrapper);
        return subscription[0];
    }

    default <T extends EventComponent> EventSubscription addEventListenerOnce(String eventType,
                                                                              Class<T> eventClass,
                                                                              Consumer<T> event) {
        final EventSubscription[] subscription = new EventSubscription[1];
        Consumer<EventComponent> wrapper = value -> {
            if (!eventClass.isInstance(value)) return;
            subscription[0].unsubscribe();
            event.accept(eventClass.cast(value));
        };
        addEventListener(eventType, wrapper);
        subscription[0] = () -> removeEventListner(eventType, wrapper);
        return subscription[0];
    }

    default EventSubscription addEventListener(String[] eventTypes, Consumer<EventComponent> event) {
        return addEventListener(Arrays.asList(eventTypes), event);
    }

    default EventSubscription addEventListener(Collection<String> eventTypes, Consumer<EventComponent> event) {
        for (String eventType : eventTypes) {
            addEventListener(eventType, event);
        }
        return () -> {
            for (String eventType : eventTypes) {
                removeEventListner(eventType, event);
            }
        };
    }

    void removeEventListner(String eventType, Consumer<EventComponent> event);
    void removeEventListner(String eventType);

    default void removeEventListener(String eventType, Consumer<EventComponent> event) {
        removeEventListner(eventType, event);
    }

    default void removeEventListener(String eventType) {
        removeEventListner(eventType);
    }

    Map<String, List<Consumer<EventComponent>>> getEventListners();

    default Map<String, List<Consumer<EventComponent>>> getEventListeners() {
        return getEventListners();
    }

    void removeAllListeners();
}
