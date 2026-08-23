package dtm.stools.component.inputfields.pinfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Campo de código de verificação com uma caixa por dígito, avanço automático e colagem distribuída.
 */
public class PinField extends PanelEventListener {

    public static final String COMPLETED = "pinCompleted";

    private char[] digits;
    private int caretIndex;

    private boolean masked;
    private boolean numericOnly = true;
    private boolean focusPainted = true;
    private boolean caretVisible = true;

    private int boxWidth = 44;
    private int boxHeight = 52;
    private int boxGap = 8;
    private int boxArc = UiTokens.radius(UiTokens.Radius.MD);
    private float focusStrokeWidth = 2f;

    private Color boxColor;
    private Color borderColor;
    private Color activeBorderColor;
    private Color textColor;

    private final Timer caretTimer;

    public PinField() {
        this(6);
    }

    public PinField(int length) {
        super(null, false);
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than zero");
        }
        this.digits = new char[length];

        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFont(UiTokens.font().deriveFont(Font.BOLD, UiTokens.font().getSize2D() + 4f));

        caretTimer = new Timer(500, e -> {
            caretVisible = !caretVisible;
            repaint();
        });

        installListeners();
        updatePreferredSize();
    }

    /**
     * Código digitado, com espaços nas posições ainda vazias removidos.
     */
    public String getValue() {
        StringBuilder builder = new StringBuilder(digits.length);
        for (char digit : digits) {
            if (digit != 0) {
                builder.append(digit);
            }
        }
        return builder.toString();
    }

    /**
     * Define o código exibido disparando eventos.
     */
    public PinField setValue(String value) {
        return setValue(value, true);
    }

    /**
     * Define o código exibido, opcionalmente sem disparar eventos.
     */
    public PinField setValue(String value, boolean fireEvent) {
        String previous = getValue();
        java.util.Arrays.fill(digits, (char) 0);
        caretIndex = 0;
        if (value != null) {
            for (int i = 0; i < value.length() && caretIndex < digits.length; i++) {
                char candidate = value.charAt(i);
                if (accepts(candidate)) {
                    digits[caretIndex++] = candidate;
                }
            }
        }
        repaint();
        if (fireEvent) {
            fireValueChanged(previous);
        }
        return this;
    }

    /**
     * Quantidade de caixas do campo.
     */
    public int getLength() {
        return digits.length;
    }

    /**
     * Define a quantidade de caixas do campo, descartando o valor corrente.
     */
    public PinField setLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be greater than zero");
        }
        this.digits = new char[length];
        this.caretIndex = 0;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Indica se o código está completo.
     */
    public boolean isComplete() {
        return getValue().length() == digits.length;
    }

    /**
     * Limpa o código digitado.
     */
    public PinField clear() {
        String previous = getValue();
        java.util.Arrays.fill(digits, (char) 0);
        caretIndex = 0;
        repaint();
        dispatchEvent(EventType.CLEAR, this, "", Map.of("oldValue", previous));
        fireValueChanged(previous);
        return this;
    }

    /**
     * Exibe pontos no lugar dos caracteres digitados.
     */
    public PinField setMasked(boolean masked) {
        this.masked = masked;
        repaint();
        return this;
    }

    /**
     * Indica se o campo está mascarado.
     */
    public boolean isMasked() {
        return masked;
    }

    /**
     * Restringe a digitação a caracteres numéricos.
     */
    public PinField setNumericOnly(boolean numericOnly) {
        this.numericOnly = numericOnly;
        return this;
    }

    /**
     * Define as dimensões de cada caixa.
     */
    public PinField setBoxSize(int boxWidth, int boxHeight) {
        if (boxWidth <= 0 || boxHeight <= 0) {
            throw new IllegalArgumentException("box dimensions must be greater than zero");
        }
        this.boxWidth = boxWidth;
        this.boxHeight = boxHeight;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o espaço entre as caixas.
     */
    public PinField setBoxGap(int boxGap) {
        if (boxGap < 0) {
            throw new IllegalArgumentException("boxGap cannot be negative");
        }
        this.boxGap = boxGap;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o raio de canto das caixas.
     */
    public PinField setBoxArc(int boxArc) {
        if (boxArc < 0) {
            throw new IllegalArgumentException("boxArc cannot be negative");
        }
        this.boxArc = boxArc;
        repaint();
        return this;
    }

    /**
     * Habilita o realce da caixa ativa.
     */
    public PinField setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do campo.
     */
    public PinField setColors(Color boxColor, Color borderColor, Color activeBorderColor, Color textColor) {
        this.boxColor = boxColor;
        this.borderColor = borderColor;
        this.activeBorderColor = activeBorderColor;
        this.textColor = textColor;
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public void removeNotify() {
        caretTimer.stop();
        super.removeNotify();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                requestFocusInWindow();
                int index = indexAt(e.getX());
                if (index >= 0) {
                    caretIndex = Math.min(index, getValue().length());
                    repaint();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) {
                    return;
                }
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    pasteFromClipboard();
                    e.consume();
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE -> {
                        deleteBackward();
                        e.consume();
                    }
                    case KeyEvent.VK_DELETE -> {
                        deleteForward();
                        e.consume();
                    }
                    case KeyEvent.VK_LEFT -> {
                        caretIndex = Math.max(0, caretIndex - 1);
                        repaint();
                        e.consume();
                    }
                    case KeyEvent.VK_RIGHT -> {
                        caretIndex = Math.min(digits.length - 1, caretIndex + 1);
                        repaint();
                        e.consume();
                    }
                    default -> {
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!isEnabled() || e.isControlDown() || e.isAltDown()) {
                    return;
                }
                char typed = e.getKeyChar();
                if (accepts(typed)) {
                    insert(typed);
                    e.consume();
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                caretVisible = true;
                caretTimer.start();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                caretTimer.stop();
                caretVisible = false;
                repaint();
            }
        });
    }

    private void updatePreferredSize() {
        int width = digits.length * boxWidth + (digits.length - 1) * boxGap;
        Dimension size = new Dimension(width, boxHeight);
        setPreferredSize(size);
        setMinimumSize(size);
        revalidate();
    }

    private boolean accepts(char candidate) {
        if (numericOnly) {
            return Character.isDigit(candidate);
        }
        return !Character.isISOControl(candidate) && !Character.isWhitespace(candidate);
    }

    private int indexAt(int x) {
        int index = x / (boxWidth + boxGap);
        return index >= 0 && index < digits.length ? index : -1;
    }

    private void insert(char candidate) {
        if (caretIndex >= digits.length) {
            return;
        }
        String previous = getValue();
        digits[caretIndex] = candidate;
        caretIndex = Math.min(digits.length - 1, caretIndex + 1);
        repaint();
        fireValueChanged(previous);
    }

    private void deleteBackward() {
        String previous = getValue();
        if (digits[caretIndex] != 0) {
            digits[caretIndex] = 0;
        } else if (caretIndex > 0) {
            caretIndex--;
            digits[caretIndex] = 0;
        }
        repaint();
        fireValueChanged(previous);
    }

    private void deleteForward() {
        String previous = getValue();
        digits[caretIndex] = 0;
        repaint();
        fireValueChanged(previous);
    }

    private void pasteFromClipboard() {
        try {
            Object content = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (content instanceof String text) {
                setValue(text);
            }
        } catch (Exception ignored) {
            repaint();
        }
    }

    private void fireValueChanged(String previous) {
        String current = getValue();
        if (previous.equals(current)) {
            return;
        }
        Map<String, Object> props = Map.of("oldValue", previous, "newValue", current);
        dispatchEvent(EventType.INPUT, this, current, props);
        dispatchEvent(EventType.CHANGE, this, current, props);
        if (isComplete()) {
            dispatchEvent(COMPLETED, this, current, props);
            dispatchEvent(EventType.SUBMIT, this, current, props);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            g2.setFont(getFont() != null ? getFont() : UiTokens.font());
            for (int i = 0; i < digits.length; i++) {
                Rectangle bounds = getBoxBounds(i);
                paintBox(g2, bounds, i);
                paintDigit(g2, bounds, i);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pela caixa do índice informado.
     */
    protected Rectangle getBoxBounds(int index) {
        return new Rectangle(index * (boxWidth + boxGap), 0, boxWidth, boxHeight);
    }

    /**
     * Pinta o fundo e a borda de uma caixa.
     */
    protected void paintBox(Graphics2D g2, Rectangle bounds, int index) {
        Color fill = boxColor != null ? boxColor : UiTokens.surface();
        if (!isEnabled()) {
            fill = UiTokens.disabled(fill);
        }
        PaintUtils.fillRoundRect(g2, bounds, boxArc, fill);

        boolean active = focusPainted && isFocusOwner() && index == caretIndex;
        Color stroke = active
                ? (activeBorderColor != null ? activeBorderColor : UiTokens.primary())
                : (borderColor != null ? borderColor : UiTokens.border());
        if (!isEnabled()) {
            stroke = UiTokens.disabled(stroke);
        }
        PaintUtils.drawRoundRect(g2, bounds, boxArc, stroke, active ? focusStrokeWidth : UiTokens.stroke());
    }

    /**
     * Pinta o caractere ou o cursor de uma caixa.
     */
    protected void paintDigit(Graphics2D g2, Rectangle bounds, int index) {
        Color color = textColor != null ? textColor : UiTokens.foreground();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }

        char digit = digits[index];
        if (digit != 0) {
            PaintUtils.drawCenteredText(g2, masked ? "•" : String.valueOf(digit), bounds, color);
            return;
        }

        if (isFocusOwner() && index == caretIndex && caretVisible) {
            int caretHeight = bounds.height / 3;
            g2.setColor(UiTokens.primary());
            g2.fillRect(bounds.x + bounds.width / 2 - 1,
                    bounds.y + (bounds.height - caretHeight) / 2,
                    2,
                    caretHeight);
        }
    }
}
