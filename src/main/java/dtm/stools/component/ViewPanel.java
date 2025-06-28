package dtm.stools.component;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.IWindowComponent;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.internal.DomComponentElementLoaderService;
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
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;

    protected ViewPanel() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.domElementLoader = new DomComponentElementLoaderService<>(this, domViewer, executorService);
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
        if (domElementLoader.isInitialized()) {
            if(!domElementLoader.isLoad())domElementLoader.completeLoad();
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
        domElementLoader.reload();
    }

    protected void onDrawing(){
        enableFocusListenerIfFocusable();
        enableClickListener();
    }

    protected void onLoad() {}

    protected void onRemoved() {}

    protected void onLostFocus(FocusEvent e) {}

    protected void onFocus(FocusEvent e) {}

    protected void onClick(java.awt.event.MouseEvent event){}

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

    protected void enableClickListener() {
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                onClick(e);
            }
        });
    }

    protected void enableFocusListenerIfFocusable() {
        if(this.isFocusable()){
            this.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    onFocus(e);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    onLostFocus(e);
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
