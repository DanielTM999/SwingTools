package dtm.stools.internal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface Listener<T> {
    String add(T listener);

    boolean remove(String id);
    T removeAndGet(String id);

    void notifyAllListeners(Consumer<T> action, Consumer<Throwable> onError);

    <S extends Future<Void>> S notifyAllListenersAsync(Consumer<T> action, Consumer<Throwable> onError);

    <S extends Future<Void>> S notifyAllListenersAsync(Consumer<T> action, Consumer<Throwable> onError, ExecutorService executor);

    List<T> getListeners();

    T getListener(String id);
}
