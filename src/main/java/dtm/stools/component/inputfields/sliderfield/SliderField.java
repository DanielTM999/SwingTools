package dtm.stools.component.inputfields.sliderfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.awt.event.MouseMotionAdapter;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Controle deslizante de valor único com trilho fino, polegar arredondado e balão de valor opcional.
 */
public class SliderField extends PanelEventListener {

    public static final String VALUE_CHANGED = "sliderValueChanged";
    public static final String DRAG_FINISHED = "sliderDragFinished";

    private double minimum;
    private double maximum = 100d;
    private double value;
    private double step = 1d;

    private boolean dragging;
    private boolean hover;
    private boolean focusPainted = true;
    private boolean showValue;
    private boolean showTicks;
    private int tickCount = 5;

    private int trackHeight = 6;
    private int thumbSize = 16;
    private int preferredWidth = 220;
    private float focusStrokeWidth = 2f;
    private int focusGap = 3;

    private Color trackColor;
    private Color fillColor;
    private Color thumbColor;
    private Color focusColor;
    private Color valueColor;

    private DoubleFunction<String> valueFormatter = current -> String.valueOf(Math.round(current));

    public SliderField() {
        this(0d, 100d, 0d);
    }

    public SliderField(double minimum, double maximum, double value) {
        super(null, false);
        if (minimum >= maximum) {
            throw new IllegalArgumentException("minimum must be lower than maximum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = clamp(value);

        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UiTokens.font());

        installListeners();
        updatePreferredSize();
    }

    /**
     * Valor corrente do controle.
     */
    public double getValue() {
        return value;
    }

    /**
     * Define o valor corrente disparando eventos.
     */
    public SliderField setValue(double value) {
        return setValue(value, true);
    }

    /**
     * Define o valor corrente, opcionalmente sem disparar eventos.
     */
    public SliderField setValue(double value, boolean fireEvent) {
        double snapped = snap(clamp(value));
        if (Double.compare(snapped, this.value) == 0) {
            return this;
        }

        double oldValue = this.value;
        this.value = snapped;
        firePropertyChange("value", oldValue, snapped);
        repaint();

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", snapped);
            dispatchEvent(EventType.CHANGE, this, snapped, props);
            dispatchEvent(VALUE_CHANGED, this, snapped, props);
        }
        return this;
    }

    /**
     * Limite inferior do intervalo.
     */
    public double getMinimum() {
        return minimum;
    }

