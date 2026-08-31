package dtm.stools.component.panels.window;

import dtm.stools.component.icon.FittedIcon;
import dtm.stools.component.menu.bar.CollapsibleMenuBar;
import dtm.stools.component.menu.bar.MenuBar;
import dtm.stools.component.delegated.DelegatedWindowPanel;
import dtm.stools.component.delegated.DelegatedWindowDesktopPanel;
import dtm.stools.controllers.component.AbstractWindowDesktopController;
import dtm.stools.controllers.component.AbstractWindowPanelController;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class WindowDesktopPanelTest {

    @Test
    void opensAtThePositionConfiguredByWindowConfig() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();

            assertPosition(desktop, "center", WindowPosition.CENTER, 350, 250);
            assertPosition(desktop, "left", WindowPosition.LEFT, 0, 250);
            assertPosition(desktop, "right", WindowPosition.RIGHT, 700, 250);
            assertPosition(desktop, "top", WindowPosition.TOP, 350, 0);
            assertPosition(desktop, "bottom", WindowPosition.BOTTOM, 350, 500);
            assertPosition(desktop, "top-left", WindowPosition.TOP_LEFT, 0, 0);
            assertPosition(desktop, "top-right", WindowPosition.TOP_RIGHT, 700, 0);
            assertPosition(desktop, "bottom-left", WindowPosition.BOTTOM_LEFT, 0, 500);
            assertPosition(desktop, "bottom-right", WindowPosition.BOTTOM_RIGHT, 700, 500);
            return null;
        });
    }

    @Test
    void rendersTheActualMenuBarConfiguredForTheWindow() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            MenuBar menuBar = new MenuBar();
            menuBar.add(new JMenu("Arquivo"));
            JPanel firstContent = new JPanel();
            WindowConfig config = new WindowConfig("with-menu", "Editor", firstContent)
                    .menuBar(menuBar);

            WindowPanel window = desktop.openWindow(config);

            assertSame(menuBar, config.getMenuBar());
            assertSame(menuBar, window.getMenuBar());
            assertEquals(WindowMenuBarPlacement.BELOW_TITLE_BAR, window.getMenuBarPlacement());
            assertSame(window.getContentHost(), menuBar.getParent());
            assertSame(window.getContentHost(), firstContent.getParent());

            JPanel replacement = new JPanel();
            window.content(replacement);
            assertSame(window.getContentHost(), menuBar.getParent(),
                    "trocar o conteudo nao deve remover a barra de menu");
            assertSame(window.getContentHost(), replacement.getParent());
            return null;
        });
    }

    @Test
    void rendersCollapsibleMenuBarInsideTheTitleBarWhenConfigured() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            CollapsibleMenuBar menuBar = new CollapsibleMenuBar();
            menuBar.addMenu("file", "Arquivo").addItem("new", "Novo");
            Color originalMenuBackground = menuBar.getBackground();
            JButton centerAction = new JButton("Executar");

            WindowPanel window = desktop.openWindow(new WindowConfig(
                            "integrated-menu", "Editor", new JPanel())
                    .bounds(new Rectangle(100, 80, 640, 420))
                    .menuBar(menuBar, WindowMenuBarPlacement.TITLE_BAR)
                    .menuBarFollowTitleBarColor(true)
                    .titleBarCenter(centerAction));

            assertSame(menuBar, window.getMenuBar());
            assertSame(menuBar, window.getTitleBar().getMenuBar());
            assertEquals(WindowMenuBarPlacement.TITLE_BAR, window.getMenuBarPlacement());
            assertSame(centerAction, window.getTitleBarCenter());
            assertTrue(SwingUtilities.isDescendingFrom(centerAction, window.getTitleBar()));
            assertFalse(window.getTitleBar().center.isOptimizedDrawingEnabled(),
                    "o host deve repintar corretamente o centro sobre o menu");
            assertTrue(SwingUtilities.isDescendingFrom(menuBar, window.getTitleBar()));
            assertFalse(SwingUtilities.isDescendingFrom(menuBar, window.getContentHost()));
            assertFalse(SwingUtilities.isDescendingFrom(
                    window.getTitleBar().getTitleLabel(), window.getTitleBar()),
                    "o titulo deve ceder toda a area central ao menu integrado");
            assertTrue(menuBar.isTitleBarEmbedded());
            assertTrue(menuBar.isOpaque());
            assertEquals(window.getWindowStyle().getActiveTitleBackground(), menuBar.getBackground());

            menuBar.setCollapsed(true);
            assertSame(centerAction, window.getTitleBarCenter());
            assertTrue(centerAction.isVisible());
            assertEquals(0, window.getTitleBar().center.getComponentZOrder(centerAction),
                    "o componente central deve continuar acima do menu ao recolher");
            menuBar.setCollapsed(false);
            assertEquals(window.getWindowStyle().getTitleBarHeight(),
                    window.getTitleBar().getPreferredSize().height,
                    "o menu nao deve aumentar a altura definida para a barra de titulo");

            WindowTitleBar titleBar = window.getTitleBar();
            titleBar.setSize(612, titleBar.getPreferredSize().height);
            titleBar.doLayout();
            titleBar.center.doLayout();
            assertEquals(0, titleBar.getBorder().getBorderInsets(titleBar).left);
            assertEquals(0, titleBar.center.getX(),
                    "sem icone o menu deve usar a area desde o inicio da barra");
            assertEquals((titleBar.getWidth() - centerAction.getWidth()) / 2,
                    titleBar.center.getX() + centerAction.getX(),
                    "a acao do usuario deve ficar no centro geometrico da barra");

            window.icon(new SquareIcon(16));
            titleBar.setSize(612, titleBar.getPreferredSize().height);
            titleBar.doLayout();
            titleBar.center.doLayout();
            assertTrue(titleBar.getIconLabel().isVisible());
            assertTrue(titleBar.center.getX() > 0,
                    "com icone a area do menu deve iniciar depois dele");
            assertFalse(SwingUtilities.isDescendingFrom(titleBar.getTitleLabel(), titleBar));

            window.setActiveDirect(false);
            assertEquals(window.getWindowStyle().getTitleBackground(), menuBar.getBackground(),
                    "o menu deve acompanhar a cor inativa da barra de titulo");
            window.setActiveDirect(true);
            assertEquals(window.getWindowStyle().getActiveTitleBackground(), menuBar.getBackground());

            window.menuBarPlacement(WindowMenuBarPlacement.BELOW_TITLE_BAR);
            assertNull(window.getTitleBar().getMenuBar());
            assertSame(window.getContentHost(), menuBar.getParent());
            assertFalse(menuBar.isTitleBarEmbedded());
            assertEquals(originalMenuBackground, menuBar.getBackground());

            JLabel replacement = new JLabel("Centro novo");
            window.titleBarCenter(replacement);
            assertSame(replacement, window.getTitleBarCenter());
            assertFalse(SwingUtilities.isDescendingFrom(centerAction, titleBar));
            return null;
        });
    }

    @Test
    void opensAndTransitionsThroughDefaultStates() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("editor", "Editor", new JTextArea())
                    .bounds(new Rectangle(30, 40, 480, 320)));

            assertTrue(window.isVisible());
            assertSame(window, desktop.getActiveWindow());
            assertEquals(new Rectangle(30, 40, 480, 320), window.getBounds());

            window.maximize();
            assertEquals(WindowState.MAXIMIZED, window.getWindowState());
            assertEquals(desktop.getAvailableDesktopBounds(), window.getBounds());
            assertEquals(new Insets(0, 0, 0, 0), window.getInsets());
            window.doLayout();
            assertEquals(window.getWidth(), window.getTitleBar().getWidth());
            assertEquals(window.getWidth(), window.getContentHost().getWidth());

            window.restore().minimize();
            assertEquals(WindowState.MINIMIZED, window.getWindowState());
            assertFalse(window.isVisible());
            assertFalse(window.isActive());
            assertTrue(desktop.getMinimizedBar().isVisible());

            window.restore();
            assertEquals(WindowState.NORMAL, window.getWindowState());
            assertTrue(window.isVisible());
            assertEquals(new Rectangle(30, 40, 480, 320), window.getBounds());
            return null;
        });
    }

    @Test
    void maximizedWindowIsFlushByDefaultAndAcceptsOptionalInsets() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel defaultWindow = desktop.openWindow(
                    new WindowConfig("flush-max", "Flush", new JPanel()));
            defaultWindow.maximize();

            assertEquals(new Rectangle(0, 0, 1000, 700), defaultWindow.getBounds());
            assertEquals(0, defaultWindow.getEffectiveShadowSize());
            assertEquals(0, defaultWindow.getEffectiveWindowArc());

            defaultWindow.restore();
            assertEquals(defaultWindow.getWindowStyle().getShadowSize(), defaultWindow.getInsets().left);

            desktop.maximizedInsets(new Insets(12, 16, 20, 24));
            defaultWindow.maximize();
            assertEquals(new Rectangle(16, 12, 960, 668), defaultWindow.getBounds());

            WindowPanel custom = desktop.openWindow(new WindowConfig("custom-max", "Custom", new JPanel())
                    .maximizedInsets(new Insets(5, 6, 7, 8)));
            custom.maximize();
            assertEquals(new Rectangle(6, 5, 986, 688), custom.getBounds());
            return null;
        });
    }

    @Test
    void cancelableEventsPreventCloseAndStateChange() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("locked", "Locked", new JPanel()));
            window.onBeforeClose(WindowEvent::cancel);
            window.onWindowEvent(EventWindowPanel.BEFORE_WINDOW_STATE_CHANGE, WindowEvent::cancel);

            window.close().maximize();

            assertTrue(window.isVisible());
            assertEquals(WindowState.NORMAL, window.getWindowState());
            assertSame(window, desktop.findWindow("locked"));
            return null;
        });
    }

    @Test
    void removeCloseOperationUnregistersWindow() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            AtomicInteger closeEvents = new AtomicInteger();
            desktop.onWindowEvent(EventWindowPanel.WINDOW_CLOSE, event -> closeEvents.incrementAndGet());
            WindowPanel window = desktop.openWindow(new WindowConfig("temporary", "Temporary", new JPanel())
                    .closeOperation(WindowCloseOperation.REMOVE));

            window.close();

            assertFalse(desktop.containsWindow("temporary"));
            assertEquals(1, closeEvents.get());
            return null;
        });
    }

    @Test
    void modalWindowBlocksActivationUntilClosed() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel normal = desktop.openWindow(new WindowConfig("normal", "Normal", new JPanel()));
            WindowPanel modal = desktop.openWindow(new WindowConfig("modal", "Modal", new JPanel()).modal(true));

            assertTrue(desktop.getModalOverlay().isVisible());
            assertFalse(desktop.activateWindow(normal));
            assertSame(modal, desktop.getActiveWindow());

            modal.close();
            assertFalse(desktop.getModalOverlay().isVisible());
            assertTrue(desktop.activateWindow(normal));
            return null;
        });
    }

    @Test
    void capturesAndRestoresKnownWindowsOnly() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel first = desktop.openWindow(new WindowConfig("first", "First", new JPanel())
                    .bounds(new Rectangle(20, 30, 350, 240)));
            WindowPanel second = desktop.openWindow(new WindowConfig("second", "Second", new JPanel())
                    .bounds(new Rectangle(180, 120, 420, 280)));
            second.applySnap(WindowSnap.RIGHT);
            WindowLayoutSnapshot snapshot = desktop.captureLayout();

            first.setBounds(0, 0, 100, 100);
            second.restore();
            desktop.restoreLayout(snapshot);

            assertEquals(new Rectangle(20, 30, 350, 240), first.getBounds());
            assertEquals(WindowSnap.RIGHT, second.getSnap());
            assertSame(second, desktop.getActiveWindow());

            second.restore();
            assertEquals(WindowSnap.NONE, second.getSnap());
            assertEquals(new Rectangle(180, 120, 420, 280), second.getBounds());
            return null;
        });
    }

    @Test
    void duplicateKeysAndCancelableRemovalPreserveRegistration() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel first = desktop.openWindow(new WindowConfig("same", "First", new JPanel()));
            assertThrows(IllegalArgumentException.class,
                    () -> desktop.openWindow(new WindowConfig("same", "Second", new JPanel())));
            desktop.onWindowEvent(EventWindowPanel.BEFORE_WINDOW_REMOVE, WindowEvent::cancel);
            assertFalse(desktop.removeWindow(first));
            assertSame(first, desktop.findWindow("same"));
            return null;
        });
    }

    @Test
    void inheritedHostAndWindowFactoriesKeepDefaultBehavior() throws Exception {
        onEdt(() -> {
            CustomDesktop desktop = new CustomDesktop();
            sizeDesktop(desktop);
            WindowPanel window = desktop.openWindow(new WindowConfig("custom", "Custom", new JPanel()));

            assertInstanceOf(CustomWindow.class, window);
            assertInstanceOf(CustomTitleBar.class, window.getTitleBar());
            window.maximize().restore().close();
            assertFalse(window.isVisible());
            return null;
        });
    }

    @Test
    void contentAndTitleBarFollowWindowResize() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            JPanel content = new JPanel(new BorderLayout());
            JScrollPane scrollPane = new JScrollPane(new JTree());
            content.add(scrollPane, BorderLayout.CENTER);
            WindowPanel window = desktop.openWindow(new WindowConfig("layout", "Layout", content)
                    .bounds(new Rectangle(40, 50, 300, 260)));
            window.doLayout();
            Rectangle initialContent = window.getContentHost().getBounds();

            window.setBounds(40, 50, 620, 480);
            window.doLayout();

            Insets insets = window.getInsets();
            assertEquals(620 - insets.left - insets.right, window.getContentHost().getWidth());
            assertEquals(620 - insets.left - insets.right, window.getTitleBar().getWidth());
            assertTrue(window.getContentHost().getWidth() > initialContent.width);
            assertEquals(480 - insets.top - insets.bottom - window.getTitleBar().getHeight(),
                    window.getContentHost().getHeight());
            assertEquals(window.getContentHost().getSize(), content.getSize());
            assertEquals(content.getSize(), scrollPane.getSize());
            return null;
        });
    }

    @Test
    void delegatedWindowForwardsHooksAndDisposesController() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            TestDelegatedWindow window = new TestDelegatedWindow();
            TestWindowController controller = window.getController();
            desktop.addWindow(window).open();
            window.maximize();

            assertEquals(1, controller.opened);
            assertEquals(1, controller.stateChanges);
            assertSame(window, controller.component());

            desktop.removeWindow(window);
            assertEquals(1, controller.disposed);
            return null;
        });
    }

    @Test
    void delegatedDesktopForwardsHostHooks() throws Exception {
        onEdt(() -> {
            TestDelegatedDesktop desktop = new TestDelegatedDesktop();
            sizeDesktop(desktop);
            TestDesktopController controller = desktop.getController();
            WindowPanel window = desktop.openWindow(new WindowConfig("host-child", "Child", new JPanel()));

            assertEquals(1, controller.added);
            assertEquals(1, controller.activeChanges);
            desktop.minimizedBarContextMenuEnabled(true);
            window.minimize();
            ((WindowDesktopPanel) desktop).performMinimizedMenuAction(
                    window, WindowMinimizedMenuAction.RESTORE);
            assertEquals(1, controller.menuChanges);
            assertEquals(1, controller.beforeMenuActions);
            assertEquals(1, controller.menuActions);
            desktop.snapLayoutsEnabled(false).snapLayoutsEnabled(true);
            assertTrue(desktop.applySnapLayout(window, WindowSnap.RIGHT));
            assertEquals(2, controller.snapLayoutChanges);
            assertEquals(1, controller.beforeSnapLayoutSelections);
            assertEquals(1, controller.snapLayoutSelections);
            desktop.previewSnapLayout(window,
                    List.of(WindowSnap.LEFT, WindowSnap.RIGHT), WindowSnap.LEFT);
            desktop.snapAssistEnabled(false);
            assertEquals(1, controller.snapPreviewChanges);
            assertEquals(1, controller.snapAssistChanges);
            desktop.removeWindow(window);
            assertEquals(1, controller.removed);

            desktop.disposeController();
            assertEquals(1, controller.disposed);
            return null;
        });
    }

    @Test
    void animationEmitsTypedLifecycleInOrder() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = new WindowPanel("animated", "Animated", new JPanel());
            desktop.addWindow(window);
            List<String> order = new ArrayList<>();
            List<WindowAnimationType> types = new ArrayList<>();
            window.onAnimationStart(event -> { order.add("start"); types.add(event.getAnimationType()); });
            window.onAnimationProgress(event -> order.add("progress"));
            window.onAnimationEnd(event -> order.add("end"));

            window.open();

            assertEquals(List.of("start", "progress", "end"), order);
            assertEquals(List.of(WindowAnimationType.OPEN), types);
            assertEquals(1f, window.getAnimationAlpha());
            assertFalse(window.isAnimating());
            return null;
        });
    }

    @Test
    void animationCanBeCanceledByUser() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            HoldingAnimator animator = new HoldingAnimator();
            desktop.windowAnimator(animator);
            WindowPanel window = new WindowPanel("cancel-animation", "Cancel", new JPanel());
            desktop.addWindow(window);
            AtomicInteger canceled = new AtomicInteger();
            window.onAnimationCancel(event -> canceled.incrementAndGet());

            window.open();
            assertTrue(window.isAnimating());
            window.cancelAnimation();

            assertFalse(window.isAnimating());
            assertEquals(1, canceled.get());
            assertEquals(1f, window.getAnimationAlpha());
            return null;
        });
    }

    @Test
    void newCancelableAndPropertyEventsGiveOperationControl() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel first = desktop.openWindow(new WindowConfig("event-first", "First", new JPanel()));
            WindowPanel second = desktop.openWindow(new WindowConfig("event-second", "Second", new JPanel()));
            AtomicInteger titleChanges = new AtomicInteger();
            AtomicInteger boundsChanges = new AtomicInteger();
            first.onWindowEvent(EventWindowPanel.WINDOW_TITLE_CHANGE, event -> titleChanges.incrementAndGet());
            first.onBoundsChange(event -> boundsChanges.incrementAndGet());
            first.onWindowEvent(EventWindowPanel.BEFORE_WINDOW_ACTIVATE, WindowEvent::cancel);

            first.title("Renamed");
            first.setBounds(15, 20, 430, 310);

            assertEquals(1, titleChanges.get());
            assertTrue(boundsChanges.get() >= 1);
            assertFalse(desktop.activateWindow(first));
            assertSame(second, desktop.getActiveWindow());

            WindowDesktopPanel guarded = createDesktop();
            guarded.onWindowEvent(EventWindowPanel.BEFORE_WINDOW_ADD, WindowEvent::cancel);
            guarded.openWindow(new WindowConfig("blocked-add", "Blocked", new JPanel()));
            assertFalse(guarded.containsWindow("blocked-add"));
            return null;
        });
    }

    @Test
    void cursorReflectsDragControlsResizeAndWindowState() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("cursor", "Cursor", new JPanel())
                    .bounds(new Rectangle(30, 30, 420, 300)));
            JLabel title = window.getTitleBar().getTitleLabel();
            WindowControlButton close = window.getControlButton(WindowControl.CLOSE);

            assertEquals(Cursor.DEFAULT_CURSOR, title.getCursor().getType());
            assertTrue(title.getMouseMotionListeners().length > 0);
            assertEquals(Cursor.DEFAULT_CURSOR, close.getCursor().getType());

            window.style(style -> style.titleBarCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)));
            assertEquals(Cursor.MOVE_CURSOR, title.getCursor().getType());

            MouseEvent edgeMove = new MouseEvent(window, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, 1, 1, 0, false);
            for (MouseMotionListener listener : window.getMouseMotionListeners()) listener.mouseMoved(edgeMove);
            assertEquals(Cursor.NW_RESIZE_CURSOR, window.getCursor().getType());

            window.maximize();
            assertEquals(Cursor.DEFAULT_CURSOR, title.getCursor().getType());
            window.restore().movable(false);
            assertEquals(Cursor.DEFAULT_CURSOR, title.getCursor().getType());
            return null;
        });
    }

    @Test
    void minimizedBarIsFixedByDefaultAndRetractableByFlag() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowMinimizedBar bar = desktop.getMinimizedBar();
            bar.animationDuration(0).collapseDelay(60_000);
            AtomicInteger expandedEvents = new AtomicInteger();
            AtomicInteger collapsedEvents = new AtomicInteger();
            desktop.onMinimizedBarExpand(event -> expandedEvents.incrementAndGet());
            desktop.onMinimizedBarCollapse(event -> collapsedEvents.incrementAndGet());

            assertFalse(desktop.isMinimizedBarAutoHideEnabled());
            WindowPanel window = desktop.openWindow(new WindowConfig("taskbar", "Taskbar", new JPanel()));
            window.minimize();

            assertTrue(bar.isVisible());
            assertTrue(bar.isExpanded());
            assertEquals(bar.getExpandedHeight(), bar.getPreferredSize().height);

            desktop.minimizedBarAutoHideEnabled(true).collapseMinimizedBar();
            assertTrue(desktop.isMinimizedBarAutoHideEnabled());
            assertFalse(bar.isExpanded());
            assertEquals(bar.getCollapsedHeight(), bar.getPreferredSize().height);
            assertEquals(1, collapsedEvents.get());

            desktop.expandMinimizedBar();
            assertTrue(bar.isExpanded());
            assertEquals(bar.getExpandedHeight(), bar.getPreferredSize().height);
            assertTrue(expandedEvents.get() >= 1);

            desktop.minimizedBarAutoHideEnabled(false);
            assertTrue(bar.isExpanded());
            return null;
        });
    }

    @Test
    void constructorCanEnableRetractableMinimizedBar() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = new WindowDesktopPanel(true);
            sizeDesktop(desktop);
            assertTrue(desktop.isMinimizedBarAutoHideEnabled());
            return null;
        });
    }

    @Test
    void minimizedBarContextMenuIsOptionalAndProvidesDefaultWindowActions() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("menu", "Editor.java", new JPanel()));
            window.minimize();
            AbstractButton button = (AbstractButton) desktop.getMinimizedBar().getComponent(0);

            assertFalse(desktop.isMinimizedBarContextMenuEnabled());
            assertNull(button.getComponentPopupMenu());

            desktop.minimizedBarContextMenuEnabled(true);
            JPopupMenu menu = button.getComponentPopupMenu();
            assertNotNull(menu);
            assertEquals("Editor.java", ((JMenuItem) menu.getComponent(0)).getText());
            assertEquals("Restaurar", ((JMenuItem) menu.getComponent(2)).getText());
            assertEquals("Maximizar", ((JMenuItem) menu.getComponent(3)).getText());
            assertEquals("Fechar janela", ((JMenuItem) menu.getComponent(5)).getText());

            ((JMenuItem) menu.getComponent(2)).doClick();
            assertEquals(WindowState.NORMAL, window.getWindowState());
            assertTrue(window.isVisible());
            return null;
        });
    }

    @Test
    void minimizedBarMenuPublishesLifecycleAndCancelableActionEvents() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop().minimizedBarContextMenuEnabled(true);
            desktop.minimizedBarAutoHideEnabled(true);
            desktop.getMinimizedBar().animationDuration(0);
            WindowPanel window = desktop.openWindow(new WindowConfig("events-menu", "Events", new JPanel()));
            window.minimize();
            JPopupMenu menu = ((AbstractButton) desktop.getMinimizedBar().getComponent(0))
                    .getComponentPopupMenu();
            AtomicInteger opened = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger performed = new AtomicInteger();
            desktop.onMinimizedBarMenuOpen(event -> opened.incrementAndGet());
            desktop.onMinimizedBarMenuClose(event -> closed.incrementAndGet());
            desktop.onMinimizedBarMenuAction(event -> performed.incrementAndGet());
            desktop.onBeforeMinimizedBarMenuAction(event -> event.cancel());

            PopupMenuEvent popupEvent = new PopupMenuEvent(menu);
            for (PopupMenuListener listener : menu.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeVisible(popupEvent);
            }
            assertTrue(desktop.getMinimizedBar().isPopupActive());
            assertEquals(1, opened.get());
            for (PopupMenuListener listener : menu.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeInvisible(popupEvent);
            }
            assertFalse(desktop.getMinimizedBar().isPopupActive());
            assertEquals(1, closed.get());

            desktop.performMinimizedMenuAction(window, WindowMinimizedMenuAction.CLOSE);
            assertEquals(WindowState.MINIMIZED, window.getWindowState());
            assertTrue(desktop.containsWindow("events-menu"));
            assertEquals(0, performed.get());
            return null;
        });
    }

    @Test
    void minimizedBarAcceptsCustomInheritableMenuFactory() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            AtomicInteger factoryCalls = new AtomicInteger();
            desktop.minimizedBarMenuFactory((host, window) -> {
                factoryCalls.incrementAndGet();
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem("Abrir recente"));
                return menu;
            }).taskbarContextMenuEnabled(true);

            WindowPanel window = desktop.openWindow(new WindowConfig("custom-menu", "Custom", new JPanel()));
            window.minimize();
            JPopupMenu menu = ((AbstractButton) desktop.getMinimizedBar().getComponent(0))
                    .getComponentPopupMenu();

            assertNotNull(menu);
            assertEquals(1, factoryCalls.get());
            assertEquals("Abrir recente", ((JMenuItem) menu.getComponent(0)).getText());
            return null;
        });
    }

    @Test
    void snapLayoutsAreEnabledByDefaultAndApplyWindowsStyleZones() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop()
                    .snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);
            WindowPanel window = desktop.openWindow(new WindowConfig(
                    "snap-layout", "Snap layout", new JPanel()));
            WindowSnapLayoutPopup popup = desktop.getSnapLayoutPopup(window);

            assertTrue(desktop.isSnapLayoutsEnabled());
            assertTrue(window.isSnapLayoutsEnabled());
            assertNotNull(popup);
            assertEquals(16, popup.getZoneButtons().size());

            assertTrue(desktop.applySnapLayout(window, WindowSnap.THIRD_CENTER));
            assertEquals(WindowSnap.THIRD_CENTER, window.getSnap());
            assertEquals(new Rectangle(333, 0, 333, 700), window.getBounds());

            assertTrue(desktop.applySnapLayout(window, WindowSnap.TWO_THIRDS_RIGHT));
            assertEquals(new Rectangle(333, 0, 667, 700), window.getBounds());
            return null;
        });
    }

    @Test
    void snapLayoutsFlagSupportsHostDefaultAndPerWindowOverride() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop()
                    .snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);
            WindowPanel inherited = desktop.openWindow(new WindowConfig(
                    "snap-inherited", "Inherited", new JPanel()));
            WindowPanel disabled = desktop.openWindow(new WindowConfig(
                    "snap-disabled", "Disabled", new JPanel()).snapLayoutsEnabled(false));

            assertNotNull(desktop.getSnapLayoutPopup(inherited));
            assertNull(desktop.getSnapLayoutPopup(disabled));

            desktop.snapLayoutsEnabled(false);
            assertFalse(inherited.isSnapLayoutsEnabled());
            assertNull(desktop.getSnapLayoutPopup(inherited));

            desktop.snapLayoutsEnabled(true);
            assertNotNull(desktop.getSnapLayoutPopup(inherited));
            assertNull(desktop.getSnapLayoutPopup(disabled));

            disabled.inheritSnapLayoutsEnabled();
            assertTrue(disabled.isSnapLayoutsEnabled());
            assertNotNull(desktop.getSnapLayoutPopup(disabled));
            return null;
        });
    }

    @Test
    void snapLayoutPublishesPopupLifecycleAndCancelableSelection() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop()
                    .snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);
            WindowPanel window = desktop.openWindow(new WindowConfig(
                    "snap-events", "Snap events", new JPanel()));
            WindowSnapLayoutPopup popup = desktop.getSnapLayoutPopup(window);
            AtomicInteger opened = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger selected = new AtomicInteger();
            desktop.onSnapLayoutMenuOpen(event -> opened.incrementAndGet());
            desktop.onSnapLayoutMenuClose(event -> closed.incrementAndGet());
            desktop.onSnapLayoutSelect(event -> selected.incrementAndGet());
            desktop.onBeforeSnapLayoutSelect(WindowEvent::cancel);

            PopupMenuEvent popupEvent = new PopupMenuEvent(popup);
            for (PopupMenuListener listener : popup.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeVisible(popupEvent);
                listener.popupMenuWillBecomeInvisible(popupEvent);
            }

            assertEquals(1, opened.get());
            assertEquals(1, closed.get());
            assertFalse(desktop.applySnapLayout(window, WindowSnap.LEFT));
            assertEquals(WindowSnap.NONE, window.getSnap());
            assertEquals(0, selected.get());
            return null;
        });
    }

    @Test
    void snapLayoutPopupCanBeReplacedByInheritance() throws Exception {
        onEdt(() -> {
            CustomSnapDesktop desktop = new CustomSnapDesktop();
            sizeDesktop(desktop);
            desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON);
            WindowPanel window = desktop.openWindow(new WindowConfig(
                    "custom-snap-popup", "Custom popup", new JPanel()));

            assertInstanceOf(CustomSnapPopup.class, desktop.getSnapLayoutPopup(window));
            return null;
        });
    }

    @Test
    void snapLayoutHoverPreviewsSelectedAndRemainingDesktopZones() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig(
                    "preview-source", "Preview", new JPanel()));
            List<WindowSnap> layout = List.of(WindowSnap.LEFT, WindowSnap.RIGHT);
            AtomicInteger previewEvents = new AtomicInteger();
            desktop.onSnapLayoutPreviewChange(event -> previewEvents.incrementAndGet());

            assertTrue(desktop.previewSnapLayout(window, layout, WindowSnap.LEFT));
            assertTrue(desktop.getSnapPreviewOverlay().isVisible());
            assertEquals(new Rectangle(0, 0, 500, 700),
                    desktop.getSnapPreviewOverlay().getSelectedBounds());
            assertEquals(2, desktop.getSnapPreviewOverlay().getZoneBounds().size());

            desktop.clearSnapLayoutPreview();
            assertFalse(desktop.getSnapPreviewOverlay().isVisible());
            assertEquals(2, previewEvents.get());
            return null;
        });
    }

    @Test
    void snapAssistShowsWindowThumbnailsAndFillsRemainingZones() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel editor = desktop.openWindow(new WindowConfig(
                    "assist-editor", "Editor.java", new JTextArea("class Editor {}")));
            WindowPanel properties = desktop.openWindow(new WindowConfig(
                    "assist-properties", "Propriedades", new JTree()));
            List<WindowSnap> layout = List.of(WindowSnap.LEFT, WindowSnap.RIGHT);
            AtomicInteger opened = new AtomicInteger();
            AtomicInteger selected = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            desktop.onSnapAssistOpen(event -> opened.incrementAndGet());
            desktop.onSnapAssistSelect(event -> selected.incrementAndGet());
            desktop.onSnapAssistClose(event -> closed.incrementAndGet());

            assertTrue(desktop.isSnapAssistEnabled());
            assertTrue(desktop.applySnapLayout(editor, layout, WindowSnap.LEFT));
            WindowSnapAssistOverlay assist = desktop.getSnapAssistOverlay();
            assertTrue(assist.isAssistVisible());
            assertEquals(List.of(WindowSnap.RIGHT), assist.getRemainingZones());
            assertEquals(List.of(properties), assist.getCandidates());
            assertEquals(1, assist.getComponentCount());
            assertEquals(new Rectangle(507, 7, 486, 686), assist.getComponent(0).getBounds());
            assertTrue(((Container) assist.getComponent(0)).getComponent(0).getWidth() > 0);
            WindowSnapAssistOverlay.WindowThumbnailButton thumbnail =
                    (WindowSnapAssistOverlay.WindowThumbnailButton)
                            ((Container) assist.getComponent(0)).getComponent(0);
            assertTrue(thumbnail.getThumbnailSize().height >= properties.getHeight());
            assertEquals(1, opened.get());

            assertTrue(desktop.applySnapAssistSelection(properties, WindowSnap.RIGHT));
            assertEquals(WindowSnap.LEFT, editor.getSnap());
            assertEquals(WindowSnap.RIGHT, properties.getSnap());
            assertFalse(assist.isAssistVisible());
            assertEquals(1, selected.get());
            assertEquals(1, closed.get());
            return null;
        });
    }

    @Test
    void snapAssistCanBeDisabledAndItsSelectionCanBeCanceled() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel disabledDesktop = createDesktop().snapAssistEnabled(false);
            WindowPanel first = disabledDesktop.openWindow(new WindowConfig(
                    "assist-disabled-first", "First", new JPanel()));
            disabledDesktop.openWindow(new WindowConfig(
                    "assist-disabled-second", "Second", new JPanel()));
            List<WindowSnap> layout = List.of(WindowSnap.LEFT, WindowSnap.RIGHT);
            assertTrue(disabledDesktop.applySnapLayout(first, layout, WindowSnap.LEFT));
            assertFalse(disabledDesktop.getSnapAssistOverlay().isAssistVisible());

            WindowDesktopPanel guarded = createDesktop();
            WindowPanel source = guarded.openWindow(new WindowConfig(
                    "assist-guard-source", "Source", new JPanel()));
            WindowPanel candidate = guarded.openWindow(new WindowConfig(
                    "assist-guard-candidate", "Candidate", new JPanel()));
            assertTrue(guarded.applySnapLayout(source, layout, WindowSnap.LEFT));
            guarded.onBeforeSnapAssistSelect(WindowEvent::cancel);

            assertFalse(guarded.applySnapAssistSelection(candidate, WindowSnap.RIGHT));
            assertEquals(WindowSnap.NONE, candidate.getSnap());
            assertTrue(guarded.getSnapAssistOverlay().isAssistVisible());
            return null;
        });
    }

    @Test
    void draggingToTopCenterOpensSelectorAndAppliesSelectedLayout() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel source = desktop.openWindow(new WindowConfig(
                    "drag-layout-source", "Drag source", new JPanel()));
            WindowPanel candidate = desktop.openWindow(new WindowConfig(
                    "drag-layout-candidate", "Drag candidate", new JPanel()));
            AtomicInteger opened = new AtomicInteger();
            AtomicInteger closed = new AtomicInteger();
            desktop.onSnapLayoutDragOpen(event -> opened.incrementAndGet());
            desktop.onSnapLayoutDragClose(event -> closed.incrementAndGet());

            assertEquals(WindowSnapLayoutTrigger.TOP_CENTER, desktop.getSnapLayoutTrigger());
            assertTrue(desktop.updateSnapLayoutDrag(source, new Point(500, 0)));
            assertTrue(desktop.getSnapDragSelector().isVisible());
            assertNotNull(desktop.getSnapDragSelector().getSelection());
            assertTrue(desktop.getSnapPreviewOverlay().isVisible());
            WindowSnap selected = desktop.getSnapDragSelector().getSelection().getSnap();

            assertTrue(desktop.completeSnapLayoutDrag(source));
            assertEquals(selected, source.getSnap());
            assertFalse(desktop.getSnapDragSelector().isVisible());
            assertTrue(desktop.getSnapAssistOverlay().isAssistVisible());
            assertEquals(List.of(candidate), desktop.getSnapAssistOverlay().getCandidates());
            assertEquals(1, opened.get());
            assertEquals(1, closed.get());
            return null;
        });
    }

    @Test
    void snapLayoutTriggerEnumChoosesButtonTopCenterBothOrDisabled() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel inherited = desktop.openWindow(new WindowConfig(
                    "trigger-inherited", "Inherited", new JPanel()));
            WindowPanel buttonOnly = desktop.openWindow(new WindowConfig(
                    "trigger-button", "Button", new JPanel())
                    .snapLayoutTrigger(WindowSnapLayoutTrigger.MAXIMIZE_BUTTON));

            desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.TOP_CENTER);
            assertNull(desktop.getSnapLayoutPopup(inherited));
            assertTrue(desktop.updateSnapLayoutDrag(inherited, new Point(500, 0)));
            desktop.cancelSnapLayoutDrag();
            assertNotNull(desktop.getSnapLayoutPopup(buttonOnly));
            assertFalse(desktop.updateSnapLayoutDrag(buttonOnly, new Point(500, 0)));

            desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.DISABLED);
            assertNull(desktop.getSnapLayoutPopup(inherited));
            assertNotNull(desktop.getSnapLayoutPopup(buttonOnly));
            assertFalse(desktop.updateSnapLayoutDrag(inherited, new Point(500, 0)));

            buttonOnly.inheritSnapLayoutTrigger();
            assertNull(desktop.getSnapLayoutPopup(buttonOnly));
            desktop.snapLayoutTrigger(WindowSnapLayoutTrigger.BOTH);
            assertNotNull(desktop.getSnapLayoutPopup(buttonOnly));
            assertTrue(desktop.updateSnapLayoutDrag(buttonOnly, new Point(500, 0)));
            return null;
        });
    }

    @Test
    void minimizedBarAcceptsCustomButtonFactoryAndRebuildsExistingButtons() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("btn", "Editor.java", new JPanel()));
            window.minimize();
            assertEquals(1, desktop.getMinimizedBar().getComponentCount());

            AtomicInteger calls = new AtomicInteger();
            desktop.minimizedBarButtonFactory((bar, target) -> {
                calls.incrementAndGet();
                return new JButton("custom:" + target.getTitle());
            });

            assertEquals(1, calls.get());
            assertEquals(1, desktop.getMinimizedBar().getComponentCount());
            AbstractButton button = (AbstractButton) desktop.getMinimizedBar().getComponent(0);
            assertEquals("custom:Editor.java", button.getText());
            assertSame(desktop.getMinimizedBar().getButtonFactory(), desktop.getMinimizedButtonFactory());
            return null;
        });
    }

    @Test
    void overriddenButtonHookTakesPrecedenceOverButtonFactory() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = new WindowDesktopPanel() {
                @Override
                protected AbstractButton createMinimizedWindowButton(WindowPanel window) {
                    return new JButton("override:" + window.getTitle());
                }
            };
            sizeDesktop(desktop);
            desktop.minimizedBarButtonFactory((bar, window) -> new JButton("factory"));

            WindowPanel window = desktop.openWindow(new WindowConfig("prec", "Props", new JPanel()));
            window.minimize();

            assertEquals("override:Props", ((AbstractButton) desktop.getMinimizedBar().getComponent(0)).getText());
            return null;
        });
    }

    @Test
    void minimizedBarFactoryIsUsedUnlessTheHookIsOverridden() throws Exception {
        onEdt(() -> {
            WindowMinimizedBar injected = new WindowMinimizedBar();
            WindowDesktopPanel desktop = new WindowDesktopPanel(false, host -> injected);
            sizeDesktop(desktop);
            assertSame(injected, desktop.getMinimizedBar());

            WindowMinimizedBar ignored = new WindowMinimizedBar();
            WindowMinimizedBar overridden = new WindowMinimizedBar();
            WindowDesktopPanel subclass = new WindowDesktopPanel(false, host -> ignored) {
                @Override
                protected WindowMinimizedBar createMinimizedWindowBar() {
                    return overridden;
                }
            };
            sizeDesktop(subclass);
            assertSame(overridden, subclass.getMinimizedBar());
            return null;
        });
    }

    @Test
    void minimizedButtonIconIsScaledDownToFitTheBar() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("icon", "Props", new JPanel())
                    .icon(new SquareIcon(48)));
            window.minimize();

            AbstractButton button = (AbstractButton) desktop.getMinimizedBar().getComponent(0);
            Icon icon = button.getIcon();
            assertNotNull(icon);
            assertTrue(icon.getIconHeight() <= desktop.getMinimizedBar().getExpandedHeight() - 18);
            assertEquals(icon.getIconWidth(), icon.getIconHeight());
            assertTrue(button.getPreferredSize().height >= icon.getIconHeight());
            return null;
        });
    }

    @Test
    void fittedIconPreservesAspectRatioAndSkipsSmallIcons() {
        Icon small = new SquareIcon(12);
        assertSame(small, FittedIcon.fit(small, 16));
        assertNull(FittedIcon.fit(null, 16));

        Icon wide = new SizedIcon(64, 32);
        Icon fitted = FittedIcon.fit(wide, 16);
        assertNotSame(wide, fitted);
        assertEquals(16, fitted.getIconWidth());
        assertEquals(8, fitted.getIconHeight());
    }

    private static class SizedIcon implements Icon {
        private final int width;
        private final int height;

        SizedIcon(int width, int height) { this.width = width; this.height = height; }

        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.fillRect(x, y, width, height);
        }
    }

    private static class SquareIcon extends SizedIcon {
        SquareIcon(int size) { super(size, size); }
    }

    @Test
    void popupHeaderIconIsScaledAndKeepsItsColorsWhileDisabled() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop().minimizedBarContextMenuEnabled(true);
            WindowPanel window = desktop.openWindow(new WindowConfig("popup-icon", "Props", new JPanel())
                    .icon(new SquareIcon(64)));
            window.minimize();

            JPopupMenu menu = ((AbstractButton) desktop.getMinimizedBar().getComponent(0))
                    .getComponentPopupMenu();
            JMenuItem header = (JMenuItem) menu.getComponent(0);
            assertEquals("Props", header.getText());
            assertFalse(header.isEnabled());
            assertEquals(16, header.getIcon().getIconWidth());
            assertEquals(16, header.getIcon().getIconHeight());
            assertSame(header.getIcon(), header.getDisabledIcon());
            return null;
        });
    }

    @Test
    void titleBarIconIsScaledToTheTitleBarHeight() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel big = desktop.openWindow(new WindowConfig("big", "Grande", new JPanel())
                    .icon(new SquareIcon(64)));
            WindowPanel small = desktop.openWindow(new WindowConfig("small", "Pequeno", new JPanel())
                    .icon(new SquareIcon(16)));
            WindowPanel none = desktop.openWindow(new WindowConfig("none", "Sem icone", new JPanel()));

            Icon scaled = big.getTitleBar().getIconLabel().getIcon();
            int limit = Math.max(12, Math.min(20, big.getWindowStyle().getTitleBarHeight() - 20));
            assertEquals(limit, scaled.getIconWidth());
            assertEquals(limit, scaled.getIconHeight());
            assertTrue(big.getTitleBar().getIconLabel().isVisible());

            assertEquals(16, small.getTitleBar().getIconLabel().getIcon().getIconWidth());
            assertNull(none.getTitleBar().getIconLabel().getIcon());
            assertFalse(none.getTitleBar().getIconLabel().isVisible());
            return null;
        });
    }

    @Test
    void titleBarIconIsVerticallyCenteredInTheTitleBar() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("centered", "Propriedades", new JPanel())
                    .bounds(new Rectangle(10, 10, 300, 200))
                    .icon(new SquareIcon(16)));

            WindowTitleBar bar = window.getTitleBar();
            int barHeight = window.getWindowStyle().getTitleBarHeight();
            bar.setSize(272, barHeight);
            bar.doLayout();
            for (Component child : bar.getComponents()) {
                child.doLayout();
            }

            JLabel icon = bar.getIconLabel();
            assertEquals(16, icon.getHeight());
            assertEquals((barHeight - icon.getHeight()) / 2, icon.getY(),
                    "o icone deve ficar centralizado verticalmente na title bar");
            return null;
        });
    }

    @Test
    void titleBarMarginsAreConfigurableThroughTheWindowStyle() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("margins", "Propriedades", new JPanel())
                    .bounds(new Rectangle(10, 10, 300, 200))
                    .icon(new SquareIcon(16)));
            WindowTitleBar bar = window.getTitleBar();

            assertEquals(new Insets(0, 10, 0, 4), bar.getBorder().getBorderInsets(bar));
            assertEquals(8, ((FlowLayout) bar.getIconLabel().getParent().getLayout()).getHgap());

            window.style(style -> style.titleBarInsets(2, 24, 2, 16).titleBarIconGap(14));

            assertEquals(new Insets(2, 24, 2, 16), bar.getBorder().getBorderInsets(bar));
            assertEquals(14, ((FlowLayout) bar.getIconLabel().getParent().getLayout()).getHgap());

            window.style(style -> style.titleBarMargin(30));
            assertEquals(new Insets(2, 30, 2, 30), bar.getBorder().getBorderInsets(bar));

            WindowStyle copy = window.getWindowStyle().copy();
            assertEquals(new Insets(2, 30, 2, 30), copy.getTitleBarInsets());
            assertEquals(14, copy.getTitleBarIconGap());
            return null;
        });
    }

    @Test
    void titleBarIconGapDoesNotLeakIntoTheOuterMargin() throws Exception {
        onEdt(() -> {
            WindowDesktopPanel desktop = createDesktop();
            WindowPanel window = desktop.openWindow(new WindowConfig("gap", "Propriedades", new JPanel())
                    .bounds(new Rectangle(10, 10, 340, 200))
                    .icon(new SquareIcon(16)));
            WindowPanel bare = desktop.openWindow(new WindowConfig("bare", "Sem icone", new JPanel())
                    .bounds(new Rectangle(10, 220, 340, 200)));

            // Margem zero deve encostar o icone na borda: o gap vale so entre componentes.
            window.style(style -> style.titleBarInsets(0, 0, 0, 0));
            assertEquals(0, absoluteIconX(window));

            window.style(style -> style.titleBarMargin(24));
            assertEquals(24, absoluteIconX(window));

            // Um leading vazio nao pode reservar largura fantasma.
            assertEquals(0, bare.getTitleBar().getIconLabel().getParent().getPreferredSize().width);
            assertTrue(window.getTitleBar().getIconLabel().getParent().getPreferredSize().width >= 16);
            return null;
        });
    }

    private static int absoluteIconX(WindowPanel window) {
        WindowTitleBar bar = window.getTitleBar();
        bar.setSize(300, window.getWindowStyle().getTitleBarHeight());
        bar.doLayout();
        Container leading = bar.getIconLabel().getParent();
        leading.doLayout();
        return leading.getX() + bar.getIconLabel().getX();
    }

    private static WindowDesktopPanel createDesktop() {
        WindowDesktopPanel desktop = new WindowDesktopPanel();
        sizeDesktop(desktop);
        return desktop;
    }

    private static void assertPosition(WindowDesktopPanel desktop, String key,
                                       WindowPosition position, int x, int y) {
        WindowPanel window = desktop.openWindow(new WindowConfig(key, key, new JPanel())
                .bounds(new Rectangle(99, 88, 300, 200))
                .position(position));
        assertEquals(new Rectangle(x, y, 300, 200), window.getBounds());
        assertEquals(position, window.getInitialPosition());
    }

    private static void sizeDesktop(WindowDesktopPanel desktop) {
        desktop.setSize(1000, 700);
        desktop.doLayout();
        desktop.getLayeredPane().setSize(1000, 700);
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        final Object[] result = new Object[1];
        final Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try { result[0] = action.call(); } catch (Throwable throwable) { failure[0] = throwable; }
        });
        if (failure[0] instanceof Exception exception) throw exception;
        if (failure[0] instanceof Error error) throw error;
        if (failure[0] != null) throw new InvocationTargetException(failure[0]);
        @SuppressWarnings("unchecked") T value = (T) result[0];
        return value;
    }

    private static class CustomDesktop extends WindowDesktopPanel {
        @Override protected WindowPanel createWindow(WindowConfig config) { return new CustomWindow(config); }
    }

    private static class CustomWindow extends WindowPanel {
        CustomWindow(WindowConfig config) { super(config); }
        @Override protected WindowTitleBar createTitleBar() { return new CustomTitleBar(this); }
    }

    private static class CustomTitleBar extends WindowTitleBar {
        CustomTitleBar(WindowPanel window) { super(window); }
    }

    private static class CustomSnapDesktop extends WindowDesktopPanel {
        @Override protected WindowSnapLayoutPopup createSnapLayoutPopup(WindowPanel window) {
            return new CustomSnapPopup(this, window);
        }
    }

    private static class CustomSnapPopup extends WindowSnapLayoutPopup {
        CustomSnapPopup(WindowDesktopPanel desktop, WindowPanel window) {
            super(desktop, window);
        }
    }

    private static class TestDelegatedWindow extends DelegatedWindowPanel<TestWindowController> {
        TestDelegatedWindow() {
            super(new WindowConfig("delegated", "Delegated", new JPanel())
                    .closeOperation(WindowCloseOperation.REMOVE));
        }
        @Override protected TestWindowController newController() { return new TestWindowController(); }
    }

    private static class TestWindowController extends AbstractWindowPanelController {
        int opened;
        int stateChanges;
        int disposed;
        @Override public void onWindowOpen(WindowPanel window) { component = window; opened++; }
        @Override public void onStateChanged(WindowPanel window, WindowState oldState, WindowState newState) { stateChanges++; }
        @Override public void onDispose(WindowPanel window) { disposed++; }
        WindowPanel component() { return component; }
    }

    private static class TestDelegatedDesktop extends DelegatedWindowDesktopPanel<TestDesktopController> {
        @Override protected TestDesktopController newController() { return new TestDesktopController(); }
    }

    private static class TestDesktopController extends AbstractWindowDesktopController {
        int added;
        int removed;
        int activeChanges;
        int menuChanges;
        int beforeMenuActions;
        int menuActions;
        int snapLayoutChanges;
        int beforeSnapLayoutSelections;
        int snapLayoutSelections;
        int snapPreviewChanges;
        int snapAssistChanges;
        int disposed;
        @Override public void onWindowAdded(WindowDesktopPanel desktop, WindowPanel window) { added++; }
        @Override public void onWindowRemoved(WindowDesktopPanel desktop, WindowPanel window) { removed++; }
        @Override public void onActiveWindowChanged(WindowDesktopPanel desktop, WindowPanel oldWindow, WindowPanel newWindow) {
            activeChanges++;
        }
        @Override public void onMinimizedBarContextMenuChanged(WindowDesktopPanel desktop, WindowEvent event) {
            menuChanges++;
        }
        @Override public void onBeforeMinimizedBarMenuAction(WindowDesktopPanel desktop, WindowEvent event) {
            beforeMenuActions++;
        }
        @Override public void onMinimizedBarMenuAction(WindowDesktopPanel desktop, WindowEvent event) {
            menuActions++;
        }
        @Override public void onSnapLayoutsChanged(WindowDesktopPanel desktop, WindowEvent event) {
            snapLayoutChanges++;
        }
        @Override public void onBeforeSnapLayoutSelect(WindowDesktopPanel desktop, WindowEvent event) {
            beforeSnapLayoutSelections++;
        }
        @Override public void onSnapLayoutSelected(WindowDesktopPanel desktop, WindowEvent event) {
            snapLayoutSelections++;
        }
        @Override public void onSnapLayoutPreviewChanged(WindowDesktopPanel desktop, WindowEvent event) {
            snapPreviewChanges++;
        }
        @Override public void onSnapAssistChanged(WindowDesktopPanel desktop, WindowEvent event) {
            snapAssistChanges++;
        }
        @Override public void onDispose(WindowDesktopPanel desktop) { disposed++; }
    }

    private static class HoldingAnimator implements WindowAnimator {
        private WindowPanel activeWindow;

        @Override
        public void animate(WindowAnimationRequest request, Consumer<WindowAnimationFrame> frames,
                            Runnable completion) {
            activeWindow = request.getWindow();
            frames.accept(new WindowAnimationFrame(.25f, request.getFromBounds(), request.getFromAlpha()));
        }

        @Override
        public boolean cancel(WindowPanel window) {
            if (activeWindow != window) return false;
            activeWindow = null;
            return true;
        }
    }
}
