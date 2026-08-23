package dtm.stools.component.inputfields.duallistfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import dtm.stools.layouts.FlexBoxLayout;

/**
 * Campo de transferência com duas listas e botões centrais para mover itens entre disponíveis e selecionados.
 */
public class DualListField<T> extends PanelEventListener {

    public static final String ITEMS_ADDED = "itemsAdded";
    public static final String ITEMS_REMOVED = "itemsRemoved";
    public static final String ORDER_CHANGED = "orderChanged";

    /**
     * Glifos desenhados nos botões de transferência e reordenação.
     */
    private enum Glyph {
        RIGHT, RIGHT_ALL, LEFT, LEFT_ALL, UP, DOWN
    }

    private final List<T> availableItems = new ArrayList<>();
    private final List<T> selectedItems = new ArrayList<>();

    private final DefaultListModel<T> availableModel = new DefaultListModel<>();
    private final DefaultListModel<T> selectedModel = new DefaultListModel<>();

    private final JList<T> availableList = new JList<>(availableModel);
    private final JList<T> selectedList = new JList<>(selectedModel);

    private final JTextField availableFilter = new JTextField();
    private final JTextField selectedFilter = new JTextField();

    private final JLabel availableTitle = new JLabel();
    private final JLabel selectedTitle = new JLabel();

    private final JPanel availablePanel = new JPanel(new BorderLayout(0, 6));
    private final JPanel selectedPanel = new JPanel(new BorderLayout(0, 6));
    private final JPanel actionsPanel = new JPanel();

    private final GlyphButton addButton = new GlyphButton(Glyph.RIGHT);
    private final GlyphButton addAllButton = new GlyphButton(Glyph.RIGHT_ALL);
    private final GlyphButton removeButton = new GlyphButton(Glyph.LEFT);
    private final GlyphButton removeAllButton = new GlyphButton(Glyph.LEFT_ALL);
    private final GlyphButton moveUpButton = new GlyphButton(Glyph.UP);
    private final GlyphButton moveDownButton = new GlyphButton(Glyph.DOWN);

    private Function<T, String> labelProvider = String::valueOf;
    private Comparator<T> comparator;
    private int maxSelected = -1;
    private boolean reorderable;
    private boolean showFilter = true;
    private boolean showCounters = true;
    private boolean showTitles = true;

    private String availableTitleText;
    private String selectedTitleText;

    public DualListField() {
        this(List.of(), List.of());
    }

    public DualListField(List<T> available) {
        this(available, List.of());
    }

    public DualListField(List<T> available, List<T> selected) {
        super(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.ROW)
                .align(FlexBoxLayout.Align.STRETCH)
                .gap(UiTokens.space(2))
                .build(), false);

        this.availableTitleText = I18n.getText(DualListField.class, "title.available", "Disponíveis");
        this.selectedTitleText = I18n.getText(DualListField.class, "title.selected", "Selecionados");

        this.availableItems.addAll(available);
        this.selectedItems.addAll(selected);
        this.availableItems.removeAll(this.selectedItems);

        buildSides();
        buildActions();
        installListeners();

        add(availablePanel, FlexBoxLayout.FlexConstraints.of().grow(1).minWidth(UiTokens.scale(120)));
        add(actionsPanel, FlexBoxLayout.FlexConstraints.of().fixedWidth(UiTokens.scale(48)));
        add(selectedPanel, FlexBoxLayout.FlexConstraints.of().grow(1).minWidth(UiTokens.scale(120)));

