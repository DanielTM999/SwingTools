package dtm.stools.component.inputfields.ratingfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.Map;

/**
 * Campo de avaliação por estrelas com suporte a meia estrela, pré-visualização no hover e modo somente leitura.
 */
public class RatingField extends PanelEventListener {

    public static final String RATED = "rated";

    private int count = 5;
    private double value;
    private double hoverValue = -1;

    private boolean allowHalf;
    private boolean readOnly;
    private boolean clearable = true;
    private boolean focusPainted = true;

    private int iconSize = 22;
    private int iconGap = 4;
    private float focusStrokeWidth = 2f;
    private int focusGap = 2;

    private Color filledColor;
    private Color emptyColor;
    private Color hoverColor;
    private Color focusColor;

    public RatingField() {
        this(5, 0d);
    }

    public RatingField(int count) {
        this(count, 0d);
    }

    public RatingField(int count, double value) {
        super(null, false);
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
        this.count = count;
        this.value = clamp(value);

        setFocusable(true);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        installListeners();
        updatePreferredSize();
    }

    /**
     * Avaliação corrente.
     */
    public double getValue() {
        return value;
    }

    /**
     * Define a avaliação corrente disparando eventos.
     */
    public RatingField setValue(double value) {
        return setValue(value, true);
    }

    /**
     * Define a avaliação corrente, opcionalmente sem disparar eventos.
     */
    public RatingField setValue(double value, boolean fireEvent) {
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
            dispatchEvent(RATED, this, snapped, props);
        }
        return this;
    }

    /**
     * Quantidade de estrelas exibidas.
     */
    public int getCount() {
        return count;
    }

    /**
     * Define a quantidade de estrelas exibidas.
     */
    public RatingField setCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than zero");
        }
        this.count = count;
        this.value = clamp(value);
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita avaliações com meia estrela.
     */
    public RatingField setAllowHalf(boolean allowHalf) {
        this.allowHalf = allowHalf;
        this.value = snap(value);
        repaint();
        return this;
    }

    /**
     * Indica se meia estrela é permitida.
     */
    public boolean isAllowHalf() {
        return allowHalf;
    }

    /**
     * Impede a alteração da avaliação pelo usuário.
     */
    public RatingField setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        setCursor(Cursor.getPredefinedCursor(readOnly ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        repaint();
        return this;
    }

    /**
     * Indica se o campo está em modo somente leitura.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Permite zerar a avaliação clicando na estrela já marcada.
     */
    public RatingField setClearable(boolean clearable) {
        this.clearable = clearable;
        return this;
    }

    /**
     * Define o tamanho de cada estrela.
     */
    public RatingField setIconSize(int iconSize) {
        if (iconSize <= 0) {
            throw new IllegalArgumentException("iconSize must be greater than zero");
        }
        this.iconSize = iconSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o espaço entre as estrelas.
     */
    public RatingField setIconGap(int iconGap) {
        if (iconGap < 0) {
            throw new IllegalArgumentException("iconGap cannot be negative");
        }
        this.iconGap = iconGap;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Habilita a pintura do anel de foco.
     */
    public RatingField setFocusPainted(boolean focusPainted) {
        this.focusPainted = focusPainted;
        repaint();
        return this;
    }

    /**
     * Define as cores das estrelas preenchidas, vazias e em hover.
     */
    public RatingField setColors(Color filledColor, Color emptyColor, Color hoverColor) {
        this.filledColor = filledColor;
        this.emptyColor = emptyColor;
        this.hoverColor = hoverColor;
        repaint();
        return this;
    }

    /**
     * Define a cor do anel de foco.
     */
    public RatingField setFocusColor(Color focusColor) {
        this.focusColor = focusColor;
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled && !readOnly ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isInteractive() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                requestFocusInWindow();
                double candidate = valueAt(e.getX());
                setValue(clearable && Double.compare(candidate, value) == 0 ? 0d : candidate);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverValue = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isInteractive()) {
                    return;
                }
                double candidate = valueAt(e.getX());
                if (Double.compare(candidate, hoverValue) != 0) {
                    hoverValue = candidate;
                    repaint();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isInteractive()) {
                    return;
                }
                double increment = allowHalf ? 0.5d : 1d;
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
                        setValue(0d);
                        e.consume();
                    }
                    case KeyEvent.VK_END -> {
                        setValue(count);
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

    private boolean isInteractive() {
        return isEnabled() && !readOnly;
    }

    private void updatePreferredSize() {
        int width = count * iconSize + (count - 1) * iconGap + focusGap * 2;
        int height = iconSize + focusGap * 2;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        revalidate();
    }

    private double clamp(double candidate) {
        return Math.max(0d, Math.min(count, candidate));
    }

    private double snap(double candidate) {
        double unit = allowHalf ? 0.5d : 1d;
        return clamp(Math.round(candidate / unit) * unit);
    }

    private double valueAt(int x) {
        int index = (x - focusGap) / (iconSize + iconGap);
        index = Math.max(0, Math.min(count - 1, index));
        if (!allowHalf) {
            return index + 1d;
        }
        int localX = x - focusGap - index * (iconSize + iconGap);
        return index + (localX < iconSize / 2 ? 0.5d : 1d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            double displayed = hoverValue >= 0 ? hoverValue : value;
            for (int i = 0; i < count; i++) {
                paintStar(g2, getStarBounds(i), Math.max(0d, Math.min(1d, displayed - i)), hoverValue >= 0);
            }
            if (focusPainted && isFocusOwner()) {
                paintFocus(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pela estrela do índice informado.
     */
    protected Rectangle getStarBounds(int index) {
        int x = focusGap + index * (iconSize + iconGap);
        int y = (getHeight() - iconSize) / 2;
        return new Rectangle(x, y, iconSize, iconSize);
    }

    /**
     * Pinta uma estrela com o preenchimento parcial informado.
     */
    protected void paintStar(Graphics2D g2, Rectangle bounds, double fillRatio, boolean previewing) {
        Shape star = buildStar(bounds);

        Color empty = emptyColor != null ? emptyColor : UiTokens.overlay(UiTokens.muted(), 0.28f);
        g2.setColor(isEnabled() ? empty : UiTokens.disabled(empty));
        g2.fill(star);

        if (fillRatio > 0) {
            Color base = previewing
                    ? (hoverColor != null ? hoverColor : UiTokens.warning())
                    : (filledColor != null ? filledColor : UiTokens.warning());
            g2.setColor(isEnabled() ? base : UiTokens.disabled(base));
            Shape previousClip = g2.getClip();
            g2.clip(new Rectangle(bounds.x, bounds.y, (int) Math.round(bounds.width * fillRatio), bounds.height));
            g2.fill(star);
            g2.setClip(previousClip);
        }

        g2.setColor(UiTokens.overlay(UiTokens.border(), 0.6f));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(star);
    }

    /**
     * Pinta o anel de foco ao redor de todas as estrelas.
     */
    protected void paintFocus(Graphics2D g2) {
        Color color = focusColor != null ? focusColor : UiTokens.overlay(UiTokens.accent(), 0.55f);
        Rectangle bounds = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
        PaintUtils.drawRoundRect(g2, bounds, UiTokens.radius(UiTokens.Radius.SM), color, focusStrokeWidth);
    }

    private Shape buildStar(Rectangle bounds) {
        Path2D.Double path = new Path2D.Double();
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();
        double outer = bounds.width / 2d;
        double inner = outer * 0.42d;

        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(-90 + i * 36);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        return new Area(path);
    }
}
