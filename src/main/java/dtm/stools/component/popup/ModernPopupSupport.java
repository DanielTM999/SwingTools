package dtm.stools.component.popup;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

final class ModernPopupSupport {

    private static final String CLOSE_ON_ESC_ACTION = "modernPopup.closeOnEsc";

    private ModernPopupSupport() {
    }

    static void installCloseOnEsc(JRootPane rootPane, boolean closeOnEsc, Runnable closeAction) {
        if (!closeOnEsc) {
            return;
        }

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_ON_ESC_ACTION);
        actionMap.put(CLOSE_ON_ESC_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closeAction.run();
            }
        });
    }
}
