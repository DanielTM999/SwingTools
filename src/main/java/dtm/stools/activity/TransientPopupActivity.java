package dtm.stools.activity;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.context.WindowExecutor;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomElementLoaderService;
import dtm.stools.internal.window.ActivityWindowExecutor;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class TransientPopupActivity extends JWindow implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final WindowExecutor windowExecutor;

    public TransientPopupActivity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            onDrawing();
            this.domElementLoader.load();
            addEvents();
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (domElementLoader.isInitialized()) {
            if(!domElementLoader.isLoad())domElementLoader.completeLoad();
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        return domViewer.getOrDefault(id, Collections.EMPTY_LIST);
    }

    @Override
    public <T extends Component> T findById(@NonNull String id) {
        List<Component> components = findAllById(id);

        if(!components.isEmpty()){
            return (T)components.getFirst();
        }

        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
    }

    @Override
    public void reloadDomElements() {
        domElementLoader.reload();
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdownNow();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @Override
    public boolean putInClient(String key, Object value) {
        return putInClient(key, value, false);
    }

    @Override
    public boolean putInClient(String key, Object value, boolean replace) {
        if (replace) {
            clientSideElements.put(key, value);
            return true;
        }else{
            return clientSideElements.putIfAbsent(key, value) == null;
        }
    }

    @Override
    public <T> T getFromClient(String key) {
        return getFromClient(key, null);
    }

    @Override
    public <T> T getFromClient(String key, T defaultValue) {
        final Object value = clientSideElements.getOrDefault(key, defaultValue);
        try{
            return (T)value;
        }catch (Exception e){
            throw new InvalidClientSideElementException(key, value, e);
        }
    }

    @Override
    public WindowExecutor getWindowExecutor() {
        return windowExecutor;
    }

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(WindowEvent e){}

    protected void onClose(WindowEvent e){}

    protected void onLostFocus(WindowEvent e){}

    protected void onFocus(WindowEvent e) {}

    /**
     * Manipula erros ocorridos na atividade.
     * Deve ser sobrescrito para tratamento personalizado de erros relacionados ao ciclo de vida da janela.
     *
     * <p>Por padrão, este método apenas relança a exceção recebida.
     * Caso o desenvolvedor deseje adicionar lógica personalizada (como logging, notificações ou integração com serviços externos),
     * isso pode ser feito sobrescrevendo este método.
     * Se não tratado, o erro será relançado para o fluxo superior.</p>
     *
     * <strong>Nota:</strong> Este método trata exclusivamente falhas internas da estrutura da aplicação,
     * como erros durante {@code init()}, {@code onDrawing()}, {@code dispose()}, entre outros
     * pontos do ciclo de vida da {@code Activity}.
     * <br>
     * <strong>Ele <u>não</u> captura exceções lançadas por componentes adicionados pelo desenvolvedor</strong>,
     * como listeners personalizados, ações em botões ou lógicas arbitrárias associadas à interface gráfica.
     * <br>
     * Para esses casos, é responsabilidade do desenvolvedor implementar o tratamento local dessas exceções.
     *
     * @implNote Este método não cobre exceções geradas por interações arbitrárias definidas pelo usuário.
     * Tais exceções devem ser tratadas diretamente onde ocorrem ou encapsuladas em um mecanismo específico de captura.
     *
     * @param action Ação identificadora onde o erro ocorreu.
     * @param error  Exceção ou erro capturado.
     */
    protected void onError(String action, Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            String className = getClass().getName();
            String message = "Unhandled exception in [" + className + "] during action [" + action + "]";
            throw new RuntimeException(message, error);
        }
    }

    protected ExecutorService getMainExecutor(){
        return executorService;
    }

    protected CompletableFuture<?> runOnWindowExecutor(Runnable command){
        return CompletableFuture.runAsync(command, executorService);
    }

    protected <T> CompletableFuture<T> runOnWindowExecutor(Supplier<T> command){
        return CompletableFuture.supplyAsync(command, executorService);
    }

    private void addEvents(){
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose(e);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                onLoad(e);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                onLostFocus(e);
            }
            @Override
            public void windowGainedFocus(WindowEvent e) {
                onFocus(e);
            }
        });
    }

    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

}
