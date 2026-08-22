package dtm.stools.component.popup;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernPopupSupportTest {

    private static final KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    @Test
    void installsCloseActionForEscapeWhenEnabled() {
        JRootPane rootPane = new JRootPane();
        AtomicBoolean closed = new AtomicBoolean();

        ModernPopupSupport.installCloseOnEsc(rootPane, true, () -> closed.set(true));

        Object actionKey = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESCAPE);
        Action action = rootPane.getActionMap().get(actionKey);

        assertNotNull(actionKey);
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "escape"));
        assertTrue(closed.get());
    }

    @Test
    void doesNotInstallCloseActionWhenDisabled() {
        JRootPane rootPane = new JRootPane();
        AtomicBoolean closed = new AtomicBoolean();

        ModernPopupSupport.installCloseOnEsc(rootPane, false, () -> closed.set(true));

        Object actionKey = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESCAPE);

        assertNull(actionKey);
        assertFalse(closed.get());
    }
}
