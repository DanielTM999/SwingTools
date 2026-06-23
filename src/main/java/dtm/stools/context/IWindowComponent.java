package dtm.stools.context;

import lombok.NonNull;

import javax.swing.*;
import java.awt.Component;
import java.util.List;

public interface IWindowComponent {
    default <T extends Component> T findById(@NonNull String id){return null;};
    default <T extends Component> List<T> findAllById(@NonNull String id){return null;};
    default void runOnUiTread(Runnable action) {
        SwingUtilities.invokeLater(action);
    }

    boolean putInClient(String key, Object value);
    boolean putInClient(String key, Object value, boolean replace);
    <T> T getFromClient(String key);
    <T> T getFromClient(String key, T defaultValue);

    void reloadDomElements();
}
