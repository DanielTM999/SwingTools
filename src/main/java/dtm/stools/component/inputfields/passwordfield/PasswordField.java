package dtm.stools.component.inputfields.passwordfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutManager;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutTarget;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import dtm.stools.utils.PaintUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Map;

public class PasswordField extends PanelEventListener implements FieldLayoutTarget {
    public static final String VISIBILITY_CHANGED = "passwordVisibilityChanged";
    private final JButton visibility = new JButton();
    private final JPasswordField input = new PasswordInput();
    private String label = "";
    private FieldLayoutManager fieldLayoutManager;
    private boolean originalOpaque;

    private class PasswordInput extends JPasswordField {
        private final DocumentListener contentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { contentChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { contentChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { contentChanged(); }
        };

        private PasswordInput() {
            super(18);
            getDocument().addDocumentListener(contentListener);
        }

        private void contentChanged() {
            if (fieldLayoutManager != null) fieldLayoutManager.fieldChanged(PasswordField.this);
            repaint();
            changed();
        }

        @Override
        public void setDocument(Document document) {
            Document previous = getDocument();
            if (previous != null && contentListener != null) previous.removeDocumentListener(contentListener);
            super.setDocument(document);
            if (document != null && contentListener != null) {
                document.addDocumentListener(contentListener);
                contentChanged();
            }
        }

        private Insets decorationInsets() {
            return fieldLayoutManager == null ? super.getInsets()
                    : fieldLayoutManager.getInsets(PasswordField.this);
        }

        @Override
        public Insets getInsets() {
            Insets insets = decorationInsets();
            return new Insets(insets.top, insets.left, insets.bottom, insets.right + UiTokens.scale(30));
        }
        @Override
        public Insets getInsets(Insets insets) {
            if (insets == null) return getInsets();
            Insets actual = getInsets();
            insets.set(actual.top, actual.left, actual.bottom, actual.right);
            return insets;
        }
        @Override
        public void doLayout() {
            Insets border = decorationInsets();
            int width = UiTokens.scale(28);
            visibility.setBounds(getWidth() - border.right - width, border.top,
                    width, Math.max(0, getHeight() - border.top - border.bottom));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension natural = super.getPreferredSize();
            return fieldLayoutManager == null || isPreferredSizeSet() ? natural
                    : fieldLayoutManager.getPreferredSize(PasswordField.this, natural);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension natural = super.getMinimumSize();
            return fieldLayoutManager == null || isMinimumSizeSet() ? natural
                    : fieldLayoutManager.getMinimumSize(PasswordField.this, natural);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (input != null && visibility.getParent() != this) {
                setLayout(null);
                add(visibility);
            }
            if (fieldLayoutManager != null) {
                setOpaque(false);
                fieldLayoutManager.themeChanged(PasswordField.this);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (fieldLayoutManager == null) { super.paintComponent(g); return; }
            Graphics2D background = PaintUtils.antialias((Graphics2D) g.create());
            try { fieldLayoutManager.paintBackground(background, PasswordField.this); }
            finally { background.dispose(); }
            Graphics content = g.create();
            try {
                Rectangle bounds = getFieldContentBounds();
                content.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
                if (!isFieldContentEmpty() || fieldLayoutManager.isPlaceholderVisible(PasswordField.this)) {
                    super.paintComponent(content);
                } else if (hasFocus()) {
                    getCaret().paint(content);
                }
            } finally { content.dispose(); }
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (fieldLayoutManager == null) { super.paintBorder(g); return; }
            Graphics2D border = PaintUtils.antialias((Graphics2D) g.create());
            try { fieldLayoutManager.paintBorder(border, PasswordField.this); }
            finally { border.dispose(); }
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (fieldLayoutManager == null) return;
            Graphics2D overlay = PaintUtils.antialias((Graphics2D) g.create());
            try { fieldLayoutManager.paintOverlay(overlay, PasswordField.this); }
            finally { overlay.dispose(); }
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (fieldLayoutManager != null) fieldLayoutManager.fieldShown(PasswordField.this);
        }

        @Override
        public void removeNotify() {
            if (fieldLayoutManager != null) fieldLayoutManager.fieldRemoved(PasswordField.this);
            super.removeNotify();
        }
    }
    private final char echoChar;
    private boolean updating;
    private boolean passwordVisible;

    public PasswordField() {
        super(new BorderLayout(UiTokens.space(1), 0), false);
        echoChar = input.getEchoChar() == 0 ? '\u2022' : input.getEchoChar();
        input.setEchoChar(echoChar);
        input.getAccessibleContext().setAccessibleName(text("name", "Senha"));
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

    @Override
    public JPasswordField getFieldComponent() { return input; }

    @Override
    public String getLabel() { return label; }

    public PasswordField setLabel(String label) {
        String previous = this.label;
        this.label = label == null ? "" : label;
        input.getAccessibleContext().setAccessibleName(this.label);
        input.putClientProperty("label", this.label);
        firePropertyChange("label", previous, this.label);
        refreshLayout();
        return this;
    }

    public FieldLayoutManager getFieldLayoutManager() { return fieldLayoutManager; }

    public PasswordField setFieldLayoutManager(FieldLayoutManager next) {
        FieldLayoutManager previous = fieldLayoutManager;
        if (previous == next) return this;
        if (previous == null) originalOpaque = input.isOpaque();
        if (previous != null) previous.uninstall(this);
        fieldLayoutManager = next;
        try {
            if (next != null) {
                input.setOpaque(false);
                next.install(this);
            } else {
                input.setOpaque(originalOpaque);
            }
        } catch (RuntimeException | Error failure) {
            fieldLayoutManager = previous;
            if (previous != null) previous.install(this);
            else input.setOpaque(originalOpaque);
            refreshLayout();
            throw failure;
        }
        firePropertyChange("fieldLayoutManager", previous, next);
        refreshLayout();
        return this;
    }

    @Override
    public boolean isFieldContentEmpty() { return input.getDocument().getLength() == 0; }

    @Override
    public Rectangle getFieldContentBounds() {
        Insets insets = input.getInsets();
        return new Rectangle(insets.left, insets.top,
                Math.max(0, input.getWidth() - insets.left - insets.right),
                Math.max(0, input.getHeight() - insets.top - insets.bottom));
    }

    private void refreshLayout() {
        input.revalidate();
        input.repaint();
        revalidate();
        repaint();
    }

    public char[] getPassword() { return input.getPassword(); }

    public String getPasswordAsString() {
        char[] password = getPassword();
        try {
            return new String(password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public PasswordField setPassword(char[] password) { return setPassword(password, true); }

    public PasswordField setPassword(char[] password, boolean fireEvent) {
        char[] old = getPassword();
        char[] next = password == null ? new char[0] : password.clone();
        try {
            if (Arrays.equals(old, next)) return this;
            updating = true;

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
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (input != null) input.setEnabled(enabled);
        if (visibility != null) visibility.setEnabled(enabled && input.isEditable());
    }
}
