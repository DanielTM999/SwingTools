package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;

import java.awt.*;
import java.awt.event.MouseEvent;

public interface GutterLayer {
    default void onMouseClick(MouseEvent e, int line) {}
    default void onLinesInserted(int atLine, int count) {}
    default void onLinesRemoved(int atLine, int count) {}
    void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height);
}
