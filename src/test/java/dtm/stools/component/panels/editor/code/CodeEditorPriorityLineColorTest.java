package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodeEditorPriorityLineColorTest {

    @Test
    void paintsPriorityLineAboveCurrentCaretLine() {
        CodeEditorTextArea area = new CodeEditorTextArea("current line");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setSize(500, 120);
        area.setCaretPosition(0, 0);
        area.setCurrentLineColor(Color.BLUE);
        area.setPriorityLineColor(0, Color.RED);
        BufferedImage image = new BufferedImage(500, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        area.paint(graphics);

        graphics.dispose();
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        assertEquals(Color.RED.getRGB(), image.getRGB(300, lineHeight / 2));
    }

    @Test
    void paintsPriorityLineAboveSelection() {
        CodeEditorTextArea area = new CodeEditorTextArea("                    ");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setSize(500, 120);
        area.setSelection(0, 0, 0, 20);
        area.setPriorityLineColor(0, Color.RED);
        BufferedImage image = new BufferedImage(500, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        area.paint(graphics);

        graphics.dispose();
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        assertEquals(Color.RED.getRGB(), image.getRGB(50, lineHeight / 2));
    }
}
