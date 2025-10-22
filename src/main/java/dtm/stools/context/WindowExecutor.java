package dtm.stools.context;

import dtm.stools.models.ThrowableRunnable;
import dtm.stools.models.ThrowableSupplier;

import java.util.function.Consumer;

public interface WindowExecutor {
    void execute(ThrowableRunnable runnable);
    void execute(ThrowableRunnable runnable, String action);
    <T> T execute(ThrowableSupplier<T> runnable, String action);
}
