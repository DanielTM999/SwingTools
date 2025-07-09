package dtm.stools.models;

import dtm.stools.configs.SystemTrayConfiguration;

import javax.swing.*;
import java.awt.*;

public class SystemTrayConfigurationConcrete implements SystemTrayConfiguration {

    private boolean systemTrayEnable;
    private boolean removeOnRestore;
    private boolean alwaysVisible;
    private Image image;

    @Override
    public void enableSystemTray() {
        this.systemTrayEnable = true;
    }

    @Override
    public void disableSystemTray() {
        this.systemTrayEnable = false;
    }

    @Override
    public boolean isRemoveOnRestore() {
        return removeOnRestore;
    }

    @Override
    public void setRemoveOnRestore(boolean removeOnRestore) {
        this.removeOnRestore = removeOnRestore;
    }

    @Override
    public boolean isAlwaysVisible() {
        return this.alwaysVisible;
    }

    @Override
    public void setAlwaysVisible(boolean alwaysVisible) {
        this.alwaysVisible = alwaysVisible;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public void setImageIcon(ImageIcon icon) {
        this.image = (icon != null) ? icon.getImage() : null;
    }

    @Override
    public void setImageIcon(Image image) {
        this.image = image;
    }

    @Override
    public boolean isAvaiable() {
        return SystemTray.isSupported() && systemTrayEnable;
    }
}
