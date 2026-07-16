package dtm.stools.component.panels.graphics.gl;

import java.awt.*;

class GraphicsGlCanvas extends Canvas {

    private final GraphicsGlHost host;

    GraphicsGlCanvas(GraphicsGlHost host) {
        this.host = host;
        setIgnoreRepaint(true);
        setBackground(Color.BLACK);
        setFocusable(true);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void update(Graphics g) {
    }

    @Override
    public void removeNotify() {
        host.requestDispose(false, true);
        super.removeNotify();
    }
}
