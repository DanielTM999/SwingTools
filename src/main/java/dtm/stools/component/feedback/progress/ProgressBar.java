package dtm.stools.component.feedback.progress;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Barra de progresso linear arredondada, com modos determinado e indeterminado.
 */
public class ProgressBar extends PanelEventListener {

    public static final String PROGRESS = "progress";
    public static final String FINISHED = "progressFinished";

    /**
     * Cor semântica da barra.
     */
    public enum Tone {
        PRIMARY, SUCCESS, WARNING, DANGER, INFO
    }

    private double value;
    private double maximum = 100d;

    private boolean indeterminate;
    private boolean animated = true;
    private boolean showLabel;

    private Tone tone = Tone.PRIMARY;
    private int barHeight = 8;
    private int animationDuration = 320;

    private double displayedValue;
    private double animationStart;
    private double animationTarget;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;

    private float indeterminatePosition;

    private Color trackColor;
    private Color fillColor;
    private Color labelColor;

    private DoubleFunction<String> labelFormatter = current -> Math.round(current) + "%";

    private final Timer animationTimer;
    private final Timer indeterminateTimer;

    public ProgressBar() {
        this(0d);
    }

    public ProgressBar(double value) {
        super(null, false);
        this.value = clamp(value);
        this.displayedValue = this.value;

        setOpaque(false);
        setFont(UiTokens.fontSmall());

        animationTimer = new Timer(16, e -> updateAnimation());
        indeterminateTimer = new Timer(16, e -> updateIndeterminate());
        updatePreferredSize();
    }

    /**
     * Valor corrente do progresso.
     */
    public double getValue() {
        return value;
    }

    /**
     * Define o valor corrente disparando eventos.
     */
    public ProgressBar setValue(double value) {
        return setValue(value, true);
    }

