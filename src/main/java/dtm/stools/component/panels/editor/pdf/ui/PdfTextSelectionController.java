package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfTextContent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class PdfTextSelectionController extends MouseAdapter implements PdfCanvasOverlay {
    private final PdfEditor editor;
    private final PdfCanvas canvas;
    private PdfTextContent content = PdfTextContent.empty();
    private int page = -1, anchor, caret, generation;
    private Point press, current;
    private boolean word;

    public PdfTextSelectionController(PdfEditor editor, PdfCanvas canvas) {
        this.editor = editor;
        this.canvas = canvas;
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addOverlay(this);
    }

    public String selectedText() {
        return content.text().substring(Math.min(anchor, caret), Math.max(anchor, caret));
    }

    public void clear() {
        generation++;
        content = PdfTextContent.empty();
        page = -1;
        anchor = caret = 0;
        press = current = null;
        canvas.repaint();
    }

    private boolean viewing() { return PdfEditor.TOOL_VIEW.equals(editor.getActiveTool()); }

    @Override
    public void mousePressed(MouseEvent event) {
        if (!viewing()) return;
        if (SwingUtilities.isRightMouseButton(event)) {
            if (!selectedText().isEmpty()) {
                JPopupMenu menu = new JPopupMenu();
                JMenuItem copy = new JMenuItem("Copiar texto");
                copy.addActionListener(ignored -> editor.copySelection());
                menu.add(copy);
                menu.show(canvas, event.getX(), event.getY());
            }
            return;
        }
        if (!SwingUtilities.isLeftMouseButton(event)) return;
        clear();
        editor.refreshTextSelection();
        page = canvas.getPageLayout().pageAt(event.getPoint());
        if (page < 0) return;
        press = current = event.getPoint();
        word = event.getClickCount() >= 2;
        int request = generation;
        editor.textContentAsync(page, value -> {
            if (generation != request || !viewing()) return;
            content = value;
            update();
        });
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (!viewing() || press == null) return;
        current = event.getPoint();
        canvas.scrollRectToVisible(new Rectangle(current.x - 8, current.y - 8, 16, 16));
        update();
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (!viewing() || press == null || !SwingUtilities.isLeftMouseButton(event)) return;
        current = event.getPoint();
        update();
    }

    private int offset(Point point) {
        double best = Double.MAX_VALUE;
        int offset = 0;
        for (var glyph : content.glyphs()) {
            var box = canvas.getPageLayout().toView(page, glyph.bounds());
            double dy = Math.max(0, Math.max(box.getMinY() - point.y, point.y - box.getMaxY()));
            double dx = Math.max(0, Math.max(box.getMinX() - point.x, point.x - box.getMaxX()));
            double distance = dy * dy * 100 + dx * dx;
            if (distance < best) {
                best = distance;
                offset = point.x < box.getCenterX() ? glyph.start() : glyph.end();
            }
        }
        return offset;
    }

    private void update() {
        if (press == null || !canvas.getPageLayout().contains(page)) return;
        anchor = offset(press);
        caret = press.distance(current) < 3 ? anchor : offset(current);
        if (word && !content.text().isEmpty()) {
            anchor = Math.min(anchor, content.text().length() - 1);
            caret = anchor;
            while (anchor > 0 && Character.isLetterOrDigit(content.text().charAt(anchor - 1))) anchor--;
            while (caret < content.text().length() && Character.isLetterOrDigit(content.text().charAt(caret))) caret++;
        }
        editor.refreshTextSelection();
        canvas.repaint();
    }

    @Override
    public void paintOverlay(Graphics2D graphics, PdfCanvas target) {
        if (!viewing() || !canvas.getPageLayout().contains(page)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.clip(canvas.getPageLayout().bounds(page));
            g.setColor(new Color(65, 135, 245, 95));
            for (var glyph : content.glyphs())
                if (glyph.end() > Math.min(anchor, caret) && glyph.start() < Math.max(anchor, caret))
                    g.fill(canvas.getPageLayout().toView(page, glyph.bounds()));
        } finally { g.dispose(); }
    }
}
