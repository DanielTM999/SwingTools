package dtm.stools.component.icon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Wraps an {@link Icon} so it is painted scaled down to fit a maximum size,
 * preserving its aspect ratio. Icons that already fit are returned untouched.
 */
public final class FittedIcon implements Icon {
    private final Icon source;
    private final int width;
    private final int height;

    private FittedIcon(Icon source, int width, int height) {
        this.source = source;
        this.width = width;
        this.height = height;
    }

    public static Icon fit(Icon source, int maxSize) {
        if (source == null || maxSize <= 0) return source;
        int sourceWidth = source.getIconWidth();
        int sourceHeight = source.getIconHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) return source;
        if (sourceWidth <= maxSize && sourceHeight <= maxSize) return source;
        double scale = Math.min(1d, Math.min(maxSize / (double) sourceWidth,
                maxSize / (double) sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new FittedIcon(source, width, height);
    }

    public Icon getSource() { return source; }

    @Override public int getIconWidth() { return width; }
    @Override public int getIconHeight() { return height; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D scoped = (Graphics2D) graphics.create();
        try {
            scoped.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            scoped.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            scoped.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            scoped.translate(x, y);
            scoped.transform(AffineTransform.getScaleInstance(
                    width / (double) source.getIconWidth(),
                    height / (double) source.getIconHeight()));
            source.paintIcon(component, scoped, 0, 0);
        } finally {
            scoped.dispose();
        }
    }
}
