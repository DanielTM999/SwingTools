package dtm.stools.internal;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DrawingOnceGate {

    private final AtomicBoolean once = new AtomicBoolean(false);
    private final AtomicBoolean allowed = new AtomicBoolean(true);

    public void enable() {
        once.set(true);
    }

    public boolean isEnabled() {
        return once.get();
    }

    public boolean tryAcquire() {
        if (allowed.compareAndSet(true, false)) {
            return true;
        }
        return !once.get();
    }

    public void dispatch(Runnable drawing) {
        if (drawing != null && tryAcquire()) {
            drawing.run();
        }
    }
}
