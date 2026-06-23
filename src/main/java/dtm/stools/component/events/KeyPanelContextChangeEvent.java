package dtm.stools.component.events;

import javax.swing.*;

public interface KeyPanelContextChangeEvent {
    JPanel getCurrentPanel();
    JPanel getNextPanel();
    String getKey();
    void cancel();
}
