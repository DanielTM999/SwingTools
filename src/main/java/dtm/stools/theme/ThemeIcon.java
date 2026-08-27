package dtm.stools.theme;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import dtm.stools.utils.ImageUtils;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class ThemeIcon implements Icon {

    private final Icon source;
    private final Supplier<Color> colorSupplier;

    private Color tintColor;
    private Icon tintedIcon;

    public ThemeIcon(Icon source) {
        this(source, null);
    }

    public ThemeIcon(Icon source, Supplier<Color> colorSupplier) {
        this.source = source;
        this.colorSupplier = colorSupplier;
    }

    public static Icon of(Icon source) {
        return source == null ? null : new ThemeIcon(source);
    }

    public static Icon of(Icon source, Supplier<Color> colorSupplier) {
        return source == null ? null : new ThemeIcon(source, colorSupplier);
    }

    public Icon getSource() {
        return source;
    }

    public ThemeIcon derive(int width, int height) {
        if (source == null || (source.getIconWidth() == width && source.getIconHeight() == height)) {
            return this;
        }
        return new ThemeIcon(ImageUtils.resizeIcon(source, width, height), colorSupplier);
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        if (source == null) {
            return;
        }

        Color color = resolveColor(component);
        if (tintedIcon == null || !color.equals(tintColor)) {
            tintedIcon = tint(source, color);
            tintColor = color;
        }

        tintedIcon.paintIcon(component, graphics, x, y);
    }

    @Override
    public int getIconWidth() {
        return source == null ? 0 : source.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return source == null ? 0 : source.getIconHeight();
    }

    private Color resolveColor(Component component) {
        if (colorSupplier != null) {
            Color color = colorSupplier.get();
            if (color != null) {
                return color;
            }
        }

        if (component != null && component.getForeground() != null) {
            return component.getForeground();
        }

        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.GRAY;
    }

    private static Icon tint(Icon icon, Color color) {
        if (icon instanceof FlatSVGIcon svgIcon) {
            FlatSVGIcon derived = new FlatSVGIcon(svgIcon);
            derived.setColorFilter(new FlatSVGIcon.ColorFilter(source -> color));
            return derived;
        }

        int width = Math.max(1, icon.getIconWidth());
        int height = Math.max(1, icon.getIconHeight());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(null, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }

        return new javax.swing.ImageIcon(ImageUtils.changeImageColor(image, color));
    }
}
