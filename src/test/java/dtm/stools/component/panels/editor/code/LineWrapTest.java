package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.junit.jupiter.api.Assertions.*;

class LineWrapTest {

    private static final String LONG = "    the quick brown fox jumps over the lazy dog and keeps running far beyond the visible area of the editor";
    private static final String TEXT = "short\n" + LONG + "\nend";

    @Test
    void disabledKeepsOneRowPerLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            int lh = area.lineHeight();
            assertFalse(area.isLineWrapEnabled());
            assertEquals(1, area.wrapRowCount(1));
            assertEquals(3 * lh, area.totalContentHeight());
            assertTrue(area.getPreferredSize().width > 240);
        });
    }

    @Test
    void enabledWrapsLongLinesOnlyVisually() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            int lh = area.lineHeight();
            int rows = area.wrapRowCount(1);

            assertTrue(rows > 1);
            assertEquals(1, area.wrapRowCount(0));
            assertEquals(3, area.lineCount());
            assertEquals(TEXT, area.text());
            assertEquals((2 + rows) * lh, area.totalContentHeight());
            assertEquals(area.yOfBufferLine(1) + rows * lh, area.yOfBufferLine(2));
            assertEquals(1, area.bufferLineAtY(area.yOfBufferLine(1) + lh + 1));
            assertEquals(240, area.getPreferredSize().width);
            assertRowsFit(area, 1);
        });
    }

    @Test
    void wrapsAfterWhitespaceAndKeepsIndentationOnContinuation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            int start = area.wrapRowStartColumn(1, 1);
            assertTrue(start > 4);
            assertEquals(' ', LONG.charAt(start - 1));
            FontMetrics fm = area.metrics();
            assertEquals(CodeEditorTextAreaState.TEXT_LEFT_MARGIN + fm.stringWidth("    "),
                    area.visualXForColumn(1, start));
            assertEquals(area.yOfBufferLine(1) + area.lineHeight(), area.yOfColumn(1, start));
        });
    }

    @Test
    void clickOnContinuationRowResolvesColumnOfThatRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            int start = area.wrapRowStartColumn(1, 1);
            int next = area.wrapRowCount(1) > 2 ? area.wrapRowStartColumn(1, 2) : LONG.length() + 1;
            int y = area.yOfBufferLine(1) + area.lineHeight() + 2;

            int[] first = area.positionFromPoint(0, y);
            assertArrayEquals(new int[]{1, start}, first);

            int[] far = area.positionFromPoint(10_000, y);
            assertEquals(1, far[0]);
            assertTrue(far[1] >= start && far[1] < next);
        });
    }

    @Test
    void editingWithoutLineBreakRewrapsLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            int lh = area.lineHeight();
            int before = area.totalContentHeight();

            area.insertText(area.offsetOf(0, 5), " grows until it no longer fits inside the narrow viewport at all");

            assertTrue(area.wrapRowCount(0) > 1);
            assertEquals(before + (area.wrapRowCount(0) - 1) * lh, area.totalContentHeight());
            assertRowsFit(area, 0);
        });
    }

    @Test
    void viewportResizeRewraps() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            int narrowRows = area.wrapRowCount(1);

            area.viewport.setSize(new Dimension(480, 300));

            assertTrue(area.wrapRowCount(1) < narrowRows);
            assertEquals((2 + area.wrapRowCount(1)) * area.lineHeight(), area.totalContentHeight());
            assertEquals(480, area.getPreferredSize().width);
        });
    }

    @Test
    void arrowDownMovesByVisualRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            area.setLineWrapEnabled(true);
            area.setCaretPosition(1, 6);
            int start = area.wrapRowStartColumn(1, 1);

            area.press(KeyEvent.VK_DOWN);
            assertEquals(1, area.getCaretLine());
            assertTrue(area.getCaretCol() >= start);
            assertEquals(1, area.wrapRowOfColumn(1, area.getCaretCol()));

            area.press(KeyEvent.VK_UP);
            assertEquals(1, area.getCaretLine());
            assertEquals(6, area.getCaretCol());

            area.press(KeyEvent.VK_UP);
            assertEquals(0, area.getCaretLine());
        });
    }

    @Test
    void disablingRestoresSingleRowLayout() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT, 240);
            Dimension original = area.getPreferredSize();
            area.setLineWrapEnabled(true);
            area.getPreferredSize();
            area.setLineWrapEnabled(false);

            assertEquals(1, area.wrapRowCount(1));
            assertEquals(3 * area.lineHeight(), area.totalContentHeight());
            assertEquals(original, area.getPreferredSize());
        });
    }

    private static void assertRowsFit(Area area, int line) {
        FontMetrics fm = area.metrics();
        String text = area.lineText(line);
        int limit = area.viewport.getWidth();
        for (int row = 0; row < area.wrapRowCount(line); row++) {
            int end = area.wrappedRowEndX(line, text, row, fm);
            assertTrue(end <= limit, "row " + row + " ends at " + end + " > " + limit);
        }
    }

    private static class Area extends CodeEditorTextArea {
        final JViewport viewport = new JViewport();

        Area(String text, int width) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            viewport.setView(this);
            viewport.setSize(new Dimension(width, 300));
        }

        String text() { return buffer.getText(); }
        int lineCount() { return buffer.lineCount(); }
        String lineText(int line) { return buffer.lineAt(line); }
        int offsetOf(int line, int col) { return buffer.offsetOfLine(line) + col; }
        FontMetrics metrics() { return fontMetricsFor(getFont()); }
        int lineHeight() { return metrics().getHeight(); }

        void press(int keyCode) {
            KeyEvent event = new KeyEvent(this, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
            for (KeyListener listener : getKeyListeners()) {
                listener.keyPressed(event);
            }
        }
    }
}
