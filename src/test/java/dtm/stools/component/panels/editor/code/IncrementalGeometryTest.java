package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.prototype.folding.FoldRange;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalGeometryTest {

    private static final String TEXT = "short\n    indented line\nthe longest line of the whole document\n\tx\nend";

    @Test
    void typingWithoutLineBreaksKeepsVerticalGeometry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT);
            area.yOfBufferLine(0);
            int[] lineY = area.cachedLineY;
            area.getPreferredSize();

            area.insertText(area.offsetOf(1, 2), "abc");
            area.deleteText(area.offsetOf(0, 0), area.offsetOf(0, 2));
            area.getPreferredSize();

            assertSame(lineY, area.cachedLineY);
            assertEquals(new Area(area.text()).getPreferredSize(), area.getPreferredSize());
        });
    }

    @Test
    void preferredWidthMatchesFreshEditorAfterEachEdit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT);
            assertSameMetrics(area);

            area.insertText(area.offsetOf(2, 10), " and now it grows even longer than before");
            assertSameMetrics(area);

            area.deleteText(area.offsetOf(2, 0), area.offsetOf(3, 0));
            assertSameMetrics(area);

            area.performUndo();
            assertSameMetrics(area);

            area.performRedo();
            assertSameMetrics(area);

            area.insertText(area.offsetOf(1, 8), "\n  ");
            assertSameMetrics(area);

            area.deleteText(area.offsetOf(1, area.lineText(1).length()), area.offsetOf(2, 0));
            assertSameMetrics(area);

            area.performUndo();
            area.performUndo();
            assertSameMetrics(area);

            area.setText("a\nbb\nccc");
            assertSameMetrics(area);
        });
    }

    @Test
    void preferredWidthMatchesFreshEditorWithStyledRanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area(TEXT);
            area.replaceStyledRanges(boldLongestLine(area));
            area.getPreferredSize();

            area.insertText(area.offsetOf(0, 5), " is no longer that short at all, it is the longest");
            area.replaceStyledRanges(boldLongestLine(area));

            Area fresh = new Area(area.text());
            fresh.replaceStyledRanges(boldLongestLine(fresh));
            assertEquals(fresh.getPreferredSize(), area.getPreferredSize());
        });
    }

    @Test
    void foldedLayoutFollowsLineInsertionsAndRemovals() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String text = "l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7";
            Area area = folded(text, 2, 4);

            area.insertText(area.offsetOf(0, 0), "new\n");
            assertSameLayout(folded(area.text(), 3, 5), area);

            area.insertText(area.offsetOf(0, 1), "x");
            assertSameLayout(folded(area.text(), 3, 5), area);

            area.deleteText(area.offsetOf(0, 0), area.offsetOf(1, 0));
            assertSameLayout(folded(text, 2, 4), area);
        });
    }

    private static Area folded(String text, int startLine, int endLine) {
        Area area = new Area(text);
        area.setCaretPosition(area.lineCount() - 1, 0);
        area.setFoldingEnabled(true);
        area.setFoldRanges(List.of(new FoldRange(startLine, endLine, "block", true)));
        return area;
    }

    private static List<StyledRange> boldLongestLine(Area area) {
        int longest = 0;
        for (int i = 1; i < area.lineCount(); i++) {
            if (area.lineText(i).length() > area.lineText(longest).length()) longest = i;
        }
        int start = area.offsetOf(longest, 0);
        TextStyle bold = TextStyle.builder().bold(true).build();
        return List.of(new StyledRange(bold, start, start + area.lineText(longest).length()));
    }

    private static void assertSameMetrics(Area area) {
        Area fresh = new Area(area.text());
        assertEquals(fresh.getPreferredSize(), area.getPreferredSize(), area.text());
        assertEquals(fresh.indentUnit(), area.indentUnit(), area.text());
    }

    private static void assertSameLayout(Area expected, Area actual) {
        assertEquals(expected.totalContentHeight(), actual.totalContentHeight());
        for (int line = 0; line < expected.lineCount(); line++) {
            assertEquals(expected.isLineHidden(line), actual.isLineHidden(line), "hidden " + line);
            assertEquals(expected.yOfBufferLine(line), actual.yOfBufferLine(line), "y " + line);
        }
        for (int y = 0; y < expected.totalContentHeight(); y += 3) {
            assertEquals(expected.bufferLineAtY(y), actual.bufferLineAtY(y), "line at y " + y);
        }
    }

    private static class Area extends CodeEditorTextArea {
        Area(String text) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        }

        String text() { return buffer.getText(); }
        int lineCount() { return buffer.lineCount(); }
        String lineText(int line) { return buffer.lineAt(line); }
        int offsetOf(int line, int col) { return buffer.offsetOfLine(line) + col; }
    }
}
