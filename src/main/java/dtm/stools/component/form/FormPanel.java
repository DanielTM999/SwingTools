package dtm.stools.component.form;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container de formulário que organiza campos em colunas, valida em bloco e expõe os valores como mapa.
 */
public class FormPanel extends PanelEventListener {

    public static final String VALIDATION_FAILED = "formValidationFailed";
    public static final String VALIDATION_PASSED = "formValidationPassed";

    private final Map<String, FormField> fields = new LinkedHashMap<>();

    private int columns = 1;
    private int gap = UiTokens.space(4);

    public FormPanel() {
        this(1);
    }

    public FormPanel(int columns) {
        super(null, false);
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be greater than zero");
        }
        this.columns = columns;
        setOpaque(false);
        applyLayout();
    }

    /**
     * Adiciona um campo com rótulo e controle.
     */
    public FormPanel addField(String name, String label, JComponent control) {
        return addField(new FormField(name, label, control));
    }

    /**
     * Adiciona um campo com rótulo, controle e regra de validação.
     */
    public <T> FormPanel addField(String name, String label, JComponent control, Validator<T> validator) {
        return addField(new FormField(name, label, control).setValidator(validator));
    }

    /**
     * Adiciona um campo já construído.
     */
    public FormPanel addField(FormField field) {
        if (field == null) {
            throw new IllegalArgumentException("field cannot be null");
        }
        if (fields.containsKey(field.getFieldName())) {
            throw new IllegalArgumentException("duplicated field name: " + field.getFieldName());
        }
        fields.put(field.getFieldName(), field);
        applyFieldSpacing(field);
        add(field, FlexBoxLayout.FlexConstraints.of().widthPercent(100d / columns));
        revalidate();
        repaint();
        return this;
    }

    /**
     * Adiciona um título de seção entre os campos.
     */
    public FormPanel addSectionTitle(String title) {
        JLabel label = new JLabel(title != null ? title : "");
        label.setFont(UiTokens.font().deriveFont(Font.BOLD));
        label.setForeground(UiTokens.foreground());
        add(label, FlexBoxLayout.FlexConstraints.of().widthPercent(100d).fixedHeight(UiTokens.scale(28)));
        revalidate();
        repaint();
        return this;
    }

    /**
     * Campo registrado com o nome informado.
     */
    public FormField getField(String name) {
        return fields.get(name);
    }

    /**
     * Campos registrados, na ordem de inserção.
     */
    public Map<String, FormField> getFields() {
        return Map.copyOf(fields);
    }

    /**
     * Valores correntes de todos os campos.
     */
    public Map<String, Object> getValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        fields.forEach((name, field) -> values.put(name, field.getValue()));
        return values;
    }

    /**
     * Escreve os valores informados nos campos correspondentes.
     */
    public FormPanel setValues(Map<String, Object> values) {
        if (values == null) {
            return this;
        }
        values.forEach((name, value) -> {
            FormField field = fields.get(name);
            if (field != null) {
                field.setValue(value);
            }
        });
        return this;
    }

    /**
     * Valida todos os campos e devolve as mensagens de erro encontradas.
     */
    public Map<String, String> validateAll() {
        Map<String, String> errors = new LinkedHashMap<>();
        fields.forEach((name, field) -> {
            ValidationResult result = field.validateField();
            if (result.isInvalid()) {
                errors.put(name, result.message());
            }
        });

        Map<String, Object> props = Map.of("errors", errors, "values", getValues());
        dispatchEvent(errors.isEmpty() ? VALIDATION_PASSED : VALIDATION_FAILED, this, errors, props);
        dispatchEvent(EventType.VALIDATE, this, errors.isEmpty(), props);
        return errors;
    }

    /**
     * Indica se todos os campos passam na validação.
     */
    public boolean isFormValid() {
        return validateAll().isEmpty();
    }

    /**
     * Valida o formulário e, sem erros, dispara o evento de envio com os valores.
     */
    public boolean submit() {
        Map<String, String> errors = validateAll();
        if (!errors.isEmpty()) {
            return false;
        }
        Map<String, Object> values = getValues();
        dispatchEvent(EventType.SUBMIT, this, values, Map.of("values", values));
        return true;
    }

    /**
     * Restaura todos os campos ao estado vazio.
     */
    public FormPanel reset() {
        fields.values().forEach(FormField::reset);
        dispatchEvent(EventType.CLEAR, this, getValues(), Map.of());
        return this;
    }

    /**
     * Limpa as mensagens de erro sem alterar os valores.
     */
    public FormPanel clearErrors() {
        fields.values().forEach(FormField::clearError);
        return this;
    }

    /**
     * Define a quantidade de colunas do formulário.
     */
    public FormPanel setColumns(int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be greater than zero");
        }
        this.columns = columns;
        applyLayout();
        for (Component component : getComponents()) {
            if (component instanceof FormField) {
                ((FlexBoxLayout) getLayout()).addLayoutComponent(component,
                        FlexBoxLayout.FlexConstraints.of().widthPercent(100d / columns));
            }
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Quantidade de colunas do formulário.
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Define o espaçamento entre os campos.
     */
    public FormPanel setGap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("gap cannot be negative");
        }
        this.gap = gap;
        applyLayout();
        fields.values().forEach(this::applyFieldSpacing);
        revalidate();
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (fields != null) {
            fields.values().forEach(field -> field.setEnabled(enabled));
        }
    }

    private void applyLayout() {
        setLayout(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.ROW)
                .align(FlexBoxLayout.Align.START)
                .justify(FlexBoxLayout.Justify.START)
                .wrap(true)
                .gap(0)
                .build());
    }

    private void applyFieldSpacing(FormField field) {
        field.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, gap, gap));
    }
}
