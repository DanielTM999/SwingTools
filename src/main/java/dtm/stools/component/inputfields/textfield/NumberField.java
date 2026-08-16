package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * A locale-aware numeric text field backed by {@link BigDecimal}.
 * Intermediate editing states are allowed and values are normalized when the
 * field loses focus or the user presses Enter.
 */
public class NumberField extends JTextFieldListener {

    private Locale numberLocale;
    private DecimalFormatSymbols symbols;
    private DecimalFormat formatter;

    private BigDecimal value;
    private BigDecimal committedValue;
    private BigDecimal minimumValue;
    private BigDecimal maximumValue;
    private BigDecimal step = BigDecimal.ONE;
    private int decimalPlaces = 2;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
    private boolean updatingDocument;

    public NumberField() {
        this(Locale.getDefault());
    }

    public NumberField(Locale locale) {
        super();
        configureLocale(Objects.requireNonNull(locale, "locale"));
        setHorizontalAlignment(JTextField.RIGHT);
        ((AbstractDocument) getDocument()).setDocumentFilter(new NumberDocumentFilter());
        installInteractionListeners();
    }

    private void installInteractionListeners() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitCurrentValue();
            }
        });

        addActionListener(e -> {
            commitCurrentValue();
            dispachEvent(EventType.SUBMIT, NumberField.this::getValue);
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || !isEditable()) return;
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    increment(1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    increment(-1);
                    e.consume();
                }
            }
        });

        addMouseWheelListener(this::handleMouseWheel);
    }

    private void handleMouseWheel(MouseWheelEvent event) {
        if (!isEnabled() || !isEditable() || !isFocusOwner() || event.getWheelRotation() == 0) return;
        increment(event.getWheelRotation() < 0 ? 1 : -1);
        event.consume();
    }

    public BigDecimal getValue() {
        return value;
    }

    public NumberField setValue(BigDecimal value) {
        return setValue(value, true);
    }

    public NumberField setValue(BigDecimal value, boolean fireEvent) {
        BigDecimal normalized = normalize(value);
        BigDecimal oldValue = committedValue;
        replaceDocumentText(formatValue(normalized));
        this.value = normalized;
        this.committedValue = normalized;
        if (fireEvent && valuesDiffer(oldValue, normalized)) {
            dispachEvent(EventType.CHANGE, this::getValue);
        }
        return this;
    }

    public NumberField setRange(BigDecimal minimumValue, BigDecimal maximumValue) {
        BigDecimal normalizedMinimum = scaleBoundary(minimumValue);
        BigDecimal normalizedMaximum = scaleBoundary(maximumValue);
        validateRange(normalizedMinimum, normalizedMaximum);
        this.minimumValue = normalizedMinimum;
        this.maximumValue = normalizedMaximum;
        normalizeAfterConfigurationChange();
        return this;
    }

    public NumberField setMinimumValue(BigDecimal minimumValue) {
        return setRange(minimumValue, maximumValue);
    }

    public NumberField setMaximumValue(BigDecimal maximumValue) {
        return setRange(minimumValue, maximumValue);
    }

    public BigDecimal getMinimumValue() {
        return minimumValue;
    }

    public BigDecimal getMaximumValue() {
        return maximumValue;
    }

    public NumberField setStep(BigDecimal step) {
        if (step == null || step.signum() <= 0) {
            throw new IllegalArgumentException("Step must be greater than zero");
        }
        this.step = step;
        return this;
    }

    public BigDecimal getStep() {
        return step;
    }

    public NumberField setDecimalPlaces(int decimalPlaces) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("Decimal places cannot be negative");
        this.decimalPlaces = decimalPlaces;
        minimumValue = scaleBoundary(minimumValue);
        maximumValue = scaleBoundary(maximumValue);
        rebuildFormatter();
        normalizeAfterConfigurationChange();
        return this;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public NumberField setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = Objects.requireNonNull(roundingMode, "roundingMode");
        minimumValue = scaleBoundary(minimumValue);
        maximumValue = scaleBoundary(maximumValue);
        rebuildFormatter();
        normalizeAfterConfigurationChange();
        return this;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public NumberField setNumberLocale(Locale locale) {
        configureLocale(Objects.requireNonNull(locale, "locale"));
        replaceDocumentText(formatValue(value));
        return this;
    }

    public Locale getNumberLocale() {
        return numberLocale;
    }

    public boolean isValueWithinRange() {
        return value == null || isWithinRange(value);
    }

    /**
     * Normalizes the current text, applies scale and range constraints, and
     * emits CHANGE only when the last committed value actually changes.
     */
    public NumberField commitValue() {
        commitCurrentValue();
        return this;
    }

    private void commitCurrentValue() {
        BigDecimal parsed = parsePotentialValue(getText());
        BigDecimal normalized = normalize(parsed);
        BigDecimal oldValue = committedValue;
        replaceDocumentText(formatValue(normalized));
        value = normalized;
        committedValue = normalized;
        if (valuesDiffer(oldValue, normalized)) {
            dispachEvent(EventType.CHANGE, this::getValue);
        }
    }

    private void increment(int direction) {
        BigDecimal base = value;
        if (base == null) base = minimumValue != null ? minimumValue : BigDecimal.ZERO;
        BigDecimal next = normalize(base.add(step.multiply(BigDecimal.valueOf(direction))));
        BigDecimal oldValue = committedValue;
        replaceDocumentText(formatValue(next));
        value = next;
        committedValue = next;
        dispachEvent(EventType.INPUT, this::getValue);
        if (valuesDiffer(oldValue, next)) {
            dispachEvent(EventType.CHANGE, this::getValue);
        }
    }

    private void normalizeAfterConfigurationChange() {
        BigDecimal normalized = normalize(value);
        replaceDocumentText(formatValue(normalized));
        value = normalized;
        committedValue = normalized;
    }

    private BigDecimal normalize(BigDecimal candidate) {
        if (candidate == null) return null;
        BigDecimal normalized = candidate.setScale(decimalPlaces, roundingMode);
        if (minimumValue != null && normalized.compareTo(minimumValue) < 0) normalized = minimumValue;
        if (maximumValue != null && normalized.compareTo(maximumValue) > 0) normalized = maximumValue;
        return normalized.setScale(decimalPlaces, roundingMode);
    }

    private boolean isWithinRange(BigDecimal candidate) {
        return (minimumValue == null || candidate.compareTo(minimumValue) >= 0)
                && (maximumValue == null || candidate.compareTo(maximumValue) <= 0);
    }

    private void validateRange(BigDecimal minimum, BigDecimal maximum) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Minimum value cannot be greater than maximum value");
        }
    }

    private BigDecimal scaleBoundary(BigDecimal boundary) {
        return boundary == null ? null : boundary.setScale(decimalPlaces, roundingMode);
    }

    private void configureLocale(Locale locale) {
        numberLocale = locale;
        symbols = DecimalFormatSymbols.getInstance(locale);
        rebuildFormatter();
    }

    private void rebuildFormatter() {
        formatter = new DecimalFormat("0", symbols);
        formatter.setGroupingUsed(false);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(decimalPlaces);
        formatter.setRoundingMode(roundingMode);
    }

    private String formatValue(BigDecimal candidate) {
        return candidate == null ? "" : formatter.format(candidate);
    }

    private void replaceDocumentText(String text) {
        updatingDocument = true;
        try {
            super.setText(text == null ? "" : text);
            setCaretPosition(getDocument().getLength());
        } finally {
            updatingDocument = false;
        }
    }

    private boolean isPotentialNumber(String text) {
        if (text == null || text.isEmpty()) return true;
        char decimalSeparator = symbols.getDecimalSeparator();
        char localeMinus = symbols.getMinusSign();
        boolean separatorSeen = false;
        int fractionalDigits = 0;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if ((character == '-' || character == localeMinus) && i == 0) continue;
            if (character == decimalSeparator && decimalPlaces > 0 && !separatorSeen) {
                separatorSeen = true;
                continue;
            }
            if (!Character.isDigit(character)) return false;
            if (separatorSeen && ++fractionalDigits > decimalPlaces) return false;
        }
        return true;
    }

    private BigDecimal parsePotentialValue(String text) {
        if (text == null || text.isEmpty()) return null;
        char decimalSeparator = symbols.getDecimalSeparator();
        char localeMinus = symbols.getMinusSign();
        StringBuilder normalized = new StringBuilder(text.length());
        boolean hasDigit = false;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '-' || character == localeMinus) {
                normalized.append('-');
            } else if (character == decimalSeparator) {
                normalized.append('.');
            } else if (Character.isDigit(character)) {
                normalized.append(Character.digit(character, 10));
                hasDigit = true;
            }
        }
        if (!hasDigit) return null;
        try {
            return new BigDecimal(normalized.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean valuesDiffer(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) return first != second;
        return first.compareTo(second) != 0;
    }

    private class NumberDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attributes)
                throws BadLocationException {
            replace(bypass, offset, 0, string, attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            replace(bypass, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            if (updatingDocument) {
                bypass.replace(offset, length, text, attributes);
                return;
            }

            String replacement = text == null ? "" : text;
            String proposed = proposedText(bypass.getDocument(), offset, length, replacement);
            if (!isPotentialNumber(proposed)) return;

            bypass.replace(offset, length, replacement, attributes);
            value = parsePotentialValue(proposed);
            dispachEvent(EventType.INPUT, NumberField.this::getValue);
        }

        private String proposedText(Document document, int offset, int length, String replacement)
                throws BadLocationException {
            StringBuilder current = new StringBuilder(document.getText(0, document.getLength()));
            current.replace(offset, offset + length, replacement);
            return current.toString();
        }
    }
}
