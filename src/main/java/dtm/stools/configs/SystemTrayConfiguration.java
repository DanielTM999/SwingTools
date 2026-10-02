package dtm.stools.configs;

import dtm.stools.context.enums.TrayIconScope;

import javax.swing.*;
import java.awt.*;

public interface SystemTrayConfiguration {

    void enableSystemTray();
    void disableSystemTray();

    boolean isRemoveOnRestore();
    void setRemoveOnRestore(boolean removeOnRestore);

    boolean isAlwaysVisible();
    void setAlwaysVisible(boolean alwaysVisible);

    TrayIconScope getTrayIconScope();
    void setTrayIconScope(TrayIconScope trayIconScope);

    Image getImage();
    void setImageIcon(ImageIcon icon);
    void setImageIcon(Image image);

    boolean isAvaiable();

}
