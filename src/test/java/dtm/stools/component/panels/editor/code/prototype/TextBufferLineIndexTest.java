package dtm.stools.component.panels.editor.code.prototype;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TextBufferLineIndexTest {

    private static final String[] FRAGMENTS = {"", "a", "xyz", "\n", "\n\n", "a\nb", "\nfoo\n", "lorem ipsum", "\n\n\nz"};

    @Test
    void randomEditsKeepLineIndexInSyncWithText() {
        Random random = new Random(42);
        TextBuffer buffer = new TextBuffer("first\nsecond\n\nthird");
        int[] observedLineCount = {buffer.lineCount()};
        buffer.addLineListener((start, removed, inserted) -> observedLineCount[0] += inserted - removed);

        for (int step = 0; step < 4000; step++) {
            int length = buffer.length();
            int op = random.nextInt(10);
            if (op < 4) {
                buffer.insert(random.nextInt(length + 1), fragment(random));
            } else if (op < 6 && length > 0) {
                int start = random.nextInt(length);
                buffer.delete(start, start + random.nextInt(Math.min(12, length - start) + 1));
            } else if (op < 8) {
                int start = random.nextInt(length + 1);
                buffer.replace(start, start + random.nextInt(Math.min(6, length - start) + 1), fragment(random));
            } else if (op == 8) {
                buffer.undo();
            } else {
                buffer.redo();
            }
            assertMatchesNaiveIndex(buffer);
            assertEquals(buffer.lineCount(), observedLineCount[0], "step " + step);
        }

        buffer.delete(0, buffer.length());
        assertMatchesNaiveIndex(buffer);
        assertEquals(1, observedLineCount[0]);
        while (buffer.canUndo()) {
            buffer.undo();
            assertMatchesNaiveIndex(buffer);
        }
        while (buffer.canRedo()) {
            buffer.redo();
            assertMatchesNaiveIndex(buffer);
        }
        assertEquals(buffer.lineCount(), observedLineCount[0]);
    }

    @Test
    void listenerReportsStartLineAndLineBreakCounts() {
        TextBuffer buffer = new TextBuffer("a\nb\nc\nd");
        buffer.lineCount();
        List<int[]> events = new ArrayList<>();
        buffer.addLineListener((start, removed, inserted) -> events.add(new int[]{start, removed, inserted}));

        buffer.insert(buffer.offsetOfLine(2) + 1, "X");
        assertArrayEquals(new int[]{2, 0, 0}, events.removeFirst());

        buffer.insert(buffer.offsetOfLine(1), "1\n2\n");
        assertArrayEquals(new int[]{1, 0, 2}, events.removeFirst());

        buffer.delete(buffer.offsetOfLine(0), buffer.offsetOfLine(3));
        assertArrayEquals(new int[]{0, 3, 0}, events.removeFirst());

        buffer.replace(0, 1, "p\nq");
        assertArrayEquals(new int[]{0, 0, 1}, events.removeFirst());

        int linesBefore = buffer.lineCount();
        buffer.setText("only");
        assertArrayEquals(new int[]{0, linesBefore - 1, 0}, events.removeFirst());
        assertTrue(events.isEmpty());
    }

    @Test
    void indexIsBuiltLazilyAndSurvivesSetText() {
        TextBuffer buffer = new TextBuffer("x\ny");
        buffer.insert(0, "\n");
        assertMatchesNaiveIndex(buffer);
        buffer.setText("1\n2\n3\n");
        assertMatchesNaiveIndex(buffer);
        buffer.insert(buffer.length(), "4");
        assertMatchesNaiveIndex(buffer);
        assertEquals("4", buffer.lineAt(3));
    }

    private static String fragment(Random random) {
        return FRAGMENTS[random.nextInt(FRAGMENTS.length)];
    }

    private static void assertMatchesNaiveIndex(TextBuffer buffer) {
        String text = buffer.getText();
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') starts.add(i + 1);
        }
        assertEquals(starts.size(), buffer.lineCount(), text);
        for (int line = 0; line < starts.size(); line++) {
            int start = starts.get(line);
            int end = line + 1 < starts.size() ? starts.get(line + 1) - 1 : text.length();
            assertEquals(start, buffer.offsetOfLine(line));
            assertEquals(text.substring(start, end), buffer.lineAt(line));
        }
        for (int offset = 0; offset <= text.length(); offset++) {
            int expected = 0;
            for (int line = 0; line < starts.size(); line++) {
                if (starts.get(line) <= offset) expected = line;
            }
            assertEquals(expected, buffer.lineOfOffset(offset), "offset " + offset);
        }
    }
}
