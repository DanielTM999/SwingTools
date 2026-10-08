package dtm.stools.component.panels.editor.pdf.ui;

import java.awt.Graphics2D;

@FunctionalInterface
public interface PdfCanvasOverlay {
    void paintOverlay(Graphics2D graphics, PdfCanvas canvas);
}
