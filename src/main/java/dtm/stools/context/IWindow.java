package dtm.stools.context;

import lombok.NonNull;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public interface IWindow {
    void init();
    void dispose();
    boolean isDisplayable();
    
    boolean putInClient(String key, Object value);
    boolean putInClient(String key, Object value, boolean replace);
    <T> T getFromClient(String key);
    <T> T getFromClient(String key, T defaultValue);

    default <T extends Component> T findById(@NonNull String id){return null;};
    default <T extends Component> List<T> findAllById(@NonNull String id){return null;};
    default <T extends IWindow> void runOnUi(Consumer<T> action){SwingUtilities.invokeLater(() -> action.accept((T)this));}
    void reloadDomElements();
}
