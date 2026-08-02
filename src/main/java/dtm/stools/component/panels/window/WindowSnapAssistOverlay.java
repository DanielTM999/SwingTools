package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mostra miniaturas das outras janelas nas vagas restantes de um Snap Layout. */
public class WindowSnapAssistOverlay extends JPanel {
    protected final WindowDesktopPanel desktop;
    protected final List<WindowSnap> remainingZones = new ArrayList<>();
    protected final List<WindowPanel> candidates = new ArrayList<>();
    protected final Map<WindowPanel, Image> thumbnails = new LinkedHashMap<>();
    protected WindowPanel sourceWindow;
    private Color overlayColor = new Color(0, 0, 0, 48);

    public WindowSnapAssistOverlay(WindowDesktopPanel desktop) {
        this.desktop = desktop;
        setLayout(null);
        setOpaque(false);
        setVisible(false);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { close(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeSnapAssist");
        getActionMap().put("closeSnapAssist", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { close(); }
        });
    }

    public boolean open(WindowPanel source, List<WindowSnap> zones,
                        List<WindowPanel> availableWindows) {
        closeDirect();
        sourceWindow = source;
        if (zones != null) {
            zones.stream().filter(zone -> zone != null && zone != WindowSnap.NONE)
                    .distinct().forEach(remainingZones::add);
        }
        if (availableWindows != null) {
            availableWindows.stream().filter(window -> window != null && window != source)
                    .distinct().forEach(candidates::add);
        }
        if (remainingZones.isEmpty() || candidates.isEmpty()) {
            closeDirect();
            return false;
        }
        captureThumbnails();
        rebuildZones();
        setVisible(true);
        doLayout();
        for (Component component : getComponents()) {
            if (component instanceof Container container) container.doLayout();
        }
        requestFocusInWindow();
        repaint();
        return true;
    }

    public WindowSnapAssistOverlay close() {
        if (!isVisible() && sourceWindow == null) return this;
        closeDirect();
        desktop.snapAssistClosed();
        return this;
    }

    void closeDirect() {
        setVisible(false);
        removeAll();
        remainingZones.clear();
        candidates.clear();
        thumbnails.clear();
        sourceWindow = null;
        revalidate();
        repaint();
    }

    void completeSelection(WindowPanel selectedWindow, WindowSnap zone) {
        remainingZones.remove(zone);
        candidates.remove(selectedWindow);
        thumbnails.remove(selectedWindow);
        if (remainingZones.isEmpty() || candidates.isEmpty()) {
            close();
        } else {
            rebuildZones();
        }
    }

    protected void captureThumbnails() {
        int targetWidth = Math.min(960, Math.max(480, getWidth()));
        int targetHeight = Math.min(760, Math.max(420, getHeight()));
        for (WindowPanel candidate : candidates) {
            thumbnails.put(candidate, captureWindowThumbnail(candidate, targetWidth, targetHeight));
        }
    }

    protected Image captureWindowThumbnail(WindowPanel window, int maximumWidth, int maximumHeight) {
        int sourceWidth = Math.max(1, window.getWidth());
        int sourceHeight = Math.max(1, window.getHeight());
        double scale = Math.min((double) maximumWidth / sourceWidth,
                (double) maximumHeight / sourceHeight);
        scale = Math.max(.1d, scale);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.scale(scale, scale);
            window.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    protected void rebuildZones() {
        removeAll();
        for (WindowSnap zone : remainingZones) add(createZonePanel(zone));
        revalidate();
        doLayout();
        repaint();
    }

    protected SnapAssistZonePanel createZonePanel(WindowSnap zone) {
        return new SnapAssistZonePanel(zone);
    }

    protected WindowThumbnailButton createThumbnailButton(WindowPanel candidate, WindowSnap zone) {
        return new WindowThumbnailButton(candidate, zone, thumbnails.get(candidate));
    }

    @Override public void doLayout() {
        for (Component component : getComponents()) {
            if (!(component instanceof SnapAssistZonePanel zonePanel)) continue;
            Rectangle bounds = desktop.getSnapPolicy().resolveBounds(
                    desktop, sourceWindow, zonePanel.getZone());
            bounds.grow(-7, -7);
            zonePanel.setBounds(bounds);
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(overlayColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g.dispose();
        }
    }

    public WindowSnapAssistOverlay overlayColor(Color value) {
        overlayColor = value == null ? new Color(0, 0, 0, 0) : value;
        repaint();
        return this;
    }

    public boolean isAssistVisible() { return isVisible(); }
    public WindowPanel getSourceWindow() { return sourceWindow; }
    public List<WindowSnap> getRemainingZones() { return List.copyOf(remainingZones); }
    public List<WindowPanel> getCandidates() { return List.copyOf(candidates); }

    protected class SnapAssistZonePanel extends JPanel {
        private final WindowSnap zone;

        protected SnapAssistZonePanel(WindowSnap zone) {
            this.zone = zone;
            int columns = Math.max(1, Math.min(3, candidates.size()));
            setLayout(new GridLayout(0, columns, 10, 10));
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
            setOpaque(false);
            for (WindowPanel candidate : candidates) add(createThumbnailButton(candidate, zone));
        }

        public WindowSnap getZone() { return zone; }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(85, 105, 125, 78));
                g.fillRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 14, 14);
                g.setColor(new Color(185, 210, 230, 145));
                g.drawRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 14, 14);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public class WindowThumbnailButton extends JButton {
        private final WindowPanel candidate;
        private final WindowSnap zone;
        private final Image thumbnail;

        protected WindowThumbnailButton(WindowPanel candidate, WindowSnap zone, Image thumbnail) {
            this.candidate = candidate;
            this.zone = zone;
            this.thumbnail = thumbnail;
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setToolTipText("Posicionar " + candidate.getTitle());
            getAccessibleContext().setAccessibleName(getToolTipText());
            addActionListener(event -> desktop.applySnapAssistSelection(candidate, zone));
        }

        public WindowPanel getCandidate() { return candidate; }
        public WindowSnap getZone() { return zone; }
        public Dimension getThumbnailSize() {
            return thumbnail == null ? new Dimension()
                    : new Dimension(thumbnail.getWidth(null), thumbnail.getHeight(null));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean highlighted = getModel().isRollover() || isFocusOwner();
                g.setColor(highlighted ? new Color(65, 155, 225, 210) : new Color(35, 38, 43, 220));
                g.fillRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 12, 12);
                g.setColor(highlighted ? new Color(125, 205, 255) : new Color(105, 110, 118));
                g.drawRoundRect(0, 0, Math.max(0, getWidth() - 1),
                        Math.max(0, getHeight() - 1), 12, 12);
                paintThumbnail(g);
                paintTitle(g);
            } finally {
                g.dispose();
            }
        }

