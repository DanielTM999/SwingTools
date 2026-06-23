package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;

import java.awt.*;

public class LineNumberLayer implements GutterLayer {

    private static final int PADDING = 8;

    @Override
    public void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height) {
        if (gutter.isBreakpointEnabled()) {
            BreakpointLayer bp = gutter.getLayer(BreakpointLayer.class);
            if (bp != null && bp.hasBreakpoint(line) && bp.isOverlay()
                    && bp.isEffectiveOverlay(gutter.getGutterWidth())) {
                return;
            }
        }
        String number = String.valueOf(line + 1);
        g.setFont(gutter.getLineNumberFont());
        FontMetrics fm = g.getFontMetrics();
        int rightEdge = x + width - gutter.getFoldStripWidth() - gutter.getBookmarkStripWidth();
        int textX = rightEdge - fm.stringWidth(number) - PADDING;
        int textY = y + fm.getAscent();
        g.setColor(gutter.getLineNumberColor());
        g.drawString(number, textX, textY);
    }

    public int getRequiredWidth(CodeEditorGutter gutter, Graphics g, int totalLines) {
        g.setFont(gutter.getLineNumberFont());
        return getRequiredWidth(gutter, g.getFontMetrics(), totalLines);
    }

    public int getRequiredWidth(CodeEditorGutter gutter, FontMetrics fm, int totalLines) {
        int digits = String.valueOf(totalLines).length();
        int charWidth = fm.charWidth('0');

        return (digits * charWidth) + gutter.getLeftMargin() + PADDING;
    }
}
