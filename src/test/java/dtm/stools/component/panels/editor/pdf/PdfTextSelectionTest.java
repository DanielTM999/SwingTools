package dtm.stools.component.panels.editor.pdf;

import org.junit.jupiter.api.Test;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PdfTextSelectionTest {
    @Test
    void viewingSelectsCharactersInBothDirectionsWithoutSelectingObjects() throws Exception {
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        AtomicReference<JFrame> window = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PdfEditor editor = new PdfEditor();
            try {
                editor.addText(0, "Alpha beta", 100, 600, 20);
                editor.addText(0, "Second line", 100, 560, 20);
            }
            catch (Exception error) { throw new IllegalStateException(error); }
            JFrame frame = new JFrame();
            frame.add(editor);
            frame.setSize(1100, 800);
            frame.addNotify();
            frame.validate();
            ref.set(editor);
            window.set(frame);
        });
        PdfEditor editor = ref.get();
        try {
            byte[] before = pageBytes(editor);
            var content = editor.getDocument().textContent(0);
            assertEquals("Alpha beta Second line", content.text().strip().replaceAll("\\s+", " "));
            for (boolean reverse : new boolean[]{false, true}) {
                SwingUtilities.invokeAndWait(() -> {
                    var layout = editor.getCanvas().getPageLayout();
                    Rectangle2D first = layout.toView(0, content.glyphs().getFirst().bounds());
                    Rectangle2D last = layout.toView(0, content.glyphs().getLast().bounds());
                    Point start = new Point((int) first.getMinX(), (int) first.getCenterY());
                    Point end = new Point((int) Math.ceil(last.getMaxX()), (int) last.getCenterY());
                    gesture(editor, reverse ? end : start, reverse ? start : end);
                });
                CompletableFuture<String> barrier = new CompletableFuture<>();
                SwingUtilities.invokeAndWait(() -> editor.pageTextAsync(0, barrier::complete));
                barrier.get(10, TimeUnit.SECONDS);
                SwingUtilities.invokeAndWait(() -> {
                    assertEquals(content.text().strip(), editor.getSelectedText());
                    assertTrue(editor.getSelection().isEmpty());
                    assertTrue(editor.getCommands().get("pdf.copy").isEnabled());
                });
            }
            assertArrayEquals(before, pageBytes(editor));
            SwingUtilities.invokeAndWait(editor::copySelection);
            assertEquals(content.text().strip(), java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor));
            SwingUtilities.invokeAndWait(() -> {
                var canvas = editor.getCanvas();
                var box = canvas.getPageLayout().toView(0, content.glyphs().getFirst().bounds());
                canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK, (int) box.getCenterX(), (int) box.getCenterY(), 2, false, MouseEvent.BUTTON1));
                canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                        0, (int) box.getCenterX(), (int) box.getCenterY(), 2, false, MouseEvent.BUTTON1));
            });
            CompletableFuture<String> wordReady = new CompletableFuture<>();
            SwingUtilities.invokeAndWait(() -> editor.pageTextAsync(0, wordReady::complete));
            wordReady.get(10, TimeUnit.SECONDS);
            SwingUtilities.invokeAndWait(() -> assertEquals("Alpha", editor.getSelectedText()));
            SwingUtilities.invokeAndWait(() -> {
                editor.selectTool(PdfEditor.TOOL_SELECT);
                assertEquals("", editor.getSelectedText());
                editor.selectTool(PdfEditor.TOOL_VIEW);
                assertEquals("", editor.getSelectedText());
            });
        } finally { SwingUtilities.invokeAndWait(() -> { editor.close(); window.get().dispose(); }); }
    }

    private static void gesture(PdfEditor editor, Point start, Point end) {
        var canvas = editor.getCanvas();
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, start.x, start.y, 1, false, MouseEvent.BUTTON1));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, end.x, end.y, 0, false, MouseEvent.NOBUTTON));
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, end.x, end.y, 1, false, MouseEvent.BUTTON1));
    }

    private static byte[] pageBytes(PdfEditor editor) throws Exception {
        try (var document = org.apache.pdfbox.Loader.loadPDF(editor.getDocument().snapshot());
             var content = document.getPage(0).getContents()) {
            return content.readAllBytes();
        }
    }
}
