package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfDocument;
import dtm.stools.component.panels.editor.pdf.api.PdfOcrResult;
import dtm.stools.component.panels.editor.pdf.api.PdfOcrWord;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfFieldInfo;
import dtm.stools.component.panels.editor.pdf.api.PdfFieldKind;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfTarget;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFreeText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlight;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquare;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationInk;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.print.PrinterJob;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PdfBoxDocument implements PdfDocument {
    private final PDDocument document;
    PdfBoxDocument(PDDocument document) { this.document = document; }
    private PDPage page(int index) {
        if (index < 0 || index >= document.getNumberOfPages()) throw new IndexOutOfBoundsException(index);
        return document.getPage(index);
    }
    @Override
    public synchronized int pageCount() { return document.getNumberOfPages(); }
    @Override
    public synchronized float pageWidth(int page) { return page(page).getCropBox().getWidth(); }
    @Override
    public synchronized float pageHeight(int page) { return page(page).getCropBox().getHeight(); }
    @Override
    public synchronized float pageX(int page) { return page(page).getCropBox().getLowerLeftX(); }
    @Override
    public synchronized float pageY(int page) { return page(page).getCropBox().getLowerLeftY(); }
    @Override
    public synchronized int pageRotation(int page) { return page(page).getRotation(); }
    @Override
    public synchronized BufferedImage render(int page, float dpi) throws IOException {
        page(page);
        return new PDFRenderer(document).renderImageWithDPI(page, dpi);
    }
    @Override
    public synchronized String text(int page) throws IOException {
        page(page);
        if (!canExtractContent()) throw new IOException("Extração de texto não permitida neste PDF");
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page + 1);
        stripper.setEndPage(page + 1);
        return stripper.getText(document);
    }
    @Override
    public synchronized void save(Path destination) throws IOException { document.save(destination.toFile()); }
    @Override
    public synchronized byte[] snapshot() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.save(output);
        return output.toByteArray();
    }
    @Override
    public synchronized void rotate(int page, int degrees) {
        if (degrees % 90 != 0) throw new IllegalArgumentException("Rotation must be a multiple of 90");
        PDPage p = page(page);
        p.setRotation(Math.floorMod(p.getRotation() + degrees, 360));
    }
    @Override
    public synchronized void remove(int page) {
        if (pageCount() == 1) throw new IllegalStateException("The last page cannot be removed");
        document.removePage(page);
    }
    @Override
    public synchronized void move(int source, int destination) {
        PDPage selected = page(source);
        if (destination < 0 || destination >= pageCount()) throw new IndexOutOfBoundsException(destination);
        if (source == destination) return;
        document.getPages().remove(selected);
        if (destination == document.getNumberOfPages()) document.addPage(selected);
        else document.getPages().insertBefore(selected, document.getPage(destination));
    }
    @Override
    public synchronized void insertPages(Path source, int destination) throws IOException {
        if (destination < 0 || destination > pageCount()) throw new IndexOutOfBoundsException(destination);
        try (PDDocument other = Loader.loadPDF(source.toFile())) {
            PDAcroForm incomingForm = other.getDocumentCatalog().getAcroForm();
            if ((incomingForm != null && !incomingForm.getFields().isEmpty())
                    || !other.getSignatureDictionaries().isEmpty())
                throw new IOException("Inserção de páginas com formulários ou assinaturas exige preservação especial");
            int offset = destination;
            for (PDPage sourcePage : other.getPages()) {
                PDPage imported = document.importPage(sourcePage);
                document.getPages().remove(imported);
                if (offset >= document.getNumberOfPages()) document.addPage(imported);
                else document.getPages().insertBefore(imported, document.getPage(offset));
                offset++;
            }
        }
    }
    @Override
    public synchronized void addText(int page, String text, float x, float y, float fontSize) throws IOException {
        if (text == null || text.isBlank() || fontSize <= 0) throw new IllegalArgumentException("Text and font size required");
        try (PDPageContentStream stream = new PDPageContentStream(document, page(page),
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }
    }
    @Override
    public synchronized void addNote(int page, String text, float x, float y) throws IOException {
        PDAnnotationText note = new PDAnnotationText();
        note.setContents(text);
        note.setRectangle(new PDRectangle(x, y, 24, 24));
        note.setColor(new PDColor(new float[]{1f, .82f, .2f}, PDDeviceRGB.INSTANCE));
        note.setPrinted(true);
        note.constructAppearances(document);
        add(page, note);
    }
    @Override
    public synchronized void addHighlight(int page, float x, float y, float width, float height) throws IOException {
        PDAnnotationTextMarkup mark = new PDAnnotationHighlight();
        mark.setRectangle(new PDRectangle(x, y, width, height));
        mark.setQuadPoints(new float[]{x,y+height,x+width,y+height,x,y,x+width,y});
        mark.setColor(new PDColor(new float[]{1f, 1f, 0f}, PDDeviceRGB.INSTANCE));
        mark.setConstantOpacity(.45f);
        mark.setPrinted(true);
        mark.constructAppearances(document);
        add(page, mark);
    }
    @Override
    public synchronized List<String> formFields() {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) return List.of();
        List<String> names = new ArrayList<>();
        for (PDField field : form.getFieldTree()) names.add(field.getFullyQualifiedName());
        return List.copyOf(names);
    }
    @Override
    public synchronized void setFormField(String name, String value) throws IOException {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        PDField field = form == null ? null : form.getField(name);
        if (field == null) throw new IllegalArgumentException("Unknown field: " + name);
        if (field instanceof PDCheckBox check) {
            if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) check.check();
            else check.unCheck();
        } else field.setValue(value);
    }
    @Override
    public synchronized void addTextField(int page, String name, float x, float y, float width, float height) throws IOException {
        if (name == null || name.isBlank() || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid field");
        PDPage p = page(page);
        PDAcroForm form = ensureForm();
        if (form.getField(name) != null) throw new IllegalArgumentException("Duplicate field: " + name);
        PDTextField field = new PDTextField(form);
        field.setPartialName(name);
        field.setDefaultAppearance(String.format(java.util.Locale.ROOT, "/Helv %.1f Tf 0 g", Math.max(6, Math.min(12, height * .6f))));
        PDAnnotationWidget widget = field.getWidgets().getFirst();
        widget.setRectangle(new PDRectangle(x, y, width, height));
        widget.setPage(p);
        widget.setPrinted(true);
        decorate(widget);
        p.getAnnotations().add(widget);
        form.getFields().add(field);
        field.setValue("");
    }
    private void decorate(PDAnnotationWidget widget) {
        PDAppearanceCharacteristicsDictionary characteristics = new PDAppearanceCharacteristicsDictionary(new COSDictionary());
        characteristics.setBorderColour(new PDColor(new float[]{.55f, .58f, .63f}, PDDeviceRGB.INSTANCE));
        characteristics.setBackground(new PDColor(new float[]{.93f, .96f, 1f}, PDDeviceRGB.INSTANCE));
        widget.setAppearanceCharacteristics(characteristics);
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(1);
        widget.setBorderStyle(border);
    }
    private PDAcroForm ensureForm() {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) {
            form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
            form.setDefaultResources(new org.apache.pdfbox.pdmodel.PDResources());
            form.getDefaultResources().put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA));
            form.setDefaultAppearance("/Helv 12 Tf 0 g");
        }
        return form;
    }
    @Override
    public synchronized void addOcrLayer(int page, PdfOcrResult result, float dpi) throws IOException {
        PDPage p = page(page);
        float height = p.getMediaBox().getHeight();
        try (PDPageContentStream stream = new PDPageContentStream(document, p,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setRenderingMode(RenderingMode.NEITHER);
            for (PdfOcrWord word : result.words()) {
                if (word.text() == null || word.text().isBlank()) continue;
                float size = Math.max(4, word.height() * 72 / dpi);
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
                stream.newLineAtOffset(word.x() * 72 / dpi, height - (word.y() + word.height()) * 72 / dpi);
                try { stream.showText(word.text()); }
                catch (IllegalArgumentException unsupported) { stream.showText(word.text().replaceAll("[^\\x20-\\x7E]", "?")); }
                stream.endText();
            }
        }
    }
    @Override
    public synchronized boolean replaceText(int page, String original, String replacement,
                                                       Rectangle2D.Float area, boolean allowReconstruction) throws IOException {
        PDPage p = page(page);
        if (original == null || original.isBlank() || replacement == null) throw new IllegalArgumentException("Texto inválido");
        if (original.matches("[\\x20-\\x7E]+") && replacement.matches("[\\x20-\\x7E]*")
                && replacement.length() <= original.length()) {
            PDFStreamParser parser = new PDFStreamParser(p);
            List<Object> tokens = parser.parse();
            boolean changed = false;
            for (int i = 1; i < tokens.size(); i++) {
                if (tokens.get(i) instanceof Operator operator && "Tj".equals(operator.getName())
                        && tokens.get(i - 1) instanceof COSString word && original.equals(word.getString())) {
                    tokens.set(i - 1, new COSString(replacement)); changed = true; break;
                }
            }
            if (changed) {
                PDStream stream = new PDStream(document);
                try (var output = stream.createOutputStream()) { new ContentStreamWriter(output).writeTokens(tokens); }
                p.setContents(stream);
                return false;
            }
        }
        if (!allowReconstruction) throw new IOException("Texto não editável internamente neste PDF");
        if (area == null || area.width <= 0 || area.height <= 0)
            throw new IOException("Informe a área do texto para reconstruir a página");
        final float dpi = 200;
        BufferedImage image = new PDFRenderer(document).renderImageWithDPI(page, dpi);
        double scale = dpi / 72.0;
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            int x = (int)Math.floor(area.x * scale);
            int y = (int)Math.floor((p.getMediaBox().getHeight() - area.y - area.height) * scale);
            graphics.fillRect(x, y, (int)Math.ceil(area.width * scale), (int)Math.ceil(area.height * scale));
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(8, (int)(area.height * scale * .75))));
            graphics.drawString(replacement, x, y + (int)(area.height * scale * .8));
        } finally { graphics.dispose(); }
        PDImageXObject picture = LosslessFactory.createFromImage(document, image);
        PDStream stream = new PDStream(document);
        p.setContents(stream);
        try (PDPageContentStream content = new PDPageContentStream(document, p,
                PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            content.drawImage(picture, 0, 0, p.getMediaBox().getWidth(), p.getMediaBox().getHeight());
        }
        return true;
    }
    @Override
    public synchronized void addImage(int page, Path image, float x, float y, float width, float height) throws IOException {
        PDImageXObject picture = PDImageXObject.createFromFileByContent(image.toFile(), document);
        try (PDPageContentStream content = new PDPageContentStream(document, page(page),
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            content.drawImage(picture, x, y, width, height);
        }
    }
    @Override
    public synchronized List<String> imageResources(int page) throws IOException {
        PDResources resources = page(page).getResources();
        if (resources == null) return List.of();
        List<String> names = new ArrayList<>();
        for (COSName name : resources.getXObjectNames())
            if (resources.getXObject(name) instanceof PDImageXObject) names.add(name.getName());
        return List.copyOf(names);
    }
    @Override
    public synchronized void replaceImageResource(int page, String resourceName, Path image) throws IOException {
        PDPage p = page(page);
        PDResources original = p.getResources();
        COSName name = COSName.getPDFName(resourceName);
        if (original == null || !(original.getXObject(name) instanceof PDImageXObject))
            throw new IllegalArgumentException("Recurso de imagem não encontrado: " + resourceName);
        COSDictionary copied = new COSDictionary(); copied.addAll(original.getCOSObject());
        COSDictionary originalXObjects = (COSDictionary)copied.getDictionaryObject(COSName.XOBJECT);
        COSDictionary copiedXObjects = new COSDictionary(); copiedXObjects.addAll(originalXObjects);
        copied.setItem(COSName.XOBJECT, copiedXObjects);
        PDResources local = new PDResources(copied);
        local.put(name, PDImageXObject.createFromFileByContent(image.toFile(), document));
        p.setResources(local);
    }
    @Override
    public synchronized void addChoiceField(int page, String name, List<String> options,
                                                        float x, float y, float width, float height) throws IOException {
        if (options == null || options.isEmpty()) throw new IllegalArgumentException("Opções necessárias");
        PDPage p = page(page);
        PDAcroForm form = ensureForm();
        if (form.getField(name) != null) throw new IllegalArgumentException("Campo duplicado: " + name);
        PDComboBox field = new PDComboBox(form);
        field.setPartialName(name);
        field.setOptions(options);
        field.setDefaultAppearance(String.format(java.util.Locale.ROOT, "/Helv %.1f Tf 0 g", Math.max(6, Math.min(12, height * .6f))));
        PDAnnotationWidget widget = field.getWidgets().getFirst();
        widget.setRectangle(new PDRectangle(x, y, width, height));
        widget.setPage(p); widget.setPrinted(true);
        decorate(widget);
        p.getAnnotations().add(widget);
        form.getFields().add(field);
        field.setValue(options.getFirst());
    }
    @Override
    public synchronized void addCheckBox(int page, String name, float x, float y, float size) throws IOException {
        if (name == null || name.isBlank() || size <= 0) throw new IllegalArgumentException("Caixa inválida");
        PDPage p = page(page);
        PDAcroForm form = ensureForm();
        if (form.getField(name) != null) throw new IllegalArgumentException("Campo duplicado: " + name);
        PDCheckBox field = new PDCheckBox(form);
        field.setPartialName(name);
        PDAnnotationWidget widget = field.getWidgets().getFirst();
        widget.setRectangle(new PDRectangle(x, y, size, size));
        widget.setPage(p); widget.setPrinted(true);
        COSDictionary appearances = new COSDictionary();
        appearances.setItem(COSName.Off, checkAppearance(size, false));
        appearances.setItem(COSName.getPDFName("Yes"), checkAppearance(size, true));
        PDAppearanceDictionary appearance = new PDAppearanceDictionary();
        appearance.setNormalAppearance(new PDAppearanceEntry(appearances));
        widget.setAppearance(appearance);
        widget.setAppearanceState("Off");
        p.getAnnotations().add(widget);
        form.getFields().add(field);
    }
    private PDAppearanceStream checkAppearance(float size, boolean checked) throws IOException {
        PDAppearanceStream appearance = new PDAppearanceStream(document);
        appearance.setBBox(new PDRectangle(size, size));
        appearance.setResources(new PDResources());
        try (PDPageContentStream content = new PDPageContentStream(document, appearance)) {
            content.setNonStrokingColor(Color.WHITE);
            content.addRect(0, 0, size, size); content.fill();
            content.setStrokingColor(Color.BLACK);
            content.setLineWidth(1);
            content.addRect(.5f, .5f, size - 1, size - 1); content.stroke();
            if (checked) {
                content.setLineWidth(Math.max(1, size / 8));
                content.moveTo(size * .2f, size * .5f);
                content.lineTo(size * .42f, size * .25f);
                content.lineTo(size * .82f, size * .78f);
                content.stroke();
            }
        }
        return appearance;
    }
    @Override
    public synchronized void addRadioGroup(int page, String name, List<String> options,
                                                       float x, float y, float size, float gap) throws IOException {
        if (name == null || name.isBlank() || options == null || options.size() < 2 || size <= 0 || gap < 0)
            throw new IllegalArgumentException("Grupo de opções inválido");
        PDPage p = page(page);
        PDAcroForm form = ensureForm();
        if (form.getField(name) != null) throw new IllegalArgumentException("Campo duplicado: " + name);
        PDRadioButton field = new PDRadioButton(form);
        field.setPartialName(name);
        List<PDAnnotationWidget> widgets = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if (!option.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Opção inválida: " + option);
            PDAnnotationWidget widget = new PDAnnotationWidget();
            widget.setParent(field);
            widget.setRectangle(new PDRectangle(x, y - i * (size + gap), size, size));
            widget.setPage(p); widget.setPrinted(true);
            COSDictionary appearances = new COSDictionary();
            appearances.setItem(COSName.Off, checkAppearance(size, false));
            appearances.setItem(COSName.getPDFName(option), checkAppearance(size, true));
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.setNormalAppearance(new PDAppearanceEntry(appearances));
            widget.setAppearance(appearance); widget.setAppearanceState("Off");
            widgets.add(widget); p.getAnnotations().add(widget);
        }
        field.setWidgets(widgets);
        field.setExportValues(options);
        form.getFields().add(field);
    }
    @Override
    public synchronized void addSquare(int page, float x, float y, float width, float height) throws IOException {
        add(page, PdfBoxAnnotations.shape(document, PdfShapeKind.RECTANGLE, new Rectangle2D.Float(x, y, width, height),
                PdfShapeStyle.defaults()));
    }
    @Override
    public synchronized void addInk(int page, float[] points) throws IOException {
        add(page, PdfBoxAnnotations.ink(document, points, PdfShapeStyle.defaults().withStroke(Color.BLACK)));
    }
    @Override
    public synchronized void eraseArea(int page, Rectangle2D.Float area) throws IOException {
        if (area == null || area.width <= 0 || area.height <= 0) throw new IllegalArgumentException("Área inválida");
        PDPage selected = page(page);
        PDRectangle crop = selected.getCropBox();
        Rectangle2D.Float cropBounds = new Rectangle2D.Float(crop.getLowerLeftX(), crop.getLowerLeftY(),
                crop.getWidth(), crop.getHeight());
        if (!cropBounds.contains(area)) throw new IOException("A seleção deve ficar dentro da página");
        for (var annotation : selected.getAnnotations()) {
            PDRectangle box = annotation.getRectangle();
            if (annotation instanceof PDAnnotationWidget && box != null
                    && area.intersects(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight()))
                throw new IOException("A seleção cruza um campo interativo; mova ou remova o campo antes de apagar a área");
        }
        int originalRotation = selected.getRotation();
        BufferedImage image;
        selected.setRotation(0);
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setAnnotationsFilter(annotation -> false);
            image = renderer.renderImageWithDPI(page, 200);
        } finally { selected.setRotation(originalRotation); }
        double scale = 200.0 / 72.0;
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect((int)Math.floor((area.x - crop.getLowerLeftX()) * scale),
                    (int)Math.floor((crop.getUpperRightY() - area.y - area.height) * scale),
                    (int)Math.ceil(area.width * scale), (int)Math.ceil(area.height * scale));
        } finally { graphics.dispose(); }
        selected.getAnnotations().removeIf(annotation -> {
            PDRectangle box = annotation.getRectangle();
            return !(annotation instanceof PDAnnotationWidget) && box != null
                    && area.intersects(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight());
        });
        selected.setResources(new PDResources());
        selected.setContents(new PDStream(document));
        PDImageXObject replacement = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream content = new PDPageContentStream(document, selected,
                PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            content.drawImage(replacement, crop.getLowerLeftX(), crop.getLowerLeftY(),
                    crop.getWidth(), crop.getHeight());
        }
    }
    @Override
    public synchronized BufferedImage copyArea(int page, Rectangle2D.Float area, float dpi) throws IOException {
        if (area == null || area.width <= 0 || area.height <= 0 || dpi <= 0) throw new IllegalArgumentException("Área inválida");
        PDPage selected = page(page);
        PDRectangle crop = selected.getCropBox();
        if (!new Rectangle2D.Float(crop.getLowerLeftX(), crop.getLowerLeftY(), crop.getWidth(), crop.getHeight()).contains(area))
            throw new IOException("A seleção deve ficar dentro da página");
        int rotation = selected.getRotation();
        selected.setRotation(0);
        BufferedImage pageImage;
        try { pageImage = new PDFRenderer(document).renderImageWithDPI(page, dpi); }
        finally { selected.setRotation(rotation); }
        double scale = dpi / 72.0;
        int px = (int)Math.floor((area.x - crop.getLowerLeftX()) * scale);
        int py = (int)Math.floor((crop.getUpperRightY() - area.y - area.height) * scale);
        int w = Math.min((int)Math.ceil(area.width * scale), pageImage.getWidth() - px);
        int h = Math.min((int)Math.ceil(area.height * scale), pageImage.getHeight() - py);
        BufferedImage copied = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copied.createGraphics();
        try { graphics.drawImage(pageImage, 0, 0, w, h, px, py, px + w, py + h, null); }
        finally { graphics.dispose(); }
        return copied;
    }
    @Override
    public synchronized void pasteImage(int page, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        if (image == null || bounds == null || bounds.width <= 0 || bounds.height <= 0)
            throw new IllegalArgumentException("Imagem ou limites inválidos");
        PDImageXObject picture = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream content = new PDPageContentStream(document, page(page),
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            content.drawImage(picture, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
    @Override
    public synchronized void transformArea(int page, Rectangle2D.Float source,
                                                      Rectangle2D.Float destination, float clockwiseDegrees) throws IOException {
        if (source == null || destination == null || source.width <= 0 || source.height <= 0
                || destination.width <= 0 || destination.height <= 0 || !Float.isFinite(clockwiseDegrees))
            throw new IllegalArgumentException("Transformação inválida");
        PDPage selected = page(page);
        PDRectangle crop = selected.getCropBox();
        Rectangle2D.Float pageBounds = new Rectangle2D.Float(crop.getLowerLeftX(), crop.getLowerLeftY(),
                crop.getWidth(), crop.getHeight());
        if (!pageBounds.contains(source) || !pageBounds.contains(destination))
            throw new IOException("A transformação deve permanecer dentro da página");
        for (var annotation : selected.getAnnotations()) {
            PDRectangle box = annotation.getRectangle();
            if (box != null && source.intersects(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight()))
                throw new IOException("A área cruza uma anotação ou campo; selecione esse elemento separadamente");
        }
        int rotation = selected.getRotation();
        selected.setRotation(0);
        BufferedImage image;
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            renderer.setAnnotationsFilter(annotation -> false);
            image = renderer.renderImageWithDPI(page, 200);
        } finally { selected.setRotation(rotation); }
        double scale = 200.0 / 72.0;
        int sx = (int)Math.floor((source.x - crop.getLowerLeftX()) * scale);
        int sy = (int)Math.floor((crop.getUpperRightY() - source.y - source.height) * scale);
        int sw = Math.min((int)Math.ceil(source.width * scale), image.getWidth() - sx);
        int sh = Math.min((int)Math.ceil(source.height * scale), image.getHeight() - sy);
        BufferedImage cutout = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cut = cutout.createGraphics();
        try { cut.drawImage(image, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null); }
        finally { cut.dispose(); }
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE); graphics.fillRect(sx, sy, sw, sh);
            double dx = (destination.x - crop.getLowerLeftX()) * scale;
            double dy = (crop.getUpperRightY() - destination.y - destination.height) * scale;
            double dw = destination.width * scale, dh = destination.height * scale;
            AffineTransform before = graphics.getTransform();
            graphics.translate(dx + dw / 2, dy + dh / 2);
            graphics.rotate(Math.toRadians(clockwiseDegrees));
            graphics.drawImage(cutout, (int)Math.round(-dw / 2), (int)Math.round(-dh / 2),
                    (int)Math.round(dw), (int)Math.round(dh), null);
            graphics.setTransform(before);
        } finally { graphics.dispose(); }
        selected.setResources(new PDResources());
        selected.setContents(new PDStream(document));
        PDImageXObject replacement = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream content = new PDPageContentStream(document, selected,
                PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            content.drawImage(replacement, crop.getLowerLeftX(), crop.getLowerLeftY(),
                    crop.getWidth(), crop.getHeight());
        }
    }
    @Override
    public synchronized List<PdfPageElement> pageElements(int page) throws IOException {
        PDPage selected = page(page);
        List<PdfPageElement> elements = new ArrayList<>();
        if (canExtractContent()) elements.addAll(new PdfBoxPageContent(document, selected).elements());
        List<PDAnnotation> annotations = selected.getAnnotations();
        for (int index = 0; index < annotations.size(); index++) {
            PDAnnotation annotation = annotations.get(index);
            if (annotation instanceof PDAnnotationPopup) continue;
            PDRectangle box = annotation.getRectangle();
            if (box == null) continue;
            String text = annotation instanceof PDAnnotationWidget widget ? fieldName(widget)
                    : annotation.getContents() == null ? "" : annotation.getContents();
            elements.add(new PdfPageElement("annotation:" + index, annotation.getSubtype(),
                    new Rectangle2D.Float(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight()),
                    1_000_000 + index, true, text));
        }
        return List.copyOf(elements);
    }
    private int annotationIndex(int page, String id) throws IOException {
        int index = PdfBoxPageContent.index(id, "annotation:");
        List<PDAnnotation> annotations = page(page).getAnnotations();
        if (index >= 0 && index < annotations.size() && !(annotations.get(index) instanceof PDAnnotationPopup)) return index;
        throw new IOException("Elemento não encontrado: " + id);
    }
    private List<Integer> annotationIndexes(int page, PdfTarget target) throws IOException {
        return annotationIndexes(page, target, false);
    }
    private List<Integer> annotationIndexes(int page, PdfTarget target, boolean touching) throws IOException {
        List<PDAnnotation> annotations = page(page).getAnnotations();
        TreeSet<Integer> indexes = new TreeSet<>();
        for (String id : target.ids()) if (id.startsWith("annotation:")) indexes.add(annotationIndex(page, id));
        Rectangle2D.Float area = target.area();
        if (area != null) for (int index = 0; index < annotations.size(); index++) {
            PDAnnotation annotation = annotations.get(index);
            PDRectangle box = annotation.getRectangle();
            if (annotation instanceof PDAnnotationPopup || box == null) continue;
            boolean hit = touching ? area.intersects(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight())
                    : area.contains(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight());
            if (hit) indexes.add(index);
        }
        return new ArrayList<>(indexes);
    }
    private boolean hasContent(PdfTarget target) {
        return target.area() != null || target.ids().stream().anyMatch(id -> !id.startsWith("annotation:"));
    }
    private PdfBoxPageContent content(int page) throws IOException {
        if (!canExtractContent()) throw new IOException("Este PDF não permite editar o conteúdo da página");
        return new PdfBoxPageContent(document, page(page));
    }
    private void removeAnnotations(int page, List<Integer> indexes) throws IOException {
        PDPage selected = page(page);
        List<PDAnnotation> annotations = selected.getAnnotations();
        for (int i = indexes.size() - 1; i >= 0; i--) {
            PDAnnotation removed = annotations.remove((int) indexes.get(i));
            if (removed instanceof PDAnnotationWidget widget) detachWidget(widget);
        }
        selected.setAnnotations(annotations);
    }
    private void detachWidget(PDAnnotationWidget widget) {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) return;
        for (PDField field : form.getFieldTree()) {
            if (!(field instanceof PDTerminalField terminal)) continue;
            List<PDAnnotationWidget> widgets = new ArrayList<>(terminal.getWidgets());
            int position = -1;
            for (int index = 0; index < widgets.size(); index++)
                if (widgets.get(index).getCOSObject() == widget.getCOSObject()) position = index;
            if (position < 0) continue;
            if (field instanceof PDRadioButton radio && radio.getExportValues().size() == widgets.size() && widgets.size() > 1) {
                List<String> values = new ArrayList<>(radio.getExportValues());
                values.remove(position);
                radio.setExportValues(values);
            }
            widgets.remove(position);
            if (!widgets.isEmpty()) { terminal.setWidgets(widgets); return; }
            PDNonTerminalField parent = field.getParent();
            if (parent == null) {
                List<PDField> fields = new ArrayList<>(form.getFields());
                fields.removeIf(candidate -> candidate.getCOSObject() == field.getCOSObject());
                form.setFields(fields);
            } else {
                List<PDField> children = new ArrayList<>(parent.getChildren());
                children.removeIf(candidate -> candidate.getCOSObject() == field.getCOSObject());
                parent.setChildren(children);
            }
            return;
        }
    }
    private PDField field(String name) throws IOException {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        PDField field = form == null ? null : form.getField(name);
        if (field == null) throw new IOException("Campo não encontrado: " + name);
        return field;
    }
    private static float fontSize(PDField field) {
        if (!(field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDVariableText text) || text.getDefaultAppearance() == null) return 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9.]+)\\s+Tf").matcher(text.getDefaultAppearance());
        return matcher.find() ? Float.parseFloat(matcher.group(1)) : 0;
    }
    private static void refreshAppearance(PDField field) throws IOException {
        if (field instanceof PDTextField text) text.setValue(text.getValue() == null ? "" : text.getValue());
        else if (field instanceof PDComboBox combo && !combo.getValue().isEmpty()) combo.setValue(combo.getValue().getFirst());
    }
    @Override
    public synchronized void renameField(String name, String newName) throws IOException {
        if (newName == null || newName.isBlank() || newName.contains(".")) throw new IllegalArgumentException("Nome de campo inválido");
        PDField field = field(name);
        String clean = newName.strip();
        if (!clean.equals(field.getPartialName()) && document.getDocumentCatalog().getAcroForm().getField(clean) != null)
            throw new IllegalArgumentException("Já existe um campo chamado " + clean);
        field.setPartialName(clean);
    }
    @Override
    public synchronized void setFieldOptions(String name, List<String> options) throws IOException {
        List<String> clean = options.stream().map(String::strip).filter(option -> !option.isEmpty()).distinct().toList();
        if (clean.isEmpty()) throw new IllegalArgumentException("Informe pelo menos uma opção");
        if (!(field(name) instanceof org.apache.pdfbox.pdmodel.interactive.form.PDChoice choice))
            throw new IOException("O campo não é uma lista");
        List<String> current = choice.getValue();
        choice.setOptions(clean);
        choice.setValue(!current.isEmpty() && clean.contains(current.getFirst()) ? current.getFirst() : clean.getFirst());
    }
    @Override
    public synchronized void setFieldFlags(String name, boolean required, boolean readOnly, boolean multiline) throws IOException {
        PDField field = field(name);
        field.setRequired(required);
        field.setReadOnly(readOnly);
        if (field instanceof PDTextField text && text.isMultiline() != multiline) {
            text.setMultiline(multiline);
            refreshAppearance(text);
        }
    }
    @Override
    public synchronized void setFieldFontSize(String name, float size) throws IOException {
        if (!Float.isFinite(size) || size < 0 || size > 200) throw new IllegalArgumentException("Tamanho de fonte inválido");
        if (!(field(name) instanceof org.apache.pdfbox.pdmodel.interactive.form.PDVariableText text))
            throw new IOException("O campo não possui texto");
        text.setDefaultAppearance(String.format(java.util.Locale.ROOT, "/Helv %.1f Tf 0 g", size));
        refreshAppearance(text);
    }
    @Override
    public synchronized void addRadioOption(int page, String name, String option) throws IOException {
        if (option == null || !option.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Use letras, números ou _ no valor da opção");
        if (!(field(name) instanceof PDRadioButton radio)) throw new IOException("O campo não é um grupo de opções");
        List<String> values = new ArrayList<>(radio.getExportValues());
        if (values.contains(option)) throw new IllegalArgumentException("A opção já existe: " + option);
        List<PDAnnotationWidget> widgets = new ArrayList<>(radio.getWidgets());
        PDRectangle last = widgets.getLast().getRectangle();
        float size = last.getWidth();
        PDAnnotationWidget widget = new PDAnnotationWidget();
        widget.setParent(radio);
        widget.setRectangle(new PDRectangle(last.getLowerLeftX(), last.getLowerLeftY() - last.getHeight() - 6, size, last.getHeight()));
        widget.setPage(page(page));
        widget.setPrinted(true);
        COSDictionary appearances = new COSDictionary();
        appearances.setItem(COSName.Off, checkAppearance(size, false));
        appearances.setItem(COSName.getPDFName(option), checkAppearance(size, true));
        PDAppearanceDictionary appearance = new PDAppearanceDictionary();
        appearance.setNormalAppearance(new PDAppearanceEntry(appearances));
        widget.setAppearance(appearance);
        widget.setAppearanceState("Off");
        if (values.size() != widgets.size()) values = new ArrayList<>(values.subList(0, Math.min(values.size(), widgets.size())));
        widgets.add(widget);
        values.add(option);
        radio.setWidgets(widgets);
        radio.setExportValues(values);
        add(page, widget);
    }
    @Override
    public synchronized void renameRadioOption(int page, String id, String option) throws IOException {
        if (option == null || !option.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Use letras, números ou _ no valor da opção");
        if (!(page(page).getAnnotations().get(annotationIndex(page, id)) instanceof PDAnnotationWidget widget))
            throw new IOException("O elemento não é uma opção");
        PDRadioButton radio = null;
        for (PDField candidate : document.getDocumentCatalog().getAcroForm().getFieldTree())
            if (candidate instanceof PDRadioButton group && group.getWidgets().stream().anyMatch(item -> item.getCOSObject() == widget.getCOSObject()))
                radio = group;
        if (radio == null) throw new IOException("O elemento não é uma opção");
        if (radio.getExportValues().contains(option)) throw new IllegalArgumentException("A opção já existe: " + option);
        String old = null;
        for (COSName key : widget.getAppearance().getNormalAppearance().getSubDictionary().keySet())
            if (!COSName.Off.equals(key)) old = key.getName();
        if (old == null) throw new IOException("A opção não possui estado definido");
        boolean selected = old.equals(radio.getValue());
        for (COSName kind : List.of(COSName.N, COSName.D, COSName.R)) {
            if (!(widget.getAppearance().getCOSObject().getDictionaryObject(kind) instanceof COSDictionary states)) continue;
            COSBase stream = states.getItem(COSName.getPDFName(old));
            if (stream == null) continue;
            states.removeItem(COSName.getPDFName(old));
            states.setItem(COSName.getPDFName(option), stream);
        }
        List<String> values = new ArrayList<>(radio.getExportValues());
        int index = values.indexOf(old);
        if (index >= 0) values.set(index, option);
        radio.setExportValues(values);
        if (selected) {
            widget.setAppearanceState(option);
            radio.getCOSObject().setName(COSName.V, option);
        }
    }
    private String fieldName(PDAnnotationWidget widget) {
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) return "";
        for (PDField field : form.getFieldTree())
            if (field instanceof PDTerminalField terminal
                    && terminal.getWidgets().stream().anyMatch(candidate -> candidate.getCOSObject() == widget.getCOSObject()))
                return field.getFullyQualifiedName();
        return "";
    }
    @Override
    public synchronized void deleteTarget(int page, PdfTarget target) throws IOException {
        List<Integer> indexes = annotationIndexes(page, target);
        if (hasContent(target)) {
            PdfBoxPageContent content = content(page);
            content.remove(content.glyphs(target), content.items(target));
            content.commit();
        }
        removeAnnotations(page, indexes);
    }
    @Override
    public synchronized void eraseRegion(int page, Rectangle2D.Float area) throws IOException {
        if (area == null || area.width <= 0 || area.height <= 0) throw new IllegalArgumentException("Área inválida");
        eraseShape(page, area);
    }
    @Override
    public synchronized void eraseShape(int page, java.awt.Shape area) throws IOException {
        Rectangle2D bounds = area.getBounds2D();
        if (bounds.isEmpty()) throw new IllegalArgumentException("Área inválida");
        PDPage selected = page(page);
        List<PDAnnotation> annotations = selected.getAnnotations();
        List<Integer> remove = new ArrayList<>();
        for (int index = 0; index < annotations.size(); index++) {
            PDAnnotation annotation = annotations.get(index);
            PDRectangle box = annotation.getRectangle();
            if (annotation instanceof PDAnnotationPopup || box == null) continue;
            Rectangle2D rect = new Rectangle2D.Double(box.getLowerLeftX(), box.getLowerLeftY(), box.getWidth(), box.getHeight());
            if (!area.intersects(rect)) continue;
            if (annotation instanceof PDAnnotationWidget) {
                if (area.contains(rect.getCenterX(), rect.getCenterY())) remove.add(index);
            } else if (area.contains(rect) || !PdfBoxAnnotations.clipAppearance(document, annotation, area)) remove.add(index);
        }
        if (canExtractContent()) {
            PdfBoxPageContent content = content(page);
            content.eraseShape(area);
            content.commit();
        }
        removeAnnotations(page, remove);
    }
    @Override
    public synchronized Optional<PdfFieldInfo> fieldInfo(int page, String id) throws IOException {
        if (!(page(page).getAnnotations().get(annotationIndex(page, id)) instanceof PDAnnotationWidget widget)) return Optional.empty();
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        if (form == null) return Optional.empty();
        for (PDField field : form.getFieldTree()) {
            if (!(field instanceof PDTerminalField terminal)
                    || terminal.getWidgets().stream().noneMatch(candidate -> candidate.getCOSObject() == widget.getCOSObject())) continue;
            String state = "";
            if (widget.getAppearance() != null && widget.getAppearance().getNormalAppearance() != null
                    && widget.getAppearance().getNormalAppearance().isSubDictionary())
                for (COSName name : widget.getAppearance().getNormalAppearance().getSubDictionary().keySet())
                    if (!COSName.Off.equals(name)) state = name.getName();
            PdfFieldKind kind = field instanceof PDCheckBox ? PdfFieldKind.CHECKBOX : field instanceof PDRadioButton ? PdfFieldKind.RADIO
                    : field instanceof PDTextField ? PdfFieldKind.TEXT
                    : field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDChoice ? PdfFieldKind.CHOICE : PdfFieldKind.OTHER;
            String value = field instanceof PDCheckBox check ? String.valueOf(check.isChecked()) : field.getValueAsString();
            List<String> options = field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDChoice choice ? choice.getOptionsDisplayValues()
                    : field instanceof PDRadioButton radio ? radio.getExportValues() : List.of();
            boolean multiline = field instanceof PDTextField text && text.isMultiline();
            return Optional.of(new PdfFieldInfo(field.getFullyQualifiedName(), kind, value, options, state,
                    field.isRequired(), field.isReadOnly(), multiline, fontSize(field)));
        }
        return Optional.empty();
    }
    @Override
    public synchronized void transformTarget(int page, PdfTarget target, AffineTransform transform) throws IOException {
        if (transform == null || transform.getDeterminant() == 0) throw new IllegalArgumentException("Transformação inválida");
        if (hasContent(target)) {
            PdfBoxPageContent content = content(page);
            content.transform(content.glyphs(target), content.items(target), transform);
            content.commit();
        }
        List<PDAnnotation> annotations = page(page).getAnnotations();
        for (int index : annotationIndexes(page, target)) PdfBoxAnnotations.transform(document, annotations.get(index), transform);
    }
    @Override
    public synchronized void duplicateTarget(int page, PdfTarget target, AffineTransform transform) throws IOException {
        if (hasContent(target)) {
            PdfBoxPageContent content = content(page);
            content.duplicate(content.glyphs(target), content.items(target), transform);
            content.commit();
        }
        PDPage selected = page(page);
        List<PDAnnotation> annotations = selected.getAnnotations();
        List<PDAnnotation> copies = new ArrayList<>();
        for (int index : annotationIndexes(page, target)) {
            PDAnnotation copy = PDAnnotation.createAnnotation(PdfBoxAnnotations.copy(annotations.get(index).getCOSObject()));
            copy.setPage(selected);
            PdfBoxAnnotations.transform(document, copy, transform);
            copies.add(copy);
        }
        annotations.addAll(copies);
        selected.setAnnotations(annotations);
    }
    @Override
    public synchronized void replaceElementText(int page, String id, String text) throws IOException {
        if (id.startsWith("annotation:")) {
            PDAnnotation annotation = page(page).getAnnotations().get(annotationIndex(page, id));
            if (annotation instanceof PDAnnotationFreeText box) {
                PdfBoxAnnotations.updateTextBox(document, box, text, PdfBoxAnnotations.textStyle(box));
                return;
            }
            annotation.setContents(text);
            return;
        }
        PdfBoxPageContent content = content(page);
        PdfPageElement element = content.elements().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new IOException("Texto não encontrado: " + id));
        Set<PdfBoxGlyph> glyphs = content.glyphs(PdfTarget.of(id));
        if (glyphs.isEmpty()) throw new IOException("Este texto não pode ser editado diretamente");
        PdfBoxGlyph first = glyphs.iterator().next();
        Color color = first.fill == null ? Color.BLACK : new Color(first.fill[0], first.fill[1], first.fill[2]);
        PDType1Font font = PdfBoxAnnotations.font(PdfBoxAnnotations.styleFor(first.font.getName(),
                element.bounds().height, color));
        content.replaceWord(id, PdfBoxAnnotations.safe(font, text == null ? "" : text), font);
        content.commit();
    }
    @Override
    public synchronized void addShape(int page, PdfShapeKind kind, Rectangle2D.Float bounds, PdfShapeStyle style) throws IOException {
        add(page, PdfBoxAnnotations.shape(document, kind, bounds, style));
    }
    @Override
    public synchronized void addLine(int page, float x1, float y1, float x2, float y2, boolean arrow,
                                               PdfShapeStyle style) throws IOException {
        if (Math.hypot(x2 - x1, y2 - y1) < 1) throw new IllegalArgumentException("Linha sem comprimento");
        add(page, PdfBoxAnnotations.line(document, x1, y1, x2, y2, arrow, style));
    }
    @Override
    public synchronized void addInk(int page, float[] points, PdfShapeStyle style) throws IOException {
        add(page, PdfBoxAnnotations.ink(document, points, style));
    }
    @Override
    public synchronized void addTextBox(int page, Rectangle2D.Float bounds, String text, PdfTextStyle style) throws IOException {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Texto vazio");
        add(page, PdfBoxAnnotations.textBox(document, bounds, text, style));
    }
    @Override
    public synchronized void updateTextBox(int page, String id, String text, PdfTextStyle style) throws IOException {
        if (!(page(page).getAnnotations().get(annotationIndex(page, id)) instanceof PDAnnotationFreeText box))
            throw new IOException("O elemento não é uma caixa de texto");
        PdfBoxAnnotations.updateTextBox(document, box, text, style);
    }
    @Override
    public synchronized Optional<PdfTextStyle> textBoxStyle(int page, String id) throws IOException {
        return page(page).getAnnotations().get(annotationIndex(page, id)) instanceof PDAnnotationFreeText box
                ? Optional.of(PdfBoxAnnotations.textStyle(box)) : Optional.empty();
    }
    @Override
    public synchronized Optional<String> textBoxText(int page, String id) throws IOException {
        return page(page).getAnnotations().get(annotationIndex(page, id)) instanceof PDAnnotationFreeText box
                ? Optional.of(box.getContents() == null ? "" : box.getContents()) : Optional.empty();
    }
    @Override
    public synchronized void addImageStamp(int page, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        add(page, PdfBoxAnnotations.imageStamp(document, image, bounds));
    }
    @Override
    public synchronized void setAnnotationStyle(int page, String id, PdfShapeStyle style) throws IOException {
        PDAnnotation annotation = page(page).getAnnotations().get(annotationIndex(page, id));
        if (annotation instanceof PDAnnotationFreeText box) {
            PdfBoxAnnotations.updateTextBox(document, box, box.getContents() == null ? "" : box.getContents(),
                    PdfBoxAnnotations.textStyle(box).withColor(style.stroke()));
        } else if (annotation instanceof PDAnnotationMarkup markup && !(annotation instanceof PDAnnotationText)
                && !(annotation instanceof PDAnnotationRubberStamp)) {
            PdfBoxAnnotations.styleShape(document, markup, style);
        } else throw new IOException("Este elemento não possui estilo editável");
    }
    @Override
    public synchronized byte[] exportAnnotations(int page, List<String> ids) throws IOException {
        PDPage source = page(page);
        List<PDAnnotation> annotations = source.getAnnotations();
        try (PDDocument exported = new PDDocument()) {
            PDPage target = new PDPage(source.getCropBox());
            exported.addPage(target);
            List<PDAnnotation> copies = new ArrayList<>();
            for (String id : ids) {
                PDAnnotation copy = PDAnnotation.createAnnotation(
                        PdfBoxAnnotations.copy(annotations.get(annotationIndex(page, id)).getCOSObject()));
                copy.setPage(target);
                copies.add(copy);
            }
            target.setAnnotations(copies);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            exported.save(output);
            return output.toByteArray();
        }
    }
    @Override
    public synchronized void importAnnotations(int page, byte[] data, float dx, float dy) throws IOException {
        PDPage selected = page(page);
        List<PDAnnotation> annotations = selected.getAnnotations();
        try (PDDocument imported = Loader.loadPDF(data)) {
            for (PDAnnotation annotation : imported.getPage(0).getAnnotations()) {
                PDAnnotation copy = PDAnnotation.createAnnotation(PdfBoxAnnotations.copy(annotation.getCOSObject()));
                copy.setPage(selected);
                PdfBoxAnnotations.transform(document, copy, AffineTransform.getTranslateInstance(dx, dy));
                annotations.add(copy);
            }
        }
        selected.setAnnotations(annotations);
    }
    @Override
    public synchronized void insertBlankPage(int destination, float width, float height) throws IOException {
        if (destination < 0 || destination > pageCount()) throw new IndexOutOfBoundsException(destination);
        PDPage blank = new PDPage(new PDRectangle(width, height));
        if (destination == pageCount()) document.addPage(blank);
        else document.getPages().insertBefore(blank, document.getPage(destination));
    }
    private void add(int page, PDAnnotation annotation) throws IOException {
        PDPage selected = page(page);
        annotation.setPage(selected);
        List<PDAnnotation> annotations = selected.getAnnotations();
        annotations.add(annotation);
        selected.setAnnotations(annotations);
    }
    @Override
    public synchronized void movePageElementLayer(int page, String id, boolean forward) throws IOException {
        var annotations = page(page).getAnnotations();
        int index = annotationIndex(page, id);
        int target = index + (forward ? 1 : -1);
        if (target < 0 || target >= annotations.size()) return;
        java.util.Collections.swap(annotations, index, target);
    }
    @Override
    public synchronized void extractPages(int first, int last, Path destination) throws IOException {
        if (first < 0 || last < first || last >= pageCount()) throw new IndexOutOfBoundsException();
        List<Integer> pages = new ArrayList<>();
        for (int page = first; page <= last; page++) pages.add(page);
        extractPages(pages, destination);
    }
    @Override
    public synchronized void extractPages(List<Integer> pages, Path destination) throws IOException {
        if (pages == null || pages.isEmpty()) throw new IllegalArgumentException("Nenhuma página selecionada");
        List<Integer> order = new ArrayList<>();
        for (int page : pages) {
            if (page < 0 || page >= pageCount()) throw new IndexOutOfBoundsException(page);
            if (!order.contains(page)) order.add(page);
        }
        try (PDDocument copy = Loader.loadPDF(snapshot())) {
            List<PDPage> kept = new ArrayList<>();
            for (int page : order) kept.add(copy.getPage(page));
            for (int index = copy.getNumberOfPages() - 1; index >= 0; index--) copy.removePage(index);
            for (PDPage page : kept) copy.addPage(page);
            pruneForm(copy, kept);
            copy.getDocumentCatalog().getCOSObject().removeItem(COSName.getPDFName("Perms"));
            copy.save(destination.toFile());
        }
    }
    private static void pruneForm(PDDocument copy, List<PDPage> kept) throws IOException {
        PDAcroForm form = copy.getDocumentCatalog().getAcroForm();
        if (form == null) return;
        java.util.Set<COSBase> remaining = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (PDPage page : kept) for (PDAnnotation annotation : page.getAnnotations()) remaining.add(annotation.getCOSObject());
        List<PDField> removed = new ArrayList<>();
        for (PDField field : form.getFieldTree()) {
            if (!(field instanceof PDTerminalField terminal)) continue;
            if (field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField) { removed.add(field); continue; }
            List<PDAnnotationWidget> widgets = new ArrayList<>(terminal.getWidgets());
            boolean merged = widgets.size() == 1 && widgets.getFirst().getCOSObject() == field.getCOSObject();
            if (!widgets.removeIf(widget -> !remaining.contains(widget.getCOSObject()))) continue;
            if (widgets.isEmpty()) removed.add(field);
            else if (!merged) terminal.setWidgets(widgets);
        }
        for (PDPage page : kept) {
            List<PDAnnotation> annotations = page.getAnnotations();
            if (annotations.removeIf(annotation -> annotation instanceof PDAnnotationWidget widget && removed.stream()
                    .anyMatch(field -> field instanceof PDTerminalField terminal && terminal.getWidgets().stream()
                            .anyMatch(item -> item.getCOSObject() == widget.getCOSObject()))))
                page.setAnnotations(annotations);
        }
        for (PDField field : removed) {
            org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField parent = field.getParent();
            if (parent == null) {
                List<PDField> fields = new ArrayList<>(form.getFields());
                fields.removeIf(candidate -> candidate.getCOSObject() == field.getCOSObject());
                form.setFields(fields);
            } else {
                List<PDField> children = new ArrayList<>(parent.getChildren());
                children.removeIf(candidate -> candidate.getCOSObject() == field.getCOSObject());
                parent.setChildren(children);
            }
        }
        if (form.getFields().isEmpty()) copy.getDocumentCatalog().setAcroForm(null);
        else form.setSignaturesExist(false);
    }
    @Override
    public synchronized String title() { return document.getDocumentInformation().getTitle(); }
    @Override
    public synchronized void setTitle(String title) { document.getDocumentInformation().setTitle(title); }
    @Override
    public synchronized boolean hasSignatures() { return !document.getSignatureDictionaries().isEmpty(); }
    @Override
    public synchronized boolean canModify() { return document.getCurrentAccessPermission().canModify(); }
    @Override
    public synchronized boolean canExtractContent() { return document.getCurrentAccessPermission().canExtractContent(); }
    @Override
    public synchronized void print() throws IOException {
        if (!document.getCurrentAccessPermission().canPrint()) throw new IOException("Impressão não permitida neste PDF");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPageable(new PDFPageable(document));
        if (job.printDialog()) try { job.print(); }
        catch (java.awt.print.PrinterException error) { throw new IOException("Falha na impressão", error); }
    }
    @Override
    public synchronized void close() throws IOException { document.close(); }
}
