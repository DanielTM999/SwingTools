package dtm.stools.component.panels.tab;

import dtm.stools.component.annimation.ComponentAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
                    updateGhostLocation(e);
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
        if (tabs.transferTabFromDrag(session.getEntry().getKey(), point)) return;
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
