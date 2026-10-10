package dtm.stools.component.panels.editor.pdf;

import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.ui.PdfCanvas;
import dtm.stools.component.panels.editor.pdf.ui.PdfPageLayout;
import dtm.stools.component.panels.editor.pdf.ui.PdfTextOverlay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PdfCanvasInteractionTest {
    private static void withEditor(ThrowingConsumer<PdfEditor> body) throws Throwable {
        AtomicReference<JFrame> frame = new AtomicReference<>();
        AtomicReference<PdfEditor> editor = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PdfEditor created = new PdfEditor();
            created.setErrorHandler(error -> { throw new AssertionError("Erro do editor", error); });
            JFrame window = new JFrame();
            window.add(created);
            window.setSize(1200, 900);
            window.setVisible(true);
            window.validate();
            frame.set(window);
            editor.set(created);
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try { body.accept(editor.get()); }
                catch (Throwable error) { failure.set(error); }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { editor.get().close(); frame.get().dispose(); });
        }
        if (failure.get() != null) throw failure.get();
    }

    private static Point view(PdfCanvas canvas, float x, float y) {
        Point2D.Double point = canvas.getPageLayout().toView(0, x, y);
        return new Point((int) Math.round(point.x), (int) Math.round(point.y));
    }

    private static void press(PdfCanvas canvas, Point at, int clicks) {
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                at.x, at.y, clicks, false, MouseEvent.BUTTON1));
    }

    private static void drag(PdfCanvas canvas, Point to) {
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), InputEvent.BUTTON1_DOWN_MASK,
                to.x, to.y, 0, false, MouseEvent.NOBUTTON));
    }

    private static void release(PdfCanvas canvas, Point at) {
        canvas.dispatchEvent(new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0,
                at.x, at.y, 1, false, MouseEvent.BUTTON1));
    }

    private static void dragFrom(PdfCanvas canvas, Point from, Point to) {
        press(canvas, from, 1);
        drag(canvas, new Point((from.x + to.x) / 2, (from.y + to.y) / 2));
        drag(canvas, to);
        release(canvas, to);
    }

    @Test
    void clickSelectsAndDragMovesAnObject() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.addShape(0, PdfShapeKind.RECTANGLE, new Rectangle2D.Float(100, 500, 120, 80));
            assertTrue(canvas.getPageLayout().contains(0));
            Point inside = view(canvas, 160, 540);
            press(canvas, inside, 1);
            release(canvas, inside);
            assertEquals(1, editor.getSelection().elements().size());
            assertTrue(editor.getSelection().elements().getFirst().annotation());
            dragFrom(canvas, inside, view(canvas, 260, 440));
            PdfPageElement moved = editor.getPageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow();
            assertEquals(200, moved.bounds().x, 3);
            assertEquals(400, moved.bounds().y, 3);
            assertFalse(editor.getSelection().isEmpty());
        });
    }

    @Test
    void deleteKeyRemovesTheSelection() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.addShape(0, PdfShapeKind.ELLIPSE, new Rectangle2D.Float(100, 500, 120, 80));
            Point inside = view(canvas, 160, 540);
            press(canvas, inside, 1);
            release(canvas, inside);
            canvas.getActionMap().get("pdf.eraseSelection").actionPerformed(null);
            assertTrue(editor.getPageElements(0).stream().noneMatch(PdfPageElement::annotation));
        });
    }

    @Test
    void textToolOpensAnEditableBoxExactlyWhereTheUserClicked() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.execute("pdf.addText");
            Point target = view(canvas, 150, 650);
            press(canvas, target, 1);
            release(canvas, target);
            PdfTextOverlay overlay = Arrays.stream(canvas.getComponents()).filter(PdfTextOverlay.class::isInstance)
                    .map(PdfTextOverlay.class::cast).findFirst().orElseThrow();
            assertEquals(target.x, overlay.getX() + 4, 3);
            overlay.setText("Olá");
            overlay.commit();
            PdfPageElement box = editor.getPageElements(0).stream().filter(PdfPageElement::textBox).findFirst().orElseThrow();
            assertEquals(150, box.bounds().x, 2);
            assertEquals(650, box.bounds().y + box.bounds().height, 2);
            assertEquals(1, editor.getSelection().elements().size());
        });
    }

    @Test
    void marqueeSelectsWordsAndEraserRemovesContent() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.addText(0, "Primeira linha", 80, 700, 14);
            editor.addText(0, "Segunda linha", 80, 600, 14);
            dragFrom(canvas, view(canvas, 70, 725), view(canvas, 300, 690));
            assertEquals("Primeira linha", editor.getSelection().text());
            editor.execute("pdf.eraser.rect");
            dragFrom(canvas, view(canvas, 70, 625), view(canvas, 300, 590));
            String text = editor.getPageText(0);
            assertTrue(text.contains("Primeira"));
            assertFalse(text.contains("Segunda"));
        });
    }

    @Test
    void brushEraserRemovesOnlyThePartItPassesOver() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.addInk(0, new float[]{100, 500, 300, 500}, dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle.defaults()
                    .withStroke(java.awt.Color.BLACK).withLineWidth(6));
            editor.execute("pdf.eraser.brush");
            editor.setEraserSize(30);
            dragFrom(canvas, view(canvas, 200, 520), view(canvas, 200, 480));
            assertEquals(1, editor.getPageElements(0).stream().filter(PdfPageElement::annotation).count());
            java.awt.image.BufferedImage image = editor.getDocument().render(0, 72);
            int top = (int) editor.getPageHeight(0);
            assertEquals(java.awt.Color.WHITE.getRGB(), image.getRGB(200, top - 500));
            assertNotEquals(java.awt.Color.WHITE.getRGB(), image.getRGB(120, top - 500));
            assertNotEquals(java.awt.Color.WHITE.getRGB(), image.getRGB(280, top - 500));
        });
    }

    @Test
    void formFieldsAreVisibleAndFilledByDoubleClick() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.addTextField(0, "nome", 100, 600, 180, 24);
            editor.addCheckBox(0, "aceite", 100, 550, 18);
            java.awt.image.BufferedImage image = editor.getDocument().render(0, 72);
            int top = (int) editor.getPageHeight(0);
            assertNotEquals(java.awt.Color.WHITE.getRGB(), image.getRGB(190, top - 612));
            Point box = view(canvas, 109, 559);
            press(canvas, box, 1);
            release(canvas, box);
            press(canvas, box, 2);
            release(canvas, box);
            var info = editor.getDocument().fieldInfo(0, editor.getPageElements(0).stream()
                    .filter(element -> element.text().equals("aceite")).findFirst().orElseThrow().id()).orElseThrow();
            assertEquals("true", info.value());
        });
    }

    @Test
    void layoutKeepsEveryPageInContinuousMode() throws Throwable {
        withEditor(editor -> {
            PdfCanvas canvas = editor.getCanvas();
            editor.insertBlankPage(1);
            editor.insertBlankPage(2);
            PdfPageLayout layout = canvas.getPageLayout();
            assertTrue(layout.contains(0) && layout.contains(1) && layout.contains(2));
            assertTrue(layout.bounds(1).y > layout.bounds(0).getMaxY());
            editor.setViewMode(dtm.stools.component.panels.editor.pdf.api.PdfViewMode.SINGLE_PAGE);
            assertFalse(canvas.getPageLayout().contains(0) && canvas.getPageLayout().contains(1));
        });
    }
}
