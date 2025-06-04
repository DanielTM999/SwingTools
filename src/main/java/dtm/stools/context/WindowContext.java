package dtm.stools.context;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class WindowContext {

    private static final Deque<IWindow> windowContextStack = new ConcurrentLinkedDeque<>();

    private WindowContext() {}

    public static void pushWindow(IWindow window) {
        windowContextStack.push(window);
    }

    public static boolean removeWindow(IWindow window) {
        return windowContextStack.remove(window);
    }

    public static Iterable<IWindow> getWindows() {
        return windowContextStack;
    }

    public static IWindow popWindow() {
        return windowContextStack.poll();
    }

    public static IWindow peekWindow() {
        return windowContextStack.peek();
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

}