        protected void paintThumbnail(Graphics2D graphics) {
            if (thumbnail == null) return;
            int footerHeight = 32;
            int availableWidth = Math.max(1, getWidth() - 14);
            int availableHeight = Math.max(1, getHeight() - footerHeight - 12);
            double scale = Math.min((double) availableWidth / thumbnail.getWidth(null),
                    (double) availableHeight / thumbnail.getHeight(null));
            scale = Math.min(1d, scale);
            int width = Math.max(1, (int) Math.round(thumbnail.getWidth(null) * scale));
            int height = Math.max(1, (int) Math.round(thumbnail.getHeight(null) * scale));
            int x = (getWidth() - width) / 2;
            int y = 7 + Math.max(0, (availableHeight - height) / 2);
            graphics.drawImage(thumbnail, x, y, width, height, null);
        }

        protected void paintTitle(Graphics2D graphics) {
            Font font = UIManager.getFont("Label.font");
            if (font != null) graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            FontMetrics metrics = graphics.getFontMetrics();
            String title = candidate.getTitle();
            int maxWidth = Math.max(1, getWidth() - 36);
            while (title.length() > 3 && metrics.stringWidth(title) > maxWidth) {
                title = title.substring(0, title.length() - 2);
            }
            if (!title.equals(candidate.getTitle())) title += "...";
            int x = 12;
            Icon icon = candidate.getIcon();
            if (icon != null) {
                int y = getHeight() - 24 + Math.max(0, (16 - icon.getIconHeight()) / 2);
                icon.paintIcon(this, graphics, x, y);
                x += icon.getIconWidth() + 7;
            }
            graphics.drawString(title, x, getHeight() - 12);
        }
    }
}
