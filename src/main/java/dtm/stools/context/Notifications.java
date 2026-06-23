package dtm.stools.context;

import dtm.stools.activity.NotificationActivity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class Notifications {

    public enum Type {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private static final int PADDING = 20;
    private static final Map<NotificationActivity.NotificationActivityLocation, ToastHost> HOSTS =
            new EnumMap<>(NotificationActivity.NotificationActivityLocation.class);
    private static int gap = 10;

    private Notifications() {}

    public static Builder builder() {
        return new Builder();
    }

    public static Builder toast(String message) {
        return builder().message(message);
    }

    public static Builder info(String message) {
        return builder().type(Type.INFO).title("Informacao").message(message);
    }

    public static Builder info(String title, String message) {
        return builder().type(Type.INFO).title(title).message(message);
    }

    public static Builder success(String message) {
        return builder().type(Type.SUCCESS).title("Sucesso").message(message);
    }

    public static Builder success(String title, String message) {
        return builder().type(Type.SUCCESS).title(title).message(message);
    }

    public static Builder warning(String message) {
        return builder().type(Type.WARNING).title("Atencao").message(message);
    }

    public static Builder warning(String title, String message) {
        return builder().type(Type.WARNING).title(title).message(message);
    }

    public static Builder error(String message) {
        return builder().type(Type.ERROR).title("Erro").message(message);
    }

    public static Builder error(String title, String message) {
        return builder().type(Type.ERROR).title(title).message(message);
    }

    public static Builder error(Throwable error) {
        return error("Erro", error);
    }

    public static Builder error(String title, Throwable error) {
        Objects.requireNonNull(error, "error");
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return error(title, message);
    }

    public static ToastHandle show(String message) {
        return toast(message).show();
    }

    public static ToastHandle showInfo(String message) {
        return info(message).show();
    }

    public static ToastHandle showSuccess(String message) {
        return success(message).show();
    }

    public static ToastHandle showWarning(String message) {
        return warning(message).show();
    }

    public static ToastHandle showError(String message) {
        return error(message).show();
    }

    public static void closeAll() {
        runOnEdt(() -> {
            for (ToastHost host : HOSTS.values().toArray(new ToastHost[0])) {
                host.closeAll();
            }
            HOSTS.clear();
        });
    }

    public static void setGap(int gap) {
        runOnEdt(() -> {
            Notifications.gap = Math.max(0, gap);
            for (ToastHost host : HOSTS.values()) {
                host.repackAndPosition();
            }
        });
    }

    public static final class Builder {
        private Type type = Type.INFO;
        private String title = "";
        private String message = "";
        private NotificationActivity.NotificationActivityLocation location =
                NotificationActivity.NotificationActivityLocation.BOTTOM_RIGHT;
        private long duration = 4_000L;
        private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
        private int width = 320;
        private Color accentColor;
        private Color background;
        private Color foreground;
        private String actionText;
        private Runnable action;
        private Runnable onClick;
        private boolean closeOnClick = true;
        private boolean closeOnAction = true;

        private Builder() {}

        public Builder type(Type type) {
            this.type = type == null ? Type.INFO : type;
            return this;
        }

        public Builder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public Builder location(NotificationActivity.NotificationActivityLocation location) {
            if (location != null) {
                this.location = location;
            }
            return this;
        }

        public Builder duration(long duration) {
            return duration(duration, TimeUnit.MILLISECONDS);
        }

        public Builder duration(long duration, TimeUnit unit) {
            this.duration = duration;
            this.durationUnit = unit == null ? TimeUnit.MILLISECONDS : unit;
            return this;
        }

        public Builder persistent() {
            this.duration = -1L;
            return this;
        }

        public Builder width(int width) {
            this.width = Math.max(220, width);
            return this;
        }

        public Builder accentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }

        public Builder background(Color background) {
            this.background = background;
            return this;
        }

        public Builder foreground(Color foreground) {
            this.foreground = foreground;
            return this;
        }

        public Builder action(String text, Runnable action) {
            this.actionText = text;
            this.action = action;
            return this;
        }

        public Builder onClick(Runnable onClick) {
            this.onClick = onClick;
            return this;
        }

        public Builder closeOnClick(boolean closeOnClick) {
            this.closeOnClick = closeOnClick;
            return this;
        }

        public Builder closeOnAction(boolean closeOnAction) {
            this.closeOnAction = closeOnAction;
            return this;
        }

        public ToastHandle show() {
            return runOnEdt(() -> {
                ToastHost host = HOSTS.computeIfAbsent(location, ToastHost::new);
                ToastCard card = new ToastCard(this);
                ToastHandle handle = new ToastHandle(host, card);
                card.setHandle(handle);
                host.addToast(card);

                long millis = duration > 0 ? durationUnit.toMillis(duration) : duration;
                if (millis > 0) {
                    Timer timer = new Timer((int) Math.min(Integer.MAX_VALUE, millis), e -> handle.close());
                    timer.setRepeats(false);
                    handle.timer = timer;
                    timer.start();
                }

                return handle;
            });
        }
    }

    public static final class ToastHandle {
        private final ToastHost host;
        private final ToastCard card;
        private Timer timer;
        private boolean closed;

        private ToastHandle(ToastHost host, ToastCard card) {
            this.host = host;
            this.card = card;
        }

        public void close() {
            runOnEdt(() -> {
                if (closed) {
                    return;
                }
                closed = true;
                if (timer != null) {
                    timer.stop();
                }
                host.removeToast(card);
            });
        }

        public boolean isShowing() {
            return !closed && card.isShowing();
        }
    }

    private static final class ToastHost extends JWindow {
        private final NotificationActivity.NotificationActivityLocation location;
        private final JPanel stack = new JPanel();

        private ToastHost(NotificationActivity.NotificationActivityLocation location) {
            this.location = location;
            setAlwaysOnTop(true);
            setFocusableWindowState(false);
            setBackground(new Color(0, 0, 0, 0));

            stack.setOpaque(false);
            stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
            stack.setBorder(new EmptyBorder(0, 0, 0, 0));
            setContentPane(stack);
        }

        private void addToast(ToastCard card) {
            if (isBottomLocation()) {
                stack.add(card, 0);
            } else {
                stack.add(card);
            }
            repackAndPosition();
            setVisible(true);
            toFront();
        }

        private void removeToast(ToastCard card) {
            stack.remove(card);
            if (stack.getComponentCount() == 0) {
                setVisible(false);
                dispose();
                HOSTS.remove(location);
                return;
            }
            repackAndPosition();
        }

        private void closeAll() {
            stack.removeAll();
            setVisible(false);
            dispose();
        }

        private void repackAndPosition() {
            int count = stack.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component component = stack.getComponent(i);
                int bottom = i == count - 1 ? 0 : gap;
                if (component instanceof JComponent jComponent) {
                    if (component instanceof ToastCard toastCard) {
                        toastCard.setBottomGap(bottom);
                    }
                    jComponent.setBorder(BorderFactory.createCompoundBorder(
                            new EmptyBorder(0, 0, bottom, 0),
                            ((ToastCard) component).contentBorder()
                    ));
                }
            }

            pack();
            position();
            revalidate();
            repaint();
        }

        private boolean isBottomLocation() {
            return location == NotificationActivity.NotificationActivityLocation.BOTTOM_LEFT
                    || location == NotificationActivity.NotificationActivityLocation.BOTTOM_RIGHT;
        }

        private void position() {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension windowSize = getSize();

            int x = switch (location) {
                case TOP_LEFT, BOTTOM_LEFT -> PADDING;
                case TOP_RIGHT, BOTTOM_RIGHT -> screenSize.width - windowSize.width - PADDING;
            };

            int y = switch (location) {
                case TOP_LEFT, TOP_RIGHT -> PADDING;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> screenSize.height - windowSize.height - PADDING;
            };

            setLocation(Math.max(PADDING, x), Math.max(PADDING, y));
        }
    }

    private static final class ToastCard extends JPanel {
        private final Color panelBg;
        private final Color primaryFg;
        private final Color secondaryFg;
        private final Color accent;
        private final Color border;
        private final int width;
        private final String actionText;
        private final Runnable action;
        private final Runnable onClick;
        private final boolean closeOnClick;
        private final boolean closeOnAction;
        private int bottomGap;
        private ToastHandle handle;

        private ToastCard(Builder builder) {
            super(new BorderLayout(12, 0));
            this.panelBg = builder.background != null ? builder.background : backgroundColor();
            this.primaryFg = builder.foreground != null ? builder.foreground : foregroundColor();
            this.secondaryFg = withAlpha(primaryFg, 150);
            this.accent = builder.accentColor != null ? builder.accentColor : defaultAccent(builder.type);
            this.border = isDark(panelBg) ? new Color(255, 255, 255, 32) : new Color(0, 0, 0, 24);
            this.width = builder.width;
            this.actionText = builder.actionText;
            this.action = builder.action;
            this.onClick = builder.onClick;
            this.closeOnClick = builder.closeOnClick;
            this.closeOnAction = builder.closeOnAction;

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buildContent(builder);
        }

        private EmptyBorder contentBorder() {
            return new EmptyBorder(14, 14, 14, 14);
        }

        private void setHandle(ToastHandle handle) {
            this.handle = handle;
        }

        private void setBottomGap(int bottomGap) {
            this.bottomGap = Math.max(0, bottomGap);
        }

        private void buildContent(Builder builder) {
            JLabel icon = new JLabel(new NotificationIcon(builder.type, accent));
            icon.setBorder(new EmptyBorder(2, 0, 0, 0));
            add(icon, BorderLayout.WEST);

            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            int textWidth = Math.max(120, width - 100);
            if (!builder.title.isBlank()) {
                JLabel titleLabel = new JLabel(html(builder.title, textWidth));
                titleLabel.setFont(font(13f, Font.BOLD));
                titleLabel.setForeground(primaryFg);
                titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(titleLabel);
            }

            if (!builder.message.isBlank()) {
                JLabel messageLabel = new JLabel(html(builder.message, textWidth));
                messageLabel.setFont(font(12f, Font.PLAIN));
                messageLabel.setForeground(secondaryFg);
                messageLabel.setBorder(new EmptyBorder(builder.title.isBlank() ? 0 : 4, 0, 0, 0));
                messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(messageLabel);
            }

            if (action != null && actionText != null && !actionText.isBlank()) {
                JButton actionButton = buildActionButton(actionText);
                actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                actionButton.setBorder(new EmptyBorder(8, 0, 0, 0));
                body.add(actionButton);
            }

            add(body, BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        runSafely(onClick);
                        if (closeOnClick && handle != null) {
                            handle.close();
                        }
                    }
                }
            });
        }

        private JButton buildActionButton(String text) {
            JButton button = new JButton(text);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setForeground(accent);
            button.setFont(font(12f, Font.BOLD));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setForeground(deriveHover(accent, panelBg, 0.12f));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setForeground(accent);
                }
            });
            button.addActionListener(e -> {
                runSafely(action);
                if (closeOnAction && handle != null) {
                    handle.close();
                }
            });
            return button;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(width, preferred.height);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension minimum = super.getMinimumSize();
            return new Dimension(width, minimum.height);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension maximum = super.getMaximumSize();
            return new Dimension(width, maximum.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(panelBg);
                int cardHeight = Math.max(0, getHeight() - bottomGap);
                g2.fillRoundRect(0, 0, getWidth(), cardHeight, 14, 14);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, cardHeight - 1, 14, 14);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class NotificationIcon implements Icon {
        private final Notifications.Type type;
        private final Color accent;

        private NotificationIcon(Notifications.Type type, Color accent) {
            this.type = type;
            this.accent = accent;
        }

        @Override
        public int getIconWidth() {
            return 34;
        }

        @Override
        public int getIconHeight() {
            return 34;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(withAlpha(accent, 34));
                g2.fillRoundRect(x, y, 34, 34, 10, 10);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = x + 17;
                int cy = y + 17;
                switch (type) {
                    case SUCCESS -> {
                        g2.drawOval(cx - 8, cy - 8, 16, 16);
                        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                        g2.drawLine(cx - 1, cy + 4, cx + 6, cy - 5);
                    }
                    case ERROR -> {
                        g2.drawOval(cx - 8, cy - 8, 16, 16);
                        g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                        g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                    }
                    case WARNING -> {
                        Polygon triangle = new Polygon(
                                new int[]{cx, cx - 9, cx + 9},
                                new int[]{cy - 9, cy + 8, cy + 8},
                                3
                        );
                        g2.drawPolygon(triangle);
                        g2.drawLine(cx, cy - 3, cx, cy + 3);
                        g2.fillOval(cx - 1, cy + 6, 2, 2);
                    }
                    default -> {
                        g2.drawOval(cx - 8, cy - 8, 16, 16);
                        g2.drawLine(cx, cy - 2, cx, cy + 5);
                        g2.fillOval(cx - 1, cy - 6, 2, 2);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static <T> T runOnEdt(java.util.function.Supplier<T> action) {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }

        final Object[] result = new Object[1];
        final RuntimeException[] error = new RuntimeException[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = action.get();
                } catch (RuntimeException e) {
                    error[0] = e;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Notification execution failed.", e);
        }

        if (error[0] != null) {
            throw error[0];
        }

        return (T) result[0];
    }

    private static void runOnEdt(Runnable action) {
        runOnEdt(() -> {
            action.run();
            return null;
        });
    }

    private static void runSafely(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        try {
            runnable.run();
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Notification action failed.", error);
        }
    }

    private static String html(String text, int width) {
        return "<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>";
    }

    private static String escape(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static Font font(float size, int style) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new Font("Segoe UI", style, (int) size);
        }
        return base.deriveFont(style, size);
    }

    private static Color backgroundColor() {
        Color color = UIManager.getColor("Panel.background");
        return color != null ? color : new Color(0x22252B);
    }

    private static Color foregroundColor() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.WHITE;
    }

    private static Color defaultAccent(Type type) {
        return switch (type) {
            case SUCCESS -> new Color(0x22C55E);
            case WARNING -> new Color(0xF59E0B);
            case ERROR -> new Color(0xEF4444);
            default -> {
                Color selected = UIManager.getColor("Button.select");
                yield selected != null ? selected : new Color(0x3B82F6);
            }
        };
    }

    private static Color deriveHover(Color color, Color panelBg, float factor) {
        boolean dark = isDark(panelBg);
        int delta = Math.round(255 * factor);
        if (dark) {
            return new Color(
                    Math.min(255, color.getRed() + delta),
                    Math.min(255, color.getGreen() + delta),
                    Math.min(255, color.getBlue() + delta),
                    color.getAlpha()
            );
        }
        return new Color(
                Math.max(0, color.getRed() - delta),
                Math.max(0, color.getGreen() - delta),
                Math.max(0, color.getBlue() - delta),
                color.getAlpha()
        );
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static boolean isDark(Color color) {
        double lum = 0.2126 * (color.getRed() / 255.0)
                + 0.7152 * (color.getGreen() / 255.0)
                + 0.0722 * (color.getBlue() / 255.0);
        return lum < 0.35;
    }
}
