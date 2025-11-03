package dtm.stools.component.textfields;

import dtm.stools.component.textfields.events.EventComponent;
import dtm.stools.component.textfields.events.EventListenerComponent;
import dtm.stools.component.textfields.events.EventType;
import lombok.Getter;
import java.math.RoundingMode;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CurrencyField extends JTextField implements EventListenerComponent {

    private final NumberFormat currencyFormatter;
    private final int decimalPlaces;
    @Getter
    private BigDecimal value;
    private final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();
    private boolean isUpdating = false;
    private String valueOnFocusGain;

    public CurrencyField() {
        this(Locale.getDefault());
    }

    public CurrencyField(Locale locale) {
        super();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        this.decimalPlaces = currencyFormatter.getMaximumFractionDigits();
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

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if (eventType == null || eventType.isEmpty() || event == null) return;

        if (eventType.equals(EventType.CHANGE) || eventType.equals(EventType.INPUT)) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        }
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
            dispachEvent(EventType.CHANGE, null);
        }
    }

    private String formatValue(BigDecimal val) {
        return currencyFormatter.format(val);
    }

    private void dispachEvent(String eventType, AWTEvent originalEvent) {
        List<Consumer<EventComponent>> consumers = listeners.get(eventType);

        if (consumers != null && !consumers.isEmpty()) {
            EventComponent event = new EventComponent() {
                private final long timestamp = System.currentTimeMillis();

                @Override
                public Component getComponent() {
                    return CurrencyField.this;
                }

                @Override
                public Object getValue() {
                    return CurrencyField.this.getValue();
                }

                @SuppressWarnings("unchecked")
                @Override
                public <T> T tryGetValue() {
                    try {
                        return (T) getValue();
                    } catch (ClassCastException e) {
                        return null;
                    }
                }

                @Override
                public String getEventType() {
                    return eventType;
                }

            };

            SwingUtilities.invokeLater(() -> {
                consumers.forEach(listener -> listener.accept(event));
            });
        }
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