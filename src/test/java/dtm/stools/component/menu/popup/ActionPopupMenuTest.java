package dtm.stools.component.menu.popup;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ActionPopupMenuTest {

    @Test
    void usesTheMouseEventComponentWhileItIsShowing() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point frameLocation = safeScreenLocation();
        JFrame frame = new JFrame();
        JPanel source = new JPanel();
        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", event -> {
        }));

        try {
            runOnEdt(() -> {
                frame.setContentPane(source);
                frame.setSize(320, 240);
                frame.setLocation(frameLocation);
                frame.setVisible(true);
            });

            Point localPoint = new Point(40, 50);
            Point screenPoint = callOnEdt(() -> {
                Point point = source.getLocationOnScreen();
                point.translate(localPoint.x, localPoint.y);
                return point;
            });
            MouseEvent event = mouseEvent(source, localPoint, screenPoint);

            runOnEdt(() -> menu.showAt(event));

            assertSame(source, callOnEdt(menu::getInvoker));
            assertEquals(screenPoint, callOnEdt(menu::getLocationOnScreen));
        } finally {
            hide(menu);
            runOnEdt(frame::dispose);
        }
    }

    @Test
    void usesAnAnchorWhenTheMouseEventComponentIsNotShowing() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point location = safeScreenLocation();
        JPanel hiddenSource = new JPanel();
        MouseEvent event = mouseEvent(hiddenSource, new Point(5, 7), location);
        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", ignored -> {
        }));

        try {
            runOnEdt(() -> menu.showAt(event));

            Window anchor = callOnEdt(() -> SwingUtilities.getWindowAncestor(menu.getInvoker()));

            assertInstanceOf(JDialog.class, anchor);
            assertNotSame(hiddenSource, callOnEdt(menu::getInvoker));
            assertEquals(location, callOnEdt(anchor::getLocation));
        } finally {
            hide(menu);
        }
    }

    @Test
    void opensAtScreenCoordinatesWithoutAVisibleComponent() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point location = safeScreenLocation();
        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", event -> {
        }));

        try {
            runOnEdt(() -> menu.showAt(location.x, location.y));

            assertTrue(callOnEdt(menu::isVisible));

            Window anchor = callOnEdt(() -> SwingUtilities.getWindowAncestor(menu.getInvoker()));

            assertInstanceOf(JDialog.class, anchor);
            assertEquals(location, callOnEdt(anchor::getLocation));
            assertSame(anchor, callOnEdt(() -> SwingUtilities.getWindowAncestor(menu)));
        } finally {
            hide(menu);
        }
    }

    @Test
    void usesAnAnchorWhenTheMouseEventHasNoSwingComponent() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point location = safeScreenLocation();
        JPanel syntheticSource = new JPanel();
        MouseEvent event = mouseEvent(syntheticSource, new Point(), location);
        event.setSource(new Object());

        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", ignored -> {
        }));

        try {
            runOnEdt(() -> menu.showAt(event));

            assertTrue(callOnEdt(menu::isVisible));

            Window anchor = callOnEdt(() -> SwingUtilities.getWindowAncestor(menu.getInvoker()));

            assertInstanceOf(JDialog.class, anchor);
            assertEquals(location, callOnEdt(anchor::getLocation));
        } finally {
            hide(menu);
        }
    }

    @Test
    void closesTheAnchorAfterAnOutsideMousePress() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point location = safeScreenLocation();
        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", event -> {
        }));

        try {
            runOnEdt(() -> menu.showAt(location.x, location.y));
            Window anchor = callOnEdt(() -> SwingUtilities.getWindowAncestor(menu.getInvoker()));
            Rectangle popupBounds = callOnEdt(() -> new Rectangle(menu.getLocationOnScreen(), menu.getSize()));
            Point outside = new Point(
                    popupBounds.x + popupBounds.width + 20,
                    popupBounds.y + popupBounds.height + 20
            );
            JPanel outsideSource = new JPanel();
            MouseEvent outsidePress = new MouseEvent(
                    outsideSource,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    0,
                    0,
                    outside.x,
                    outside.y,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );

            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(outsidePress);

            awaitHidden(menu);
            assertFalse(callOnEdt(anchor::isDisplayable));
        } finally {
            hide(menu);
        }
    }

    @Test
    void closesWhenAnotherWindowReceivesFocus() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o popup precisa de ambiente grafico");

        Point location = safeScreenLocation();
        ActionPopupMenu menu = callOnEdt(() -> ActionPopupMenu.create().item("Abrir", event -> {
        }));
        JDialog otherWindow = callOnEdt(() -> new JDialog((Frame) null));

        try {
            runOnEdt(() -> menu.showAt(location.x, location.y));
            Window anchor = callOnEdt(() -> SwingUtilities.getWindowAncestor(menu));

            awaitFocused(anchor);

            runOnEdt(() -> {
                otherWindow.setSize(120, 80);
                otherWindow.setLocation(location.x + 300, location.y + 200);
                otherWindow.setVisible(true);
                otherWindow.toFront();
                otherWindow.requestFocus();
            });

            awaitHidden(menu);
            assertFalse(callOnEdt(anchor::isDisplayable));
        } finally {
            hide(menu);
            runOnEdt(otherWindow::dispose);
        }
    }

    private static Point safeScreenLocation() {
        GraphicsConfiguration configuration = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);

        return new Point(
                bounds.x + insets.left + 100,
                bounds.y + insets.top + 100
        );
    }

    private static MouseEvent mouseEvent(Component source, Point local, Point screen) {
        return new MouseEvent(
                source,
                MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                0,
                local.x,
                local.y,
                screen.x,
                screen.y,
                1,
                true,
                MouseEvent.BUTTON3
        );
    }

    private static void awaitHidden(ActionPopupMenu menu) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline && callOnEdt(menu::isVisible)) {
            Thread.sleep(25);
        }

        runOnEdt(() -> {
        });
        assertFalse(callOnEdt(menu::isVisible), "o popup deveria fechar depois do clique externo");
    }

    private static void awaitFocused(Window window) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline && !callOnEdt(window::isFocused)) {
            Thread.sleep(25);
        }

        assertTrue(callOnEdt(window::isFocused), "a ancora deveria receber o foco do sistema");
    }

    private static void hide(ActionPopupMenu menu) throws Exception {
        if (menu == null) {
            return;
        }

        runOnEdt(() -> menu.setVisible(false));
        runOnEdt(() -> {
        });
    }

    private static void runOnEdt(Runnable action) throws Exception {
        callOnEdt(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T callOnEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.call();
        }

        Object[] result = new Object[1];
        Throwable[] failure = new Throwable[1];

        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = action.call();
            } catch (Throwable throwable) {
                failure[0] = throwable;
            }
        });

        if (failure[0] != null) {
            if (failure[0] instanceof Exception exception) {
                throw exception;
            }

            if (failure[0] instanceof Error error) {
                throw error;
            }

            throw new RuntimeException(failure[0]);
        }

        @SuppressWarnings("unchecked")
        T typedResult = (T) result[0];
        return typedResult;
    }
}
