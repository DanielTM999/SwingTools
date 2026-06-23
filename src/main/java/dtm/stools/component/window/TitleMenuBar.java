package dtm.stools.component.window;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.stools.component.menu.bar.MenuBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TitleMenuBar extends JPanel {

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private final MenuBar menuBar;
    private final JPanel menuHost = new JPanel(new GridBagLayout());
    private final JPanel captionArea = new JPanel(new BorderLayout());
    private final JPanel centerSlot = new JPanel(new GridBagLayout());
    private final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    private final JPanel buttonsPlaceholder = new JPanel();
    private final JPanel rightSide = new JPanel(new BorderLayout());

    private Color backgroundStart = new Color(0x20262B);
    private Color backgroundEnd = new Color(0x334B3B);
    private Color foreground = new Color(0xDFE1E5);
    private double gradientAngleDegrees = 0;
    private float gradientIntensity = 1f;
    private int titleBarHeight = 40;

    public TitleMenuBar(MenuBar menuBar) {
        this.menuBar = menuBar == null ? new MenuBar() : menuBar;
        setLayout(null);
        setOpaque(false);

        menuHost.setOpaque(false);
        menuHost.add(this.menuBar);

        captionArea.setOpaque(false);
        captionArea.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true);

        centerSlot.setOpaque(false);

        trailing.setOpaque(false);

        rightSide.setOpaque(false);
        rightSide.add(trailing, BorderLayout.CENTER);
        rightSide.add(buttonsPlaceholder, BorderLayout.EAST);

        buttonsPlaceholder.setOpaque(false);
        buttonsPlaceholder.putClientProperty(
                FlatClientProperties.FULL_WINDOW_CONTENT_BUTTONS_PLACEHOLDER,
                "win"
        );

        configureMenuBar();
        installWindowDragger();
        add(captionArea);
        add(menuHost);
        add(centerSlot);
        add(rightSide);
        updatePreferredHeight();
    }

    private void installWindowDragger() {
        WindowDragger dragger = new WindowDragger();
        for (JComponent component : new JComponent[]{this, menuHost, centerSlot, trailing, rightSide}) {
            component.addMouseListener(dragger);
            component.addMouseMotionListener(dragger);
        }
    }

    private class WindowDragger extends MouseAdapter {
        private Point dragStartScreen;
        private Point windowStart;
        private boolean dragging;

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            Window window = SwingUtilities.getWindowAncestor(TitleMenuBar.this);
            if (window == null) return;
            dragStartScreen = e.getLocationOnScreen();
            windowStart = window.getLocation();
            dragging = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            dragStartScreen = null;
            windowStart = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!dragging || dragStartScreen == null) return;
            Window window = SwingUtilities.getWindowAncestor(TitleMenuBar.this);
            if (window == null) return;
            Point current = e.getLocationOnScreen();
            int dx = current.x - dragStartScreen.x;
            int dy = current.y - dragStartScreen.y;
            if (window instanceof Frame frame
                    && (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0) {
                frame.setExtendedState(frame.getExtendedState() & ~Frame.MAXIMIZED_BOTH);
                Dimension size = window.getSize();
                int newX = current.x - size.width / 2;
                int newY = current.y - getHeight() / 2;
                window.setLocation(newX, newY);
                windowStart = window.getLocation();
                dragStartScreen = current;
                return;
            }
            window.setLocation(windowStart.x + dx, windowStart.y + dy);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
            Window window = SwingUtilities.getWindowAncestor(TitleMenuBar.this);
            if (!(window instanceof Frame frame)) return;
            int state = frame.getExtendedState();
            if ((state & Frame.MAXIMIZED_BOTH) != 0) {
                frame.setExtendedState(state & ~Frame.MAXIMIZED_BOTH);
            } else {
                frame.setExtendedState(state | Frame.MAXIMIZED_BOTH);
            }
        }
    }

    public static TitleMenuBar install(JFrame frame, MenuBar menuBar) {
        TitleMenuBar titleMenuBar = new TitleMenuBar(menuBar);
        titleMenuBar.installOn(frame);
        return titleMenuBar;
    }

    public TitleMenuBar installOn(JFrame frame) {
        if (frame == null) return this;
        configureRootPane(frame.getRootPane());
        configureButtonHeight(titleBarHeight);
        configureRootPaneColors(frame.getRootPane());
        frame.add(this, BorderLayout.NORTH);
        return this;
    }

    public TitleMenuBar gradient(Color start, Color end) {
        return gradient(start, end, gradientAngleDegrees, gradientIntensity);
    }

    public TitleMenuBar gradient(Color start, Color end, double angleDegrees) {
        return gradient(start, end, angleDegrees, gradientIntensity);
    }

    public TitleMenuBar gradient(Color start, Color end, float intensity) {
        return gradient(start, end, gradientAngleDegrees, intensity);
    }

    public TitleMenuBar gradient(Color start, Color end, double angleDegrees, float intensity) {
        if (start != null) this.backgroundStart = start;
        if (end != null) this.backgroundEnd = end;
        this.gradientAngleDegrees = angleDegrees;
        this.gradientIntensity = clamp(intensity);
        applyFrameColors();
        repaint();
        return this;
    }

    public TitleMenuBar background(Color color) {
        return gradient(color, color);
    }

    public TitleMenuBar foreground(Color color) {
        if (color != null) {
            this.foreground = color;
            menuBar.setBarForeground(color);
            applyFrameColors();
            repaint();
        }
        return this;
    }

    public TitleMenuBar gradientAngle(double angleDegrees) {
        this.gradientAngleDegrees = angleDegrees;
        repaint();
        return this;
    }

    public TitleMenuBar gradientIntensity(float intensity) {
        this.gradientIntensity = clamp(intensity);
        repaint();
        return this;
    }

    public TitleMenuBar titleBarHeight(int height) {
        this.titleBarHeight = Math.max(28, height);
        updatePreferredHeight();
        configureButtonHeight(titleBarHeight);
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, titleBarHeight);
        }
        revalidate();
        repaint();
        return this;
    }

    public TitleMenuBar addTrailing(Component component) {
        if (component != null) {
            trailing.add(component);
            revalidate();
            repaint();
        }
        return this;
    }

    public TitleMenuBar center(Component component) {
        centerSlot.removeAll();
        if (component != null) {
            centerSlot.add(component);
        }
        revalidate();
        repaint();
        return this;
    }

    public TitleMenuBar clearCenter() {
        return center(null);
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public static void configureRootPane(JRootPane rootPane) {
        if (rootPane == null) return;
        rootPane.putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
        rootPane.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, true);
        rootPane.putClientProperty(FlatClientProperties.MENU_BAR_EMBEDDED, false);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_ICON, false);
    }

    public static void configureLookAndFeelDefaults(Color background, Color foreground) {
        UIManager.put("TitlePane.menuBarEmbedded", false);
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("TitlePane.background", background);
        UIManager.put("TitlePane.foreground", foreground);
        configureButtonHeight(40);
        UIManager.put("MenuBar.background", background);
        UIManager.put("MenuBar.foreground", foreground);
    }

    public static void configureButtonHeight(int height) {
        int safeHeight = Math.max(28, height);
        UIManager.put("TitlePane.buttonSize", new Dimension(44, safeHeight));
        UIManager.put("TitlePane.buttonMaximizedHeight", -1);
    }

    private void configureMenuBar() {
        menuBar.clearBackgroundGradient()
                .setBarBackground(TRANSPARENT)
                .setBarForeground(foreground);
        menuBar.style(style -> style
                .bottomBorder(false)
                .barPadding(new Insets(0, 8, 0, 8))
                .menuPadding(new Insets(7, 12, 7, 12)));
        keepMenuBarTransparent();
        menuBar.addPropertyChangeListener("UI", e -> SwingUtilities.invokeLater(() ->
                SwingUtilities.invokeLater(this::keepMenuBarTransparent)
        ));
    }

    private void keepMenuBarTransparent() {
        menuBar.clearBackgroundGradient();
        menuBar.setOpaque(false);
        menuBar.setBackground(TRANSPARENT);
        for (Component component : menuBar.getComponents()) {
            if (component instanceof JComponent jc) {
                jc.setOpaque(false);
            }
            if (component instanceof AbstractButton button) {
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
            }
        }
    }

    private void applyFrameColors() {
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane == null) return;
        configureRootPaneColors(rootPane);
    }

    private void configureRootPaneColors(JRootPane rootPane) {
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, backgroundStart);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND, foreground);
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_HEIGHT, titleBarHeight);
    }

    private void updatePreferredHeight() {
        setPreferredSize(new Dimension(10, titleBarHeight));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        applyFrameColors();
        SwingUtilities.invokeLater(this::keepMenuBarTransparent);
    }

    @Override
    public void doLayout() {
        int width = getWidth();
        int height = getHeight();

        Dimension menuSize = menuHost.getPreferredSize();
        Dimension rightSize = rightSide.getPreferredSize();
        Dimension centerSize = centerSlot.getPreferredSize();

        int rightWidth = Math.min(rightSize.width, width);
        int menuWidth = Math.min(menuSize.width, Math.max(0, width - rightWidth));
        int centerWidth = Math.min(centerSize.width, Math.max(0, width - rightWidth));
        int centerHeight = Math.min(centerSize.height, height);

        int centerX = Math.max(menuWidth, (width - centerWidth) / 2);
        int maxCenterX = Math.max(menuWidth, width - rightWidth - centerWidth);
        centerX = Math.min(centerX, maxCenterX);
        int centerY = Math.max(0, (height - centerHeight) / 2);

        captionArea.setBounds(menuWidth, 0, Math.max(0, width - menuWidth - rightWidth), height);
        menuHost.setBounds(0, 0, menuWidth, height);
        centerSlot.setBounds(centerX, centerY, centerWidth, centerHeight);
        rightSide.setBounds(width - rightWidth, 0, rightWidth, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Color end = blend(backgroundStart, backgroundEnd, gradientIntensity);
            double rad = Math.toRadians(gradientAngleDegrees);
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            double dx = Math.cos(rad);
            double dy = Math.sin(rad);
            double half = (Math.abs(dx) * getWidth() + Math.abs(dy) * getHeight()) / 2.0;
            float x1 = (float) (cx - dx * half);
            float y1 = (float) (cy - dy * half);
            float x2 = (float) (cx + dx * half);
            float y2 = (float) (cy + dy * half);

            g2.setPaint(new GradientPaint(x1, y1, backgroundStart, x2, y2, end));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static Color blend(Color from, Color to, float amount) {
        float clamped = clamp(amount);
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }
}
