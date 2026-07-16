package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.GraphicsContext;
import dtm.stools.component.panels.graphics.GraphicsInput;

public class GraphicsGlContext implements GraphicsContext {

    private final GraphicsGlHost host;

    private volatile int width;
    private volatile int height;
    private volatile float deltaTime;
    private volatile int fps;
    private volatile long frameCount;

    private long lastFrameNanos;
    private long fpsWindowStartNanos;
    private int fpsWindowFrames;

    GraphicsGlContext(GraphicsGlHost host) {
        this.host = host;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void runOnUiThread(Runnable task) {
        host.runOnUiThread(task);
    }

    @Override
    public GraphicsInput getInput() {
        return host.getInput();
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public int getFps() {
        return fps;
    }

    public long getFrameCount() {
        return frameCount;
    }

    void updateSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void updateTiming(long frameNanos) {
        if (lastFrameNanos != 0) {
            deltaTime = (frameNanos - lastFrameNanos) / 1_000_000_000f;
        }
        lastFrameNanos = frameNanos;
        frameCount++;

        if (fpsWindowStartNanos == 0) fpsWindowStartNanos = frameNanos;
        fpsWindowFrames++;
        long elapsed = frameNanos - fpsWindowStartNanos;
        if (elapsed >= 1_000_000_000L) {
            fps = (int) Math.round(fpsWindowFrames * 1_000_000_000.0 / elapsed);
            fpsWindowStartNanos = frameNanos;
            fpsWindowFrames = 0;
        }
    }

    void resetTiming() {
        lastFrameNanos = 0;
        fpsWindowStartNanos = 0;
        fpsWindowFrames = 0;
        deltaTime = 0f;
        fps = 0;
    }
}
