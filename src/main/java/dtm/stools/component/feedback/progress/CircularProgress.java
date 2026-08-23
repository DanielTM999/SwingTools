package dtm.stools.component.feedback.progress;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Anel de progresso com modos determinado e indeterminado e texto central opcional.
 */
public class CircularProgress extends PanelEventListener {

    public static final String PROGRESS = "circularProgress";

    private double value;
    private double maximum = 100d;

    private boolean indeterminate;
    private boolean showLabel = true;

    private int diameter = 64;
    private int ringThickness = 6;
    private int startAngle = 90;

    private float sweepPosition;
    private double displayedValue;
    private double animationStart;
    private double animationTarget;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;
    private int animationDuration = 320;

    private Color trackColor;
    private Color fillColor;
    private Color labelColor;

    private DoubleFunction<String> labelFormatter = current -> Math.round(current) + "%";

    private final Timer animationTimer;
    private final Timer indeterminateTimer;

    public CircularProgress() {
        this(0d);
    }

    public CircularProgress(double value) {
        super(null, false);
        this.value = clamp(value);
        this.displayedValue = this.value;

        setOpaque(false);
        setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));

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
    public CircularProgress setValue(double value) {
        return setValue(value, true);
    }

    /**
     * Define o valor corrente, opcionalmente sem disparar eventos.
     */
    public CircularProgress setValue(double value, boolean fireEvent) {
        double clamped = clamp(value);
        if (Double.compare(clamped, this.value) == 0) {
            return this;
        }

        double oldValue = this.value;
        this.value = clamped;
        animateToValue();

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", clamped, "maximum", maximum);
            dispatchEvent(PROGRESS, this, clamped, props);
            dispatchEvent(EventType.CHANGE, this, clamped, props);
        }
        return this;
    }

    /**
     * Define o valor máximo do progresso.
     */
    public CircularProgress setMaximum(double maximum) {
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
    public CircularProgress setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (indeterminate) {
            animationTimer.stop();
            if (isShowing()) {
                indeterminateTimer.start();
            }
        } else {
            indeterminateTimer.stop();
        }
        repaint();
        return this;
    }

    /**
     * Indica se o anel está em modo indeterminado.
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    /**
     * Define o diâmetro do anel.
     */
    public CircularProgress setDiameter(int diameter) {
        if (diameter <= 0) {
            throw new IllegalArgumentException("diameter must be greater than zero");
        }
        this.diameter = diameter;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a espessura do anel.
     */
    public CircularProgress setRingThickness(int ringThickness) {
        if (ringThickness <= 0) {
            throw new IllegalArgumentException("ringThickness must be greater than zero");
        }
        this.ringThickness = ringThickness;
        repaint();
        return this;
    }

    /**
     * Exibe ou oculta o texto central.
     */
    public CircularProgress setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
        repaint();
        return this;
    }

    /**
     * Define como o valor é convertido em texto central.
     */
    public CircularProgress setLabelFormatter(DoubleFunction<String> labelFormatter) {
        if (labelFormatter == null) {
            throw new IllegalArgumentException("labelFormatter cannot be null");
        }
        this.labelFormatter = labelFormatter;
        repaint();
        return this;
    }

    /**
     * Define as cores do trilho, do preenchimento e do texto.
     */
    public CircularProgress setColors(Color trackColor, Color fillColor, Color labelColor) {
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
        Dimension size = new Dimension(diameter, diameter);
        setPreferredSize(size);
        setMinimumSize(size);
        revalidate();
    }

    private void animateToValue() {
        if (animationDuration <= 0 || !isShowing()) {
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
        sweepPosition = (sweepPosition + 6f) % 360f;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle ring = getRingBounds();
            paintTrack(g2, ring);
            paintArc(g2, ring);
            if (showLabel && !indeterminate) {
                paintLabel(g2, ring);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo que envolve o anel.
     */
    protected Rectangle getRingBounds() {
        int size = Math.min(getWidth(), getHeight()) - ringThickness;
        return new Rectangle((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
    }

    /**
     * Pinta o anel de fundo.
     */
    protected void paintTrack(Graphics2D g2, Rectangle ring) {
        Color color = trackColor != null ? trackColor : UiTokens.overlay(UiTokens.muted(), 0.18f);
        g2.setColor(isEnabled() ? color : UiTokens.disabled(color));
        g2.setStroke(new BasicStroke(ringThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Float(ring.x, ring.y, ring.width, ring.height, 0, 360, Arc2D.OPEN));
    }

    /**
     * Pinta o arco de progresso.
     */
    protected void paintArc(Graphics2D g2, Rectangle ring) {
        Color color = fillColor != null ? fillColor : UiTokens.primary();
        g2.setColor(isEnabled() ? color : UiTokens.disabled(color));
        g2.setStroke(new BasicStroke(ringThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        float extent = indeterminate ? -90f : (float) (-360d * displayedValue / maximum);
        float start = indeterminate ? startAngle - sweepPosition : startAngle;
        g2.draw(new Arc2D.Float(ring.x, ring.y, ring.width, ring.height, start, extent, Arc2D.OPEN));
    }

    /**
     * Pinta o texto central do anel.
     */
    protected void paintLabel(Graphics2D g2, Rectangle ring) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.fontSmall());
        String text = labelFormatter.apply(displayedValue / maximum * 100d);
        Color color = labelColor != null ? labelColor : UiTokens.foreground();
        PaintUtils.drawCenteredText(g2, text, ring, isEnabled() ? color : UiTokens.disabled(color));
    }
}
