package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;

/**
 * Área de texto moderna com placeholder, contador de caracteres, limite opcional e crescimento automático.
 */
public class TextAreaField extends PanelEventListener {

    public static final String LIMIT_REACHED = "textAreaLimitReached";

    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(textArea);
    private final JLabel counterLabel = new JLabel();

    private String placeholder = "";
    private int maxLength = -1;
    private boolean showCounter = true;
    private boolean autoGrow;
    private int minRows = 3;
    private int maxRows = 10;
    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private boolean focused;
    private boolean errorState;

    private Color backgroundColor;
    private Color borderColor;
    private Color focusBorderColor;
    private Color placeholderColor;

    public TextAreaField() {
        this("");
    }

    public TextAreaField(String placeholder) {
        super(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.STRETCH)
                .gap(UiTokens.space(1))
                .build(), false);

        this.placeholder = placeholder != null ? placeholder : "";

        setOpaque(false);
        configureTextArea();
        configureScrollPane();
        configureCounter();

        add(scrollPane, FlexBoxLayout.FlexConstraints.of().grow(1));
        add(counterLabel, FlexBoxLayout.FlexConstraints.of().fixedHeight(UiTokens.scale(16)));

        installListeners();
        updateCounter();
        setPreferredSize(new Dimension(UiTokens.scale(320), UiTokens.scale(110)));
    }

    /**
     * Texto digitado.
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Define o texto disparando eventos.
     */
    public TextAreaField setText(String text) {
        return setText(text, true);
    }

    /**
     * Define o texto, opcionalmente sem disparar eventos.
     */
    public TextAreaField setText(String text, boolean fireEvent) {
        String previous = textArea.getText();
        textArea.setText(text != null ? text : "");
        updateCounter();
        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", previous, "newValue", textArea.getText());
            dispatchEvent(EventType.CHANGE, this, textArea.getText(), props);
        }
        return this;
    }

    /**
     * Texto exibido quando o campo está vazio e sem foco.
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Define o texto de placeholder.
     */
    public TextAreaField setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        repaint();
        return this;
    }

    /**
     * Limite de caracteres; valor negativo remove o limite.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Define o limite de caracteres aceito pelo campo.
     */
    public TextAreaField setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        if (maxLength >= 0 && textArea.getText().length() > maxLength) {
            textArea.setText(textArea.getText().substring(0, maxLength));
        }
        updateCounter();
        return this;
    }

    /**
     * Exibe ou oculta o contador de caracteres.
     */
    public TextAreaField setShowCounter(boolean showCounter) {
        this.showCounter = showCounter;
        counterLabel.setVisible(showCounter);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Habilita o crescimento automático conforme o número de linhas digitadas.
     */
    public TextAreaField setAutoGrow(boolean autoGrow) {
        this.autoGrow = autoGrow;
        applyAutoGrow();
        return this;
    }

    /**
     * Define a faixa de linhas usada pelo crescimento automático.
     */
    public TextAreaField setRowRange(int minRows, int maxRows) {
        if (minRows <= 0 || maxRows < minRows) {
            throw new IllegalArgumentException("invalid row range");
        }
        this.minRows = minRows;
        this.maxRows = maxRows;
        applyAutoGrow();
        return this;
    }

    /**
     * Ativa o realce de erro na borda do campo.
     */
    public TextAreaField setErrorState(boolean errorState) {
        this.errorState = errorState;
        repaint();
        return this;
    }

    /**
     * Indica se o campo está em estado de erro.
     */
    public boolean isErrorState() {
        return errorState;
    }

    /**
     * Define o raio de canto do campo.
     */
    public TextAreaField setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do campo.
     */
    public TextAreaField setColors(Color backgroundColor, Color borderColor, Color focusBorderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.focusBorderColor = focusBorderColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do placeholder.
     */
    public TextAreaField setPlaceholderColor(Color placeholderColor) {
        this.placeholderColor = placeholderColor;
        repaint();
        return this;
    }

    /**
     * Área de texto interna, exposta para configurações avançadas.
     */
    public JTextArea getTextArea() {
        return textArea;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textArea.setEnabled(enabled);
        repaint();
    }

    private void configureTextArea() {
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setFont(UiTokens.font());
        textArea.setForeground(UiTokens.foreground());
        textArea.setCaretColor(UiTokens.foreground());
        textArea.setRows(minRows);
        textArea.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.space(2), UiTokens.space(2), UiTokens.space(2), UiTokens.space(2)));
        ((PlainDocument) textArea.getDocument()).setDocumentFilter(new LengthFilter());
    }

    private void configureScrollPane() {
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void configureCounter() {
        counterLabel.setFont(UiTokens.fontSmall());
        counterLabel.setForeground(UiTokens.muted());
        counterLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private void installListeners() {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleInput();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleInput();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleInput();
            }
        });

        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
                dispatchEvent(EventType.FOCUS, TextAreaField.this, textArea.getText(), Map.of());
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
                dispatchEvent(EventType.BLUR, TextAreaField.this, textArea.getText(), Map.of());
            }
        });
    }

    private void handleInput() {
        updateCounter();
        applyAutoGrow();
        repaint();
        dispatchEvent(EventType.INPUT, this, textArea.getText(), Map.of("length", textArea.getText().length()));
    }

    private void updateCounter() {
        if (!showCounter) {
            return;
        }
        int length = textArea.getText().length();
        counterLabel.setText(maxLength >= 0 ? length + "/" + maxLength : String.valueOf(length));
        boolean nearLimit = maxLength >= 0 && length >= maxLength;
        counterLabel.setForeground(nearLimit ? UiTokens.danger() : UiTokens.muted());
    }

    private void applyAutoGrow() {
        if (!autoGrow) {
            return;
        }
        int lines = Math.max(minRows, Math.min(maxRows, textArea.getLineCount()));
        if (lines == textArea.getRows()) {
            return;
        }
        textArea.setRows(lines);
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = getFieldBounds();
            paintBackground(g2, bounds);
            if (textArea.getText().isEmpty() && !focused) {
                paintPlaceholder(g2, bounds);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pela área de digitação.
     */
    protected Rectangle getFieldBounds() {
        Rectangle bounds = scrollPane.getBounds();
        return bounds.isEmpty() ? new Rectangle(0, 0, getWidth(), getHeight()) : bounds;
    }

    /**
     * Pinta o fundo e a borda do campo.
     */
    protected void paintBackground(Graphics2D g2, Rectangle bounds) {
        Color fill = backgroundColor != null ? backgroundColor : UiTokens.surface();
        if (!isEnabled()) {
            fill = UiTokens.disabled(fill);
        }
        PaintUtils.fillRoundRect(g2, bounds, arc, fill);

        Color stroke = resolveBorderColor();
        PaintUtils.drawRoundRect(g2, bounds, arc, stroke, focused || errorState ? 2f : UiTokens.stroke());
    }

    /**
     * Pinta o texto de placeholder.
     */
    protected void paintPlaceholder(Graphics2D g2, Rectangle bounds) {
        if (placeholder.isEmpty()) {
            return;
        }
        Color color = placeholderColor != null ? placeholderColor : UiTokens.muted();
        FontMetrics metrics = getFontMetrics(UiTokens.font());
        Rectangle textBounds = new Rectangle(
                bounds.x + UiTokens.space(2),
                bounds.y + UiTokens.space(2),
                Math.max(0, bounds.width - UiTokens.space(4)),
                metrics.getHeight());
        PaintUtils.drawPlaceholder(g2, placeholder, textBounds, color, UiTokens.font());
    }

    private Color resolveBorderColor() {
        if (errorState) {
            return UiTokens.danger();
        }
        if (!isEnabled()) {
            return UiTokens.disabled(UiTokens.border());
        }
        if (focused) {
            return focusBorderColor != null ? focusBorderColor : UiTokens.primary();
        }
        return borderColor != null ? borderColor : UiTokens.border();
    }

    /**
     * Bloqueia digitação acima do limite configurado.
     */
    private final class LengthFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                throws BadLocationException {
            super.insertString(bypass, offset, truncate(bypass, 0, text), attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            super.replace(bypass, offset, length, truncate(bypass, length, text), attributes);
        }

        private String truncate(FilterBypass bypass, int removed, String text) {
            if (maxLength < 0 || text == null) {
                return text;
            }
            int room = maxLength - (bypass.getDocument().getLength() - removed);
            if (text.length() <= room) {
                return text;
            }
            notifyLimit();
            return room <= 0 ? "" : text.substring(0, room);
        }

        private void notifyLimit() {
            dispatchEvent(LIMIT_REACHED, TextAreaField.this, textArea.getText(), Map.of("maxLength", maxLength));
        }
    }
}
