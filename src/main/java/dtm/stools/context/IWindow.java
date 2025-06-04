package dtm.stools.context;

import javax.swing.*;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public interface IWindow {
    void init();
    void dispose();
    default <T extends IWindow> void runOnUi(Consumer<T> action){
        SwingUtilities.invokeLater(() -> action.accept((T)this));
    }
}