        setPreferredSize(new Dimension(UiTokens.scale(520), UiTokens.scale(260)));
        setMinimumSize(new Dimension(UiTokens.scale(320), UiTokens.scale(160)));
        refreshModels();
    }

    /**
     * Itens ainda não selecionados, na ordem corrente.
     */
    public List<T> getAvailable() {
        return List.copyOf(availableItems);
    }

    /**
     * Itens selecionados, na ordem corrente.
     */
    public List<T> getSelected() {
        return List.copyOf(selectedItems);
    }

    /**
     * Substitui a lista de itens disponíveis.
     */
    public DualListField<T> setAvailable(List<T> items) {
        availableItems.clear();
        if (items != null) {
            availableItems.addAll(items);
            availableItems.removeAll(selectedItems);
        }
        refreshModels();
        return this;
    }

    /**
     * Substitui a lista de itens selecionados disparando evento de mudança.
     */
    public DualListField<T> setSelected(List<T> items) {
        return setSelected(items, true);
    }

    /**
     * Substitui a lista de itens selecionados, opcionalmente sem disparar evento.
     */
    public DualListField<T> setSelected(List<T> items, boolean fireEvent) {
        List<T> previous = List.copyOf(selectedItems);
        availableItems.addAll(selectedItems);
        selectedItems.clear();
        if (items != null) {
            for (T item : items) {
                if (isSelectionFull()) {
                    break;
                }
                selectedItems.add(item);
                availableItems.remove(item);
            }
        }
        refreshModels();
        if (fireEvent) {
            fireChange(previous, difference(selectedItems, previous), difference(previous, selectedItems));
        }
        return this;
    }

    /**
     * Define como cada item é convertido em texto.
     */
    public DualListField<T> setLabelProvider(Function<T, String> labelProvider) {
        if (labelProvider == null) {
            throw new IllegalArgumentException("labelProvider cannot be null");
        }
        this.labelProvider = labelProvider;
        repaintLists();
        return this;
    }

    /**
     * Define um renderizador customizado para ambas as listas.
     */
    public DualListField<T> setCellRenderer(ListCellRenderer<? super T> renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }
        availableList.setCellRenderer(renderer);
        selectedList.setCellRenderer(renderer);
        repaintLists();
        return this;
    }

    /**
     * Define a ordenação aplicada às duas listas; {@code null} preserva a ordem de inserção.
     */
    public DualListField<T> setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
        refreshModels();
        return this;
    }

    /**
     * Define os títulos exibidos acima de cada lista.
     */
    public DualListField<T> setTitles(String available, String selected) {
        this.availableTitleText = available != null ? available : "";
        this.selectedTitleText = selected != null ? selected : "";
        refreshTitles();
        return this;
    }

    /**
     * Habilita os botões de reordenação da lista de selecionados.
     */
    public DualListField<T> setReorderable(boolean reorderable) {
        this.reorderable = reorderable;
        moveUpButton.setVisible(reorderable);
        moveDownButton.setVisible(reorderable);
        actionsPanel.revalidate();
        actionsPanel.repaint();
        updateButtonStates();
        return this;
    }

    /**
     * Indica se a reordenação está habilitada.
     */
    public boolean isReorderable() {
        return reorderable;
    }

    /**
     * Exibe ou oculta os campos de filtro de cada lista.
     */
    public DualListField<T> setShowFilter(boolean showFilter) {
        this.showFilter = showFilter;
        availableFilter.setVisible(showFilter);
        selectedFilter.setVisible(showFilter);
        if (!showFilter) {
            availableFilter.setText("");
            selectedFilter.setText("");
        }
        refreshModels();
        revalidate();
        return this;
    }

    /**
     * Indica se os filtros estão visíveis.
     */
    public boolean isShowFilter() {
        return showFilter;
    }

    /**
     * Exibe ou oculta a contagem de itens nos títulos.
     */
    public DualListField<T> setShowCounters(boolean showCounters) {
        this.showCounters = showCounters;
        refreshTitles();
        return this;
    }

    /**
     * Exibe ou oculta os títulos das listas.
     */
    public DualListField<T> setShowTitles(boolean showTitles) {
        this.showTitles = showTitles;
        availableTitle.setVisible(showTitles);
        selectedTitle.setVisible(showTitles);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Limita a quantidade de itens selecionáveis; valor negativo remove o limite.
     */
    public DualListField<T> setMaxSelected(int maxSelected) {
        this.maxSelected = maxSelected;
        while (maxSelected >= 0 && selectedItems.size() > maxSelected) {
            availableItems.add(selectedItems.remove(selectedItems.size() - 1));
        }
        refreshModels();
        return this;
    }

    /**
     * Quantidade máxima de itens selecionáveis.
     */
    public int getMaxSelected() {
        return maxSelected;
    }

    /**
     * Define a altura de cada linha das listas.
     */
    public DualListField<T> setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("rowHeight must be greater than zero");
        }
        availableList.setFixedCellHeight(rowHeight);
        selectedList.setFixedCellHeight(rowHeight);
        return this;
    }

    /**
     * Move para os selecionados os itens marcados na lista de disponíveis.
     */
    public DualListField<T> addSelection() {
        transfer(availableList.getSelectedValuesList(), true);
        return this;
    }

    /**
     * Move para os selecionados todos os itens visíveis na lista de disponíveis.
     */
    public DualListField<T> addAllItems() {
        transfer(visibleItems(availableModel), true);
        return this;
    }

    /**
     * Devolve aos disponíveis os itens marcados na lista de selecionados.
     */
    public DualListField<T> removeSelection() {
        transfer(selectedList.getSelectedValuesList(), false);
        return this;
    }

    /**
     * Devolve aos disponíveis todos os itens visíveis na lista de selecionados.
     */
    public DualListField<T> removeAllItems() {
        transfer(visibleItems(selectedModel), false);
        return this;
    }

    /**
     * Move uma posição acima os itens marcados na lista de selecionados.
     */
    public DualListField<T> moveUp() {
        return shift(-1);
    }

    /**
     * Move uma posição abaixo os itens marcados na lista de selecionados.
     */
    public DualListField<T> moveDown() {
        return shift(1);
    }

    /**
     * Lista de disponíveis, exposta para customizações avançadas.
     */
    public JList<T> getAvailableList() {
        return availableList;
    }

    /**
     * Lista de selecionados, exposta para customizações avançadas.
     */
    public JList<T> getSelectedList() {
        return selectedList;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        availableList.setEnabled(enabled);
        selectedList.setEnabled(enabled);
        availableFilter.setEnabled(enabled);
        selectedFilter.setEnabled(enabled);
        updateButtonStates();
    }

    private void buildSides() {
        availablePanel.setOpaque(false);
        selectedPanel.setOpaque(false);

        configureTitle(availableTitle);
        configureTitle(selectedTitle);

        configureFilter(availableFilter, I18n.getText(DualListField.class, "filter.placeholder", "Filtrar..."));
        configureFilter(selectedFilter, I18n.getText(DualListField.class, "filter.placeholder", "Filtrar..."));

        configureList(availableList);
        configureList(selectedList);

        availablePanel.add(buildHeader(availableTitle, availableFilter), BorderLayout.NORTH);
        availablePanel.add(buildScroll(availableList), BorderLayout.CENTER);

        selectedPanel.add(buildHeader(selectedTitle, selectedFilter), BorderLayout.NORTH);
        selectedPanel.add(buildScroll(selectedList), BorderLayout.CENTER);

        refreshTitles();
    }

    private JPanel buildHeader(JLabel title, JTextField filter) {
        JPanel header = new JPanel(new BorderLayout(0, UiTokens.space(1)));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(filter, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane buildScroll(JList<T> list) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTokens.border(), 1, true));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UiTokens.surface());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void configureTitle(JLabel title) {
        title.setFont(UiTokens.fontSmall().deriveFont(java.awt.Font.BOLD));
        title.setForeground(UiTokens.muted());
        title.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void configureFilter(JTextField filter, String placeholder) {
        filter.setFont(UiTokens.fontSmall());
        filter.putClientProperty("JTextField.placeholderText", placeholder);
        filter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTokens.border(), 1, true),
                BorderFactory.createEmptyBorder(UiTokens.space(1), UiTokens.space(2), UiTokens.space(1), UiTokens.space(2))));
    }

    private void configureList(JList<T> list) {
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(UiTokens.surface());
        list.setForeground(UiTokens.foreground());
        list.setSelectionBackground(UiTokens.accent());
        list.setSelectionForeground(UiTokens.onColor(UiTokens.accent()));
        list.setFont(UiTokens.font());
        list.setFixedCellHeight(UiTokens.scale(26));
        list.setCellRenderer(new LabelRenderer());
    }

    private void buildActions() {
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .justify(FlexBoxLayout.Justify.CENTER)
                .align(FlexBoxLayout.Align.CENTER)
                .gap(UiTokens.space(1))
                .build());

        addButton.setToolTipText(I18n.getText(DualListField.class, "action.add", "Adicionar selecionados"));
        addAllButton.setToolTipText(I18n.getText(DualListField.class, "action.addAll", "Adicionar todos"));
        removeButton.setToolTipText(I18n.getText(DualListField.class, "action.remove", "Remover selecionados"));
        removeAllButton.setToolTipText(I18n.getText(DualListField.class, "action.removeAll", "Remover todos"));
        moveUpButton.setToolTipText(I18n.getText(DualListField.class, "action.moveUp", "Mover para cima"));
        moveDownButton.setToolTipText(I18n.getText(DualListField.class, "action.moveDown", "Mover para baixo"));

        moveUpButton.setVisible(false);
        moveDownButton.setVisible(false);

        actionsPanel.add(addButton);
        actionsPanel.add(addAllButton);
        actionsPanel.add(removeButton);
        actionsPanel.add(removeAllButton);
        actionsPanel.add(moveUpButton);
        actionsPanel.add(moveDownButton);
    }

    private void installListeners() {
        addButton.onClick(this::addSelection);
        addAllButton.onClick(this::addAllItems);
        removeButton.onClick(this::removeSelection);
        removeAllButton.onClick(this::removeAllItems);
        moveUpButton.onClick(this::moveUp);
        moveDownButton.onClick(this::moveDown);

        availableList.addListSelectionListener(e -> updateButtonStates());
        selectedList.addListSelectionListener(e -> updateButtonStates());

        installTransferGestures(availableList, true);
        installTransferGestures(selectedList, false);

        availableFilter.getDocument().addDocumentListener(new FilterListener());
        selectedFilter.getDocument().addDocumentListener(new FilterListener());
    }

    private void installTransferGestures(JList<T> list, boolean toSelected) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled() || e.getClickCount() != 2) {
                    return;
                }
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) {
                    return;
                }
                list.setSelectedIndex(index);
                transfer(list.getSelectedValuesList(), toSelected);
            }
        });

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled() || e.getKeyCode() != KeyEvent.VK_ENTER) {
                    return;
                }
                transfer(list.getSelectedValuesList(), toSelected);
                e.consume();
            }
        });
    }

    private void transfer(List<T> items, boolean toSelected) {
        if (!isEnabled() || items == null || items.isEmpty()) {
            return;
        }

        List<T> previous = List.copyOf(selectedItems);
        List<T> moved = new ArrayList<>();

        for (T item : items) {
            if (toSelected) {
                if (isSelectionFull() || !availableItems.remove(item)) {
                    continue;
                }
                selectedItems.add(item);
            } else {
                if (!selectedItems.remove(item)) {
                    continue;
                }
                availableItems.add(item);
            }
            moved.add(item);
        }

        if (moved.isEmpty()) {
            return;
        }

        refreshModels();
        Map<String, Object> props = Map.of(
                "oldValue", previous,
                "newValue", List.copyOf(selectedItems),
                "moved", List.copyOf(moved));
        dispatchEvent(toSelected ? ITEMS_ADDED : ITEMS_REMOVED, this, List.copyOf(moved), props);
        dispatchEvent(EventType.CHANGE, this, List.copyOf(selectedItems), props);
    }

    private DualListField<T> shift(int offset) {
        if (!reorderable || !isEnabled()) {
            return this;
        }

        int[] indices = selectedList.getSelectedIndices();
        if (indices.length == 0) {
            return this;
        }

        List<T> visible = visibleItems(selectedModel);
        if (visible.size() != selectedItems.size()) {
            return this;
        }

        List<T> previous = List.copyOf(selectedItems);
        int[] ordered = indices.clone();
        if (offset > 0) {
            reverse(ordered);
        }

        for (int index : ordered) {
            int target = index + offset;
            if (target < 0 || target >= selectedItems.size()) {
                return this;
            }
        }

        for (int index : ordered) {
            Collections.swap(selectedItems, index, index + offset);
        }

        Comparator<T> previousComparator = comparator;
        comparator = null;
        refreshModels();
        comparator = previousComparator;

        int[] newIndices = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            newIndices[i] = indices[i] + offset;
        }
        selectedList.setSelectedIndices(newIndices);

        Map<String, Object> props = Map.of(
                "oldValue", previous,
                "newValue", List.copyOf(selectedItems),
                "offset", offset);
        dispatchEvent(ORDER_CHANGED, this, List.copyOf(selectedItems), props);
        dispatchEvent(EventType.CHANGE, this, List.copyOf(selectedItems), props);
        return this;
    }

    private void refreshModels() {
        if (comparator != null) {
            availableItems.sort(comparator);
            selectedItems.sort(comparator);
        }
        fill(availableModel, availableItems, availableFilter.getText());
        fill(selectedModel, selectedItems, selectedFilter.getText());
        refreshTitles();
        updateButtonStates();
    }

    private void fill(DefaultListModel<T> model, List<T> source, String filter) {
        model.clear();
        String needle = showFilter && filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        for (T item : source) {
            if (needle.isEmpty() || labelProvider.apply(item).toLowerCase(Locale.ROOT).contains(needle)) {
                model.addElement(item);
            }
        }
    }

    private void refreshTitles() {
        availableTitle.setText(titleFor(availableTitleText, availableItems.size()));
        selectedTitle.setText(titleFor(selectedTitleText, selectedItems.size()));
    }

    private String titleFor(String text, int count) {
        if (!showCounters) {
            return text;
        }
        if (maxSelected >= 0 && text.equals(selectedTitleText)) {
            return text + " (" + count + "/" + maxSelected + ")";
        }
        return text + " (" + count + ")";
    }

    private void updateButtonStates() {
        boolean enabled = isEnabled();
        addButton.setEnabled(enabled && !availableList.isSelectionEmpty() && !isSelectionFull());
        addAllButton.setEnabled(enabled && !availableModel.isEmpty() && !isSelectionFull());
        removeButton.setEnabled(enabled && !selectedList.isSelectionEmpty());
        removeAllButton.setEnabled(enabled && !selectedModel.isEmpty());

        boolean canReorder = enabled && reorderable && !selectedList.isSelectionEmpty();
        moveUpButton.setEnabled(canReorder && selectedList.getMinSelectionIndex() > 0);
        moveDownButton.setEnabled(canReorder && selectedList.getMaxSelectionIndex() < selectedModel.size() - 1);
    }

    private boolean isSelectionFull() {
        return maxSelected >= 0 && selectedItems.size() >= maxSelected;
    }

    private List<T> visibleItems(DefaultListModel<T> model) {
        List<T> items = new ArrayList<>(model.size());
        for (int i = 0; i < model.size(); i++) {
            items.add(model.get(i));
        }
        return items;
    }

    private List<T> difference(List<T> source, List<T> other) {
        List<T> result = new ArrayList<>(source);
        result.removeAll(other);
        return result;
    }

    private void fireChange(List<T> previous, List<T> added, List<T> removed) {
        Map<String, Object> props = Map.of(
                "oldValue", previous,
                "newValue", List.copyOf(selectedItems),
                "added", added,
                "removed", removed);
        dispatchEvent(EventType.CHANGE, this, List.copyOf(selectedItems), props);
    }

    private void repaintLists() {
        availableList.repaint();
        selectedList.repaint();
    }

    private static void reverse(int[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            int temp = values[i];
            values[i] = values[j];
            values[j] = temp;
        }
    }

    /**
     * Renderizador padrão que converte o item usando o provedor de rótulos.
     */
    private final class LabelRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            @SuppressWarnings("unchecked")
            T item = (T) value;
            setText(item == null ? "" : labelProvider.apply(item));
            setBorder(BorderFactory.createEmptyBorder(0, UiTokens.space(2), 0, UiTokens.space(2)));
            return this;
        }
    }

    /**
     * Reaplica o filtro sempre que o texto de busca muda.
     */
    private final class FilterListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            refreshModels();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refreshModels();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            refreshModels();
        }
    }

    /**
     * Botão compacto que desenha o glifo de transferência ou reordenação.
     */
    private static final class GlyphButton extends JComponent {

        private final Glyph glyph;
        private Runnable action = () -> { };
        private boolean hover;
        private boolean pressed;

        private GlyphButton(Glyph glyph) {
            this.glyph = glyph;
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(UiTokens.scale(32), UiTokens.scale(28)));
            setMaximumSize(new Dimension(UiTokens.scale(32), UiTokens.scale(28)));
            installMouse();
        }

        private void onClick(Runnable action) {
            this.action = action != null ? action : () -> { };
        }

        private void installMouse() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = isEnabled();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean wasPressed = pressed;
                    pressed = false;
                    repaint();
                    if (wasPressed && isEnabled() && contains(e.getPoint())) {
                        action.run();
                    }
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled) {
                hover = false;
                pressed = false;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
                paintBackground(g2, bounds);
                paintGlyph(g2, bounds);
            } finally {
                g2.dispose();
            }
        }

        private void paintBackground(Graphics2D g2, Rectangle bounds) {
            if (!isEnabled() || (!hover && !pressed)) {
                return;
            }
            java.awt.Color base = UiTokens.surfaceAlt();
            PaintUtils.fillRoundRect(g2, bounds, UiTokens.radius(UiTokens.Radius.SM),
                    pressed ? UiTokens.pressed(base) : UiTokens.hover(base));
        }

        private void paintGlyph(Graphics2D g2, Rectangle bounds) {
            java.awt.Color color = isEnabled()
                    ? (hover ? UiTokens.primary() : UiTokens.foreground())
                    : UiTokens.disabled(UiTokens.foreground());
            g2.setColor(color);
            g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = bounds.width / 2;
            int cy = bounds.height / 2;
            int arm = UiTokens.scale(4);
            int gap = UiTokens.scale(3);

            switch (glyph) {
                case RIGHT -> chevronHorizontal(g2, cx, cy, arm, 1);
                case LEFT -> chevronHorizontal(g2, cx, cy, arm, -1);
                case RIGHT_ALL -> {
                    chevronHorizontal(g2, cx - gap, cy, arm, 1);
                    chevronHorizontal(g2, cx + gap, cy, arm, 1);
                }
                case LEFT_ALL -> {
                    chevronHorizontal(g2, cx - gap, cy, arm, -1);
                    chevronHorizontal(g2, cx + gap, cy, arm, -1);
                }
                case UP -> chevronVertical(g2, cx, cy, arm, -1);
                case DOWN -> chevronVertical(g2, cx, cy, arm, 1);
            }
        }

        private void chevronHorizontal(Graphics2D g2, int cx, int cy, int arm, int direction) {
            g2.drawLine(cx - arm * direction / 2, cy - arm, cx + arm * direction / 2, cy);
            g2.drawLine(cx + arm * direction / 2, cy, cx - arm * direction / 2, cy + arm);
        }

        private void chevronVertical(Graphics2D g2, int cx, int cy, int arm, int direction) {
            g2.drawLine(cx - arm, cy - arm * direction / 2, cx, cy + arm * direction / 2);
            g2.drawLine(cx, cy + arm * direction / 2, cx + arm, cy - arm * direction / 2);
        }
    }
}
