package dtm.stools.component.inputfields.switchfield;

import dtm.stools.component.events.EventType;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchFieldTest {

    @Test
    void supportsResponsiveAndExplicitGeometry() throws Exception {
        onEdt(() -> {
            TestSwitchField field = new TestSwitchField();
            assertEquals(new Dimension(56, 30), field.getPreferredSize());
            assertFalse(field.isFocusPainted());

            field.setSize(56, 30);
            assertEquals(new Rectangle(0, 0, 56, 30), field.switchBounds());

            field.setShowText(true);
            assertEquals(new Dimension(72, 30), field.getPreferredSize());

            field.setSwitchSize(92, 42)
                    .setTrackInsets(new Insets(1, 2, 3, 4))
                    .setThumbSize(20)
                    .setThumbPadding(4)
                    .setTrackArc(12)
                    .setFocusStrokeWidth(3f)
                    .setFocusGap(2)
                    .setFocusPainted(true);

            assertEquals(new Dimension(92, 42), field.getPreferredSize());
            assertEquals(new Insets(1, 2, 3, 4), field.getTrackInsets());
            assertEquals(20, field.getThumbSize());
            assertEquals(4, field.getThumbPadding());
            assertEquals(12, field.getTrackArc());
            assertEquals(3f, field.getFocusStrokeWidth());
            assertEquals(2, field.getFocusGap());
            assertTrue(field.isFocusPainted());

            field.setSize(18, 10);
            Rectangle bounds = field.switchBounds();
            assertTrue(bounds.x >= 0 && bounds.y >= 0);
            assertTrue(bounds.x + bounds.width <= field.getWidth());
            assertTrue(bounds.y + bounds.height <= field.getHeight());
            return null;
        });
    }

    @Test
    void validatesGeometryAndKeepsSelectionEventsCompatible() throws Exception {
        onEdt(() -> {
            SwitchField field = new SwitchField();
            AtomicInteger changes = new AtomicInteger();
            AtomicInteger switchOns = new AtomicInteger();
            AtomicReference<Boolean> propertyValue = new AtomicReference<>();
            field.addEventListener(EventType.CHANGE, event -> changes.incrementAndGet());
            field.addEventListener(SwitchField.SWITCH_ON, event -> switchOns.incrementAndGet());
            field.addPropertyChangeListener("selected", event -> propertyValue.set((Boolean) event.getNewValue()));

            field.setSelected(true);
            assertTrue(field.isSelected());
            assertEquals(1, changes.get());
            assertEquals(1, switchOns.get());
            assertEquals(Boolean.TRUE, propertyValue.get());

            field.setSelected(false, false);
            assertFalse(field.isSelected());
            assertEquals(1, changes.get());

            assertThrows(IllegalArgumentException.class, () -> field.setSwitchSize(0, 20));
            assertThrows(IllegalArgumentException.class, () -> field.setTrackInsets(new Insets(-1, 0, 0, 0)));
            assertThrows(IllegalArgumentException.class, () -> field.setThumbSize(-1));
            assertThrows(IllegalArgumentException.class, () -> field.setFocusStrokeWidth(0f));
            return null;
        });
    }

    @Test
    void reversesAnimationFromItsCurrentPosition() throws Exception {
        onEdt(() -> {
            ShowingSwitchField field = new ShowingSwitchField();
            field.setAnimationDuration(10_000);
            field.setSelected(true, false);
            Thread.sleep(20);
            field.tick();
            float beforeReverse = field.progress();

            field.setSelected(false, false);
            field.tick();
            float afterReverse = field.progress();
            field.setAnimated(false);

            assertTrue(beforeReverse > 0f && beforeReverse < 0.1f);
            assertTrue(Math.abs(afterReverse - beforeReverse) < 0.05f,
                    "Reversing must not jump the thumb to the opposite endpoint");
            return null;
        });
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

    private static class TestSwitchField extends SwitchField {
        Rectangle switchBounds() {
            return getSwitchBounds();
        }
    }

    private static class ShowingSwitchField extends SwitchField {
        @Override
        public boolean isShowing() {
            return true;
        }

        void tick() {
            updateAnimation();
        }

        float progress() {
            return getAnimationProgress();
        }
    }
}
