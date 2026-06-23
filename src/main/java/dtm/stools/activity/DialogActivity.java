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
import java.awt.event.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class DialogActivity extends JDialog implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final WindowExecutor windowExecutor;

    {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
            Thread t = new Thread(r);
            t.setName("DialogActivity-Main-Worker-" + System.identityHashCode(this));
            t.setDaemon(true);
            return t;
        });
    }

    protected DialogActivity() {
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    protected DialogActivity(Window frame) {
        super(frame);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    protected DialogActivity(Window frame, String title) {
        super(frame, title);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
        addEvents();
    }

    protected DialogActivity(Window frame, String title, ModalityType modalityType) {
        super(frame, title, modalityType);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
        addEvents();
    }

    @Override
    public void init() {
        windowExecutor.execute(() -> {
            if (executorService.isShutdown()) return;

            if (initialized.compareAndSet(false, true)) {
                try {
                    onDrawing();

                    if (!executorService.isShutdown()) {
                        try {
                            this.domElementLoader.load();
                        } catch (IllegalStateException | RejectedExecutionException e) {
                            if (executorService.isShutdown()) return;
                            throw e;
                        }
                    }

                    addEvents();

                    if (!executorService.isShutdown()) {
                        SwingUtilities.invokeLater(() -> {
                            if (!executorService.isShutdown()) setVisible(true);
                        });
                    }
                } catch (Exception e) {
                    if (!executorService.isShutdown()) {
                        onError("init", e);
                    }
                }
            }
        }, "init");
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdown();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @Override
    public void requestClose() {
        WindowEvent event = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
        windowExecutor.execute(() -> onClose(event), "onClose");
    }

    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (domElementLoader.isInitialized()) {
            if(!domElementLoader.isLoad())domElementLoader.completeLoad();
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        return domViewer.getOrDefault(id, Collections.emptyList());
    }

    @Override
    public <T extends Component> T findById(@NonNull String id) {
        List<Component> components = findAllById(id);
        if (!components.isEmpty()) return (T) components.getFirst();
        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
    }

    @Override
    public void reloadDomElements() {
        domElementLoader.reload();
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

    public boolean setPseudoOwner(Frame owner){
        try{
            this.setLocationRelativeTo(owner);
            owner.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    DialogActivity.this.dispose();
                }
            });

            owner.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    DialogActivity.this.toFront();
                }
                @Override
                public void windowLostFocus(WindowEvent e) {

                }
            });

            return true;
        }catch (Exception e){
            return false;
        }
    }

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(WindowEvent e) throws Exception {}

    protected void onClose(WindowEvent e) throws Exception {}

    protected void onLostFocus(WindowEvent e) throws Exception {}

    protected void onFocus(WindowEvent e) throws Exception {}


    /**
     * Evento chamado quando a janela é redimensionada.
     * Pode ser sobrescrito para reagir a mudanças de tamanho.
     */
    protected void onResize() {}

    /**
     * Evento chamado quando a janela é movida para outra posição na tela.
     * Pode ser sobrescrito para reagir a mudanças de posição.
     */
    protected void onMove() {}

    /**
     * Evento chamado quando a janela se torna visível.
     * Pode ser sobrescrito para executar lógica ao exibir a janela.
     */
    protected void onShow() {}

    /**
     * Evento chamado quando a janela é ocultada.
     * Pode ser sobrescrito para executar lógica ao esconder a janela.
     */
    protected void onHidden() {}

    /**
     * Manipula erros ocorridos na atividade.
     * Deve ser sobrescrito para tratamento personalizado.
     *
     * <p>Por padrão, relança a exceção recebida.
     *
     * @param error Exceção ou erro capturado.
     */
    protected void onError(String action, Throwable error){
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            throw new RuntimeException("Unhandled exception in {" + getClass() + "}", error);
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

    private void addEvents() {
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                windowExecutor.execute(() -> onClose(e), "onClose");
            }

            @Override
            public void windowOpened(WindowEvent e) {
                windowExecutor.execute(() -> onLoad(e), "onLoad");
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                windowExecutor.execute(() -> onLostFocus(e), "onLostFocus");
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                windowExecutor.execute(() -> onFocus(e), "onFocus");
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                windowExecutor.execute(() -> onResize(), "onResize");
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                windowExecutor.execute(() -> onMove(), "onMove");
            }

            @Override
            public void componentShown(ComponentEvent e) {
                windowExecutor.execute(() -> onShow(), "onShow");
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                windowExecutor.execute(() -> onHidden(), "onHidden");
            }
        });
    }

    private void setupWindow() {
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
}
