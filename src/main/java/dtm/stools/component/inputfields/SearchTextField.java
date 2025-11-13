package dtm.stools.component.inputfields;

import dtm.stools.component.events.EventType;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class SearchTextField<T> extends MaskedTextField{

    private final List<T> dataSources;
    private final List<Function<T, String>> searchOptions;
    private final JList<T> suggestionList;
    private final AtomicInteger minLength;
    private final AtomicInteger maxResults;
    private final AtomicReference<ExecutorService> executorRef;
    private final AtomicInteger searchId;
    private final AtomicBoolean showPopupEnable;
    private Function<T, String> displayFunction = Object::toString;
    private BiPredicate<String, String> searchStrategy = (text, value) -> value.toLowerCase().contains(text.toLowerCase());

    @Setter
    private Comparator<T> sortComparator = null;

    @Setter
    private Consumer<T> onSelectListener = null;
    private JPopupMenu suggestionPopup;
    private boolean selectingFromList = false;
    private boolean caseSensitive = false;

    @Setter
    private boolean autoCloseOnFocusLost = true;

    private int popupHeight = 120;

    /**
     * Cria um novo campo de texto sem máscara.
     */
    public SearchTextField() {
        this(null, '_');
    }

    /**
     * Cria um novo campo de texto com máscara.
     *
     * @param mask A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     */
    public SearchTextField(String mask) {
        this(mask, '_');
    }

    public SearchTextField(String mask, char placeholder){
        super(mask, placeholder);
        this.searchId = new AtomicInteger(0);
        this.minLength = new AtomicInteger(2);
        this.maxResults = new AtomicInteger(50);
        this.showPopupEnable = new AtomicBoolean(true);
        this.executorRef = new AtomicReference<>(Executors.newSingleThreadExecutor());
        this.dataSources = Collections.synchronizedList(new ArrayList<>());
        this.searchOptions = new CopyOnWriteArrayList<>();

        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFocusable(false);

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    T selected = suggestionList.getModel().getElementAt(index);
                    selectSuggestion(selected);
                }
            }
        });

        suggestionList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = suggestionList.locationToIndex(e.getPoint());
                if (index >= 0 && index != suggestionList.getSelectedIndex()) {
                    suggestionList.setSelectedIndex(index);
                    suggestionList.ensureIndexIsVisible(index);
                }
            }
        });

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setFocusable(false);
        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setFocusable(false);
        suggestionPopup.add(scrollPane);

        addEventListner(EventType.INPUT, e -> {
            if (selectingFromList) return;

            String text = getText().trim();
            if (text.length() >= minLength.get() && this.showPopupEnable.get()) {
                int currentSearch = searchId.incrementAndGet();
                submitTask(() -> {
                    List<T> results = search(text);
                    if (currentSearch == searchId.get()) {
                        SwingUtilities.invokeLater(() -> showSuggestions(results));
                    }
                });
            } else {
                suggestionPopup.setVisible(false);
            }
        });

        addKeyListenerSelect();
        addFocusListenerAutoClose();
    }

    /** Define a lista base de objetos para busca */
    public void setDataSource(List<T> data) {
        dataSources.clear();
        if (data != null) {
            dataSources.addAll(data);
        }
    }

    /** Adiciona uma opção de busca (ex: obj -> ((Cliente)obj).getNome()) */
    public void addSearchOption(Function<T, String> fn) {
        if (fn != null) {
            searchOptions.add(fn);
        }
    }

    /** Define o número mínimo de caracteres para iniciar a busca */
    public void setMinLength(int length) {
        if (length < 1) length = 1;
        minLength.set(length);
    }

    /** Retorna o valor atual do minLength */
    public int getMinLength() {
        return minLength.get();
    }

    /** Define o número máximo de resultados a exibir */
    public void setMaxResults(int max) {
        if (max < 1) max = 10;
        maxResults.set(max);
    }

    /** Retorna o valor atual do maxResults */
    public int getMaxResults() {
        return maxResults.get();
    }

    /** Define a função de exibição dos itens */
    public void setDisplayFunction(Function<T, String> fn) {
        if (fn != null) {
            displayFunction = fn;
        }
    }

    /** Define uma estratégia customizada de busca (ex: busca por início, regex, etc) */
    public void setSearchStrategy(BiPredicate<String, String> strategy) {
        if (strategy != null) {
            this.searchStrategy = strategy;
        }
    }

    /** Usa estratégia de busca que só encontra itens que começam com o texto */
    public void useStartsWithSearch() {
        this.searchStrategy = (text, value) ->
                value.toLowerCase().startsWith(text.toLowerCase());
    }

    /** Usa estratégia de busca exata (igual) */
    public void useExactSearch() {
        this.searchStrategy = (text, value) ->
                value.equalsIgnoreCase(text);
    }

    /** Define se a busca é case sensitive */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        if (caseSensitive) {
            this.searchStrategy = (text, value) -> value.contains(text);
        }
    }

    /** Ordena resultados alfabeticamente pela função de exibição */
    public void sortResultsAlphabetically() {
        this.sortComparator = Comparator.comparing(displayFunction);
    }

    /** Define a altura do popup de sugestões */
    public void setPopupHeight(int height) {
        if (height > 0) {
            this.popupHeight = height;
        }
    }

    /** Retorna o item atualmente selecionado na lista (ou null) */
    public T getSelectedSuggestion() {
        return suggestionList.getSelectedValue();
    }

    /** Força a abertura do popup com todos os resultados */
    public void showAllSuggestions() {
        submitTask(() -> {
            List<T> results = getAllResults();
            SwingUtilities.invokeLater(() -> showSuggestions(results));
        });
    }

    /** Limpa o campo e fecha o popup */
    public void clearAndClose() {
        setText("");
        suggestionPopup.setVisible(false);
    }

    /** Fecha o popup de sugestões */
    public void closePopup() {
        suggestionPopup.setVisible(false);
    }

    public void setShowPopup(boolean show){
        this.showPopupEnable.set(show);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        ExecutorService executor = executorRef.getAndSet(null);
        if (executor != null) {
            executor.shutdown();
        }
    }

    private List<T> search(String text) {
        if (text == null || text.isEmpty() || dataSources.isEmpty()) return Collections.emptyList();

        String searchText = caseSensitive ? text : text.toLowerCase();
        List<T> results = new ArrayList<>();

        synchronized (dataSources) {
            for (T item : dataSources) {
                if (results.size() >= maxResults.get()) break;

                for (Function<T, String> fn : searchOptions) {
                    String value = fn.apply(item);
                    if (value != null) {
                        String compareValue = caseSensitive ? value : value.toLowerCase();
                        if (searchStrategy.test(searchText, compareValue)) {
                            results.add(item);
                            break;
                        }
                    }
                }
            }
        }

        if (sortComparator != null) {
            results.sort(sortComparator);
        }

        return results;
    }

    private List<T> getAllResults() {
        List<T> results = new ArrayList<>();
        synchronized (dataSources) {
            results.addAll(dataSources);
        }
        if (sortComparator != null) {
            results.sort(sortComparator);
        }
        int limit = maxResults.get();
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    private void showSuggestions(List<T> results) {
        if (results == null || results.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        DefaultListModel<T> model = new DefaultListModel<>();
        for (T r : results) model.addElement(r);
        suggestionList.setModel(model);
        suggestionList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(displayFunction.apply(value));
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return label;
        });

        JScrollPane scroll = (JScrollPane) suggestionPopup.getComponent(0);
        scroll.setPreferredSize(new java.awt.Dimension(getWidth(), popupHeight));

        if (!suggestionPopup.isVisible()) {
            suggestionPopup.show(this, 0, getHeight());
            requestFocusInWindow();
        } else {
            suggestionPopup.pack();
        }

        SwingUtilities.invokeLater(suggestionList::repaint);
    }

    private void selectSuggestion(T selected) {
        if (selected == null) return;

        selectingFromList = true;
        try {
            setText(displayFunction.apply(selected));
            suggestionPopup.setVisible(false);

            if (onSelectListener != null) {
                onSelectListener.accept(selected);
            }
        } finally {
            SwingUtilities.invokeLater(() -> selectingFromList = false);
        }
    }

    private void addKeyListenerSelect(){
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionPopup.isVisible()) return;

                int index = suggestionList.getSelectedIndex();
                int size = suggestionList.getModel().getSize();

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        if (size == 0) return;
                        if (index < 0) index = 0;
                        else if (index < size - 1) index++;
                        suggestionList.setSelectedIndex(index);
                        suggestionList.ensureIndexIsVisible(index);
                        e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        if (size == 0) return;
                        if (index > 0) index--;
                        else index = 0;
                        suggestionList.setSelectedIndex(index);
                        suggestionList.ensureIndexIsVisible(index);
                        e.consume();
                    }
                    case KeyEvent.VK_ENTER -> {
                        T selected = suggestionList.getSelectedValue();
                        if (selected != null) {
                            selectSuggestion(selected);
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        suggestionPopup.setVisible(false);
                        e.consume();
                    }
                }
            }
        });
    }

    private void addFocusListenerAutoClose() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (autoCloseOnFocusLost && !e.isTemporary()) {
                    SwingUtilities.invokeLater(() -> suggestionPopup.setVisible(false));
                }
            }
        });
    }

    private ExecutorService getMainExecutor(){
        while (true) {
            ExecutorService executor = executorRef.get();

            if (executor == null || executor.isShutdown() || executor.isTerminated()) {
                ExecutorService newExecutor = Executors.newSingleThreadExecutor();
                if (executorRef.compareAndSet(executor, newExecutor)) {
                    executor = newExecutor;
                } else {
                    newExecutor.shutdown();
                    continue;
                }
            }

            return executor;
        }
    }

    private void submitTask(Runnable task) {
        ExecutorService executor = getMainExecutor();
        try {
            executor.submit(task);
        } catch (RejectedExecutionException e) {
            submitTask(task);
        }
    }


}