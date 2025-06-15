package dtm.stools.activity;

import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
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

public abstract class TransientPopupActivity extends JWindow implements IWindow {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private Future<Void> loadDomList;

    public TransientPopupActivity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            onDrawing();
            loadDomList = loadDomView();
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (loadDomList != null) {
            loadDomList.get();
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
        loadDomList = loadDomView();
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdownNow();
        super.dispose();
    }

    protected void onDrawing() {
        setupWindow();
        addEvents();
    }

    protected void onLoad(WindowEvent e){}

    protected void onClose(WindowEvent e){}

    protected void onLostFocus(WindowEvent e){}

    protected void onFocus(WindowEvent e) {}

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

    private Future<Void> loadDomView(){
        return CompletableFuture.runAsync(this::loadThis, executorService);
    }

    private void loadThis(){
        List<Component> rootList = this.domViewer.computeIfAbsent("root", k ->
                Collections.synchronizedList(new ArrayList<>())
        );
        rootList.add(this);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Component component : this.getComponents()) {
            futures.add(CompletableFuture.runAsync(() ->
                            collectComponentsRecursive(component)
                    , executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    private void collectComponentsRecursive(Component component) {
        if (component == null) return;

        String name = component.getName();
        if (name != null && !name.isBlank()) {
            domViewer
                    .computeIfAbsent(name, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(component);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponentsRecursive(child);
            }
        }
    }


}
