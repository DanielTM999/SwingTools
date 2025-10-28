package dtm.stools.internal.wrapper;

import java.lang.ref.WeakReference;
import java.util.*;

public class ConcurrentWeakReferenceDeque<T> extends ConcurrentWeakReferenceQueue<T> implements Deque<T> {

    @Override
    public void addFirst(T t) {
        if (t != null) {
            weakReferences.addFirst(new WeakReference<>(t));
        }
    }

    @Override
    public void addLast(T t) {
        if (t != null) {
            weakReferences.addLast(new WeakReference<>(t));
        }
    }

    @Override
    public boolean offerFirst(T t) {
        if (t == null) return false;
        weakReferences.offerFirst(new WeakReference<>(t));
        return true;
    }

    @Override
    public boolean offerLast(T t) {
        if (t == null) return false;
        weakReferences.offerLast(new WeakReference<>(t));
        return true;
    }

    @Override
    public T removeFirst() {
        T element = pollFirst();
        if (element == null)
            throw new NoSuchElementException("Deque is empty");
        return element;
    }

    @Override
    public T removeLast() {
        T element = pollLast();
        if (element == null)
            throw new NoSuchElementException("Deque is empty");
        return element;
    }

    @Override
    public T pollFirst() {
        while (!weakReferences.isEmpty()) {
            WeakReference<T> ref = weakReferences.pollFirst();
            T element = unwrap(ref);
            if (element != null) return element;
        }
        return null;
    }

    @Override
    public T pollLast() {
        while (!weakReferences.isEmpty()) {
            WeakReference<T> ref = weakReferences.pollLast();
            T element = unwrap(ref);
            if (element != null) return element;
        }
        return null;
    }

    @Override
    public T getFirst() {
        T element = peekFirst();
        if (element == null)
            throw new NoSuchElementException("Deque is empty");
        return element;
    }

    @Override
    public T getLast() {
        T element = peekLast();
        if (element == null)
            throw new NoSuchElementException("Deque is empty");
        return element;
    }

    @Override
    public T peekFirst() {
        cleanup();
        for (WeakReference<T> ref : weakReferences) {
            T element = unwrap(ref);
            if (element != null)
                return element;
        }
        return null;
    }

    @Override
    public T peekLast() {
        cleanup();
        Iterator<WeakReference<T>> descending = weakReferences.descendingIterator();
        while (descending.hasNext()) {
            T element = unwrap(descending.next());
            if (element != null)
                return element;
        }
        return null;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) return false;
        for (Iterator<WeakReference<T>> it = weakReferences.iterator(); it.hasNext();) {
            T element = unwrap(it.next());
            if (o.equals(element)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) return false;
        Iterator<WeakReference<T>> it = weakReferences.descendingIterator();
        while (it.hasNext()) {
            T element = unwrap(it.next());
            if (o.equals(element)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public void push(T t) {
        addFirst(t);
    }

    @Override
    public T pop() {
        return removeFirst();
    }

    @Override
    public Iterator<T> descendingIterator() {
        cleanup();
        List<T> elements = new ArrayList<>();
        Iterator<WeakReference<T>> it = weakReferences.descendingIterator();
        while (it.hasNext()) {
            T element = unwrap(it.next());
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.iterator();
    }

}
