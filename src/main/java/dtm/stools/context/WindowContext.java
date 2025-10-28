package dtm.stools.context;

import dtm.stools.internal.wrapper.ConcurrentWeakReferenceDeque;
import dtm.stools.internal.wrapper.ConcurrentWeakReferenceQueue;

import java.util.Deque;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Classe utilitária para gerenciamento do contexto de janelas (windows) da aplicação.
 * Utiliza uma pilha thread-safe para armazenar e manipular objetos {@link IWindow}.
 */
public final class WindowContext {

    private static final Deque<IWindow> windowContextStack = new ConcurrentWeakReferenceDeque<>();

    private WindowContext() {}

    /**
     * Adiciona uma nova janela ao topo da pilha de contexto.
     *
     * @param window a janela a ser empilhada
     * @param <T> o tipo da janela
     */
    public static <T extends IWindow> void pushWindow(T window) {
        windowContextStack.push(window);
    }

    /**
     * Remove uma janela específica da pilha, independentemente de sua posição.
     *
     * @param window a janela a ser removida
     * @param <T> o tipo da janela
     * @return true se a janela foi removida com sucesso; false caso contrário
     */
    public static <T extends IWindow> boolean removeWindow(T window) {
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
     * @param <T> o tipo da janela
     * @return a janela removida, ou null se a pilha estiver vazia
     */
    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T popWindow() {
        return (T) windowContextStack.pop();
    }

    /**
     * Retorna a janela do topo da pilha sem removê-la.
     *
     * @param <T> o tipo da janela
     * @return a janela no topo, ou null se a pilha estiver vazia
     */
    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T peekWindow() {
        return (T) windowContextStack.peek();
    }

    /**
     * Retorna a janela imediatamente abaixo do topo da pilha (penúltima inserida),
     * sem removê-la.
     *
     * @param <T> o tipo da janela
     * @return a penúltima janela ou null se houver menos de duas janelas na pilha
     */
    @SuppressWarnings("unchecked")
    public static <T extends IWindow> T peekLastWindow() {
        if (windowContextStack.size() < 2) {
            return null;
        }

        var iterator = windowContextStack.iterator();
        iterator.next();

        return (T) iterator.next();
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
     * @param <T> o tipo da janela
     * @return true se a janela foi reanexada com sucesso; false se a janela não está displayable e não pôde ser reanexada
     */
    public static <T extends IWindow> boolean reattachWindow(T window) {
        return reattachWindow(window, 1);
    }

    /**
     * Reanexa uma janela ao contexto em uma posição específica da pilha.
     * O índice 0 representa o topo da pilha, 1 é a posição logo abaixo, e assim por diante.
     * Se o índice for maior que o tamanho da pilha, a janela será inserida na base.
     *
     * @param window a janela a ser reanexada
     * @param index a posição desejada (0 = topo)
     * @param <T> o tipo da janela
     * @return true se a janela foi reanexada com sucesso; false se a janela não está displayable e não pôde ser reanexada
     */
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

    /**
     * Reanexa uma pilha de janelas ao contexto, empilhando todos os elementos na ordem da pilha.
     * O topo da pilha fornecida será o topo da pilha no contexto após a operação.
     *
     * @param windows a pilha de janelas a ser reanexada
     * @param <T> o tipo da janela
     */
    public static <T extends IWindow> void reattachStack(Stack<T> windows) {
        while (!windows.isEmpty()) {
            windowContextStack.push(windows.pop());
        }
    }


    /**
     * Remove e retorna uma pilha de janelas do contexto até encontrar a primeira janela
     * que seja uma instância da classe alvo especificada, incluindo essa janela.
     *
     * Durante o processo, janelas finalizadas (não displayable) são ignoradas e descartadas.
     *
     * Caso a janela alvo não seja encontrada, todas as janelas removidas temporariamente
     * são reempilhadas na ordem original, e uma pilha vazia é retornada.
     *
     * @param <T>        o tipo da janela buscada, que estende {@link IWindow}
     * @param targetClass a classe da janela alvo a ser buscada na pilha
     * @return uma {@link Stack} contendo as janelas desempilhadas, com a janela alvo no topo;
     *         ou uma pilha vazia se a janela alvo não for encontrada
     */
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
