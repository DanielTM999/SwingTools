package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.WordTask;
import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class WordTaskRunner implements AutoCloseable {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<WordTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private volatile Consumer<Throwable> errorHandler = e -> {};
    private volatile boolean closed;

    public void setErrorHandler(Consumer<Throwable> handler) { errorHandler = Objects.requireNonNull(handler); }
    public boolean isClosed() { return closed; }
    public <T> WordTask<T> task() { WordTask<T> task = new WordTask<>(); tasks.add(task); task.completion().whenComplete((v,e) -> tasks.remove(task)); return task; }
    public Future<?> submit(Runnable work) { return executor.submit(work); }
    public void fail(WordTask<?> task, Throwable error) {
        SwingUtilities.invokeLater(() -> { task.fail(error); if (!closed && !task.isCancelled()) errorHandler.accept(error); });
    }
    public int active() { return tasks.size(); }
    @Override public void close() {
        if (closed) return;
        closed = true;
        for (WordTask<?> task : tasks) task.cancel();
        executor.shutdownNow();
    }
}
