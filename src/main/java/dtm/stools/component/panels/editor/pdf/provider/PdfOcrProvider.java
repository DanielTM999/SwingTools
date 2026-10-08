package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.api.PdfOcrResult;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.IntConsumer;

public interface PdfOcrProvider extends PdfProvider {
    PdfOcrResult recognize(BufferedImage page, String languages, IntConsumer progress) throws IOException;
}
