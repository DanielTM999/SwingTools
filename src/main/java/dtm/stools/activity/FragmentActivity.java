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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class FragmentActivity extends JDialog implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final WindowExecutor windowExecutor;

    {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
            Thread t = new Thread(r);
            t.setName("FragmentActivity-Main-Worker-" + System.identityHashCode(this));
            t.setDaemon(true);
            return t;
        });
    }

    protected FragmentActivity(){
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    protected FragmentActivity(JFrame owner, boolean modal){
        super(owner, modal);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    protected FragmentActivity(JFrame owner, String title, boolean modal){
        super(owner, title, modal);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    @Override
    public void init(){
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

    protected void onResize() {}

    protected void onMove() {}

    protected void onShow() {}

    protected void onHidden() {}

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
        setSize(800, 600);
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

}
