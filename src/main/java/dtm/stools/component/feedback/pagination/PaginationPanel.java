package dtm.stools.component.feedback.pagination;

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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controle de paginação com botões de página, elipse para faixas ocultas e navegação anterior e próxima.
 */
public class PaginationPanel extends PanelEventListener {

    public static final String PAGE_CHANGED = "pageChanged";

    private static final int ELLIPSIS = -1;
    private static final int PREVIOUS = -2;
    private static final int NEXT = -3;

    private final List<Integer> slots = new ArrayList<>();
    private final List<Rectangle> hitAreas = new ArrayList<>();

    private int pageCount = 1;
    private int currentPage;
    private int siblingCount = 1;

    private int hoverSlot = -100;
    private int buttonSize = 30;
    private int buttonGap = UiTokens.space(1);
    private int arc = UiTokens.radius(UiTokens.Radius.SM);

    private Color activeColor;
    private Color textColor;

    public PaginationPanel() {
        this(1);
    }

    public PaginationPanel(int pageCount) {
        super(null, false);
        if (pageCount <= 0) {
            throw new IllegalArgumentException("pageCount must be greater than zero");
        }
        this.pageCount = pageCount;

        setOpaque(false);
        setFont(UiTokens.fontSmall());
        installListeners();
        updatePreferredSize();
    }

    /**
     * Quantidade total de páginas.
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * Define a quantidade total de páginas.
     */
    public PaginationPanel setPageCount(int pageCount) {
        if (pageCount <= 0) {
            throw new IllegalArgumentException("pageCount must be greater than zero");
        }
        this.pageCount = pageCount;
        this.currentPage = Math.min(currentPage, pageCount - 1);
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Índice da página corrente, começando em zero.
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Define a página corrente disparando eventos.
     */
    public PaginationPanel setCurrentPage(int currentPage) {
        return setCurrentPage(currentPage, true);
    }

    /**
     * Define a página corrente, opcionalmente sem disparar eventos.
     */
    public PaginationPanel setCurrentPage(int currentPage, boolean fireEvent) {
        int clamped = Math.max(0, Math.min(pageCount - 1, currentPage));
        if (clamped == this.currentPage) {
            return this;
        }

        int oldValue = this.currentPage;
        this.currentPage = clamped;
        repaint();

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", clamped, "pageCount", pageCount);
            dispatchEvent(PAGE_CHANGED, this, clamped, props);
            dispatchEvent(EventType.PAGE, this, clamped, props);
            dispatchEvent(EventType.CHANGE, this, clamped, props);
        }
        return this;
    }

    /**
     * Avança uma página.
     */
    public PaginationPanel nextPage() {
        return setCurrentPage(currentPage + 1);
    }

    /**
     * Retrocede uma página.
     */
    public PaginationPanel previousPage() {
        return setCurrentPage(currentPage - 1);
    }

