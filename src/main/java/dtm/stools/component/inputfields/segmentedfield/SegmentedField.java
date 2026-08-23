package dtm.stools.component.inputfields.segmentedfield;

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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Controle segmentado em formato de pílula com indicador deslizante animado.
 */
public class SegmentedField<T> extends PanelEventListener {

    public static final String SEGMENT_SELECTED = "segmentSelected";

    /**
     * Segmento exibido pelo controle.
     */
    public record Segment<T>(String label, T value) {
    }

    private final List<Segment<T>> segments = new ArrayList<>();

    private int selectedIndex = -1;
    private int hoverIndex = -1;
    private boolean animated = true;
    private boolean focusPainted = true;

    private int animationDuration = 180;
    private float indicatorPosition;
    private float indicatorStart;
    private float indicatorTarget;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;

    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private int segmentPadding = 14;
    private int trackPadding = 3;
    private int minSegmentWidth = 56;
    private int preferredHeight = 36;

    private Color trackColor;
    private Color indicatorColor;
    private Color selectedTextColor;
    private Color textColor;
    private Color focusColor;

    private final Timer animationTimer;

    public SegmentedField() {
        super(null, false);
        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UiTokens.font());

        animationTimer = new Timer(16, e -> updateAnimation());
        installListeners();
        updatePreferredSize();
    }

    /**
     * Adiciona um segmento com rótulo e valor.
     */
    public SegmentedField<T> addSegment(String label, T value) {
        segments.add(new Segment<>(label != null ? label : "", value));
        if (selectedIndex < 0) {
            selectedIndex = 0;
            indicatorPosition = 0f;
            indicatorTarget = 0f;
        }
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Substitui os segmentos usando os valores e o provedor de rótulos informados.
     */
    public SegmentedField<T> setSegments(List<T> values, Function<T, String> labelProvider) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        Function<T, String> labels = labelProvider != null ? labelProvider : String::valueOf;
        segments.clear();
        selectedIndex = -1;
        for (T value : values) {
            addSegment(labels.apply(value), value);
        }
        return this;
    }

    /**
     * Substitui os segmentos a partir de um mapa ordenado de rótulo e valor.
     */
    public SegmentedField<T> setSegments(LinkedHashMap<String, T> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        segments.clear();
        selectedIndex = -1;
        entries.forEach(this::addSegment);
        return this;
    }

    /**
     * Índice do segmento marcado.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Valor do segmento marcado, ou {@code null} quando não há seleção.
     */
    public T getSelectedValue() {
        return isValidIndex(selectedIndex) ? segments.get(selectedIndex).value() : null;
    }

    /**
     * Marca o segmento pelo índice disparando eventos.
     */
    public SegmentedField<T> setSelectedIndex(int index) {
        return setSelectedIndex(index, true);
    }

    /**
     * Marca o segmento pelo índice, opcionalmente sem disparar eventos.
     */
    public SegmentedField<T> setSelectedIndex(int index, boolean fireEvent) {
        if (!isValidIndex(index) || index == selectedIndex) {
            return this;
        }

        int oldIndex = selectedIndex;
        T oldValue = isValidIndex(oldIndex) ? segments.get(oldIndex).value() : null;
        selectedIndex = index;
        animateToSelection();
        firePropertyChange("selectedIndex", oldIndex, index);

        if (fireEvent) {
            T newValue = segments.get(index).value();
            Map<String, Object> props = Map.of(
                    "oldValue", String.valueOf(oldValue),
                    "newValue", String.valueOf(newValue),
                    "index", index);
            dispatchEvent(EventType.CHANGE, this, newValue, props);
            dispatchEvent(EventType.SELECT, this, newValue, props);
            dispatchEvent(SEGMENT_SELECTED, this, newValue, props);
        }
        return this;
    }

    /**
     * Marca o segmento cujo valor corresponde ao informado.
     */
    public SegmentedField<T> setSelectedValue(T value) {
        return setSelectedValue(value, true);
    }

    /**
     * Marca o segmento cujo valor corresponde ao informado, opcionalmente sem disparar eventos.
     */
    public SegmentedField<T> setSelectedValue(T value, boolean fireEvent) {
        for (int i = 0; i < segments.size(); i++) {
            if (Objects.equals(segments.get(i).value(), value)) {
                return setSelectedIndex(i, fireEvent);
            }
        }
        return this;
    }

    /**
     * Habilita a animação do indicador.
     */
    public SegmentedField<T> setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            indicatorPosition = selectedIndex;
            indicatorTarget = indicatorPosition;
            repaint();
        }
        return this;
    }

    /**
     * Define a duração da animação em milissegundos.
     */
    public SegmentedField<T> setAnimationDuration(int animationDuration) {
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration cannot be negative");
        }
        this.animationDuration = animationDuration;
        return this;
    }

    /**
     * Define o raio de canto do trilho e do indicador.
     */
    public SegmentedField<T> setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define o espaçamento horizontal interno de cada segmento.
     */
    public SegmentedField<T> setSegmentPadding(int segmentPadding) {
        if (segmentPadding < 0) {
            throw new IllegalArgumentException("segmentPadding cannot be negative");
        }
        this.segmentPadding = segmentPadding;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a largura mínima de cada segmento.
     */
    public SegmentedField<T> setMinSegmentWidth(int minSegmentWidth) {
        if (minSegmentWidth < 0) {
            throw new IllegalArgumentException("minSegmentWidth cannot be negative");
        }
        this.minSegmentWidth = minSegmentWidth;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a altura preferencial do controle.
     */
    public SegmentedField<T> setPreferredHeight(int preferredHeight) {
        if (preferredHeight <= 0) {
            throw new IllegalArgumentException("preferredHeight must be greater than zero");
        }
        this.preferredHeight = preferredHeight;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita a pintura do anel de foco.
     */
    public SegmentedField<T> setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define as cores principais do controle.
     */
    public SegmentedField<T> setColors(Color trackColor, Color indicatorColor, Color selectedTextColor, Color textColor) {
        this.trackColor = trackColor;
        this.indicatorColor = indicatorColor;
        this.selectedTextColor = selectedTextColor;
        this.textColor = textColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do anel de foco.
     */
    public SegmentedField<T> setFocusColor(Color focusColor) {
        this.focusColor = focusColor;
        repaint();
        return this;
    }

    /**
     * Segmentos registrados no controle.
     */
    public List<Segment<T>> getSegments() {
        return List.copyOf(segments);
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        updatePreferredSize();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
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
                    setSelectedIndex(index);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = isEnabled() ? indexAt(e.getX()) : -1;
                if (index != hoverIndex) {
                    hoverIndex = index;
                    repaint();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || segments.isEmpty()) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> {
                        setSelectedIndex(Math.max(0, selectedIndex - 1));
                        e.consume();
                    }
                    case KeyEvent.VK_RIGHT -> {
                        setSelectedIndex(Math.min(segments.size() - 1, selectedIndex + 1));
                        e.consume();
                    }
                    case KeyEvent.VK_HOME -> {
                        setSelectedIndex(0);
                        e.consume();
                    }
                    case KeyEvent.VK_END -> {
                        setSelectedIndex(segments.size() - 1);
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
        if (segments == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.font());
        int widest = minSegmentWidth;
        for (Segment<T> segment : segments) {
            widest = Math.max(widest, metrics.stringWidth(segment.label()) + segmentPadding * 2);
        }
        int width = Math.max(widest * Math.max(1, segments.size()) + trackPadding * 2, minSegmentWidth);
        Dimension size = new Dimension(width, preferredHeight);
        setPreferredSize(size);
        setMinimumSize(new Dimension(minSegmentWidth, preferredHeight));
        revalidate();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < segments.size();
    }

    private int indexAt(int x) {
        if (segments.isEmpty()) {
            return -1;
        }
        Rectangle track = getTrackBounds();
        float segmentWidth = segmentWidth(track);
        int index = (int) ((x - track.x) / segmentWidth);
        return isValidIndex(index) ? index : -1;
    }

    private float segmentWidth(Rectangle track) {
        return (float) (track.width - trackPadding * 2) / Math.max(1, segments.size());
    }

    private void animateToSelection() {
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationTimer.stop();
            indicatorPosition = selectedIndex;
            indicatorTarget = indicatorPosition;
            repaint();
            return;
        }

        indicatorStart = indicatorPosition;
        indicatorTarget = selectedIndex;
        animationStartedAtNanos = System.nanoTime();
        animationRunDurationNanos = (long) (animationDuration * 1_000_000L
                * Math.min(1f, Math.abs(indicatorTarget - indicatorStart)));
        if (animationRunDurationNanos <= 0) {
            indicatorPosition = indicatorTarget;
            repaint();
            return;
        }
        animationTimer.start();
    }

    private void updateAnimation() {
        long elapsed = System.nanoTime() - animationStartedAtNanos;
        float ratio = Math.min(1f, (float) elapsed / animationRunDurationNanos);
        indicatorPosition = indicatorStart + (indicatorTarget - indicatorStart) * PaintUtils.easeInOut(ratio);
        if (ratio >= 1f) {
            indicatorPosition = indicatorTarget;
            animationTimer.stop();
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
            if (!segments.isEmpty()) {
                paintIndicator(g2, track);
                paintLabels(g2, track);
            }
            if (focusPainted && isFocusOwner()) {
                paintFocus(g2, track);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pelo trilho do controle.
     */
    protected Rectangle getTrackBounds() {
        return new Rectangle(0, 0, getWidth(), getHeight());
    }

    /**
     * Pinta o trilho de fundo.
     */
    protected void paintTrack(Graphics2D g2, Rectangle track) {
        Color color = trackColor != null ? trackColor : UiTokens.surfaceAlt();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        PaintUtils.fillRoundRect(g2, track, arc, color);
        PaintUtils.drawRoundRect(g2, track, arc, UiTokens.border(), UiTokens.stroke());
    }

    /**
     * Pinta o indicador deslizante do segmento marcado.
     */
    protected void paintIndicator(Graphics2D g2, Rectangle track) {
        if (indicatorPosition < 0) {
            return;
        }
        float segmentWidth = segmentWidth(track);
        Rectangle indicator = new Rectangle(
                Math.round(track.x + trackPadding + indicatorPosition * segmentWidth),
                track.y + trackPadding,
                Math.round(segmentWidth),
                track.height - trackPadding * 2);

        Color color = indicatorColor != null ? indicatorColor : UiTokens.surface();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }
        PaintUtils.fillRoundRect(g2, indicator, Math.max(0, arc - trackPadding), color);
        PaintUtils.drawRoundRect(g2, indicator, Math.max(0, arc - trackPadding),
                UiTokens.overlay(UiTokens.border(), 0.6f), 1f);
    }

    /**
     * Pinta o rótulo de cada segmento.
     */
    protected void paintLabels(Graphics2D g2, Rectangle track) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.font());
        float segmentWidth = segmentWidth(track);

        for (int i = 0; i < segments.size(); i++) {
            Rectangle bounds = new Rectangle(
                    Math.round(track.x + trackPadding + i * segmentWidth),
                    track.y + trackPadding,
                    Math.round(segmentWidth),
                    track.height - trackPadding * 2);
            PaintUtils.drawCenteredText(g2, segments.get(i).label(), inset(bounds), resolveLabelColor(i));
        }
    }

    /**
     * Pinta o anel de foco ao redor do trilho.
     */
    protected void paintFocus(Graphics2D g2, Rectangle track) {
        Color color = focusColor != null ? focusColor : UiTokens.overlay(UiTokens.accent(), 0.5f);
        Rectangle ring = new Rectangle(track.x + 1, track.y + 1, track.width - 2, track.height - 2);
        PaintUtils.drawRoundRect(g2, ring, arc, color, 2f);
    }

    private Rectangle inset(Rectangle bounds) {
        Insets insets = new Insets(0, segmentPadding / 2, 0, segmentPadding / 2);
        return new Rectangle(
                bounds.x + insets.left,
                bounds.y,
                Math.max(0, bounds.width - insets.left - insets.right),
                bounds.height);
    }

    private Color resolveLabelColor(int index) {
        if (!isEnabled()) {
            return UiTokens.disabled(UiTokens.foreground());
        }
        if (index == selectedIndex) {
            return selectedTextColor != null ? selectedTextColor : UiTokens.foreground();
        }
        Color base = textColor != null ? textColor : UiTokens.muted();
        return index == hoverIndex ? UiTokens.foreground() : base;
    }
}
