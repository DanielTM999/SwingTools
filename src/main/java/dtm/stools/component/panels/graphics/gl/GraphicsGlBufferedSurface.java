package dtm.stools.component.panels.graphics.gl;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Lightweight Swing surface used by {@link GraphicsGlPresentationMode#BUFFERED}. */
final class GraphicsGlBufferedSurface extends JComponent {

    private final Object frameLock;
    private BufferedImage frame;

    GraphicsGlBufferedSurface(Object frameLock) {
        this.frameLock = frameLock;
        setOpaque(true);
        setBackground(Color.BLACK);
        setFocusable(true);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    void present(BufferedImage frame) {
        synchronized (frameLock) {
            this.frame = frame;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        synchronized (frameLock) {
            if (frame != null) {
                graphics.drawImage(frame, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }
}
