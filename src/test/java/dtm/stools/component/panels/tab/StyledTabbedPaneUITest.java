package dtm.stools.component.panels.tab;

import com.formdev.flatlaf.FlatDarkLaf;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyledTabbedPaneUITest {

    @Test
    void removesTrailingTabAreaGapOnlyForMenuOverflow() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel();
            ExposedTabbedPaneUI menuUi = new ExposedTabbedPaneUI(tabs);
            tabs.getTabbedPane().setUI(menuUi);

            assertEquals(0, menuUi.effectiveTabAreaInsets().right);

            tabs.setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS);
            ExposedTabbedPaneUI chevronUi = new ExposedTabbedPaneUI(tabs);
            tabs.getTabbedPane().setUI(chevronUi);

            assertEquals(tabs.getTabStyle().getTabAreaInsets().right,
                    chevronUi.effectiveTabAreaInsets().right);
            return null;
        });
    }

    @Test
    void usesSingleOverflowMenuButtonByDefault() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel();
            for (int index = 0; index < 16; index++) {
                tabs.addTab("tab-" + index, "Document-" + index + ".java", new JPanel());
            }

            JTabbedPane tabbedPane = tabs.getTabbedPane();
            tabbedPane.setSize(420, 240);
            tabbedPane.doLayout();

            List<JButton> scrollButtons = findScrollButtons(tabbedPane);
            assertEquals(TabOverflowMode.MENU, tabs.getTabOverflowMode());
            assertEquals(JTabbedPane.SCROLL_TAB_LAYOUT, tabs.getTabLayoutPolicy());
            assertEquals(16, tabbedPane.getTabCount());
            assertEquals(2, scrollButtons.size());
            assertEquals(1, scrollButtons.stream().filter(Component::isVisible).count());

            JButton menuButton = scrollButtons.stream()
                    .filter(button -> Boolean.TRUE.equals(button.getClientProperty(
                            StyledTabbedPaneUI.OVERFLOW_MENU_BUTTON_PROPERTY
                    )))
                    .findFirst()
                    .orElseThrow();
            assertTrue(menuButton.isVisible());

            JViewport tabViewport = findDirectTabViewport(tabbedPane);
            assertNotNull(tabViewport);
            assertEquals(menuButton.getX(), tabViewport.getX() + tabViewport.getWidth());
            Rectangle visibleTab = tabbedPane.getBoundsAt(tabbedPane.getTabCount() - 1);
            assertEquals(visibleTab.y, menuButton.getY());
            assertEquals(visibleTab.height, menuButton.getHeight());
            assertEquals(
                    visibleTab.y + visibleTab.height / 2,
                    menuButton.getY() + menuButton.getHeight() / 2
            );

            Rectangle stableViewportBounds = tabViewport.getBounds();
            Point stableViewPosition = tabViewport.getViewPosition();
            tabbedPane.doLayout();
            assertEquals(stableViewportBounds, tabViewport.getBounds());
            assertEquals(stableViewPosition, tabViewport.getViewPosition());

            JPopupMenu menu = tabs.createTabOverflowMenu(menuButton);
            JList<?> overflowList = findOverflowList(menu);
            assertNotNull(overflowList);
            assertTrue(overflowList.getModel().getSize() > 0);
            assertTrue(overflowList.getModel().getSize() < tabs.getTabCount());
            return null;
        });
    }

    @Test
    void alignsOverflowMenuButtonWithTabRowUnderFlatLaf() throws Exception {
        onEdt(() -> {
            LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());

                TabbedPanel tabs = new TabbedPanel();
                for (int index = 0; index < 16; index++) {
                    tabs.addTab("tab-" + index, "Document-" + index + ".java", new JPanel());
                }

                JTabbedPane tabbedPane = tabs.getTabbedPane();
                tabbedPane.setSize(960, 520);
                tabbedPane.doLayout();

                JButton menuButton = findScrollButtons(tabbedPane).stream()
                        .filter(button -> Boolean.TRUE.equals(button.getClientProperty(
                                StyledTabbedPaneUI.OVERFLOW_MENU_BUTTON_PROPERTY
                        )))
                        .findFirst()
                        .orElseThrow();
                Rectangle selectedTab = tabbedPane.getBoundsAt(tabbedPane.getSelectedIndex());

                assertTrue(menuButton.isVisible());
                assertEquals(selectedTab.y, menuButton.getY());
                assertEquals(
                        selectedTab.y + selectedTab.height / 2,
                        menuButton.getY() + menuButton.getHeight() / 2
                );
                return null;
            } finally {
                UIManager.setLookAndFeel(previousLookAndFeel);
            }
        });
    }

    @Test
    void closingFromOverflowRefreshesTheOpenList() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel();
            for (int index = 0; index < 16; index++) {
                tabs.addTab("tab-" + index, "Document-" + index + ".java", new JPanel());
            }

            JTabbedPane tabbedPane = tabs.getTabbedPane();
            tabbedPane.setSize(420, 240);
            tabbedPane.doLayout();
            JButton menuButton = findScrollButtons(tabbedPane).stream()
                    .filter(button -> Boolean.TRUE.equals(button.getClientProperty(
                            StyledTabbedPaneUI.OVERFLOW_MENU_BUTTON_PROPERTY
                    )))
                    .findFirst()
                    .orElseThrow();

            JPopupMenu menu = tabs.createTabOverflowMenu(menuButton);
            JList<?> overflowList = findOverflowList(menu);
            assertNotNull(overflowList);
            overflowList.setSelectedIndex(0);
            TabEntry closing = (TabEntry) overflowList.getSelectedValue();
            int tabCountBefore = tabs.getTabCount();

            overflowList.getActionMap().get("closeOverflowTab").actionPerformed(null);

            assertEquals(tabCountBefore - 1, tabs.getTabCount());
            assertFalse(tabs.contains(closing.getKey()));
            assertTrue(overflowList.getModel().getSize() > 0);
            for (int index = 0; index < overflowList.getModel().getSize(); index++) {
                TabEntry remaining = (TabEntry) overflowList.getModel().getElementAt(index);
                assertFalse(closing.getKey().equals(remaining.getKey()));
            }
            return null;
        });
    }

    @Test
    void createsModernThemeAwareScrollButton() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel()
                    .setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS);
            ExposedTabbedPaneUI ui = new ExposedTabbedPaneUI(tabs);
            JButton button = ui.scrollButton(SwingConstants.EAST);

            assertEquals(28, button.getPreferredSize().width);
            assertEquals(28, button.getPreferredSize().height);
            assertEquals(SwingConstants.EAST, button.getClientProperty(StyledTabbedPaneUI.SCROLL_BUTTON_PROPERTY));
            assertFalse(button.isContentAreaFilled());
            assertFalse(button.isBorderPainted());
            assertFalse(button.isFocusPainted());
            assertFalse(button.isFocusable());
            assertFalse(button.isOpaque());
            assertEquals(Cursor.HAND_CURSOR, button.getCursor().getType());
            assertNotNull(button.getToolTipText());
            assertNotNull(button.getAccessibleContext().getAccessibleName());
            return null;
        });
    }

    @Test
    void keepsChevronButtonsAsAnExplicitOverflowMode() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel()
                    .setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS);
            for (int index = 0; index < 12; index++) {
                tabs.addTab("tab-" + index, "Long-document-name-" + index, new JPanel());
            }

            JTabbedPane tabbedPane = tabs.getTabbedPane();
            tabbedPane.setSize(360, 220);
            tabbedPane.doLayout();

            List<JButton> buttons = findScrollButtons(tabbedPane);
            assertEquals(TabOverflowMode.SCROLL_BUTTONS, tabs.getTabOverflowMode());
            assertEquals(2, buttons.size());
            assertTrue(buttons.stream().allMatch(Component::isVisible));
            assertTrue(buttons.stream().noneMatch(button -> Boolean.TRUE.equals(
                    button.getClientProperty(StyledTabbedPaneUI.OVERFLOW_MENU_BUTTON_PROPERTY)
            )));
            return null;
        });
    }

    @Test
    void paintsChevronInEveryTabDirection() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel()
                    .setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS);
            tabs.setTabScrollButtonForeground(new Color(0xDDE7F5));
            ExposedTabbedPaneUI ui = new ExposedTabbedPaneUI(tabs);

            for (int direction : new int[]{
                    SwingConstants.NORTH,
                    SwingConstants.SOUTH,
                    SwingConstants.EAST,
                    SwingConstants.WEST
            }) {
                JButton button = ui.scrollButton(direction);
                button.setSize(button.getPreferredSize());
                BufferedImage image = new BufferedImage(
                        button.getWidth(),
                        button.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D graphics = image.createGraphics();
                button.paint(graphics);
                graphics.dispose();
                assertTrue(hasPaintedPixel(image), "Chevron not painted for direction " + direction);
            }
            return null;
        });
    }

    @Test
    void supportsFluentScrollButtonCustomizationAndValidation() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel()
                    .setTabOverflowMode(TabOverflowMode.SCROLL_BUTTONS)
                    .setTabScrollButtonSize(34)
                    .setTabScrollButtonArc(12)
                    .setTabScrollButtonStrokeWidth(2.25f)
                    .setTabScrollButtonHoverBackground(new Color(0x334155))
                    .setTabScrollButtonPressedBackground(new Color(0x1E293B));

            JButton button = new ExposedTabbedPaneUI(tabs).scrollButton(SwingConstants.WEST);
            assertEquals(new Dimension(34, 34), button.getPreferredSize());
            assertEquals(12, tabs.getTabStyle().getTabScrollButtonArc());
            assertEquals(2.25f, tabs.getTabStyle().getTabScrollButtonStrokeWidth());

            assertThrows(IllegalArgumentException.class, () -> tabs.setTabScrollButtonSize(17));
            assertThrows(IllegalArgumentException.class, () -> tabs.setTabScrollButtonArc(-1));
            assertThrows(IllegalArgumentException.class, () -> tabs.setTabScrollButtonStrokeWidth(Float.NaN));
            assertThrows(IllegalArgumentException.class, () -> new ExposedTabbedPaneUI(tabs).scrollButton(999));
            return null;
        });
    }

    private static boolean hasPaintedPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<JButton> findScrollButtons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button
                    && button.getClientProperty(StyledTabbedPaneUI.SCROLL_BUTTON_PROPERTY) != null) {
                result.add(button);
            }
            if (component instanceof Container container) {
                result.addAll(findScrollButtons(container));
            }
        }
        return result;
    }

    private static JList<?> findOverflowList(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JList<?> list
                    && Boolean.TRUE.equals(list.getClientProperty(TabbedPanel.TAB_OVERFLOW_LIST_PROPERTY))) {
                return list;
            }
            if (component instanceof Container container) {
                JList<?> result = findOverflowList(container);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JViewport findDirectTabViewport(JTabbedPane tabbedPane) {
        for (Component component : tabbedPane.getComponents()) {
            if (component instanceof JViewport viewport) {
                return viewport;
            }
        }
        return null;
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.call();
        }

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new InvocationTargetException(throwable);
        }
        return result.get();
    }

    private static final class ExposedTabbedPaneUI extends StyledTabbedPaneUI {

        private ExposedTabbedPaneUI(TabbedPanel tabs) {
            super(tabs);
        }

        private JButton scrollButton(int direction) {
            return createScrollButton(direction);
        }

        private Insets effectiveTabAreaInsets() {
            return tabAreaInsets;
        }
    }
}