    /**
     * Limite superior do intervalo.
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * Define o intervalo aceito pelo controle.
     */
    public SliderField setRange(double minimum, double maximum) {
        if (minimum >= maximum) {
            throw new IllegalArgumentException("minimum must be lower than maximum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        setValue(clamp(value), false);
        repaint();
        return this;
    }

    /**
     * Incremento aplicado ao arrastar e às teclas de seta.
     */
    public double getStep() {
        return step;
    }

    /**
     * Define o incremento do controle; zero permite valores contínuos.
     */
    public SliderField setStep(double step) {
        if (step < 0) {
            throw new IllegalArgumentException("step cannot be negative");
        }
        this.step = step;
        return this;
    }

    /**
     * Exibe ou oculta o balão com o valor corrente.
     */
    public SliderField setShowValue(boolean showValue) {
        this.showValue = showValue;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Exibe ou oculta as marcações do trilho.
     */
    public SliderField setShowTicks(boolean showTicks) {
        this.showTicks = showTicks;
        repaint();
        return this;
    }

    /**
     * Define a quantidade de marcações exibidas no trilho.
     */
    public SliderField setTickCount(int tickCount) {
        if (tickCount < 2) {
            throw new IllegalArgumentException("tickCount must be at least two");
        }
        this.tickCount = tickCount;
        repaint();
        return this;
    }

    /**
     * Define a espessura do trilho.
     */
    public SliderField setTrackHeight(int trackHeight) {
        if (trackHeight <= 0) {
            throw new IllegalArgumentException("trackHeight must be greater than zero");
        }
        this.trackHeight = trackHeight;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o diâmetro do polegar.
     */
    public SliderField setThumbSize(int thumbSize) {
        if (thumbSize <= 0) {
            throw new IllegalArgumentException("thumbSize must be greater than zero");
        }
        this.thumbSize = thumbSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a largura preferencial do controle.
     */
    public SliderField setPreferredWidth(int preferredWidth) {
        if (preferredWidth <= 0) {
            throw new IllegalArgumentException("preferredWidth must be greater than zero");
        }
        this.preferredWidth = preferredWidth;
        updatePreferredSize();
        return this;
    }

    /**
     * Habilita a pintura do anel de foco.
     */
    public SliderField setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define como o valor é convertido em texto no balão.
     */
    public SliderField setValueFormatter(DoubleFunction<String> valueFormatter) {
        if (valueFormatter == null) {
            throw new IllegalArgumentException("valueFormatter cannot be null");
        }
        this.valueFormatter = valueFormatter;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do controle.
     */
    public SliderField setColors(Color trackColor, Color fillColor, Color thumbColor) {
        this.trackColor = trackColor;
        this.fillColor = fillColor;
        this.thumbColor = thumbColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do anel de foco.
     */
    public SliderField setFocusColor(Color focusColor) {
        this.focusColor = focusColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do texto do balão de valor.
     */
    public SliderField setValueColor(Color valueColor) {
        this.valueColor = valueColor;
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                requestFocusInWindow();
                dragging = true;
                setValue(valueAt(e.getX()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) {
                    return;
                }
                dragging = false;
                repaint();
                dispatchEvent(DRAG_FINISHED, SliderField.this, value, Map.of("newValue", value));
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

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isEnabled() || !dragging) {
                    return;
                }
                setValue(valueAt(e.getX()));
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) {
                    return;
                }
                double increment = step > 0 ? step : (maximum - minimum) / 100d;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_DOWN -> {
                        setValue(value - increment);
                        e.consume();
                    }
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_UP -> {
                        setValue(value + increment);
                        e.consume();
                    }
                    case KeyEvent.VK_HOME -> {
                        setValue(minimum);
                        e.consume();
                    }
                    case KeyEvent.VK_END -> {
                        setValue(maximum);
                        e.consume();
                    }
                    case KeyEvent.VK_PAGE_UP -> {
                        setValue(value + increment * 10);
                        e.consume();
                    }
                    case KeyEvent.VK_PAGE_DOWN -> {
                        setValue(value - increment * 10);
                        e.consume();
                    }
                    default -> {
                    }
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
        int height = Math.max(thumbSize, trackHeight) + focusGap * 2;
        if (showValue) {
            height += getFontMetrics(getFont() != null ? getFont() : UiTokens.font()).getHeight() + UiTokens.space(1);
        }
        setPreferredSize(new Dimension(preferredWidth, height));
        setMinimumSize(new Dimension(thumbSize * 4, height));
        revalidate();
    }

    private double clamp(double candidate) {
        return Math.max(minimum, Math.min(maximum, candidate));
    }

    private double snap(double candidate) {
        if (step <= 0) {
            return candidate;
        }
        double steps = Math.round((candidate - minimum) / step);
        return clamp(minimum + steps * step);
    }

    private double valueAt(int x) {
        Rectangle track = getTrackBounds();
        if (track.width <= 0) {
            return value;
        }
        double ratio = (double) (x - track.x) / track.width;
        return minimum + ratio * (maximum - minimum);
    }

    private float progress() {
        return (float) ((value - minimum) / (maximum - minimum));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle track = getTrackBounds();
            paintTrack(g2, track);
            if (showTicks) {
                paintTicks(g2, track);
            }
            paintFill(g2, track);
            Rectangle thumb = getThumbBounds(track);
            if (focusPainted && isFocusOwner()) {
                paintFocus(g2, thumb);
            }
            paintThumb(g2, thumb);
            if (showValue) {
                paintValue(g2, thumb);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pelo trilho.
     */
    protected Rectangle getTrackBounds() {
        int half = thumbSize / 2;
        int y = showValue
                ? getHeight() - Math.max(thumbSize, trackHeight) / 2 - trackHeight / 2 - focusGap
                : (getHeight() - trackHeight) / 2;
        return new Rectangle(half, y, Math.max(0, getWidth() - thumbSize), trackHeight);
    }

    /**
     * Retângulo ocupado pelo polegar.
     */
    protected Rectangle getThumbBounds(Rectangle track) {
        int x = Math.round(track.x + track.width * progress()) - thumbSize / 2;
        int y = track.y + track.height / 2 - thumbSize / 2;
        return new Rectangle(x, y, thumbSize, thumbSize);
    }

    /**
     * Pinta o trilho de fundo.
     */
    protected void paintTrack(Graphics2D g2, Rectangle track) {
        Color color = trackColor != null ? trackColor : UiTokens.surfaceAlt();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        PaintUtils.fillRoundRect(g2, track, track.height, color);
    }

    /**
     * Pinta a porção preenchida do trilho.
     */
    protected void paintFill(Graphics2D g2, Rectangle track) {
        Color color = fillColor != null ? fillColor : UiTokens.primary();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        Rectangle filled = new Rectangle(track.x, track.y, Math.round(track.width * progress()), track.height);
        PaintUtils.fillRoundRect(g2, filled, track.height, color);
    }

    /**
     * Pinta as marcações do trilho.
     */
    protected void paintTicks(Graphics2D g2, Rectangle track) {
        g2.setColor(UiTokens.overlay(UiTokens.muted(), 0.55f));
        for (int i = 0; i < tickCount; i++) {
            int x = track.x + Math.round((float) track.width * i / (tickCount - 1));
            g2.fillOval(x - 1, track.y + track.height / 2 - 1, 2, 2);
        }
    }

    /**
     * Pinta o polegar do controle.
     */
    protected void paintThumb(Graphics2D g2, Rectangle thumb) {
        if (isEnabled()) {
            g2.setColor(UiTokens.overlay(Color.BLACK, 0.14f));
            g2.fillOval(thumb.x, thumb.y + 1, thumb.width, thumb.height);
        }

        Color color = thumbColor != null ? thumbColor : UiTokens.surface();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        } else if (dragging) {
            color = UiTokens.pressed(color);
        } else if (hover) {
            color = UiTokens.hover(color);
        }

        g2.setColor(color);
        g2.fillOval(thumb.x, thumb.y, thumb.width, thumb.height);
        g2.setColor(isEnabled() ? (fillColor != null ? fillColor : UiTokens.primary()) : UiTokens.border());
        g2.setStroke(new java.awt.BasicStroke(UiTokens.stroke()));
        g2.drawOval(thumb.x, thumb.y, thumb.width - 1, thumb.height - 1);
    }

    /**
     * Pinta o anel de foco ao redor do polegar.
     */
    protected void paintFocus(Graphics2D g2, Rectangle thumb) {
        Color color = focusColor != null ? focusColor : UiTokens.overlay(UiTokens.accent(), 0.35f);
        g2.setColor(color);
        g2.fillOval(thumb.x - focusGap, thumb.y - focusGap, thumb.width + focusGap * 2, thumb.height + focusGap * 2);
    }

    /**
     * Pinta o balão com o valor corrente acima do polegar.
     */
    protected void paintValue(Graphics2D g2, Rectangle thumb) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.font());
        FontMetrics metrics = g2.getFontMetrics();
        String text = valueFormatter.apply(value);
        int width = metrics.stringWidth(text);
        Rectangle bounds = new Rectangle(
                Math.max(0, Math.min(getWidth() - width, thumb.x + thumb.width / 2 - width / 2)),
                0,
                width,
                metrics.getHeight());
        Color color = valueColor != null ? valueColor : UiTokens.muted();
        PaintUtils.drawCenteredText(g2, text, bounds, isEnabled() ? color : UiTokens.disabled(color));
    }
}
