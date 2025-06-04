package dtm.stools.models;

import dtm.stools.configs.SystemTrayConfiguration;

import javax.swing.*;
import java.awt.*;

public class SystemTrayConfigurationConcrete implements SystemTrayConfiguration {

    private boolean systemTrayEnable;
    private boolean removeOnRestore;
    private ImageIcon imageIcon;

    @Override
    public void enableSystemTray() {
        this.systemTrayEnable = true;
    }

    @Override
    public void disableSystemTray() {
        this.systemTrayEnable = false;
    }

    @Override
    public boolean removeOnRestore() {
        return removeOnRestore;
    }

    @Override
    public void setRemoveOnRestore(boolean removeOnRestore) {
        this.removeOnRestore = removeOnRestore;
    }

    @Override
    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    @Override
    public void setImageIcon(ImageIcon icon) {
        this.imageIcon = icon;
    }

    @Override
    public boolean isAvaiable() {
        return SystemTray.isSupported() && systemTrayEnable && (imageIcon != null);
    }
}
