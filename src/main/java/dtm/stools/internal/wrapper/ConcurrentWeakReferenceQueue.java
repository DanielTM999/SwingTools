package dtm.stools.internal.wrapper;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConcurrentWeakReferenceQueue<T> implements Queue<T> {

    protected final Deque<WeakReference<T>> weakReferences = new ConcurrentLinkedDeque<>();

    @Override
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        weakReferences.forEach(ref -> {
            T element = unwrap(ref);
            if (element != null) {
                action.accept(element);
            }
        });
    }

    @Override
    public int size() {
        cleanup();
        return weakReferences.size();
    }

    @Override
    public boolean isEmpty() {
        cleanup();
        return weakReferences.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) return false;

        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null && element.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        List<T> elements = new ArrayList<>();
        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.iterator();
    }

    @Override
    public Object[] toArray() {
        cleanup();
        List<T> elements = new ArrayList<>();
        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        cleanup();
        List<T> elements = new ArrayList<>();
        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.toArray(a);
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return toArray(generator.apply(0));
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;

        return weakReferences.removeIf(ref -> {
            T element = unwrap(ref);
            return element == null || element.equals(o);
        });
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object element : c) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;
        for (T element : c) {
            if (offer(element)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object element : c) {
            while (remove(element)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        Objects.requireNonNull(filter);
        return weakReferences.removeIf(ref -> {
            T element = unwrap(ref);
            return element == null || filter.test(element);
        });
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return weakReferences.removeIf(ref -> {
            T element = unwrap(ref);
            return element == null || !c.contains(element);
        });
    }

    @Override
    public void clear() {
        weakReferences.clear();
    }

    @Override
    public Spliterator<T> spliterator() {
        List<T> elements = new ArrayList<>();
        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public Stream<T> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public boolean add(T t) {
        if (t == null)  return false;
        return weakReferences.add(new WeakReference<>(t));
    }

    @Override
    public boolean offer(T t) {
        if (t == null) return false;
        return weakReferences.offer(new WeakReference<>(t));
    }

    @Override
    public T remove() {
        while (!weakReferences.isEmpty()) {
            WeakReference<T> ref = weakReferences.remove();
            T element = unwrap(ref);
            if (element != null) {
                return element;
            }
        }
        throw new NoSuchElementException("Queue is empty");
    }

    @Override
    public T poll() {
        while (!weakReferences.isEmpty()) {
            WeakReference<T> ref = weakReferences.poll();
            T element = unwrap(ref);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    @Override
    public T element() {
        T element = peek();
        if (element == null) {
            throw new NoSuchElementException("Queue is empty");
        }
        return element;
    }

    @Override
    public T peek() {
        while (!weakReferences.isEmpty()) {
            WeakReference<T> ref = weakReferences.peek();
            T element = unwrap(ref);
            if (element != null) {
                return element;
            }
            weakReferences.poll();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }


    protected void cleanup() {
        weakReferences.removeIf(ref -> ref.get() == null);
    }

    protected T unwrap(WeakReference<T> ref) {
        return ref != null ? ref.get() : null;
    }

}
