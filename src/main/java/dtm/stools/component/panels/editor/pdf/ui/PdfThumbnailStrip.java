package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.configs.UiTokens;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PdfThumbnailStrip extends JComponent implements Scrollable {
    private static final int PAD = 14, LABEL = 20, GAP = 10, MAX_WIDTH = 170;

    private final PdfEditor editor;
    private int hover = -1, dragFrom = -1, dropIndex = -1;
    private Point pressed;
    private boolean dragging;

    public PdfThumbnailStrip(PdfEditor editor) {
        this.editor = editor;
        setName("pdf.thumbnails");
        setOpaque(true);
        setFocusable(false);
        setAutoscrolls(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { setHover(pageAt(event.getPoint())); }
            @Override public void mouseExited(MouseEvent event) { setHover(-1); }
            @Override public void mousePressed(MouseEvent event) {
                int page = pageAt(event.getPoint());
                if (SwingUtilities.isRightMouseButton(event) || event.isPopupTrigger()) {
                    if (page >= 0) {
                        if (!editor.isPageSelected(page)) editor.clickPage(page, false, false);
                        showMenu(event, page);
                    }
                    return;
                }
                pressed = event.getPoint();
                dragFrom = page;
                dragging = false;
            }
            @Override public void mouseDragged(MouseEvent event) {
                if (dragFrom < 0 || pressed == null || editor.isReadOnly()) return;
                if (!dragging && pressed.distance(event.getPoint()) < 6) return;
                dragging = true;
                dropIndex = insertionAt(event.getY());
                scrollRectToVisible(new Rectangle(event.getX(), event.getY(), 1, 1));
                repaint();
            }
            @Override public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    int page = pageAt(event.getPoint());
                    if (page >= 0) showMenu(event, page);
                    return;
                }
                int from = dragFrom;
                boolean moved = dragging;
                int drop = dropIndex;
                dragFrom = -1; dropIndex = -1; dragging = false; pressed = null;
                repaint();
                if (from < 0) return;
                if (!moved) { editor.clickPage(from, event.isControlDown() || event.isMetaDown(), event.isShiftDown()); return; }
                int target = drop > from ? drop - 1 : drop;
                if (drop >= 0 && target != from) editor.perform(() -> editor.movePage(from, target));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void setHover(int page) {
        if (hover == page) return;
        hover = page;
        repaint();
    }

    private void showMenu(MouseEvent event, int page) {
        JPopupMenu menu = new JPopupMenu();
        for (Action action : editor.thumbnailActions(page)) {
            if (action == null) menu.addSeparator();
            else menu.add(action).setIcon(PdfIcon.small(PdfRibbon.iconOf(action)));
        }
        menu.show(this, event.getX(), event.getY());
    }

    private int thumbWidth() {
        int available = (getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth()) - 2 * PAD;
        return Math.max(60, Math.min(MAX_WIDTH, available));
    }

    private Rectangle thumb(int page, int width, int y) {
        PdfPageGeometry geometry = editor.getPageGeometry(page, 1);
        int height = (int) Math.round(width * geometry.aspect());
        int available = getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth();
        return new Rectangle(Math.max(PAD, (available - width) / 2), y, width, height);
    }

    public Rectangle cardBounds(int page) {
        int width = thumbWidth(), y = PAD;
        for (int index = 0; index < editor.getPageCount(); index++) {
            Rectangle box = thumb(index, width, y);
            if (index == page) return new Rectangle(box.x - 6, box.y - 6, box.width + 12, box.height + LABEL + 8);
            y += box.height + LABEL + GAP;
        }
        return null;
    }

    private int pageAt(Point point) {
        int width = thumbWidth(), y = PAD;
        for (int index = 0; index < editor.getPageCount(); index++) {
            Rectangle box = thumb(index, width, y);
            Rectangle card = new Rectangle(box.x - 6, box.y - 6, box.width + 12, box.height + LABEL + 8);
            if (card.contains(point)) return index;
            y += box.height + LABEL + GAP;
        }
        return -1;
    }

    private int insertionAt(int pointY) {
        int width = thumbWidth(), y = PAD, count = editor.getPageCount();
        for (int index = 0; index < count; index++) {
            Rectangle box = thumb(index, width, y);
            if (pointY < box.y + box.height / 2) return index;
            y += box.height + LABEL + GAP;
        }
        return count;
    }

    public void scrollToCurrent() {
        Rectangle card = cardBounds(editor.getCurrentPage());
        if (card != null) scrollRectToVisible(new Rectangle(card.x, card.y - 8, card.width, card.height + 16));
    }

    @Override public Dimension getPreferredSize() {
        int width = thumbWidth(), y = PAD;
        for (int index = 0; index < editor.getPageCount(); index++) y += thumb(index, width, y).height + LABEL + GAP;
        return new Dimension(width + 2 * PAD, y + PAD);
    }

    @Override protected void paintComponent(Graphics original) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle clip = g.getClipBounds() == null ? new Rectangle(0, 0, getWidth(), getHeight()) : g.getClipBounds();
            g.setColor(UiTokens.surfaceAlt());
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
            int width = thumbWidth(), y = PAD, current = editor.getCurrentPage();
            double device = getGraphicsConfiguration() == null ? 1 : Math.max(1, getGraphicsConfiguration().getDefaultTransform().getScaleX());
            Font font = UiTokens.fontSmall();
            for (int page = 0; page < editor.getPageCount(); page++) {
                Rectangle box = thumb(page, width, y);
                y += box.height + LABEL + GAP;
                Rectangle card = new Rectangle(box.x - 6, box.y - 6, box.width + 12, box.height + LABEL + 8);
                if (!card.intersects(clip)) continue;
                boolean selected = page == current;
                boolean marked = editor.isPageSelected(page) && editor.getSelectedPages().size() > 1;
                if (marked && !selected) {
                    g.setColor(UiTokens.overlay(UiTokens.accent(), .10f));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 10, 10);
                }
                if (selected || page == hover) {
                    g.setColor(selected ? UiTokens.overlay(UiTokens.accent(), .16f) : UiTokens.overlay(UiTokens.foreground(), .06f));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 10, 10);
                }
                g.setColor(new Color(0, 0, 0, UiTokens.isDarkTheme() ? 60 : 28));
                g.fillRoundRect(box.x + 1, box.y + 2, box.width, box.height, 4, 4);
                g.setColor(Color.WHITE);
                g.fillRect(box.x, box.y, box.width, box.height);
                int target = page;
                BufferedImage image = editor.getRenderer().thumbnail(page, (int) Math.round(width * device), () -> repaintPage(target));
                if (image != null) g.drawImage(image, box.x, box.y, box.width, box.height, null);
                else {
                    g.setColor(new Color(0xE9ECF1));
                    for (int line = 0; line < 6; line++)
                        g.fillRect(box.x + box.width / 8, box.y + box.height / 8 + line * box.height / 9, box.width * (line % 3 == 2 ? 2 : 3) / 4, 3);
                }
                g.setStroke(new BasicStroke(selected || marked ? 2.2f : 1f));
                g.setColor(selected || marked ? UiTokens.accent() : UiTokens.border());
                g.drawRect(box.x, box.y, box.width - 1, box.height - 1);
                g.setFont(selected ? font.deriveFont(Font.BOLD) : font);
                g.setColor(selected ? UiTokens.accent() : UiTokens.muted());
                String label = String.valueOf(page + 1);
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(label, box.x + (box.width - metrics.stringWidth(label)) / 2, box.y + box.height + 15);
            }
            if (dragging && dropIndex >= 0) {
                int lineY = PAD - GAP / 2;
                for (int page = 0; page < dropIndex; page++) lineY += thumb(page, width, 0).height + LABEL + GAP;
                g.setColor(UiTokens.accent());
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int available = getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth();
                g.drawLine(PAD, lineY, available - PAD, lineY);
            }
        } finally { g.dispose(); }
    }

    private void repaintPage(int page) {
        Rectangle card = cardBounds(page);
        if (card == null) repaint(); else repaint(card);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(200, 400); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? Math.max(24, visible.height - 40) : visible.width;
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

}
