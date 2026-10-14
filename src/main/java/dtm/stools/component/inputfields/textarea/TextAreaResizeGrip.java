package dtm.stools.component.inputfields.textarea;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class TextAreaResizeGrip extends JComponent {

    private final SmartTextAreaScrollPane pane;
    private Point pressPoint;
    private Dimension pressSize;
    private Dimension pressPreferred;
    private Dimension originalPreferred;
    private boolean originalCaptured;

    TextAreaResizeGrip(SmartTextAreaScrollPane pane) {
        this.pane = pane;
        setOpaque(false);
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (e.getClickCount() == 2) {
                    restore();
                    return;
                }
                begin(e.getLocationOnScreen());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressPoint == null) return;
                Point now = e.getLocationOnScreen();
                resizeBy(now.x - pressPoint.x, now.y - pressPoint.y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressPoint = null;
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
        syncCursor();
    }

    void syncCursor() {
        TextAreaResize mode = pane.getTextArea().getResize();
        int cursor = switch (mode) {
            case BOTH -> Cursor.SE_RESIZE_CURSOR;
            case HORIZONTAL -> Cursor.E_RESIZE_CURSOR;
            case VERTICAL -> Cursor.S_RESIZE_CURSOR;
            case NONE -> Cursor.DEFAULT_CURSOR;
        };
        setCursor(Cursor.getPredefinedCursor(cursor));
        setVisible(mode != TextAreaResize.NONE);
    }

    void begin(Point screenPoint) {
        if (!originalCaptured) {
            originalPreferred = pane.isPreferredSizeSet() ? pane.getPreferredSize() : null;
            originalCaptured = true;
        }
        pressPoint = screenPoint;
        pressSize = pane.getSize();
        pressPreferred = pane.getPreferredSize();
    }

    void resizeBy(int dx, int dy) {
        if (pressSize == null) begin(new Point());
        TextAreaResize mode = pane.getTextArea().getResize();
        if (mode == TextAreaResize.NONE) return;
        Dimension minimum = minimumResizeSize();
        Dimension maximum = pane.isMaximumSizeSet() ? pane.getMaximumSize() : new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        int width = mode.allowsHorizontal() ? Math.clamp(pressSize.width + dx, minimum.width, maximum.width) : pressPreferred.width;
        int height = mode.allowsVertical() ? Math.clamp(pressSize.height + dy, minimum.height, maximum.height) : pressPreferred.height;
        apply(new Dimension(width, height));
    }

    void restore() {
        if (!originalCaptured) return;
        pane.setPreferredSize(originalPreferred);
        relayout();
        pane.getTextArea().fireResized(pane.getPreferredSize());
    }

    @Override
    public Dimension getPreferredSize() {
        int size = UiTokens.scale(14);
        return new Dimension(size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            g2.setColor(UiTokens.muted());
            g2.setStroke(new BasicStroke(Math.max(1f, UiTokens.stroke()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int w = getWidth() - UiTokens.scale(3);
            int h = getHeight() - UiTokens.scale(3);
            int gap = UiTokens.scale(4);
            for (int i = 1; i <= 2; i++) {
                int offset = gap * i;
                g2.drawLine(w - offset, h, w, h - offset);
            }
        } finally {
            g2.dispose();
        }
    }

    private void apply(Dimension size) {
        if (size.equals(pane.getPreferredSize()) && pane.isPreferredSizeSet()) return;
        pane.setPreferredSize(size);
        relayout();
        pane.getTextArea().fireResized(size);
    }

    private void relayout() {
        pane.revalidate();
        Container parent = pane.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private Dimension minimumResizeSize() {
        int row = pane.getTextArea().getFontMetrics(pane.getTextArea().getFont()).getHeight();
        Insets insets = pane.getInsets();
        int height = row + insets.top + insets.bottom + UiTokens.scale(8);
        int width = UiTokens.scale(80);
        if (pane.isMinimumSizeSet()) {
            Dimension minimum = pane.getMinimumSize();
            width = Math.max(width, minimum.width);
            height = Math.max(height, minimum.height);
        }
        return new Dimension(width, Math.max(height, getPreferredSize().height * 2));
    }
}
