package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.GraphicsHost;
import dtm.stools.component.panels.graphics.GraphicsInputState;
import dtm.stools.component.panels.graphics.GraphicsRender;

import java.awt.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class GraphicsGlHost implements GraphicsHost<GraphicsGlContext> {

    private static final long DISPOSE_WAIT_MILLIS = 5000;
    private static final int MAX_INITIALIZE_ATTEMPTS = 5;

    private final GraphicsGlCanvas canvas;
    private final GraphicsGlContext context;
    private final GraphicsInputState inputState = new GraphicsInputState();
    private final Queue<Runnable> uiTasks = new ConcurrentLinkedQueue<>();

    private volatile GraphicsRender<GraphicsGlContext> renderer;
    private volatile Thread renderThread;
    private volatile boolean contextCreated;
    private volatile boolean rendererReady;
    private volatile boolean registered;
    private volatile boolean disposeRequested;
    private volatile boolean disposed;
    private volatile boolean vsync;
    private volatile boolean vsyncDirty;
    private volatile int fps = 60;
    private CountDownLatch disposeLatch;

    private long contextHandle;
    private GraphicsRender<GraphicsGlContext> initializedRenderer;
    private int initializeFailures;
    private long nextFrameNanos;

    GraphicsGlHost() {
        this.canvas = new GraphicsGlCanvas(this);
        this.context = new GraphicsGlContext(this);
        this.inputState.attach(canvas);
    }

    @Override
    public Component getComponent() {
        return canvas;
    }

    @Override
    public void setRenderer(GraphicsRender<GraphicsGlContext> renderer) {
        this.renderer = renderer;
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

    void runOnUiThread(Runnable task) {
        if (task == null) return;
        Thread rt = renderThread;
        if (rt != null && rt == Thread.currentThread()) {
            task.run();
            return;
        }
        uiTasks.add(task);
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

        if (contextHandle == 0) {
            if (!canvas.isDisplayable()) {
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
            if (currentRenderer != null && rendererReady) {
                try {
                    currentRenderer.resize(context, w, h);
                } catch (Throwable t) {
                    t.printStackTrace();
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
        GlNative.nSwapBuffers(contextHandle);
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
}
