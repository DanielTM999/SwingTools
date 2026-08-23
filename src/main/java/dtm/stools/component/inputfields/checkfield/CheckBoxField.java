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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Caixa de seleção desenhada manualmente, com marcação animada, estado indeterminado e rótulo opcional.
 */
public class CheckBoxField extends PanelEventListener {

    public static final String CHECKED = "checked";
    public static final String UNCHECKED = "unchecked";

    private boolean selected;
    private boolean indeterminate;
    private boolean animated = true;
    private boolean focusPainted = true;
    private boolean hover;

    private int animationDuration = 140;
    private float animationProgress;
    private float animationStartProgress;
    private float animationTargetProgress;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;

    private int boxSize = 18;
    private int boxArc = 6;
    private int textGap = 8;
    private float focusStrokeWidth = 2f;
    private int focusGap = 2;

    private String text = "";

    private Color selectedColor;
    private Color unselectedColor;
    private Color borderColor;
    private Color checkColor;
    private Color textColor;
    private Color focusColor;

    private final Timer animationTimer;

    public CheckBoxField() {
        this("", false);
    }

    public CheckBoxField(String text) {
        this(text, false);
    }

    public CheckBoxField(String text, boolean selected) {
        super(null, false);
        this.text = text != null ? text : "";
        this.selected = selected;
        this.animationProgress = selected ? 1f : 0f;
        this.animationTargetProgress = this.animationProgress;

        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UiTokens.font());

