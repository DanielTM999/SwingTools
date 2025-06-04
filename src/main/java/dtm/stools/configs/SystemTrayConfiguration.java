package dtm.stools.configs;

import javax.swing.*;

public interface SystemTrayConfiguration {

    void enableSystemTray();
    void disableSystemTray();

    boolean removeOnRestore();
    void setRemoveOnRestore(boolean removeOnRestore);

    ImageIcon getImageIcon();
    void setImageIcon(ImageIcon icon);

    boolean isAvaiable();

}
