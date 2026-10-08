package dtm.stools.component.panels.editor.pdf;

import dtm.stools.component.panels.editor.pdf.api.PdfChange;
import dtm.stools.component.panels.editor.pdf.api.PdfDocument;
import dtm.stools.component.panels.editor.pdf.api.PdfOcrResult;
import dtm.stools.component.panels.editor.pdf.api.PdfOcrWord;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfSignatureStatus;
import dtm.stools.component.panels.editor.pdf.api.PdfTarget;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import dtm.stools.component.panels.editor.pdf.backend.PdfBoxBackendProvider;
import dtm.stools.component.panels.editor.pdf.backend.PdfBoxSignatureProvider;
import dtm.stools.component.panels.editor.pdf.config.PdfEditorConfig;
import dtm.stools.component.panels.editor.pdf.config.PdfServices;
import dtm.stools.component.panels.editor.pdf.element.PdfElementFactory;
import dtm.stools.component.panels.editor.pdf.provider.PdfOcrProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfTrustProvider;
import dtm.stools.component.panels.editor.pdf.ui.PdfCanvas;
import dtm.stools.component.panels.editor.pdf.ui.PdfPageGeometry;
import dtm.stools.component.panels.editor.pdf.ui.PdfRenderScheduler;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbon;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonGroup;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem;
import dtm.stools.component.panels.editor.pdf.ui.PdfUiFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PdfEditorTest {
    private static PdfEditor createEditor() throws Throwable {
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            PdfEditor editor = new PdfEditor();
            editor.setErrorHandler(error -> { throw new AssertionError("Erro do editor", error); });
            ref.set(editor);
        });
        return ref.get();
    }

    private static void onEdt(Executable body) throws Throwable {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { body.execute(); } catch (Throwable error) { failure.set(error); }
        });
        if (failure.get() != null) throw failure.get();
    }

    @Test void ocrIsAvailableOnlyWhileAnApplicationProviderIsRegistered() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            assertTrue(editor.active(PdfOcrProvider.class).isEmpty());
            assertFalse(editor.getCommands().get("pdf.ocr").isEnabled());
            assertThrows(IllegalStateException.class, () -> editor.recognizePage(0, "por", true));
            PdfOcrProvider custom = new PdfOcrProvider() {
                @Override public String id() { return "test.ocr"; }
                @Override public int priority() { return 10; }
                @Override public PdfOcrResult recognize(BufferedImage image, String languages, java.util.function.IntConsumer progress) {
                    return new PdfOcrResult("custom", List.of(new PdfOcrWord("custom", 10, 10, 20, 10)));
                }
            };
            AtomicReference<AutoCloseable> registration = new AtomicReference<>();
            onEdt(() -> registration.set(editor.addProvider(custom)));
            assertSame(custom, editor.active(PdfOcrProvider.class).orElseThrow());
            assertTrue(editor.getCommands().get("pdf.ocr").isEnabled());
            AtomicReference<java.util.concurrent.CompletableFuture<PdfOcrResult>> future = new AtomicReference<>();
            onEdt(() -> future.set(editor.recognizePage(0, "por", true).completion().toCompletableFuture()));
            assertEquals("custom", future.get().get(10, TimeUnit.SECONDS).text());
            assertTrue(editor.getPageText(0).contains("custom"));
            onEdt(() -> registration.get().close());
            assertTrue(editor.active(PdfOcrProvider.class).isEmpty());
            assertFalse(editor.getCommands().get("pdf.ocr").isEnabled());
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void servicesInjectOcrBeforeFirstDocument() throws Throwable {
        PdfOcrProvider custom = new PdfOcrProvider() {
            @Override public String id() { return "test.initial.ocr"; }
            @Override public PdfOcrResult recognize(BufferedImage image, String language, java.util.function.IntConsumer progress) {
                return new PdfOcrResult("injected", List.of());
            }
        };
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new PdfEditor(PdfEditorConfig.defaults(), PdfServices.defaults().withOcr(custom))));
        try { assertSame(custom, ref.get().active(PdfOcrProvider.class).orElseThrow()); }
        finally { SwingUtilities.invokeAndWait(ref.get()::close); }
    }

    @Test void respectsEncryptedPdfPermissions() throws Throwable {
        Path file = Files.createTempFile("pdf-protected-", ".pdf");
        try {
            org.apache.pdfbox.pdmodel.PDDocument source = new org.apache.pdfbox.pdmodel.PDDocument();
            source.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            var permissions = new org.apache.pdfbox.pdmodel.encryption.AccessPermission();
            permissions.setCanModify(false);
            permissions.setCanExtractContent(false);
            source.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner", "reader", permissions));
            source.save(file.toFile());
            source.close();
            PdfBoxBackendProvider backend = new PdfBoxBackendProvider();
            Exception error = assertThrows(Exception.class, () -> backend.open(file, null));
            assertTrue(backend.isPasswordError(error));
            try (PdfDocument opened = backend.open(file, "reader".toCharArray())) {
                assertFalse(opened.canModify());
                assertFalse(opened.canExtractContent());
                assertEquals(1, opened.pageCount());
            }
        } finally { Files.deleteIfExists(file); }
    }

    @Test void replacesSimpleTextWithoutRasterizationAndUsesConfiguredFallback() throws Throwable {
        PdfBoxBackendProvider backend = new PdfBoxBackendProvider();
        try (PdfDocument document = backend.create()) {
            document.addText(0, "Original", 50, 700, 12);
            assertFalse(document.replaceText(0, "Original", "Changed", null, false));
            assertTrue(document.text(0).contains("Changed"));
            assertThrows(Exception.class, () -> document.replaceText(0, "Changed", "Much longer replacement",
                    new Rectangle2D.Float(50, 690, 150, 20), false));
            assertTrue(document.replaceText(0, "Changed", "Much longer replacement",
                    new Rectangle2D.Float(50, 690, 150, 20), true));
            assertTrue(document.render(0, 72).getWidth() > 100);
        }
    }

    @Test void signsAndValidatesWithTrustedCertificate() throws Throwable {
        Path source = Files.createTempFile("pdf-unsigned-", ".pdf");
        Path signed = Files.createTempFile("pdf-signed-", ".pdf");
        Path storeFile = Files.createTempFile("pdf-key-", ".p12");
        char[] password = "test-pass".toCharArray();
        try {
            try (PdfDocument document = new PdfBoxBackendProvider().create()) { document.save(source); }
            var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            var name = new org.bouncycastle.asn1.x500.X500Name("CN=PDF Test");
            var builder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(name,
                    BigInteger.ONE, Date.from(Instant.now().minusSeconds(60)),
                    Date.from(Instant.now().plusSeconds(3600)), name, keyPair.getPublic());
            var holder = builder.build(new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA")
                    .build(keyPair.getPrivate()));
            X509Certificate certificate = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(holder);
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, password);
            store.setKeyEntry("signer", keyPair.getPrivate(), password, new java.security.cert.Certificate[]{certificate});
            try (var output = Files.newOutputStream(storeFile)) { store.store(output, password); }
            PdfBoxSignatureProvider signatures = new PdfBoxSignatureProvider();
            signatures.sign(source, signed, storeFile, password, "Test");
            PdfTrustProvider trust = new PdfTrustProvider() {
                @Override public String id() { return "test"; }
                @Override public Set<TrustAnchor> anchors() { return Set.of(new TrustAnchor(certificate, null)); }
                @Override public boolean checkRevocation() { return false; }
            };
            assertEquals(PdfSignatureStatus.VALID, signatures.validate(signed, trust).getFirst().status());
            Files.write(signed, new byte[]{'X'}, java.nio.file.StandardOpenOption.APPEND);
            assertEquals(PdfSignatureStatus.INVALID, signatures.validate(signed, trust).getFirst().status());
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(signed);
            Files.deleteIfExists(storeFile);
        }
    }

    @Test void pdfBoxRoundTripPreservesPagesTextAnnotationsAndForms() throws Throwable {
        Path file = Files.createTempFile("pdf-editor-", ".pdf");
        Path picture = Files.createTempFile("pdf-picture-", ".png");
        BufferedImage pixel = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        var brush = pixel.createGraphics();
        brush.setColor(Color.BLUE);
        brush.fillRect(0, 0, 8, 8);
        brush.dispose();
        ImageIO.write(pixel, "png", picture.toFile());
        PdfBoxBackendProvider backend = new PdfBoxBackendProvider();
        try (PdfDocument document = backend.create()) {
            document.addText(0, "Example", 50, 700, 14);
            document.addNote(0, "Review", 90, 680);
            document.addHighlight(0, 50, 695, 70, 18);
            document.addTextField(0, "client", 50, 600, 130, 25);
            document.addChoiceField(0, "state", List.of("BA", "SP"), 50, 560, 130, 25);
            document.addCheckBox(0, "approved", 50, 530, 20);
            document.addRadioGroup(0, "priority", List.of("Low", "High"), 150, 530, 18, 6);
            document.setFormField("client", "Alice");
            document.setFormField("approved", "true");
            document.setFormField("priority", "High");
            document.addSquare(0, 50, 500, 80, 40);
            document.addInk(0, new float[]{50, 450, 60, 460, 70, 450});
            document.addImage(0, picture, 50, 400, 40, 40);
            String imageName = document.imageResources(0).getFirst();
            document.replaceImageResource(0, imageName, picture);
            assertTrue(document.pageElements(0).stream().anyMatch(element -> element.type().equals("Image")));
            document.save(file);
        }
        try (PdfDocument loaded = backend.open(file, null)) {
            assertEquals(1, loaded.pageCount());
            assertTrue(loaded.text(0).contains("Example"));
            assertTrue(loaded.formFields().containsAll(List.of("client", "state", "approved", "priority")));
            assertFalse(loaded.imageResources(0).isEmpty());
            assertTrue(loaded.render(0, 72).getWidth() > 100);
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(picture);
        }
    }

    @Test void editorUndoReturnsToCleanStateAndCanSaveAndOpen() throws Throwable {
        PdfEditor editor = createEditor();
        Path file = Files.createTempFile("pdf-editor-save-", ".pdf");
        try {
            onEdt(() -> {
                editor.addText(0, "Hello", 40, 700, 12);
                assertTrue(editor.isDirty());
                editor.undo();
                assertFalse(editor.isDirty());
                editor.redo();
                assertTrue(editor.isDirty());
            });
            AtomicReference<java.util.concurrent.CompletableFuture<Void>> saved = new AtomicReference<>();
            onEdt(() -> saved.set(editor.save(file).completion().toCompletableFuture()));
            saved.get().get(20, TimeUnit.SECONDS);
            assertFalse(editor.isDirty());
            assertTrue(editor.getPageText(0).contains("Hello"));
        } finally {
            SwingUtilities.invokeAndWait(editor::close);
            Files.deleteIfExists(file);
        }
    }

    @Test void geometryMapsRotatedPagesBackToPdfPoints() {
        for (int rotation : new int[]{0, 90, 180, 270}) {
            PdfPageGeometry geometry = new PdfPageGeometry(10, 20, 600, 800, rotation, 1.5);
            for (float[] point : new float[][]{{10, 20}, {310, 420}, {610, 820}}) {
                var view = geometry.toView(point[0], point[1]);
                var restored = geometry.toPdf(view.x, view.y);
                assertEquals(point[0], restored.x, .01);
                assertEquals(point[1], restored.y, .01);
            }
            Rectangle2D.Float area = new Rectangle2D.Float(100, 200, 50, 30);
            Rectangle2D.Float back = geometry.toPdf(geometry.toView(area));
            assertEquals(area.x, back.x, .01);
            assertEquals(area.width, back.width, .01);
        }
    }

    @Test void pageMappingsSupportStructuralUndo() {
        int[] moved = PdfChange.moved(4, 0, 2);
        assertArrayEquals(new int[]{2, 0, 1, 3}, moved);
        PdfChange forward = PdfChange.structure(PdfChange.removed(3, 1));
        PdfChange inverse = forward.inverse(3);
        assertArrayEquals(new int[]{0, 2, -1}, inverse.mapping());
        assertArrayEquals(new int[]{0, 1, 3, 4}, PdfChange.inserted(4, 2, 1));
    }

    @Test void selectionOperationsRemoveContentAndKeepEditableAnnotations() throws Throwable {
        Path file = Files.createTempFile("pdf-selection-", ".pdf");
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "REMOVE", 50, 700, 12);
            document.addText(0, "KEEP", 50, 600, 12);
            document.addNote(0, "Move me", 250, 500);
            PdfPageElement annotation = document.pageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow();
            Rectangle2D.Float moved = new Rectangle2D.Float(300, 500, 20, 20);
            document.setPageElementBounds(0, annotation.id(), moved);
            assertEquals(300, document.pageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow().bounds().x, .5);
            document.eraseRegion(0, new Rectangle2D.Float(40, 690, 100, 30));
            assertFalse(document.text(0).contains("REMOVE"));
            assertTrue(document.text(0).contains("KEEP"));
            assertTrue(document.pageElements(0).stream().noneMatch(element -> element.type().equals("Image")));
            assertEquals(1, document.pageElements(0).stream().filter(PdfPageElement::annotation).count());
            document.save(file);
        }
        try (PdfDocument reopened = new PdfBoxBackendProvider().open(file, null)) {
            assertFalse(reopened.text(0).contains("REMOVE"));
            PdfPageElement remaining = reopened.pageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow();
            reopened.deletePageElement(0, remaining.id());
            assertTrue(reopened.pageElements(0).stream().noneMatch(PdfPageElement::annotation));
        } finally { Files.deleteIfExists(file); }
    }

    @Test void areaTransformMovesPixelsAndRemovesOriginalTextLayer() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "MOVE", 50, 700, 14);
            assertTrue(document.pageElements(0).stream().anyMatch(element -> element.type().equals("Text") && element.text().contains("MOVE")));
            Rectangle2D.Float source = new Rectangle2D.Float(45, 695, 100, 28);
            BufferedImage copied = document.copyArea(0, source, 144);
            assertTrue(copied.getWidth() > 100);
            document.transformArea(0, source, new Rectangle2D.Float(200, 600, 140, 40), 30);
            assertFalse(document.text(0).contains("MOVE"));
            assertTrue(document.render(0, 72).getWidth() > 100);
        }
    }

    @Test void importedTextIsDeletedWithoutRasterizingThePage() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "DELETE these words", 60, 620, 16);
            PdfPageElement word = document.pageElements(0).stream()
                    .filter(element -> element.type().equals("Text") && element.text().equals("DELETE")).findFirst().orElseThrow();
            assertTrue(word.direct());
            document.deletePageElement(0, word.id());
            String text = document.text(0);
            assertFalse(text.contains("DELETE"));
            assertTrue(text.contains("these words"));
            assertTrue(document.pageElements(0).stream().noneMatch(element -> element.type().equals("Image")));
        }
    }

    @Test void textWordsMoveAndDuplicateAsVectorContent() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "Alpha Beta", 60, 600, 18);
            PdfPageElement beta = document.pageElements(0).stream().filter(element -> element.text().equals("Beta")).findFirst().orElseThrow();
            document.transformTarget(0, PdfTarget.of(beta.id()), AffineTransform.getTranslateInstance(0, -200));
            PdfPageElement moved = document.pageElements(0).stream().filter(element -> element.text().equals("Beta")).findFirst().orElseThrow();
            assertEquals(beta.bounds().y - 200, moved.bounds().y, 1.5);
            assertEquals(beta.bounds().x, moved.bounds().x, 1.5);
            document.duplicateTarget(0, PdfTarget.of(moved.id()), AffineTransform.getTranslateInstance(100, 0));
            assertEquals(2, document.pageElements(0).stream().filter(element -> element.text().equals("Beta")).count());
            assertTrue(document.pageElements(0).stream().noneMatch(element -> element.type().equals("Image")));
        }
    }

    @Test void annotationsCanBeStyledDuplicatedRotatedAndCopied() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addShape(0, PdfShapeKind.ELLIPSE, new Rectangle2D.Float(100, 100, 80, 40), PdfShapeStyle.defaults());
            document.addLine(0, 50, 50, 150, 80, true, PdfShapeStyle.defaults());
            List<PdfPageElement> annotations = document.pageElements(0).stream().filter(PdfPageElement::annotation).toList();
            assertEquals(2, annotations.size());
            String ellipse = annotations.getFirst().id();
            document.setAnnotationStyle(0, ellipse, PdfShapeStyle.defaults().withStroke(Color.RED).withFill(Color.YELLOW));
            document.duplicateTarget(0, PdfTarget.of(ellipse), AffineTransform.getTranslateInstance(10, -10));
            assertEquals(3, document.pageElements(0).stream().filter(PdfPageElement::annotation).count());
            document.transformTarget(0, PdfTarget.of(ellipse), AffineTransform.getRotateInstance(Math.toRadians(45), 140, 120));
            byte[] exported = document.exportAnnotations(0, List.of(ellipse));
            document.importAnnotations(0, exported, 200, 0);
            assertEquals(4, document.pageElements(0).stream().filter(PdfPageElement::annotation).count());
            assertTrue(document.render(0, 72).getWidth() > 100);
        }
    }

    @Test void textBoxesAndImageStampsAreEditableObjects() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            PdfTextStyle style = PdfTextStyle.defaults().withSize(16).withBold(true).withColor(Color.BLUE);
            document.addTextBox(0, new Rectangle2D.Float(72, 700, 0, 0), "Olá, edição\nsegunda linha", style);
            PdfPageElement box = document.pageElements(0).stream().filter(PdfPageElement::textBox).findFirst().orElseThrow();
            assertEquals("Olá, edição\nsegunda linha", document.textBoxText(0, box.id()).orElseThrow());
            assertTrue(document.textBoxStyle(0, box.id()).orElseThrow().bold());
            document.updateTextBox(0, box.id(), "Alterado", style.withItalic(true));
            assertEquals("Alterado", document.textBoxText(0, box.id()).orElseThrow());
            BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
            document.addImageStamp(0, image, new Rectangle2D.Float(100, 300, 80, 40));
            PdfPageElement stamp = document.pageElements(0).stream().filter(element -> element.type().equals("Stamp")).findFirst().orElseThrow();
            document.setPageElementBounds(0, stamp.id(), new Rectangle2D.Float(120, 320, 160, 80));
            PdfPageElement resized = document.pageElements(0).stream().filter(element -> element.id().equals(stamp.id())).findFirst().orElseThrow();
            assertEquals(160, resized.bounds().width, .5);
        }
    }

    @Test void eraserEditsPartiallyCoveredImagesInsteadOfRasterizingThePage() throws Throwable {
        Path picture = Files.createTempFile("pdf-erase-", ".png");
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            BufferedImage red = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            var brush = red.createGraphics();
            brush.setColor(Color.RED);
            brush.fillRect(0, 0, 100, 100);
            brush.dispose();
            ImageIO.write(red, "png", picture.toFile());
            document.addImage(0, picture, 100, 100, 200, 200);
            document.addText(0, "Vector", 400, 400, 12);
            document.eraseRegion(0, new Rectangle2D.Float(100, 100, 100, 200));
            BufferedImage rendered = document.render(0, 72);
            int top = (int) Math.round(document.pageHeight(0));
            assertEquals(Color.WHITE.getRGB(), rendered.getRGB(150, top - 200));
            assertNotEquals(Color.WHITE.getRGB(), rendered.getRGB(250, top - 200));
            assertTrue(document.text(0).contains("Vector"));
        } finally { Files.deleteIfExists(picture); }
    }

    @Test void eraserRemovesEverythingItTouchesIncludingFormFields() throws Exception {
        Path file = Files.createTempFile("pdf-eraser-fields-", ".pdf");
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addCheckBox(0, "aceite", 100, 600, 20);
            document.addRadioGroup(0, "prioridade", List.of("Baixa", "Media", "Alta"), 140, 500, 18, 6);
            document.addTextBox(0, new Rectangle2D.Float(300, 600, 0, 0), "1", PdfTextStyle.defaults());
            assertEquals(5, document.pageElements(0).stream().filter(PdfPageElement::annotation).count());
            document.eraseRegion(0, new Rectangle2D.Float(105, 605, 10, 10));
            document.eraseRegion(0, new Rectangle2D.Float(130, 440, 40, 90));
            PdfPageElement box = document.pageElements(0).stream().filter(PdfPageElement::textBox).findFirst().orElseThrow();
            document.eraseRegion(0, new Rectangle2D.Float(box.bounds().x + 1, box.bounds().y + 1, 3, 3));
            assertEquals(1, document.pageElements(0).stream().filter(PdfPageElement::annotation).count());
            Rectangle2D.Float all = box.bounds();
            document.eraseRegion(0, new Rectangle2D.Float(all.x - 2, all.y - 2, all.width + 4, all.height + 4));
            assertTrue(document.pageElements(0).stream().noneMatch(PdfPageElement::annotation));
            assertTrue(document.formFields().isEmpty());
            document.save(file);
        }
        try (PdfDocument reopened = new PdfBoxBackendProvider().open(file, null)) {
            assertTrue(reopened.formFields().isEmpty());
            assertTrue(reopened.pageElements(0).stream().noneMatch(PdfPageElement::annotation));
        } finally { Files.deleteIfExists(file); }
    }

    @Test void formFieldsCanBeRenamedAndHaveTheirOptionsEdited() throws Exception {
        Path file = Files.createTempFile("pdf-fields-edit-", ".pdf");
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addRadioGroup(0, "prioridade", List.of("Baixa", "Alta"), 100, 600, 18, 6);
            document.addChoiceField(0, "estado", List.of("BA", "SP"), 200, 600, 120, 22);
            document.addTextField(0, "nome", 200, 500, 160, 22);
            document.addRadioOption(0, "prioridade", "Media");
            List<PdfPageElement> radios = document.pageElements(0).stream().filter(element -> element.text().equals("prioridade")).toList();
            assertEquals(3, radios.size());
            document.renameRadioOption(0, radios.getLast().id(), "Normal");
            assertEquals(List.of("Baixa", "Alta", "Normal"), document.fieldInfo(0, radios.getFirst().id()).orElseThrow().options());
            document.setFormField("prioridade", "Normal");
            document.deleteTarget(0, PdfTarget.of(radios.get(1).id()));
            document.setFieldOptions("estado", List.of("MG", "BA", "RJ"));
            document.renameField("nome", "cliente");
            document.setFieldFlags("cliente", true, false, true);
            document.setFieldFontSize("cliente", 9);
            document.save(file);
        }
        try (PdfDocument reopened = new PdfBoxBackendProvider().open(file, null)) {
            assertTrue(reopened.formFields().containsAll(List.of("prioridade", "estado", "cliente")));
            PdfPageElement radio = reopened.pageElements(0).stream().filter(element -> element.text().equals("prioridade")).findFirst().orElseThrow();
            var info = reopened.fieldInfo(0, radio.id()).orElseThrow();
            assertEquals(List.of("Baixa", "Normal"), info.options());
            assertEquals("Normal", info.value());
            PdfPageElement choice = reopened.pageElements(0).stream().filter(element -> element.text().equals("estado")).findFirst().orElseThrow();
            assertEquals(List.of("MG", "BA", "RJ"), reopened.fieldInfo(0, choice.id()).orElseThrow().options());
            PdfPageElement text = reopened.pageElements(0).stream().filter(element -> element.text().equals("cliente")).findFirst().orElseThrow();
            var cliente = reopened.fieldInfo(0, text.id()).orElseThrow();
            assertTrue(cliente.required() && cliente.multiline());
            assertEquals(9, cliente.fontSize(), .01);
        } finally { Files.deleteIfExists(file); }
    }

    @Test void contextMenuOffersFieldSpecificActions() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            onEdt(() -> {
                editor.addRadioGroup(0, "grupo", List.of("A", "B"), 100, 600, 18, 6);
                PdfPageElement radio = editor.getPageElements(0).stream().filter(element -> element.text().equals("grupo")).findFirst().orElseThrow();
                editor.setSelection(PdfSelection.of(0, List.of(radio)));
                List<String> names = editor.canvasActions(0).stream().filter(java.util.Objects::nonNull)
                        .map(action -> String.valueOf(action.getValue(javax.swing.Action.NAME))).toList();
                assertTrue(names.containsAll(List.of("Selecionar esta opção", "Adicionar opção…", "Renomear opção…", "Excluir opção",
                        "Renomear campo…", "Obrigatório")));
                assertEquals("Selecionar esta opção", names.getFirst());
            });
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void derivesANewPdfWithOnlyTheChosenPagesAndTheirFields() throws Exception {
        Path file = Files.createTempFile("pdf-extract-", ".pdf");
        try (PdfDocument document = new PdfBoxBackendProvider().create()) {
            document.addText(0, "Primeira", 72, 700, 14);
            document.addTextField(0, "campoUm", 72, 600, 120, 22);
            document.insertBlankPage(1, 595, 842);
            document.addText(1, "Segunda", 72, 700, 14);
            document.addTextField(1, "campoDois", 72, 600, 120, 22);
            document.insertBlankPage(2, 595, 842);
            document.addText(2, "Terceira", 72, 700, 14);
            document.extractPages(List.of(2, 1), file);
            assertEquals(3, document.pageCount());
        }
        try (PdfDocument extracted = new PdfBoxBackendProvider().open(file, null)) {
            assertEquals(2, extracted.pageCount());
            assertTrue(extracted.text(0).contains("Terceira"));
            assertTrue(extracted.text(1).contains("Segunda"));
            assertEquals(List.of("campoDois"), extracted.formFields());
        } finally { Files.deleteIfExists(file); }
        assertEquals(List.of(0, 2, 3, 4), dtm.stools.component.panels.editor.pdf.command.PdfCommandCatalog.parsePages("1, 3-5", 5));
        assertEquals(List.of(3, 2, 1), dtm.stools.component.panels.editor.pdf.command.PdfCommandCatalog.parsePages("4-2", 5));
        assertThrows(IllegalArgumentException.class, () -> dtm.stools.component.panels.editor.pdf.command.PdfCommandCatalog.parsePages("9", 5));
    }

    @Test void thumbnailsSupportMultiplePageSelection() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            onEdt(() -> {
                editor.insertBlankPage(1);
                editor.insertBlankPage(2);
                editor.insertBlankPage(3);
                editor.clickPage(0, false, false);
                assertEquals(List.of(0), editor.getSelectedPages());
                editor.clickPage(2, false, true);
                assertEquals(List.of(0, 1, 2), editor.getSelectedPages());
                editor.clickPage(1, true, false);
                assertEquals(List.of(0, 2), editor.getSelectedPages());
                editor.clickPage(3, false, false);
                assertEquals(List.of(3), editor.getSelectedPages());
            });
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void editorDeletesSelectionAndUndoRestoresIt() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            onEdt(() -> {
                editor.addShape(0, PdfShapeKind.RECTANGLE, new Rectangle2D.Float(100, 100, 60, 40));
                PdfPageElement shape = editor.getPageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow();
                editor.setSelection(PdfSelection.of(0, List.of(shape)));
                editor.execute("pdf.eraseSelection");
                assertTrue(editor.getPageElements(0).stream().noneMatch(PdfPageElement::annotation));
                assertTrue(editor.getSelection().isEmpty());
                editor.undo();
                assertEquals(1, editor.getPageElements(0).stream().filter(PdfPageElement::annotation).count());
                editor.setSelection(PdfSelection.of(0, editor.getPageElements(0)));
                editor.moveSelection(20, 10);
                PdfPageElement moved = editor.getPageElements(0).stream().filter(PdfPageElement::annotation).findFirst().orElseThrow();
                assertEquals(120, moved.bounds().x, 1);
                assertFalse(editor.getSelection().isEmpty());
            });
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void textToolCreatesTextBoxAtTheClickedPoint() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            onEdt(() -> {
                editor.selectTool("pdf.factory.text");
                PdfElementFactory text = editor.getActiveFactory();
                editor.placeElement(text, new PdfPlacement(0, new java.awt.geom.Point2D.Float(80, 700), null));
                assertEquals(PdfEditor.TOOL_SELECT, editor.getActiveTool());
                var overlay = java.util.Arrays.stream(editor.getCanvas().getComponents())
                        .filter(component -> component instanceof dtm.stools.component.panels.editor.pdf.ui.PdfTextOverlay)
                        .map(component -> (dtm.stools.component.panels.editor.pdf.ui.PdfTextOverlay) component).findFirst().orElseThrow();
                overlay.setText("Digitado");
                overlay.commit();
                PdfPageElement box = editor.getPageElements(0).stream().filter(PdfPageElement::textBox).findFirst().orElseThrow();
                assertEquals(80, box.bounds().x, 1);
                assertEquals(700, box.bounds().y + box.bounds().height, 1);
            });
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void renderSchedulerKeepsPreviousImageUntilTheNewOneIsReady() throws Throwable {
        try (PdfDocument document = new PdfBoxBackendProvider().create();
             PdfRenderScheduler scheduler = new PdfRenderScheduler(() -> document,
                     page -> new PdfPageGeometry(0, 0, document.pageWidth(page), document.pageHeight(page), 0, 1), () -> 4)) {
            java.util.concurrent.CountDownLatch first = new java.util.concurrent.CountDownLatch(1);
            AtomicReference<BufferedImage> initial = new AtomicReference<>();
            onEdt(() -> initial.set(scheduler.page(0, 36, first::countDown)));
            assertNull(initial.get());
            assertTrue(first.await(20, TimeUnit.SECONDS));
            AtomicReference<BufferedImage> rendered = new AtomicReference<>();
            onEdt(() -> rendered.set(scheduler.page(0, 36, null)));
            assertNotNull(rendered.get());
            onEdt(() -> {
                assertTrue(scheduler.isFresh(0, 36));
                scheduler.invalidate(0);
                assertFalse(scheduler.isFresh(0, 36));
                assertSame(rendered.get(), scheduler.page(0, 36, null));
            });
        }
    }

    @Test void editingOnePageKeepsOtherPagesCached() throws Throwable {
        PdfEditor editor = createEditor();
        try {
            onEdt(() -> editor.insertBlankPage(1));
            java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
            onEdt(() -> {
                editor.getRenderer().page(0, 36, ready::countDown);
                editor.getRenderer().page(1, 36, ready::countDown);
            });
            assertTrue(ready.await(20, TimeUnit.SECONDS));
            onEdt(() -> {
                assertTrue(editor.getRenderer().isFresh(0, 36));
                editor.addText(1, "Only page two", 50, 700, 12);
                assertTrue(editor.getRenderer().isFresh(0, 36));
                assertFalse(editor.getRenderer().isFresh(1, 36));
                editor.movePage(1, 0);
                assertTrue(editor.getRenderer().isFresh(1, 36));
            });
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    @Test void uiFactoryAndElementFactoryCanBeInstalledAndRemoved() throws Throwable {
        AtomicReference<PdfCanvas> created = new AtomicReference<>();
        PdfUiFactory ui = owner -> {
            PdfCanvas canvas = new PdfCanvas(owner);
            created.set(canvas);
            return canvas;
        };
        AtomicReference<PdfEditor> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new PdfEditor(PdfEditorConfig.defaults(), PdfServices.defaults().withUiFactory(ui))));
        PdfEditor editor = ref.get();
        try {
            assertNotNull(created.get());
            PdfElementFactory factory = new PdfElementFactory() {
                @Override public String id() { return "test.element"; }
                @Override public String title() { return "Marca"; }
                @Override public void insert(PdfEditor owner, PdfPlacement placement) throws java.io.IOException {
                    owner.addSquare(placement.page(), placement.point().x, placement.point().y, 20, 20);
                }
            };
            AtomicReference<AutoCloseable> handle = new AtomicReference<>();
            onEdt(() -> handle.set(editor.addProvider(factory)));
            assertTrue(editor.getCommands().containsKey(factory.commandId()));
            PdfRibbon ribbon = (PdfRibbon) editor.getRibbon();
            assertTrue(ribbonHas(ribbon, factory.commandId()));
            onEdt(() -> editor.selectElementFactory(factory.id()));
            onEdt(() -> {
                editor.placeElement(factory, new PdfPlacement(0, new java.awt.geom.Point2D.Float(100, 100), null));
                assertEquals(1, editor.getPageElements(0).stream().filter(PdfPageElement::annotation).count());
            });
            onEdt(() -> handle.get().close());
            assertFalse(editor.getCommands().containsKey(factory.commandId()));
            assertFalse(ribbonHas(ribbon, factory.commandId()));
            assertThrows(IllegalArgumentException.class, () -> editor.selectElementFactory(factory.id()));
        } finally { SwingUtilities.invokeAndWait(editor::close); }
    }

    private static boolean ribbonHas(PdfRibbon ribbon, String command) {
        for (var tab : ribbon.tabs())
            for (PdfRibbonGroup group : tab.groups())
                for (PdfRibbonItem item : group.items()) if (command.equals(item.command())) return true;
        return false;
    }
}