    /**
     * Define o valor corrente, opcionalmente sem disparar eventos.
     */
    public ProgressBar setValue(double value, boolean fireEvent) {
        double clamped = clamp(value);
        if (Double.compare(clamped, this.value) == 0) {
            return this;
        }

        double oldValue = this.value;
        this.value = clamped;
        animateToValue();
        firePropertyChange("value", oldValue, clamped);

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", clamped, "maximum", maximum);
            dispatchEvent(PROGRESS, this, clamped, props);
            dispatchEvent(EventType.CHANGE, this, clamped, props);
            if (Double.compare(clamped, maximum) == 0) {
                dispatchEvent(FINISHED, this, clamped, props);
            }
        }
        return this;
    }

    /**
     * Valor máximo do progresso.
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * Define o valor máximo do progresso.
     */
    public ProgressBar setMaximum(double maximum) {
        if (maximum <= 0) {
            throw new IllegalArgumentException("maximum must be greater than zero");
        }
        this.maximum = maximum;
        this.value = clamp(value);
        repaint();
        return this;
    }

    /**
     * Alterna entre progresso determinado e indeterminado.
     */
    public ProgressBar setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (indeterminate) {
            animationTimer.stop();
            if (isShowing()) {
                indeterminateTimer.start();
            }
        } else {
            indeterminateTimer.stop();
            indeterminatePosition = 0f;
        }
        repaint();
        return this;
    }

    /**
     * Indica se a barra está em modo indeterminado.
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    /**
     * Habilita a animação de transição do valor.
     */
    public ProgressBar setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            displayedValue = value;
            repaint();
        }
        return this;
    }

    /**
     * Define a duração da animação em milissegundos.
     */
    public ProgressBar setAnimationDuration(int animationDuration) {
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration cannot be negative");
        }
        this.animationDuration = animationDuration;
        return this;
    }

    /**
     * Define a cor semântica da barra.
     */
    public ProgressBar setTone(Tone tone) {
        if (tone == null) {
            throw new IllegalArgumentException("tone cannot be null");
        }
        this.tone = tone;
        repaint();
        return this;
    }

    /**
     * Define a espessura da barra.
     */
    public ProgressBar setBarHeight(int barHeight) {
        if (barHeight <= 0) {
            throw new IllegalArgumentException("barHeight must be greater than zero");
        }
        this.barHeight = barHeight;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Exibe o rótulo com o percentual à direita da barra.
     */
    public ProgressBar setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define como o valor é convertido em texto no rótulo.
     */
    public ProgressBar setLabelFormatter(DoubleFunction<String> labelFormatter) {
        if (labelFormatter == null) {
            throw new IllegalArgumentException("labelFormatter cannot be null");
        }
        this.labelFormatter = labelFormatter;
        repaint();
        return this;
    }

    /**
     * Define as cores do trilho, do preenchimento e do rótulo.
     */
    public ProgressBar setColors(Color trackColor, Color fillColor, Color labelColor) {
        this.trackColor = trackColor;
        this.fillColor = fillColor;
        this.labelColor = labelColor;
        repaint();
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (indeterminate) {
            indeterminateTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        indeterminateTimer.stop();
        super.removeNotify();
    }

    private double clamp(double candidate) {
        return Math.max(0d, Math.min(maximum, candidate));
    }

    private void updatePreferredSize() {
        int height = Math.max(barHeight, showLabel
                ? getFontMetrics(getFont() != null ? getFont() : UiTokens.fontSmall()).getHeight()
                : barHeight);
        setPreferredSize(new Dimension(UiTokens.scale(220), height));
        setMinimumSize(new Dimension(UiTokens.scale(60), height));
        revalidate();
    }

    private void animateToValue() {
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationTimer.stop();
            displayedValue = value;
            repaint();
            return;
        }

        animationStart = displayedValue;
        animationTarget = value;
        animationStartedAtNanos = System.nanoTime();
        animationRunDurationNanos = (long) (animationDuration * 1_000_000L
                * Math.min(1d, Math.abs(animationTarget - animationStart) / maximum));
        if (animationRunDurationNanos <= 0) {
            displayedValue = value;
            repaint();
            return;
        }
        animationTimer.start();
    }

    private void updateAnimation() {
        long elapsed = System.nanoTime() - animationStartedAtNanos;
        float ratio = Math.min(1f, (float) elapsed / animationRunDurationNanos);
        displayedValue = animationStart + (animationTarget - animationStart) * PaintUtils.easeOut(ratio);
        if (ratio >= 1f) {
            displayedValue = animationTarget;
            animationTimer.stop();
        }
        repaint();
    }

    private void updateIndeterminate() {
        indeterminatePosition += 0.014f;
        if (indeterminatePosition > 1f) {
            indeterminatePosition = -0.35f;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle track = getTrackBounds();
            paintTrack(g2, track);
            if (indeterminate) {
                paintIndeterminate(g2, track);
            } else {
                paintFill(g2, track);
            }
            if (showLabel) {
                paintLabel(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pelo trilho da barra.
     */
    protected Rectangle getTrackBounds() {
        int width = showLabel ? getWidth() - labelWidth() - UiTokens.space(2) : getWidth();
        return new Rectangle(0, (getHeight() - barHeight) / 2, Math.max(0, width), barHeight);
    }

    /**
     * Pinta o trilho de fundo.
     */
    protected void paintTrack(Graphics2D g2, Rectangle track) {
        Color color = trackColor != null ? trackColor : UiTokens.overlay(UiTokens.muted(), 0.18f);
        PaintUtils.fillRoundRect(g2, track, track.height, isEnabled() ? color : UiTokens.disabled(color));
    }

    /**
     * Pinta a porção preenchida da barra.
     */
    protected void paintFill(Graphics2D g2, Rectangle track) {
        int width = (int) Math.round(track.width * (displayedValue / maximum));
        if (width <= 0) {
            return;
        }
        Rectangle filled = new Rectangle(track.x, track.y, width, track.height);
        PaintUtils.fillRoundRect(g2, filled, track.height, resolveFill());
    }

    /**
     * Pinta o bloco deslizante do modo indeterminado.
     */
    protected void paintIndeterminate(Graphics2D g2, Rectangle track) {
        int blockWidth = Math.max(UiTokens.scale(40), track.width / 3);
        int x = Math.round(track.x + track.width * indeterminatePosition);

        Shape previousClip = g2.getClip();
        g2.clip(PaintUtils.roundRect(track, track.height));
        PaintUtils.fillRoundRect(g2, new Rectangle(x, track.y, blockWidth, track.height),
                track.height, resolveFill());
        g2.setClip(previousClip);
    }

    /**
     * Pinta o rótulo com o percentual corrente.
     */
    protected void paintLabel(Graphics2D g2) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.fontSmall());
        String text = labelFormatter.apply(displayedValue / maximum * 100d);
        Color color = labelColor != null ? labelColor : UiTokens.muted();
        Rectangle bounds = new Rectangle(getWidth() - labelWidth(), 0, labelWidth(), getHeight());
        PaintUtils.drawCenteredText(g2, text, bounds, isEnabled() ? color : UiTokens.disabled(color));
    }

    private int labelWidth() {
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.fontSmall());
        return metrics.stringWidth("100%") + UiTokens.space(2);
    }

    private Color resolveFill() {
        Color base = fillColor != null ? fillColor : switch (tone) {
            case SUCCESS -> UiTokens.success();
            case WARNING -> UiTokens.warning();
            case DANGER -> UiTokens.danger();
            case INFO -> UiTokens.info();
            case PRIMARY -> UiTokens.primary();
        };
        return isEnabled() ? base : UiTokens.disabled(base);
    }

    /**
     * Fonte usada no rótulo, ajustada para negrito.
     */
    public ProgressBar setLabelBold(boolean bold) {
        setFont(bold ? UiTokens.fontSmall().deriveFont(Font.BOLD) : UiTokens.fontSmall());
        repaint();
        return this;
    }
}
