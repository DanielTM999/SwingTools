package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFreeText;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationInk;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLine;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquare;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationSquareCircle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PdfBoxAnnotations {
    static final COSName STYLE = COSName.getPDFName("SwingToolsStyle");
    private static final float PAD = 3;
    private static final Set<COSName> SKIPPED = Set.of(COSName.P, COSName.PARENT, COSName.getPDFName("Popup"),
            COSName.getPDFName("IRT"));

    private PdfBoxAnnotations() {}

    static PDColor color(Color color) {
        return new PDColor(new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f},
                PDDeviceRGB.INSTANCE);
    }

    static PDAnnotationMarkup shape(PDDocument document, PdfShapeKind kind, Rectangle2D.Float bounds, PdfShapeStyle style) throws IOException {
        if (bounds.width <= 0 || bounds.height <= 0) throw new IllegalArgumentException("Forma sem tamanho");
        return switch (kind) {
            case RECTANGLE, ELLIPSE -> {
                PDAnnotationSquareCircle shape = kind == PdfShapeKind.RECTANGLE ? new PDAnnotationSquare() : new PDAnnotationCircle();
                shape.setRectangle(new PDRectangle(bounds.x, bounds.y, bounds.width, bounds.height));
                styleShape(document, shape, style);
                yield shape;
            }
            case LINE, ARROW -> line(document, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                    kind == PdfShapeKind.ARROW, style);
        };
    }

    static PDAnnotationLine line(PDDocument document, float x1, float y1, float x2, float y2, boolean arrow,
                                 PdfShapeStyle style) throws IOException {
        PDAnnotationLine line = new PDAnnotationLine();
        line.setLine(new float[]{x1, y1, x2, y2});
        float margin = Math.max(6, style.lineWidth() * 4);
        line.setRectangle(new PDRectangle(Math.min(x1, x2) - margin, Math.min(y1, y2) - margin,
                Math.abs(x2 - x1) + 2 * margin, Math.abs(y2 - y1) + 2 * margin));
        if (arrow) line.setEndPointEndingStyle(PDAnnotationLine.LE_CLOSED_ARROW);
        styleShape(document, line, style);
        return line;
    }

    static PDAnnotationInk ink(PDDocument document, float[] points, PdfShapeStyle style) throws IOException {
        if (points == null || points.length < 4 || points.length % 2 != 0) throw new IllegalArgumentException("Traço inválido");
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < points.length; i += 2) {
            minX = Math.min(minX, points[i]); maxX = Math.max(maxX, points[i]);
            minY = Math.min(minY, points[i + 1]); maxY = Math.max(maxY, points[i + 1]);
        }
        float margin = Math.max(2, style.lineWidth());
        PDAnnotationInk ink = new PDAnnotationInk();
        ink.setInkList(new float[][]{points.clone()});
        ink.setRectangle(new PDRectangle(minX - margin, minY - margin, Math.max(1, maxX - minX) + 2 * margin,
                Math.max(1, maxY - minY) + 2 * margin));
        styleShape(document, ink, style);
        return ink;
    }

    static void styleShape(PDDocument document, PDAnnotationMarkup shape, PdfShapeStyle style) throws IOException {
        shape.setColor(color(style.stroke()));
        if (style.fill() == null) shape.getCOSObject().removeItem(COSName.IC);
        else if (shape instanceof PDAnnotationSquareCircle filled) filled.setInteriorColor(color(style.fill()));
        else if (shape instanceof PDAnnotationLine line) line.setInteriorColor(color(style.fill()));
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(style.lineWidth());
        shape.setBorderStyle(border);
        shape.setConstantOpacity(style.opacity());
        shape.setPrinted(true);
        float rotation = rotation(shape);
        shape.constructAppearances(document);
        if (rotation != 0) rotateAppearance(shape, rotation);
    }

    static PDAnnotationFreeText textBox(PDDocument document, Rectangle2D.Float bounds, String text, PdfTextStyle style) throws IOException {
        PDType1Font font = font(style);
        boolean fixed = bounds.width >= 12;
        float width = fixed ? bounds.width : 0;
        List<String> lines = lines(font, style.size(), text, fixed ? width - 2 * PAD : 0);
        if (!fixed) for (String line : lines) width = Math.max(width, font.getStringWidth(safe(font, line)) / 1000 * style.size());
        if (!fixed) width += 2 * PAD + 2;
        float height = Math.max(bounds.height, lines.size() * style.size() * 1.2f + 2 * PAD);
        float top = bounds.y + bounds.height;
        PDAnnotationFreeText box = new PDAnnotationFreeText();
        box.setRectangle(new PDRectangle(bounds.x, top - height, width, height));
        box.setPrinted(true);
        writeTextBox(document, box, text, style, width, height, 0);
        return box;
    }

    static void updateTextBox(PDDocument document, PDAnnotationFreeText box, String text, PdfTextStyle style) throws IOException {
        COSDictionary stored = box.getCOSObject().getCOSDictionary(STYLE);
        PDRectangle rect = box.getRectangle();
        float width = stored == null ? rect.getWidth() : stored.getFloat(COSName.W, rect.getWidth());
        float height = stored == null ? rect.getHeight() : stored.getFloat(COSName.H, rect.getHeight());
        float rotation = stored == null ? 0 : stored.getFloat(COSName.ROTATE, 0);
        PDType1Font font = font(style);
        List<String> lines = lines(font, style.size(), text, width - 2 * PAD);
        float needed = lines.size() * style.size() * 1.2f + 2 * PAD;
        if (needed > height && rotation == 0) {
            box.setRectangle(new PDRectangle(rect.getLowerLeftX(), rect.getUpperRightY() - needed, rect.getWidth(), needed));
            height = needed;
        }
        writeTextBox(document, box, text, style, width, Math.max(height, needed), rotation);
    }

    private static void writeTextBox(PDDocument document, PDAnnotationFreeText box, String text, PdfTextStyle style,
                                     float width, float height, float rotation) throws IOException {
        PDType1Font font = font(style);
        List<String> lines = lines(font, style.size(), text, width - 2 * PAD);
        box.setContents(text);
        box.setDefaultAppearance(String.format(java.util.Locale.ROOT, "/Helv %.1f Tf %.3f %.3f %.3f rg", style.size(),
                style.color().getRed() / 255f, style.color().getGreen() / 255f, style.color().getBlue() / 255f));
        COSDictionary stored = new COSDictionary();
        stored.setName(COSName.getPDFName("Family"), style.family());
        stored.setFloat(COSName.SIZE, style.size());
        COSArray color = new COSArray();
        color.add(new COSFloat(style.color().getRed() / 255f));
        color.add(new COSFloat(style.color().getGreen() / 255f));
        color.add(new COSFloat(style.color().getBlue() / 255f));
        stored.setItem(COSName.C, color);
        stored.setBoolean(COSName.getPDFName("Bold"), style.bold());
        stored.setBoolean(COSName.getPDFName("Italic"), style.italic());
        stored.setFloat(COSName.W, width);
        stored.setFloat(COSName.H, height);
        stored.setFloat(COSName.ROTATE, rotation);
        box.getCOSObject().setItem(STYLE, stored);
        PDAppearanceStream appearance = new PDAppearanceStream(document);
        appearance.setBBox(new PDRectangle(width, height));
        appearance.setResources(new PDResources());
        try (PDPageContentStream content = new PDPageContentStream(document, appearance)) {
            content.beginText();
            content.setFont(font, style.size());
            content.setNonStrokingColor(style.color());
            float ascent = font.getFontDescriptor() == null ? 750 : font.getFontDescriptor().getAscent();
            content.newLineAtOffset(PAD, height - PAD - ascent / 1000 * style.size());
            for (String line : lines) {
                content.showText(safe(font, line));
                content.newLineAtOffset(0, -style.size() * 1.2f);
            }
            content.endText();
        }
        if (rotation != 0) appearance.setMatrix(AffineTransform.getRotateInstance(Math.toRadians(rotation)));
        PDAppearanceDictionary dictionary = new PDAppearanceDictionary();
        dictionary.setNormalAppearance(appearance);
        box.setAppearance(dictionary);
    }

    static PdfTextStyle textStyle(PDAnnotationFreeText box) {
        COSDictionary stored = box.getCOSObject().getCOSDictionary(STYLE);
        if (stored == null) return PdfTextStyle.defaults();
        Color color = Color.BLACK;
        if (stored.getDictionaryObject(COSName.C) instanceof COSArray array && array.size() == 3) {
            float[] values = array.toFloatArray();
            color = new Color(clamp(values[0]), clamp(values[1]), clamp(values[2]));
        }
        String family = stored.getNameAsString(COSName.getPDFName("Family"), PdfTextStyle.HELVETICA);
        return new PdfTextStyle(family, stored.getFloat(COSName.SIZE, 14), color,
                stored.getBoolean(COSName.getPDFName("Bold"), false), stored.getBoolean(COSName.getPDFName("Italic"), false));
    }

    static PDAnnotationRubberStamp imageStamp(PDDocument document, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        if (image == null || bounds.width <= 0 || bounds.height <= 0) throw new IllegalArgumentException("Imagem inválida");
        boolean alpha = image.getColorModel().hasAlpha();
        PDImageXObject picture = !alpha && (long) image.getWidth() * image.getHeight() > 400_000
                ? JPEGFactory.createFromImage(document, image, .9f) : LosslessFactory.createFromImage(document, image);
        PDAnnotationRubberStamp stamp = new PDAnnotationRubberStamp();
        stamp.setRectangle(new PDRectangle(bounds.x, bounds.y, bounds.width, bounds.height));
        stamp.setPrinted(true);
        stamp.getCOSObject().setName(COSName.NAME, "SwingToolsImage");
        PDAppearanceStream appearance = new PDAppearanceStream(document);
        appearance.setBBox(new PDRectangle(bounds.width, bounds.height));
        appearance.setResources(new PDResources());
        try (PDPageContentStream content = new PDPageContentStream(document, appearance)) {
            content.drawImage(picture, 0, 0, bounds.width, bounds.height);
        }
        PDAppearanceDictionary dictionary = new PDAppearanceDictionary();
        dictionary.setNormalAppearance(appearance);
        stamp.setAppearance(dictionary);
        return stamp;
    }

    static void transform(PDDocument document, PDAnnotation annotation, AffineTransform transform) throws IOException {
        PDRectangle rect = annotation.getRectangle();
        if (rect == null) return;
        Rectangle2D moved = PdfBoxContentScanner.transformBox(transform, rect.getLowerLeftX(), rect.getLowerLeftY(),
                rect.getUpperRightX(), rect.getUpperRightY());
        COSDictionary dictionary = annotation.getCOSObject();
        for (String key : List.of("L", "QuadPoints", "Vertices", "CL")) points(dictionary.getDictionaryObject(COSName.getPDFName(key)), transform);
        if (dictionary.getDictionaryObject(COSName.getPDFName("InkList")) instanceof COSArray strokes)
            for (int i = 0; i < strokes.size(); i++) points(strokes.getObject(i), transform);
        double degrees = Math.toDegrees(Math.atan2(transform.getShearY(), transform.getScaleX()));
        boolean rotates = Math.abs(degrees) > .01;
        COSDictionary stored = dictionary.getCOSDictionary(STYLE);
        if (rotates) {
            PDAppearanceStream appearance = annotation.getNormalAppearanceStream();
            if (appearance == null) {
                annotation.constructAppearances(document);
                appearance = annotation.getNormalAppearanceStream();
            }
            if (appearance != null) {
                AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians(degrees));
                Matrix matrix = appearance.getMatrix();
                if (matrix != null) rotate.concatenate(matrix.createAffineTransform());
                appearance.setMatrix(rotate);
            }
            if (stored == null) { stored = new COSDictionary(); dictionary.setItem(STYLE, stored); }
            stored.setFloat(COSName.ROTATE, (float) (stored.getFloat(COSName.ROTATE, 0) + degrees));
        } else if (stored != null && stored.getFloat(COSName.ROTATE, 0) == 0) {
            stored.setFloat(COSName.W, (float) (stored.getFloat(COSName.W, rect.getWidth()) * Math.abs(transform.getScaleX())));
            stored.setFloat(COSName.H, (float) (stored.getFloat(COSName.H, rect.getHeight()) * Math.abs(transform.getScaleY())));
        }
        annotation.setRectangle(new PDRectangle((float) moved.getX(), (float) moved.getY(),
                (float) Math.max(1, moved.getWidth()), (float) Math.max(1, moved.getHeight())));
        if (annotation instanceof PDAnnotationFreeText box && !rotates && stored != null && stored.containsKey(COSName.SIZE))
            updateTextBox(document, box, box.getContents() == null ? "" : box.getContents(), textStyle(box));
    }

    static boolean clipAppearance(PDDocument document, PDAnnotation annotation, Shape pageArea) throws IOException {
        PDAppearanceStream appearance = annotation.getNormalAppearanceStream();
        if (appearance == null) {
            annotation.constructAppearances(document);
            appearance = annotation.getNormalAppearanceStream();
        }
        PDRectangle rect = annotation.getRectangle();
        PDRectangle box = appearance == null ? null : appearance.getBBox();
        if (appearance == null || rect == null || box == null || box.getWidth() <= 0 || box.getHeight() <= 0) return false;
        Matrix matrix = appearance.getMatrix();
        AffineTransform form = matrix == null ? new AffineTransform() : matrix.createAffineTransform();
        Rectangle2D transformed = PdfBoxContentScanner.transformBox(form, box.getLowerLeftX(), box.getLowerLeftY(),
                box.getUpperRightX(), box.getUpperRightY());
        if (transformed.getWidth() <= 0 || transformed.getHeight() <= 0) return false;
        AffineTransform toPage = new AffineTransform();
        toPage.translate(rect.getLowerLeftX(), rect.getLowerLeftY());
        toPage.scale(rect.getWidth() / transformed.getWidth(), rect.getHeight() / transformed.getHeight());
        toPage.translate(-transformed.getX(), -transformed.getY());
        toPage.concatenate(form);
        AffineTransform inverse;
        try { inverse = toPage.createInverse(); }
        catch (java.awt.geom.NoninvertibleTransformException error) { return false; }
        List<Object> tokens = new ArrayList<>(new org.apache.pdfbox.pdfparser.PDFStreamParser(appearance).parse());
        Rectangle2D outer = new Rectangle2D.Double(box.getLowerLeftX() - 1000, box.getLowerLeftY() - 1000,
                box.getWidth() + 2000, box.getHeight() + 2000);
        List<Object> output = new ArrayList<>(List.of(PdfBoxPageContent.operator("q")));
        output.addAll(PdfBoxPageContent.path(outer, new AffineTransform()));
        output.addAll(PdfBoxPageContent.path(pageArea, inverse));
        output.add(PdfBoxPageContent.operator("W*"));
        output.add(PdfBoxPageContent.operator("n"));
        output.addAll(tokens);
        output.add(PdfBoxPageContent.operator("Q"));
        try (OutputStream out = appearance.getCOSObject().createOutputStream(COSName.FLATE_DECODE)) {
            new org.apache.pdfbox.pdfwriter.ContentStreamWriter(out).writeTokens(output);
        }
        return true;
    }

    private static void points(COSBase value, AffineTransform transform) {
        if (!(value instanceof COSArray array)) return;
        for (int i = 0; i + 1 < array.size(); i += 2) {
            if (!(array.getObject(i) instanceof COSNumber x) || !(array.getObject(i + 1) instanceof COSNumber y)) continue;
            double[] point = {x.floatValue(), y.floatValue()};
            transform.transform(point, 0, point, 0, 1);
            array.set(i, new COSFloat((float) point[0]));
            array.set(i + 1, new COSFloat((float) point[1]));
        }
    }

    private static float rotation(PDAnnotation annotation) {
        COSDictionary stored = annotation.getCOSObject().getCOSDictionary(STYLE);
        return stored == null ? 0 : stored.getFloat(COSName.ROTATE, 0);
    }

    private static void rotateAppearance(PDAnnotation annotation, float degrees) {
        PDAppearanceStream appearance = annotation.getNormalAppearanceStream();
        if (appearance != null) appearance.setMatrix(AffineTransform.getRotateInstance(Math.toRadians(degrees)));
    }

    static COSDictionary copy(COSDictionary source) throws IOException {
        return (COSDictionary) copy(source, new IdentityHashMap<>());
    }

    private static COSBase copy(COSBase value, Map<COSBase, COSBase> seen) throws IOException {
        COSBase base = value instanceof COSObject object ? object.getObject() : value;
        if (base == null) return COSNull.NULL;
        COSBase known = seen.get(base);
        if (known != null) return known;
        if (base instanceof COSStream stream) {
            COSStream copy = new COSStream();
            seen.put(base, copy);
            for (Map.Entry<COSName, COSBase> entry : stream.entrySet())
                if (!COSName.LENGTH.equals(entry.getKey())) copy.setItem(entry.getKey(), copy(entry.getValue(), seen));
            try (InputStream input = stream.createRawInputStream(); OutputStream output = copy.createRawOutputStream()) {
                input.transferTo(output);
            }
            return copy;
        }
        if (base instanceof COSDictionary dictionary) {
            COSDictionary copy = new COSDictionary();
            seen.put(base, copy);
            for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
                if (!SKIPPED.contains(entry.getKey())) copy.setItem(entry.getKey(), copy(entry.getValue(), seen));
            return copy;
        }
        if (base instanceof COSArray array) {
            COSArray copy = new COSArray();
            seen.put(base, copy);
            for (int i = 0; i < array.size(); i++) copy.add(copy(array.get(i), seen));
            return copy;
        }
        return base;
    }

    static PDType1Font font(PdfTextStyle style) {
        Standard14Fonts.FontName name = switch (style.family()) {
            case PdfTextStyle.TIMES -> style.bold() && style.italic() ? Standard14Fonts.FontName.TIMES_BOLD_ITALIC
                    : style.bold() ? Standard14Fonts.FontName.TIMES_BOLD : style.italic() ? Standard14Fonts.FontName.TIMES_ITALIC
                    : Standard14Fonts.FontName.TIMES_ROMAN;
            case PdfTextStyle.COURIER -> style.bold() && style.italic() ? Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE
                    : style.bold() ? Standard14Fonts.FontName.COURIER_BOLD : style.italic() ? Standard14Fonts.FontName.COURIER_OBLIQUE
                    : Standard14Fonts.FontName.COURIER;
            default -> style.bold() && style.italic() ? Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE
                    : style.bold() ? Standard14Fonts.FontName.HELVETICA_BOLD : style.italic() ? Standard14Fonts.FontName.HELVETICA_OBLIQUE
                    : Standard14Fonts.FontName.HELVETICA;
        };
        return new PDType1Font(name);
    }

    static PdfTextStyle styleFor(String fontName, float size, Color color) {
        String name = fontName == null ? "" : fontName.toLowerCase(java.util.Locale.ROOT);
        String family = name.contains("times") || name.contains("serif") && !name.contains("sans") ? PdfTextStyle.TIMES
                : name.contains("courier") || name.contains("mono") ? PdfTextStyle.COURIER : PdfTextStyle.HELVETICA;
        return new PdfTextStyle(family, Math.max(2, Math.min(400, size)), color, name.contains("bold"),
                name.contains("italic") || name.contains("oblique"));
    }

    static String safe(PDType1Font font, String text) {
        StringBuilder builder = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String value = new String(Character.toChars(codePoint));
            try {
                font.encode(value);
                builder.append(value);
            } catch (IOException | IllegalArgumentException error) {
                builder.append('?');
            }
        });
        return builder.toString();
    }

    static List<String> lines(PDType1Font font, float size, String text, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        for (String paragraph : (text == null ? "" : text).split("\n", -1)) {
            if (maxWidth <= 0) { result.add(paragraph); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ", -1)) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width(font, size, candidate) <= maxWidth || line.isEmpty() && width(font, size, word) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) result.add(line.toString());
                line.setLength(0);
                String rest = word;
                while (width(font, size, rest) > maxWidth && rest.length() > 1) {
                    int cut = rest.length() - 1;
                    while (cut > 1 && width(font, size, rest.substring(0, cut)) > maxWidth) cut--;
                    result.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                line.append(rest);
            }
            result.add(line.toString());
        }
        return result;
    }

    private static float width(PDType1Font font, float size, String text) throws IOException {
        return font.getStringWidth(safe(font, text)) / 1000 * size;
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

    static boolean boolValue(COSBase value) { return value instanceof COSBoolean flag && flag.getValue(); }
}
