package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JTextFieldListenerClearButtonTest {

    @Test
    void reservesLeftSpaceForLeadingIcon() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.setBorder(BorderFactory.createEmptyBorder());
            int withoutIcon = field.getInsets().left;

            field.setIcon(new SquareIcon(16));
            field.setIconGap(6);

            assertEquals(withoutIcon + 16 + 6, field.getInsets().left);

            field.setIcon(null);
            assertEquals(withoutIcon, field.getInsets().left);
            return null;
        });
    }

    @Test
    void reservesRightSpaceOnlyWhileClearButtonIsEnabled() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setIconGap(6);

            assertTrue(field.isClearButtonEnabled());
            int reserved = field.getInsets().right;

            field.setClearButtonEnabled(false);
            int bare = field.getInsets().right;
            assertEquals(16 + 6, reserved - bare);

            field.setClearButtonEnabled(true);
            field.setEditable(false);
            assertEquals(bare, field.getInsets().right);
            return null;
        });
    }

    @Test
    void clearButtonBecomesClickableOnlyWithText() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setSize(200, 28);

            AtomicInteger cleared = new AtomicInteger();
            AtomicReference<Object> clearedValue = new AtomicReference<>();
            field.addEventListener(EventType.CLEAR, event -> {
                cleared.incrementAndGet();
                clearedValue.set(event.getValue());
            });

            paint(field);
            clickAt(field, 200 - 8, 14);
            assertEquals(0, cleared.get());

            field.setText("hello");
            paint(field);
            clickAt(field, 200 - 8, 14);

            assertEquals(1, cleared.get());
            assertEquals("", clearedValue.get());
            assertEquals("", field.getText());
            return null;
        });
    }

    @Test
    void clickOutsideClearButtonDoesNotClear() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setSize(200, 28);
            field.setText("hello");

            AtomicInteger cleared = new AtomicInteger();
            field.addEventListener(EventType.CLEAR, event -> cleared.incrementAndGet());

            paint(field);
            clickAt(field, 40, 14);

            assertEquals(0, cleared.get());
            assertEquals("hello", field.getText());
            return null;
        });
    }

    @Test
    void disabledClearButtonIgnoresClicks() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setSize(200, 28);
            field.setClearButtonEnabled(false);
            field.setText("hello");

            AtomicInteger cleared = new AtomicInteger();
            field.addEventListener(EventType.CLEAR, event -> cleared.incrementAndGet());

            paint(field);
            clickAt(field, 200 - 8, 14);

            assertFalse(field.isClearButtonEnabled());
            assertEquals(0, cleared.get());
            assertEquals("hello", field.getText());
            return null;
        });
    }

    @Test
    void maskedFieldKeepsIconAndClearSupport() throws Exception {
        onEdt(() -> {
            MaskedTextField field = new MaskedTextField();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setSize(200, 28);
            field.setPlaceholder("Buscar");
            field.setIconGap(6);
            int withoutIcon = field.getInsets().left;
            field.setIcon(new SquareIcon(15));
            field.setText("nota");

            assertEquals(withoutIcon + 15 + 6, field.getInsets().left);
            paint(field);

            AtomicInteger cleared = new AtomicInteger();
            field.addEventListener(EventType.CLEAR, event -> cleared.incrementAndGet());
            clickAt(field, 200 - 8, 14);

            assertEquals(1, cleared.get());
            assertEquals("", field.getText());
            return null;
        });
    }

    private static void paint(JTextFieldListener field) {
        BufferedImage image = new BufferedImage(Math.max(1, field.getWidth()),
                Math.max(1, field.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            field.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    private static void clickAt(JTextFieldListener field, int x, int y) {
        MouseEvent event = new MouseEvent(field, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener listener : field.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
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
        if (throwable instanceof Exception exception) throw exception;
        if (throwable instanceof Error error) throw error;
        if (throwable != null) throw new InvocationTargetException(throwable);
        return result.get();
    }

    private static final class SquareIcon implements Icon {

        private final int size;

        private SquareIcon(int size) {
            this.size = size;
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(Color.RED);
            g.fillRect(x, y, size, size);
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
