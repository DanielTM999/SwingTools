package dtm.stools.component.panels.editor.pdf.command;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public final class PdfClipboard {
    private byte[] annotations;
    private BufferedImage image;
    private Rectangle2D.Float bounds;
    private int pastes;

    public void store(byte[] annotationData, BufferedImage picture, Rectangle2D.Float area, String text) {
        annotations = annotationData;
        image = picture;
        bounds = area == null ? null : (Rectangle2D.Float) area.clone();
        pastes = 0;
        try {
            Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (text != null && !text.isBlank()) system.setContents(new StringSelection(text), null);
            else if (picture != null) system.setContents(new PdfImageSelection(picture), null);
        } catch (HeadlessException | IllegalStateException ignored) {
            pastes = 0;
        }
    }

    public boolean isEmpty() { return annotations == null && image == null; }
    public byte[] annotations() { return annotations; }
    public BufferedImage image() { return image; }
    public Rectangle2D.Float bounds() { return bounds == null ? null : (Rectangle2D.Float) bounds.clone(); }
    public float nextOffset() { return 12f * ++pastes; }

    public static BufferedImage systemImage() {
        try {
            Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (system.isDataFlavorAvailable(DataFlavor.imageFlavor)
                    && system.getData(DataFlavor.imageFlavor) instanceof java.awt.Image picture) {
                BufferedImage copy = new BufferedImage(Math.max(1, picture.getWidth(null)), Math.max(1, picture.getHeight(null)), BufferedImage.TYPE_INT_ARGB);
                copy.getGraphics().drawImage(picture, 0, 0, null);
                return copy;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    public static String systemText() {
        try {
            Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (system.isDataFlavorAvailable(DataFlavor.stringFlavor)) return (String) system.getData(DataFlavor.stringFlavor);
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
