package dtm.stools.component.accessibility;

import dtm.stools.configs.UiTokens;
import javax.accessibility.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Keyboard and assistive-technology behavior for buttons painted by the library. */
public abstract class AccessibleButton extends JComponent implements Accessible {
    protected AccessibleButton() {
        setFocusable(true);
        for (String key : new String[]{"SPACE", "ENTER"}) {
            getInputMap().put(KeyStroke.getKeyStroke(key), "activate");
        }
        getActionMap().put("activate", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (isEnabled()) activate(); }
        });
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { repaint(); }
            public void focusLost(FocusEvent e) { repaint(); }
        });
    }
    protected abstract void activate();
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessiblePaintedButton();
        return accessibleContext;
    }
    protected class AccessiblePaintedButton extends AccessibleJComponent implements AccessibleAction {
        @Override
        public AccessibleRole getAccessibleRole() { return AccessibleRole.PUSH_BUTTON; }
        @Override
        public String getAccessibleName() {
            String name = super.getAccessibleName(); return name != null ? name : getToolTipText();
        }
        @Override
        public AccessibleAction getAccessibleAction() { return this; }
        @Override
        public int getAccessibleActionCount() { return 1; }
        @Override
        public String getAccessibleActionDescription(int index) { return index == 0 ? getAccessibleName() : null; }
        @Override
        public boolean doAccessibleAction(int index) {
            if (index != 0 || !isEnabled()) return false;
            if (SwingUtilities.isEventDispatchThread()) activate();
            else SwingUtilities.invokeLater(() -> { if (isEnabled()) activate(); });
            return true;
        }
    }
    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (isFocusOwner()) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(UiTokens.accent()); g.setStroke(new BasicStroke(UiTokens.stroke()));
                g.drawRoundRect(2, 2, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5), UiTokens.scale(6), UiTokens.scale(6));
            } finally { g.dispose(); }
        }
    }
}
