package dtm.stools.component.panels.breadcrumb;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trilha de navegação clicável que colapsa os itens intermediários quando o espaço é insuficiente.
 */
public class BreadcrumbBar extends PanelEventListener {

    public static final String CRUMB_SELECTED = "crumbSelected";

    /**
     * Item exibido na trilha.
     */
    public record Crumb<T>(String label, T value) {
    }

    private static final String COLLAPSE_MARK = "…";

    private final List<Crumb<Object>> crumbs = new ArrayList<>();
    private final List<Rectangle> hitAreas = new ArrayList<>();

    private int hoverIndex = -1;
    private int separatorGap = UiTokens.space(2);
    private boolean lastClickable;

    private Color textColor;
    private Color activeColor;
    private Color separatorColor;

    public BreadcrumbBar() {
        super(null, false);
        setOpaque(false);
        setFont(UiTokens.font());
        setPreferredSize(new Dimension(UiTokens.scale(320), UiTokens.scale(26)));
        installListeners();
    }

    /**
     * Adiciona um item ao fim da trilha.
     */
    public BreadcrumbBar addCrumb(String label, Object value) {
        crumbs.add(new Crumb<>(label != null ? label : "", value));
        repaint();
        return this;
    }

    /**
     * Substitui todos os itens da trilha.
     */
    public BreadcrumbBar setCrumbs(List<String> labels) {
        crumbs.clear();
        if (labels != null) {
            labels.forEach(label -> crumbs.add(new Crumb<>(label, label)));
        }
        repaint();
        return this;
    }

    /**
     * Remove os itens posteriores ao índice informado.
     */
    public BreadcrumbBar truncateTo(int index) {
        if (index < 0 || index >= crumbs.size()) {
            throw new IllegalArgumentException("invalid crumb index: " + index);
        }
        while (crumbs.size() > index + 1) {
            crumbs.remove(crumbs.size() - 1);
        }
        repaint();
        return this;
    }

    /**
     * Remove todos os itens da trilha.
     */
    public BreadcrumbBar clearCrumbs() {
        crumbs.clear();
        hoverIndex = -1;
        repaint();
        return this;
    }

    /**
     * Itens registrados na trilha.
     */
    public List<Crumb<Object>> getCrumbs() {
        return List.copyOf(crumbs);
    }

    /**
     * Permite que o último item também responda a cliques.
     */
    public BreadcrumbBar setLastClickable(boolean lastClickable) {
        this.lastClickable = lastClickable;
        return this;
    }

    /**
     * Define o espaço entre o texto e o separador.
     */
    public BreadcrumbBar setSeparatorGap(int separatorGap) {
        if (separatorGap < 0) {
            throw new IllegalArgumentException("separatorGap cannot be negative");
        }
        this.separatorGap = separatorGap;
        repaint();
        return this;
    }

