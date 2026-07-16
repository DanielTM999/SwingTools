package dtm.stools.context;

import dtm.stools.internal.wrapper.ConcurrentWeakReferenceDeque;
import dtm.stools.internal.wrapper.ConcurrentWeakReferenceQueue;

import java.util.Deque;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class WindowContext {

    private static final Deque<IWindow> windowContextStack = new ConcurrentWeakReferenceDeque<>();

    private WindowContext() {}

    public static <T extends IWindow> void pushWindow(T window) {
        windowContextStack.push(window);
    }

    public static <T extends IWindow> boolean removeWindow(T window) {
        return windowContextStack.remove(window);
    }

    public static Iterable<IWindow> getWindows() {
        return windowContextStack;
    }

    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T popWindow() {
        return (T) windowContextStack.pop();
    }

    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T peekWindow() {
        return (T) windowContextStack.peek();
    }

    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T peekLastWindow() {
        if (windowContextStack.size() < 2) {
            return null;
        }

        var iterator = windowContextStack.iterator();
        iterator.next();

        return (T) iterator.next();
    }

    public static boolean isEmpty() {
        return windowContextStack.isEmpty();
    }

    public static int size() {
        return windowContextStack.size();
    }

    public static void clear() {
        windowContextStack.clear();
    }

    public static <T extends IWindow> boolean reattachWindow(T window) {
        return reattachWindow(window, 1);
    }

    public static <T extends IWindow> boolean reattachWindow(T window, int index) {
        if (!window.isDisplayable()) return false;
        Deque<IWindow> tempStack = new ConcurrentLinkedDeque<>();
        int currentIndex = 0;

        while (!windowContextStack.isEmpty() && currentIndex < index) {
            tempStack.push(windowContextStack.pop());
            currentIndex++;
        }

        windowContextStack.push(window);

        while (!tempStack.isEmpty()) {
            windowContextStack.push(tempStack.pop());
        }

        return true;
    }

    public static <T extends IWindow> void reattachStack(Stack<T> windows) {
        while (!windows.isEmpty()) {
            windowContextStack.push(windows.pop());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends IWindow> Stack<T> popUntilWindow(Class<T> targetClass) {
        Stack<IWindow> tempStack = new Stack<>();
        Stack<T> resultStack = new Stack<>();

        while (!windowContextStack.isEmpty()) {
            IWindow topWindow = windowContextStack.pop();

            if (!topWindow.isDisplayable()) {
                continue;
            }

            tempStack.push(topWindow);

            if (targetClass.isInstance(topWindow)) {
                while (!tempStack.isEmpty()) {
                    IWindow w = tempStack.pop();
                    resultStack.push((T) w);
                }
                return resultStack;
            }
        }

        while (!tempStack.isEmpty()) {
            windowContextStack.push(tempStack.pop());
        }

        return new Stack<>();
    }

}