    /**
     * Define quantas páginas vizinhas são exibidas ao redor da corrente.
     */
    public PaginationPanel setSiblingCount(int siblingCount) {
        if (siblingCount < 0) {
            throw new IllegalArgumentException("siblingCount cannot be negative");
        }
        this.siblingCount = siblingCount;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o tamanho de cada botão.
     */
    public PaginationPanel setButtonSize(int buttonSize) {
        if (buttonSize <= 0) {
            throw new IllegalArgumentException("buttonSize must be greater than zero");
        }
        this.buttonSize = buttonSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define as cores da página ativa e do texto.
     */
    public PaginationPanel setColors(Color activeColor, Color textColor) {
        this.activeColor = activeColor;
        this.textColor = textColor;
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
                int slot = slotAt(e.getPoint());
                switch (slot) {
                    case PREVIOUS -> previousPage();
                    case NEXT -> nextPage();
                    case ELLIPSIS -> {
                    }
                    default -> {
                        if (slot >= 0) {
                            setCurrentPage(slot);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverSlot = -100;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int slot = isEnabled() ? slotAt(e.getPoint()) : -100;
                boolean clickable = slot >= 0 || slot == PREVIOUS || slot == NEXT;
                setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (slot != hoverSlot) {
                    hoverSlot = clickable ? slot : -100;
                    repaint();
                }
            }
        });
    }

    private void updatePreferredSize() {
        int visible = Math.min(pageCount, siblingCount * 2 + 5) + 2;
        int width = visible * (buttonSize + buttonGap);
        setPreferredSize(new Dimension(width, buttonSize));
        setMinimumSize(new Dimension(buttonSize * 3, buttonSize));
        revalidate();
    }

    private int slotAt(Point point) {
        for (int i = 0; i < hitAreas.size(); i++) {
            if (hitAreas.get(i).contains(point)) {
                return slots.get(i);
            }
        }
        return -100;
    }

    private void rebuildSlots() {
        slots.clear();
        slots.add(PREVIOUS);

        int total = siblingCount * 2 + 5;
        if (pageCount <= total) {
            for (int i = 0; i < pageCount; i++) {
                slots.add(i);
            }
        } else {
            int left = Math.max(1, currentPage - siblingCount);
            int right = Math.min(pageCount - 2, currentPage + siblingCount);

            slots.add(0);
            if (left > 1) {
                slots.add(ELLIPSIS);
            }
            for (int i = left; i <= right; i++) {
                slots.add(i);
            }
            if (right < pageCount - 2) {
                slots.add(ELLIPSIS);
            }
            slots.add(pageCount - 1);
        }

        slots.add(NEXT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            rebuildSlots();
            hitAreas.clear();

            int x = 0;
            for (int slot : slots) {
                Rectangle bounds = new Rectangle(x, 0, buttonSize, buttonSize);
                hitAreas.add(bounds);
                paintSlot(g2, bounds, slot);
                x += buttonSize + buttonGap;
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta um botão de página, elipse ou navegação.
     */
    protected void paintSlot(Graphics2D g2, Rectangle bounds, int slot) {
        boolean active = slot == currentPage;
        boolean hovered = slot == hoverSlot;
        boolean navigationDisabled = (slot == PREVIOUS && currentPage == 0)
                || (slot == NEXT && currentPage == pageCount - 1);

        if (active) {
            PaintUtils.fillRoundRect(g2, bounds, arc, resolveActive());
        } else if (hovered && !navigationDisabled) {
            PaintUtils.fillRoundRect(g2, bounds, arc, UiTokens.overlay(UiTokens.muted(), 0.14f));
        }

        Color color = active
                ? UiTokens.onColor(resolveActive())
                : (textColor != null ? textColor : UiTokens.foreground());
        if (!isEnabled() || navigationDisabled) {
            color = UiTokens.disabled(color);
        }

        if (slot == PREVIOUS || slot == NEXT) {
            paintChevron(g2, bounds, color, slot == NEXT ? 1 : -1);
            return;
        }

        g2.setFont(UiTokens.fontSmall().deriveFont(active ? Font.BOLD : Font.PLAIN));
        PaintUtils.drawCenteredText(g2, slot == ELLIPSIS ? "…" : String.valueOf(slot + 1), bounds,
                slot == ELLIPSIS ? UiTokens.muted() : color);
    }

    private void paintChevron(Graphics2D g2, Rectangle bounds, Color color, int direction) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int cx = bounds.x + bounds.width / 2;
        int cy = bounds.y + bounds.height / 2;
        int arm = UiTokens.scale(4);

        g2.drawLine(cx - arm * direction / 2, cy - arm, cx + arm * direction / 2, cy);
        g2.drawLine(cx + arm * direction / 2, cy, cx - arm * direction / 2, cy + arm);
    }

    private Color resolveActive() {
        return activeColor != null ? activeColor : UiTokens.primary();
    }
}
