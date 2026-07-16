package dtm.stools.component.panels.graphics.gl;

import dtm.stools.component.panels.graphics.RenderThreadingMode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

class GraphicsGlUiSchedule {

    public static final GraphicsGlUiSchedule INSTANCE = new GraphicsGlUiSchedule();

    private static final long MAX_PARK_NANOS = 50_000_000L;
    private static final long SPIN_MARGIN_NANOS = 2_000_000L;

    private final List<GraphicsGlHost> sharedHosts = new CopyOnWriteArrayList<>();
    private final AtomicInteger threadCounter = new AtomicInteger();
    private final Object lock = new Object();
    private Thread sharedThread;

    private GraphicsGlUiSchedule() {
    }

    public void register(GraphicsGlHost host, RenderThreadingMode mode) {
        synchronized (lock) {
            if (!host.markRegistered()) return;
            if (mode == RenderThreadingMode.INDIVIDUAL) {
                Thread thread = new Thread(() -> individualLoop(host),
                        "SwingTools-GL-Render-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                thread.start();
            } else {
                sharedHosts.add(host);
                if (sharedThread == null) {
                    sharedThread = new Thread(this::sharedLoop, "SwingTools-GL-Render");
                    sharedThread.setDaemon(true);
                    sharedThread.start();
                } else {
                    LockSupport.unpark(sharedThread);
                }
            }
        }
    }

    public void unregister(GraphicsGlHost host) {
        host.requestDispose(false, false);
    }

    private void sharedLoop() {
        while (true) {
            long now = System.nanoTime();
            long earliest = now + MAX_PARK_NANOS;
            for (GraphicsGlHost host : sharedHosts) {
                try {
                    if (host.isDisposeRequested()) {
                        sharedHosts.remove(host);
                        host.disposeOnRenderThread();
                        continue;
                    }
                    long next = host.getNextFrameNanos();
                    if (now >= next) {
                        host.renderFrame(now);
                        next = host.getNextFrameNanos();
                    }
                    if (next < earliest) earliest = next;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            synchronized (lock) {
                if (sharedHosts.isEmpty()) {
                    sharedThread = null;
                    return;
                }
            }
            waitUntil(earliest);
        }
    }

    private void individualLoop(GraphicsGlHost host) {
        while (!host.isDisposeRequested()) {
            try {
                long now = System.nanoTime();
                long next = host.getNextFrameNanos();
                if (now >= next) {
                    host.renderFrame(now);
                } else {
                    waitUntil(next);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        host.disposeOnRenderThread();
    }

    private static void waitUntil(long deadlineNanos) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining > SPIN_MARGIN_NANOS) {
            LockSupport.parkNanos(Math.min(remaining - SPIN_MARGIN_NANOS, MAX_PARK_NANOS));
        } else if (remaining > 0) {
            Thread.onSpinWait();
        }
    }
}
