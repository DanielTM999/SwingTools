package dtm.stools.component.charts2d;

import dtm.stools.component.charts.style.ChartColor;
import dtm.stools.component.charts.style.ChartTheme;
import dtm.stools.component.charts2d.render.Chart2DRender;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Iterator;
import java.util.Objects;

public class ChartExporter {

    private final Chart2DRender render;
    private int width = 800;
    private int height = 450;
    private double scale = 1d;
    private ChartImageFormat format = ChartImageFormat.PNG;
    private float jpegQuality = 0.9f;
    private ChartTheme theme;
    private ChartColor background;
    private Color jpegMatte = Color.WHITE;

    public ChartExporter(Chart2DRender render) {
        this.render = Objects.requireNonNull(render, "render cannot be null");
    }

    public Chart2DRender getRender() {
        return render;
    }

    public int getWidth() {
        return width;
    }

    public ChartExporter setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than zero");
        }
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public ChartExporter setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than zero");
        }
        this.height = height;
        return this;
    }

    public ChartExporter setSize(int width, int height) {
        return setWidth(width).setHeight(height);
    }

    public double getScale() {
        return scale;
    }

    public ChartExporter setScale(double scale) {
        if (!(scale > 0d)) {
            throw new IllegalArgumentException("scale must be greater than zero");
        }
        this.scale = scale;
        return this;
    }

    public ChartImageFormat getFormat() {
        return format;
    }

    public ChartExporter setFormat(ChartImageFormat format) {
        this.format = Objects.requireNonNull(format, "format cannot be null");
        return this;
    }

    public float getJpegQuality() {
        return jpegQuality;
    }

    public ChartExporter setJpegQuality(float jpegQuality) {
        this.jpegQuality = Math.max(0f, Math.min(1f, jpegQuality));
        return this;
    }

    public ChartTheme getTheme() {
        return theme;
    }

    public ChartExporter setTheme(ChartTheme theme) {
        this.theme = theme;
        return this;
    }

    public ChartColor getBackground() {
        return background;
    }

    public ChartExporter setBackground(ChartColor background) {
        this.background = background;
        return this;
    }

    public ChartExporter setBackground(Color background) {
        return setBackground(background == null ? null : ChartColor.fromAwt(background));
    }

    public ChartExporter setTransparentBackground() {
        return setBackground(ChartColor.TRANSPARENT);
    }

    public Color getJpegMatte() {
        return jpegMatte;
    }

    public ChartExporter setJpegMatte(Color jpegMatte) {
        this.jpegMatte = Objects.requireNonNull(jpegMatte, "jpegMatte cannot be null");
        return this;
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            write(renderImage(format), format, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String toBase64() {
        return Base64.getEncoder().encodeToString(toBytes());
    }

    public void writeTo(File file) throws IOException {
        Objects.requireNonNull(file, "file cannot be null");
        ChartImageFormat fileFormat = ChartImageFormat.fromFile(file);
        ChartImageFormat target = fileFormat != null ? fileFormat : format;
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        BufferedImage image = renderImage(target);
        try (OutputStream output = Files.newOutputStream(file.toPath())) {
            write(image, target, output);
        }
    }

    private BufferedImage renderImage(ChartImageFormat target) {
        int pixelWidth = Math.max(1, (int) Math.round(width * scale));
        int pixelHeight = Math.max(1, (int) Math.round(height * scale));
        int type = target.isOpaque() ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, type);
        Graphics2D g2 = image.createGraphics();
        try {
            if (target.isOpaque()) {
                g2.setColor(jpegMatte);
                g2.fillRect(0, 0, pixelWidth, pixelHeight);
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.scale(scale, scale);
            render.paintSnapshot(g2, width, height, resolveTheme());
        } finally {
            g2.dispose();
        }
        return image;
    }

    private ChartTheme resolveTheme() {
        if (background == null) {
            return theme;
        }
        ChartTheme base = theme != null ? theme : render.getTheme();
        return base.copy().setBackground(background);
    }

    private void write(BufferedImage image, ChartImageFormat target, OutputStream output) throws IOException {
        if (target != ChartImageFormat.JPEG) {
            if (!ImageIO.write(image, target.getImageIoName(), output)) {
                throw new IOException("No image writer available for " + target);
            }
            return;
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(target.getImageIoName());
        if (!writers.hasNext()) {
            throw new IOException("No image writer available for " + target);
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(jpegQuality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
