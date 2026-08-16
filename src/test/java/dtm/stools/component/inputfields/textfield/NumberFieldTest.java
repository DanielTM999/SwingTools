package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberFieldTest {

    @Test
    void parsesLocaleTextAndClampsOnSubmit() throws Exception {
        onEdt(() -> {
            NumberField field = new NumberField(Locale.forLanguageTag("pt-BR"))
                    .setRange(new BigDecimal("-10"), new BigDecimal("10"));
            AtomicInteger changes = new AtomicInteger();
            AtomicInteger submits = new AtomicInteger();
            AtomicReference<BigDecimal> submitted = new AtomicReference<>();
            field.addEventListener(EventType.CHANGE, event -> changes.incrementAndGet());
            field.addEventListener(EventType.SUBMIT, event -> {
                submits.incrementAndGet();
                submitted.set(event.tryGetValue());
            });

            field.setText("12,34");
            assertEquals(0, new BigDecimal("12.34").compareTo(field.getValue()));
            assertFalse(field.isValueWithinRange());

            field.postActionEvent();
            assertEquals("10", field.getText());
            assertEquals(0, new BigDecimal("10.00").compareTo(field.getValue()));
            assertEquals(1, changes.get());
            assertEquals(1, submits.get());
            assertEquals(0, new BigDecimal("10").compareTo(submitted.get()));
            assertTrue(field.isValueWithinRange());
            return null;
        });
    }

    @Test
    void supportsEmptyAndRejectsInvalidOrExcessPrecision() throws Exception {
        onEdt(() -> {
            NumberField field = new NumberField(Locale.US).setDecimalPlaces(2);
            field.setText("-");
            assertNull(field.getValue());
            loseFocus(field);
            assertEquals("", field.getText());
            assertNull(field.getValue());

            field.setText("1.25");
            field.selectAll();
            field.replaceSelection("letters");
            assertEquals("1.25", field.getText());

            field.selectAll();
            field.replaceSelection("1.234");
            assertEquals("1.25", field.getText());
            return null;
        });
    }

    @Test
    void configuresPrecisionStepLocaleAndProgrammaticEvents() throws Exception {
        onEdt(() -> {
            NumberField field = new NumberField(Locale.US)
                    .setRoundingMode(RoundingMode.HALF_UP)
                    .setDecimalPlaces(2)
                    .setRange(BigDecimal.ZERO, new BigDecimal("2"))
                    .setStep(new BigDecimal("0.25"));
            AtomicInteger inputs = new AtomicInteger();
            AtomicInteger changes = new AtomicInteger();
            field.addEventListener(EventType.INPUT, event -> inputs.incrementAndGet());
            field.addEventListener(EventType.CHANGE, event -> changes.incrementAndGet());

            field.setValue(new BigDecimal("1.126"));
            assertEquals("1.13", field.getText());
            assertEquals(1, changes.get());

            pressKey(field, KeyEvent.VK_UP);
            assertEquals(0, new BigDecimal("1.38").compareTo(field.getValue()));
            assertEquals(1, inputs.get());
            assertEquals(2, changes.get());

            field.setNumberLocale(Locale.GERMANY);
            assertEquals("1,38", field.getText());
            assertEquals(Locale.GERMANY, field.getNumberLocale());
            return null;
        });
    }

    @Test
    void changesByMouseWheelOnlyWhileFocused() throws Exception {
        onEdt(() -> {
            FocusedNumberField field = new FocusedNumberField();
            field.setStep(new BigDecimal("0.5")).setValue(BigDecimal.ONE, false);
            MouseWheelEvent event = new MouseWheelEvent(field, MouseWheelEvent.MOUSE_WHEEL,
                    System.currentTimeMillis(), 0, 0, 0, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1);

            for (MouseWheelListener listener : field.getMouseWheelListeners()) listener.mouseWheelMoved(event);

            assertEquals(0, new BigDecimal("1.5").compareTo(field.getValue()));
            assertTrue(event.isConsumed());
            return null;
        });
    }

    @Test
    void validatesConfiguration() throws Exception {
        onEdt(() -> {
            NumberField field = new NumberField();
            assertThrows(IllegalArgumentException.class,
                    () -> field.setRange(BigDecimal.TEN, BigDecimal.ONE));
            assertThrows(IllegalArgumentException.class, () -> field.setStep(BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class, () -> field.setDecimalPlaces(-1));
            assertThrows(NullPointerException.class, () -> field.setRoundingMode(null));
            assertThrows(NullPointerException.class, () -> field.setNumberLocale(null));
            return null;
        });
    }

    private static void loseFocus(NumberField field) {
        FocusEvent event = new FocusEvent(field, FocusEvent.FOCUS_LOST);
        for (FocusListener listener : field.getFocusListeners()) listener.focusLost(event);
    }

    private static void pressKey(NumberField field, int keyCode) {
        KeyEvent event = new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                keyCode, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : field.getKeyListeners()) listener.keyPressed(event);
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

    private static class FocusedNumberField extends NumberField {
        @Override
        public boolean isFocusOwner() {
            return true;
        }
    }
}
