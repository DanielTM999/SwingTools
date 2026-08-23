package dtm.stools.component.panels.toolbar;

import dtm.stools.component.events.EventType;
import dtm.stools.component.menu.popup.ActionPopupMenu;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.component.panels.divider.DividerPanel;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;
import dtm.stools.utils.PaintUtils;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Barra de ações com grupos, separadores e menu de excedente quando o espaço acaba.
 */
public class ToolBarPanel extends PanelEventListener {

    public static final String ACTION_TRIGGERED = "toolBarAction";

    private final List<JComponent> items = new ArrayList<>();
    private final Map<String, Runnable> overflowActions = new LinkedHashMap<>();
    private final OverflowButton overflowButton = new OverflowButton();

    private int itemGap = UiTokens.space(1);
    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private boolean paintSurface = true;

    private Color backgroundColor;
    private Color borderColor;

    public ToolBarPanel() {
        super(null, false);
        setOpaque(false);
        applyLayout();
        setPreferredSize(new Dimension(UiTokens.scale(420), UiTokens.scale(42)));
        overflowButton.onClick(this::showOverflowMenu);
        overflowButton.setVisible(false);
        add(overflowButton, FlexBoxLayout.FlexConstraints.of().fixedWidth(UiTokens.scale(30)));
    }

    /**
     * Adiciona um componente à barra.
     */
    public ToolBarPanel addItem(JComponent item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        items.add(item);
        remove(overflowButton);
        add(item);
        add(overflowButton, FlexBoxLayout.FlexConstraints.of().fixedWidth(UiTokens.scale(30)));
        revalidate();
        repaint();
        return this;
    }

    /**
     * Adiciona um botão de ação com texto e ícone opcionais.
     */
    public ToolBarPanel addAction(String text, Icon icon, Runnable action) {
        JButton button = new JButton(text != null ? text : "", icon);
        button.setFocusable(false);
        button.addActionListener(e -> {
            if (action != null) {
                action.run();
            }
            Map<String, Object> props = Map.of("action", String.valueOf(text));
            dispatchEvent(ACTION_TRIGGERED, this, text, props);
            dispatchEvent(EventType.ACTION, this, text, props);
        });
        return addItem(button);
    }

    /**
     * Adiciona um separador vertical entre grupos de ações.
     */
    public ToolBarPanel addSeparator() {
        DividerPanel divider = new DividerPanel("", DividerPanel.Orientation.VERTICAL);
        divider.setInset(UiTokens.space(2));
        divider.setPreferredSize(new Dimension(UiTokens.space(3), UiTokens.scale(24)));
        return addItem(divider);
    }

    /**
     * Adiciona um espaçador flexível que empurra os próximos itens para a direita.
     */
    public ToolBarPanel addSpacer() {
        JComponent spacer = new JComponent() {
        };
        spacer.setOpaque(false);
        items.add(spacer);
        remove(overflowButton);
        add(spacer, FlexBoxLayout.FlexConstraints.of().grow(1));
        add(overflowButton, FlexBoxLayout.FlexConstraints.of().fixedWidth(UiTokens.scale(30)));
        revalidate();
        repaint();
        return this;
    }

    /**
     * Registra uma ação exibida apenas no menu de excedente.
     */
    public ToolBarPanel addOverflowAction(String text, Runnable action) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text cannot be null or blank");
        }
        overflowActions.put(text, action != null ? action : () -> { });
        overflowButton.setVisible(true);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Remove todos os itens da barra.
     */
    public ToolBarPanel clearItems() {
        items.forEach(this::remove);
        items.clear();
        overflowActions.clear();
        overflowButton.setVisible(false);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o espaço entre os itens.
     */
    public ToolBarPanel setItemGap(int itemGap) {
        if (itemGap < 0) {
            throw new IllegalArgumentException("itemGap cannot be negative");
        }
        this.itemGap = itemGap;
        applyLayout();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Habilita a pintura da superfície de fundo da barra.
     */
    public ToolBarPanel setPaintSurface(boolean paintSurface) {
        this.paintSurface = paintSurface;
        repaint();
        return this;
    }

    /**
     * Define as cores de fundo e de borda.
     */
    public ToolBarPanel setColors(Color backgroundColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        repaint();
        return this;
    }

    /**
     * Define o raio de canto da barra.
     */
    public ToolBarPanel setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    private void applyLayout() {
        setLayout(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.ROW)
                .align(FlexBoxLayout.Align.CENTER)
                .justify(FlexBoxLayout.Justify.START)
                .gap(itemGap)
                .padding(UiTokens.space(1), UiTokens.space(2))
                .build());
    }

    private void showOverflowMenu() {
        if (overflowActions.isEmpty()) {
            return;
        }
        ActionPopupMenu menu = new ActionPopupMenu();
        overflowActions.forEach((text, action) -> menu.item(text, e -> {
            action.run();
            Map<String, Object> props = Map.of("action", text);
            dispatchEvent(ACTION_TRIGGERED, this, text, props);
            dispatchEvent(EventType.ACTION, this, text, props);
        }));
        menu.show(overflowButton, 0, overflowButton.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!paintSurface) {
            return;
        }
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            Color fill = backgroundColor != null ? backgroundColor : UiTokens.surface();
            PaintUtils.fillRoundRect(g2, bounds, arc, isEnabled() ? fill : UiTokens.disabled(fill));
            PaintUtils.drawRoundRect(g2, bounds, arc,
                    borderColor != null ? borderColor : UiTokens.border(), UiTokens.stroke());
        } finally {
            g2.dispose();
        }
    }

    /**
     * Botão de reticências que abre o menu de ações excedentes.
     */
    private static final class OverflowButton extends JComponent {

        private Runnable action = () -> { };
        private boolean hover;

        private OverflowButton() {
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(UiTokens.scale(30), UiTokens.scale(26)));
            addMouseListener(new MouseAdapter() {
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

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                        action.run();
                    }
                }
            });
        }

        private void onClick(Runnable action) {
            this.action = action != null ? action : () -> { };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                if (hover) {
                    PaintUtils.fillRoundRect(g2, new Rectangle(0, 0, getWidth(), getHeight()),
                            UiTokens.radius(UiTokens.Radius.SM), UiTokens.hover(UiTokens.surfaceAlt()));
                }
                g2.setColor(hover ? UiTokens.primary() : UiTokens.muted());
                g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cy = getHeight() / 2;
                int cx = getWidth() / 2;
                int gap = UiTokens.scale(5);
                for (int i = -1; i <= 1; i++) {
                    g2.fillOval(cx + i * gap - 1, cy - 1, 3, 3);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
