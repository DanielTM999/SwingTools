package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfChange;
import dtm.stools.component.panels.editor.pdf.api.PdfEdit;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.api.PdfTarget;
import dtm.stools.component.panels.editor.pdf.ui.PdfPageGeometry;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PdfSelectionOperations {
    private final PdfEditor editor;
    private final PdfClipboard clipboard = new PdfClipboard();
    private int pastePage = -1;
    private Point2D.Float pastePoint;
    private boolean reconstructionAccepted;

    public PdfSelectionOperations(PdfEditor editor) {
        this.editor = editor;
    }

    private boolean run(String label, PdfChange change, PdfEdit operation) {
        return editor.perform(() -> editor.edit(label, change, operation));
    }

    public void selectAll() {
        int page = editor.getCurrentPage();
        List<PdfPageElement> all = editor.getPageElements(page);
        if (all.isEmpty()) return;
        editor.setSelection(PdfSelection.of(page, all));
    }

    public void refreshSelection(int page) {
        if (editor.getSelection().isEmpty() || editor.getSelection().page() != page) return;
        List<PdfPageElement> fresh = editor.getPageElements(page);
        List<PdfPageElement> kept = new ArrayList<>();
        for (PdfPageElement element : editor.getSelection().elements())
            fresh.stream().filter(candidate -> candidate.id().equals(element.id()) && candidate.type().equals(element.type()))
                    .findFirst().ifPresent(kept::add);
        editor.setSelection(kept.isEmpty() && !editor.getSelection().hasArea() ? PdfSelection.empty() : new PdfSelection(page, kept, editor.getSelection().area()));
    }

    void reselect(int page, List<Rectangle2D> expected, List<String> annotationIds, Rectangle2D.Float area) {
        List<PdfPageElement> fresh = editor.getPageElements(page);
        List<PdfPageElement> found = new ArrayList<>();
        for (PdfPageElement candidate : fresh) {
            if (candidate.annotation()) {
                if (annotationIds.contains(candidate.id())) found.add(candidate);
                continue;
            }
            Rectangle2D.Float bounds = candidate.bounds();
            for (Rectangle2D target : expected) {
                if (Math.abs(bounds.getCenterX() - target.getCenterX()) <= 2 + target.getWidth() * .1
                        && Math.abs(bounds.getCenterY() - target.getCenterY()) <= 2 + target.getHeight() * .1) {
                    found.add(candidate);
                    break;
                }
            }
        }
        editor.setSelection(area != null ? PdfSelection.area(page, found, area) : found.isEmpty() ? PdfSelection.empty() : PdfSelection.of(page, found));
    }

    public void deleteSelection() {
        if (editor.getSelection().isEmpty() || editor.isReadOnly()) return;
        int page = editor.getSelection().page();
        PdfTarget target = editor.getSelection().target();
        List<PdfPageElement> fixed = editor.getSelection().elements().stream().filter(element -> !element.direct()).toList();
        boolean reconstruct = !fixed.isEmpty() && confirmReconstruction();
        run("Apagar seleção", PdfChange.forPages(page), d -> {
            d.deleteTarget(page, target);
            if (reconstruct) for (PdfPageElement element : fixed) d.eraseArea(page, element.bounds());
        });
        editor.setSelection(PdfSelection.empty());
    }

    public void eraseRegion(int page, Rectangle2D.Float area) {
        if (editor.isReadOnly()) return;
        run("Borracha", PdfChange.forPages(page), d -> d.eraseRegion(page, area));
    }

    private boolean confirmReconstruction() {
        if (!editor.getConfig().pageReconstructionEnabled()) {
            editor.showMessage("Alguns itens só podem ser removidos reconstruindo a página, recurso desativado");
            return false;
        }
        if (reconstructionAccepted) return true;
        int choice = editor.getDialogs().choose(editor, "Reconstruir página",
                "Alguns itens selecionados não podem ser alterados diretamente. A página pode ser reconstruída como imagem, "
                        + "perdendo texto pesquisável e vetores. Continuar?",
                "Reconstruir", "Reconstruir e não perguntar de novo", "Manter itens");
        if (choice == 1) reconstructionAccepted = true;
        return choice == 0 || choice == 1;
    }

    public void transformSelection(AffineTransform transform, String label) {
        if (editor.getSelection().isEmpty() || editor.isReadOnly() || transform == null || transform.isIdentity()) return;
        int page = editor.getSelection().page();
        PdfTarget target = editor.getSelection().target();
        List<Rectangle2D> expected = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        for (PdfPageElement element : editor.getSelection().elements()) {
            if (element.annotation()) annotations.add(element.id());
            else expected.add(transform.createTransformedShape(element.bounds()).getBounds2D());
        }
        Rectangle2D.Float area = null;
        if (editor.getSelection().hasArea()) {
            Rectangle2D moved = transform.createTransformedShape(editor.getSelection().area()).getBounds2D();
            area = new Rectangle2D.Float((float) moved.getX(), (float) moved.getY(), (float) moved.getWidth(), (float) moved.getHeight());
        }
        if (editor.getSelection().elements().stream().anyMatch(element -> !element.direct()))
            editor.showMessage("Alguns itens não podem ser transformados sem reconstruir a página e foram mantidos");
        if (run(label, PdfChange.forPages(page), d -> d.transformTarget(page, target, transform)))
            reselect(page, expected, annotations, area);
    }

    public void moveSelection(float dx, float dy) { transformSelection(AffineTransform.getTranslateInstance(dx, dy), "Mover"); }

    public void nudgeSelection(int dx, int dy) {
        if (editor.getSelection().isEmpty()) return;
        PdfPageGeometry geometry = editor.getPageGeometry(editor.getSelection().page(), 1);
        Point2D.Float a = geometry.toPdf(0, 0), b = geometry.toPdf(dx, dy);
        moveSelection(b.x - a.x, b.y - a.y);
    }

    public void rotateSelection(double clockwiseDegrees) {
        if (editor.getSelection().isEmpty()) return;
        Rectangle2D.Float bounds = editor.getSelection().bounds();
        transformSelection(AffineTransform.getRotateInstance(Math.toRadians(-clockwiseDegrees), bounds.getCenterX(), bounds.getCenterY()), "Girar");
    }

    public void duplicateSelection() {
        if (editor.getSelection().isEmpty() || editor.isReadOnly()) return;
        int page = editor.getSelection().page();
        PdfTarget target = editor.getSelection().target();
        AffineTransform offset = AffineTransform.getTranslateInstance(12, -12);
        List<Rectangle2D> expected = new ArrayList<>();
        for (PdfPageElement element : editor.getSelection().elements()) expected.add(offset.createTransformedShape(element.bounds()).getBounds2D());
        int annotationsBefore = (int) editor.getPageElements(page).stream().filter(PdfPageElement::annotation).count();
        long copies = editor.getSelection().elements().stream().filter(PdfPageElement::annotation).count();
        if (!run("Duplicar", PdfChange.forPages(page), d -> d.duplicateTarget(page, target, offset))) return;
        List<PdfPageElement> fresh = editor.getPageElements(page);
        List<String> created = new ArrayList<>();
        List<PdfPageElement> annotations = fresh.stream().filter(PdfPageElement::annotation).toList();
        for (int index = annotationsBefore; index < annotations.size() && created.size() < copies; index++) created.add(annotations.get(index).id());
        reselect(page, expected, created, null);
    }

    public void arrangeSelection(boolean forward) {
        List<PdfPageElement> annotations = editor.getSelection().elements().stream().filter(PdfPageElement::annotation).toList();
        if (annotations.size() != 1 || editor.isReadOnly()) {
            if (!editor.getSelection().isEmpty()) editor.showMessage("A ordem pode ser alterada em um objeto anotado por vez");
            return;
        }
        int page = editor.getSelection().page();
        String id = annotations.getFirst().id();
        if (run(forward ? "Trazer à frente" : "Enviar para trás", PdfChange.forPages(page), d -> d.movePageElementLayer(page, id, forward))) {
            int index = Integer.parseInt(id.substring("annotation:".length())) + (forward ? 1 : -1);
            editor.getPageElements(page).stream().filter(element -> element.id().equals("annotation:" + index)).findFirst()
                    .ifPresent(element -> editor.setSelection(PdfSelection.of(page, List.of(element))));
        }
    }

    public void alignSelection(String mode) {
        if (editor.getSelection().isEmpty() || editor.isReadOnly()) return;
        int page = editor.getSelection().page();
        List<PdfPageElement> items = editor.getSelection().elements();
        Rectangle2D reference = items.size() > 1 ? editor.getSelection().bounds() : new Rectangle2D.Float(editor.getDocument().pageX(page), editor.getDocument().pageY(page),
                editor.getDocument().pageWidth(page), editor.getDocument().pageHeight(page));
        Map<String, AffineTransform> moves = new LinkedHashMap<>();
        List<Rectangle2D> expected = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        List<PdfPageElement> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparingInt((PdfPageElement element) -> element.textual() ? 1 : 0)
                .thenComparing(Comparator.comparingInt(PdfSelectionOperations::elementIndex).reversed()));
        for (PdfPageElement element : ordered) {
            Rectangle2D.Float bounds = element.bounds();
            double dx = switch (mode) {
                case "left" -> reference.getMinX() - bounds.getMinX();
                case "center" -> reference.getCenterX() - bounds.getCenterX();
                case "right" -> reference.getMaxX() - bounds.getMaxX();
                default -> 0;
            };
            double dy = switch (mode) {
                case "top" -> reference.getMaxY() - bounds.getMaxY();
                case "middle" -> reference.getCenterY() - bounds.getCenterY();
                case "bottom" -> reference.getMinY() - bounds.getMinY();
                default -> 0;
            };
            AffineTransform move = AffineTransform.getTranslateInstance(dx, dy);
            moves.put(element.id(), move);
            if (element.annotation()) annotations.add(element.id());
            else expected.add(move.createTransformedShape(bounds).getBounds2D());
        }
        if (run("Alinhar", PdfChange.forPages(page), d -> {
            for (Map.Entry<String, AffineTransform> entry : moves.entrySet())
                if (!entry.getValue().isIdentity()) d.transformTarget(page, PdfTarget.of(entry.getKey()), entry.getValue());
        })) reselect(page, expected, annotations, null);
    }

    private static int elementIndex(PdfPageElement element) {
        int colon = element.id().indexOf(':');
        try { return Integer.parseInt(element.id().substring(colon + 1)); }
        catch (NumberFormatException error) { return 0; }
    }

    public void setPastePoint(int page, Point2D.Float point) {
        pastePage = page;
        pastePoint = point == null ? null : (Point2D.Float) point.clone();
    }

    public void copySelection() {
        if (editor.getSelection().isEmpty()) return;
        int page = editor.getSelection().page();
        try {
            List<String> annotations = editor.getSelection().elements().stream().filter(PdfPageElement::annotation).map(PdfPageElement::id).toList();
            byte[] exported = annotations.isEmpty() ? null : editor.getDocument().exportAnnotations(page, annotations);
            Rectangle2D.Float contentBounds = null;
            for (PdfPageElement element : editor.getSelection().elements()) {
                if (element.annotation()) continue;
                if (contentBounds == null) contentBounds = element.bounds();
                else Rectangle2D.union(contentBounds, element.bounds(), contentBounds);
            }
            if (editor.getSelection().hasArea()) contentBounds = editor.getSelection().area();
            BufferedImage image = null;
            if (contentBounds != null && contentBounds.width > 1 && contentBounds.height > 1 && editor.getDocument().canExtractContent())
                image = editor.getDocument().copyArea(page, clampToPage(page, contentBounds), 200);
            clipboard.store(exported, image, contentBounds, editor.getSelection().text());
            pastePage = -1;
            pastePoint = null;
            editor.showMessage(editor.getSelection().text().isBlank() ? "Copiado" : "Texto copiado");
        } catch (IOException | RuntimeException error) { editor.reportError(error); }
    }

    private Rectangle2D.Float clampToPage(int page, Rectangle2D.Float area) {
        Rectangle2D.Float pageBox = new Rectangle2D.Float(editor.getDocument().pageX(page), editor.getDocument().pageY(page), editor.getDocument().pageWidth(page), editor.getDocument().pageHeight(page));
        Rectangle2D.Float result = new Rectangle2D.Float();
        Rectangle2D.intersect(area, pageBox, result);
        return result;
    }

    public void cutSelection() {
        copySelection();
        deleteSelection();
    }

    public void pasteClipboard() {
        if (editor.isReadOnly()) return;
        int page = pastePage >= 0 && pastePage < editor.getDocument().pageCount() ? pastePage : editor.getCurrentPage();
        Point2D.Float at = pastePoint;
        pastePage = -1;
        pastePoint = null;
        if (!clipboard.isEmpty()) {
            Rectangle2D.Float bounds = clipboard.bounds();
            float offset = at == null ? clipboard.nextOffset() : 0;
            float dx = offset, dy = -offset;
            if (at != null && bounds != null) { dx = at.x - bounds.x; dy = at.y - (bounds.y + bounds.height); }
            float moveX = dx, moveY = dy;
            byte[] annotations = clipboard.annotations();
            BufferedImage image = clipboard.image();
            Rectangle2D.Float placed = bounds == null ? null : new Rectangle2D.Float(bounds.x + moveX, bounds.y + moveY, bounds.width, bounds.height);
            run("Colar", PdfChange.forPages(page), d -> {
                if (annotations != null) d.importAnnotations(page, annotations, moveX, moveY);
                if (image != null && placed != null) d.addImageStamp(page, image, placed);
            });
            return;
        }
        BufferedImage image = PdfClipboard.systemImage();
        if (image != null) {
            float width = (float) Math.min(image.getWidth() * .75, editor.getDocument().pageWidth(page) * .5);
            float height = width * image.getHeight() / Math.max(1, image.getWidth());
            Point2D.Float point = at != null ? at : new Point2D.Float(editor.getDocument().pageX(page) + 72, editor.getDocument().pageY(page) + editor.getDocument().pageHeight(page) - 72);
            Rectangle2D.Float bounds = new Rectangle2D.Float(point.x, point.y - height, width, height);
            run("Colar imagem", PdfChange.forPages(page), d -> d.addImageStamp(page, image, bounds));
            return;
        }
        String text = PdfClipboard.systemText();
        if (text != null && !text.isBlank()) {
            Point2D.Float point = at != null ? at : new Point2D.Float(editor.getDocument().pageX(page) + 72, editor.getDocument().pageY(page) + editor.getDocument().pageHeight(page) - 72);
            Rectangle2D.Float bounds = new Rectangle2D.Float(point.x, point.y, 0, 0);
            run("Colar texto", PdfChange.forPages(page), d -> d.addTextBox(page, bounds, text, editor.getTextStyle()));
        }
    }

    public boolean hasClipboard() { return !clipboard.isEmpty(); }
}
