package dtm.stools.utils;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import dtm.stools.theme.ThemeIcon;
import dtm.stools.exceptions.ResourceNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;

public final class ImageUtils {

    private ImageUtils(){
        throw new IllegalStateException("utility class");
    }

    public static Optional<ImageIcon> getImageIconByResource(Class<?> aClass, String path){
        return getImageIconByResource(aClass, path, false);
    }

    public static Optional<ImageIcon> getImageIconByResource(Class<?> aClass, String path, boolean external){
        try{
            URL url = ResourceUtils.getResource(aClass, path, external);
            return Optional.of(loadImageIcon(url, path));
        }catch (Exception e){
            return Optional.empty();
        }
    }

    public static ImageIcon getImageIconByResourceOrThrow(Class<?> aClass, String path){
        return getImageIconByResourceOrThrow(aClass, path, false);
    }

    public static ImageIcon getImageIconByResourceOrThrow(Class<?> aClass, String path, boolean external){
        try{
            URL url = ResourceUtils.getResource(aClass, path, external);
            return loadImageIcon(url, path);
        }catch (Exception e){
            throw new ResourceNotFoundException("Erro ao achar Recurso: "+path, path);
        }
    }

    public static Optional<Icon> getIconByResource(Class<?> aClass, String path){
        return getIconByResource(aClass, path, false);
    }

    public static Optional<Icon> getIconByResource(Class<?> aClass, String path, boolean external){
        try{
            URL url = ResourceUtils.getResource(aClass, path, external);
            return Optional.of(loadIcon(url, path));
        }catch (Exception e){
            return Optional.empty();
        }
    }

    public static Icon getIconByResourceOrThrow(Class<?> aClass, String path){
        return getIconByResourceOrThrow(aClass, path, false);
    }

    public static Icon getIconByResourceOrThrow(Class<?> aClass, String path, boolean external){
        try{
            URL url = ResourceUtils.getResource(aClass, path, external);
            return loadIcon(url, path);
        }catch (Exception e){
            throw new ResourceNotFoundException("Erro ao achar Recurso: "+path, path);
        }
    }

    public static Optional<Image> getImageByResource(Class<?> aClass, String path){
        return getImageByResource(aClass, path, false);
    }

    public static Optional<Image> getImageByResource(Class<?> aClass, String path, boolean external){
        try{
            URL url = ResourceUtils.getResource(aClass, path, external);
            return Optional.of(loadImageIcon(url, path).getImage());
        }catch (Exception e){
            return Optional.empty();
        }
    }

    public static ImageIcon getColoredImageIconByResourceOrThrow(Class<?> aClass, String path, Color targetColor) {
        return getColoredImageIconByResource(aClass, path, targetColor).orElseThrow(() -> new ResourceNotFoundException("Erro ao achar Recurso: "+path, path));
    }

    public static ImageIcon getColoredImageIconByResourceOrThrow(Class<?> aClass, String path, Color targetColor, boolean external) {
        return getColoredImageIconByResource(aClass, path, targetColor, external).orElseThrow(() -> new ResourceNotFoundException("Erro ao achar Recurso: "+path, path));
    }

    public static Optional<ImageIcon> getColoredImageIconByResource(Class<?> aClass, String path, Color targetColor) {
        return getColoredImageIconByResource(aClass, path, targetColor, false);
    }

