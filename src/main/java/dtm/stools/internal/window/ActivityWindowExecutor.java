package dtm.stools.internal.window;

import dtm.stools.activity.Activity;
import dtm.stools.context.WindowExecutor;
import dtm.stools.models.ThrowableRunnable;
import dtm.stools.models.ThrowableSupplier;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ActivityWindowExecutor implements WindowExecutor {

    private final BiConsumer<String, Throwable> onErrorReference;

    public ActivityWindowExecutor(BiConsumer<String, Throwable> errorAction){
        this.onErrorReference = errorAction;
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
