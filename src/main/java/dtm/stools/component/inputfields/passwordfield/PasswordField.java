package dtm.stools.component.inputfields.passwordfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Map;

/** Password input. Callers own, and should erase, arrays returned by getPassword(). */
public class PasswordField extends PanelEventListener {
    public static final String VISIBILITY_CHANGED = "passwordVisibilityChanged";
    private final JButton visibility = new JButton();
    private final JPasswordField input = new JPasswordField(18) {
        @Override public Insets getInsets() {
            Insets insets = super.getInsets();
            insets.right += UiTokens.scale(30);
            return insets;
        }
        @Override public Insets getInsets(Insets insets) {
            Insets actual = getInsets();
            insets.set(actual.top, actual.left, actual.bottom, actual.right);
            return insets;
        }
        @Override public void doLayout() {
            Insets border = super.getInsets();
            int width = UiTokens.scale(28);
            visibility.setBounds(getWidth() - border.right - width, border.top,
                    width, Math.max(0, getHeight() - border.top - border.bottom));
        }
    };
    private final char echoChar;
    private boolean updating;
    private boolean passwordVisible;

    public PasswordField() {
        super(new BorderLayout(UiTokens.space(1), 0), false);
        echoChar = input.getEchoChar() == 0 ? '\u2022' : input.getEchoChar();
        input.setEchoChar(echoChar);
        input.getAccessibleContext().setAccessibleName(text("name", "Senha"));
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
        });
        visibility.addActionListener(e -> {
            if (isEnabled() && input.isEditable()) setPasswordVisible(!passwordVisible);
        });
        visibility.setName("password.visibility");
        visibility.setBorderPainted(false);
        visibility.setFocusPainted(false);
        visibility.setContentAreaFilled(false);
        visibility.setMargin(new Insets(0, 0, 0, 0));
        visibility.setIcon(new Icon() {
            public int getIconWidth() { return UiTokens.scale(20); }
            public int getIconHeight() { return UiTokens.scale(20); }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.translate(x, y); g.scale(UiTokens.getScaleFactor(), UiTokens.getScaleFactor());
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(c.isEnabled() ? UiTokens.foreground() : UiTokens.muted());
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D eye = new Path2D.Double();
                    eye.moveTo(1, 10); eye.curveTo(5, 3, 15, 3, 19, 10);
                    eye.curveTo(15, 17, 5, 17, 1, 10); g.draw(eye); g.drawOval(7, 7, 6, 6);
                    if (passwordVisible) g.drawLine(3, 3, 17, 17);
                } finally { g.dispose(); }
            }
        });
        input.setLayout(null);
        input.add(visibility);
        add(input, BorderLayout.CENTER);
        input.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("alt V"), "password.toggleVisibility");
        input.getActionMap().put("password.toggleVisibility", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { visibility.doClick(); }
        });
        refreshVisibility();
    }

    private static String text(String key, String fallback) {
        return I18n.getText(PasswordField.class, key, fallback);
    }

    /** Exposes the native input for focus, selection and Swing customization. */
    public JPasswordField getPasswordField() { return input; }
    public char[] getPassword() { return input.getPassword(); }
    public PasswordField setPassword(char[] password) { return setPassword(password, true); }

    public PasswordField setPassword(char[] password, boolean fireEvent) {
        char[] old = getPassword();
        char[] next = password == null ? new char[0] : password.clone();
        try {
            if (Arrays.equals(old, next)) return this;
            updating = true;
            // Swing's Document API takes text; the public value contract remains char[].
            input.setText(new String(next));
        } finally {
            updating = false;
            Arrays.fill(old, '\0');
            Arrays.fill(next, '\0');
        }
        if (fireEvent) changed();
        return this;
    }

    public PasswordField clear() { return clear(true); }
    public PasswordField clear(boolean fireEvent) { return setPassword(null, fireEvent); }
    public boolean isPasswordVisible() { return passwordVisible; }
    public PasswordField setPasswordVisible(boolean visible) { return setPasswordVisible(visible, true); }

    public PasswordField setPasswordVisible(boolean visible, boolean fireEvent) {
        if (passwordVisible == visible) return this;
        int dot = input.getCaret().getDot(), mark = input.getCaret().getMark();
        boolean previous = passwordVisible;
        passwordVisible = visible;
        input.setEchoChar(visible ? (char) 0 : echoChar);
        input.getCaret().setDot(mark);
        input.getCaret().moveDot(dot);
        refreshVisibility();
        if (fireEvent) dispatchEvent(VISIBILITY_CHANGED, visible, Map.of("oldValue", previous, "newValue", visible));
        return this;
    }

    private void refreshVisibility() {
        String name = passwordVisible ? text("hide", "Ocultar senha") : text("show", "Mostrar senha");
        visibility.setToolTipText(name);
        visibility.getAccessibleContext().setAccessibleName(name);
        visibility.repaint();
    }

    private void changed() {
        if (!updating) dispatchEvent(EventType.CHANGE, (Object) null, Map.of());
    }

    public PasswordField setPlaceholder(String placeholder) {
        input.putClientProperty("JTextField.placeholderText", placeholder);
        return this;
    }
    public boolean isEditable() { return input.isEditable(); }
    public PasswordField setEditable(boolean editable) {
        input.setEditable(editable);
        visibility.setEnabled(isEnabled() && editable);
        return this;
    }
    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (input != null) input.setEnabled(enabled);
        if (visibility != null) visibility.setEnabled(enabled && input.isEditable());
    }
}
