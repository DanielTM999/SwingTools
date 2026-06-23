package dtm.stools.internal.window;

import dtm.stools.context.WindowExecutor;
import dtm.stools.models.ThrowableRunnable;
import dtm.stools.models.ThrowableSupplier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

public class ActivityWindowExecutor implements WindowExecutor {

    private final BiConsumer<String, Throwable> onErrorReference;
    private final ExecutorService executorService;

    public ActivityWindowExecutor(BiConsumer<String, Throwable> errorAction, ExecutorService executorService){
        this.onErrorReference = errorAction;
        this.executorService = executorService;
    }

    @Override
    public void execute(ThrowableRunnable runnable) {
       execute(runnable, "undefined");
    }

    @Override
    public void execute(ThrowableRunnable runnable, String action) {
        try{
            runnable.run();
        }catch (Throwable throwable){
            callExceptionHandler(action, throwable);
        }
    }

    @Override
    public <T> T execute(ThrowableSupplier<T> runnable, String action) {
        try{
            return runnable.run();
        }catch (Throwable throwable){
            callExceptionHandler(action, throwable);
        }
        return null;
    }


    @Override
    public void executeAsync(ThrowableRunnable runnable) {
        executeAsync(runnable, "undefined");
    }

    @Override
    public void executeAsync(ThrowableRunnable runnable, String action) {
        executorService.execute(() -> {
            try{
                runnable.run();
            }catch (Throwable throwable){
                callExceptionHandler(action, throwable);
            }
        });
    }

    @Override
    public <T> Future<T> executeAsync(ThrowableSupplier<T> runnable, String action) {
        return executorService.submit(() -> {
            try{
                return runnable.run();
            }catch (Throwable throwable){
                callExceptionHandler(action, throwable);
            }
            return null;
        });
    }


    private void callExceptionHandler(String action, Throwable throwable){
        if (onErrorReference != null) {
            onErrorReference.accept(action, throwable);
        } else {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw new RuntimeException("Unhandled exception in action: " + action, throwable);
            }
        }
    }
}
