package dtm.stools.component.panels.editor.pdf.backend;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.PDLineDashPattern;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

final class PdfBoxContentScanner extends PDFGraphicsStreamEngine {
    private static final Set<String> TEXT_OPERATORS = Set.of("Tj", "TJ", "'", "\"");

    final List<PdfBoxGlyph> glyphs = new ArrayList<>();
    final List<PdfBoxItem> items = new ArrayList<>();
    private int depth, nesting, op = -1, element;
    private String operatorName = "";
    private List<COSBase> operands = List.of();
    private List<byte[]> codes;
    private int codeIndex;
    private Point2D current;
    private int pathStart = -1;
    private Rectangle2D pathBounds;
    private boolean pathClip;
    private Rectangle2D nested;

    PdfBoxContentScanner(PDPage page) { super(page); }

    void scan() throws IOException { processPage(getPage()); }

    int operatorCount() { return op + 1; }

    @Override protected void processOperator(Operator operator, List<COSBase> arguments) throws IOException {
        boolean top = depth == 0 && nesting == 0;
        if (top) {
            op++;
            operatorName = operator.getName();
            operands = arguments;
            element = -1;
        }
        nesting++;
        try { super.processOperator(operator, arguments); }
        finally { nesting--; }
    }

    @Override protected void showText(byte[] string) throws IOException {
        if (depth == 0 && TEXT_OPERATORS.contains(operatorName)) {
            element = nextStringElement();
            codes = split(getGraphicsState().getTextState().getFont(), string);
            codeIndex = 0;
        }
        try { super.showText(string); }
        finally { if (depth == 0) codes = null; }
    }

    private int nextStringElement() {
        if (!"TJ".equals(operatorName) || operands.isEmpty() || !(operands.getFirst() instanceof COSArray array)) return 0;
        for (int index = element + 1; index < array.size(); index++) if (array.getObject(index) instanceof COSString) return index;
        return element + 1;
    }

    private static List<byte[]> split(PDFont font, byte[] string) throws IOException {
        List<byte[]> parts = new ArrayList<>();
        if (font == null) {
            for (byte value : string) parts.add(new byte[]{value});
            return parts;
        }
        ByteArrayInputStream input = new ByteArrayInputStream(string);
        int offset = 0;
        while (input.available() > 0) {
            int before = input.available();
            font.readCode(input);
            int length = Math.max(1, before - input.available());
            parts.add(Arrays.copyOfRange(string, offset, Math.min(string.length, offset + length)));
            offset += length;
        }
        return parts;
    }

