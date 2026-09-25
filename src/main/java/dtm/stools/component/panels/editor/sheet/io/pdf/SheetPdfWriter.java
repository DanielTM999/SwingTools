package dtm.stools.component.panels.editor.sheet.io.pdf;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SheetPdfWriter {
    private record PageImage(byte[] jpeg, int pixelWidth, int pixelHeight, double width, double height) {}

    private final List<PageImage> pages = new ArrayList<>();
    private final float quality;
    private String title = "";

    public SheetPdfWriter() { this(0.9f); }
    public SheetPdfWriter(float quality) { this.quality = Math.max(0.1f, Math.min(1f, quality)); }

    public SheetPdfWriter title(String value) { title = value == null ? "" : value; return this; }

    public void addPage(BufferedImage image, double widthPoints, double heightPoints) throws IOException {
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = rgb.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
        pages.add(new PageImage(jpeg(rgb), rgb.getWidth(), rgb.getHeight(), widthPoints, heightPoints));
    }

    private byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    public int pageCount() { return pages.size(); }

    public void write(OutputStream target) throws IOException {
        if (pages.isEmpty()) throw new IOException("O PDF não possui páginas.");
        CountingStream out = new CountingStream(target);
        List<Long> offsets = new ArrayList<>();
        out.ascii("%PDF-1.4\n%âãÏÓ\n");
        int objects = 3 + pages.size() * 3;
        List<Integer> pageIds = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) pageIds.add(4 + i * 3);
        offsets.add(out.count);
        out.ascii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        offsets.add(out.count);
        StringBuilder kids = new StringBuilder();
        for (int id : pageIds) kids.append(id).append(" 0 R ");
        out.ascii("2 0 obj\n<< /Type /Pages /Count " + pages.size() + " /Kids [" + kids + "] >>\nendobj\n");
        offsets.add(out.count);
        out.ascii("3 0 obj\n<< /Producer (SwingTools SheetEditor) /Title (" + escape(title) + ") >>\nendobj\n");
        for (int i = 0; i < pages.size(); i++) {
            PageImage p = pages.get(i);
            int pageId = 4 + i * 3, contentId = pageId + 1, imageId = pageId + 2;
            String w = num(p.width()), h = num(p.height());
            offsets.add(out.count);
            out.ascii(pageId + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + w + " " + h + "] /Resources << /XObject << /Im" + i + " " + imageId + " 0 R >> /ProcSet [/PDF /ImageC] >> /Contents " + contentId + " 0 R >>\nendobj\n");
            byte[] content = ("q " + w + " 0 0 " + h + " 0 0 cm /Im" + i + " Do Q\n").getBytes(StandardCharsets.US_ASCII);
            offsets.add(out.count);
            out.ascii(contentId + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.write(content);
            out.ascii("endstream\nendobj\n");
            offsets.add(out.count);
            out.ascii(imageId + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + p.pixelWidth() + " /Height " + p.pixelHeight() + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + p.jpeg().length + " >>\nstream\n");
            out.write(p.jpeg());
            out.ascii("\nendstream\nendobj\n");
        }
        long xref = out.count;
        out.ascii("xref\n0 " + (objects + 1) + "\n0000000000 65535 f \n");
        for (long off : offsets) out.ascii(String.format("%010d 00000 n \n", off));
        out.ascii("trailer\n<< /Size " + (objects + 1) + " /Root 1 0 R /Info 3 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        out.flush();
    }

    private static String num(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.2f", v); }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
            else if (c < 32 || c > 126) b.append('?');
            else b.append(c);
        }
        return b.toString();
    }

    private static final class CountingStream extends OutputStream {
        private final OutputStream out;
        long count;

        CountingStream(OutputStream out) { this.out = out; }

        void ascii(String s) throws IOException { write(s.getBytes(StandardCharsets.ISO_8859_1)); }

        @Override public void write(int b) throws IOException { out.write(b); count++; }
        @Override public void write(byte[] b, int off, int len) throws IOException { out.write(b, off, len); count += len; }
        @Override public void flush() throws IOException { out.flush(); }
    }
}
