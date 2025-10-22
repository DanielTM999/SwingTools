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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
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
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    protected DialogActivity() {
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
    }

    protected DialogActivity(Frame frame) {
        super(frame);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
    }

    protected DialogActivity(Frame frame, String title) {
        super(frame, title);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
        addEvents();
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            onDrawing();
            this.domElementLoader.load();
            addEvents();
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdownNow();
        WindowContext.removeWindow(this);
        super.dispose();
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
    }

    private void setupWindow() {
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
}
