package dtm.stools.models;

public interface ThrowableSupplier<T> {
    T run() throws Throwable;
}
