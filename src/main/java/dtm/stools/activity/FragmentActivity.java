package dtm.stools.activity;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomElementLoaderService;
import lombok.NonNull;
import lombok.SneakyThrows;
import javax.swing.*;
import java.awt.*;
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

    protected FragmentActivity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, boolean modal){
        super(owner, modal);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, String title, boolean modal){
        super(owner, title, modal);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        if (initialized.compareAndSet(false, true)) {
            onDrawing();
            this.domElementLoader.load();
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

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(WindowEvent e){}

    protected void onClose(WindowEvent e){}

    protected void onLostFocus(WindowEvent e){}

    protected void onFocus(WindowEvent e) {}

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
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

}
