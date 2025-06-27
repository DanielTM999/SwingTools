package dtm.stools.activity;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.internal.DomElementLoaderService;
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

public abstract class TransientPopupActivity extends JWindow implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;

    public TransientPopupActivity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            onDrawing();
            this.domElementLoader.load();
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (domElementLoader.isInitialized()) {
            if(!domElementLoader.isLoad())domElementLoader.completeLoad();
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        return domViewer.getOrDefault(id, Collections.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
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

    protected void onDrawing() {
        setupWindow();
        addEvents();
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
    }

}
