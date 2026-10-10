package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class ImageElementFactory extends BasePdfElementFactory {
    public ImageElementFactory() {
        this("pdf.factory.image", "pdf.image", "Imagem", "image", "Clique ou arraste na página para inserir uma imagem");
    }
    protected ImageElementFactory(String id, String commandId, String title, String icon, String tip) {
        super(id, commandId, title, icon, tip, PdfPlacementMode.DRAG_RECT);
    }

    @Override
    public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        Optional<Path> chosen = editor.getFiles().chooseImage(editor);
        if (chosen.isEmpty()) return;
        BufferedImage image = ImageIO.read(chosen.get().toFile());
        if (image == null) throw new IOException("Formato de imagem não suportado");
        editor.addImageStamp(placement.page(), image, fit(editor, placement, image));
    }

    protected Rectangle2D.Float fit(PdfEditor editor, PdfPlacement placement, BufferedImage image) {
        double ratio = image.getHeight() / (double) Math.max(1, image.getWidth());
        Rectangle2D.Float area = placement.bounds();
        if (placement.dragged()) {
            float width = area.width, height = (float) (width * ratio);
            if (height > area.height) { height = area.height; width = (float) (height / ratio); }
            return new Rectangle2D.Float(area.x + (area.width - width) / 2, area.y + (area.height - height) / 2, width, height);
        }
        float width = (float) Math.min(image.getWidth() * .75, editor.getPageWidth(placement.page()) * .45);
        float height = (float) (width * ratio);
        return new Rectangle2D.Float(placement.point().x, placement.point().y - height, width, height);
    }
}
