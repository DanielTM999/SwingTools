package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.model.WordInlineObject;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

@FunctionalInterface
public interface WordObjectPainter {
    void paint(Graphics2D graphics, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document);
}
