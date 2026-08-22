package dtm.stools.component.popup;

import dtm.stools.i18n.I18n;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public final class ModernInputDialog {

    public static ModernInputDialogBuilder builder() {
        return new ModernInputDialogBuilder();
    }

    public static ModernInputDialogBuilder modernDialogBuilder() {
        return builder();
    }

    private static String text(String key, String defaultValue) {
        return I18n.getText(ModernInputDialog.class, key, defaultValue);
    }

    public static class ModernInputDialogBuilder {

        private SubmitHandler submitHandler = null;
        private ValidationHandler validationHandler = null;

        private boolean closeOnSubmitSuccess = true;
        private boolean disableConfirmWhenInvalid = true;
        private boolean enterConfirms = true;
        private boolean closeOnEsc = true;

        private int validationDelayMs = 450;

        private String title = "";
        private String message = "";

        private Color accentColor = null;
        private Color errorColor = null;

        private Color confirmButtonColor = null;
        private Color confirmButtonForeground = null;
        private Color cancelButtonColor = null;
        private Color cancelButtonForeground = null;

        private JComponent inputComponent = null;
        private Component parent = null;

        private String confirmText = text("button.confirm", "Confirmar");
        private String cancelText = text("button.cancel", "Cancelar");

        private boolean draggable = true;
        private boolean showIcon = true;

        public ModernInputDialogBuilder title(String t) {
            this.title = t;
            return this;
        }

        public ModernInputDialogBuilder message(String m) {
            this.message = m;
            return this;
        }

        public ModernInputDialogBuilder accentColor(Color c) {
            this.accentColor = c;
            return this;
        }

        public ModernInputDialogBuilder errorColor(Color c) {
            this.errorColor = c;
            return this;
        }

        public ModernInputDialogBuilder buttonColor(Color c) {
            this.confirmButtonColor = c;
            return this;
        }

        public ModernInputDialogBuilder confirmButtonColor(Color c) {
            this.confirmButtonColor = c;
            return this;
        }

        public ModernInputDialogBuilder confirmButtonForeground(Color c) {
            this.confirmButtonForeground = c;
            return this;
        }

        public ModernInputDialogBuilder cancelButtonColor(Color c) {
            this.cancelButtonColor = c;
            return this;
        }

        public ModernInputDialogBuilder cancelButtonForeground(Color c) {
            this.cancelButtonForeground = c;
            return this;
        }

        public ModernInputDialogBuilder confirmText(String t) {
            this.confirmText = t;
            return this;
        }

        public ModernInputDialogBuilder cancelText(String t) {
            this.cancelText = t;
            return this;
        }

        public ModernInputDialogBuilder input(JComponent component) {
            this.inputComponent = component;
            return this;
        }

        public ModernInputDialogBuilder parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public ModernInputDialogBuilder parentComponent(Component parent) {
            return parent(parent);
        }

        public ModernInputDialogBuilder draggable(boolean d) {
            this.draggable = d;
            return this;
        }

        public ModernInputDialogBuilder showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public ModernInputDialogBuilder onSubmit(SubmitHandler handler) {
            this.submitHandler = handler;
            return this;
        }

        public ModernInputDialogBuilder onValidate(ValidationHandler handler) {
            this.validationHandler = handler;
            return this;
        }

        public ModernInputDialogBuilder validationDelayMs(int delayMs) {
            this.validationDelayMs = Math.max(0, delayMs);
            return this;
        }

        public ModernInputDialogBuilder disableConfirmWhenInvalid(boolean disable) {
            this.disableConfirmWhenInvalid = disable;
            return this;
        }

        public ModernInputDialogBuilder closeOnSubmitSuccess(boolean close) {
            this.closeOnSubmitSuccess = close;
            return this;
        }

        public ModernInputDialogBuilder enterConfirms(boolean enterConfirms) {
            this.enterConfirms = enterConfirms;
            return this;
        }

        public ModernInputDialogBuilder closeOnEsc(boolean closeOnEsc) {
            this.closeOnEsc = closeOnEsc;
            return this;
        }

        public String show() {
            return show(parent);
        }

        public String show(Component parent) {
            Color panelBg = lfBackground();
            Color fgPrimary = lfForeground();
            Color fgSecondary = new Color(
                    fgPrimary.getRed(),
                    fgPrimary.getGreen(),
                    fgPrimary.getBlue(),
                    Math.max(0, (int) (fgPrimary.getAlpha() * 0.55f))
            );

            Color accent = accentColor != null ? accentColor : resolveAccent();
            Color resolvedErrorColor = errorColor != null ? errorColor : new Color(0xEF4444);
            Color secondaryBg = deriveSecondaryBg(panelBg);

            boolean darkTheme = isDark(panelBg);

            Color borderColor = darkTheme
                    ? new Color(255, 255, 255, 22)
                    : new Color(0, 0, 0, 22);

            JTextField defaultField = null;

            if (inputComponent == null) {
                defaultField = new JTextField();
                defaultField.setBackground(secondaryBg);
                defaultField.setForeground(fgPrimary);
                defaultField.setCaretColor(fgPrimary);
                defaultField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                defaultField.setFont(font(12, Font.PLAIN));
                inputComponent = defaultField;
            }

            normalizeInputSize(inputComponent);

            final JTextField finalField = defaultField;
            final String[] result = {null};

            Window owner;

            if (parent instanceof Window w) {
                owner = w;
            } else {
                owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
            }

            JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);

            JPanel root = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(panelBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(
                            0.5f,
                            0.5f,
                            getWidth() - 1,
                            getHeight() - 1,
                            14,
                            14
                    ));

                    g2.dispose();
                }
            };

            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

            if (draggable) {
                int[] dragOffset = new int[2];

                header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

                header.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        dragOffset[0] = e.getX();
                        dragOffset[1] = e.getY();
                    }
                });

                header.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        dialog.setLocation(
                                dialog.getX() + e.getX() - dragOffset[0],
                                dialog.getY() + e.getY() - dragOffset[1]
                        );
                    }
                });
            }

            JLabel titleLbl = new JLabel(title);
            titleLbl.setForeground(fgPrimary);
            titleLbl.setFont(font(14, Font.BOLD));

            header.add(titleLbl, BorderLayout.WEST);
            header.add(buildCloseBtn(dialog, fgSecondary, darkTheme), BorderLayout.EAST);

            JPanel body = new JPanel(new BorderLayout(14, 0));
            body.setOpaque(false);
            body.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

            if (showIcon) {
                body.add(new JLabel(buildIcon(accent)), BorderLayout.WEST);
            }

            JPanel contentBlock = new JPanel();
            contentBlock.setOpaque(false);
            contentBlock.setLayout(new BoxLayout(contentBlock, BoxLayout.Y_AXIS));

            if (message != null && !message.isBlank()) {
                JLabel msgLbl = new JLabel("<html><div style='width:260px'>" + escapeHtml(message) + "</div></html>");
                msgLbl.setForeground(fgSecondary);
                msgLbl.setFont(font(12, Font.PLAIN));
                msgLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
                msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentBlock.add(msgLbl);
            }

            inputComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentBlock.add(inputComponent);

            JLabel errorLbl = new JLabel();
            errorLbl.setVisible(false);
            errorLbl.setForeground(resolvedErrorColor);
            errorLbl.setFont(font(12, Font.PLAIN));
            errorLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            errorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentBlock.add(errorLbl);

            body.add(contentBlock, BorderLayout.CENTER);

            Color confirmBg = confirmButtonColor != null ? confirmButtonColor : accent;
            Color confirmHover = deriveHover(confirmBg, 0.12f);
            Color confirmFg = confirmButtonForeground != null
                    ? confirmButtonForeground
                    : readableForeground(confirmBg);

            Color cancelBg = cancelButtonColor != null ? cancelButtonColor : secondaryBg;
            Color cancelHover = deriveHover(cancelBg, 0.12f);
            Color cancelFg = cancelButtonForeground != null
                    ? cancelButtonForeground
                    : fgPrimary;

            final JButton[] confirmBtnRef = new JButton[1];

            JButton confirmBtn = buildBtn(confirmText, confirmBg, confirmHover, confirmFg, () -> {
                String value = getInputValue(finalField, inputComponent);

                try {
                    if (validationHandler != null && isBlank(value)) {
                        setEmptyState(dialog, errorLbl, confirmBtnRef[0]);
                        return;
                    }

                    validateValue(value, inputComponent, dialog);
                    hideError(errorLbl, confirmBtnRef[0]);
                    repack(dialog);

                    if (submitHandler != null) {
                        submitHandler.submit(new SubmitContext(value, inputComponent, dialog));
                    }

                    result[0] = value;

                    if (closeOnSubmitSuccess) {
                        dialog.dispose();
                    }
                } catch (Exception ex) {
                    showError(dialog, errorLbl, confirmBtnRef[0], ex);
                }
            });

            confirmBtnRef[0] = confirmBtn;

            if (disableConfirmWhenInvalid && validationHandler != null) {
                confirmBtn.setEnabled(!isBlank(getInputValue(finalField, inputComponent)));
            }

            installRealtimeValidation(
                    dialog,
                    finalField,
                    inputComponent,
                    errorLbl,
                    confirmBtn
            );

            JButton cancelBtn = buildBtn(cancelText, cancelBg, cancelHover, cancelFg, dialog::dispose);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnRow.setOpaque(false);
            btnRow.add(cancelBtn);
            btnRow.add(confirmBtn);

            root.add(header, BorderLayout.NORTH);
            root.add(body, BorderLayout.CENTER);
            root.add(btnRow, BorderLayout.SOUTH);

            dialog.setContentPane(root);
            repack(dialog);

            if (parent != null) {
                dialog.setLocationRelativeTo(parent);
            } else {
                dialog.setLocationRelativeTo(null);
            }

            if (enterConfirms) {
                dialog.getRootPane().setDefaultButton(confirmBtn);
            }

            ModernPopupSupport.installCloseOnEsc(dialog.getRootPane(), closeOnEsc, dialog::dispose);

            SwingUtilities.invokeLater(inputComponent::requestFocusInWindow);

            dialog.setVisible(true);

            return result[0];
        }

        private void installRealtimeValidation(
                JDialog dialog,
                JTextField finalField,
                JComponent inputComponent,
                JLabel errorLbl,
                JButton confirmBtn
        ) {
            if (validationHandler == null) {
                return;
            }

            JTextComponent textComponent = null;

            if (finalField != null) {
                textComponent = finalField;
            } else if (inputComponent instanceof JTextComponent component) {
                textComponent = component;
            }

            if (textComponent == null) {
                return;
            }

            JTextComponent finalTextComponent = textComponent;

            Timer validationTimer = new Timer(validationDelayMs, e -> {
                String value = finalTextComponent.getText();

                if (isBlank(value)) {
                    setEmptyState(dialog, errorLbl, confirmBtn);
                    return;
                }

                try {
                    validateValue(value, inputComponent, dialog);
                    hideError(errorLbl, confirmBtn);
                    repack(dialog);
                } catch (Exception ex) {
                    showError(dialog, errorLbl, confirmBtn, ex);
                }
            });

            validationTimer.setRepeats(false);

            textComponent.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    restartValidation(validationTimer);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    restartValidation(validationTimer);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    restartValidation(validationTimer);
                }
            });
        }

        private void restartValidation(Timer timer) {
            if (validationDelayMs <= 0) {
                timer.stop();

                for (ActionListener listener : timer.getActionListeners()) {
                    listener.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "validate"));
                }

                return;
            }

            timer.restart();
        }

        private void validateValue(String value, JComponent inputComponent, JDialog dialog) throws Exception {
            if (validationHandler != null) {
                validationHandler.validate(new ValidationContext(value, inputComponent, dialog));
            }
        }

        private void setEmptyState(JDialog dialog, JLabel errorLbl, JButton confirmBtn) {
            errorLbl.setVisible(false);
            errorLbl.setText("");

            if (disableConfirmWhenInvalid && confirmBtn != null) {
                confirmBtn.setEnabled(false);
            }

            repack(dialog);
        }

        private void showError(JDialog dialog, JLabel errorLbl, JButton confirmBtn, Exception ex) {
            String errorMessage = ex.getMessage();

            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = text("error.confirm", "Erro ao confirmar.");
            }

            errorLbl.setText("<html><div style='width:260px'>" + escapeHtml(errorMessage) + "</div></html>");
            errorLbl.setVisible(true);

            if (disableConfirmWhenInvalid && confirmBtn != null) {
                confirmBtn.setEnabled(false);
            }

            repack(dialog);
        }

        private void hideError(JLabel errorLbl, JButton confirmBtn) {
            errorLbl.setVisible(false);
            errorLbl.setText("");

            if (disableConfirmWhenInvalid && confirmBtn != null) {
                confirmBtn.setEnabled(true);
            }
        }

        private void repack(JDialog dialog) {
            dialog.setMinimumSize(new Dimension(340, 1));
            dialog.pack();

            Dimension size = dialog.getSize();

            if (size.width < 340) {
                dialog.setSize(340, size.height);
            }

            dialog.setShape(new RoundRectangle2D.Float(
                    0,
                    0,
                    dialog.getWidth(),
                    dialog.getHeight(),
                    14,
                    14
            ));
        }

        private void normalizeInputSize(JComponent component) {
            Dimension preferred = component.getPreferredSize();

            int width = Math.max(preferred.width, 320);
            int height = Math.max(preferred.height, 32);

            component.setPreferredSize(new Dimension(width, height));
            component.setMinimumSize(new Dimension(width, height));
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        private JButton buildBtn(String text, Color bg, Color hover, Color fg, Runnable action) {
            JButton btn = new JButton(text) {
                private boolean isHover = false;

                {
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            isHover = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            isHover = false;
                            repaint();
                        }
                    });
                    addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            repaint();
                        }

                        @Override
                        public void focusLost(FocusEvent e) {
                            repaint();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Color currentBg = isEnabled()
                            ? isHover ? hover : bg
                            : disabledColor(bg);

                    g2.setColor(currentBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    if (isEnabled() && isFocusOwner()) {
                        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 170));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                    }
                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            btn.setForeground(fg);
            btn.setFont(font(12, Font.PLAIN));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> action.run());

            return btn;
        }

        private JButton buildCloseBtn(JDialog dialog, Color fg, boolean darkTheme) {
            JButton btn = new JButton("×") {
                private boolean hover = false;

                {
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            hover = true;
                            repaint();
                        }

                        @Override
                        public void mouseExited(MouseEvent e) {
                            hover = false;
                            repaint();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Color hoverColor = darkTheme
                            ? new Color(255, 255, 255, 22)
                            : new Color(0, 0, 0, 22);

                    g2.setColor(hover ? hoverColor : new Color(0, 0, 0, 0));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();

                    super.paintComponent(g);
                }
            };

            btn.setForeground(fg);
            btn.setFont(font(16, Font.PLAIN));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFocusable(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(text("button.close", "Fechar"));
            btn.addActionListener(e -> dialog.dispose());

            return btn;
        }

        private static Icon buildIcon(Color accent) {
            return new Icon() {
                @Override
                public int getIconWidth() {
                    return 44;
                }

                @Override
                public int getIconHeight() {
                    return 44;
                }

                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                    g2.fillRoundRect(x, y, 44, 44, 12, 12);

                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(x, y, 43, 43, 12, 12);

                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = x + 22;
                    int cy = y + 22;

                    g2.drawOval(cx - 9, cy - 9, 18, 18);
                    g2.drawLine(cx, cy - 2, cx, cy + 5);
                    g2.fillOval(cx - 1, cy - 6, 2, 2);

                    g2.dispose();
                }
            };
        }

        private Font font(float size, int style) {
            Font base = UIManager.getFont("Label.font");

            if (base == null) {
                base = new Font("Segoe UI", style, (int) size);
            }

            return base.deriveFont(style, size);
        }
    }

    @FunctionalInterface
    public interface SubmitHandler {
        void submit(SubmitContext context) throws Exception;
    }

    @FunctionalInterface
    public interface ValidationHandler {
        void validate(ValidationContext context) throws Exception;
    }

    public record SubmitContext(
            String value,
            JComponent inputComponent,
            JDialog dialog
    ) {
    }

    public record ValidationContext(
            String value,
            JComponent inputComponent,
            JDialog dialog
    ) {
    }

    private static String getInputValue(JTextField defaultField, JComponent inputComponent) {
        if (defaultField != null) {
            return defaultField.getText();
        }

        if (inputComponent instanceof JTextArea textArea) {
            return textArea.getText();
        }

        if (inputComponent instanceof JTextField textField) {
            return textField.getText();
        }

        if (inputComponent instanceof JComboBox<?> comboBox) {
            Object selectedItem = comboBox.getSelectedItem();
            return selectedItem == null ? "" : String.valueOf(selectedItem);
        }

        return "";
    }

    private static Color lfBackground() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new Color(0x22252B);
    }

    private static Color lfForeground() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.WHITE;
    }

    private static Color resolveAccent() {
        Color lf = UIManager.getColor("Button.select");
        return lf != null ? lf : new Color(0x3B82F6);
    }

    private static Color readableForeground(Color bg) {
        return isDark(bg) ? Color.WHITE : new Color(0x1a1a1a);
    }

    private static Color disabledColor(Color base) {
        int alpha = Math.max(90, Math.min(150, base.getAlpha()));

        return new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                alpha
        );
    }

    private static boolean isDark(Color c) {
        return 0.2126 * (c.getRed() / 255.0)
                + 0.7152 * (c.getGreen() / 255.0)
                + 0.0722 * (c.getBlue() / 255.0) < 0.35;
    }

    private static Color deriveHover(Color base, float factor) {
        if (isDark(base)) {
            return new Color(
                    Math.min(255, (int) (base.getRed() + 255 * factor)),
                    Math.min(255, (int) (base.getGreen() + 255 * factor)),
                    Math.min(255, (int) (base.getBlue() + 255 * factor)),
                    base.getAlpha()
            );
        }

        return new Color(
                Math.max(0, (int) (base.getRed() - 255 * factor)),
                Math.max(0, (int) (base.getGreen() - 255 * factor)),
                Math.max(0, (int) (base.getBlue() - 255 * factor)),
                base.getAlpha()
        );
    }

    private static Color deriveSecondaryBg(Color panelBg) {
        return deriveHover(panelBg, 0.10f);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
