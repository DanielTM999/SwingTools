package dtm.stools.component.form;

import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;
import dtm.stools.utils.PaintUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;

/**
 * Envolve um controle com rótulo, marcação de obrigatório, texto de ajuda e mensagem de erro.
 */
public class FormField extends PanelEventListener {

    public static final String VALID = "fieldValid";
    public static final String INVALID = "fieldInvalid";

    private final String name;
    private final JLabel label = new JLabel();
    private final JComponent control;
    private final JLabel message = new JLabel();

    private Validator<Object> validator;
    private String labelText;
    private String helperText = "";
    private boolean required;
    private boolean valid = true;
    private boolean validateOnChange = true;

    public FormField(String name, String labelText, JComponent control) {
        super(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.STRETCH)
                .gap(UiTokens.space(1))
                .build(), false);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (control == null) {
            throw new IllegalArgumentException("control cannot be null");
        }

        this.name = name;
        setName(name);
        this.control = control;
        this.labelText = labelText != null ? labelText : "";

        setOpaque(false);
        configureLabel();
        configureMessage();

        add(label, FlexBoxLayout.FlexConstraints.of().fixedHeight(labelHeight()));
        add(control, FlexBoxLayout.FlexConstraints.of().grow(1).minHeight(UiTokens.scale(28)));
        add(message, FlexBoxLayout.FlexConstraints.of().fixedHeight(messageHeight()));

        installListeners();
        refreshLabel();
        refreshMessage();
    }

    /**
     * Nome usado como chave do campo dentro do formulário.
     */
    public String getFieldName() {
        return name;
    }

    /**
     * Controle envolvido por este campo.
     */
    public JComponent getControl() {
        return control;
    }

    /**
     * Valor corrente do controle.
     */
    public Object getValue() {
        return FormValues.read(control);
    }

    /**
     * Escreve o valor no controle sem disparar eventos.
     */
    public FormField setValue(Object value) {
        FormValues.write(control, value);
        return this;
    }

    /**
     * Define a regra de validação aplicada ao campo.
     */
    @SuppressWarnings("unchecked")
    public <T> FormField setValidator(Validator<T> validator) {
        this.validator = (Validator<Object>) validator;
        return this;
    }

    /**
     * Marca o campo como obrigatório, encadeando a regra correspondente.
     */
    public FormField setRequired(boolean required) {
        this.required = required;
        refreshLabel();
        return this;
    }

    /**
     * Indica se o campo é obrigatório.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Define o texto de ajuda exibido quando não há erro.
     */
    public FormField setHelperText(String helperText) {
        this.helperText = helperText != null ? helperText : "";
        refreshMessage();
        return this;
    }

    /**
     * Define o rótulo do campo.
     */
    public FormField setLabelText(String labelText) {
        this.labelText = labelText != null ? labelText : "";
        refreshLabel();
        return this;
    }

    /**
     * Habilita a revalidação automática a cada alteração do controle.
     */
    public FormField setValidateOnChange(boolean validateOnChange) {
        this.validateOnChange = validateOnChange;
        return this;
    }

    /**
     * Indica se o campo passou na última validação.
     */
    public boolean isFieldValid() {
        return valid;
    }

    /**
     * Executa a validação do campo e atualiza a mensagem exibida.
     */
    public ValidationResult validateField() {
        Object value = getValue();
        ValidationResult result = ValidationResult.ok();

        if (required) {
            result = Validators.required().validate(value);
        }
        if (result.valid() && validator != null) {
            result = validator.validate(value);
        }

        applyResult(result);
        return result;
    }

    /**
     * Limpa o estado de erro sem executar a validação.
     */
    public FormField clearError() {
        applyResult(ValidationResult.ok());
        return this;
    }

    /**
     * Exibe uma mensagem de erro definida externamente.
     */
    public FormField setError(String errorMessage) {
        applyResult(ValidationResult.error(errorMessage));
        return this;
    }

    /**
     * Restaura o controle ao estado vazio e limpa o erro.
     */
    public FormField reset() {
        FormValues.clear(control);
        return clearError();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        control.setEnabled(enabled);
        label.setForeground(enabled ? UiTokens.foreground() : UiTokens.disabled(UiTokens.foreground()));
    }

    @Override
    public java.awt.Dimension getPreferredSize() {
        java.awt.Insets insets = getInsets();
        int height = labelHeight()
                + Math.max(control.getPreferredSize().height, UiTokens.scale(28))
                + messageHeight()
                + UiTokens.space(1) * 2
                + insets.top + insets.bottom;
        int width = Math.max(control.getPreferredSize().width, UiTokens.scale(180))
                + insets.left + insets.right;
        return new java.awt.Dimension(width, height);
    }

    private int labelHeight() {
        return getFontMetrics(UiTokens.fontSmall().deriveFont(Font.BOLD)).getHeight() + UiTokens.scale(2);
    }

    private int messageHeight() {
        return getFontMetrics(UiTokens.fontSmall()).getHeight() + UiTokens.scale(2);
    }

    private void configureLabel() {
        label.setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));
        label.setForeground(UiTokens.foreground());
    }

    private void configureMessage() {
        message.setFont(UiTokens.fontSmall());
        message.setForeground(UiTokens.muted());
    }

    private void installListeners() {
        if (control instanceof EventListenerComponent listener) {
            listener.addEventListener(EventType.CHANGE, event -> revalidateOnChange());
            listener.addEventListener(EventType.INPUT, event -> revalidateOnChange());
            return;
        }

        if (control instanceof JTextComponent text) {
            text.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    revalidateOnChange();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    revalidateOnChange();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    revalidateOnChange();
                }
            });
        }
    }

    private void revalidateOnChange() {
        if (!validateOnChange || valid) {
            return;
        }
        validateField();
    }

    private void applyResult(ValidationResult result) {
        boolean previous = valid;
        valid = result.valid();
        refreshMessage(result.message());
        applyErrorHighlight();

        if (previous == valid) {
            return;
        }

        Map<String, Object> props = Map.of(
                "field", name,
                "message", result.message() != null ? result.message() : "");
        dispatchEvent(valid ? VALID : INVALID, this, getValue(), props);
        dispatchEvent(EventType.VALIDATE, this, valid, props);
    }

    private void applyErrorHighlight() {
        if (control instanceof dtm.stools.component.inputfields.textarea.TextAreaField area) {
            area.setErrorState(!valid);
            return;
        }
        control.putClientProperty("JComponent.outline", valid ? null : "error");
        control.repaint();
    }

    private void refreshLabel() {
        label.setText(required && !labelText.isEmpty() ? labelText + " *" : labelText);
        label.setVisible(!labelText.isEmpty());
    }

    private void refreshMessage() {
        refreshMessage(null);
    }

    private void refreshMessage(String errorMessage) {
        boolean hasError = !valid && errorMessage != null && !errorMessage.isEmpty();
        message.setText(hasError ? errorMessage : helperText);
        message.setForeground(hasError ? UiTokens.danger() : UiTokens.muted());
        message.setVisible(hasError || !helperText.isEmpty());
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (valid) {
            return;
        }
        java.awt.Graphics2D g2 = PaintUtils.antialias((java.awt.Graphics2D) g.create());
        try {
            Color accent = UiTokens.overlay(UiTokens.danger(), 0.55f);
            g2.setColor(accent);
            g2.fillRect(0, control.getY(), 2, control.getHeight());
        } finally {
            g2.dispose();
        }
    }
}
