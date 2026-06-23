package dtm.stools.component.panels.editor.code.api;

import java.awt.FontMetrics;
import java.awt.Graphics2D;

@FunctionalInterface
public interface WordHoverPainter {

    void paint(Graphics2D g2, FontMetrics fm, WordHoverContext context);
}
