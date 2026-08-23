package dtm.stools.component.panels.scroll;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Barra de rolagem fina, sem botões de seta, com polegar arredondado e realce no hover.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    private final int thickness;
    private final boolean paintTrack;

    private Color thumbBase;
    private Color trackBase;

    public ModernScrollBarUI() {
        this(UiTokens.scale(10), false);
    }

    public ModernScrollBarUI(int thickness, boolean paintTrack) {
        if (thickness <= 0) {
            throw new IllegalArgumentException("thickness must be greater than zero");
        }
        this.thickness = thickness;
        this.paintTrack = paintTrack;
    }

    /**
     * Define as cores do polegar e do trilho.
     */
    public ModernScrollBarUI setColors(Color thumbBase, Color trackBase) {
        this.thumbBase = thumbBase;
        this.trackBase = trackBase;
        return this;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroSizeButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroSizeButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent component, Rectangle bounds) {
        if (!paintTrack) {
            return;
        }
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Color color = trackBase != null ? trackBase : UiTokens.overlay(UiTokens.muted(), 0.08f);
            PaintUtils.fillRoundRect(g2, bounds, thickness, color);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintThumb(Graphics g, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            int inset = Math.max(1, thickness / 4);
            Rectangle thumb = new Rectangle(
                    bounds.x + inset,
                    bounds.y + inset,
                    Math.max(1, bounds.width - inset * 2),
                    Math.max(1, bounds.height - inset * 2));

            Color base = thumbBase != null ? thumbBase : UiTokens.overlay(UiTokens.muted(), 0.45f);
            Color color = isDragging || isThumbRollover() ? UiTokens.pressed(base) : base;
            PaintUtils.fillRoundRect(g2, thumb, Math.min(thumb.width, thumb.height), color);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(thickness, UiTokens.scale(28));
    }

    @Override
    public Dimension getPreferredSize(JComponent component) {
        return scrollbar.getOrientation() == javax.swing.JScrollBar.VERTICAL
                ? new Dimension(thickness, UiTokens.scale(48))
                : new Dimension(UiTokens.scale(48), thickness);
    }

    private JButton zeroSizeButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        button.setBorder(null);
        return button;
    }
}
