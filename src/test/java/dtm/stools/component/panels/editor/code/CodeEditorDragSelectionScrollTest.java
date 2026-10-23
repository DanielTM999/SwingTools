package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import static org.junit.jupiter.api.Assertions.*;

class CodeEditorDragSelectionScrollTest {

    private static final int VIEW_WIDTH = 400;

    @Test
    void holdingMouseInsideRightMarginDoesNotKeepScrolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("a".repeat(5000) + "\nsecond");
            int y = area.yOfBufferLine(0) + 2;
            area.press(50, y);
            assertEquals(0, area.viewX());

            for (int i = 0; i < 20; i++) {
                area.drag(area.viewX() + VIEW_WIDTH - 20, y);
            }

            assertEquals(0, area.viewX());
            assertTrue(area.hasSelection());
            assertEquals(area.positionFromPoint(VIEW_WIDTH - 20, y)[1], area.getCaretCol());
        });
    }

    @Test
    void holdingMouseInsideLeftMarginDoesNotKeepScrolling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("a".repeat(5000) + "\nsecond");
            int y = area.yOfBufferLine(0) + 2;
            area.viewport.setViewPosition(new Point(2000, 0));
            area.press(2000 + 200, y);
            assertEquals(2000, area.viewX());

            for (int i = 0; i < 20; i++) {
                area.drag(area.viewX() + 3, y);
            }

            assertEquals(2000, area.viewX());
            assertTrue(area.hasSelection());
        });
    }

    @Test
    void draggingPastRightEdgeScrollsJustEnoughToShowCaret() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("a".repeat(5000) + "\nsecond");
            int y = area.yOfBufferLine(0) + 2;
            area.press(50, y);

            int previous = area.viewX();
            for (int i = 0; i < 5; i++) {
                area.drag(area.viewX() + VIEW_WIDTH + 30, y);
                int current = area.viewX();
                assertTrue(current > previous, "drag " + i + " should advance past " + previous);
                int caretX = area.visualXForColumn(0, area.getCaretCol());
                assertTrue(caretX >= current, "caret left of view");
                assertTrue(caretX + CodeEditorTextAreaState.CARET_WIDTH <= current + VIEW_WIDTH, "caret right of view");
                previous = current;
            }
        });
    }

    @Test
    void draggingPastBottomEdgeStillScrollsVertically() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 200; i++) {
                text.append("line ").append(i).append('\n');
            }
            Area area = new Area(text.toString());
            area.press(20, 5);

            area.drag(20, area.viewY() + 300 + 40);

            assertTrue(area.viewY() > 0);
            assertTrue(area.getCaretLine() > 0);
            assertTrue(area.hasSelection());
        });
    }

    @Test
    void clickNearRightEdgeKeepsCaretMargin() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Area area = new Area("a".repeat(5000) + "\nsecond");
            int y = area.yOfBufferLine(0) + 2;

            area.press(VIEW_WIDTH - 20, y);

            int caretX = area.visualXForColumn(0, area.getCaretCol());
            assertTrue(area.viewX() > 0);
            assertTrue(caretX + CodeEditorTextAreaState.CARET_WIDTH <= area.viewX() + VIEW_WIDTH - 40);
        });
    }

    private static class Area extends CodeEditorTextArea {
        final JViewport viewport = new JViewport();

        Area(String text) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            viewport.setView(this);
            viewport.setSize(new Dimension(VIEW_WIDTH, 300));
            setSize(getPreferredSize());
        }

        int viewX() { return viewport.getViewPosition().x; }
        int viewY() { return viewport.getViewPosition().y; }

        void press(int x, int y) {
            MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, 0,
                    InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
            for (MouseListener listener : getMouseListeners()) {
                listener.mousePressed(event);
            }
        }

        void drag(int x, int y) {
            MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_DRAGGED, 0,
                    InputEvent.BUTTON1_DOWN_MASK, x, y, 0, false, MouseEvent.NOBUTTON);
            for (MouseMotionListener listener : getMouseMotionListeners()) {
                listener.mouseDragged(event);
            }
        }
    }
}
