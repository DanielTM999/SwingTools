package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;
import lombok.Getter;
import java.math.RoundingMode;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class CurrencyField extends MaskedTextField {

    private final AtomicReference<NumberFormat> currencyFormatterRef;
    private final int decimalPlaces;
    @Getter
    private BigDecimal value;
    private boolean isUpdating = false;
    private String valueOnFocusGain;

    public CurrencyField() {
        this(Locale.getDefault());
    }

    public CurrencyField(Locale locale) {
        super();
        this.currencyFormatterRef = new AtomicReference<>(NumberFormat.getCurrencyInstance(locale));
        this.decimalPlaces = currencyFormatterRef.get().getMaximumFractionDigits();
        this.value = BigDecimal.ZERO.setScale(decimalPlaces, RoundingMode.HALF_UP);
        this.valueOnFocusGain = formatValue(this.value);

        setHorizontalAlignment(JTextField.LEFT);
        setText(valueOnFocusGain);

        ((AbstractDocument) getDocument()).setDocumentFilter(new CurrencyDocumentFilter());

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                valueOnFocusGain = getText();
                SwingUtilities.invokeLater(CurrencyField.this::selectAll);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!Objects.equals(getText(), valueOnFocusGain)) {
                    dispachEvent(EventType.CHANGE, e);
                    valueOnFocusGain = getText();
                }
            }
        });
    }

    public void setValueLocale(Locale locale){
        if(locale == null) return;
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        if(numberFormat == null) return;
        this.currencyFormatterRef.set(numberFormat);
    }

    public void setValue(BigDecimal value) {
        BigDecimal oldValue = this.value;

        if (value == null) {
            value = BigDecimal.ZERO;
        }
        this.value = value.setScale(decimalPlaces, RoundingMode.HALF_UP);

        setText(formatValue(this.value));

        valueOnFocusGain = getText();

        if (!Objects.equals(oldValue, this.value)) {
            dispachEvent(EventType.CHANGE, this::getValue);
        }
    }

    private String formatValue(BigDecimal val) {
        return currencyFormatterRef.get().format(val);
    }

    private class CurrencyDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (isUpdating) {
                fb.insertString(offset, string, attr);
                return;
            }
            String newText = getNewText(fb, offset, 0, string);
            updateValue(fb, newText);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (isUpdating) {
                fb.remove(offset, length);
                return;
            }
            String newText = getNewText(fb, offset, length, "");
            updateValue(fb, newText);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (isUpdating) {
                fb.replace(offset, length, text, attrs);
                return;
            }
            String newText = getNewText(fb, offset, length, text);
            updateValue(fb, newText);
        }

        private String getNewText(FilterBypass fb, int offset, int length, String text) throws BadLocationException {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);
            return sb.toString();
        }

        private void updateValue(FilterBypass fb, String proposedText) throws BadLocationException {
            isUpdating = true;
            try {
                String digits = proposedText.replaceAll("\\D", "");

                BigDecimal newValue;
                if (digits.isEmpty()) {
                    newValue = BigDecimal.ZERO.setScale(decimalPlaces, RoundingMode.HALF_UP);
                } else {
                    BigDecimal rawValue = new BigDecimal(digits);
                    newValue = rawValue.movePointLeft(decimalPlaces);
                }

                CurrencyField.this.value = newValue;

                String formattedText = formatValue(newValue);

                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formattedText, null);

                SwingUtilities.invokeLater(() -> setCaretPosition(getText().length()));

                dispachEvent(EventType.INPUT, null);

            } finally {
                isUpdating = false;
            }
        }
    }
}