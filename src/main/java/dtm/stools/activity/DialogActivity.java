package dtm.stools.activity;


import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public abstract class DialogActivity extends JDialog implements IWindow {
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private Future<Void> loadDomList;

    protected DialogActivity() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected DialogActivity(Frame frame) {
        super(frame);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected DialogActivity(Frame frame, String title) {
        super(frame, title);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            onDrawing();
            loadDomList = loadDomView();
            SwingUtilities.invokeLater(() -> setVisible(true));
        }
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdownNow();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @SneakyThrows
    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (loadDomList != null) {
            loadDomList.get();
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        return domViewer.getOrDefault(id, Collections.emptyList());
    }

    @SneakyThrows
    @Override
    public <T extends Component> T findById(@NonNull String id) {
        List<Component> components = findAllById(id);
        if (!components.isEmpty()) return (T) components.getFirst();
        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
    }

    @Override
    public void reloadDomElements() {
        loadDomList = loadDomView();
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

    protected void onLoad() {}

    protected void onClose() {}

    protected void onLostFocus() {}

    protected void onError(Throwable error) {}

    private void addEvents() {
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onClose(); }
            @Override public void windowOpened(WindowEvent e) { onLoad(); }
            @Override public void windowLostFocus(WindowEvent e) { onLostFocus(); }
        });
    }

    private void setupWindow() {
        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
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
