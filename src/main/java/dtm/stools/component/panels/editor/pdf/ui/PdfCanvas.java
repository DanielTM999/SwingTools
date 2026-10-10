package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.configs.UiTokens;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PdfCanvas extends JComponent implements Scrollable {
    private final PdfEditor editor;
    private final List<PdfCanvasOverlay> overlays = new ArrayList<>();
    private PdfPageLayout layout = PdfPageLayout.empty();
    private String placeholder;

    public PdfCanvas(PdfEditor editor) {
        this.editor = editor;
        setName("pdf.canvas");
        setOpaque(true);
        setFocusable(true);
        setLayout(null);
        setAutoscrolls(true);
    }

    public PdfEditor getEditor() { return editor; }
    public PdfPageLayout getPageLayout() { return layout; }

    public void setPageLayout(PdfPageLayout value) {
        PdfPageLayout previous = layout;
        layout = value == null ? PdfPageLayout.empty() : value;
        if (!previous.size().equals(layout.size())) revalidate();
        repaint();
    }

    public void setPlaceholder(String value) {
        placeholder = value;
        repaint();
    }

    public void addOverlay(PdfCanvasOverlay overlay) { overlays.add(overlay); repaint(); }
    public void removeOverlay(PdfCanvasOverlay overlay) { overlays.remove(overlay); repaint(); }

    public void repaintPage(int page) {
        Rectangle box = layout.bounds(page);
        if (box == null) repaint();
        else repaint(box.x - 12, box.y - 12, box.width + 24, box.height + 24);
    }

    public float renderResolution() {
        double device = 1;
        if (getGraphicsConfiguration() != null) {
            AffineTransform transform = getGraphicsConfiguration().getDefaultTransform();
            device = Math.max(1, transform.getScaleX());
        }
        return (float) (96 * editor.getZoom() * device);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = layout.size();
        return size.width == 0 ? new Dimension(600, 800) : size;
    }

    @Override
    protected void paintComponent(Graphics original) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setColor(background());
            Rectangle clip = g.getClipBounds() == null ? new Rectangle(0, 0, getWidth(), getHeight()) : g.getClipBounds();
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            float resolution = renderResolution();
            for (int page = 0; page < layout.pageCount(); page++) {
                Rectangle box = layout.bounds(page);
                if (box == null || !box.intersects(clip.x - 16, clip.y - 16, clip.width + 32, clip.height + 32)) continue;
                paintPage(g, page, box, resolution);
            }
            if (layout.pageCount() == 0 && placeholder != null) {
                g.setColor(UiTokens.muted());
                g.drawString(placeholder, 24, 36);
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (PdfCanvasOverlay overlay : List.copyOf(overlays)) overlay.paintOverlay(g, this);
        } finally { g.dispose(); }
    }

    private void paintPage(Graphics2D g, int page, Rectangle box, float resolution) {
        for (int i = 4; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, UiTokens.isDarkTheme() ? 26 : 12 + (4 - i) * 4));
            g.fillRoundRect(box.x - i + 1, box.y - i + 3, box.width + 2 * i - 2, box.height + 2 * i - 2, 2 * i, 2 * i);
        }
        g.setColor(Color.WHITE);
        g.fillRect(box.x, box.y, box.width, box.height);
        BufferedImage image = editor.getRenderer().page(page, resolution, () -> repaintPage(page));
        if (image != null && similar(image, box)) g.drawImage(image, box.x, box.y, box.width, box.height, null);
        else if (image == null) paintLoading(g, box);
        g.setColor(UiTokens.isDarkTheme() ? new Color(0, 0, 0, 90) : new Color(0, 0, 0, 38));
        g.drawRect(box.x, box.y, box.width - 1, box.height - 1);
    }

    private static boolean similar(BufferedImage image, Rectangle box) {
        double expected = box.height / (double) Math.max(1, box.width);
        double actual = image.getHeight() / (double) Math.max(1, image.getWidth());
        return Math.abs(expected - actual) < .03;
    }

    private void paintLoading(Graphics2D g, Rectangle box) {
        g.setColor(new Color(0xEEF1F5));
        int y = box.y + Math.max(24, box.height / 12);
        for (int line = 0; line < 8 && y < box.getMaxY() - 30; line++) {
            int width = (int) (box.width * (line % 3 == 2 ? .45 : .72));
            g.fillRoundRect(box.x + box.width / 10, y, width, Math.max(6, box.height / 90), 6, 6);
            y += Math.max(18, box.height / 28);
        }
    }

    private Color background() {
        return UiTokens.isDarkTheme() ? new Color(0x2A2D33) : new Color(0xE9ECF1);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 32; }
    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(32, (orientation == javax.swing.SwingConstants.VERTICAL ? visible.height : visible.width) - 48);
    }
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport viewport && viewport.getWidth() >= layout.size().width;
    }
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() >= layout.size().height;
    }
}
