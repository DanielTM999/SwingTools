package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;

import java.awt.Font;
import java.awt.FontMetrics;

interface LineRunVisitor {
    boolean visit(int startCol, int endCol, int visualCol, String expandedRun, TextStyle style, Font font, FontMetrics fontMetrics, int x, int runWidth);
}
