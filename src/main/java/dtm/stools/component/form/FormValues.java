package dtm.stools.component.form;

import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.inputfields.checkfield.RadioGroupField;
import dtm.stools.component.inputfields.duallistfield.DualListField;
import dtm.stools.component.inputfields.pinfield.PinField;
import dtm.stools.component.inputfields.ratingfield.RatingField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.component.inputfields.switchfield.SwitchField;
import dtm.stools.component.inputfields.textarea.TextAreaField;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.text.JTextComponent;
import java.math.BigDecimal;
import java.util.List;

/**
 * Leitura e escrita do valor de controles suportados pelos formulários.
 */
public final class FormValues {

    private FormValues() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Lê o valor corrente do controle informado.
     */
    public static Object read(JComponent control) {
        return switch (control) {
            case JTextComponent text -> text.getText();
            case TextAreaField area -> area.getText();
            case JComboBox<?> combo -> combo.getSelectedItem();
            case CheckBoxField check -> check.isSelected();
            case SwitchField toggle -> toggle.isSelected();
            case AbstractButton button -> button.isSelected();
            case RadioGroupField<?> group -> group.getSelectedValue();
            case SegmentedField<?> segmented -> segmented.getSelectedValue();
            case SliderField slider -> slider.getValue();
            case JSlider slider -> slider.getValue();
            case RatingField rating -> rating.getValue();
            case PinField pin -> pin.getValue();
            case StepperField stepper -> stepper.getValue();
            case DualListField<?> dual -> dual.getSelected();
            case JSpinner spinner -> spinner.getValue();
            case JList<?> list -> list.getSelectedValuesList();
            default -> null;
        };
    }

    /**
     * Escreve o valor no controle informado sem disparar eventos de mudança.
     */
    @SuppressWarnings("unchecked")
    public static void write(JComponent control, Object value) {
        switch (control) {
            case JTextComponent text -> text.setText(value != null ? String.valueOf(value) : "");
            case TextAreaField area -> area.setText(value != null ? String.valueOf(value) : "", false);
            case JComboBox<?> combo -> ((JComboBox<Object>) combo).setSelectedItem(value);
            case CheckBoxField check -> check.setSelected(Boolean.TRUE.equals(value), false);
            case SwitchField toggle -> toggle.setSelected(Boolean.TRUE.equals(value), false);
            case AbstractButton button -> button.setSelected(Boolean.TRUE.equals(value));
            case RadioGroupField<?> group -> ((RadioGroupField<Object>) group).setSelectedValue(value, false);
            case SegmentedField<?> segmented -> ((SegmentedField<Object>) segmented).setSelectedValue(value, false);
            case SliderField slider -> slider.setValue(toDouble(value), false);
            case JSlider slider -> slider.setValue((int) toDouble(value));
            case RatingField rating -> rating.setValue(toDouble(value), false);
            case PinField pin -> pin.setValue(value != null ? String.valueOf(value) : "", false);
            case StepperField stepper -> stepper.setValue(toBigDecimal(value), false);
            case DualListField<?> dual -> ((DualListField<Object>) dual).setSelected(toList(value), false);
            case JSpinner spinner -> spinner.setValue(value);
            default -> {
            }
        }
    }

    /**
     * Restaura o controle ao seu estado vazio.
     */
    public static void clear(JComponent control) {
        switch (control) {
            case JTextComponent text -> text.setText("");
            case TextAreaField area -> area.setText("", false);
            case JComboBox<?> combo -> combo.setSelectedIndex(combo.getItemCount() > 0 ? 0 : -1);
            case CheckBoxField check -> check.setSelected(false, false);
            case SwitchField toggle -> toggle.setSelected(false, false);
            case AbstractButton button -> button.setSelected(false);
            case RadioGroupField<?> group -> group.clearSelection(false);
            case SegmentedField<?> segmented -> segmented.setSelectedIndex(0, false);
            case SliderField slider -> slider.setValue(slider.getMinimum(), false);
            case RatingField rating -> rating.setValue(0d, false);
            case PinField pin -> pin.setValue("", false);
            case StepperField stepper -> stepper.setValue(BigDecimal.ZERO, false);
            case DualListField<?> dual -> ((DualListField<Object>) dual).setSelected(List.of(), false);
            default -> {
            }
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : 0d;
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return value != null ? new BigDecimal(String.valueOf(value)) : BigDecimal.ZERO;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return value != null ? List.of(value) : List.of();
    }
}
