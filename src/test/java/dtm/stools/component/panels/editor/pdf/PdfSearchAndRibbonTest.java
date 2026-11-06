package dtm.stools.component.panels.editor.pdf;

import dtm.stools.component.panels.editor.pdf.backend.PdfBoxBackendProvider;
import dtm.stools.component.panels.editor.pdf.config.PdfEditorConfig;
import dtm.stools.component.panels.editor.pdf.provider.PdfToolbarProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PdfSearchAndRibbonTest {
    @Test
    void findsEveryOccurrenceAndOnlyHighlightsTheMatchingCharacters() throws Exception {
        try (var document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "banana BANANA", 100, 600, 20);
            var matches = document.findText(0, "ana");
            assertEquals(2, matches.size());
            assertTrue(matches.getFirst().x > 100);
            assertTrue(matches.getFirst().width < 40);
            assertTrue(matches.getFirst().contains(120, 603));
            assertTrue(matches.get(1).x > matches.getFirst().getMaxX());
            assertEquals(2, document.findText(0, "banana").size());
            assertEquals(1, document.findText(0, "banana banana").size());
            assertTrue(document.findText(0, "missing").isEmpty());
            assertTrue(document.findText(0, "").isEmpty());
            assertFalse(document.pageElements(0).stream().anyMatch(element -> element.type().equals("Highlight")));
        }
    }

    @Test
    void preservesPdfCoordinatesOnCroppedAndRotatedPages() throws Exception {
        byte[] snapshot;
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(600, 800));
            page.setCropBox(new PDRectangle(40, 70, 500, 700));
            page.setRotation(90);
            source.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(source, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 20);
                content.newLineAtOffset(100, 600);
                content.showText("Busca PDF");
                content.endText();
            }
            source.save(output);
            snapshot = output.toByteArray();
        }
        try (var document = new PdfBoxBackendProvider().restore(snapshot)) {
            var matches = document.findText(0, "busca");
            assertEquals(1, matches.size(), document.text(0));
            assertEquals(100, matches.getFirst().x, .1);
            assertTrue(matches.getFirst().contains(110, 614));
        }
    }

    @Test
    void newerSearchesAndClearingTheFieldDiscardPendingResults() throws Exception {
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        edt(() -> {
            ref.set(new PdfEditor());
            ref.get().addText(0, "alpha beta", 100, 600, 20);
        });
        PdfEditor editor = ref.get();
        try {
            CompletableFuture<Map<Integer, String>> latest = new CompletableFuture<>();
            AtomicBoolean stale = new AtomicBoolean();
            edt(() -> {
                editor.searchAsync("alpha", ignored -> stale.set(true));
                editor.searchAsync("BETA", latest::complete);
            });
            assertEquals(1, latest.get(10, TimeUnit.SECONDS).size());
            edt(() -> {
                assertFalse(stale.get());
                assertEquals(1, editor.getSearchHighlights(0).size());
                assertFalse(editor.isClosed());
                editor.clearSearchHighlights();
                assertTrue(editor.getSearchHighlights(0).isEmpty());
                JTextField field = (JTextField) named(editor, "pdf.sidebar.search");
                field.setText("alpha");
                field.postActionEvent();
                field.setText("");
            });
            CompletableFuture<String> barrier = new CompletableFuture<>();
            edt(() -> editor.pageTextAsync(0, barrier::complete));
            barrier.get(10, TimeUnit.SECONDS);
            edt(() -> {
                assertTrue(editor.getSearchHighlights(0).isEmpty());
                assertEquals(0, ((JList<?>) named(editor, "pdf.sidebar.searchResults")).getModel().getSize());
            });
        } finally { edt(editor::close); }
    }

    @Test
    void searchPaintsHighlightsAndRibbonTogglePreservesOtherPanels() throws Exception {
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        AtomicReference<JFrame> frameRef = new AtomicReference<>();
        edt(() -> {
            PdfEditor editor = new PdfEditor(PdfEditorConfig.defaults().withReadOnly(true));
            editor.getDocument().addText(0, "Buscar no PDF", 100, 600, 20);
            editor.addProvider(new PdfToolbarProvider() {
                @Override
                public String id() { return "test.toolbar"; }
                @Override
                public JComponent createToolbar(PdfEditor value) { return new JLabel("Ferramenta adicional"); }
            });
            JFrame frame = new JFrame();
            frame.add(editor);
            frame.setSize(1100, 800);
            frame.addNotify();
            frame.validate();
            ref.set(editor);
            frameRef.set(frame);
        });
        PdfEditor editor = ref.get();
        JFrame frame = frameRef.get();
        try {
            CompletableFuture<Map<Integer, String>> found = new CompletableFuture<>();
            edt(() -> editor.searchAsync("buscar", found::complete));
            assertEquals(1, found.get(10, TimeUnit.SECONDS).size());
            CompletableFuture<Void> rendered = new CompletableFuture<>();
            edt(() -> {
                editor.setZoom(1.5);
                frame.validate();
                editor.getRenderer().page(0, editor.getCanvas().renderResolution(), () -> rendered.complete(null));
                if (editor.getRenderer().isFresh(0, editor.getCanvas().renderResolution())) rendered.complete(null);
            });
            rendered.get(10, TimeUnit.SECONDS);
            edt(() -> {
                var boxes = editor.getSearchHighlights(0);
                assertEquals(1, boxes.size());
                assertFalse(editor.isDirty());
                var canvas = editor.getCanvas();
                canvas.setSize(canvas.getPreferredSize());
                Rectangle2D area = canvas.getPageLayout().toView(0, boxes.getFirst());
                BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                canvas.paint(graphics);
                graphics.dispose();
                int yellow = 0;
                for (int y = (int) area.getMinY(); y < area.getMaxY(); y++)
                    for (int x = (int) area.getMinX(); x < area.getMaxX(); x++) {
                        int pixel = image.getRGB(x, y);
                        int red = (pixel >> 16) & 255, green = (pixel >> 8) & 255, blue = pixel & 255;
                        if (red > 200 && green > 150 && blue < 190) yellow++;
                    }
                assertTrue(yellow > 50);
                ImageIO.write(image, "png", Path.of("target", "pdf-search-highlight.png").toFile());
                JButton toggle = (JButton) named(editor, "pdf.ribbon.toggle");
                JComponent providerBar = (JComponent) named(editor, "pdf.providerBar");
                Component status = ((java.awt.BorderLayout) editor.getLayout()).getLayoutComponent(java.awt.BorderLayout.SOUTH);
                int expanded = editor.getPreferredSize().height;
                capture(frame, "pdf-search-expanded.png");
                toggle.doClick();
                frame.validate();
                assertFalse(editor.getRibbon().isVisible());
                assertTrue(toggle.isVisible());
                assertTrue(editor.getSidebar().isVisible());
                assertTrue(providerBar.isVisible());
                assertTrue(status.isVisible());
                assertTrue(editor.getPreferredSize().height < expanded);
                capture(frame, "pdf-search-collapsed.png");
                editor.setStatusVisible(false);
                assertFalse(editor.getRibbon().isVisible());
                toggle.doClick();
                assertTrue(editor.getRibbon().isVisible());
                assertFalse(status.isVisible());
                assertTrue(providerBar.isVisible());
                assertEquals(boxes, editor.getSearchHighlights(0));
            });
        } finally { edt(() -> { editor.close(); frame.dispose(); }); }
    }

    private static Component named(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component found = named(container, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void capture(JFrame frame, String name) throws Exception {
        Container content = frame.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        content.paint(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", Path.of("target", name).toFile());
    }

    private static void edt(Executable body) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { body.execute(); } catch (Throwable error) { failure.set(error); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
}
