package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.GraphicsHost;
import dtm.stools.component.panels.graphics.GraphicsInputState;
import dtm.stools.component.panels.graphics.GraphicsRender;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Queue;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class GraphicsGlHost implements GraphicsHost<GraphicsGlContext> {

    private static final long DISPOSE_WAIT_MILLIS = 5000;
    private static final int MAX_INITIALIZE_ATTEMPTS = 5;

    private final GraphicsGlCanvas canvas;
    private final Component component;
    private final GraphicsGlPresentationMode presentationMode;
    private final GraphicsGlBufferedSurface bufferedSurface;
    private final GraphicsGlContext context;
    private final GraphicsInputState inputState = new GraphicsInputState();
    private final Queue<Runnable> uiTasks = new ConcurrentLinkedQueue<>();
    private final Object frameLock = new Object();

    private volatile GraphicsRender<GraphicsGlContext> renderer;
    private volatile Thread renderThread;
    private volatile boolean contextCreated;
    private volatile boolean rendererReady;
    private volatile boolean registered;
    private volatile boolean disposeRequested;
    private volatile boolean disposed;
    private volatile boolean vsync;
    private volatile boolean vsyncDirty;
    private volatile boolean renderOnDemand;
    private volatile boolean renderRequested = true;
    private volatile boolean skipUnchangedFrames = true;
    private volatile long renderRequestVersion;
    private volatile int fps = 60;
    private CountDownLatch disposeLatch;

    private long contextHandle;
    private GraphicsRender<GraphicsGlContext> initializedRenderer;
    private int initializeFailures;
    private volatile long nextFrameNanos;
    private BufferedImage[] frameBuffers;
    private int nextFrameBuffer;
    private boolean hasPresentedFrame;

    GraphicsGlHost(GraphicsGlPresentationMode presentationMode) {
        this.presentationMode = presentationMode;
        this.canvas = new GraphicsGlCanvas(this);
        if (presentationMode == GraphicsGlPresentationMode.BUFFERED) {
            this.bufferedSurface = new GraphicsGlBufferedSurface(frameLock);
            this.component = bufferedSurface;
            canvas.setVisible(false);
        } else {
            this.bufferedSurface = null;
            this.component = canvas;
        }
        this.context = new GraphicsGlContext(this);
        this.inputState.attach(component);
    }

    @Override
    public Component getComponent() {
        return component;
    }

    GraphicsGlCanvas getContextCanvas() {
        return canvas;
    }

    boolean isBufferedPresentation() {
        return presentationMode == GraphicsGlPresentationMode.BUFFERED;
    }

    @Override
    public void setRenderer(GraphicsRender<GraphicsGlContext> renderer) {
        this.renderer = renderer;
        requestRender();
    }

    @Override
    public GraphicsRender<GraphicsGlContext> getRenderer() {
        return renderer;
    }

    @Override
    public void dispose() {
        requestDispose(true, true);
    }

    GraphicsGlContext getContext() {
        return context;
    }

    GraphicsInputState getInput() {
        return inputState;
    }

    boolean isContextCreated() {
        return contextCreated;
    }

    boolean isReady() {
        return contextCreated && rendererReady;
    }

    boolean isRendering() {
        return registered && !disposeRequested && !disposed;
    }

    boolean isDisposed() {
        return disposed;
    }

    boolean isDisposeRequested() {
        return disposeRequested;
    }

    void setVsync(boolean vsync) {
        this.vsync = vsync;
        this.vsyncDirty = true;
        wakeRenderThread();
    }

    boolean isVsync() {
        return vsync;
    }

    void setFps(int fps) {
        this.fps = fps;
        wakeRenderThread();
    }

    int getFps() {
        return fps;
    }

    void setRenderOnDemand(boolean renderOnDemand) {
        this.renderOnDemand = renderOnDemand;
        if (!renderOnDemand) {
            nextFrameNanos = 0;
            wakeRenderThread();
        }
    }

    boolean isRenderOnDemand() {
        return renderOnDemand;
    }

    void setSkipUnchangedFrames(boolean skipUnchangedFrames) {
        this.skipUnchangedFrames = skipUnchangedFrames;
    }

    boolean isSkipUnchangedFrames() {
        return skipUnchangedFrames;
    }

    void requestRender() {
        renderRequestVersion++;
        renderRequested = true;
        nextFrameNanos = 0;
        wakeRenderThread();
    }

    void runOnUiThread(Runnable task) {
        if (task == null) return;
        Thread rt = renderThread;
        if (rt != null && rt == Thread.currentThread()) {
            task.run();
            return;
        }
        uiTasks.add(task);
        requestRender();
        wakeRenderThread();
    }

    private void wakeRenderThread() {
        Thread rt = renderThread;
        if (rt != null) LockSupport.unpark(rt);
    }

    synchronized boolean markRegistered() {
        if (registered || disposed) return false;
        registered = true;
        disposeRequested = false;
        return true;
    }

    void requestDispose(boolean terminal, boolean wait) {
        if (terminal) disposed = true;
        CountDownLatch latch;
        synchronized (this) {
            if (!registered) return;
            if (disposeLatch == null) disposeLatch = new CountDownLatch(1);
            latch = disposeLatch;
            disposeRequested = true;
        }
        Thread rt = renderThread;
        wakeRenderThread();
        if (wait && rt != Thread.currentThread()) {
            try {
                latch.await(DISPOSE_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void renderFrame(long frameStartNanos) {
        renderThread = Thread.currentThread();
        if (disposeRequested || disposed) return;
        if (renderOnDemand && !renderRequested) {
            scheduleNext(frameStartNanos);
            return;
        }
        long frameRequestVersion = renderRequestVersion;

        if (contextHandle == 0) {
            if (!canvas.isDisplayable()
                    || canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
                scheduleNext(frameStartNanos);
                return;
            }
            long handle;
            try {
                handle = GlNative.nCreateContext(canvas);
            } catch (LinkageError e) {
                e.printStackTrace();
                requestDispose(true, false);
                return;
            }
            if (handle == 0) {
                scheduleNext(frameStartNanos);
                return;
            }
            contextHandle = handle;
            contextCreated = true;
            GlNative.nMakeCurrent(contextHandle);
            GlNative.nSetVsync(contextHandle, vsync);
            vsyncDirty = false;
            context.updateSize(Math.max(1, canvas.getWidth()), Math.max(1, canvas.getHeight()));
            GL.glViewport(0, 0, context.getWidth(), context.getHeight());
        } else {
            GlNative.nMakeCurrent(contextHandle);
        }

        if (vsyncDirty) {
            GlNative.nSetVsync(contextHandle, vsync);
            vsyncDirty = false;
        }

        drainTasks();

        GraphicsRender<GraphicsGlContext> currentRenderer = renderer;
        if (currentRenderer != initializedRenderer) {
            disposeInitializedRenderer();
            if (currentRenderer == null) {
                initializedRenderer = null;
            } else if (initializeRenderer(currentRenderer)) {
                initializedRenderer = currentRenderer;
                initializeFailures = 0;
            } else if (++initializeFailures >= MAX_INITIALIZE_ATTEMPTS) {
                initializedRenderer = currentRenderer;
            } else {
                scheduleNext(frameStartNanos);
                return;
            }
        }

        int w = Math.max(1, canvas.getWidth());
        int h = Math.max(1, canvas.getHeight());
        if (w != context.getWidth() || h != context.getHeight()) {
            context.updateSize(w, h);
            GL.glViewport(0, 0, w, h);
            if (currentRenderer != null) {
                if (rendererReady) {
                    try {
                        currentRenderer.resize(context, w, h);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } else {
                    initializedRenderer = null;
                    initializeFailures = 0;
                }
            }
        }

        context.updateTiming(frameStartNanos);
        if (currentRenderer != null && rendererReady) {
            try {
                currentRenderer.render(context);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (bufferedSurface != null) {
            presentBufferedFrame(w, h);
        }
        GlNative.nSwapBuffers(contextHandle);
        if (frameRequestVersion == renderRequestVersion) {
            renderRequested = false;
        }
        scheduleNext(frameStartNanos);
    }

    void disposeOnRenderThread() {
        if (contextHandle != 0) {
            GlNative.nMakeCurrent(contextHandle);
            disposeInitializedRenderer();
            GlNative.nDestroyContext(contextHandle);
            contextHandle = 0;
        }
        initializedRenderer = null;
        initializeFailures = 0;
        contextCreated = false;
        rendererReady = false;
        nextFrameNanos = 0;
        uiTasks.clear();
        synchronized (frameLock) {
            frameBuffers = null;
            nextFrameBuffer = 0;
            hasPresentedFrame = false;
        }
        context.resetTiming();
        renderThread = null;
        synchronized (this) {
            registered = false;
            disposeRequested = false;
            CountDownLatch latch = disposeLatch;
            disposeLatch = null;
            if (latch != null) latch.countDown();
        }
    }

    long getNextFrameNanos() {
        return nextFrameNanos;
    }

    private void scheduleNext(long frameStartNanos) {
        if (renderOnDemand) {
            nextFrameNanos = Long.MAX_VALUE;
            return;
        }
        int f = fps;
        if (f <= 0 || vsync) {
            nextFrameNanos = frameStartNanos;
            return;
        }
        long period = 1_000_000_000L / f;
        long next = nextFrameNanos + period;
        if (next <= frameStartNanos) next = frameStartNanos + period;
        nextFrameNanos = next;
    }

    private boolean initializeRenderer(GraphicsRender<GraphicsGlContext> currentRenderer) {
        try {
            if (currentRenderer instanceof GraphicsGlRender glRenderer) {
                GraphicsGlRenderContextRegistry.bind(glRenderer, context);
            }
            currentRenderer.initialize(context);
            GL.glViewport(0, 0, context.getWidth(), context.getHeight());
            currentRenderer.resize(context, context.getWidth(), context.getHeight());
            rendererReady = true;
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                currentRenderer.dispose(context);
            } catch (Throwable ignored) {
            }
            if (currentRenderer instanceof GraphicsGlRender glRenderer) {
                GraphicsGlRenderContextRegistry.unbind(glRenderer);
            }
            rendererReady = false;
            return false;
        }
    }

    private void drainTasks() {
        Runnable task;
        while ((task = uiTasks.poll()) != null) {
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void disposeInitializedRenderer() {
        if (initializedRenderer != null) {
            try {
                initializedRenderer.dispose(context);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (initializedRenderer instanceof GraphicsGlRender glRenderer) {
                GraphicsGlRenderContextRegistry.unbind(glRenderer);
            }
            initializedRenderer = null;
        }
        rendererReady = false;
    }

    private void presentBufferedFrame(int width, int height) {
        synchronized (frameLock) {
            ensureFrameBuffers(width, height);
            BufferedImage image = frameBuffers[nextFrameBuffer];
            int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            GlNative.nReadPixels(width, height, pixels);
            flipRows(pixels, width, height);
            BufferedImage previousImage = frameBuffers[(nextFrameBuffer + 1) % frameBuffers.length];
            int[] previousPixels = ((DataBufferInt) previousImage.getRaster().getDataBuffer()).getData();
            if (!skipUnchangedFrames || !hasPresentedFrame || !Arrays.equals(pixels, previousPixels)) {
                bufferedSurface.present(image);
                hasPresentedFrame = true;
            }
            nextFrameBuffer = (nextFrameBuffer + 1) % frameBuffers.length;
        }
    }

    private void ensureFrameBuffers(int width, int height) {
        if (frameBuffers != null && frameBuffers[0].getWidth() == width && frameBuffers[0].getHeight() == height) {
            return;
        }
        frameBuffers = new BufferedImage[]{
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        };
        nextFrameBuffer = 0;
        hasPresentedFrame = false;
    }

    private static void flipRows(int[] pixels, int width, int height) {
        if (pixels == null) return;
        for (int top = 0, bottom = height - 1; top < bottom; top++, bottom--) {
            int topOffset = top * width;
            int bottomOffset = bottom * width;
            for (int x = 0; x < width; x++) {
                int pixel = pixels[topOffset + x];
                pixels[topOffset + x] = pixels[bottomOffset + x];
                pixels[bottomOffset + x] = pixel;
            }
        }
    }
}