        animationTimer = new Timer(16, e -> updateAnimation());
        installListeners();
        updatePreferredSize();
    }

    /**
     * Alterna o estado de marcação.
     */
    public void toggle() {
        setSelected(!selected);
    }

    /**
     * Indica se a caixa está marcada.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Define o estado de marcação disparando eventos.
     */
    public CheckBoxField setSelected(boolean selected) {
        return setSelected(selected, true);
    }

    /**
     * Define o estado de marcação, opcionalmente sem disparar eventos.
     */
    public CheckBoxField setSelected(boolean selected, boolean fireEvent) {
        if (this.selected == selected && !indeterminate) {
            return this;
        }

        boolean oldValue = this.selected;
        this.selected = selected;
        this.indeterminate = false;
        animateToSelection();
        firePropertyChange("selected", oldValue, selected);

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", selected);
            dispatchEvent(EventType.CHANGE, this, selected, props);
            dispatchEvent(selected ? CHECKED : UNCHECKED, this, selected, props);
        }
        return this;
    }

    /**
     * Indica se a caixa está no estado indeterminado.
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    /**
     * Ativa ou desativa o estado indeterminado.
     */
    public CheckBoxField setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (indeterminate) {
            this.selected = false;
        }
        animateToSelection();
        return this;
    }

    /**
     * Rótulo exibido ao lado da caixa.
     */
    public String getText() {
        return text;
    }

    /**
     * Define o rótulo exibido ao lado da caixa.
     */
    public CheckBoxField setText(String text) {
        this.text = text != null ? text : "";
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita a animação da marcação.
     */
    public CheckBoxField setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            animationProgress = targetProgress();
            animationTargetProgress = animationProgress;
            repaint();
        }
        return this;
    }

    /**
     * Define a duração da animação em milissegundos.
     */
    public CheckBoxField setAnimationDuration(int animationDuration) {
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration cannot be negative");
        }
        this.animationDuration = animationDuration;
        return this;
    }

    /**
     * Define o tamanho da caixa em pixels.
     */
    public CheckBoxField setBoxSize(int boxSize) {
        if (boxSize <= 0) {
            throw new IllegalArgumentException("boxSize must be greater than zero");
        }
        this.boxSize = boxSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o raio de canto da caixa.
     */
    public CheckBoxField setBoxArc(int boxArc) {
        if (boxArc < 0) {
            throw new IllegalArgumentException("boxArc cannot be negative");
        }
        this.boxArc = boxArc;
        repaint();
        return this;
    }

    /**
     * Define o espaço entre a caixa e o rótulo.
     */
    public CheckBoxField setTextGap(int textGap) {
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
    public CheckBoxField setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do componente.
     */
    public CheckBoxField setColors(Color selectedColor, Color unselectedColor, Color checkColor) {
        this.selectedColor = selectedColor;
        this.unselectedColor = unselectedColor;
        this.checkColor = checkColor;
        repaint();
        return this;
    }

    /**
     * Define a cor da borda da caixa.
     */
    public CheckBoxField setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do rótulo.
     */
    public CheckBoxField setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do anel de foco.
     */
    public CheckBoxField setFocusColor(Color focusColor) {
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

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                requestFocusInWindow();
                toggle();
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
                    toggle();
                    e.consume();
                }
            }
        });

        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                repaint();
            }
        });
    }

    private void updatePreferredSize() {
        if (text == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.font());
        int width = boxSize;
        int height = Math.max(boxSize, metrics.getHeight());
        if (!text.isEmpty()) {
            width += textGap + metrics.stringWidth(text);
        }
        Dimension size = new Dimension(width + focusGap * 2, height + focusGap * 2);
        setPreferredSize(size);
        setMinimumSize(size);
        revalidate();
    }

    private float targetProgress() {
        return selected || indeterminate ? 1f : 0f;
    }

    private void animateToSelection() {
        float target = targetProgress();
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
            Rectangle box = getBoxBounds();
            paintBox(g2, box);
            paintMark(g2, box);
            if (!text.isEmpty()) {
                paintText(g2, box);
            }
            if (focusPainted && isFocusOwner()) {
                paintFocus(g2, box);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pela caixa de marcação.
     */
    protected Rectangle getBoxBounds() {
        int y = (getHeight() - boxSize) / 2;
        return new Rectangle(focusGap, y, boxSize, boxSize);
    }

    /**
     * Pinta o fundo e a borda da caixa.
     */
    protected void paintBox(Graphics2D g2, Rectangle box) {
        Color fill = resolveFillColor();
        PaintUtils.fillRoundRect(g2, box, boxArc, fill);
        if (animationProgress < 1f) {
            Color stroke = resolveBorderColor();
            PaintUtils.drawRoundRect(g2, box, boxArc, stroke, UiTokens.stroke());
        }
    }

    /**
     * Pinta o traço de marcação ou o traço do estado indeterminado.
     */
    protected void paintMark(Graphics2D g2, Rectangle box) {
        if (animationProgress <= 0f) {
            return;
        }

        Color mark = checkColor != null ? checkColor : UiTokens.onColor(resolveSelectedColor());
        if (!isEnabled()) {
            mark = UiTokens.disabled(mark);
        }
        g2.setColor(mark);
        g2.setStroke(new BasicStroke(Math.max(1.6f, boxSize / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (indeterminate) {
            int inset = Math.round(box.width * 0.26f);
            int y = box.y + box.height / 2;
            int half = Math.round((box.width - inset * 2) * animationProgress / 2f);
            g2.drawLine(box.x + box.width / 2 - half, y, box.x + box.width / 2 + half, y);
            return;
        }

        float startX = box.x + box.width * 0.26f;
        float startY = box.y + box.height * 0.52f;
        float midX = box.x + box.width * 0.44f;
        float midY = box.y + box.height * 0.70f;
        float endX = box.x + box.width * 0.76f;
        float endY = box.y + box.height * 0.32f;

        float firstLeg = Math.min(1f, animationProgress / 0.4f);
        g2.drawLine(Math.round(startX), Math.round(startY),
                Math.round(startX + (midX - startX) * firstLeg),
                Math.round(startY + (midY - startY) * firstLeg));

        if (animationProgress > 0.4f) {
            float secondLeg = (animationProgress - 0.4f) / 0.6f;
            g2.drawLine(Math.round(midX), Math.round(midY),
                    Math.round(midX + (endX - midX) * secondLeg),
                    Math.round(midY + (endY - midY) * secondLeg));
        }
    }

    /**
     * Pinta o rótulo ao lado da caixa.
     */
    protected void paintText(Graphics2D g2, Rectangle box) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.font());
        Color color = textColor != null ? textColor : UiTokens.foreground();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        int x = box.x + box.width + textGap;
        Rectangle textBounds = new Rectangle(x, 0, Math.max(0, getWidth() - x - focusGap), getHeight());
        PaintUtils.drawLeftText(g2, text, textBounds, color);
    }

    /**
     * Pinta o anel de foco ao redor da caixa.
     */
    protected void paintFocus(Graphics2D g2, Rectangle box) {
        Color color = focusColor != null ? focusColor : UiTokens.overlay(UiTokens.accent(), 0.55f);
        PaintUtils.focusRing(g2, box, boxArc, color, focusStrokeWidth, focusGap);
    }

    private Color resolveFillColor() {
        Color selectedFill = resolveSelectedColor();
        Color emptyFill = unselectedColor != null ? unselectedColor : UiTokens.surface();
        Color base = PaintUtils.blend(emptyFill, selectedFill, animationProgress);
        if (!isEnabled()) {
            return UiTokens.disabled(base);
        }
        return hover && animationProgress < 1f ? UiTokens.hover(base) : base;
    }

    private Color resolveSelectedColor() {
        return selectedColor != null ? selectedColor : UiTokens.primary();
    }

    private Color resolveBorderColor() {
        Color base = borderColor != null ? borderColor : UiTokens.border();
        if (!isEnabled()) {
            return UiTokens.disabled(base);
        }
        return hover ? UiTokens.primary() : base;
    }
}
