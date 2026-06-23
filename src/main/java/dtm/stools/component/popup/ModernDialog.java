package dtm.stools.component.popup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public final class ModernDialog {

    public enum Type { SUCCESS, ERROR, INFO, QUESTION }

    public static ModernDialogBuilder builder() {
        return new ModernDialogBuilder();
    }

    public static ModernDialogBuilder modernDialogBuilder() {
        return builder();
    }

    private static Color lfBackground() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new Color(0x22252B);
    }

    private static Color lfForeground() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.WHITE;
    }

    private static boolean isDark(Color c) {
        double lum = 0.2126 * (c.getRed() / 255.0)
                + 0.7152 * (c.getGreen() / 255.0)
                + 0.0722 * (c.getBlue() / 255.0);
        return lum < 0.35;
    }

    private static Color deriveHover(Color base, float factor) {
        if (isDark(base)) {
            return new Color(
                    Math.min(255, (int) (base.getRed() + 255 * factor)),
                    Math.min(255, (int) (base.getGreen() + 255 * factor)),
                    Math.min(255, (int) (base.getBlue() + 255 * factor))
            );
        } else {
            return new Color(
                    Math.max(0, (int) (base.getRed() - 255 * factor)),
                    Math.max(0, (int) (base.getGreen() - 255 * factor)),
                    Math.max(0, (int) (base.getBlue() - 255 * factor))
            );
        }
    }

    private static Color deriveSecondaryBg(Color panelBg) {
        return deriveHover(panelBg, 0.10f);
    }

    private static Color defaultAccentFor(Type type) {
        return switch (type) {
            case SUCCESS -> new Color(0x22C55E);
            case ERROR -> new Color(0xEF4444);
            case QUESTION -> new Color(0xF0C040);
            default -> new Color(0x3B82F6);
        };
    }

    private static Color resolveAccent(Color global, Type type) {
        if (global != null) return global;
        Color lf = UIManager.getColor("Button.select");
        if (lf != null) return lf;
        return defaultAccentFor(type);
    }

    static class Btn {
        String text;
        int value;
        boolean isPrimary;
        Color explicitColor;
        Color explicitFg;

        Btn(String text, int value, boolean isPrimary, Color explicitColor, Color explicitFg) {
            this.text = text;
            this.value = value;
            this.isPrimary = isPrimary;
            this.explicitColor = explicitColor;
            this.explicitFg = explicitFg;
        }
    }

    public static class ModernDialogBuilder {

        private String title = "";
        private String message = "";
        private Type type = Type.INFO;
        private Color accentColor = null;
        private Component parent = null;
        private boolean draggable = false;
        private boolean enterConfirms = true;
        private boolean showIcon = true;

        private final List<Btn> buttons = new ArrayList<>();

        public ModernDialogBuilder title(String t) {
            this.title = t;
            return this;
        }

        public ModernDialogBuilder message(String m) {
            this.message = m;
            return this;
        }

        public ModernDialogBuilder type(Type t) {
            this.type = t;
            return this;
        }

        public ModernDialogBuilder accentColor(Color c) {
            this.accentColor = c;
            return this;
        }

        public ModernDialogBuilder parent(Component parent) {
            this.parent = parent;
            return this;
        }

        public ModernDialogBuilder enterConfirms(boolean enterConfirms) {
            this.enterConfirms = enterConfirms;
            return this;
        }

        public ModernDialogBuilder showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public ModernDialogBuilder parentComponent(Component parent) {
            return parent(parent);
        }

        public ModernDialogBuilder option(String text, int value) {
            buttons.add(new Btn(text, value, buttons.isEmpty(), null, null));
            return this;
        }

        public ModernDialogBuilder option(String text, int value, Color color) {
            buttons.add(new Btn(text, value, buttons.isEmpty(), color, null));
            return this;
        }

        public ModernDialogBuilder option(String text, int value, Color color, Color fg) {
            buttons.add(new Btn(text, value, buttons.isEmpty(), color, fg));
            return this;
        }

        public int show() {
            return show(parent);
        }

        public int show(Component parent) {
            if (buttons.isEmpty()) {
                buttons.add(new Btn("OK", JOptionPane.OK_OPTION, true, null, null));
            }

            Color panelBg = lfBackground();
            Color fgPrimary = lfForeground();
            Color fgSecondary = new Color(
                    fgPrimary.getRed(),
                    fgPrimary.getGreen(),
                    fgPrimary.getBlue(),
                    Math.max(0, (int) (fgPrimary.getAlpha() * 0.55f))
            );
            Color accent = resolveAccent(accentColor, type);
            Color secondaryBg = deriveSecondaryBg(panelBg);
            boolean darkTheme = isDark(panelBg);
            Color borderColor = darkTheme
                    ? new Color(255, 255, 255, 22)
                    : new Color(0, 0, 0, 22);

            Window owner;

            if (parent instanceof Window w) {
                owner = w;
            } else {
                owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
            }

            JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);

            JPanel root = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(panelBg);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                    g2.dispose();
                }
            };
            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

            JPanel typeChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            typeChip.setOpaque(false);

            JLabel dot = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(accent);
                    g2.fillOval(0, 2, 7, 7);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(7, 11);
                }
            };

            JLabel typeLabel = new JLabel(
                    type.name().charAt(0) + type.name().substring(1).toLowerCase()
            );
            typeLabel.setForeground(fgSecondary);
            typeLabel.setFont(font(11, Font.PLAIN));

            typeChip.add(dot);
            typeChip.add(typeLabel);

            header.add(typeChip, BorderLayout.WEST);


            if (draggable) {
                int[] dragOffset = new int[2];
                header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                header.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        dragOffset[0] = e.getX();
                        dragOffset[1] = e.getY();
                    }
                });
                header.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        dialog.setLocation(
                                dialog.getX() + e.getX() - dragOffset[0],
                                dialog.getY() + e.getY() - dragOffset[1]
                        );
                    }
                });
            }

            header.add(buildCloseBtn(dialog, fgSecondary, darkTheme), BorderLayout.EAST);

            JPanel body = new JPanel(new BorderLayout(14, 0));
            body.setOpaque(false);
            body.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
            if (showIcon) {
                body.add(new JLabel(buildIcon(type, accent)), BorderLayout.WEST);
            }

            JPanel textBlock = new JPanel();
            textBlock.setOpaque(false);
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

            JLabel titleLbl = new JLabel("<html>" + title + "</html>");
            titleLbl.setForeground(fgPrimary);
            titleLbl.setFont(font(14, Font.BOLD));
            titleLbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));

            JLabel msgLbl = new JLabel("<html><div style='width:240px'>" + message + "</div></html>");
            msgLbl.setForeground(fgSecondary);
            msgLbl.setFont(font(12, Font.PLAIN));

            textBlock.add(titleLbl);
            textBlock.add(msgLbl);
            body.add(textBlock, BorderLayout.CENTER);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnRow.setOpaque(false);

            int[] result = {JOptionPane.CLOSED_OPTION};

            JButton primaryButton = null;

            for (Btn b : buttons) {
                Color btnBg = b.explicitColor != null
                        ? b.explicitColor
                        : b.isPrimary ? accent : secondaryBg;

                Color btnHover = deriveHover(btnBg, 0.12f);
                Color btnFg = b.explicitFg != null ? b.explicitFg : isDark(btnBg) ? Color.WHITE : new Color(0x1a1a1a);

                JButton built = buildButton(b, btnBg, btnHover, btnFg, dialog, result);
                if (b.isPrimary && primaryButton == null) {
                    primaryButton = built;
                }
                btnRow.add(built);
            }

            root.add(header, BorderLayout.NORTH);
            root.add(body, BorderLayout.CENTER);
            root.add(btnRow, BorderLayout.SOUTH);

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setShape(new RoundRectangle2D.Float(
                    0, 0, dialog.getWidth(), dialog.getHeight(), 14, 14
            ));

            if (parent != null) dialog.setLocationRelativeTo(parent);
            else dialog.setLocationRelativeTo(null);

            if (enterConfirms && primaryButton != null) {
                dialog.getRootPane().setDefaultButton(primaryButton);
            }

            final JButton focusTarget = primaryButton;
            if (focusTarget != null) {
                SwingUtilities.invokeLater(focusTarget::requestFocusInWindow);
            }

            dialog.setVisible(true);
            return result[0];
        }

        private Font font(float size, int style) {
            Font base = UIManager.getFont("Label.font");
            if (base == null) base = new Font("Segoe UI", style, (int) size);
            return base.deriveFont(style, size);
        }

        public ModernDialogBuilder draggable(boolean d) { this.draggable = d; return this; }

        private JButton buildButton(Btn b, Color normalBg, Color hoverBg, Color fg, JDialog dialog, int[] result) {
            JButton btn = new JButton(b.text) {
                private boolean hover = false;

                {
                    addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            hover = true;
                            repaint();
                        }

                        public void mouseExited(MouseEvent e) {
                            hover = false;
                            repaint();
                        }
                    });
                    addFocusListener(new FocusAdapter() {
                        public void focusGained(FocusEvent e) {
                            repaint();
                        }

                        public void focusLost(FocusEvent e) {
                            repaint();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(hover ? hoverBg : normalBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    if (isFocusOwner()) {
                        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 170));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            btn.setForeground(fg);
            btn.setFont(font(12, Font.PLAIN));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                result[0] = b.value;
                dialog.dispose();
            });

            return btn;
        }

        private JButton buildCloseBtn(JDialog dialog, Color fg, boolean darkTheme) {
            JButton btn = new JButton("×") {
                private boolean hover = false;

                {
                    addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) {
                            hover = true;
                            repaint();
                        }

                        public void mouseExited(MouseEvent e) {
                            hover = false;
                            repaint();
                        }
                    });
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color hc = darkTheme ? new Color(255, 255, 255, 22) : new Color(0, 0, 0, 22);
                    g2.setColor(hover ? hc : new Color(0, 0, 0, 0));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };

            btn.setForeground(fg);
            btn.setFont(font(16, Font.PLAIN));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFocusable(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> dialog.dispose());

            return btn;
        }

        private static Icon buildIcon(Type type, Color accent) {
            return new Icon() {
                @Override
                public int getIconWidth() {
                    return 44;
                }

                @Override
                public int getIconHeight() {
                    return 44;
                }

                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                    g2.fillRoundRect(x, y, 44, 44, 12, 12);

                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(x, y, 43, 43, 12, 12);

                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = x + 22, cy = y + 22;

                    switch (type) {
                        case SUCCESS:
                            g2.drawOval(cx - 9, cy - 9, 18, 18);
                            g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                            g2.drawLine(cx - 1, cy + 4, cx + 5, cy - 4);
                            break;
                        case ERROR:
                            g2.drawOval(cx - 9, cy - 9, 18, 18);
                            g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                            g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                            break;
                        case QUESTION:
                            g2.drawOval(cx - 9, cy - 9, 18, 18);
                            g2.drawArc(cx - 4, cy - 7, 8, 7, 0, 180);
                            g2.drawLine(cx, cy, cx, cy + 3);
                            g2.fillOval(cx - 1, cy + 5, 2, 2);
                            break;
                        default:
                            g2.drawOval(cx - 9, cy - 9, 18, 18);
                            g2.drawLine(cx, cy - 2, cx, cy + 5);
                            g2.fillOval(cx - 1, cy - 6, 2, 2);
                            break;
                    }

                    g2.dispose();
                }
            };
        }
    }
}
