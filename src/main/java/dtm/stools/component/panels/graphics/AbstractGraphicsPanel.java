package dtm.stools.component.panels.graphics;

import javax.swing.*;

public abstract class AbstractGraphicsPanel<C extends GraphicsContext> extends JPanel {

    private volatile RenderThreadingMode renderMode = RenderThreadingMode.SHARED;

    public void setRenderMode(RenderThreadingMode renderMode) {
        if (renderMode == null) throw new IllegalArgumentException("renderMode cannot be null");
        if (isRendering()) throw new IllegalStateException("Render mode cannot be changed while the panel is rendering");
        this.renderMode = renderMode;
    }

    public RenderThreadingMode getRenderMode() {
        return renderMode;
    }

    public abstract void setRenderer(GraphicsRender<C> renderer);
    public abstract GraphicsRender<C> getRenderer();

    public abstract void runOnUiThread(Runnable task);

    public abstract GraphicsInput getInput();

    public abstract boolean isReady();
    public abstract boolean isContextCreated();
    public abstract boolean isRendering();
    public abstract boolean isDisposed();

    public abstract void setVsync(boolean vsync);
    public abstract boolean isVsync();

    public abstract void setFPS(int fps);
    public abstract int getFPS();
    public abstract int getCurrentFPS();

    public abstract void init();
    public abstract void dispose();
}
