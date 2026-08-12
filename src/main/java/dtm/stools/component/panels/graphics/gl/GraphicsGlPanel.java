package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.AbstractGraphicsPanel;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.GraphicsRender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GraphicsGlPanel extends AbstractGraphicsPanel<GraphicsGlContext> {

    private static final GraphicsGlUiSchedule uiSchedule = GraphicsGlUiSchedule.INSTANCE;
    private final GraphicsGlHost graphicsGlHost;
    private final GraphicsGlPresentationMode presentationMode;

    public GraphicsGlPanel() {
        this(GraphicsGlPresentationMode.BUFFERED);
    }

    public GraphicsGlPanel(GraphicsGlPresentationMode presentationMode) {
        if (presentationMode == null) throw new IllegalArgumentException("presentationMode cannot be null");
        this.presentationMode = presentationMode;
        this.graphicsGlHost = new GraphicsGlHost(presentationMode);
        setLayout(null);
        if (graphicsGlHost.isBufferedPresentation()) {
            add(graphicsGlHost.getContextCanvas());
        }
        add(graphicsGlHost.getComponent());
        installEventForwarding(graphicsGlHost.getComponent());
    }

    public GraphicsGlPanel(GraphicsGlRender renderer) {
        this();
        setRenderer(renderer);
    }

    public GraphicsGlPanel(GraphicsGlRender renderer, GraphicsGlPresentationMode presentationMode) {
        this(presentationMode);
        setRenderer(renderer);
    }

    public GraphicsGlPresentationMode getPresentationMode() {
        return presentationMode;
    }

    /**
     * When enabled, rendering stops after the next requested frame. Use
     * {@link #requestRender()} whenever application state changes.
     */
    public void setRenderOnDemand(boolean renderOnDemand) {
        graphicsGlHost.setRenderOnDemand(renderOnDemand);
    }

    public boolean isRenderOnDemand() {
        return graphicsGlHost.isRenderOnDemand();
    }

    /** Requests one frame when {@linkplain #isRenderOnDemand() on-demand rendering} is enabled. */
    public void requestRender() {
        graphicsGlHost.requestRender();
    }

    /**
     * Controls whether unchanged buffered frames skip Swing repaint requests.
     * This optimization is enabled by default.
     */
    public void setSkipUnchangedFrames(boolean skipUnchangedFrames) {
        graphicsGlHost.setSkipUnchangedFrames(skipUnchangedFrames);
    }

    public boolean isSkipUnchangedFrames() {
        return graphicsGlHost.isSkipUnchangedFrames();
    }

    @Override
    public void setRenderer(GraphicsRender<GraphicsGlContext> renderer) {
        graphicsGlHost.setRenderer(renderer);
    }

    @Override
    public GraphicsRender<GraphicsGlContext> getRenderer() {
        return graphicsGlHost.getRenderer();
    }

    public GraphicsGlContext getGraphicsContext() {
        return graphicsGlHost.getContext();
    }

    @Override
    public void runOnUiThread(Runnable task) {
        graphicsGlHost.runOnUiThread(task);
    }

    @Override
    public GraphicsInput getInput() {
        return graphicsGlHost.getInput();
    }

    @Override
    public boolean isReady() {
        return graphicsGlHost.isReady();
    }

    @Override
    public boolean isContextCreated() {
        return graphicsGlHost.isContextCreated();
    }

    @Override
    public boolean isRendering() {
        return graphicsGlHost.isRendering();
    }

    @Override
    public boolean isDisposed() {
        return graphicsGlHost.isDisposed();
    }

    @Override
    public void setVsync(boolean vsync) {
        graphicsGlHost.setVsync(vsync);
    }

    @Override
    public boolean isVsync() {
        return graphicsGlHost.isVsync();
    }

    @Override
    public void setFPS(int fps) {
        graphicsGlHost.setFps(fps);
    }

    @Override
    public int getFPS() {
        return graphicsGlHost.getFps();
    }

    @Override
    public int getCurrentFPS() {
        return graphicsGlHost.getContext().getFps();
    }

    @Override
    public void init() {
        if (isDisplayable() && !graphicsGlHost.isDisposed()) {
            uiSchedule.register(graphicsGlHost, getRenderMode());
        }
    }

    @Override
    public void dispose() {
        graphicsGlHost.dispose();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!graphicsGlHost.isDisposed()) {
            uiSchedule.register(graphicsGlHost, getRenderMode());
            if (graphicsGlHost.isRenderOnDemand()) {
                graphicsGlHost.requestRender();
            }
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
    }

    @Override
    public boolean requestFocusInWindow() {
        return graphicsGlHost.getComponent().requestFocusInWindow();
    }

    @Override
    public void requestFocus() {
        graphicsGlHost.getComponent().requestFocus();
    }

    @Override
    public void doLayout() {
        int width = getWidth();
        int height = getHeight();
        graphicsGlHost.getContextCanvas().setBounds(0, 0, width, height);
        if (graphicsGlHost.getComponent() != graphicsGlHost.getContextCanvas()) {
            graphicsGlHost.getComponent().setBounds(0, 0, width, height);
        }
        if (graphicsGlHost.isRenderOnDemand()) {
            graphicsGlHost.requestRender();
        }
    }

    private void installEventForwarding(Component surface) {
        surface.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                forwardKeyEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                forwardKeyEvent(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                forwardKeyEvent(e);
            }
        });

        MouseAdapter mouseForwarder = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                forwardMouseEvent(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                forwardMouseEvent(e);
            }
        };
        surface.addMouseListener(mouseForwarder);
        surface.addMouseMotionListener(mouseForwarder);
        surface.addMouseWheelListener(mouseForwarder);
    }

    private void forwardKeyEvent(KeyEvent e) {
        processKeyEvent(new KeyEvent(this, e.getID(), e.getWhen(), e.getModifiersEx(),
                e.getKeyCode(), e.getKeyChar(), e.getKeyLocation()));
    }

    private void forwardMouseEvent(MouseEvent e) {
        MouseEvent converted = SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
        if (converted instanceof MouseWheelEvent wheelEvent) {
            processMouseWheelEvent(wheelEvent);
        } else if (converted.getID() == MouseEvent.MOUSE_MOVED || converted.getID() == MouseEvent.MOUSE_DRAGGED) {
            processMouseMotionEvent(converted);
        } else {
            processMouseEvent(converted);
        }
    }
}
