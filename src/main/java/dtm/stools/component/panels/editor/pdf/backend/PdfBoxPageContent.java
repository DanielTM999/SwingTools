package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfTarget;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class PdfBoxPageContent {
    private static final double TOLERANCE = .75;

    private final PDDocument document;
    private final PDPage page;
    private final List<Object> tokens;
    private final int[] positions;
    private final List<PdfBoxGlyph> glyphs;
    private final List<PdfBoxItem> items;
    private final List<List<PdfBoxGlyph>> words;
    private final List<PdfBoxItem> images = new ArrayList<>(), forms = new ArrayList<>(), paths = new ArrayList<>();
    private final Map<Integer, List<Object>> replacements = new HashMap<>();
    private final Map<Integer, List<Object>> before = new HashMap<>(), after = new HashMap<>();
    private final Set<PdfBoxGlyph> removed = new LinkedHashSet<>();
    private final List<Object> appended = new ArrayList<>();
    private boolean changed;

    PdfBoxPageContent(PDDocument document, PDPage page) throws IOException {
        this.document = document;
        this.page = page;
        tokens = page.hasContents() ? new ArrayList<>(new PDFStreamParser(page).parse()) : new ArrayList<>();
        List<Integer> found = new ArrayList<>();
        for (int index = 0; index < tokens.size(); index++) if (tokens.get(index) instanceof Operator) found.add(index);
        positions = found.stream().mapToInt(Integer::intValue).toArray();
        PdfBoxContentScanner scanner = new PdfBoxContentScanner(page);
        scanner.scan();
        if (scanner.operatorCount() != positions.length)
            throw new IOException("O conteúdo da página não pôde ser analisado com segurança");
        glyphs = scanner.glyphs;
        items = scanner.items;
        for (PdfBoxItem item : items) {
            switch (item.kind) {
                case IMAGE, INLINE_IMAGE -> images.add(item);
                case FORM -> forms.add(item);
                case PATH -> { if (!item.clip) paths.add(item); }
                default -> { }
            }
        }
        words = groupWords(glyphs);
    }

    List<PdfPageElement> elements() {
        List<PdfPageElement> result = new ArrayList<>();
        PDRectangle crop = page.getCropBox();
        double pageArea = Math.max(1, crop.getWidth() * crop.getHeight());
        for (int index = 0; index < paths.size(); index++) {
            PdfBoxItem item = paths.get(index);
            if (item.bounds.getWidth() * item.bounds.getHeight() > pageArea * .5) continue;
            result.add(new PdfPageElement("path:" + index, "Path", rect(item.bounds), item.first, true, ""));
        }
        for (int index = 0; index < images.size(); index++) {
            PdfBoxItem item = images.get(index);
            result.add(new PdfPageElement("image:" + index, "Image", rect(item.bounds), item.first, true, ""));
        }
        for (int index = 0; index < forms.size(); index++) {
            PdfBoxItem item = forms.get(index);
            if (item.bounds.getWidth() * item.bounds.getHeight() > pageArea * .9) continue;
            result.add(new PdfPageElement("form:" + index, "Form", rect(item.bounds), item.first, true, ""));
        }
        for (int index = 0; index < words.size(); index++) {
            List<PdfBoxGlyph> word = words.get(index);
            Rectangle2D bounds = null;
            StringBuilder text = new StringBuilder();
            boolean editable = true;
            for (PdfBoxGlyph glyph : word) {
                if (bounds == null) bounds = (Rectangle2D) glyph.box.clone();
                else bounds.add(glyph.box);
                if (glyph.unicode != null) text.append(glyph.unicode);
                editable &= glyph.editable;
            }
            if (bounds != null) result.add(new PdfPageElement("text:" + index, "Text", rect(bounds), word.getFirst().op,
                    editable, text.toString()));
        }
        result.sort((a, b) -> Integer.compare(a.layer(), b.layer()));
        return result;
    }

    Set<PdfBoxGlyph> glyphs(PdfTarget target) {
        Set<PdfBoxGlyph> result = new LinkedHashSet<>();
        for (String id : target.ids()) {
            int index = index(id, "text:");
            if (index >= 0 && index < words.size()) result.addAll(words.get(index));
        }
        Rectangle2D.Float area = target.area();
        if (area != null) for (PdfBoxGlyph glyph : glyphs) if (area.contains(glyph.center)) result.add(glyph);
        result.removeIf(glyph -> !glyph.editable);
        return result;
    }

    Set<PdfBoxItem> items(PdfTarget target) {
        Set<PdfBoxItem> result = new LinkedHashSet<>();
        for (String id : target.ids()) {
            pick(result, images, index(id, "image:"));
            pick(result, forms, index(id, "form:"));
            pick(result, paths, index(id, "path:"));
        }
        Rectangle2D.Float area = target.area();
        if (area != null) for (PdfBoxItem item : items) if (!item.clip && inside(area, item.bounds)) result.add(item);
        return result;
    }

    List<PdfBoxItem> partialItems(Rectangle2D area) {
        List<PdfBoxItem> result = new ArrayList<>();
        for (PdfBoxItem item : items) if (!item.clip && item.bounds.intersects(area) && !inside(area, item.bounds)) result.add(item);
        return result;
    }

    boolean editable(PdfBoxItem item) {
        return item.kind != PdfBoxItemKind.PATH || !item.clip;
    }

    void remove(Collection<PdfBoxGlyph> selected, Collection<PdfBoxItem> selectedItems) {
        removed.addAll(selected);
        for (PdfBoxItem item : selectedItems) {
            if (item.kind == PdfBoxItemKind.PATH) replacements.put(item.last, List.of(operator("n")));
            else replacements.put(item.first, List.of());
        }
        changed |= !selected.isEmpty() || !selectedItems.isEmpty();
    }

    void transform(Collection<PdfBoxGlyph> selected, Collection<PdfBoxItem> selectedItems, AffineTransform transform) throws IOException {
        for (PdfBoxItem item : selectedItems) {
            AffineTransform local;
            try { local = item.ctm.createInverse(); }
            catch (NoninvertibleTransformException error) { throw new IOException("Elemento com transformação inválida", error); }
            local.concatenate(transform);
            local.concatenate(item.ctm);
            List<Object> prefix = new ArrayList<>(List.of(operator("q")));
            prefix.addAll(matrix(local));
            prefix.add(operator("cm"));
            wrap(item, prefix, List.of(operator("Q")));
        }
        removed.addAll(selected);
        append(selected, transform);
        changed |= !selected.isEmpty() || !selectedItems.isEmpty();
    }

    void duplicate(Collection<PdfBoxGlyph> selected, Collection<PdfBoxItem> selectedItems, AffineTransform transform) {
        append(selected, transform);
        for (PdfBoxItem item : selectedItems) {
            if (item.kind == PdfBoxItemKind.SHADING) continue;
            AffineTransform placed = new AffineTransform(transform);
            placed.concatenate(item.ctm);
            List<Object> copy = new ArrayList<>(List.of(operator("q")));
            copy.addAll(matrix(placed));
            copy.add(operator("cm"));
            if (item.kind == PdfBoxItemKind.PATH) {
                copy.addAll(graphicsState(item));
                int from = item.first == 0 ? 0 : positions[item.first - 1] + 1;
                copy.addAll(tokens.subList(from, positions[item.last] + 1));
            } else {
                int from = item.first == 0 ? 0 : positions[item.first - 1] + 1;
                copy.addAll(tokens.subList(from, positions[item.first] + 1));
            }
            copy.add(operator("Q"));
            appended.addAll(copy);
        }
        changed |= !selected.isEmpty() || !selectedItems.isEmpty();
    }

    void eraseShape(Shape area) throws IOException {
        Rectangle2D bounds = area.getBounds2D();
        Set<PdfBoxGlyph> glyphTargets = new LinkedHashSet<>();
        for (PdfBoxGlyph glyph : glyphs) if (glyph.editable && area.contains(glyph.center)) glyphTargets.add(glyph);
        Set<PdfBoxItem> inside = new LinkedHashSet<>();
        List<PdfBoxItem> partial = new ArrayList<>();
        for (PdfBoxItem item : items) {
            if (item.clip || !item.bounds.intersects(bounds) || !area.intersects(grown(item.bounds))) continue;
            if (area.contains(grown(item.bounds))) inside.add(item);
            else partial.add(item);
        }
        remove(glyphTargets, inside);
        PDRectangle crop = page.getCropBox();
        Rectangle2D outer = new Rectangle2D.Double(crop.getLowerLeftX() - 2000, crop.getLowerLeftY() - 2000,
                crop.getWidth() + 4000, crop.getHeight() + 4000);
        for (PdfBoxItem item : partial) {
            if (replacements.containsKey(item.first) || replacements.containsKey(item.last)) continue;
            if (item.kind == PdfBoxItemKind.IMAGE && erasePixels(item, area)) continue;
            AffineTransform inverse;
            try { inverse = item.ctm.createInverse(); }
            catch (NoninvertibleTransformException error) { continue; }
            List<Object> prefix = new ArrayList<>(List.of(operator("q")));
            prefix.addAll(path(outer, inverse));
            prefix.addAll(path(area, inverse));
            prefix.add(operator("W*"));
            prefix.add(operator("n"));
            wrap(item, prefix, List.of(operator("Q")));
            changed = true;
        }
    }

    private static Rectangle2D grown(Rectangle2D bounds) {
        return new Rectangle2D.Double(bounds.getX() - .01, bounds.getY() - .01, Math.max(.02, bounds.getWidth() + .02),
                Math.max(.02, bounds.getHeight() + .02));
    }

    void replaceWord(String id, String text, PDFont font) throws IOException {
        int index = index(id, "text:");
        if (index < 0 || index >= words.size()) throw new IOException("Texto não encontrado: " + id);
        List<PdfBoxGlyph> word = words.get(index);
        if (word.stream().anyMatch(glyph -> !glyph.editable)) throw new IOException("Este texto não pode ser editado diretamente");
        removed.addAll(word);
        changed = true;
        if (text == null || text.isEmpty()) return;
        PdfBoxGlyph first = word.getFirst();
        AffineTransform matrix = new AffineTransform(first.matrix);
        COSName name = resources().add(font);
        appended.add(operator("q"));
        appended.add(operator("BT"));
        appended.add(name);
        appended.add(COSInteger.ONE);
        appended.add(operator("Tf"));
        appended.addAll(color(first.fill, "rg"));
        appended.addAll(matrix(matrix));
        appended.add(operator("Tm"));
        appended.add(new COSString(font.encode(text)));
        appended.add(operator("Tj"));
        appended.add(operator("ET"));
        appended.add(operator("Q"));
    }

    boolean commit() throws IOException {
        if (!changed) return false;
        Map<Integer, List<PdfBoxGlyph>> byOperator = new LinkedHashMap<>();
        for (PdfBoxGlyph glyph : removed) byOperator.computeIfAbsent(glyph.op, ignored -> new ArrayList<>()).add(glyph);
        for (Map.Entry<Integer, List<PdfBoxGlyph>> entry : byOperator.entrySet()) {
            int op = entry.getKey();
            if (replacements.containsKey(op)) continue;
            replacements.put(op, rebuildText(op, Set.copyOf(entry.getValue())));
        }
        List<Object> output = new ArrayList<>();
        boolean wrap = !appended.isEmpty();
        if (wrap) output.add(operator("q"));
        int start = 0, balance = 0;
        for (int op = 0; op < positions.length; op++) {
            List<Object> prefix = before.get(op);
            if (prefix != null) output.addAll(prefix);
            List<Object> replacement = replacements.get(op);
            Operator original = (Operator) tokens.get(positions[op]);
            if (replacement != null) output.addAll(replacement);
            else output.addAll(tokens.subList(start, positions[op] + 1));
            if ("q".equals(original.getName())) balance++;
            if ("Q".equals(original.getName())) balance = Math.max(0, balance - 1);
            List<Object> suffix = after.get(op);
            if (suffix != null) output.addAll(suffix);
            start = positions[op] + 1;
        }
        if (wrap) {
            for (int i = 0; i < balance; i++) output.add(operator("Q"));
            output.add(operator("Q"));
            output.addAll(appended);
        }
        PDStream stream = new PDStream(document);
        try (OutputStream out = stream.createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(out).writeTokens(output);
        }
        page.setContents(stream);
        return true;
    }

    static byte[] serialize(List<Object> tokens) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new ContentStreamWriter(output).writeTokens(tokens);
        return output.toByteArray();
    }

    private List<Object> rebuildText(int op, Set<PdfBoxGlyph> drop) throws IOException {
        Operator operator = (Operator) tokens.get(positions[op]);
        List<Object> operands = new ArrayList<>(tokens.subList(op == 0 ? 0 : positions[op - 1] + 1, positions[op]));
        String name = operator.getName();
        List<PdfBoxGlyph> all = new ArrayList<>();
        for (PdfBoxGlyph glyph : glyphs) if (glyph.op == op) all.add(glyph);
        COSArray source;
        if ("TJ".equals(name)) {
            if (operands.isEmpty() || !(operands.getLast() instanceof COSArray array)) throw new IOException("Operador de texto inválido");
            source = array;
        } else {
            if (operands.isEmpty() || !(operands.getLast() instanceof COSString string)) throw new IOException("Operador de texto inválido");
            source = new COSArray();
            source.add(string);
        }
        COSArray result = new COSArray();
        float pending = 0;
        boolean hasPending = false;
        ByteArrayOutputStream kept = new ByteArrayOutputStream();
        for (int element = 0; element < source.size(); element++) {
            COSBase value = source.getObject(element);
            if (value instanceof COSNumber number) {
                flush(result, kept);
                pending += number.floatValue();
                hasPending = true;
                continue;
            }
            if (!(value instanceof COSString string)) continue;
            int index = element;
            List<PdfBoxGlyph> parts = all.stream().filter(glyph -> glyph.element == index).toList();
            ByteArrayOutputStream joined = new ByteArrayOutputStream();
            for (PdfBoxGlyph glyph : parts) joined.write(glyph.bytes);
            if (!Arrays.equals(joined.toByteArray(), string.getBytes()))
                throw new IOException("Texto com codificação não suportada para edição direta");
            for (PdfBoxGlyph glyph : parts) {
                if (drop.contains(glyph)) {
                    flush(result, kept);
                    pending += (float) glyph.kerning();
                    hasPending = true;
                } else {
                    if (hasPending) { result.add(new COSFloat(pending)); pending = 0; hasPending = false; }
                    kept.write(glyph.bytes);
                }
            }
        }
        flush(result, kept);
        if (hasPending && pending != 0) result.add(new COSFloat(pending));
        List<Object> replacement = new ArrayList<>();
        switch (name) {
            case "'" -> replacement.add(operator("T*"));
            case "\"" -> {
                replacement.add(operands.get(0));
                replacement.add(operator("Tw"));
                replacement.add(operands.get(1));
                replacement.add(operator("Tc"));
                replacement.add(operator("T*"));
            }
            default -> { }
        }
        replacement.add(result);
        replacement.add(operator("TJ"));
        return replacement;
    }

    private static void flush(COSArray result, ByteArrayOutputStream kept) {
        if (kept.size() == 0) return;
        result.add(new COSString(kept.toByteArray()));
        kept.reset();
    }

    private void append(Collection<PdfBoxGlyph> selected, AffineTransform transform) {
        if (selected.isEmpty()) return;
        appended.add(operator("q"));
        appended.add(operator("BT"));
        PDFont font = null;
        String fill = null, stroke = null;
        RenderingMode mode = RenderingMode.FILL;
        for (PdfBoxGlyph glyph : selected) {
            if (glyph.font != font) {
                font = glyph.font;
                appended.add(resources().add(font));
                appended.add(COSInteger.ONE);
                appended.add(operator("Tf"));
            }
            String fillKey = Arrays.toString(glyph.fill), strokeKey = Arrays.toString(glyph.stroke);
            if (!fillKey.equals(fill)) { appended.addAll(color(glyph.fill, "rg")); fill = fillKey; }
            if (!strokeKey.equals(stroke)) { appended.addAll(color(glyph.stroke, "RG")); stroke = strokeKey; }
            if (glyph.renderingMode != null && glyph.renderingMode != mode) {
                mode = glyph.renderingMode;
                appended.add(COSInteger.get(mode.intValue()));
                appended.add(operator("Tr"));
            }
            AffineTransform placed = new AffineTransform(transform);
            placed.concatenate(glyph.matrix);
            appended.addAll(matrix(placed));
            appended.add(operator("Tm"));
            appended.add(new COSString(glyph.bytes));
            appended.add(operator("Tj"));
        }
        appended.add(operator("ET"));
        appended.add(operator("Q"));
    }

    private List<Object> graphicsState(PdfBoxItem item) {
        List<Object> state = new ArrayList<>();
        state.add(new COSFloat(item.lineWidth));
        state.add(operator("w"));
        state.add(COSInteger.get(item.lineCap));
        state.add(operator("J"));
        state.add(COSInteger.get(item.lineJoin));
        state.add(operator("j"));
        COSArray dash = new COSArray();
        for (float value : item.dash) dash.add(new COSFloat(value));
        state.add(dash);
        state.add(COSInteger.get(item.dashPhase));
        state.add(operator("d"));
        state.addAll(color(item.fill, "rg"));
        state.addAll(color(item.stroke, "RG"));
        return state;
    }

    private boolean erasePixels(PdfBoxItem item, Shape area) throws IOException {
        if (item.name == null || !(item.image instanceof PDImageXObject picture) || picture.isStencil()) return false;
        AffineTransform inverse;
        try { inverse = item.ctm.createInverse(); }
        catch (NoninvertibleTransformException error) { return false; }
        BufferedImage source = picture.getImage();
        int width = source.getWidth(), height = source.getHeight();
        boolean alpha = source.getColorModel().hasAlpha();
        BufferedImage copy = new BufferedImage(width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
            AffineTransform pixels = new AffineTransform(width, 0, 0, -height, 0, height);
            pixels.concatenate(inverse);
            Shape shape = pixels.createTransformedShape(area);
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            if (alpha) graphics.setComposite(AlphaComposite.Clear);
            else graphics.setColor(Color.WHITE);
            graphics.fill(shape);
        } finally { graphics.dispose(); }
        PDImageXObject replacement = !alpha && "jpg".equals(picture.getSuffix())
                ? JPEGFactory.createFromImage(document, copy, .92f) : LosslessFactory.createFromImage(document, copy);
        COSName name = resources().add(replacement);
        replacements.put(item.first, List.of(name, operator("Do")));
        changed = true;
        return true;
    }

    private void wrap(PdfBoxItem item, List<Object> prefix, List<Object> suffix) {
        before.computeIfAbsent(item.first, ignored -> new ArrayList<>()).addAll(prefix);
        after.computeIfAbsent(item.last, ignored -> new ArrayList<>()).addAll(0, suffix);
    }

    private PDResources resources() {
        PDResources resources = page.getResources();
        if (resources == null) {
            resources = new PDResources();
            page.setResources(resources);
        }
        return resources;
    }

    static List<Object> path(Shape shape, AffineTransform transform) {
        List<Object> path = new ArrayList<>();
        double[] values = new double[6];
        double lastX = 0, lastY = 0;
        for (PathIterator iterator = shape.getPathIterator(transform); !iterator.isDone(); iterator.next()) {
            int segment = iterator.currentSegment(values);
            switch (segment) {
                case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> {
                    path.add(new COSFloat((float) values[0]));
                    path.add(new COSFloat((float) values[1]));
                    path.add(operator(segment == PathIterator.SEG_MOVETO ? "m" : "l"));
                    lastX = values[0]; lastY = values[1];
                }
                case PathIterator.SEG_QUADTO -> {
                    double c1x = lastX + 2.0 / 3 * (values[0] - lastX), c1y = lastY + 2.0 / 3 * (values[1] - lastY);
                    double c2x = values[2] + 2.0 / 3 * (values[0] - values[2]), c2y = values[3] + 2.0 / 3 * (values[1] - values[3]);
                    for (double value : new double[]{c1x, c1y, c2x, c2y, values[2], values[3]}) path.add(new COSFloat((float) value));
                    path.add(operator("c"));
                    lastX = values[2]; lastY = values[3];
                }
                case PathIterator.SEG_CUBICTO -> {
                    for (int i = 0; i < 6; i++) path.add(new COSFloat((float) values[i]));
                    path.add(operator("c"));
                    lastX = values[4]; lastY = values[5];
                }
                default -> path.add(operator("h"));
            }
        }
        return path;
    }

    private static List<Object> matrix(AffineTransform transform) {
        return List.of(new COSFloat((float) transform.getScaleX()), new COSFloat((float) transform.getShearY()),
                new COSFloat((float) transform.getShearX()), new COSFloat((float) transform.getScaleY()),
                new COSFloat((float) transform.getTranslateX()), new COSFloat((float) transform.getTranslateY()));
    }

    private static List<Object> color(float[] rgb, String operator) {
        float[] value = rgb == null ? new float[]{0, 0, 0} : rgb;
        return List.of(new COSFloat(value[0]), new COSFloat(value[1]), new COSFloat(value[2]), operator(operator));
    }

    static Operator operator(String name) { return Operator.getOperator(name); }

    private static boolean inside(Rectangle2D area, Rectangle2D bounds) {
        return bounds.getMinX() >= area.getMinX() - TOLERANCE && bounds.getMaxX() <= area.getMaxX() + TOLERANCE
                && bounds.getMinY() >= area.getMinY() - TOLERANCE && bounds.getMaxY() <= area.getMaxY() + TOLERANCE;
    }

    private static void pick(Set<PdfBoxItem> result, List<PdfBoxItem> source, int index) {
        if (index >= 0 && index < source.size()) result.add(source.get(index));
    }

    static int index(String id, String prefix) {
        if (id == null || !id.startsWith(prefix)) return -1;
        try { return Integer.parseInt(id.substring(prefix.length())); }
        catch (NumberFormatException error) { return -1; }
    }

    private static Rectangle2D.Float rect(Rectangle2D bounds) {
        return new Rectangle2D.Float((float) bounds.getX(), (float) bounds.getY(),
                (float) Math.max(.5, bounds.getWidth()), (float) Math.max(.5, bounds.getHeight()));
    }

    private static List<List<PdfBoxGlyph>> groupWords(List<PdfBoxGlyph> source) {
        List<List<PdfBoxGlyph>> result = new ArrayList<>();
        List<PdfBoxGlyph> current = new ArrayList<>();
        PdfBoxGlyph previous = null;
        for (PdfBoxGlyph glyph : source) {
            if (glyph.whitespace()) {
                if (!current.isEmpty()) result.add(current);
                current = new ArrayList<>();
                previous = null;
                continue;
            }
            if (previous != null && breaks(previous, glyph)) {
                result.add(current);
                current = new ArrayList<>();
            }
            current.add(glyph);
            previous = glyph;
        }
        if (!current.isEmpty()) result.add(current);
        return result;
    }

    private static boolean breaks(PdfBoxGlyph previous, PdfBoxGlyph next) {
        double dx = previous.end.getX() - previous.origin.getX(), dy = previous.end.getY() - previous.origin.getY();
        double length = Math.hypot(dx, dy);
        double ux = length < 1e-6 ? 1 : dx / length, uy = length < 1e-6 ? 0 : dy / length;
        double gx = next.origin.getX() - previous.end.getX(), gy = next.origin.getY() - previous.end.getY();
        double along = gx * ux + gy * uy, across = -gx * uy + gy * ux;
        double height = Math.max(previous.height(), next.height());
        return Math.abs(across) > height * .5 || along > height * .3 || along < -height * .6
                || !Objects.equals(previous.font, next.font) && along > height * .15;
    }

    static Point2D.Double point(double x, double y) { return new Point2D.Double(x, y); }
}
