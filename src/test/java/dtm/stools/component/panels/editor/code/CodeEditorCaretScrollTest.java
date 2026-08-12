package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorCaretScrollTest {

    @Test
    void consumesArrowAfterMovingCaretSoScrollPaneCannotScrollAgain() {
        TestTextArea area = new TestTextArea("first\nsecond\nthird");
        KeyEvent down = new KeyEvent(
                area,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_DOWN,
                KeyEvent.CHAR_UNDEFINED
        );

        area.press(down);

        assertEquals(1, area.getCaretLine());
        assertTrue(down.isConsumed());
    }

    @Test
    void keepsViewportStillWhileCaretIsInsideMargins() {
        TestTextArea area = new TestTextArea();

        Point result = area.calculate(
                new Point(100, 200),
                new Dimension(400, 300),
                new Rectangle(250, 320, 2, 16)
        );

        assertEquals(new Point(100, 200), result);
    }

    @Test
    void scrollsOnlyAfterCaretCrossesRightAndBottomEdges() {
        TestTextArea area = new TestTextArea();

        Point result = area.calculate(
                new Point(100, 200),
                new Dimension(400, 300),
                new Rectangle(470, 495, 2, 16)
        );

        assertEquals(new Point(120, 211), result);
    }

    @Test
    void restoresMarginsWhenCaretCrossesLeftAndTopEdges() {
        TestTextArea area = new TestTextArea();

        Point result = area.calculate(
                new Point(100, 200),
                new Dimension(400, 300),
                new Rectangle(104, 190, 2, 16)
        );

        assertEquals(new Point(96, 190), result);
    }

    private static final class TestTextArea extends CodeEditorTextArea {
        private TestTextArea() {
            this("");
        }

        private TestTextArea(String text) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            setSize(2_000, 2_000);
        }

        private void press(KeyEvent event) {
            KeyAdapter handler = createKeyHandler();
            handler.keyPressed(event);
        }

        private Point calculate(Point position, Dimension extent, Rectangle caret) {
            return calculateCaretScrollPosition(position, extent, caret);
        }
    }
}