    public static Optional<ImageIcon> getColoredImageIconByResource(Class<?> aClass, String path, Color targetColor, boolean external) {
        try {
            URL url = ResourceUtils.getResource(aClass, path, external);
            if (isSvg(path)) {
                FlatSVGIcon svg = applySvgColor(new FlatSVGIcon(url), targetColor);
                return Optional.of(new ImageIcon(rasterize(svg)));
            }
            ImageIcon original = new ImageIcon(url);
            return Optional.of(changeIconColor(original, targetColor));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Icon> getColoredIconByResource(Class<?> aClass, String path, Color targetColor) {
        return getColoredIconByResource(aClass, path, targetColor, false);
    }

    public static Optional<Icon> getColoredIconByResource(Class<?> aClass, String path, Color targetColor, boolean external) {
        try {
            URL url = ResourceUtils.getResource(aClass, path, external);
            if (isSvg(path)) {
                return Optional.of(applySvgColor(new FlatSVGIcon(url), targetColor));
            }
            return Optional.of(changeIconColor(new ImageIcon(url), targetColor));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<Image> getColoredImageByResource(Class<?> aClass, String path, Color targetColor) {
        return getColoredImageByResource(aClass, path, targetColor, false);
    }

    public static Optional<Image> getColoredImageByResource(Class<?> aClass, String path, Color targetColor, boolean external) {
        try {
            URL url = ResourceUtils.getResource(aClass, path, external);
            if (isSvg(path)) {
                FlatSVGIcon svg = applySvgColor(new FlatSVGIcon(url), targetColor);
                return Optional.of(rasterize(svg));
            }
            ImageIcon original = new ImageIcon(url);
            BufferedImage coloredImage = changeImageColor(original.getImage(), targetColor);
            return Optional.of(coloredImage);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static BufferedImage changeImageColor(Image image, Color targetColor) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        BufferedImage coloredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bufferedImage.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha > 0) {
                    int newPixel = (alpha << 24) | (targetColor.getRGB() & 0x00ffffff);
                    coloredImage.setRGB(x, y, newPixel);
                } else {
                    coloredImage.setRGB(x, y, pixel);
                }
            }
        }

        return coloredImage;
    }

    public static ImageIcon changeIconColor(ImageIcon icon, Color targetColor) {
        BufferedImage coloredImage = changeImageColor(icon.getImage(), targetColor);
        return new ImageIcon(coloredImage);
    }

    public static BufferedImage invertImageColors(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        BufferedImage invertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bufferedImage.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                red = 255 - red;
                green = 255 - green;
                blue = 255 - blue;

                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                invertedImage.setRGB(x, y, newPixel);
            }
        }

        return invertedImage;
    }

    public static ImageIcon resizeImageIcon(ImageIcon baseIcon, int resizedWidth, int resizedHeight){
        Image img = baseIcon.getImage();
        Image resizedImage = img.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }

    public static Icon resizeIcon(Icon baseIcon, int resizedWidth, int resizedHeight){
        if (baseIcon instanceof ThemeIcon themeIcon) {
            return themeIcon.derive(resizedWidth, resizedHeight);
        }
        if (baseIcon instanceof FlatSVGIcon svg) {
            return svg.derive(resizedWidth, resizedHeight);
        }
        if (baseIcon instanceof ImageIcon imageIcon) {
            return resizeImageIcon(imageIcon, resizedWidth, resizedHeight);
        }
        BufferedImage img = new BufferedImage(baseIcon.getIconWidth(), baseIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        baseIcon.paintIcon(null, g2, 0, 0);
        g2.dispose();
        Image scaled = img.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static ImageIcon textToIcon(String text, Font font, Color color, int size) {

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setFont(font);
        g2.setColor(color);

        FontMetrics fm = g2.getFontMetrics();

        int x = (size - fm.stringWidth(text)) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();

        g2.drawString(text, x, y);
        g2.dispose();

        return new ImageIcon(image);
    }

    private static boolean isSvg(String path) {
        return path != null && path.toLowerCase().endsWith(".svg");
    }

    private static Icon loadIcon(URL url, String path) {
        return isSvg(path) ? new FlatSVGIcon(url) : new ImageIcon(url);
    }

    private static ImageIcon loadImageIcon(URL url, String path) {
        if (isSvg(path)) {
            return new ImageIcon(rasterize(new FlatSVGIcon(url)));
        }
        return new ImageIcon(url);
    }

    private static BufferedImage rasterize(FlatSVGIcon svg) {
        int w = Math.max(1, svg.getIconWidth());
        int h = Math.max(1, svg.getIconHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        svg.paintIcon(null, g2, 0, 0);
        g2.dispose();
        return img;
    }

    private static FlatSVGIcon applySvgColor(FlatSVGIcon svg, Color targetColor) {
        svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> targetColor));
        return svg;
    }
}
