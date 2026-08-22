package dtm.stools.component.popup;

import dtm.stools.i18n.I18n;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ModernComponentDialog<T> {

    private ModernComponentDialog() {}

    private static String text(String key, String defaultValue) {
        return I18n.getText(ModernComponentDialog.class, key, defaultValue);
    }

    public static <T> ModernComponentDialogBuilder<T> builder(Class<T> resultType) {
        return new ModernComponentDialogBuilder<>(resultType);
    }

    public static <T> ModernComponentDialogBuilder<T> builder() {
        return new ModernComponentDialogBuilder<>(null);
    }

    public static <T> ModernComponentDialogBuilder<T> modernDialogBuilder(Class<T> resultType) {
        return builder(resultType);
    }

    public static <T> ModernComponentDialogBuilder<T> modernDialogBuilder() {
        return builder();
    }

    public static final class ModernComponentDialogBuilder<T> {

        private final Class<T> resultType;

        private String title = "";
        private String message = "";
        private String typeLabel = null;
        private ModernDialog.Type type = ModernDialog.Type.INFO;
        private Color accentColor = null;
        private boolean draggable = true;
        private boolean showTypeLabel = true;
        private boolean showIcon = true;

        private Color confirmButtonColor = null;
        private Color confirmButtonForeground = null;
        private Color cancelButtonColor = null;
        private Color cancelButtonForeground = null;

        private String confirmText = text("button.confirm", "Confirmar");
        private String cancelText = text("button.cancel", "Cancelar");
        private boolean showCancelButton = true;
        private boolean closeOnSubmitSuccess = true;
        private boolean validateOnChange = false;
        private boolean disableConfirmWhenInvalid = true;
        private boolean enterConfirms = true;
        private boolean closeOnEsc = true;
        private boolean requestFocusOnLoad = false;
        private int validationDelayMs = 450;

        private JComponent component = null;
        private Component parent = null;
        private ResultProvider<T> resultProvider = null;
        private ValidationHandler<T> validationHandler = null;
        private SubmitHandler<T> submitHandler = null;

        private final java.util.List<Component> validationTriggerComponents = new java.util.ArrayList<>();
        private final java.util.List<CustomValidationTrigger<?>> customValidationTriggers = new java.util.ArrayList<>();
        private final java.util.List<Btn<T>> buttons = new java.util.ArrayList<>();

        private ModernComponentDialogBuilder(Class<T> resultType) {
            this.resultType = resultType;
        }

        public ModernComponentDialogBuilder<T> title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public ModernComponentDialogBuilder<T> message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public ModernComponentDialogBuilder<T> type(ModernDialog.Type type) {
            this.type = type == null ? ModernDialog.Type.INFO : type;
            return this;
        }

        public ModernComponentDialogBuilder<T> typeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
            return this;
        }

        public ModernComponentDialogBuilder<T> showTypeLabel(boolean showTypeLabel) {
            this.showTypeLabel = showTypeLabel;
            return this;
        }

        public ModernComponentDialogBuilder<T> accentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public ModernComponentDialogBuilder<T> draggable(boolean draggable) {
            this.draggable = draggable;
            return this;
        }

        public ModernComponentDialogBuilder<T> showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public ModernComponentDialogBuilder<T> component(JComponent component) {
            this.component = Objects.requireNonNull(component, "component");
            return this;
        }

        public ModernComponentDialogBuilder<T> content(JComponent component) {
            return component(component);
        }

        public ModernComponentDialogBuilder<T> parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public ModernComponentDialogBuilder<T> parentComponent(Component parent) {
            return parent(parent);
        }

        public ModernComponentDialogBuilder<T> form(FormConfigurer configurer) {
            FormPanel form = new FormPanel();
            if (configurer != null) {
                configurer.configure(form);
            }
            this.component = form;
            return this;
        }

        public ModernComponentDialogBuilder<T> result(ResultProvider<T> provider) {
            this.resultProvider = provider;
            return this;
        }

        public ModernComponentDialogBuilder<T> onValidate(ValidationHandler<T> handler) {
            this.validationHandler = handler;
            return this;
        }

        public ModernComponentDialogBuilder<T> validateOnChange() {
            return validateOnChange(true);
        }

        public ModernComponentDialogBuilder<T> validateOnChange(boolean validate) {
            this.validateOnChange = validate;
            return this;
        }

        public ModernComponentDialogBuilder<T> applyValidationOnChange() {
            return validateOnChange(true);
        }

        public ModernComponentDialogBuilder<T> applyValidationOnChange(boolean apply) {
            return validateOnChange(apply);
        }

        public ModernComponentDialogBuilder<T> validationDelayMs(int delayMs) {
            this.validationDelayMs = Math.max(0, delayMs);
            return this;
        }

        public ModernComponentDialogBuilder<T> disableConfirmWhenInvalid(boolean disable) {
            this.disableConfirmWhenInvalid = disable;
            return this;
        }

        public ModernComponentDialogBuilder<T> validationTrigger(Component component) {
            if (component != null) {
                this.validateOnChange = true;
                validationTriggerComponents.add(component);
            }
            return this;
        }

        public <C extends Component> ModernComponentDialogBuilder<T> validationTrigger(C component, ValidationTriggerInstaller<C> installer) {
            if (component != null && installer != null) {
                this.validateOnChange = true;
                customValidationTriggers.add(new CustomValidationTrigger<>(component, installer));
            }
            return this;
        }

        public <C extends Component> ModernComponentDialogBuilder<T> customValidationTrigger(C component, ValidationTriggerInstaller<C> installer) {
            return validationTrigger(component, installer);
        }

        public ModernComponentDialogBuilder<T> onSubmit(SubmitHandler<T> handler) {
            this.submitHandler = handler;
            return this;
        }

        public ModernComponentDialogBuilder<T> closeOnSubmitSuccess(boolean close) {
            this.closeOnSubmitSuccess = close;
            return this;
        }

        public ModernComponentDialogBuilder<T> enterConfirms(boolean enterConfirms) {
            this.enterConfirms = enterConfirms;
            return this;
        }

        public ModernComponentDialogBuilder<T> closeOnEsc(boolean closeOnEsc) {
            this.closeOnEsc = closeOnEsc;
            return this;
        }

        public ModernComponentDialogBuilder<T> requestFocusOnLoad(boolean requestFocusOnLoad) {
            this.requestFocusOnLoad = requestFocusOnLoad;
            return this;
        }

        public ModernComponentDialogBuilder<T> confirmText(String text) {
            this.confirmText = text == null ? "" : text;
            return this;
        }

        public ModernComponentDialogBuilder<T> cancelText(String text) {
            this.cancelText = text == null ? "" : text;
            return this;
        }

        public ModernComponentDialogBuilder<T> showCancelButton(boolean show) {
            this.showCancelButton = show;
            return this;
        }

        public ModernComponentDialogBuilder<T> buttonColor(Color color) {
            this.confirmButtonColor = color;
            return this;
        }

        public ModernComponentDialogBuilder<T> confirmButtonColor(Color color) {
            this.confirmButtonColor = color;
            return this;
        }

        public ModernComponentDialogBuilder<T> confirmButtonForeground(Color color) {
            this.confirmButtonForeground = color;
            return this;
        }

        public ModernComponentDialogBuilder<T> cancelButtonColor(Color color) {
            this.cancelButtonColor = color;
            return this;
        }

        public ModernComponentDialogBuilder<T> cancelButtonForeground(Color color) {
            this.cancelButtonForeground = color;
            return this;
        }

        public ModernComponentDialogBuilder<T> option(String text, T value) {
            buttons.add(new Btn<>(text, value, null, !hasPrimaryButton(), null, null, false));
            return this;
        }

        public ModernComponentDialogBuilder<T> option(String text, T value, Color color) {
            buttons.add(new Btn<>(text, value, null, !hasPrimaryButton(), color, null, false));
            return this;
        }

        public ModernComponentDialogBuilder<T> option(String text, T value, Color color, Color foreground) {
            buttons.add(new Btn<>(text, value, null, !hasPrimaryButton(), color, foreground, false));
            return this;
        }

        public ModernComponentDialogBuilder<T> submitOption(String text) {
            buttons.add(new Btn<>(text, null, null, !hasPrimaryButton(), null, null, true));
            return this;
        }

        public ModernComponentDialogBuilder<T> submitOption(String text, ResultProvider<T> provider) {
            buttons.add(new Btn<>(text, null, provider, !hasPrimaryButton(), null, null, true));
            return this;
        }

        public ModernComponentDialogBuilder<T> submitOption(String text, ResultProvider<T> provider, Color color) {
            buttons.add(new Btn<>(text, null, provider, !hasPrimaryButton(), color, null, true));
            return this;
        }

        public ModernComponentDialogBuilder<T> submitOption(String text, ResultProvider<T> provider, Color color, Color foreground) {
            buttons.add(new Btn<>(text, null, provider, !hasPrimaryButton(), color, foreground, true));
            return this;
        }

        public ModernComponentDialogBuilder<T> cancelOption(String text) {
            buttons.add(new Btn<>(text, null, null, false, null, null, false));
            return this;
        }

        public T show() {
            return show(parent);
        }

        public T show(Component parent) {
            if (component == null) {
                component = new FormPanel();
            }

            normalizeContentSize(component);

            Color panelBg = lfBackground();
            Color fgPrimary = lfForeground();
            Color fgSecondary = new Color(
                    fgPrimary.getRed(),
                    fgPrimary.getGreen(),
                    fgPrimary.getBlue(),
                    Math.max(0, (int) (fgPrimary.getAlpha() * 0.55f))
            );
            Color accent = resolveAccent(accentColor, type);
            Color secondaryBg = deriveSecondaryBg(panelBg);
            boolean darkTheme = isDark(panelBg);
            Color borderColor = darkTheme
                    ? new Color(255, 255, 255, 22)
                    : new Color(0, 0, 0, 22);

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
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                    g2.dispose();
                }
            };
            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
            if (showTypeLabel) {
                header.add(buildTypeChip(type, accent, fgSecondary, typeLabel), BorderLayout.WEST);
            }

            if (draggable) {
                installDragSupport(header, dialog);
            }

            header.add(buildCloseBtn(dialog, fgSecondary, darkTheme), BorderLayout.EAST);

            JPanel body = new JPanel(new BorderLayout(14, 0));
            body.setOpaque(false);
            body.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));
            if (showIcon) {
                body.add(new JLabel(buildIcon(type, accent)), BorderLayout.WEST);
            }

            JPanel contentBlock = new JPanel();
            contentBlock.setOpaque(false);
            contentBlock.setLayout(new BoxLayout(contentBlock, BoxLayout.Y_AXIS));

            if (!title.isBlank()) {
                JLabel titleLbl = new JLabel("<html>" + escapeHtml(title) + "</html>");
                titleLbl.setForeground(fgPrimary);
                titleLbl.setFont(font(14, Font.BOLD));
                titleLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
                titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentBlock.add(titleLbl);
            }

            if (!message.isBlank()) {
                JLabel msgLbl = new JLabel("<html><div style='width:320px'>" + escapeHtml(message) + "</div></html>");
                msgLbl.setForeground(fgSecondary);
                msgLbl.setFont(font(12, Font.PLAIN));
                msgLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
                msgLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentBlock.add(msgLbl);
            }

            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentBlock.add(component);

            JLabel errorLbl = new JLabel();
            errorLbl.setVisible(false);
            errorLbl.setForeground(new Color(0xEF4444));
            errorLbl.setFont(font(12, Font.PLAIN));
            errorLbl.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            errorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentBlock.add(errorLbl);

            body.add(contentBlock, BorderLayout.CENTER);

            if (buttons.isEmpty()) {
                if (showCancelButton) {
                    buttons.add(new Btn<>(cancelText, null, null, false, cancelButtonColor, cancelButtonForeground, false));
                }
                buttons.add(new Btn<>(confirmText, null, resultProvider, true, confirmButtonColor, confirmButtonForeground, true));
            }

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnRow.setOpaque(false);

            Object[] result = {null};
            DialogContext<T> context = new DialogContext<>(resultType, component, dialog);
            java.util.List<JButton> validatingButtons = new ArrayList<>();
            JButton primaryButton = null;

            for (Btn<T> button : buttons) {
                Color btnBg = button.explicitColor != null
                        ? button.explicitColor
                        : button.isPrimary ? accent : secondaryBg;
                Color btnHover = deriveHover(btnBg, 0.12f);
                Color btnFg = button.explicitForeground != null
                        ? button.explicitForeground
                        : readableForeground(btnBg);

                JButton builtButton = buildButton(button, btnBg, btnHover, btnFg, dialog, errorLbl, context, result);
                if (button.validateBeforeClose) {
                    validatingButtons.add(builtButton);
                }
                if (button.isPrimary && primaryButton == null) {
                    primaryButton = builtButton;
                }
                btnRow.add(builtButton);
            }

            installRealtimeValidation(dialog, errorLbl, context, validatingButtons);

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

            if (enterConfirms && primaryButton != null) {
                dialog.getRootPane().setDefaultButton(primaryButton);
            }

            ModernPopupSupport.installCloseOnEsc(dialog.getRootPane(), closeOnEsc, dialog::dispose);

            if (requestFocusOnLoad) {
                installFocusOnLoad(dialog, component);
            }

            dialog.setVisible(true);
            return castResult(result[0]);
        }

        private boolean hasPrimaryButton() {
            for (Btn<T> button : buttons) {
                if (button.isPrimary) {
                    return true;
                }
            }
            return false;
        }

        private JButton buildButton(
                Btn<T> button,
                Color normalBg,
                Color hoverBg,
                Color foreground,
                JDialog dialog,
                JLabel errorLbl,
                DialogContext<T> context,
                Object[] result
        ) {
            JButton btn = new JButton(button.text) {
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
                    g2.setColor(hover ? hoverBg : normalBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    if (isEnabled() && isFocusOwner()) {
                        g2.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 170));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            btn.setForeground(foreground);
            btn.setFont(font(12, Font.PLAIN));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                try {
                    if (button.validateBeforeClose) {
                        validate(context);
                    }

                    T value = resolveButtonValue(button, context);

                    if (button.validateBeforeClose && submitHandler != null) {
                        submitHandler.submit(new SubmitContext<>(value, resultType, component, dialog));
                    }

                    if (button.validateBeforeClose) {
                        hideError(errorLbl, null);
                    }

                    result[0] = value;

                    if (!button.validateBeforeClose || closeOnSubmitSuccess) {
                        dialog.dispose();
                    }
                } catch (Exception ex) {
                    showError(dialog, errorLbl, ex);
                }
            });

            return btn;
        }

        private T resolveButtonValue(Btn<T> button, DialogContext<T> context) throws Exception {
            if (button.provider != null) {
                return castResult(button.provider.get(context));
            }

            if (button.validateBeforeClose) {
                if (resultProvider != null) {
                    return castResult(resultProvider.get(context));
                }
                return defaultValue(context);
            }

            return castResult(button.value);
        }

        private T defaultValue(DialogContext<T> context) {
            if (resultType == null || resultType == Void.class || resultType == Void.TYPE) {
                return null;
            }

            JComponent content = context.component();

            if (content instanceof FormPanel formPanel && Map.class.isAssignableFrom(resultType)) {
                return castResult(formPanel.values());
            }

            if (content instanceof JTextComponent textComponent && resultType == String.class) {
                return castResult(textComponent.getText());
            }

            if (content instanceof JComboBox<?> comboBox) {
                Object selectedItem = comboBox.getSelectedItem();
                if (selectedItem == null || resultType.isInstance(selectedItem)) {
                    return castResult(selectedItem);
                }
                if (resultType == String.class) {
                    return castResult(String.valueOf(selectedItem));
                }
            }

            if (content instanceof JCheckBox checkBox && (resultType == Boolean.class || resultType == Boolean.TYPE)) {
                return castResult(checkBox.isSelected());
            }

            if (resultType.isInstance(content)) {
                return castResult(content);
            }

            return null;
        }

        private void validate(DialogContext<T> context) throws Exception {
            if (validationHandler != null) {
                validationHandler.validate(context);
            }
        }

        private void installRealtimeValidation(
                JDialog dialog,
                JLabel errorLbl,
                DialogContext<T> context,
                java.util.List<JButton> validatingButtons
        ) {
            if (!validateOnChange || validationHandler == null || validatingButtons.isEmpty()) {
                return;
            }

            Timer validationTimer = new Timer(validationDelayMs, e -> {
                try {
                    validate(context);
                    hideError(errorLbl, validatingButtons);
                    repack(dialog);
                } catch (Exception ex) {
                    showError(dialog, errorLbl, validatingButtons, ex);
                }
            });
            validationTimer.setRepeats(false);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    validationTimer.stop();
                }
            });

            installValidationTriggers(component, validationTimer, new HashSet<>());
            for (Component triggerComponent : validationTriggerComponents) {
                installValidationTriggers(triggerComponent, validationTimer, new HashSet<>());
            }
            for (CustomValidationTrigger<?> trigger : customValidationTriggers) {
                trigger.install(() -> restartValidation(validationTimer));
            }
        }

        private void installValidationTriggers(Component current, Timer validationTimer, Set<Component> visited) {
            if (current == null || !visited.add(current)) {
                return;
            }

            if (current instanceof JTextComponent textComponent) {
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

            if (current instanceof JComboBox<?> comboBox) {
                comboBox.addItemListener(e -> restartValidation(validationTimer));
            }

            if (current instanceof AbstractButton button) {
                button.addItemListener(e -> restartValidation(validationTimer));
            }

            if (current instanceof JSpinner spinner) {
                spinner.addChangeListener(e -> restartValidation(validationTimer));
            }

            if (current instanceof JSlider slider) {
                slider.addChangeListener(e -> restartValidation(validationTimer));
            }

            if (current instanceof Container container) {
                for (Component child : container.getComponents()) {
                    installValidationTriggers(child, validationTimer, visited);
                }
            }
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

        @SuppressWarnings("unchecked")
        private T castResult(Object value) {
            if (value == null) {
                return null;
            }

            if (resultType == null || resultType.isInstance(value)) {
                return (T) value;
            }

            if (resultType == Boolean.TYPE && value instanceof Boolean) {
                return (T) value;
            }

            if (resultType == Integer.TYPE && value instanceof Integer) {
                return (T) value;
            }

            if (resultType == Long.TYPE && value instanceof Long) {
                return (T) value;
            }

            if (resultType == Double.TYPE && value instanceof Double) {
                return (T) value;
            }

            throw new IllegalStateException(
                    "Dialog returned " + value.getClass().getName()
                            + " but expected " + resultType.getName() + "."
            );
        }

        private void showError(JDialog dialog, JLabel errorLbl, Exception ex) {
            showError(dialog, errorLbl, null, ex);
        }

        private void showError(JDialog dialog, JLabel errorLbl, Collection<JButton> buttons, Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = text("error.confirm", "Erro ao confirmar.");
            }
            errorLbl.setText("<html><div style='width:320px'>" + escapeHtml(message) + "</div></html>");
            errorLbl.setVisible(true);
            setButtonsEnabled(buttons, false);
            repack(dialog);
        }

        private void hideError(JLabel errorLbl, Collection<JButton> buttons) {
            errorLbl.setText("");
            errorLbl.setVisible(false);
            setButtonsEnabled(buttons, true);
        }

        private void setButtonsEnabled(Collection<JButton> buttons, boolean enabled) {
            if (!disableConfirmWhenInvalid || buttons == null) {
                return;
            }

            for (JButton button : buttons) {
                button.setEnabled(enabled);
            }
        }

        private void repack(JDialog dialog) {
            dialog.setMinimumSize(new Dimension(380, 1));
            dialog.pack();

            Dimension size = dialog.getSize();
            if (size.width < 380) {
                dialog.setSize(380, size.height);
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

        private void normalizeContentSize(JComponent component) {
            Dimension preferred = component.getPreferredSize();
            int width = Math.max(preferred.width, 320);
            int height = Math.max(preferred.height, 32);

            component.setPreferredSize(new Dimension(width, height));
            component.setMinimumSize(new Dimension(Math.min(width, 320), Math.min(height, 32)));
        }
    }

    public static final class FormPanel extends JPanel {

        private final Map<String, JComponent> fields = new LinkedHashMap<>();
        private int row = 0;

        public FormPanel() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public FormPanel field(String name, String label, JComponent component) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(component, "component");

            fields.put(name, component);

            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.WEST;
            labelConstraints.insets = new Insets(0, 0, 8, 10);

            JLabel labelComponent = new JLabel(label == null ? name : label);
            labelComponent.setFont(font(12, Font.PLAIN));
            labelComponent.setForeground(lfForeground());
            add(labelComponent, labelConstraints);

            GridBagConstraints fieldConstraints = new GridBagConstraints();
            fieldConstraints.gridx = 1;
            fieldConstraints.gridy = row;
            fieldConstraints.weightx = 1;
            fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
            fieldConstraints.insets = new Insets(0, 0, 8, 0);
            add(component, fieldConstraints);

            row++;
            return this;
        }

        public FormPanel field(String name, JComponent component) {
            return field(name, name, component);
        }

        public <C extends JComponent> C field(String name, Class<C> type) {
            JComponent field = fields.get(name);
            if (field == null) {
                return null;
            }
            if (!type.isInstance(field)) {
                throw new IllegalArgumentException(
                        "Field '" + name + "' is " + field.getClass().getName()
                                + " but expected " + type.getName() + "."
                );
            }
            return type.cast(field);
        }

        public JComponent field(String name) {
            return fields.get(name);
        }

        public String text(String name) {
            JComponent field = fields.get(name);
            Object value = valueOf(field);
            return value == null ? "" : String.valueOf(value);
        }

        public Object value(String name) {
            return valueOf(fields.get(name));
        }

        public Map<String, Object> values() {
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
                values.put(entry.getKey(), valueOf(entry.getValue()));
            }
            return values;
        }

        public Map<String, JComponent> fields() {
            return Map.copyOf(fields);
        }

        private Object valueOf(JComponent component) {
            if (component == null) {
                return null;
            }
            if (component instanceof JTextComponent textComponent) {
                return textComponent.getText();
            }
            if (component instanceof JComboBox<?> comboBox) {
                return comboBox.getSelectedItem();
            }
            if (component instanceof JCheckBox checkBox) {
                return checkBox.isSelected();
            }
            if (component instanceof JSpinner spinner) {
                return spinner.getValue();
            }
            if (component instanceof JSlider slider) {
                return slider.getValue();
            }
            return component;
        }
    }

    @FunctionalInterface
    public interface FormConfigurer {
        void configure(FormPanel form);
    }

    @FunctionalInterface
    public interface ResultProvider<T> {
        T get(DialogContext<T> context) throws Exception;
    }

    @FunctionalInterface
    public interface ValidationHandler<T> {
        void validate(DialogContext<T> context) throws Exception;
    }

    @FunctionalInterface
    public interface SubmitHandler<T> {
        void submit(SubmitContext<T> context) throws Exception;
    }

    @FunctionalInterface
    public interface ValidationTriggerInstaller<C extends Component> {
        void install(C component, Runnable validate);
    }

    public record DialogContext<T>(
            Class<T> resultType,
            JComponent component,
            JDialog dialog
    ) {
        public FormPanel form() {
            return component instanceof FormPanel formPanel ? formPanel : null;
        }

        public <C extends JComponent> C component(Class<C> type) {
            if (!type.isInstance(component)) {
                throw new IllegalArgumentException(
                        "Dialog component is " + component.getClass().getName()
                                + " but expected " + type.getName() + "."
                );
            }
            return type.cast(component);
        }
    }

    public record SubmitContext<T>(
            T value,
            Class<T> resultType,
            JComponent component,
            JDialog dialog
    ) {
        public FormPanel form() {
            return component instanceof FormPanel formPanel ? formPanel : null;
        }
    }

    private record Btn<T>(
            String text,
            T value,
            ResultProvider<T> provider,
            boolean isPrimary,
            Color explicitColor,
            Color explicitForeground,
            boolean validateBeforeClose
    ) {
    }

    private record CustomValidationTrigger<C extends Component>(
            C component,
            ValidationTriggerInstaller<C> installer
    ) {
        void install(Runnable validate) {
            installer.install(component, validate);
        }
    }

    private static JPanel buildTypeChip(
            ModernDialog.Type type,
            Color accent,
            Color foreground,
            String customLabel
    ) {
        JPanel typeChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        typeChip.setOpaque(false);

        JLabel dot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(0, 2, 7, 7);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(7, 11);
            }
        };

        JLabel typeLabel = new JLabel(customLabel != null ? customLabel : typeText(type));
        typeLabel.setForeground(foreground);
        typeLabel.setFont(font(11, Font.PLAIN));

        typeChip.add(dot);
        typeChip.add(typeLabel);
        return typeChip;
    }

    private static void installDragSupport(JComponent component, JDialog dialog) {
        int[] dragOffset = new int[2];
        component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getX();
                dragOffset[1] = e.getY();
            }
        });
        component.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                dialog.setLocation(
                        dialog.getX() + e.getX() - dragOffset[0],
                        dialog.getY() + e.getY() - dragOffset[1]
                );
            }
        });
    }

    private static void installFocusOnLoad(JDialog dialog, JComponent content) {
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                dialog.removeWindowListener(this);
                SwingUtilities.invokeLater(() -> {
                    Component target = firstFocusable(content);
                    if (target != null && !target.requestFocusInWindow()) {
                        target.requestFocus();
                    }
                });
            }
        });
    }

    private static Component firstFocusable(Component component) {
        if (component.isVisible() && component.isEnabled() && component.isFocusable()) {
            return component;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component target = firstFocusable(child);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    private static JButton buildCloseBtn(JDialog dialog, Color fg, boolean darkTheme) {
        JButton btn = new JButton("x") {
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
                Color hoverColor = darkTheme ? new Color(255, 255, 255, 22) : new Color(0, 0, 0, 22);
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

    private static String typeText(ModernDialog.Type type) {
        return switch (type) {
            case SUCCESS -> text("type.success", "Sucesso");
            case ERROR -> text("type.error", "Erro");
            case QUESTION -> text("type.question", "Pergunta");
            default -> text("type.info", "Informacao");
        };
    }

    private static Icon buildIcon(ModernDialog.Type type, Color accent) {
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

                switch (type) {
                    case SUCCESS:
                        g2.drawOval(cx - 9, cy - 9, 18, 18);
                        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                        g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
                        break;
                    case ERROR:
                        g2.drawOval(cx - 9, cy - 9, 18, 18);
                        g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                        g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                        break;
                    case QUESTION:
                        g2.drawOval(cx - 9, cy - 9, 18, 18);
                        g2.drawArc(cx - 4, cy - 7, 8, 7, 0, 180);
                        g2.drawLine(cx, cy, cx, cy + 3);
                        g2.fillOval(cx - 1, cy + 5, 2, 2);
                        break;
                    default:
                        g2.drawOval(cx - 9, cy - 9, 18, 18);
                        g2.drawLine(cx, cy - 2, cx, cy + 5);
                        g2.fillOval(cx - 1, cy - 6, 2, 2);
                        break;
                }

                g2.dispose();
            }
        };
    }

    private static Color lfBackground() {
        Color color = UIManager.getColor("Panel.background");
        return color != null ? color : new Color(0x22252B);
    }

    private static Color lfForeground() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.WHITE;
    }

    private static Color resolveAccent(Color global, ModernDialog.Type type) {
        if (global != null) {
            return global;
        }

        Color lf = UIManager.getColor("Button.select");
        if (lf != null) {
            return lf;
        }

        return defaultAccentFor(type);
    }

    private static Color defaultAccentFor(ModernDialog.Type type) {
        return switch (type) {
            case SUCCESS -> new Color(0x22C55E);
            case ERROR -> new Color(0xEF4444);
            case QUESTION -> new Color(0xF0C040);
            default -> new Color(0x3B82F6);
        };
    }

    private static Color readableForeground(Color bg) {
        return isDark(bg) ? Color.WHITE : new Color(0x1a1a1a);
    }

    private static boolean isDark(Color color) {
        double lum = 0.2126 * (color.getRed() / 255.0)
                + 0.7152 * (color.getGreen() / 255.0)
                + 0.0722 * (color.getBlue() / 255.0);
        return lum < 0.35;
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

    private static Font font(float size, int style) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font("Segoe UI", style, (int) size);
        }
        return base.deriveFont(style, size);
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
