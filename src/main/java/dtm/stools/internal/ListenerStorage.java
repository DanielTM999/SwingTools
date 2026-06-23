package dtm.stools.internal;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ListenerStorage<T> implements Listener<T> {

    private final Map<String, T> listeners = new ConcurrentHashMap<>();

    @Override
    public String add(T listener) {
        if (listener == null) return null;
        String id = UUID.randomUUID().toString();
        listeners.put(id, listener);
        return id;
    }

    @Override
    public boolean remove(String id) {
        if (id == null) return false;
        return listeners.remove(id) != null;
    }

    @Override
    public T removeAndGet(String id) {
        if (id == null) return null;
        return listeners.remove(id);
    }

    @Override
    public void notifyAllListeners(Consumer<T> action, Consumer<Throwable> onError) {
        listeners.values().forEach(listener -> {
            try {
                action.accept(listener);
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    @Override
    public <S extends Future<Void>> S notifyAllListenersAsync(Consumer<T> action, Consumer<Throwable> onError) {
        return notifyAllListenersAsync(action, onError, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends Future<Void>> S notifyAllListenersAsync(Consumer<T> action, Consumer<Throwable> onError, ExecutorService executor) {
        ExecutorService exec = (executor != null) ? executor : ForkJoinPool.commonPool();

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            notifyAllListeners(action, onError);
        }, exec);

        return (S) future;
    }

    @Override
    public List<T> getListeners() {
        return new ArrayList<>(listeners.values());
    }

    @Override
    public T getListener(String id) {
        return listeners.get(id);
    }

}

