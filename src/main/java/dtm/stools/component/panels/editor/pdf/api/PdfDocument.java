package dtm.stools.component.panels.editor.pdf.api;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface PdfDocument extends AutoCloseable {
    int pageCount();
    float pageWidth(int page);
    float pageHeight(int page);
    default float pageX(int page) { return 0; }
    default float pageY(int page) { return 0; }
    default int pageRotation(int page) { return 0; }
    BufferedImage render(int page, float dpi) throws IOException;
    String text(int page) throws IOException;
    void save(Path destination) throws IOException;
    byte[] snapshot() throws IOException;
    void rotate(int page, int degrees) throws IOException;
    void remove(int page) throws IOException;
    void move(int source, int destination) throws IOException;
    void insertPages(Path source, int destination) throws IOException;
    default void insertBlankPage(int destination, float width, float height) throws IOException {
        throw new IOException("Este backend não permite inserir página em branco");
    }
    void addText(int page, String text, float x, float y, float fontSize) throws IOException;
    void addNote(int page, String text, float x, float y) throws IOException;
    void addHighlight(int page, float x, float y, float width, float height) throws IOException;
    List<String> formFields();
    void setFormField(String name, String value) throws IOException;
    void addTextField(int page, String name, float x, float y, float width, float height) throws IOException;
    void addOcrLayer(int page, PdfOcrResult result, float dpi) throws IOException;
    boolean replaceText(int page, String original, String replacement, Rectangle2D.Float area,
                        boolean allowReconstruction) throws IOException;
    void addImage(int page, Path image, float x, float y, float width, float height) throws IOException;
    List<String> imageResources(int page) throws IOException;
    void replaceImageResource(int page, String resourceName, Path image) throws IOException;
    void addChoiceField(int page, String name, List<String> options,
                        float x, float y, float width, float height) throws IOException;
    void addCheckBox(int page, String name, float x, float y, float size) throws IOException;
    void addRadioGroup(int page, String name, List<String> options,
                       float x, float y, float size, float gap) throws IOException;
    void addSquare(int page, float x, float y, float width, float height) throws IOException;
    void addInk(int page, float[] points) throws IOException;
    default void addInk(int page, float[] points, PdfShapeStyle style) throws IOException { addInk(page, points); }
    default void addShape(int page, PdfShapeKind kind, Rectangle2D.Float bounds, PdfShapeStyle style) throws IOException {
        if (kind != PdfShapeKind.RECTANGLE) throw new IOException("Este backend não permite a forma " + kind);
        addSquare(page, bounds.x, bounds.y, bounds.width, bounds.height);
    }
    default void addLine(int page, float x1, float y1, float x2, float y2, boolean arrow, PdfShapeStyle style) throws IOException {
        addShape(page, arrow ? PdfShapeKind.ARROW : PdfShapeKind.LINE, new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2),
                Math.abs(x2 - x1), Math.abs(y2 - y1)), style);
    }
    default void addTextBox(int page, Rectangle2D.Float bounds, String text, PdfTextStyle style) throws IOException {
        addText(page, text, bounds.x, bounds.y + bounds.height - style.size(), style.size());
    }
    default void updateTextBox(int page, String id, String text, PdfTextStyle style) throws IOException {
        throw new IOException("Este backend não permite editar caixas de texto");
    }
    default Optional<PdfTextStyle> textBoxStyle(int page, String id) throws IOException { return Optional.empty(); }
    default Optional<String> textBoxText(int page, String id) throws IOException { return Optional.empty(); }
    default void addImageStamp(int page, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        pasteImage(page, image, bounds);
    }
    default void setAnnotationStyle(int page, String id, PdfShapeStyle style) throws IOException {
        throw new IOException("Este backend não permite alterar o estilo do elemento");
    }
    default void eraseArea(int page, Rectangle2D.Float area) throws IOException {
        throw new IOException("Este backend não permite apagar uma área");
    }
    default void eraseRegion(int page, Rectangle2D.Float area) throws IOException { eraseArea(page, area); }
    default void eraseShape(int page, Shape area) throws IOException {
        Rectangle2D bounds = area.getBounds2D();
        eraseRegion(page, new Rectangle2D.Float((float) bounds.getX(), (float) bounds.getY(), (float) bounds.getWidth(), (float) bounds.getHeight()));
    }
    default Optional<PdfFieldInfo> fieldInfo(int page, String id) throws IOException { return Optional.empty(); }
    default void renameField(String name, String newName) throws IOException {
        throw new IOException("Este backend não permite renomear campos");
    }
    default void setFieldOptions(String name, List<String> options) throws IOException {
        throw new IOException("Este backend não permite alterar as opções do campo");
    }
    default void setFieldFlags(String name, boolean required, boolean readOnly, boolean multiline) throws IOException {
        throw new IOException("Este backend não permite alterar as propriedades do campo");
    }
    default void setFieldFontSize(String name, float size) throws IOException {
        throw new IOException("Este backend não permite alterar a fonte do campo");
    }
    default void addRadioOption(int page, String name, String option) throws IOException {
        throw new IOException("Este backend não permite adicionar opções");
    }
    default void renameRadioOption(int page, String id, String option) throws IOException {
        throw new IOException("Este backend não permite renomear opções");
    }
    default List<PdfPageElement> pageElements(int page) throws IOException { return List.of(); }
    default void deleteTarget(int page, PdfTarget target) throws IOException {
        throw new IOException("Este backend não permite apagar a seleção");
    }
    default void transformTarget(int page, PdfTarget target, AffineTransform transform) throws IOException {
        throw new IOException("Este backend não permite transformar a seleção");
    }
    default void duplicateTarget(int page, PdfTarget target, AffineTransform transform) throws IOException {
        throw new IOException("Este backend não permite duplicar a seleção");
    }
    default void replaceElementText(int page, String id, String text) throws IOException {
        throw new IOException("Este backend não permite editar o texto do elemento");
    }
    default byte[] exportAnnotations(int page, List<String> ids) throws IOException {
        throw new IOException("Este backend não permite copiar anotações");
    }
    default void importAnnotations(int page, byte[] data, float dx, float dy) throws IOException {
        throw new IOException("Este backend não permite colar anotações");
    }
    default void deletePageElement(int page, String id) throws IOException { deleteTarget(page, PdfTarget.of(id)); }
    default void setPageElementBounds(int page, String id, Rectangle2D.Float bounds) throws IOException {
        PdfPageElement element = pageElements(page).stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new IOException("Elemento não encontrado: " + id));
        Rectangle2D.Float from = element.bounds();
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0 || from.width <= 0 || from.height <= 0)
            throw new IllegalArgumentException("Limites inválidos");
        AffineTransform transform = new AffineTransform();
        transform.translate(bounds.x, bounds.y);
        transform.scale(bounds.width / from.width, bounds.height / from.height);
        transform.translate(-from.x, -from.y);
        transformTarget(page, PdfTarget.of(id), transform);
    }
    default void movePageElementLayer(int page, String id, boolean forward) throws IOException {
        throw new IOException("Este backend não permite ordenar o elemento");
    }
    default void transformArea(int page, Rectangle2D.Float source, Rectangle2D.Float destination,
                               float clockwiseDegrees) throws IOException {
        throw new IOException("Este backend não permite transformar uma área");
    }
    default BufferedImage copyArea(int page, Rectangle2D.Float area, float dpi) throws IOException {
        throw new IOException("Este backend não permite copiar uma área");
    }
    default void pasteImage(int page, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        throw new IOException("Este backend não permite colar uma imagem");
    }
    void extractPages(int first, int last, Path destination) throws IOException;
    default void extractPages(List<Integer> pages, Path destination) throws IOException {
        if (pages.isEmpty()) throw new IllegalArgumentException("Nenhuma página selecionada");
        List<Integer> sorted = pages.stream().distinct().sorted().toList();
        if (sorted.getLast() - sorted.getFirst() + 1 != sorted.size())
            throw new IOException("Este backend só extrai páginas consecutivas");
        extractPages(sorted.getFirst(), sorted.getLast(), destination);
    }
    String title();
    void setTitle(String title);
    boolean hasSignatures();
    boolean canModify();
    boolean canExtractContent();
    void print() throws IOException;
    @Override
    void close() throws IOException;
}
