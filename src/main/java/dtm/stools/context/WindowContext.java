package dtm.stools.context;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Classe utilitária para gerenciamento do contexto de janelas (windows) da aplicação.
 * Utiliza uma pilha thread-safe para armazenar e manipular objetos {@link IWindow}.
 */
public final class WindowContext {

    private static final Deque<IWindow> windowContextStack = new ConcurrentLinkedDeque<>();

    private WindowContext() {}

    /**
     * Adiciona uma nova janela ao topo da pilha de contexto.
     *
     * @param window a janela a ser empilhada
     */
    public static void pushWindow(IWindow window) {
        windowContextStack.push(window);
    }

    /**
     * Remove uma janela específica da pilha, independentemente de sua posição.
     *
     * @param window a janela a ser removida
     * @return true se a janela foi removida com sucesso; false caso contrário
     */
    public static boolean removeWindow(IWindow window) {
        return windowContextStack.remove(window);
    }

    /**
     * Retorna todas as janelas atualmente presentes na pilha.
     *
     * @return um {@link Iterable} contendo todas as janelas no contexto
     */
    public static Iterable<IWindow> getWindows() {
        return windowContextStack;
    }

    /**
     * Remove e retorna a janela do topo da pilha (a janela mais recente).
     *
     * @return a janela removida, ou null se a pilha estiver vazia
     */
    public static IWindow popWindow() {
        return windowContextStack.poll();
    }

    /**
     * Retorna a janela do topo da pilha sem removê-la.
     *
     * @return a janela no topo, ou null se a pilha estiver vazia
     */
    public static IWindow peekWindow() {
        return windowContextStack.peek();
    }

    /**
     * Retorna a janela imediatamente abaixo do topo da pilha (penúltima inserida),
     * sem removê-la.
     *
     * @return a penúltima janela ou null se houver menos de duas janelas na pilha
     */
    public static IWindow peekLastWindow() {
        if (windowContextStack.size() < 2) {
            return null;
        }

        var iterator = windowContextStack.iterator();
        iterator.next();

        return iterator.next();
    }

    /**
     * Verifica se a pilha de contexto está vazia.
     *
     * @return true se não houver janelas no contexto; false caso contrário
     */
    public static boolean isEmpty() {
        return windowContextStack.isEmpty();
    }

    /**
     * Retorna a quantidade de janelas atualmente na pilha de contexto.
     *
     * @return o número de janelas empilhadas
     */
    public static int size() {
        return windowContextStack.size();
    }

    /**
     * Remove todas as janelas do contexto.
     */
    public static void clear() {
        windowContextStack.clear();
    }

    /**
     * Reanexa uma janela ao contexto, imediatamente abaixo do topo da pilha.
     * Se a pilha estiver vazia, a janela será adicionada no topo.
     *
     * @param window a janela a ser reanexada
     */
    public static void reattachWindow(IWindow window) {
        reattachWindow(window, 1);
    }

    /**
     * Reanexa uma janela ao contexto em uma posição específica da pilha.
     * O índice 0 representa o topo da pilha, 1 é a posição logo abaixo, e assim por diante.
     * Se o índice for maior que o tamanho da pilha, a janela será inserida na base.
     *
     * @param window a janela a ser reanexada
     * @param index a posição desejada (0 = topo)
     */
    public static void reattachWindow(IWindow window, int index) {
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
    }


}
