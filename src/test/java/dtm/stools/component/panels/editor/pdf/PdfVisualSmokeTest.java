package dtm.stools.component.panels.editor.pdf;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfVisualSmokeTest {
    @Test
    void paintsEditorInLightDarkAndNarrowLayouts() throws Exception {
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            FlatLightLaf.setup();
            PdfEditor editor = new PdfEditor();
            try {
                editor.addText(0, "Relatório de exemplo", 72, 760, 22);
                editor.addText(0, "Texto vetorial que pode ser selecionado, movido e apagado.", 72, 720, 12);
                editor.addShape(0, PdfShapeKind.RECTANGLE, new Rectangle2D.Float(72, 560, 200, 110));
                editor.addShape(0, PdfShapeKind.ELLIPSE, new Rectangle2D.Float(300, 560, 160, 110));
                editor.addTextBox(0, new Rectangle2D.Float(72, 520, 0, 0), "Caixa de texto editável",
                        PdfTextStyle.defaults().withColor(new Color(0xC8323C)).withBold(true));
                editor.addHighlight(0, 70, 715, 300, 18);
                editor.insertBlankPage(1);
                editor.addText(1, "Segunda página", 72, 760, 20);
                editor.insertBlankPage(2);
                List<PdfPageElement> shapes = editor.getPageElements(0).stream().filter(PdfPageElement::annotation).toList();
                editor.setCurrentPage(0);
                editor.setSelection(PdfSelection.of(0, List.of(shapes.get(1))));
            } catch (Exception error) { throw new RuntimeException(error); }
            JFrame frame = new JFrame("PDF");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(editor);
            frame.setSize(new Dimension(1280, 820));
            frame.setVisible(true);
            frameRef.set(frame);
            ref.set(editor);
        });
        PdfEditor editor = ref.get();
        JFrame frame = frameRef.get();
        try {
            Thread.sleep(1500);
            capture(frame, "pdf-editor-smoke.png");
            SwingUtilities.invokeAndWait(() -> {
                FlatDarkLaf.setup();
                SwingUtilities.updateComponentTreeUI(frame);
            });
            Thread.sleep(800);
            capture(frame, "pdf-editor-dark-smoke.png");
            SwingUtilities.invokeAndWait(() -> {
                FlatLightLaf.setup();
                SwingUtilities.updateComponentTreeUI(frame);
                frame.setSize(760, 700);
                frame.validate();
            });
            Thread.sleep(1000);
            capture(frame, "pdf-editor-narrow-smoke.png");
            SwingUtilities.invokeAndWait(() -> {
                frame.setSize(1280, 820);
                editor.selectTool(PdfEditor.TOOL_SELECT);
                ((PdfEditor) editor).getSidebar().show("tools");
                if (editor.getRibbon() instanceof dtm.stools.component.panels.editor.pdf.ui.PdfRibbon ribbon) ribbon.select("insert");
                frame.validate();
            });
            Thread.sleep(800);
            capture(frame, "pdf-editor-insert-smoke.png");
            assertTrue(Files.size(Path.of("target", "pdf-editor-smoke.png")) > 5_000);
            assertTrue(Files.size(Path.of("target", "pdf-editor-dark-smoke.png")) > 5_000);
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                editor.close();
                frame.dispose();
                FlatLightLaf.setup();
            });
        }
    }

    private static void capture(JFrame frame, String name) throws Exception {
        Path output = Path.of("target", name);
        Files.createDirectories(output.getParent());
        SwingUtilities.invokeAndWait(() -> {
            var root = frame.getContentPane();
            BufferedImage image = new BufferedImage(Math.max(1, root.getWidth()), Math.max(1, root.getHeight()), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            root.paint(graphics);
            graphics.dispose();
            try { ImageIO.write(image, "png", output.toFile()); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
    }
}
