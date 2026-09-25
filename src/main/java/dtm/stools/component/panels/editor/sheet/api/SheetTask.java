package dtm.stools.component.panels.editor.sheet.api;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class SheetTask<T> {
    private final CompletableFuture<T> result = new CompletableFuture<>();
    private final AtomicInteger progress = new AtomicInteger();
    private Future<?> worker;
    private boolean committing;

    public static <T> SheetTask<T> completed(T value) { SheetTask<T> t = new SheetTask<>(); t.complete(value); return t; }
    public static <T> SheetTask<T> failed(Throwable error) { SheetTask<T> t = new SheetTask<>(); t.fail(error); return t; }

    public CompletionStage<T> completion() { return result.minimalCompletionStage(); }
    public CompletableFuture<T> future() { return result.copy(); }
    public int progress() { return progress.get(); }
    public boolean isCancelled() { return result.isCancelled(); }
    public boolean isDone() { return result.isDone(); }

    public synchronized boolean cancel() {
        if (committing || result.isDone()) return false;
        boolean cancelled = result.cancel(false);
        if (cancelled && worker != null) worker.cancel(true);
        return cancelled;
    }

    public synchronized void attach(Future<?> value) { worker = value; if (result.isCancelled()) worker.cancel(true); }
    public void progress(int value) { progress.set(Math.max(0, Math.min(100, value))); }
    public synchronized void complete(T value) { if (!result.isDone()) { progress.set(100); result.complete(value); } }
    public synchronized void fail(Throwable error) { result.completeExceptionally(error); }
    public synchronized boolean beginCommit() { if (result.isDone()) return false; committing = true; return true; }

    public synchronized void commit(Callable<T> operation) {
        if (result.isDone()) return;
        try { complete(operation.call()); } catch (Throwable error) { fail(error); }
    }
}
