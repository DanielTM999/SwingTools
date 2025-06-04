package dtm.stools.context;

import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public interface IWindow {
    void init();
    void dispose();
    default <T extends Component> T findById(@NonNull String id){return null;};
    default <T extends IWindow> void runOnUi(Consumer<T> action){
        SwingUtilities.invokeLater(() -> action.accept((T)this));
    }
}
