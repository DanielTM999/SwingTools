package dtm.stools.component;

import dtm.stools.context.IWindow;
import dtm.stools.context.IWindowComponent;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public abstract class ViewPanel extends JPanel implements IWindowComponent {

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private Future<Void> loadDomList;

    protected ViewPanel() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        setupHierarchyListener();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        onDrawing();
        reloadDomElements();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        onRemoved();
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

    protected void onDrawing(){
        enableFocusListenerIfFocusable();
    }

    protected void onLoad() {}

    protected void onRemoved() {}

    protected void onLostFocus() {}

    protected void onFocus() {}

    private void setupHierarchyListener() {
        this.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (this.isShowing()) {
                    onLoad();
                } else {
                    onRemoved();
                }
            }
        });
    }

    protected void enableFocusListenerIfFocusable() {
        if(this.isFocusable()){
            this.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    onFocus();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    onLostFocus();
                }
            });
        }
    }

    private Future<Void> loadDomView() {
        return CompletableFuture.runAsync(this::loadThis, executorService);
    }

    private void loadThis() {
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
