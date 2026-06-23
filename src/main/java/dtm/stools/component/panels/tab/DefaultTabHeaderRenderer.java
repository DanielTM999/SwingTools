package dtm.stools.component.panels.tab;

import dtm.stools.component.icon.TintedIconLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class DefaultTabHeaderRenderer implements TabHeaderFactory{

    @Override
    public JComponent createHeader(TabbedPanel tabs, TabEntry entry, boolean selected) {
        TabStyle style = tabs.getTabStyle();

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, style.getTabHeaderGap(), 0));

        Insets padding = style.getTabHeaderPadding();
        header.setBorder(BorderFactory.createEmptyBorder(
                padding.top,
                padding.left,
                padding.bottom,
                padding.right
        ));

        applyHeaderStyle(tabs, header, selected);

        JLabel iconLabel = createIconLabel(tabs, entry, selected);
        if (iconLabel != null) {
            header.add(iconLabel);
        }

        header.add(createPinButton(tabs, entry));

        JLabel title = createTitleLabel(tabs, entry, selected);
        if (!title.getText().isEmpty()) {
            header.add(title);
        }

        if (entry.getBadge() != null && !entry.getBadge().isBlank()) {
            header.add(createBadgeLabel(tabs, entry));
        }

        HoverCloseButton closeButton = null;

        if (tabs.isCloseButtonsVisible() && entry.isClosable()) {
            closeButton = createCloseButton(tabs, entry);
            closeButton.setVisuallyVisible(selected);
            header.add(closeButton);
        }

        if (closeButton != null) {
            installCloseButtonHoverBehavior(header, closeButton, selected);
        }

        return header;
    }

    protected void applyHeaderStyle(TabbedPanel tabs, JComponent header, boolean selected) {
        header.setOpaque(false);
        header.setCursor(tabs.isTabDragEnabled()
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    protected JLabel createIconLabel(TabbedPanel tabs, TabEntry entry, boolean selected) {
        Icon icon = resolveIcon(tabs, entry, selected);
        return icon == null ? null : new JLabel(icon);
    }

    protected Icon resolveIcon(TabbedPanel tabs, TabEntry entry, boolean selected) {
        TabStyle style = tabs.getTabStyle();

        if (selected && entry.getSelectedIcon() != null) {
            return entry.getSelectedIcon();
        }

        if (entry.getIcon() != null) {
            return entry.getIcon();
        }

        if (selected && style.getDefaultSelectedTabIcon() != null) {
            return style.getDefaultSelectedTabIcon();
        }

        return style.getDefaultTabIcon();
    }

    protected JLabel createTitleLabel(TabbedPanel tabs, TabEntry entry, boolean selected) {
        TabStyle style = tabs.getTabStyle();

        String text = entry.isPinned() && !tabs.isPinnedTabsShowTitle()
                ? ""
                : entry.getTitle();

        if (entry.isDirty()) {
            text = "* " + text;
        }

        Color diagnostic = entry.getDiagnosticColor();

        JLabel title = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (diagnostic != null && !getText().isEmpty()) {
                    paintDiagnosticUnderline((Graphics2D) g, this, diagnostic);
                }
            }
        };

        Color foreground;

        if (selected && entry.getSelectedTitleForeground() != null) {
            foreground = entry.getSelectedTitleForeground();
        } else if (entry.getTitleForeground() != null) {
            foreground = entry.getTitleForeground();
        } else if (selected && style.getSelectedTabTitleForeground() != null) {
            foreground = style.getSelectedTabTitleForeground();
        } else {
            foreground = style.getTabTitleForeground();
        }

        if (foreground != null) {
            title.setForeground(foreground);
        }

        Font font = selected && style.getSelectedTabTitleFont() != null
                ? style.getSelectedTabTitleFont()
                : style.getTabTitleFont();

        if (font != null) {
            title.setFont(font);
        }

        return title;
    }

    protected JComponent createBadgeLabel(TabbedPanel tabs, TabEntry entry) {
        TabStyle style = tabs.getTabStyle();

        JLabel badge = new JLabel(entry.getBadge()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (style.getTabBadgeBackground() != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(style.getTabBadgeBackground());
                    g2.fillRoundRect(
                            0,
                            0,
                            getWidth(),
                            getHeight(),
                            style.getTabBadgeArc(),
                            style.getTabBadgeArc()
                    );
                    g2.dispose();
                }

                super.paintComponent(g);
            }
        };

        badge.setOpaque(style.getTabBadgeBackground() == null);

        if (style.getTabBadgeBackground() != null) {
            badge.setBackground(style.getTabBadgeBackground());
        }

        if (style.getTabBadgeForeground() != null) {
            badge.setForeground(style.getTabBadgeForeground());
        }

        if (style.getTabBadgeFont() != null) {
            badge.setFont(style.getTabBadgeFont());
        } else {
            badge.setFont(badge.getFont().deriveFont(
                    Font.BOLD,
                    Math.max(9f, badge.getFont().getSize2D() - 2f)
            ));
        }

        badge.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));

        return badge;
    }

    protected void paintDiagnosticUnderline(Graphics2D g, JLabel label, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1f));

        FontMetrics metrics = label.getFontMetrics(label.getFont());
        Insets insets = label.getInsets();

        int textWidth = Math.min(
                metrics.stringWidth(label.getText()),
                label.getWidth() - insets.left - insets.right
        );
        if (textWidth <= 0) {
            g2.dispose();
            return;
        }

        int amplitude = 1;
        int step = 2;
        int startX = insets.left;
        int endX = startX + textWidth;
        int baseY = label.getHeight() - insets.bottom - amplitude - 1;

        GeneralPath wave = new GeneralPath();
        wave.moveTo(startX, baseY);

        boolean up = true;
        for (int x = startX; x < endX; x += step) {
            int nextX = Math.min(x + step, endX);
            wave.lineTo(nextX, up ? baseY - amplitude : baseY + amplitude);
            up = !up;
        }

        g2.draw(wave);
        g2.dispose();
    }

    protected JButton createPinButton(TabbedPanel tabs, TabEntry entry) {
        TabStyle style = tabs.getTabStyle();

        JButton pin = new JButton();

        Icon icon = loadPinnedIcon(style, entry.isPinned());
        if (icon != null) {
            pin.setIcon(icon);
        }

        pin.setPreferredSize(new Dimension(16, 16));
        pin.setMinimumSize(new Dimension(16, 16));
        pin.setMaximumSize(new Dimension(16, 16));

        pin.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        pin.setMargin(new Insets(0, 0, 0, 0));
        pin.setFocusable(false);
        pin.setContentAreaFilled(false);
        pin.setBorderPainted(false);
        pin.setOpaque(false);
        pin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pin.setToolTipText(entry.isPinned() ? "Desafixar aba" : "Fixar aba");

        pin.addActionListener(e -> {
            tabs.togglePinned(entry.getKey());
            pin.repaint();
        });

        return pin;
    }

    protected HoverCloseButton createCloseButton(TabbedPanel tabs, TabEntry entry) {
        TabStyle style = tabs.getTabStyle();

        HoverCloseButton close = new HoverCloseButton();

        Icon icon = loadCloseIcon(style);
        if (icon != null) {
            close.setIcon(icon);
        } else {
            close.setText(style.getCloseButtonText());
            close.setFont(close.getFont().deriveFont(Font.PLAIN, 10f));
        }

        close.setPreferredSize(new Dimension(14, 14));
        close.setMinimumSize(new Dimension(14, 14));
        close.setMaximumSize(new Dimension(14, 14));

        close.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        close.setMargin(new Insets(0, 0, 0, 0));
        close.setFocusable(false);
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setOpaque(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText(style.getCloseButtonTooltip());

        close.addActionListener(e -> {
            if (close.isVisuallyVisible()) {
                tabs.closeTab(entry.getKey());
            }
        });

        return close;
    }

    protected void installCloseButtonHoverBehavior(JComponent header, HoverCloseButton closeButton, boolean selected) {
        closeButton.setVisuallyVisible(selected);

        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setVisuallyVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (selected) {
                    return;
                }

                Point point = SwingUtilities.convertPoint(
                        e.getComponent(),
                        e.getPoint(),
                        header
                );

                if (header.contains(point)) {
                    return;
                }

                closeButton.setVisuallyVisible(false);
            }
        };

        addMouseListenerRecursive(header, hoverListener);
    }

    protected void addMouseListenerRecursive(Component component, MouseListener listener) {
        component.addMouseListener(listener);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                addMouseListenerRecursive(child, listener);
            }
        }
    }

    protected Icon loadCloseIcon(TabStyle style) {
        String resource = style.getCloseButtonIconResource();

        if (resource == null || resource.isEmpty()) {
            return null;
        }

        Color tint = style.getCloseButtonIconColor();

        if (tint == null) {
            tint = UIManager.getColor("Button.foreground");

            if (tint == null) {
                tint = UIManager.getColor("Label.foreground");
            }
        }

        return TintedIconLoader.load(resource, style.getCloseButtonIconSize(), tint);
    }

    protected Icon loadPinnedIcon(TabStyle style, boolean pinned) {
        String resource = pinned
                ? style.getPinnedButtonIconResource()
                : style.getUnpinnedButtonIconResource();

        if (resource == null || resource.isEmpty()) {
            return null;
        }

        Color tint = style.getPinnedButtonIconColor();

        if (tint == null) {
            tint = UIManager.getColor("Button.foreground");

            if (tint == null) {
                tint = UIManager.getColor("Label.foreground");
            }
        }

        return TintedIconLoader.load(resource, style.getPinnedButtonIconSize(), tint);
    }

    protected static class HoverCloseButton extends JButton {

        private boolean visuallyVisible = true;
        private boolean hovered = false;

        public HoverCloseButton() {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        public boolean isVisuallyVisible() {
            return visuallyVisible;
        }

        public void setVisuallyVisible(boolean visuallyVisible) {
            this.visuallyVisible = visuallyVisible;
            setEnabled(visuallyVisible);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!visuallyVisible) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (hovered) {
                Color bg = UIManager.getColor("Button.hoverBackground");

                if (bg == null) {
                    bg = UIManager.getColor("Component.focusColor");
                }

                if (bg == null) {
                    bg = new Color(255, 255, 255, 35);
                }

                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(bg);
                g2.fillOval(x, y, size, size);
            }

            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            if (visuallyVisible) {
                super.paintBorder(g);
            }
        }
    }
}