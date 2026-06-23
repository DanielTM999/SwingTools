package dtm.stools.component;

import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.IWindowComponent;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomComponentElementLoaderService;
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
public abstract class ViewPanel extends JPanel implements IWindowComponent {
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
    }

    protected ViewPanel(LayoutManager layout){
        super(layout);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.domElementLoader = new DomComponentElementLoaderService<>(this, domViewer, executorService);
        setupHierarchyListener();
    }

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

    /**
     * Armazena um valor no contexto da janela.
     *
     * @param key Chave de identificação.
     * @param value Valor a ser armazenado.
     * @return {@code true} se a chave ainda não existia.
     */
    @Override
    public boolean putInClient(String key, Object value) {
        return putInClient(key, value, false);
    }

    /**
     * Armazena um valor no contexto da janela com opção de sobrescrever.
     *
     * @param key Chave de identificação.
     * @param value Valor a ser armazenado.
     * @param replace Se {@code true}, sobrescreve o valor existente.
     * @return {@code true} se a chave foi adicionada ou sobrescrita.
     */
    @Override
    public boolean putInClient(String key, Object value, boolean replace) {
        if (replace) {
            clientSideElements.put(key, value);
            return true;
        }else{
            return clientSideElements.putIfAbsent(key, value) == null;
        }
    }

    /**
     * Recupera um valor armazenado no contexto da janela.
     *
     * @param key Chave de identificação.
     * @param <T> Tipo esperado do valor.
     * @return Valor associado ou {@code null} se não encontrado.
     */
    @Override
    public <T> T getFromClient(String key) {
        return getFromClient(key, null);
    }

    /**
     * Recupera um valor do contexto com valor padrão.
     *
     * @param key Chave de identificação.
     * @param defaultValue Valor padrão se a chave não existir.
     * @param <T> Tipo esperado do valor.
     * @return Valor armazenado ou valor padrão.
     */
    @Override
    public <T> T getFromClient(String key, T defaultValue) {
        final Object value = clientSideElements.getOrDefault(key, defaultValue);
        try{
            return (T)value;
        }catch (Exception e){
            throw new InvalidClientSideElementException(key, value, e);
        }
    }

    /**
     * Responsável por desenhar os elementos do painel.
     * Chamado automaticamente quando o componente é adicionado à hierarquia.
     * Pode ser sobrescrito para configurar a UI do painel.
     */
    protected void onDrawing(){
        enableFocusListenerIfFocusable();
        enableClickListener();
    }

    /**
     * Evento chamado quando o painel se torna visível na hierarquia.
     * Pode ser sobrescrito para executar lógica ao exibir o painel.
     */
    protected void onLoad() {}

    /**
     * Evento chamado quando o painel é removido da hierarquia.
     * Pode ser sobrescrito para liberar recursos ou executar lógica de limpeza.
     */
    protected void onRemoved() {}

    /**
     * Evento chamado quando o painel perde o foco.
     * Só é disparado se o painel for focável.
     *
     * @param e Evento de foco.
     */
    protected void onLostFocus(FocusEvent e) {}

    /**
     * Evento chamado quando o painel ganha o foco.
     * Só é disparado se o painel for focável.
     *
     * @param e Evento de foco.
     */
    protected void onFocus(FocusEvent e) {}

    /**
     * Evento chamado quando o painel recebe um clique do mouse.
     *
     * @param event Evento de mouse.
     */
    protected void onClick(MouseEvent event) {}

    /**
     * Evento chamado quando o painel é redimensionado.
     * Pode ser sobrescrito para reagir a mudanças de tamanho.
     */
    protected void onResize() {}

    /**
     * Evento chamado quando o painel é movido para outra posição.
     * Pode ser sobrescrito para reagir a mudanças de posição.
     */
    protected void onMove() {}

    /**
     * Evento chamado quando o painel se torna visível.
     * Pode ser sobrescrito para executar lógica ao exibir o painel.
     */
    protected void onShow() {}

    /**
     * Evento chamado quando o painel é ocultado.
     * Pode ser sobrescrito para executar lógica ao esconder o painel.
     */
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
