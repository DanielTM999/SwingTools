package dtm.stools.theme;

import dtm.stools.utils.FontUtils;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ThemeSupport {

    private static final String DIVIDER_GUARD_PROPERTY = "SwingTools.dividerGuard";

    private static final Map<String, Runnable> LISTENERS = new ConcurrentHashMap<>();

    private ThemeSupport() {
    }

    public static void setLookAndFeel(LookAndFeel lookAndFeel) throws Exception {
        setLookAndFeel(lookAndFeel, null);
    }

    public static void setLookAndFeel(LookAndFeel lookAndFeel, Runnable afterInstall) throws Exception {
        if (lookAndFeel == null) {
            return;
        }

        Font globalFont = FontUtils.getGlobalFont();
        UIManager.setLookAndFeel(lookAndFeel);
        FontUtils.setGlobalFont(globalFont);

        if (afterInstall != null) {
            afterInstall.run();
        }

        refreshAllWindows();
    }

    public static void setLookAndFeel(String lookAndFeelClassName) throws Exception {
        if (lookAndFeelClassName == null || lookAndFeelClassName.isBlank()) {
            return;
        }

        Font globalFont = FontUtils.getGlobalFont();
        UIManager.setLookAndFeel(lookAndFeelClassName);
        FontUtils.setGlobalFont(globalFont);
        refreshAllWindows();
    }

    public static void refreshAllWindows() {
        for (Window window : Window.getWindows()) {
            refresh(window);
        }
        notifyListeners();
    }

    public static void refresh(Component root) {
        if (root == null) {
            return;
        }

        try {
            SwingUtilities.updateComponentTreeUI(root);
        } catch (Exception ignored) {
        }

        root.invalidate();
        root.validate();
        root.repaint();
    }

    public static void addThemeListener(String id, Runnable listener) {
        if (id == null || listener == null) {
            return;
        }
        LISTENERS.put(id, listener);
    }

    public static void removeThemeListener(String id) {
        if (id == null) {
            return;
        }
        LISTENERS.remove(id);
    }

    public static JSplitPane protectDividerLocation(JSplitPane splitPane) {
        if (splitPane == null
                || splitPane instanceof ThemeAwareSplitPane
                || Boolean.TRUE.equals(splitPane.getClientProperty(DIVIDER_GUARD_PROPERTY))) {
            return splitPane;
        }

        splitPane.putClientProperty(DIVIDER_GUARD_PROPERTY, Boolean.TRUE);

        int[] lastLocation = {splitPane.getDividerLocation()};
        int[] lastSize = {splitPane.getDividerSize()};

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            int location = splitPane.getDividerLocation();
            if (location > 0) {
                lastLocation[0] = location;
            }
        });
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_SIZE_PROPERTY, event -> {
            if (!Boolean.TRUE.equals(splitPane.getClientProperty(DIVIDER_GUARD_PROPERTY + ".restoring"))) {
                lastSize[0] = splitPane.getDividerSize();
            }
        });
        splitPane.addPropertyChangeListener("UI", event -> SwingUtilities.invokeLater(() -> {
            splitPane.putClientProperty(DIVIDER_GUARD_PROPERTY + ".restoring", Boolean.TRUE);
            try {
                splitPane.setDividerSize(lastSize[0]);
                if (lastLocation[0] > 0) {
                    splitPane.setDividerLocation(lastLocation[0]);
                }
            } finally {
                splitPane.putClientProperty(DIVIDER_GUARD_PROPERTY + ".restoring", null);
            }
        }));

        return splitPane;
    }

    public static void applyThemeTree(Component root) {
        if (root == null) {
            return;
        }

        if (root instanceof ThemeAware themeAware) {
            try {
                themeAware.applyTheme();
            } catch (Exception ignored) {
            }
        }

        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyThemeTree(child);
            }
        }

        if (root instanceof JComponent component) {
            component.repaint();
        }
    }

    private static void notifyListeners() {
        LISTENERS.forEach((id, listener) -> {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        });
    }
}