    /**
     * Define as cores dos itens, do item ativo e do separador.
     */
    public BreadcrumbBar setColors(Color textColor, Color activeColor, Color separatorColor) {
        this.textColor = textColor;
        this.activeColor = activeColor;
        this.separatorColor = separatorColor;
        repaint();
        return this;
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int index = indexAt(e.getPoint());
                if (index < 0 || !isClickable(index)) {
                    return;
                }
                Crumb<Object> crumb = crumbs.get(index);
                Map<String, Object> props = Map.of("index", index, "label", crumb.label());
                dispatchEvent(CRUMB_SELECTED, BreadcrumbBar.this, crumb.value(), props);
                dispatchEvent(EventType.SELECT, BreadcrumbBar.this, crumb.value(), props);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = isEnabled() ? indexAt(e.getPoint()) : -1;
                boolean clickable = index >= 0 && isClickable(index);
                setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (index != hoverIndex) {
                    hoverIndex = clickable ? index : -1;
                    repaint();
                }
            }
        });
    }

    private boolean isClickable(int index) {
        return lastClickable || index < crumbs.size() - 1;
    }

    private int indexAt(java.awt.Point point) {
        for (int i = 0; i < hitAreas.size(); i++) {
            if (hitAreas.get(i) != null && hitAreas.get(i).contains(point)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            g2.setFont(getFont() != null ? getFont() : UiTokens.font());
            layoutAndPaint(g2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Calcula as áreas de cada item e pinta a trilha, colapsando o meio quando necessário.
     */
    protected void layoutAndPaint(Graphics2D g2) {
        hitAreas.clear();
        for (int i = 0; i < crumbs.size(); i++) {
            hitAreas.add(null);
        }
        if (crumbs.isEmpty()) {
            return;
        }

        FontMetrics metrics = g2.getFontMetrics();
        List<Integer> visible = resolveVisibleIndices(metrics);

        int x = 0;
        for (int position = 0; position < visible.size(); position++) {
            int index = visible.get(position);

            if (index < 0) {
                x = paintCollapseMark(g2, metrics, x);
                continue;
            }

            String label = crumbs.get(index).label();
            int width = metrics.stringWidth(label);
            Rectangle bounds = new Rectangle(x, 0, width, getHeight());
            hitAreas.set(index, bounds);

            paintCrumb(g2, label, bounds, index);
            x += width;

            if (position < visible.size() - 1) {
                x = paintSeparator(g2, x);
            }
        }
    }

    private List<Integer> resolveVisibleIndices(FontMetrics metrics) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < crumbs.size(); i++) {
            all.add(i);
        }
        if (crumbs.size() <= 2 || totalWidth(metrics, all) <= getWidth()) {
            return all;
        }

        List<Integer> collapsed = new ArrayList<>();
        collapsed.add(0);
        collapsed.add(-1);
        for (int i = crumbs.size() - 1; i >= 1 && totalWidth(metrics, collapsed) <= getWidth(); i--) {
            collapsed.add(2, i);
        }
        if (collapsed.size() > 2 && totalWidth(metrics, collapsed) > getWidth()) {
            collapsed.remove(2);
        }
        return collapsed;
    }

    private int totalWidth(FontMetrics metrics, List<Integer> indices) {
        int width = 0;
        for (int index : indices) {
            width += index < 0 ? metrics.stringWidth(COLLAPSE_MARK) : metrics.stringWidth(crumbs.get(index).label());
        }
        return width + Math.max(0, indices.size() - 1) * (separatorGap * 2 + UiTokens.scale(5));
    }

    /**
     * Pinta o rótulo de um item da trilha.
     */
    protected void paintCrumb(Graphics2D g2, String label, Rectangle bounds, int index) {
        boolean active = index == crumbs.size() - 1;
        Color color = active
                ? (activeColor != null ? activeColor : UiTokens.foreground())
                : (textColor != null ? textColor : UiTokens.muted());
        if (index == hoverIndex) {
            color = UiTokens.primary();
        }
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }

        Font previous = g2.getFont();
        if (active) {
            g2.setFont(previous.deriveFont(Font.BOLD));
        }
        PaintUtils.drawLeftText(g2, label, bounds, color);
        g2.setFont(previous);

        if (index == hoverIndex) {
            FontMetrics metrics = g2.getFontMetrics();
            int baseline = PaintUtils.centeredBaseline(metrics, bounds.y, bounds.height);
            g2.setColor(color);
            g2.fillRect(bounds.x, baseline + 2, bounds.width, 1);
        }
    }

    /**
     * Pinta o separador entre dois itens e devolve a posição horizontal seguinte.
     */
    protected int paintSeparator(Graphics2D g2, int x) {
        int arm = UiTokens.scale(3);
        int cx = x + separatorGap + arm;
        int cy = getHeight() / 2;

        g2.setColor(separatorColor != null ? separatorColor : UiTokens.overlay(UiTokens.muted(), 0.6f));
        g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - arm / 2, cy - arm, cx + arm / 2, cy);
        g2.drawLine(cx + arm / 2, cy, cx - arm / 2, cy + arm);

        return x + separatorGap * 2 + arm * 2;
    }

    private int paintCollapseMark(Graphics2D g2, FontMetrics metrics, int x) {
        int width = metrics.stringWidth(COLLAPSE_MARK);
        PaintUtils.drawLeftText(g2, COLLAPSE_MARK, new Rectangle(x, 0, width, getHeight()), UiTokens.muted());
        return paintSeparator(g2, x + width);
    }
}
