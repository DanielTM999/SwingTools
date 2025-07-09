package dtm.stools.configs;

import javax.swing.*;
import java.awt.*;

public interface SystemTrayConfiguration {

    void enableSystemTray();
    void disableSystemTray();

    boolean isRemoveOnRestore();
    void setRemoveOnRestore(boolean removeOnRestore);

    boolean isAlwaysVisible();
    void setAlwaysVisible(boolean alwaysVisible);

    Image getImage();
    void setImageIcon(ImageIcon icon);
    void setImageIcon(Image image);

    boolean isAvaiable();

}