    @Override protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        AffineTransform matrix = textRenderingMatrix.createAffineTransform();
        double width = Math.max(.01, displacement.getX());
        Rectangle2D box = transformBox(matrix, 0, -.22, width, 1);
        if (depth > 0) note(box);
        else if (codes != null && codeIndex < codes.size()) {
            PDGraphicsState state = getGraphicsState();
            PDTextState text = state.getTextState();
            double size = text.getFontSize(), scaling = text.getHorizontalScaling() / 100.0;
            byte[] bytes = codes.get(codeIndex++);
            double wordSpacing = bytes.length == 1 && code == 32 ? text.getWordSpacing() : 0;
            double advance = (displacement.getX() * size + text.getCharacterSpacing() + wordSpacing) * scaling;
            Point2D center = matrix.transform(new Point2D.Double(width / 2, .3), null);
            Point2D origin = matrix.transform(new Point2D.Double(0, 0), null);
            Point2D end = matrix.transform(new Point2D.Double(displacement.getX(), 0), null);
            String unicode = font.toUnicode(code);
            boolean editable = !font.isVertical() && size * scaling != 0;
            glyphs.add(new PdfBoxGlyph(op, Math.max(0, element), bytes, unicode, box, center, origin, end, advance, size, scaling,
                    matrix, font, rgb(state.getNonStrokingColor()), rgb(state.getStrokingColor()),
                    text.getRenderingMode(), editable));
        }
        super.showGlyph(textRenderingMatrix, font, code, displacement);
    }

    @Override protected void showType3Glyph(Matrix textRenderingMatrix, PDType3Font font, int code, Vector displacement) throws IOException {
        depth++;
        try { super.showType3Glyph(textRenderingMatrix, font, code, displacement); }
        finally { depth--; }
    }

    @Override public void showForm(PDFormXObject form) throws IOException {
        if (depth > 0) {
            depth++;
            try { super.showForm(form); } finally { depth--; }
            return;
        }
        int index = op;
        AffineTransform ctm = ctm();
        nested = null;
        depth++;
        try { super.showForm(form); } finally { depth--; }
        Rectangle2D bounds = nested;
        PDRectangle box = form.getBBox();
        if (bounds == null && box != null) {
            AffineTransform transform = new AffineTransform(ctm);
            Matrix matrix = form.getMatrix();
            if (matrix != null) transform.concatenate(matrix.createAffineTransform());
            bounds = transformBox(transform, box.getLowerLeftX(), box.getLowerLeftY(), box.getUpperRightX(), box.getUpperRightY());
        }
        nested = null;
        if (bounds != null) items.add(item(PdfBoxItemKind.FORM, index, index, bounds, ctm, false, doName(), null));
    }

    @Override public void showTransparencyGroup(PDTransparencyGroup form) throws IOException { showForm(form); }

    @Override public void drawImage(PDImage image) throws IOException {
        AffineTransform ctm = ctm();
        Rectangle2D bounds = transformBox(ctm, 0, 0, 1, 1);
        if (depth > 0) { note(bounds); return; }
        boolean inline = "BI".equals(operatorName);
        items.add(item(inline ? PdfBoxItemKind.INLINE_IMAGE : PdfBoxItemKind.IMAGE, op, op, bounds, ctm, false,
                inline ? null : doName(), image));
    }

    private COSName doName() {
        return !operands.isEmpty() && operands.getFirst() instanceof COSName name ? name : null;
    }

    @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        startPath();
        include(p0); include(p1); include(p2); include(p3);
        current = p0;
    }
    @Override public void clip(int windingRule) { if (depth == 0) pathClip = true; }
    @Override public void moveTo(float x, float y) { startPath(); include(new Point2D.Float(x, y)); current = new Point2D.Float(x, y); }
    @Override public void lineTo(float x, float y) { startPath(); include(new Point2D.Float(x, y)); current = new Point2D.Float(x, y); }
    @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        startPath();
        include(new Point2D.Float(x1, y1)); include(new Point2D.Float(x2, y2)); include(new Point2D.Float(x3, y3));
        current = new Point2D.Float(x3, y3);
    }
    @Override public Point2D getCurrentPoint() { return current; }
    @Override public void closePath() {}
    @Override public void endPath() { finishPath(false, false); }
    @Override public void strokePath() { finishPath(true, false); }
    @Override public void fillPath(int windingRule) { finishPath(false, true); }
    @Override public void fillAndStrokePath(int windingRule) { finishPath(true, true); }
    @Override public void shadingFill(COSName shadingName) {
        Rectangle2D bounds = getGraphicsState().getCurrentClippingPath().getBounds2D();
        if (depth > 0) { note(bounds); return; }
        items.add(item(PdfBoxItemKind.SHADING, op, op, bounds, ctm(), false, shadingName, null));
    }

    private void startPath() {
        if (depth == 0 && pathStart < 0) pathStart = op;
    }
    private void include(Point2D point) {
        Rectangle2D dot = new Rectangle2D.Double(point.getX(), point.getY(), 0, 0);
        if (pathBounds == null) pathBounds = dot;
        else pathBounds.add(point);
    }
    private void finishPath(boolean stroke, boolean fill) {
        Rectangle2D bounds = pathBounds;
        int start = pathStart;
        boolean clipped = pathClip;
        pathBounds = null; pathStart = -1; pathClip = false;
        if (bounds == null) return;
        if (stroke) {
            AffineTransform ctm = ctm();
            double scale = Math.sqrt(Math.abs(ctm.getDeterminant()));
            double half = Math.max(.5, getGraphicsState().getLineWidth() * scale / 2);
            bounds = new Rectangle2D.Double(bounds.getX() - half, bounds.getY() - half,
                    bounds.getWidth() + 2 * half, bounds.getHeight() + 2 * half);
        }
        if (depth > 0) { note(bounds); return; }
        if (!stroke && !fill) return;
        items.add(item(PdfBoxItemKind.PATH, start < 0 ? op : start, op, bounds, ctm(), clipped, null, null));
    }

    private PdfBoxItem item(PdfBoxItemKind kind, int first, int last, Rectangle2D bounds, AffineTransform ctm,
                            boolean clip, COSName name, PDImage image) {
        PDGraphicsState state = getGraphicsState();
        PDLineDashPattern dash = state.getLineDashPattern();
        return new PdfBoxItem(kind, first, last, bounds, ctm, clip, name, image,
                rgb(state.getNonStrokingColor()), rgb(state.getStrokingColor()), state.getLineWidth(),
                state.getLineCap(), state.getLineJoin(), dash == null ? new float[0] : dash.getDashArray(),
                dash == null ? 0 : dash.getPhase());
    }

    private void note(Rectangle2D bounds) {
        if (bounds == null) return;
        if (nested == null) nested = (Rectangle2D) bounds.clone();
        else nested.add(bounds);
    }

    private AffineTransform ctm() { return getGraphicsState().getCurrentTransformationMatrix().createAffineTransform(); }

    static Rectangle2D transformBox(AffineTransform transform, double x1, double y1, double x2, double y2) {
        double[] points = {x1, y1, x2, y1, x2, y2, x1, y2};
        transform.transform(points, 0, points, 0, 4);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < points.length; i += 2) {
            minX = Math.min(minX, points[i]); maxX = Math.max(maxX, points[i]);
            minY = Math.min(minY, points[i + 1]); maxY = Math.max(maxY, points[i + 1]);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    private static float[] rgb(PDColor color) {
        if (color == null) return null;
        try {
            int value = color.toRGB();
            return new float[]{((value >> 16) & 255) / 255f, ((value >> 8) & 255) / 255f, (value & 255) / 255f};
        } catch (IOException | RuntimeException error) {
            return null;
        }
    }
}
