package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

public class FoldingLayer implements GutterLayer {

    @Getter
    @Setter
    private Color chevronColor;

    @Getter
    @Setter
    private boolean chevronVisibleOnHoverOnly = true;

    @Getter
    @Setter
    private int hoverLine = -1;

    public void clearHover() {
        hoverLine = -1;
    }

    @Override
    public void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height) {
        CodeEditorTextArea textArea = gutter.getTextArea();
        if (textArea == null || !textArea.isFoldingEnabled()) return;
        if (!textArea.isFoldAnchorLine(line)) return;
        if (chevronVisibleOnHoverOnly && hoverLine != line) return;

        FoldRegion region = textArea.getFoldRegionStartingAt(line);
        if (region == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int chevronW = gutter.getFoldStripWidth();
            if (chevronW <= 0) return;
            int cx = x + width - gutter.getFoldStripRightInset() - chevronW;
            int size = Math.max(6, Math.min(chevronW - 4, height - 6));
            int centerX = cx + (chevronW - size) / 2;
            int centerY = y + (height - size) / 2;

            Color color = chevronColor != null
                    ? chevronColor
                    : resolveColor();
            g2.setColor(color);

            if (region.folded()) {
                Polygon p = new Polygon();
                p.addPoint(centerX, centerY);
                p.addPoint(centerX + size, centerY + size / 2);
                p.addPoint(centerX, centerY + size);
                g2.fillPolygon(p);
            } else {
                Polygon p = new Polygon();
                p.addPoint(centerX, centerY);
                p.addPoint(centerX + size, centerY);
                p.addPoint(centerX + size / 2, centerY + size);
                g2.fillPolygon(p);
            }
        } finally {
            g2.dispose();
        }
    }

    private Color resolveColor() {
        Color c = UIManager.getColor("Label.disabledForeground");
        if (c == null) c = new Color(128, 128, 128);
        return c;
    }
}
