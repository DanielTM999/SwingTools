package dtm.stools.activity.delegated;

public interface DelegatedWindow {
    default void onRecieveEvent(Object eventArgs){};
    void sendEvent(Object eventArgs);
}
