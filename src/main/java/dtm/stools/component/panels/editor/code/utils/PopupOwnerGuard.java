package dtm.stools.component.panels.editor.code.utils;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;

public final class PopupOwnerGuard {

    private PopupOwnerGuard() {
    }

    public static boolean canShow(Component owner) {
        if (owner == null) return false;
        if (!owner.isDisplayable() || !owner.isShowing()) return false;
        Window window = SwingUtilities.getWindowAncestor(owner);
        return window != null && window.isShowing();
    }
}
