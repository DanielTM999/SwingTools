package dtm.stools.component.panels.editor.word.io.pdf;

import dtm.stools.component.panels.editor.word.layout.WordLayout;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.provider.WordExportProvider;
import dtm.stools.component.panels.editor.word.render.WordRenderer;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class WordPdfExportProvider implements WordExportProvider {
    private final String id;
    private final double dpi;
    private final WordRenderer renderer;

    public WordPdfExportProvider() { this("word.export.pdf",150,new WordRenderer()); }
    public WordPdfExportProvider(String id, double dpi, WordRenderer renderer) {
        if (!Double.isFinite(dpi) || dpi < 72 || dpi > 600) throw new IllegalArgumentException("DPI must be 72..600");
        this.id = Objects.requireNonNull(id); this.dpi = dpi; this.renderer = Objects.requireNonNull(renderer);
    }
    @Override public String id() { return id; }
    @Override public String extension() { return "pdf"; }

    @Override public void export(WordDocument document, WordLayout layout, OutputStream output) throws IOException {
        List<Integer> offsets = new ArrayList<>();
        ByteCounter out = new ByteCounter(output);
        out.write("%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1));
        int pages = layout.pages().size(), objects = 2 + pages*3;
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages; i++) kids.append(3 + i*3).append(" 0 R ");
        object(out,offsets,1,"<< /Type /Catalog /Pages 2 0 R >>");
        object(out,offsets,2,"<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>");
        for (int i = 0; i < pages; i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled");
            WordLayout.Page page = layout.pages().get(i);
            int pageId = 3 + i*3, imageId = pageId+1, contentId = pageId+2;
            int w = (int)Math.ceil(page.width()*dpi/72), h = (int)Math.ceil(page.height()*dpi/72);
            BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try { g.scale(dpi/72,dpi/72); renderer.paintPage(g,page,null,null); } finally { g.dispose(); }
            byte[] jpeg = jpeg(image);
            String size = String.format(Locale.ROOT,"%.3f %.3f",page.width(),page.height());
            object(out,offsets,pageId,"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + size + "] /Resources << /XObject << /Im0 " + imageId + " 0 R >> >> /Contents " + contentId + " 0 R >>");
            offsets.add(out.count);
            out.write((imageId + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + w + " /Height " + h + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpeg.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(jpeg);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
            String content = String.format(Locale.ROOT,"q %.3f 0 0 %.3f 0 0 cm /Im0 Do Q",page.width(),page.height());
            object(out,offsets,contentId,"<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream");
        }
        int xref = out.count;
        StringBuilder b = new StringBuilder("xref\n0 " + (objects+1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) b.append(String.format(Locale.ROOT,"%010d 00000 n \n",offset));
        b.append("trailer\n<< /Size ").append(objects+1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.write(b.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }
    private static void object(ByteCounter out, List<Integer> offsets, int id, String body) throws IOException {
        offsets.add(out.count);
        out.write((id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
    }
    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam(); param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); param.setCompressionQuality(0.9f);
            writer.write(null,new IIOImage(image,null,null),param);
        } finally { writer.dispose(); }
        return bytes.toByteArray();
    }
    private static final class ByteCounter extends OutputStream {
        private final OutputStream target; int count;
        ByteCounter(OutputStream target) { this.target = target; }
        @Override public void write(int b) throws IOException { target.write(b); count++; }
        @Override public void write(byte[] b, int off, int len) throws IOException { target.write(b,off,len); count += len; }
        @Override public void flush() throws IOException { target.flush(); }
    }
}
