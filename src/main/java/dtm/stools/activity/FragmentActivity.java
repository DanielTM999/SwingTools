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

public abstract class FragmentActivity extends JDialog implements IWindow {

    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private Future<Void> loadDomList;

    protected FragmentActivity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, boolean modal){
        super(owner, modal);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, String title, boolean modal){
        super(owner, title, modal);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        onDrawing();
        loadDomList = loadDomView();
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    @Override
    public void dispose() {
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public <T extends Component> T findById(@NonNull String id) {
        if (loadDomList != null) {
            try(executorService){
                loadDomList.get();
            }
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        List<Component> components = domViewer.get(id);
        if (components != null && !components.isEmpty()) {
            return (T) components.getFirst();
        }

        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
    }

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(){}

    protected void onClose(){}

    protected void onLostFocus(){}

    private void addEvents(){
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                onLoad();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                onLostFocus();
            }

        });
    }

    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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
