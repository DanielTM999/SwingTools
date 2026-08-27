package dtm.stools.activity.delegated;

public interface DelegatedWindow {
    default void onReceiveEvent(Object eventArgs){};
    void sendEvent(Object eventArgs);


    @Deprecated(forRemoval = true)
    default void onRecieveEvent(Object eventArgs){};
}
