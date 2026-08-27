package dtm.stools.component;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindowComponent;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomComponentElementLoaderService;
import dtm.stools.theme.ThemeAware;
import lombok.NonNull;
import lombok.SneakyThrows;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
public abstract class ViewPanel extends JPanel implements IWindowComponent, ThemeAware {
    private boolean themeHookReady;
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final Map<String, Object> clientSideElements;

    protected ViewPanel() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomComponentElementLoaderService<>(this, domViewer, executorService);
        setupHierarchyListener();
        this.themeHookReady = true;
    }

    protected ViewPanel(LayoutManager layout){
        super(layout);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomComponentElementLoaderService<>(this, domViewer, executorService);
        setupHierarchyListener();
        this.themeHookReady = true;
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (!themeHookReady) {
            return;
        }

        applyTheme();
    }

    @Override
    public final void applyTheme() {
        try {
            onThemeChanged();
        } catch (Exception ignored) {
        }
    }

    protected void onThemeChanged() {}

    @Override
    public void addNotify() {
        super.addNotify();
        onDrawing();
        reloadDomElements();
        onInit();
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
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

    protected void onDrawing(){
        enableFocusListenerIfFocusable();
        enableClickListener();
    }

    protected void onLoad() {}

    protected void onRemoved() {}

    protected void onLostFocus(FocusEvent e) {}

    protected void onFocus(FocusEvent e) {}

    protected void onClick(MouseEvent event) {}

    protected void onResize() {}

    protected void onMove() {}

    protected void onShow() {}

    protected void onHidden() {}

    protected void onInit() {}

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

        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) { onResize(); }
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e)   { onMove();   }
            @Override
            public void componentShown(java.awt.event.ComponentEvent e)   { onShow();   }
            @Override
            public void componentHidden(java.awt.event.ComponentEvent e)  { onHidden(); }
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
