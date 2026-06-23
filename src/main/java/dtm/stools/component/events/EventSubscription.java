package dtm.stools.component.events;

public interface EventSubscription {
    void unsubscribe();

    default void close() {
        unsubscribe();
    }
}
