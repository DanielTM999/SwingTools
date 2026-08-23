package dtm.stools.component.inputfields.checkfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Botão de opção desenhado manualmente, com preenchimento animado e valor associado.
 */
public class RadioField<T> extends PanelEventListener {

    public static final String SELECTED = "radioSelected";

    private boolean selected;
    private boolean animated = true;
    private boolean focusPainted = true;
    private boolean hover;

    private T value;
    private String text = "";

    private int circleSize = 18;
    private int textGap = 8;
    private float focusStrokeWidth = 2f;
    private int focusGap = 2;

    private int animationDuration = 140;
    private float animationProgress;
    private float animationStartProgress;
    private float animationTargetProgress;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;

    private Color selectedColor;
    private Color borderColor;
    private Color dotColor;
    private Color textColor;
    private Color focusColor;

    private final Timer animationTimer;

    private RadioGroupField<T> group;

    public RadioField() {
        this("", null);
    }

    public RadioField(String text) {
        this(text, null);
    }

    public RadioField(String text, T value) {
        super(null, false);
        this.text = text != null ? text : "";
        this.value = value;

        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UiTokens.font());

        animationTimer = new Timer(16, e -> updateAnimation());
        installListeners();
        updatePreferredSize();
    }

    /**
     * Indica se a opção está marcada.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Marca ou desmarca a opção disparando eventos.
     */
    public RadioField<T> setSelected(boolean selected) {
        return setSelected(selected, true);
    }

    /**
     * Marca ou desmarca a opção, opcionalmente sem disparar eventos.
     */
    public RadioField<T> setSelected(boolean selected, boolean fireEvent) {
        if (this.selected == selected) {
            return this;
        }

        boolean oldValue = this.selected;
        this.selected = selected;
        animateToSelection();
        firePropertyChange("selected", oldValue, selected);

        if (selected && group != null) {
            group.notifySelection(this, fireEvent);
        }

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", selected);
            dispatchEvent(EventType.CHANGE, this, selected, props);
            if (selected) {
                dispatchEvent(SELECTED, this, value, props);
                dispatchEvent(EventType.SELECT, this, value, props);
            }
        }
        return this;
    }

    /**
     * Valor associado a esta opção.
     */
    public T getValue() {
        return value;
    }

    /**
     * Define o valor associado a esta opção.
     */
    public RadioField<T> setValue(T value) {
        this.value = value;
        return this;
    }

    /**
     * Rótulo exibido ao lado do círculo.
     */
    public String getText() {
        return text;
    }

    /**
     * Define o rótulo exibido ao lado do círculo.
     */
    public RadioField<T> setText(String text) {
        this.text = text != null ? text : "";
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita a animação do preenchimento.
     */
    public RadioField<T> setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            animationProgress = selected ? 1f : 0f;
            animationTargetProgress = animationProgress;
            repaint();
        }
        return this;
    }

    /**
     * Define a duração da animação em milissegundos.
     */
    public RadioField<T> setAnimationDuration(int animationDuration) {
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration cannot be negative");
        }
        this.animationDuration = animationDuration;
        return this;
    }

    /**
     * Define o diâmetro do círculo em pixels.
     */
    public RadioField<T> setCircleSize(int circleSize) {
        if (circleSize <= 0) {
            throw new IllegalArgumentException("circleSize must be greater than zero");
        }
        this.circleSize = circleSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o espaço entre o círculo e o rótulo.
     */
    public RadioField<T> setTextGap(int textGap) {
        if (textGap < 0) {
            throw new IllegalArgumentException("textGap cannot be negative");
        }
        this.textGap = textGap;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita a pintura do anel de foco.
     */
    public RadioField<T> setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do componente.
     */
    public RadioField<T> setColors(Color selectedColor, Color borderColor, Color dotColor) {
        this.selectedColor = selectedColor;
        this.borderColor = borderColor;
        this.dotColor = dotColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do rótulo.
     */
    public RadioField<T> setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do anel de foco.
     */
    public RadioField<T> setFocusColor(Color focusColor) {
        this.focusColor = focusColor;
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        updatePreferredSize();
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    void attachGroup(RadioGroupField<T> group) {
        this.group = group;
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                requestFocusInWindow();
                setSelected(true);
            }

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

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    setSelected(true);
                    e.consume();
                }
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

    private void updatePreferredSize() {
        if (text == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.font());
        int width = circleSize;
        int height = Math.max(circleSize, metrics.getHeight());
        if (!text.isEmpty()) {
            width += textGap + metrics.stringWidth(text);
        }
        Dimension size = new Dimension(width + focusGap * 2, height + focusGap * 2);
        setPreferredSize(size);
        setMinimumSize(size);
        revalidate();
    }

    private void animateToSelection() {
        float target = selected ? 1f : 0f;
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationTimer.stop();
            animationProgress = target;
            animationTargetProgress = target;
            repaint();
            return;
        }

        animationStartProgress = animationProgress;
        animationTargetProgress = target;
        animationStartedAtNanos = System.nanoTime();
        animationRunDurationNanos = (long) (animationDuration * 1_000_000L
                * Math.abs(target - animationStartProgress));
        if (animationRunDurationNanos <= 0) {
            animationProgress = target;
            repaint();
            return;
        }
        animationTimer.start();
    }

    private void updateAnimation() {
        long elapsed = System.nanoTime() - animationStartedAtNanos;
        float ratio = Math.min(1f, (float) elapsed / animationRunDurationNanos);
        animationProgress = animationStartProgress
                + (animationTargetProgress - animationStartProgress) * PaintUtils.easeOut(ratio);
        if (ratio >= 1f) {
            animationProgress = animationTargetProgress;
            animationTimer.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle circle = getCircleBounds();
            paintCircle(g2, circle);
            paintDot(g2, circle);
            if (!text.isEmpty()) {
                paintText(g2, circle);
            }
            if (focusPainted && isFocusOwner()) {
                paintFocus(g2, circle);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo que envolve o círculo da opção.
     */
    protected Rectangle getCircleBounds() {
        int y = (getHeight() - circleSize) / 2;
        return new Rectangle(focusGap, y, circleSize, circleSize);
    }

    /**
     * Pinta o círculo externo e sua borda.
     */
    protected void paintCircle(Graphics2D g2, Rectangle circle) {
        Color fill = isEnabled() ? UiTokens.surface() : UiTokens.disabled(UiTokens.surface());
        if (hover && isEnabled() && !selected) {
            fill = UiTokens.hover(fill);
        }
        g2.setColor(fill);
        g2.fillOval(circle.x, circle.y, circle.width, circle.height);

        Color stroke = resolveBorderColor();
        float width = UiTokens.stroke() + animationProgress;
        g2.setColor(stroke);
        g2.setStroke(new BasicStroke(width));
        float inset = width / 2f;
        g2.drawOval(Math.round(circle.x + inset), Math.round(circle.y + inset),
                Math.round(circle.width - width), Math.round(circle.height - width));
    }

    /**
     * Pinta o ponto central que cresce conforme a animação.
     */
    protected void paintDot(Graphics2D g2, Rectangle circle) {
        if (animationProgress <= 0f) {
            return;
        }
        Color color = dotColor != null ? dotColor : resolveSelectedColor();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        int maxSize = Math.round(circle.width * 0.45f);
        int size = Math.round(maxSize * animationProgress);
        g2.setColor(color);
        g2.fillOval(circle.x + (circle.width - size) / 2, circle.y + (circle.height - size) / 2, size, size);
    }

    /**
     * Pinta o rótulo ao lado do círculo.
     */
    protected void paintText(Graphics2D g2, Rectangle circle) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.font());
        Color color = textColor != null ? textColor : UiTokens.foreground();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        int x = circle.x + circle.width + textGap;
        Rectangle textBounds = new Rectangle(x, 0, Math.max(0, getWidth() - x - focusGap), getHeight());
        PaintUtils.drawLeftText(g2, text, textBounds, color);
    }

    /**
     * Pinta o anel de foco ao redor do círculo.
     */
    protected void paintFocus(Graphics2D g2, Rectangle circle) {
        Color color = focusColor != null ? focusColor : UiTokens.overlay(UiTokens.accent(), 0.55f);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(focusStrokeWidth));
        float inset = focusStrokeWidth / 2f;
        g2.drawOval(Math.round(circle.x - focusGap + inset), Math.round(circle.y - focusGap + inset),
                Math.round(circle.width + focusGap * 2 - focusStrokeWidth),
                Math.round(circle.height + focusGap * 2 - focusStrokeWidth));
    }

    private Color resolveSelectedColor() {
        return selectedColor != null ? selectedColor : UiTokens.primary();
    }

    private Color resolveBorderColor() {
        Color base = selected
                ? resolveSelectedColor()
                : (borderColor != null ? borderColor : UiTokens.border());
        if (!isEnabled()) {
            return UiTokens.disabled(base);
        }
        return hover && !selected ? UiTokens.primary() : base;
    }
}
