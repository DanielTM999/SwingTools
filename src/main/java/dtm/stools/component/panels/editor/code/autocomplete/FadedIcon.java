package dtm.stools.component.panels.editor.code.autocomplete;

import javax.swing.Icon;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

public final class FadedIcon implements Icon {

    private final Icon delegate;
    private final float alpha;

    public FadedIcon(Icon delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    public Icon delegate() {
        return delegate;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            delegate.paintIcon(c, g2, x, y);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return delegate.getIconHeight();
    }
}
