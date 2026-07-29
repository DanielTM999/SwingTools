package dtm.stools.component.panels.tab;

import dtm.stools.component.annimation.ComponentAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class TabDragController {
    private final TabbedPanel tabs;
    private TabDragSession session;
    private int startThreshold = 6;
    private boolean animationEnabled = true;
    private boolean reorderWhileDragging;
    private boolean splitPreviewEnabled = true;
    private Color ghostBorderColor = new Color(0xE5E7EB);
    private Color splitPreviewColor = new Color(0x2563EB);
    private float ghostAlpha = 0.9f;
    private float splitPreviewAlpha = 0.18f;
    private boolean detachedPreviewEnabled = true;
    private Dimension detachedPreviewSize = new Dimension(360, 220);
    private float detachedPreviewAlpha = 0.88f;
    private ComponentAnimator<JComponent> animator;
    private AWTEventListener releaseListener;

    public TabDragController(TabbedPanel tabs) {
        this.tabs = tabs;
    }

    public void install(JComponent header, TabEntry entry) {
        header.setCursor(tabs.isTabDragEnabled()
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    tabs.showTabMenu(entry, e);
                    return;
                }
                if (tabs.isTabDragEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tabs.getTabbedPane());
                    session = new TabDragSession(entry, header, point);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    tabs.handleHeaderMiddleClick(entry);
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    tabs.handleHeaderDoubleClick(entry);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!tabs.isTabDragEnabled() || session == null || session.getEntry() != entry) return;

                if (!session.isStarted() && shouldStartDrag(e)) {
                    beginDrag();
                }

                if (session.isStarted()) {
                    Point screenPoint = e.getLocationOnScreen();
                    boolean detachedPreview = shouldShowDetachedPreview(screenPoint);
                    updateDragPreview(e, screenPoint, detachedPreview);
                    if (detachedPreview) {
                        return;
                    }

                    Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tabs.getTabbedPane());
                    boolean transferTarget = tabs.hasTransferTarget(point);
                    TabSplitPlacement placement = transferTarget ? null : tabs.resolveSplitPlacement(point);
                    updateSplitPreview(placement);
                    if (!transferTarget && placement == null && reorderWhileDragging) {
                        liveReorder(point);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    tabs.showTabMenu(entry, e);
                    stopDrag();
                    return;
                }
                if (session != null && session.getEntry() == entry) {
                    if (session.isStarted()) {
                        Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tabs.getTabbedPane());
                        finishDrag(point);
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        tabs.switchTo(entry.getKey());
                    }
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    tabs.switchTo(entry.getKey());
                }
                stopDrag();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (session != null
                        && session.getEntry() == entry
                        && session.isStarted()
                        && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
                    stopDrag();
                }
            }
        };
        installMouseHandler(header, adapter);
    }

    public int getStartThreshold() {
        return startThreshold;
    }

    public void setStartThreshold(int startThreshold) {
        this.startThreshold = Math.max(1, startThreshold);
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public Color getGhostBorderColor() {
        return ghostBorderColor;
    }

    public void setGhostBorderColor(Color ghostBorderColor) {
        this.ghostBorderColor = ghostBorderColor;
    }

    public float getGhostAlpha() {
        return ghostAlpha;
    }

    public void setGhostAlpha(float ghostAlpha) {
        this.ghostAlpha = Math.max(0.1f, Math.min(1f, ghostAlpha));
    }

    public ComponentAnimator<JComponent> getAnimator() {
        return animator;
    }

    public void setAnimator(ComponentAnimator<JComponent> animator) {
        this.animator = animator;
    }

    public boolean isReorderWhileDragging() {
        return reorderWhileDragging;
    }

    public void setReorderWhileDragging(boolean reorderWhileDragging) {
        this.reorderWhileDragging = reorderWhileDragging;
    }

    public boolean isSplitPreviewEnabled() {
        return splitPreviewEnabled;
    }

    public void setSplitPreviewEnabled(boolean splitPreviewEnabled) {
        this.splitPreviewEnabled = splitPreviewEnabled;
    }

    public Color getSplitPreviewColor() {
        return splitPreviewColor;
    }

    public void setSplitPreviewColor(Color splitPreviewColor) {
        this.splitPreviewColor = splitPreviewColor;
    }

    public float getSplitPreviewAlpha() {
        return splitPreviewAlpha;
    }

    public void setSplitPreviewAlpha(float splitPreviewAlpha) {
        this.splitPreviewAlpha = Math.max(0.05f, Math.min(0.7f, splitPreviewAlpha));
    }

    public boolean isDetachedPreviewEnabled() {
        return detachedPreviewEnabled;
    }

    public void setDetachedPreviewEnabled(boolean detachedPreviewEnabled) {
        this.detachedPreviewEnabled = detachedPreviewEnabled;
        if (!detachedPreviewEnabled) {
            hideDetachedPreview();
        }
    }

    public Dimension getDetachedPreviewSize() {
        return new Dimension(detachedPreviewSize);
    }

    public void setDetachedPreviewSize(Dimension detachedPreviewSize) {
        if (detachedPreviewSize == null) {
            return;
        }
        this.detachedPreviewSize = new Dimension(
                Math.max(160, detachedPreviewSize.width),
                Math.max(100, detachedPreviewSize.height)
        );
    }

    public float getDetachedPreviewAlpha() {
        return detachedPreviewAlpha;
    }

    public void setDetachedPreviewAlpha(float detachedPreviewAlpha) {
        this.detachedPreviewAlpha = Math.max(0.2f, Math.min(1f, detachedPreviewAlpha));
    }

    private void installMouseHandler(Component component, MouseAdapter adapter) {
        if (!(component instanceof AbstractButton)) {
            component.addMouseListener(adapter);
            component.addMouseMotionListener(adapter);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installMouseHandler(child, adapter);
            }
        }
    }

    private boolean shouldStartDrag(MouseEvent event) {
        Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), tabs.getTabbedPane());
        int dx = point.x - session.getStartPoint().x;
        int dy = point.y - session.getStartPoint().y;
        return Math.hypot(dx, dy) >= startThreshold;
    }

    private void beginDrag() {
        cleanupGhosts();
        session.setOriginalCursor(session.getHeader().getCursor());
        session.getHeader().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        session.setStarted(true);
        installGlobalReleaseListener();
        if (animationEnabled) {
            showGhost();
        }
        if (animator != null) {
            JComponent target = session.getGhostComponent() == null ? session.getHeader() : session.getGhostComponent();
            animator.animate(session.getHeader(), target, () -> {});
        }
    }

    private void installGlobalReleaseListener() {
        if (releaseListener != null) return;

        releaseListener = event -> {
            if (!(event instanceof MouseEvent mouseEvent)) return;
            if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                SwingUtilities.invokeLater(() -> {
                    if (session != null && session.isStarted()) {
                        Point point = mouseEvent.getLocationOnScreen();
                        SwingUtilities.convertPointFromScreen(point, tabs.getTabbedPane());
                        finishDrag(point);
                    }
                    stopDrag();
                });
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(releaseListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void uninstallGlobalReleaseListener() {
        if (releaseListener == null) return;
        Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
        releaseListener = null;
    }

    private void finishDrag(Point point) {
        if (session == null || !session.isStarted()) return;
        hideDetachedPreview();
        hideSplitPreview();
        if (tabs.transferTabFromDrag(session.getEntry().getKey(), point)) return;
        if (tabs.detachTabToWindowFromDrag(session.getEntry().getKey(), point)) return;
        if (tabs.splitTabFromDrag(session.getEntry().getKey(), point)) return;
        liveReorder(point);
    }

    private void liveReorder(Point point) {
        int targetIndex = tabs.resolveDropIndex(point);
        int oldIndex = tabs.indexOf(session.getEntry().getKey());
        if (targetIndex >= 0 && oldIndex >= 0 && oldIndex != targetIndex) {
            tabs.moveTabFromDrag(session.getEntry().getKey(), targetIndex, oldIndex);
        }
    }

    private void showGhost() {
        JRootPane rootPane = SwingUtilities.getRootPane(tabs);
        if (rootPane == null) return;

        JLayeredPane layeredPane = rootPane.getLayeredPane();
        cleanupGhosts(layeredPane);
        JComponent ghost = createGhost();
        ghost.putClientProperty("TabbedPanel.dragGhost", Boolean.TRUE);
        Dimension size = ghost.getPreferredSize();
        ghost.setSize(size);
        ghost.setVisible(true);
        ghost.setEnabled(false);

        session.setDragLayer(layeredPane);
        session.setGhostComponent(ghost);
        layeredPane.add(ghost, JLayeredPane.DRAG_LAYER);

        Point mouse = MouseInfo.getPointerInfo().getLocation();
        Point offset = new Point(Math.max(8, session.getHeader().getWidth() / 2), Math.max(8, session.getHeader().getHeight() / 2));
        Point layeredPoint = new Point(mouse);
        SwingUtilities.convertPointFromScreen(layeredPoint, layeredPane);
        session.setGhostOffset(offset);
        ghost.setLocation(layeredPoint.x - offset.x, layeredPoint.y - offset.y);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private JComponent createGhost() {
        JComponent header = session.getHeader();
        Dimension size = header.getSize();
        if (size.width <= 0 || size.height <= 0) {
            size = header.getPreferredSize();
            header.setSize(size);
        }

        BufferedImage image = new BufferedImage(Math.max(1, size.width), Math.max(1, size.height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        header.paint(g2);
        g2.dispose();

        JComponent ghost = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(image.getWidth(), image.getHeight());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.SrcOver.derive(ghostAlpha));
                g2.drawImage(image, 0, 0, null);
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(ghostBorderColor == null ? Color.GRAY : ghostBorderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        ghost.setOpaque(false);
        return ghost;
    }

    private void updateGhostLocation(MouseEvent event) {
        if (session.getGhostComponent() == null || session.getDragLayer() == null) return;
        Point mouse = event.getLocationOnScreen();
        Point offset = session.getGhostOffset() == null ? new Point(0, 0) : session.getGhostOffset();
        SwingUtilities.convertPointFromScreen(mouse, session.getDragLayer());
        session.getGhostComponent().setLocation(mouse.x - offset.x, mouse.y - offset.y);
        session.getDragLayer().repaint();
    }

    private void updateDragPreview(MouseEvent event, Point screenPoint, boolean detached) {
        JComponent ghost = session.getGhostComponent();
        if (detached) {
            if (ghost != null) {
                ghost.setVisible(false);
            }
            hideSplitPreview();
            showDetachedPreview(screenPoint);
            return;
        }

        hideDetachedPreview();
        if (ghost != null) {
            ghost.setVisible(true);
        }
        updateGhostLocation(event);
    }

    private boolean shouldShowDetachedPreview(Point screenPoint) {
        if (!detachedPreviewEnabled || !tabs.isTabWindowEnabled() || screenPoint == null) {
            return false;
        }
        Window owner = SwingUtilities.getWindowAncestor(tabs);
        return owner != null && !owner.getBounds().contains(screenPoint);
    }

    private void showDetachedPreview(Point screenPoint) {
        if (session == null || screenPoint == null || GraphicsEnvironment.isHeadless()) {
            return;
        }

        JWindow previewWindow = session.getDetachedPreviewWindow();
        if (previewWindow == null) {
            BufferedImage image = createDetachedPreviewImage(session.getEntry());
            Window owner = SwingUtilities.getWindowAncestor(tabs);
            previewWindow = owner == null ? new JWindow() : new JWindow(owner);
            previewWindow.setFocusableWindowState(false);
            previewWindow.setAutoRequestFocus(false);
            previewWindow.setType(Window.Type.POPUP);
            try {
                previewWindow.setBackground(new Color(0, 0, 0, 0));
                previewWindow.setAlwaysOnTop(true);
            } catch (RuntimeException ignored) {
                // Some window managers do not support per-pixel transparency or always-on-top.
            }
            previewWindow.setContentPane(createDetachedPreviewComponent(image));
            previewWindow.pack();
            session.setDetachedPreviewWindow(previewWindow);
        }

        Point location = resolveDetachedPreviewLocation(screenPoint, previewWindow.getSize());
        previewWindow.setLocation(location);
        if (!previewWindow.isVisible()) {
            previewWindow.setVisible(true);
        }
        previewWindow.repaint();
    }

    BufferedImage createDetachedPreviewImage(TabEntry entry) {
        Component content = entry == null ? null : entry.getComponent();
        Dimension sourceSize = content == null ? new Dimension(640, 400) : content.getSize();
        if (sourceSize.width <= 0 || sourceSize.height <= 0) {
            Dimension preferred = content == null ? null : content.getPreferredSize();
            sourceSize = preferred == null ? new Dimension(640, 400) : preferred;
        }
        sourceSize = new Dimension(Math.max(1, sourceSize.width), Math.max(1, sourceSize.height));

        int maxWidth = Math.max(160, detachedPreviewSize.width);
        int maxHeight = Math.max(100, detachedPreviewSize.height);
        int headerHeight = Math.min(30, Math.max(24, maxHeight / 6));
        double scale = Math.min(
                1d,
                Math.min(
                        (double) maxWidth / sourceSize.width,
                        (double) (maxHeight - headerHeight) / sourceSize.height
                )
        );
        int contentWidth = Math.max(1, (int) Math.round(sourceSize.width * scale));
        int contentHeight = Math.max(1, (int) Math.round(sourceSize.height * scale));
        int width = Math.max(160, contentWidth);
        int height = headerHeight + contentHeight;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color background = uiColor("Panel.background", new Color(43, 45, 48));
        Color headerBackground = uiColor("TabbedPane.background", background.darker());
        Color foreground = uiColor("Label.foreground", new Color(220, 221, 222));
        g2.setColor(background);
        g2.fillRect(0, 0, width, height);
        g2.setColor(headerBackground);
        g2.fillRect(0, 0, width, headerHeight);

        int titleX = 10;
        Icon icon = entry == null ? null : entry.getIcon();
        if (icon != null) {
            int iconY = Math.max(0, (headerHeight - icon.getIconHeight()) / 2);
            icon.paintIcon(null, g2, titleX, iconY);
            titleX += icon.getIconWidth() + 7;
        }

        Font font = UIManager.getFont("Label.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        g2.setFont(font.deriveFont(Math.min(12f, font.getSize2D())));
        g2.setColor(foreground);
        FontMetrics metrics = g2.getFontMetrics();
        String title = entry == null || entry.getTitle() == null ? "" : entry.getTitle();
        title = clipText(title, metrics, Math.max(1, width - titleX - 10));
        g2.drawString(title, titleX, (headerHeight - metrics.getHeight()) / 2 + metrics.getAscent());

        if (content != null) {
            Dimension oldSize = content.getSize();
            boolean needsLayout = oldSize.width <= 0 || oldSize.height <= 0;
            if (needsLayout) {
                content.setSize(sourceSize);
                if (content instanceof Container container) {
                    container.doLayout();
                }
            }

            Graphics2D contentGraphics = (Graphics2D) g2.create();
            contentGraphics.translate((width - contentWidth) / 2d, headerHeight);
            contentGraphics.clipRect(0, 0, contentWidth, contentHeight);
            contentGraphics.scale(scale, scale);
            content.printAll(contentGraphics);
            contentGraphics.dispose();

            if (needsLayout) {
                content.setSize(oldSize);
            }
        }
        g2.dispose();
        return image;
    }

    private JComponent createDetachedPreviewComponent(BufferedImage image) {
        int margin = 8;
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(image.getWidth() + margin * 2, image.getHeight() + margin * 2);
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = image.getWidth();
                int height = image.getHeight();
                g2.setComposite(AlphaComposite.SrcOver.derive(0.28f));
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(margin + 2, margin + 3, width, height, 12, 12);

                Shape clip = new RoundRectangle2D.Float(margin, margin, width, height, 10, 10);
                g2.setClip(clip);
                g2.setComposite(AlphaComposite.SrcOver.derive(detachedPreviewAlpha));
                g2.drawImage(image, margin, margin, null);
                g2.setClip(null);
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(ghostBorderColor == null ? new Color(0x6B7280) : ghostBorderColor);
                g2.drawRoundRect(margin, margin, width - 1, height - 1, 10, 10);
                g2.dispose();
            }
        };
    }

    private Point resolveDetachedPreviewLocation(Point cursor, Dimension previewSize) {
        Rectangle screen = screenBoundsAt(cursor);
        int gap = 18;
        int x = cursor.x + gap;
        int y = cursor.y + gap;
        if (x + previewSize.width > screen.x + screen.width) {
            x = cursor.x - previewSize.width - gap;
        }
        if (y + previewSize.height > screen.y + screen.height) {
            y = cursor.y - previewSize.height - gap;
        }
        return new Point(Math.max(screen.x, x), Math.max(screen.y, y));
    }

    private Rectangle screenBoundsAt(Point point) {
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration configuration = device.getDefaultConfiguration();
            if (configuration.getBounds().contains(point)) {
                return configuration.getBounds();
            }
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
    }

    private String clipText(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int suffixWidth = metrics.stringWidth(suffix);
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }

    private Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private void hideDetachedPreview() {
        if (session == null) {
            return;
        }
        JWindow previewWindow = session.getDetachedPreviewWindow();
        if (previewWindow == null) {
            return;
        }
        session.setDetachedPreviewWindow(null);
        previewWindow.setVisible(false);
        previewWindow.dispose();
    }

    private void updateSplitPreview(TabSplitPlacement placement) {
        if (!splitPreviewEnabled || placement == null || session.getDragLayer() == null) {
            hideSplitPreview();
            return;
        }

        Rectangle bounds = tabs.getSplitPreviewBounds(placement);
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            hideSplitPreview();
            return;
        }

        JComponent preview = session.getSplitPreviewComponent();
        if (preview == null) {
            preview = createSplitPreview();
            session.setSplitPreviewComponent(preview);
            session.getDragLayer().add(preview, JLayeredPane.PALETTE_LAYER);
        }

        Point location = SwingUtilities.convertPoint(tabs.getTabbedPane(), bounds.getLocation(), session.getDragLayer());
        preview.setBounds(location.x, location.y, bounds.width, bounds.height);
        preview.setVisible(true);
        session.getDragLayer().repaint();
    }

    private JComponent createSplitPreview() {
        JComponent preview = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = splitPreviewColor == null ? new Color(0x2563EB) : splitPreviewColor;
                g2.setComposite(AlphaComposite.SrcOver.derive(splitPreviewAlpha));
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, splitPreviewAlpha + 0.35f)));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                g2.dispose();
            }
        };
        preview.putClientProperty("TabbedPanel.splitPreview", Boolean.TRUE);
        preview.setOpaque(false);
        preview.setEnabled(false);
        return preview;
    }

    private void hideSplitPreview() {
        if (session == null || session.getSplitPreviewComponent() == null || session.getDragLayer() == null) return;
        session.getDragLayer().remove(session.getSplitPreviewComponent());
        session.getDragLayer().revalidate();
        session.getDragLayer().repaint();
        session.setSplitPreviewComponent(null);
    }

    private void stopDrag() {
        if (session == null) return;

        hideDetachedPreview();
        hideSplitPreview();
        if (session.getGhostComponent() != null && session.getDragLayer() != null) {
            session.getDragLayer().remove(session.getGhostComponent());
            session.getDragLayer().revalidate();
            session.getDragLayer().repaint();
        }
        session.getEntry().setDragging(false);
        if (session.getHeader() != null) {
            session.getHeader().setCursor(session.getOriginalCursor());
            session.getHeader().repaint();
        }
        uninstallGlobalReleaseListener();
        cleanupGhosts();
        session = null;
    }

    private void cleanupGhosts() {
        JRootPane rootPane = SwingUtilities.getRootPane(tabs);
        if (rootPane != null) {
            cleanupGhosts(rootPane.getLayeredPane());
        }
    }

    private void cleanupGhosts(JLayeredPane layeredPane) {
        if (layeredPane == null) return;
        boolean changed = false;
        for (Component component : layeredPane.getComponents()) {
            if (component instanceof JComponent jComponent
                    && (Boolean.TRUE.equals(jComponent.getClientProperty("TabbedPanel.dragGhost"))
                    || Boolean.TRUE.equals(jComponent.getClientProperty("TabbedPanel.splitPreview")))) {
                layeredPane.remove(jComponent);
                changed = true;
            }
        }
        if (changed) {
            layeredPane.revalidate();
            layeredPane.repaint();
        }
    }
}
