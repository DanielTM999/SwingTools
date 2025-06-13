package dtm.stools.activity.delegated;

public interface DelegatedWindow {
    default void onRecieveServerEvent(Object eventArgs){};
    void sendServerEvent(Object eventArgs);
}
