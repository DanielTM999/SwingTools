package dtm.stools.component.panels.editor.word.layout;

import dtm.stools.component.panels.editor.word.model.WordPlacement;
import java.awt.geom.Rectangle2D;

interface WordLayoutFlow {
    float y();
    void setY(float y);
    float left();
    float width();
    float top();
    boolean paginated();
    boolean atTop();
    float remaining();
    default boolean fits(float height) { return height <= remaining() + 0.01f; }
    void breakPage(boolean columnOnly);
    float[] slot(float top, float height, float x, float width);
    void addLine(WordLayout.Line line);
    void addObject(WordLayout.ObjectBox box);
    void addDecoration(WordLayout.Decoration decoration);
    void addCell(WordLayout.CellBox cell);
    void addFloat(Rectangle2D.Float bounds, WordPlacement.Wrap wrap);
    void noteReferenced(String noteId);
    int pageNumber();
    WordLayout.Region region();
}
