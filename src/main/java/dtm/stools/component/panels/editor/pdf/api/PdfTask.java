package dtm.stools.component.panels.editor.pdf.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class PdfTask<T> {
    private final CompletableFuture<T> result = new CompletableFuture<>();
    private final AtomicInteger progress = new AtomicInteger();
    private Future<?> worker;
    private boolean committing;

    public CompletionStage<T> completion() { return result.minimalCompletionStage(); }
    public int progress() { return progress.get(); }
    public boolean isDone() { return result.isDone(); }
    public boolean isCancelled() { return result.isCancelled(); }
    public synchronized boolean cancel() {
        if (result.isDone() || committing) return false;
        result.cancel(false);
        if (worker != null) worker.cancel(true);
        return true;
    }
    public synchronized void attach(Future<?> value) {
        worker = value;
        if (result.isCancelled()) value.cancel(true);
    }
    public void progress(int value) { progress.set(Math.max(0, Math.min(100, value))); }
    public synchronized boolean beginCommit() { if (result.isDone()) return false; committing = true; return true; }
    public void complete(T value) { progress.set(100); result.complete(value); }
    public void fail(Throwable error) { result.completeExceptionally(error); }
}
