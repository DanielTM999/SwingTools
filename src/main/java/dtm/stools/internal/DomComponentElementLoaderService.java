package dtm.stools.internal;

import dtm.stools.context.DomElementLoader;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DomComponentElementLoaderService<T extends JComponent> implements DomElementLoader {
    private final ExecutorService executorService;
    private final AtomicBoolean initialized;
    private final Map<String, List<Component>> domViewer;
    private final T jComponent;
    private Future<Void> loadDomList;

    public DomComponentElementLoaderService(T jComponent, Map<String, List<Component>> domMap, ExecutorService executorService){
        this.initialized = new AtomicBoolean(false);
        this.domViewer = domMap;
        this.executorService = executorService;
        this.jComponent = jComponent;
    }


    @Override
    public void load() {
        this.loadDomList = loadDomView();
    }

    @Override
    public void reload() {
        this.initialized.set(false);
        domViewer.clear();
        this.loadDomList = loadDomView();
    }

    @SneakyThrows
    @Override
    public void completeLoad() {
        if(initialized.compareAndSet(false, true)){
            loadDomList.get();
        }
    }

    @Override
    public boolean isLoad() {
        return initialized.get();
    }

    @Override
    public boolean isInitialized() {
        return this.loadDomList != null;
    }

    @Override
    public Map<String, List<Component>> getDomElements() {
        return domViewer;
    }

    @Override
    public Future<Void> getLoadAction() {
        return loadDomList;
    }

    private Future<Void> loadDomView(){
        if (executorService.isShutdown() || executorService.isTerminated()) {
            throw new IllegalStateException("ExecutorService já foi desligado");
        }
        return CompletableFuture.runAsync(this::loadThis, executorService);
    }

    private void loadThis(){
        List<Component> rootList = this.domViewer.computeIfAbsent("root", k ->
                Collections.synchronizedList(new ArrayList<>())
        );
        rootList.add(jComponent);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Component component : jComponent.getComponents()) {
            futures.add(CompletableFuture.runAsync(() -> collectComponentsRecursive(component), executorService));
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
