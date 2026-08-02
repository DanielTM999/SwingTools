package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Popup visual de layouts de encaixe, semelhante ao Snap Layouts do Windows. */
public class WindowSnapLayoutPopup extends JPopupMenu {
    protected final WindowDesktopPanel desktop;
    protected final WindowPanel window;
    protected final JPanel layoutsPanel;
    protected final List<SnapZoneButton> zoneButtons = new ArrayList<>();

    public WindowSnapLayoutPopup(WindowDesktopPanel desktop, WindowPanel window) {
        this.desktop = desktop;
        this.window = window;
        layoutsPanel = createLayoutsPanel();
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(resolveBorderColor()),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        setOpaque(true);
        setBackground(resolveBackground());
        buildLayouts();
        add(layoutsPanel);
    }

    protected JPanel createLayoutsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 8, 8));
        panel.setOpaque(false);
        return panel;
    }

    protected List<List<WindowSnap>> createLayouts() {
        return List.of(
                List.of(WindowSnap.LEFT, WindowSnap.RIGHT),
                List.of(WindowSnap.TWO_THIRDS_LEFT, WindowSnap.THIRD_RIGHT),
                List.of(WindowSnap.THIRD_LEFT, WindowSnap.THIRD_CENTER, WindowSnap.THIRD_RIGHT),
                List.of(WindowSnap.LEFT, WindowSnap.TOP_RIGHT, WindowSnap.BOTTOM_RIGHT),
                List.of(WindowSnap.TOP_LEFT, WindowSnap.TOP_RIGHT,
                        WindowSnap.BOTTOM_LEFT, WindowSnap.BOTTOM_RIGHT),
                List.of(WindowSnap.THIRD_LEFT, WindowSnap.TWO_THIRDS_RIGHT)
        );
    }

    protected void buildLayouts() {
        layoutsPanel.removeAll();
        zoneButtons.clear();
        for (List<WindowSnap> layout : createLayouts()) {
            layoutsPanel.add(createLayoutPreview(layout));
        }
    }

    protected JComponent createLayoutPreview(List<WindowSnap> layout) {
        return new LayoutPreview(layout);
    }

    protected SnapZoneButton createZoneButton(WindowSnap snap) {
        return new SnapZoneButton(snap);
    }

    protected SnapZoneButton createZoneButton(List<WindowSnap> layout, WindowSnap snap) {
        return new SnapZoneButton(layout, snap);
    }

    protected Rectangle resolveZoneBounds(WindowSnap snap, Rectangle area) {
        int halfWidth = area.width / 2;
        int halfHeight = area.height / 2;
        int thirdWidth = area.width / 3;
        return switch (snap) {
            case LEFT -> new Rectangle(area.x, area.y, halfWidth, area.height);
            case RIGHT -> new Rectangle(area.x + halfWidth, area.y,
                    area.width - halfWidth, area.height);
            case THIRD_LEFT -> new Rectangle(area.x, area.y, thirdWidth, area.height);
            case THIRD_CENTER -> new Rectangle(area.x + thirdWidth, area.y, thirdWidth, area.height);
            case THIRD_RIGHT -> new Rectangle(area.x + thirdWidth * 2, area.y,
                    area.width - thirdWidth * 2, area.height);
            case TWO_THIRDS_LEFT -> new Rectangle(area.x, area.y, thirdWidth * 2, area.height);
            case TWO_THIRDS_RIGHT -> new Rectangle(area.x + thirdWidth, area.y,
                    area.width - thirdWidth, area.height);
            case TOP_LEFT -> new Rectangle(area.x, area.y, halfWidth, halfHeight);
            case TOP_RIGHT -> new Rectangle(area.x + halfWidth, area.y,
                    area.width - halfWidth, halfHeight);
            case BOTTOM_LEFT -> new Rectangle(area.x, area.y + halfHeight,
                    halfWidth, area.height - halfHeight);
            case BOTTOM_RIGHT -> new Rectangle(area.x + halfWidth, area.y + halfHeight,
                    area.width - halfWidth, area.height - halfHeight);
            case NONE -> new Rectangle(area);
        };
    }

    protected String resolveZoneTooltip(WindowSnap snap) {
        return switch (snap) {
            case LEFT -> "Metade esquerda";
            case RIGHT -> "Metade direita";
            case THIRD_LEFT -> "Terco esquerdo";
            case THIRD_CENTER -> "Terco central";
            case THIRD_RIGHT -> "Terco direito";
            case TWO_THIRDS_LEFT -> "Dois tercos a esquerda";
            case TWO_THIRDS_RIGHT -> "Dois tercos a direita";
            case TOP_LEFT -> "Quadrante superior esquerdo";
            case TOP_RIGHT -> "Quadrante superior direito";
            case BOTTOM_LEFT -> "Quadrante inferior esquerdo";
            case BOTTOM_RIGHT -> "Quadrante inferior direito";
            case NONE -> "Restaurar";
        };
    }

    protected Color resolveBackground() {
        Color color = UIManager.getColor("PopupMenu.background");
        return color == null ? new Color(0x292B2F) : color;
    }

    protected Color resolveBorderColor() {
        Color color = UIManager.getColor("PopupMenu.borderColor");
        return color == null ? new Color(0x55585D) : color;
    }

    protected Color resolveZoneColor() {
        Color color = UIManager.getColor("Button.background");
        return color == null ? new Color(0x46494D) : color;
    }

    protected Color resolveZoneBorderColor() {
        Color color = UIManager.getColor("Component.borderColor");
        return color == null ? new Color(0x777B80) : color;
    }

    protected Color resolveZoneHoverColor() {
        Color color = UIManager.getColor("Component.accentColor");
        return color == null ? new Color(0x4BA8E8) : color;
    }

    public WindowDesktopPanel getDesktop() { return desktop; }
    public WindowPanel getWindow() { return window; }
    public List<SnapZoneButton> getZoneButtons() { return List.copyOf(zoneButtons); }

    public List<WindowSnap> findLayoutFor(WindowSnap snap) {
        return createLayouts().stream().filter(layout -> layout.contains(snap))
                .findFirst().map(List::copyOf).orElse(List.of(snap));
    }

    protected class LayoutPreview extends JPanel {
        protected final List<SnapZoneButton> buttons = new ArrayList<>();

        protected LayoutPreview(List<WindowSnap> layout) {
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(126, 58));
            setToolTipText("Escolher layout de encaixe");
            for (WindowSnap snap : layout) {
                SnapZoneButton button = createZoneButton(layout, snap);
                buttons.add(button);
                zoneButtons.add(button);
                add(button);
            }
        }

        @Override public void doLayout() {
            Rectangle area = new Rectangle(1, 1,
                    Math.max(1, getWidth() - 2), Math.max(1, getHeight() - 2));
            for (SnapZoneButton button : buttons) {
                Rectangle bounds = resolveZoneBounds(button.getSnap(), area);
                bounds.grow(-2, -2);
                button.setBounds(bounds);
            }
        }
    }

    public class SnapZoneButton extends JButton {
        private final WindowSnap snap;
        private final List<WindowSnap> layout;

        protected SnapZoneButton(WindowSnap snap) {
            this(List.of(snap), snap);
        }

        protected SnapZoneButton(List<WindowSnap> layout, WindowSnap snap) {
            this.snap = snap;
            this.layout = List.copyOf(layout);
            setFocusable(true);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(resolveZoneTooltip(snap));
            getAccessibleContext().setAccessibleName(resolveZoneTooltip(snap));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) {
                    desktop.previewSnapLayout(window, SnapZoneButton.this.layout, snap);
                }

                @Override public void mouseExited(MouseEvent event) {
                    desktop.clearSnapLayoutPreview();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) {
                    desktop.previewSnapLayout(window, SnapZoneButton.this.layout, snap);
                }

                @Override public void focusLost(FocusEvent event) {
                    desktop.clearSnapLayoutPreview();
                }
            });
            addActionListener(event -> desktop.applySnapLayout(window, layout, snap));
        }

        public WindowSnap getSnap() { return snap; }
        public List<WindowSnap> getSnapLayout() { return layout; }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean highlighted = getModel().isRollover() || getModel().isPressed() || isFocusOwner();
                g.setColor(highlighted ? resolveZoneHoverColor() : resolveZoneColor());
                g.fillRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 5, 5);
                g.setColor(highlighted ? resolveZoneHoverColor().brighter() : resolveZoneBorderColor());
                g.drawRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 5, 5);
            } finally {
                g.dispose();
            }
        }
    }
}
