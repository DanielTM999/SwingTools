package dtm.stools.component.icon;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class TintedIconLoader {

    private TintedIconLoader() {}

    public static Icon load(String resourcePath, int size, Color tint) {
        if (resourcePath == null) return null;
        try (InputStream in = TintedIconLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            BufferedImage src = ImageIO.read(in);
            if (src == null) return null;

            BufferedImage tinted = tint == null ? toARGB(src) : tint(src, tint);

            if (tinted.getWidth() == size && tinted.getHeight() == size) {
                return new ImageIcon(tinted);
            }

            Image base = scale(tinted, size);
            Image x2 = scale(tinted, size * 2);
            Image x3 = scale(tinted, size * 3);
            Image x4 = scale(tinted, size * 4);

            return new ImageIcon(new BaseMultiResolutionImage(base, x2, x3, x4));
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage toARGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage tint(BufferedImage src, Color tint) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int rgb = (tint.getRed() << 16) | (tint.getGreen() << 8) | tint.getBlue();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (src.getRGB(x, y) >>> 24) & 0xFF;
                out.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return out;
    }

    private static Image scale(BufferedImage src, int size) {
        if (src.getWidth() == size && src.getHeight() == size) return src;

        BufferedImage current = src;
        int intermediate = size * 2;
        if (current.getWidth() > intermediate * 2) {
            current = areaAverage(current, intermediate);
        }
        return areaAverage(current, size);
    }

    private static BufferedImage areaAverage(BufferedImage src, int size) {
        Image scaled = src.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
        new ImageIcon(scaled);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }
}
