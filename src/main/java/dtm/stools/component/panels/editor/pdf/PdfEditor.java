package dtm.stools.component.panels.editor.pdf;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.editor.pdf.api.PdfChange;
import dtm.stools.component.panels.editor.pdf.api.PdfDocument;
import dtm.stools.component.panels.editor.pdf.api.PdfEdit;
import dtm.stools.component.panels.editor.pdf.api.PdfEraserMode;
import dtm.stools.component.panels.editor.pdf.api.PdfFieldInfo;
import dtm.stools.component.panels.editor.pdf.api.PdfOcrResult;
import dtm.stools.component.panels.editor.pdf.api.PdfOperation;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.api.PdfSelectionListener;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfSignatureValidation;
import dtm.stools.component.panels.editor.pdf.api.PdfTarget;
import dtm.stools.component.panels.editor.pdf.api.PdfTask;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfViewMode;
import dtm.stools.component.panels.editor.pdf.command.PdfAction;
import dtm.stools.component.panels.editor.pdf.command.PdfSelectionOperations;
import dtm.stools.component.panels.editor.pdf.command.PdfCommandCatalog;
import dtm.stools.component.panels.editor.pdf.command.PdfCommands;
import dtm.stools.component.panels.editor.pdf.command.PdfFormFieldMenuProvider;
import dtm.stools.component.panels.editor.pdf.command.PdfHistory;
import dtm.stools.component.panels.editor.pdf.command.PdfHistoryEntry;
import dtm.stools.component.panels.editor.pdf.config.PdfEditorConfig;
import dtm.stools.component.panels.editor.pdf.config.PdfServices;
import dtm.stools.component.panels.editor.pdf.element.PdfElementFactory;
import dtm.stools.component.panels.editor.pdf.provider.PdfBackendProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfCommandProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfContextMenuProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfDialogProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfFileDialogProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfOcrProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfProviderRegistration;
import dtm.stools.component.panels.editor.pdf.provider.PdfRibbonContributor;
import dtm.stools.component.panels.editor.pdf.provider.PdfSignatureProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfToolbarProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfTrustProvider;
import dtm.stools.component.panels.editor.pdf.ui.PdfCanvas;
import dtm.stools.component.panels.editor.pdf.ui.PdfPageGeometry;
import dtm.stools.component.panels.editor.pdf.ui.PdfPageLayout;
import dtm.stools.component.panels.editor.pdf.ui.PdfRenderScheduler;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbon;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonGroup;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem;
import dtm.stools.component.panels.editor.pdf.ui.PdfSelectionController;
import dtm.stools.component.panels.editor.pdf.ui.PdfSidebar;
import dtm.stools.component.panels.editor.pdf.ui.PdfStatusBar;
import dtm.stools.component.panels.editor.pdf.ui.PdfTextOverlay;
import dtm.stools.component.panels.editor.pdf.ui.PdfUiFactory;
import dtm.stools.configs.UiTokens;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class PdfEditor extends BlockingPanel implements AutoCloseable {
    public static final String TOOL_SELECT = "pdf.select", TOOL_AREA = "pdf.selectArea", TOOL_ERASER = "pdf.eraser";

    private final ExecutorService workers = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "swingtools-pdf");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, PdfProviderEntry> providers = new LinkedHashMap<>();
    private final Map<String, PdfElementFactory> factories = new LinkedHashMap<>();
    private final List<PdfSelectionListener> selectionListeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, List<PdfPageElement>> elements = new HashMap<>();
    private final PdfCommands commands;
    private final PdfHistory history = new PdfHistory();
    private final PdfSelectionOperations operations = new PdfSelectionOperations(this);
    private final PdfRenderScheduler renderer;
    private final PdfCanvas canvas;
    private final JScrollPane scroll;
    private final JComponent ribbon;
    private final PdfRibbon defaultRibbon;
    private final PdfSidebar sidebar;
    private final PdfStatusBar status;
    private final PdfSelectionController controller;
    private final JSplitPane split;
    private final JPanel providerBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private PdfEditorConfig config;
    private volatile PdfDocument document;
    private Path currentFile;
    private FileTime fileTime;
    private long stateId, savedStateId, nextStateId, registrationOrder, elementVersion;
    private int currentPage, pageAnchor;
    private final java.util.Set<Integer> selectedPages = new java.util.TreeSet<>();
    private PdfPageGeometry[] pages = new PdfPageGeometry[0];
    private boolean modifiable = true, formFields;
    private String activeTool = TOOL_SELECT, activeFactory;
    private PdfSelection selection = PdfSelection.empty();
    private PdfTextStyle textStyle = PdfTextStyle.defaults();
    private PdfShapeStyle shapeStyle = PdfShapeStyle.defaults();
    private PdfEraserMode eraserMode = PdfEraserMode.BRUSH;
    private float eraserSize = 16;
    private PdfTextOverlay textOverlay;
    private boolean closed, programmaticScroll;
    private int sidebarWidth = 214;
    private Consumer<Throwable> errorHandler = error -> firePropertyChange("error", null, error);

    public PdfEditor() { this(PdfEditorConfig.defaults(), PdfServices.defaults()); }
    public PdfEditor(PdfEditorConfig config) { this(config, PdfServices.defaults()); }
    public PdfEditor(PdfEditorConfig config, PdfServices services) {
        this.config = Objects.requireNonNull(config);
        Objects.requireNonNull(services);
        setLayout(new BorderLayout());
        commands = new PdfCommands(this::reportError, () -> !isReadOnly());
        addProvider(services.backend());
        if (services.ocr() != null) addProvider(services.ocr());
        addProvider(services.signatures());
        addProvider(services.trust());
        addProvider(services.dialogs());
        addProvider(services.files());
        try { document = backend().create(); }
        catch (IOException error) { throw new IllegalStateException(error); }
        renderer = new PdfRenderScheduler(() -> document, page -> page >= 0 && page < pages.length ? pages[page] : null,
                () -> this.config.cachePages());
        PdfUiFactory ui = services.uiFactory();
        canvas = Objects.requireNonNull(ui.createCanvas(this));
        PdfCommandCatalog.registerAll(this, commands);
        for (PdfElementFactory factory : services.elementFactories()) addProvider(factory);
        addProvider(new PdfFormFieldMenuProvider());
        ribbon = Objects.requireNonNull(ui.createRibbon(this));
        defaultRibbon = ribbon instanceof PdfRibbon value ? value : null;
        sidebar = Objects.requireNonNull(ui.createSidebar(this));
        status = Objects.requireNonNull(ui.createStatusBar(this));
        sidebar.setTools(toolActions());
        controller = Objects.requireNonNull(ui.createSelectionController(this, canvas));
        controller.install();
        scroll = new JScrollPane(canvas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(32);
        scroll.getHorizontalScrollBar().setUnitIncrement(32);
        scroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        scroll.getViewport().addChangeListener(event -> viewportChanged());
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { relayout(); }
        });
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, scroll);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(5);
        split.setContinuousLayout(true);
        split.setResizeWeight(0);
        split.setDividerLocation(sidebarWidth);
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            if (sidebar.isVisible() && split.getDividerLocation() > 40) sidebarWidth = split.getDividerLocation();
        });
        providerBar.setVisible(false);
        providerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()));
        providerBar.setName("pdf.providerBar");
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        ribbon.setAlignmentX(LEFT_ALIGNMENT);
        providerBar.setAlignmentX(LEFT_ALIGNMENT);
        north.add(ribbon);
        north.add(providerBar);
        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        for (PdfProviderEntry entry : List.copyOf(providers.values()))
            reinstallInterface(entry);
        installKeys();
        applyConfig();
        documentReplaced();
    }

    private void reinstallInterface(PdfProviderEntry entry) {
        if (entry.provider() instanceof PdfElementFactory factory) entry.hooks().add(installFactoryItem(factory));
        if (entry.provider() instanceof PdfToolbarProvider toolbar) entry.hooks().add(installToolbar(toolbar));
        if (entry.provider() instanceof PdfRibbonContributor contributor) entry.hooks().add(installContribution(contributor));
    }

    private void installKeys() {
        key(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, "pdf.save");
        key(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "pdf.saveAs");
        key(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, "pdf.open");
        key(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, "pdf.new");
        key(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, "pdf.print");
        key(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, "pdf.undo");
        key(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, "pdf.redo");
        key(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "pdf.redo");
        key(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, "pdf.find");
        key(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK, "pdf.zoomIn");
        key(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK, "pdf.zoomIn");
        key(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK, "pdf.zoomOut");
        key(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK, "pdf.zoomOut");
        key(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK, "pdf.zoom100");
        key(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK, "pdf.next");
        key(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_DOWN_MASK, "pdf.previous");
    }

    private void key(int code, int modifiers, String id) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(code, modifiers), id);
        getActionMap().put(id, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { execute(id); }
        });
    }

    public PdfEditorConfig getConfig() { return config; }
    public void setConfig(PdfEditorConfig value) {
        ensureOpen();
        PdfEditorConfig old = config;
        config = Objects.requireNonNull(value);
        applyConfig();
        if (old.viewMode() != value.viewMode()) { relayout(); scrollToPage(currentPage); }
        else if (old.zoom() != value.zoom()) zoomAround(old.zoom(), null);
        commands.refresh();
        refreshStatus();
    }
    public void setReadOnly(boolean value) { setConfig(config.withReadOnly(value)); }
    public boolean isReadOnly() { return config.readOnly() || !modifiable; }
    public void setZoom(double value) { setConfig(config.withZoom(Math.max(.1, Math.min(8, value)))); }
    public double getZoom() { return config.zoom(); }
    public PdfViewMode getViewMode() { return config.viewMode(); }
    public void setViewMode(PdfViewMode value) { setConfig(config.withViewMode(value)); }
    public void setPageReconstructionEnabled(boolean value) { setConfig(config.withPageReconstructionEnabled(value)); }
    public boolean isDirty() { return stateId != savedStateId; }
    public boolean isClosed() { return closed; }
    public int getPageCount() { return pages.length; }
    public int getCurrentPage() { return currentPage; }
    public float getPageWidth(int page) { return pages[page].width(); }
    public float getPageHeight(int page) { return pages[page].height(); }
    public boolean hasFormFields() { return formFields; }
    public Optional<Path> getCurrentFile() { return Optional.ofNullable(currentFile); }
    public PdfDocument getDocument() { return document; }
    public PdfRenderScheduler getRenderer() { return renderer; }
    public PdfCanvas getCanvas() { return canvas; }
    public JComponent getRibbon() { return ribbon; }
    public PdfSidebar getSidebar() { return sidebar; }
    public PdfDialogProvider getDialogs() { return active(PdfDialogProvider.class).orElseThrow(); }
    public PdfFileDialogProvider getFiles() { return active(PdfFileDialogProvider.class).orElseThrow(); }
    public Map<String, Action> getCommands() { return commands.all(); }
    public boolean canUndo() { return !isReadOnly() && history.canUndo(); }
    public boolean canRedo() { return !isReadOnly() && history.canRedo(); }
    public void setErrorHandler(Consumer<Throwable> handler) { errorHandler = Objects.requireNonNull(handler); }

    public void execute(String id) {
        Action action = commands.get(id);
        if (action != null && action.isEnabled()) action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, id));
    }

    public boolean perform(PdfOperation operation) {
        try {
            operation.run();
            return true;
        } catch (IOException | RuntimeException error) {
            reportError(error);
            return false;
        }
    }

    public void reportError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getMessage() == null) cause = cause.getCause();
        errorHandler.accept(error);
        if (status != null) status.message("Erro: " + (cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage()));
    }

    public PdfPageGeometry getPageGeometry(int page) { return getPageGeometry(page, scale()); }
    public PdfPageGeometry getPageGeometry(int page, double scale) {
        return pages[page].withScale(scale);
    }
    private double scale() { return 96 * config.zoom() / 72; }

    public void setCurrentPage(int page) {
        ensureOpen();
        if (page < 0 || page >= getPageCount()) return;
        boolean changed = page != currentPage;
        currentPage = page;
        if (config.viewMode() == PdfViewMode.SINGLE_PAGE) {
            relayout();
            scroll.getViewport().setViewPosition(new Point(scroll.getViewport().getViewPosition().x, 0));
        } else scrollToPage(page);
        if (changed) pageChanged();
    }

    private void pageChanged() {
        sidebar.currentPageChanged();
        commands.refresh();
        refreshStatus();
    }

    private void scrollToPage(int page) {
        Rectangle box = canvas.getPageLayout().bounds(page);
        if (box == null) return;
        programmaticScroll = true;
        JViewport viewport = scroll.getViewport();
        int maxY = Math.max(0, canvas.getHeight() - viewport.getExtentSize().height);
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, Math.max(0, Math.min(maxY, box.y - PdfPageLayout.MARGIN / 2))));
        SwingUtilities.invokeLater(() -> programmaticScroll = false);
    }

    private void viewportChanged() {
        if (closed || programmaticScroll || config.viewMode() != PdfViewMode.CONTINUOUS) return;
        int page = canvas.getPageLayout().pageAtViewportCenter(scroll.getViewport().getViewRect());
        if (page >= 0 && page != currentPage) {
            currentPage = page;
            pageChanged();
        }
    }

    public void relayout() {
        if (canvas == null || scroll == null || document == null) return;
        int count = getPageCount();
        PdfPageGeometry[] pages = new PdfPageGeometry[count];
        double scale = scale();
        for (int page = 0; page < count; page++) pages[page] = getPageGeometry(page, scale);
        int width = scroll.getViewport().getExtentSize().width;
        canvas.setPageLayout(PdfPageLayout.compute(pages, config.viewMode() == PdfViewMode.CONTINUOUS, currentPage, width));
        if (textOverlay != null) textOverlay.relayout();
    }

    public void zoomAt(double factor, Point anchor) {
        double value = Math.max(.1, Math.min(8, config.zoom() * factor));
        double old = config.zoom();
        config = config.withZoom(value);
        zoomAround(old, anchor);
        refreshStatus();
    }

    private void zoomAround(double oldZoom, Point anchor) {
        JViewport viewport = scroll.getViewport();
        Rectangle view = viewport.getViewRect();
        Point point = anchor != null ? anchor : new Point(view.x + view.width / 2, view.y + view.height / 3);
        PdfPageLayout before = canvas.getPageLayout();
        int page = before.pageAt(point) >= 0 ? before.pageAt(point) : before.nearestPage(point);
        Point2D.Float pdf = page >= 0 ? before.toPdf(page, point) : null;
        int offsetX = point.x - view.x, offsetY = point.y - view.y;
        relayout();
        scroll.validate();
        if (pdf == null || !canvas.getPageLayout().contains(page)) return;
        Point2D.Double target = canvas.getPageLayout().toView(page, pdf.x, pdf.y);
        programmaticScroll = true;
        int maxX = Math.max(0, canvas.getWidth() - viewport.getExtentSize().width);
        int maxY = Math.max(0, canvas.getHeight() - viewport.getExtentSize().height);
        viewport.setViewPosition(new Point((int) Math.max(0, Math.min(maxX, target.x - offsetX)),
                (int) Math.max(0, Math.min(maxY, target.y - offsetY))));
        SwingUtilities.invokeLater(() -> programmaticScroll = false);
    }

    public void fitWidth() {
        int available = scroll.getViewport().getExtentSize().width - 2 * PdfPageLayout.MARGIN - 4;
        PdfPageGeometry geometry = getPageGeometry(currentPage, 96.0 / 72);
        if (available > 50) setZoom(available / geometry.viewWidth());
    }

    public void fitPage() {
        Dimension extent = scroll.getViewport().getExtentSize();
        PdfPageGeometry geometry = getPageGeometry(currentPage, 96.0 / 72);
        double zoom = Math.min((extent.width - 2.0 * PdfPageLayout.MARGIN) / geometry.viewWidth(),
                (extent.height - 2.0 * PdfPageLayout.MARGIN) / geometry.viewHeight());
        if (zoom > .05) setZoom(zoom);
        scrollToPage(currentPage);
    }

    private void applyConfig() {
        ribbon.setVisible(config.ribbonVisible());
        status.setVisible(config.statusVisible());
        boolean show = config.thumbnailsVisible();
        if (sidebar.isVisible() != show) {
            sidebar.setVisible(show);
            if (show) split.setDividerLocation(sidebarWidth);
        }
        split.setDividerSize(show ? 5 : 0);
        revalidate();
        repaint();
    }

    public void setSidebarVisible(boolean value) { setConfig(config.withThumbnailsVisible(value)); }
    public void setStatusVisible(boolean value) { setConfig(config.withStatusVisible(value)); }

    public String getActiveTool() { return activeFactory != null ? activeFactory : activeTool; }
    public PdfElementFactory getActiveFactory() { return activeFactory == null ? null : factories.get(activeFactory); }
    public List<PdfElementFactory> getElementFactories() { return List.copyOf(factories.values()); }

    public void selectTool(String id) {
        ensureOpen();
        commitTextInput();
        if (factories.containsKey(id)) {
            activeFactory = id;
            if (!selection.isEmpty()) setSelection(PdfSelection.empty());
        } else if (TOOL_SELECT.equals(id) || TOOL_AREA.equals(id) || TOOL_ERASER.equals(id)) {
            activeFactory = null;
            activeTool = id;
            if (TOOL_ERASER.equals(id) && !selection.isEmpty()) setSelection(PdfSelection.empty());
        } else throw new IllegalArgumentException("Ferramenta desconhecida: " + id);
        canvas.setCursor(java.awt.Cursor.getDefaultCursor());
        commands.refresh();
        refreshStatus();
        canvas.repaint();
    }

    public void selectElementFactory(String id) {
        if (!factories.containsKey(id)) throw new IllegalArgumentException("Ferramenta desconhecida: " + id);
        selectTool(id);
    }

    public void placeElement(PdfElementFactory factory, PdfPlacement placement) {
        if (isReadOnly()) { reportError(new IllegalStateException("Editor somente leitura ou PDF protegido")); return; }
        try { factory.insert(this, placement); }
        catch (IOException | RuntimeException error) { reportError(error); }
        if (!factory.keepActive() && factory.id().equals(activeFactory)) {
            activeFactory = null;
            activeTool = TOOL_SELECT;
            canvas.setCursor(java.awt.Cursor.getDefaultCursor());
            commands.refresh();
            refreshStatus();
        }
    }

    public PdfTextStyle getTextStyle() { return textStyle; }
    public void setTextStyle(PdfTextStyle value) {
        textStyle = Objects.requireNonNull(value);
        if (textOverlay != null) textOverlay.applyStyle(value);
        else {
            List<PdfPageElement> boxes = selection.elements().stream().filter(PdfPageElement::textBox).toList();
            if (!boxes.isEmpty() && !isReadOnly()) {
                int page = selection.page();
                runEdit("Formatar texto", PdfChange.forPages(page), d -> {
                    for (PdfPageElement box : boxes) d.updateTextBox(page, box.id(), d.textBoxText(page, box.id()).orElse(""), value);
                });
                operations.refreshSelection(page);
            }
        }
        if (defaultRibbon != null) defaultRibbon.updateStyle(textStyle, shapeStyle);
    }

    public PdfShapeStyle getShapeStyle() { return shapeStyle; }
    public PdfEraserMode getEraserMode() { return eraserMode; }
    public void setEraserMode(PdfEraserMode value) {
        eraserMode = Objects.requireNonNull(value);
        if (!TOOL_ERASER.equals(getActiveTool())) selectTool(TOOL_ERASER);
        commands.refresh();
        refreshStatus();
    }
    public float getEraserSize() { return eraserSize; }
    public void setEraserSize(float value) {
        eraserSize = Math.max(1, Math.min(200, value));
        commands.refresh();
    }
    public void setShapeStyle(PdfShapeStyle value) {
        shapeStyle = Objects.requireNonNull(value);
        List<PdfPageElement> shapes = selection.elements().stream().filter(PdfPageElement::annotation)
                .filter(element -> !List.of("Text", "Stamp", "Highlight", "Widget").contains(element.type())).toList();
        if (!shapes.isEmpty() && !isReadOnly()) {
            int page = selection.page();
            runEdit("Formatar objeto", PdfChange.forPages(page), d -> {
                for (PdfPageElement shape : shapes) {
                    try { d.setAnnotationStyle(page, shape.id(), value); }
                    catch (IOException ignored) { }
                }
            });
            operations.refreshSelection(page);
        }
        if (defaultRibbon != null) defaultRibbon.updateStyle(textStyle, shapeStyle);
    }

    public PdfSelection getSelection() { return selection; }
    public Optional<Rectangle2D.Float> getSelectedArea() { return Optional.ofNullable(selection.hasArea() ? selection.area() : null); }
    public void addSelectionListener(PdfSelectionListener listener) { selectionListeners.add(Objects.requireNonNull(listener)); }
    public void removeSelectionListener(PdfSelectionListener listener) { selectionListeners.remove(listener); }
    public void clearSelection() { setSelection(PdfSelection.empty()); }

    public void setSelection(PdfSelection value) {
        PdfSelection next = value == null ? PdfSelection.empty() : value;
        if (next.equals(selection)) return;
        selection = next;
        if (!next.isEmpty() && next.page() != currentPage && next.page() < getPageCount()) {
            currentPage = next.page();
            sidebar.currentPageChanged();
        }
        List<PdfPageElement> boxes = next.elements().stream().filter(PdfPageElement::textBox).toList();
        if (boxes.size() == 1) {
            try { document.textBoxStyle(next.page(), boxes.getFirst().id()).ifPresent(style -> textStyle = style); }
            catch (IOException ignored) { }
        }
        if (defaultRibbon != null) {
            boolean annotations = next.elements().stream().anyMatch(PdfPageElement::annotation);
            defaultRibbon.setContextualTabs(annotations ? List.of("format") : List.of());
            defaultRibbon.updateStyle(textStyle, shapeStyle);
        }
        commands.refresh();
        refreshStatus();
        canvas.repaint();
        for (PdfSelectionListener listener : selectionListeners) listener.selectionChanged(next);
    }

    public List<PdfPageElement> getPageElements(int page) {
        if (page < 0 || page >= getPageCount()) return List.of();
        List<PdfPageElement> cached = elements.get(page);
        if (cached != null) return cached;
        try {
            List<PdfPageElement> loaded = document.pageElements(page);
            elements.put(page, loaded);
            return loaded;
        } catch (IOException | RuntimeException error) {
            elements.put(page, List.of());
            return List.of();
        }
    }

    public List<PdfPageElement> cachedPageElements(int page) {
        if (elements.containsKey(page) || page < 0 || page >= getPageCount()) return elements.get(page);
        long version = elementVersion;
        PdfDocument source = document;
        elements.put(page, null);
        renderer.background(() -> {
            List<PdfPageElement> loaded;
            try { loaded = source.pageElements(page); }
            catch (Throwable error) { loaded = List.of(); }
            List<PdfPageElement> result = loaded;
            SwingUtilities.invokeLater(() -> {
                if (closed || version != elementVersion || source != document) return;
                elements.put(page, result);
                canvas.repaint();
            });
        });
        return null;
    }

    public PdfPageElement elementAt(int page, Point2D point, double tolerance) {
        return elementAt(getPageElements(page), point, tolerance);
    }

    public PdfPageElement cachedElementAt(int page, Point2D point, double tolerance) {
        List<PdfPageElement> cached = cachedPageElements(page);
        return cached == null ? null : elementAt(cached, point, tolerance);
    }

    private static PdfPageElement elementAt(List<PdfPageElement> list, Point2D point, double tolerance) {
        for (int index = list.size() - 1; index >= 0; index--) {
            PdfPageElement element = list.get(index);
            Rectangle2D.Float bounds = element.bounds();
            Rectangle2D.Double grown = new Rectangle2D.Double(bounds.x - tolerance, bounds.y - tolerance,
                    bounds.width + 2 * tolerance, bounds.height + 2 * tolerance);
            if (grown.contains(point)) return element;
        }
        return null;
    }

    public void beginTextInput(PdfPlacement placement) {
        ensureOpen();
        if (isReadOnly()) return;
        commitTextInput();
        int page = placement.page();
        if (!canvas.getPageLayout().contains(page)) setCurrentPage(page);
        boolean fixed = placement.dragged();
        Rectangle2D.Float anchor;
        if (fixed) anchor = placement.bounds();
        else {
            float height = textStyle.size() * 1.25f + 4;
            anchor = new Rectangle2D.Float(placement.point().x, placement.point().y - height, 1, height);
        }
        openOverlay(new PdfTextOverlay(this, canvas, page, anchor, fixed, "", textStyle, null, false));
    }

    private void openOverlay(PdfTextOverlay overlay) {
        textOverlay = overlay;
        canvas.add(overlay);
        overlay.relayout();
        overlay.requestFocusInWindow();
        overlay.selectAll();
        refreshStatus();
    }

    public void editElement(int page, PdfPageElement element) {
        if (isReadOnly()) return;
        try {
            if (element.textBox()) {
                String text = document.textBoxText(page, element.id()).orElse("");
                PdfTextStyle style = document.textBoxStyle(page, element.id()).orElse(textStyle);
                setSelection(PdfSelection.empty());
                openOverlay(new PdfTextOverlay(this, canvas, page, element.bounds(), true, text, style, element.id(), false));
                return;
            }
            if (element.textual()) {
                if (!element.direct()) { status.message("Este texto não pode ser editado diretamente"); return; }
                Rectangle2D.Float bounds = element.bounds();
                PdfTextStyle style = textStyle.withSize(Math.max(4, Math.min(200, bounds.height * .82f)));
                setSelection(PdfSelection.empty());
                openOverlay(new PdfTextOverlay(this, canvas, page, bounds, false, element.text(), style, element.id(), true));
                return;
            }
            if (element.annotation() && "Widget".equals(element.type())) {
                fillWidget(page, element);
                return;
            }
            if (element.annotation() && ("Text".equals(element.type()) || !element.text().isBlank())) {
                getDialogs().input(this, "Editar nota", "Texto:", element.text()).ifPresent(value ->
                        runEdit("Editar nota", PdfChange.forPages(page), d -> d.replaceElementText(page, element.id(), value)));
                operations.refreshSelection(page);
            }
        } catch (IOException error) { reportError(error); }
    }

    private void fillWidget(int page, PdfPageElement element) throws IOException {
        Optional<PdfFieldInfo> info = document.fieldInfo(page, element.id());
        if (info.isEmpty()) return;
        PdfFieldInfo field = info.get();
        String value = switch (field.kind()) {
            case CHECKBOX -> String.valueOf(!Boolean.parseBoolean(field.value()));
            case RADIO -> field.widgetValue().isBlank() ? null : field.widgetValue();
            case CHOICE -> {
                if (field.options().isEmpty()) yield null;
                int choice = getDialogs().choose(this, field.name(), "Escolha uma opção:", field.options().toArray(String[]::new));
                yield choice < 0 ? null : field.options().get(choice);
            }
            default -> getDialogs().input(this, "Preencher campo", field.name() + ":", field.value()).orElse(null);
        };
        if (value == null) return;
        runEdit("Preencher campo", PdfChange.forPages(page), d -> d.setFormField(field.name(), value));
        operations.refreshSelection(page);
    }

    public void reloadSelection() {
        if (!selection.isEmpty()) operations.refreshSelection(selection.page());
    }

    public void editSelectedText() {
        if (selection.elements().size() == 1) editElement(selection.page(), selection.elements().getFirst());
    }

    public void commitTextInput() {
        if (textOverlay != null) textOverlay.commit();
    }

    public void finishTextInput(PdfTextOverlay overlay, String text) {
        if (overlay != textOverlay) return;
        textOverlay = null;
        int page = overlay.page();
        String value = text == null ? "" : text.stripTrailing();
        if (overlay.elementId() == null) {
            if (value.isBlank()) { refreshStatus(); return; }
            Rectangle2D.Float anchor = overlay.anchor();
            Rectangle2D.Float bounds = overlay.fixedWidth() ? anchor : new Rectangle2D.Float(anchor.x, anchor.y + anchor.height, 0, 0);
            if (runEdit("Inserir texto", PdfChange.forPages(page), d -> d.addTextBox(page, bounds, value, overlay.style()))) {
                List<PdfPageElement> list = getPageElements(page);
                list.stream().filter(PdfPageElement::textBox).reduce((first, second) -> second)
                        .ifPresent(created -> setSelection(PdfSelection.of(page, List.of(created))));
            }
        } else if (overlay.word()) {
            String id = overlay.elementId();
            runEdit("Editar texto", PdfChange.forPages(page), d -> d.replaceElementText(page, id, value));
        } else {
            String id = overlay.elementId();
            if (value.isBlank()) runEdit("Apagar caixa de texto", PdfChange.forPages(page), d -> d.deleteTarget(page, PdfTarget.of(id)));
            else if (runEdit("Editar caixa de texto", PdfChange.forPages(page), d -> d.updateTextBox(page, id, value, overlay.style())))
                getPageElements(page).stream().filter(element -> element.id().equals(id)).findFirst()
                        .ifPresent(element -> setSelection(PdfSelection.of(page, List.of(element))));
        }
        refreshStatus();
    }

    public void cancelTextInput(PdfTextOverlay overlay) {
        if (overlay == textOverlay) textOverlay = null;
        refreshStatus();
    }

    public void selectAll() { operations.selectAll(); }
    public void deleteSelection() { operations.deleteSelection(); }
    public void eraseSelection() { operations.deleteSelection(); }
    public void eraseRegion(int page, Rectangle2D.Float area) { operations.eraseRegion(page, area); }
    public void eraseShape(int page, java.awt.Shape area) {
        if (isReadOnly() || area == null || area.getBounds2D().isEmpty()) return;
        runEdit("Borracha", PdfChange.forPages(page), d -> d.eraseShape(page, area));
    }
    public void transformSelection(AffineTransform transform, String label) { operations.transformSelection(transform, label); }
    public void moveSelection(float dx, float dy) { operations.moveSelection(dx, dy); }
    public void nudgeSelection(int dx, int dy) { operations.nudgeSelection(dx, dy); }
    public void rotateSelection(double clockwiseDegrees) { operations.rotateSelection(clockwiseDegrees); }
    public void duplicateSelection() { operations.duplicateSelection(); }
    public void arrangeSelection(boolean forward) { operations.arrangeSelection(forward); }
    public void alignSelection(String mode) { operations.alignSelection(mode); }
    public void setPastePoint(int page, Point2D.Float point) { operations.setPastePoint(page, point); }
    public void copySelection() { operations.copySelection(); }
    public void cutSelection() { operations.cutSelection(); }
    public void pasteClipboard() { operations.pasteClipboard(); }
    public boolean hasClipboard() { return operations.hasClipboard(); }
    public void showMessage(String text) { status.message(text); }

    public List<Action> canvasActions(int page) {
        List<Action> actions = new ArrayList<>();
        for (PdfContextMenuProvider provider : providersOf(PdfContextMenuProvider.class)) {
            List<Action> extra = provider.canvasActions(this, page, selection);
            if (extra != null && !extra.isEmpty()) { actions.addAll(extra); actions.add(null); }
        }
        for (String id : List.of("pdf.cut", "pdf.copy", "pdf.paste", "pdf.duplicate", "-", "pdf.editText", "pdf.eraseSelection",
                "-", "pdf.forward", "pdf.backward", "pdf.rotateSelection", "pdf.rotateSelectionLeft", "-", "pdf.selectAll")) {
            if (id.equals("-")) { if (!actions.isEmpty() && actions.getLast() != null) actions.add(null); continue; }
            Action action = commands.get(id);
            if (action != null) actions.add(action);
        }
        return actions;
    }

    public List<Action> thumbnailActions(int page) {
        List<Action> actions = new ArrayList<>();
        for (String id : List.of("pdf.rotate", "pdf.rotateLeft", "-", "pdf.blankPage", "pdf.insertPages", "pdf.extract", "-",
                "pdf.moveUp", "pdf.moveDown", "-", "pdf.delete")) {
            if (id.equals("-")) { actions.add(null); continue; }
            Action action = commands.get(id);
            if (action != null) actions.add(action);
        }
        for (PdfContextMenuProvider provider : providersOf(PdfContextMenuProvider.class)) {
            List<Action> extra = provider.thumbnailActions(this, page);
            if (extra != null && !extra.isEmpty()) { actions.add(null); actions.addAll(extra); }
        }
        return actions;
    }

    private <T> List<T> providersOf(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (PdfProviderEntry entry : providers.values()) if (type.isInstance(entry.provider())) result.add(type.cast(entry.provider()));
        return result;
    }

    public void searchAsync(String query, Consumer<Map<Integer, String>> result) {
        PdfDocument source = document;
        Locale locale = config.locale();
        workers.execute(() -> {
            Map<Integer, String> found = new LinkedHashMap<>();
            try {
                String needle = query.toLowerCase(locale);
                for (int page = 0; page < source.pageCount(); page++) {
                    String text = source.text(page);
                    int index = text.toLowerCase(locale).indexOf(needle);
                    if (index < 0) continue;
                    int start = Math.max(0, index - 20), end = Math.min(text.length(), index + needle.length() + 30);
                    found.put(page, text.substring(start, end).replaceAll("\\s+", " ").strip());
                }
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> reportError(error)); }
            SwingUtilities.invokeLater(() -> { if (!closed) result.accept(found); });
        });
    }

    public void pageTextAsync(int page, Consumer<String> result) {
        PdfDocument source = document;
        workers.execute(() -> {
            String text;
            try { text = source.canExtractContent() ? source.text(page) : "Extração de texto não permitida neste PDF."; }
            catch (Throwable error) { text = ""; }
            String value = text;
            SwingUtilities.invokeLater(() -> { if (!closed) result.accept(value); });
        });
    }

    public void newDocument() {
        ensureOpen();
        if (isDirty() && !getDialogs().confirm(this, "Novo PDF", "Descartar alterações não salvas?")) return;
        try {
            replace(backend().create());
            currentFile = null;
            fileTime = null;
            currentPage = 0;
            stateId = ++nextStateId;
            savedStateId = stateId;
            history.clear();
            documentReplaced();
        } catch (IOException error) { reportError(error); }
    }

    public void openWithDialog() {
        if (isDirty() && !getDialogs().confirm(this, "Abrir PDF", "Descartar alterações não salvas?")) return;
        getFiles().chooseOpen(this).ifPresent(path -> openInteractive(path, null));
    }

    private void openInteractive(Path path, char[] password) {
        load(path, password, true).completion().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (password != null) Arrays.fill(password, '\0');
            if (error == null) return;
            if (backend().isPasswordError(error))
                getDialogs().password(this, "Abrir PDF", "Senha do PDF:").ifPresent(value -> openInteractive(path, value));
            else reportError(error);
        }));
    }

    public void saveCurrent() {
        if (currentFile == null || document.hasSignatures()) saveWithDialog();
        else save(currentFile).completion().whenComplete((ignored, error) -> { if (error != null) SwingUtilities.invokeLater(() -> reportError(error)); });
    }

    public void saveWithDialog() {
        getFiles().chooseSave(this).ifPresent(path -> {
            try {
                save(path).completion().whenComplete((ignored, error) -> {
                    if (error != null) SwingUtilities.invokeLater(() -> reportError(error));
                });
            } catch (RuntimeException error) { reportError(error); }
        });
    }

    public PdfTask<Void> open(Path source) { return open(source, null); }
    public PdfTask<Void> open(Path source, char[] password) {
        if (isDirty()) throw new IllegalStateException("Há alterações não salvas");
        return load(source, password, false);
    }

    private PdfTask<Void> load(Path source, char[] password, boolean discard) {
        ensureOpen();
        Objects.requireNonNull(source);
        commitTextInput();
        PdfTask<Void> task = new PdfTask<>();
        long expected = stateId;
        PdfBackendProvider backend = backend();
        char[] secret = password == null ? null : password.clone();
        task.attach(workers.submit(() -> {
            try {
                PdfDocument loaded = backend.open(source, secret);
                FileTime modified = Files.getLastModifiedTime(source);
                SwingUtilities.invokeLater(() -> {
                    if (task.isCancelled() || closed || (!discard && stateId != expected)) {
                        try { loaded.close(); } catch (IOException ignored) { }
                        return;
                    }
                    try { replace(loaded); } catch (IOException error) { reportError(error); }
                    currentFile = source.toAbsolutePath();
                    fileTime = modified;
                    stateId = ++nextStateId;
                    savedStateId = stateId;
                    history.clear();
                    currentPage = 0;
                    documentReplaced();
                    task.complete(null);
                });
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> task.fail(error)); }
            finally { if (secret != null) Arrays.fill(secret, '\0'); }
        }));
        return task;
    }

    public PdfTask<Void> save(Path destination) {
        ensureOpen();
        Objects.requireNonNull(destination);
        commitTextInput();
        if (document.hasSignatures() && currentFile != null && currentFile.equals(destination.toAbsolutePath()))
            throw new IllegalStateException("PDF assinado: salve uma nova cópia; a assinatura anterior perderá validade");
        PdfTask<Void> task = new PdfTask<>();
        long savedState = stateId;
        Path target = destination.toAbsolutePath();
        PdfDocument source = document;
        Path known = currentFile;
        FileTime knownTime = fileTime;
        task.attach(workers.submit(() -> {
            Path temporary = null;
            try {
                if (target.equals(known) && knownTime != null && Files.exists(target)
                        && !knownTime.equals(Files.getLastModifiedTime(target)))
                    throw new IOException("O arquivo foi alterado fora do editor");
                temporary = Files.createTempFile(target.getParent(), ".swingtools-pdf-", ".tmp");
                source.save(temporary);
                if (!task.beginCommit()) return;
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                FileTime modified = Files.getLastModifiedTime(target);
                SwingUtilities.invokeLater(() -> {
                    if (!closed) {
                        currentFile = target;
                        fileTime = modified;
                        savedStateId = savedState;
                        status.message("Salvo em " + target.getFileName());
                        refreshStatus();
                    }
                    task.complete(null);
                });
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> task.fail(error)); }
            finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
        }));
        return task;
    }

    public void edit(String label, PdfEdit operation) throws IOException { edit(label, PdfChange.all(), operation); }
    public void edit(String label, PdfChange change, PdfEdit operation) throws IOException { edit(label, operation, ignored -> change); }

    private void edit(String label, PdfEdit operation, IntFunction<PdfChange> change) throws IOException {
        ensureOpen();
        if (isReadOnly()) throw new IllegalStateException("Editor somente leitura ou PDF protegido");
        byte[] before = document.snapshot();
        int countBefore = document.pageCount();
        try { operation.apply(document); }
        catch (Throwable error) {
            replace(backend().restore(before));
            refreshPages();
            renderer.invalidateAll();
            invalidateElements(null);
            canvas.repaint();
            if (error instanceof IOException io) throw io;
            if (error instanceof RuntimeException runtime) throw runtime;
            throw new IOException(error);
        }
        PdfChange applied = change.apply(countBefore);
        history.record(new PdfHistoryEntry(before, stateId, label, applied, document.pageCount()), config.historyLimit());
        stateId = ++nextStateId;
        applyChange(applied);
        firePropertyChange("edit", null, label);
    }

    private boolean runEdit(String label, PdfChange change, PdfEdit operation) {
        try {
            edit(label, change, operation);
            return true;
        } catch (IOException | RuntimeException error) {
            reportError(error);
            return false;
        }
    }

    private void refreshPages() {
        PdfDocument current = document;
        int count = current.pageCount();
        PdfPageGeometry[] next = new PdfPageGeometry[count];
        for (int page = 0; page < count; page++)
            next[page] = new PdfPageGeometry(current.pageX(page), current.pageY(page), current.pageWidth(page),
                    current.pageHeight(page), current.pageRotation(page), 1);
        pages = next;
        modifiable = current.canModify();
        formFields = !current.formFields().isEmpty();
    }

    private void applyChange(PdfChange change) {
        refreshPages();
        if (change.structural()) selectedPages.clear();
        if (change.structural()) {
            renderer.remap(change.mapping());
            invalidateElements(null);
            int[] pages = change.pages();
            if (pages != null) renderer.invalidate(pages);
        } else if (change.affectsAll()) {
            renderer.invalidateAll();
            invalidateElements(null);
        } else {
            renderer.invalidate(change.pages());
            invalidateElements(change.pages());
        }
        currentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
        relayout();
        if (change.structural() || change.affectsAll()) sidebar.pagesChanged();
        else sidebar.repaintThumbnails();
        if (!selection.isEmpty() && (selection.page() >= getPageCount() || change.structural())) setSelection(PdfSelection.empty());
        commands.refresh();
        refreshStatus();
        canvas.repaint();
    }

    private void invalidateElements(int[] pages) {
        elementVersion++;
        if (pages == null) elements.clear();
        else for (int page : pages) elements.remove(page);
    }

    private void documentReplaced() {
        refreshPages();
        selectedPages.clear();
        renderer.clear();
        invalidateElements(null);
        selection = PdfSelection.empty();
        if (textOverlay != null) { PdfTextOverlay overlay = textOverlay; textOverlay = null; overlay.cancel(); }
        currentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
        relayout();
        scroll.getViewport().setViewPosition(new Point(0, 0));
        sidebar.pagesChanged();
        sidebar.currentPageChanged();
        if (defaultRibbon != null) defaultRibbon.setContextualTabs(List.of());
        commands.refresh();
        refreshStatus();
        canvas.repaint();
    }

    public void undo() {
        ensureOpen();
        if (textOverlay != null) { textOverlay.cancel(); return; }
        if (isReadOnly() || !history.canUndo()) return;
        try {
            PdfHistoryEntry entry = history.popUndo();
            history.pushRedo(new PdfHistoryEntry(document.snapshot(), stateId, entry.label(), entry.change(), entry.pageCount()));
            replace(backend().restore(entry.snapshot()));
            stateId = entry.state();
            setSelection(PdfSelection.empty());
            applyChange(entry.change().inverse(document.pageCount()));
            status.message("Desfeito: " + entry.label());
        } catch (IOException error) { reportError(error); }
    }

    public void redo() {
        ensureOpen();
        if (isReadOnly() || !history.canRedo()) return;
        try {
            PdfHistoryEntry entry = history.popRedo();
            history.pushUndo(new PdfHistoryEntry(document.snapshot(), stateId, entry.label(), entry.change(), entry.pageCount()));
            replace(backend().restore(entry.snapshot()));
            stateId = entry.state();
            setSelection(PdfSelection.empty());
            applyChange(entry.change());
            status.message("Refeito: " + entry.label());
        } catch (IOException error) { reportError(error); }
    }

    private void replace(PdfDocument next) throws IOException {
        PdfDocument old = document;
        document = next;
        if (old != null) old.close();
    }

    public void rotatePage(int page, int degrees) throws IOException { edit("Girar página", PdfChange.forPages(page), d -> d.rotate(page, degrees)); }
    public void removePage(int page) throws IOException {
        int count = getPageCount();
        edit("Excluir página", PdfChange.structure(PdfChange.removed(count, page)), d -> d.remove(page));
        setCurrentPage(Math.min(page, getPageCount() - 1));
    }
    public void movePage(int source, int destination) throws IOException {
        int count = getPageCount();
        if (source == destination) return;
        edit("Mover página", PdfChange.structure(PdfChange.moved(count, source, destination)), d -> d.move(source, destination));
        currentPage = destination;
        sidebar.currentPageChanged();
        if (config.viewMode() == PdfViewMode.CONTINUOUS) scrollToPage(destination); else relayout();
        refreshStatus();
    }
    public void insertPages(Path source, int destination) throws IOException {
        edit("Inserir páginas", d -> d.insertPages(source, destination),
                before -> PdfChange.structure(PdfChange.inserted(before, destination, document.pageCount() - before)));
    }
    public void insertBlankPage(int destination) throws IOException {
        int reference = Math.max(0, Math.min(currentPage, getPageCount() - 1));
        float width = getPageWidth(reference), height = getPageHeight(reference);
        edit("Inserir página em branco", d -> d.insertBlankPage(destination, width, height),
                before -> PdfChange.structure(PdfChange.inserted(before, destination, 1)));
        setCurrentPage(destination);
    }
    public void addText(int page, String text, float x, float y, float size) throws IOException { edit("Inserir texto", PdfChange.forPages(page), d -> d.addText(page, text, x, y, size)); }
    public void addTextBox(int page, Rectangle2D.Float bounds, String text, PdfTextStyle style) throws IOException {
        edit("Inserir texto", PdfChange.forPages(page), d -> d.addTextBox(page, bounds, text, style));
    }
    public void addImage(int page, Path image, float x, float y, float width, float height) throws IOException {
        edit("Inserir imagem", PdfChange.forPages(page), d -> d.addImage(page, image, x, y, width, height));
    }
    public void addImageStamp(int page, BufferedImage image, Rectangle2D.Float bounds) throws IOException {
        edit("Inserir imagem", PdfChange.forPages(page), d -> d.addImageStamp(page, image, bounds));
    }
    public void addShape(int page, PdfShapeKind kind, Rectangle2D.Float bounds) throws IOException {
        PdfShapeStyle style = shapeStyle;
        edit("Inserir forma", PdfChange.forPages(page), d -> d.addShape(page, kind, bounds, style));
    }
    public void addLine(int page, float x1, float y1, float x2, float y2, boolean arrow) throws IOException {
        PdfShapeStyle style = shapeStyle;
        edit(arrow ? "Inserir seta" : "Inserir linha", PdfChange.forPages(page), d -> d.addLine(page, x1, y1, x2, y2, arrow, style));
    }
    public List<String> getImageResources(int page) throws IOException { return document.imageResources(page); }
    public void replaceImageResource(int page, String resourceName, Path image) throws IOException {
        edit("Substituir imagem", PdfChange.forPages(page), d -> d.replaceImageResource(page, resourceName, image));
    }
    public void extractPages(List<Integer> pages, Path destination) throws IOException {
        ensureOpen();
        document.extractPages(pages, destination);
        status.message((pages.size() == 1 ? "1 página salva em " : pages.size() + " páginas salvas em ") + destination.getFileName());
    }

    public List<Integer> getSelectedPages() {
        List<Integer> valid = selectedPages.stream().filter(page -> page >= 0 && page < pages.length).sorted().toList();
        return valid.isEmpty() ? List.of(currentPage) : valid;
    }

    public boolean isPageSelected(int page) {
        return selectedPages.contains(page) || selectedPages.isEmpty() && page == currentPage;
    }

    public void selectPages(java.util.Collection<Integer> value) {
        selectedPages.clear();
        for (int page : value) if (page >= 0 && page < pages.length) selectedPages.add(page);
        sidebar.repaintThumbnails();
        refreshStatus();
    }

    public void clickPage(int page, boolean toggle, boolean range) {
        if (page < 0 || page >= pages.length) return;
        if (toggle) {
            if (selectedPages.isEmpty()) selectedPages.add(currentPage);
            if (!selectedPages.remove(page)) selectedPages.add(page);
            if (selectedPages.isEmpty()) selectedPages.add(page);
            pageAnchor = page;
        } else if (range) {
            int from = Math.min(pageAnchor, page), to = Math.max(pageAnchor, page);
            selectedPages.clear();
            for (int index = from; index <= to; index++) selectedPages.add(index);
        } else {
            selectedPages.clear();
            pageAnchor = page;
        }
        setCurrentPage(page);
        sidebar.repaintThumbnails();
        refreshStatus();
    }

    public void extractPages(int first, int last, Path destination) throws IOException {
        ensureOpen();
        document.extractPages(first, last, destination);
    }
    public String getTitle() { return document.title(); }
    public void setTitle(String title) throws IOException { edit("Alterar título", PdfChange.forPages(), d -> d.setTitle(title)); }
    public boolean hasSignatures() { return document.hasSignatures(); }
    public void print() throws IOException { ensureOpen(); document.print(); }
    public boolean replaceText(int page, String original, String replacement, Rectangle2D.Float area) throws IOException {
        boolean[] reconstructed = {false};
        edit("Substituir texto", PdfChange.forPages(page), d -> reconstructed[0] = d.replaceText(page, original, replacement,
                area, config.pageReconstructionEnabled()));
        if (reconstructed[0]) firePropertyChange("pageReconstructed", false, true);
        return reconstructed[0];
    }
    public void addNote(int page, String text, float x, float y) throws IOException { edit("Inserir nota", PdfChange.forPages(page), d -> d.addNote(page, text, x, y)); }
    public void addSquare(int page, float x, float y, float width, float height) throws IOException {
        edit("Inserir forma", PdfChange.forPages(page), d -> d.addSquare(page, x, y, width, height));
    }
    public void addInk(int page, float[] points) throws IOException { edit("Desenhar", PdfChange.forPages(page), d -> d.addInk(page, points)); }
    public void addInk(int page, float[] points, PdfShapeStyle style) throws IOException {
        edit("Desenhar", PdfChange.forPages(page), d -> d.addInk(page, points, style));
    }
    public void addVisualSignature(int page, Path image, float x, float y, float width, float height) throws IOException {
        edit("Assinatura visual", PdfChange.forPages(page), d -> d.addImage(page, image, x, y, width, height));
    }
    public void addHighlight(int page, float x, float y, float width, float height) throws IOException {
        edit("Destacar", PdfChange.forPages(page), d -> d.addHighlight(page, x, y, width, height));
    }
    public List<String> getFormFields() { return document.formFields(); }
    public void setFormField(String name, String value) throws IOException { edit("Preencher campo", d -> d.setFormField(name, value)); }
    public void addTextField(int page, String name, float x, float y, float width, float height) throws IOException {
        edit("Criar campo", PdfChange.forPages(page), d -> d.addTextField(page, name, x, y, width, height));
    }
    public void addChoiceField(int page, String name, List<String> options, float x, float y, float width, float height) throws IOException {
        edit("Criar lista", PdfChange.forPages(page), d -> d.addChoiceField(page, name, options, x, y, width, height));
    }
    public void addCheckBox(int page, String name, float x, float y, float size) throws IOException {
        edit("Criar caixa", PdfChange.forPages(page), d -> d.addCheckBox(page, name, x, y, size));
    }
    public void addRadioGroup(int page, String name, List<String> options, float x, float y, float size, float gap) throws IOException {
        edit("Criar opções", PdfChange.forPages(page), d -> d.addRadioGroup(page, name, options, x, y, size, gap));
    }
    public String getPageText(int page) throws IOException { return document.text(page); }

    public List<Integer> findPages(String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        List<Integer> found = new ArrayList<>();
        for (int page = 0; page < document.pageCount(); page++)
            if (document.text(page).toLowerCase(config.locale()).contains(query.toLowerCase(config.locale()))) found.add(page);
        return List.copyOf(found);
    }

    public PdfTask<PdfOcrResult> recognizePage(int page, String languages, boolean addSearchableLayer) {
        ensureOpen();
        if (!document.canExtractContent()) throw new IllegalStateException("OCR não permitido neste PDF protegido");
        if (addSearchableLayer && isReadOnly()) throw new IllegalStateException("PDF somente leitura");
        PdfOcrProvider provider = active(PdfOcrProvider.class)
                .orElseThrow(() -> new IllegalStateException("Nenhum provider de OCR registrado"));
        if (page < 0 || page >= getPageCount()) throw new IndexOutOfBoundsException(page);
        PdfTask<PdfOcrResult> task = new PdfTask<>();
        long expected = stateId;
        PdfDocument source = document;
        float dpi = 300;
        task.attach(workers.submit(() -> {
            try {
                task.progress(5);
                BufferedImage image = source.render(page, dpi);
                PdfOcrResult result = provider.recognize(image, languages, task::progress);
                SwingUtilities.invokeLater(() -> {
                    if (task.isCancelled() || closed) return;
                    if (expected != stateId) { task.fail(new IllegalStateException("Documento alterado durante o OCR")); return; }
                    try {
                        if (addSearchableLayer) edit("Camada OCR", PdfChange.forPages(page), d -> d.addOcrLayer(page, result, dpi));
                        task.complete(result);
                    } catch (Throwable error) { task.fail(error); }
                });
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> task.fail(error)); }
        }));
        return task;
    }

    public void addOcrLayer(int page, PdfOcrResult corrected, float dpi) throws IOException {
        Objects.requireNonNull(corrected);
        if (!Float.isFinite(dpi) || dpi <= 0) throw new IllegalArgumentException("DPI inválido");
        edit("Camada OCR", PdfChange.forPages(page), d -> d.addOcrLayer(page, corrected, dpi));
    }

    public PdfTask<Void> sign(Path destination, Path pkcs12, char[] password, String reason) {
        ensureOpen();
        if (isReadOnly() || isDirty() || currentFile == null) throw new IllegalStateException("Salve o PDF antes de assinar");
        Path source = currentFile;
        if (source.equals(destination.toAbsolutePath())) throw new IllegalArgumentException("Assine em uma nova cópia");
        PdfSignatureProvider signer = active(PdfSignatureProvider.class)
                .orElseThrow(() -> new IllegalStateException("Nenhum provider de assinatura registrado"));
        PdfTask<Void> task = new PdfTask<>();
        task.attach(workers.submit(() -> {
            try {
                signer.sign(source, destination, pkcs12, password, reason);
                SwingUtilities.invokeLater(() -> task.complete(null));
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> task.fail(error)); }
        }));
        return task;
    }

    public PdfTask<List<PdfSignatureValidation>> validateSignatures() {
        ensureOpen();
        if (currentFile == null || isDirty()) throw new IllegalStateException("Salve o PDF antes de validar assinaturas");
        PdfSignatureProvider signer = active(PdfSignatureProvider.class)
                .orElseThrow(() -> new IllegalStateException("Nenhum provider de assinatura registrado"));
        PdfTrustProvider trust = active(PdfTrustProvider.class)
                .orElseThrow(() -> new IllegalStateException("Nenhum provider de confiança registrado"));
        Path source = currentFile;
        PdfTask<List<PdfSignatureValidation>> task = new PdfTask<>();
        task.attach(workers.submit(() -> {
            try {
                List<PdfSignatureValidation> result = signer.validate(source, trust);
                SwingUtilities.invokeLater(() -> task.complete(result));
            } catch (Throwable error) { SwingUtilities.invokeLater(() -> task.fail(error)); }
        }));
        return task;
    }

    public PdfProviderRegistration addProvider(PdfProvider provider) {
        ensureOpen();
        Objects.requireNonNull(provider);
        String id = provider.id();
        if (id == null || id.isBlank() || providers.containsKey(id))
            throw new IllegalArgumentException("Duplicate or empty provider id: " + id);
        if (provider instanceof PdfElementFactory factory && (factories.containsKey(factory.id()) || commands.contains(factory.commandId())))
            throw new IllegalArgumentException("Duplicate element factory: " + factory.id());
        PdfProviderEntry entry = new PdfProviderEntry(provider, new ArrayList<>(), ++registrationOrder);
        providers.put(id, entry);
        try {
            if (provider instanceof PdfCommandProvider commandProvider)
                entry.hooks().add(commands.addCustom(Map.copyOf(commandProvider.commands(this))));
            if (provider instanceof PdfElementFactory factory) entry.hooks().add(installFactory(factory));
            if (status != null) {
                if (provider instanceof PdfToolbarProvider toolbar) entry.hooks().add(installToolbar(toolbar));
                if (provider instanceof PdfRibbonContributor contributor) entry.hooks().add(installContribution(contributor));
            }
            entry.hooks().add(Objects.requireNonNull(provider.attach(this)));
        } catch (RuntimeException error) {
            providers.remove(id);
            unwind(entry, error);
            throw error;
        }
        providersChanged();
        boolean[] removed = {false};
        return () -> {
            if (removed[0]) return;
            removed[0] = true;
            removeProvider(id);
        };
    }

    private PdfProviderRegistration installFactory(PdfElementFactory factory) {
        factories.put(factory.id(), factory);
        PdfAction action = new PdfAction(factory.commandId(), factory.title(), () -> selectTool(factory.id()), this::reportError)
                .icon(factory.icon()).tip(factory.tip()).selected(() -> factory.id().equals(activeFactory)).edits();
        PdfProviderRegistration command = commands.put(factory.commandId(), action);
        PdfProviderRegistration item = defaultRibbon == null ? PdfProviderRegistration.none() : installFactoryItem(factory);
        return () -> {
            item.close();
            command.close();
            factories.remove(factory.id());
            if (factory.id().equals(activeFactory)) {
                activeFactory = null;
                activeTool = TOOL_SELECT;
            }
        };
    }

    private PdfProviderRegistration installFactoryItem(PdfElementFactory factory) {
        if (!factory.ribbonItem() || defaultRibbon == null) return PdfProviderRegistration.none();
        String groupId = "factories." + factory.ribbonGroup();
        defaultRibbon.addItem(factory.ribbonTab(), groupId, factory.ribbonGroup(),
                factory.large() ? PdfRibbonItem.large(factory.commandId()) : PdfRibbonItem.small(factory.commandId()));
        return () -> defaultRibbon.removeItem(factory.ribbonTab(), groupId, factory.commandId());
    }

    private PdfProviderRegistration installToolbar(PdfToolbarProvider provider) {
        JComponent toolbar = Objects.requireNonNull(provider.createToolbar(this));
        providerBar.add(toolbar);
        providerBar.setVisible(true);
        providerBar.revalidate();
        return () -> {
            providerBar.remove(toolbar);
            providerBar.setVisible(providerBar.getComponentCount() > 0);
            providerBar.revalidate();
            providerBar.repaint();
        };
    }

    private PdfProviderRegistration installContribution(PdfRibbonContributor contributor) {
        if (defaultRibbon == null) return PdfProviderRegistration.none();
        PdfRibbonGroup group = contributor.ribbonGroup(this);
        defaultRibbon.addGroup(contributor.tab(), group);
        return () -> defaultRibbon.removeGroup(contributor.tab(), group.id());
    }

    public void removeProvider(String id) {
        ensureOpen();
        PdfProviderEntry entry = providers.remove(id);
        if (entry == null) return;
        unwind(entry, null);
        providersChanged();
    }

    private void unwind(PdfProviderEntry entry, RuntimeException failure) {
        List<PdfProviderRegistration> hooks = new ArrayList<>(entry.hooks());
        Collections.reverse(hooks);
        for (PdfProviderRegistration hook : hooks) {
            try { hook.close(); }
            catch (RuntimeException error) {
                if (failure != null) failure.addSuppressed(error);
                else reportError(error);
            }
        }
    }

    private void providersChanged() {
        commands.refresh();
        if (defaultRibbon != null) defaultRibbon.refresh();
        if (sidebar != null) sidebar.setTools(toolActions());
        if (status != null) refreshStatus();
    }

    private List<Action> toolActions() {
        List<Action> tools = new ArrayList<>();
        for (String id : List.of(TOOL_SELECT, TOOL_AREA, TOOL_ERASER)) {
            Action action = commands.get(id);
            if (action != null) tools.add(action);
        }
        tools.add(null);
        for (PdfElementFactory factory : factories.values()) {
            Action action = commands.get(factory.commandId());
            if (action != null) tools.add(action);
        }
        return tools;
    }

    private PdfBackendProvider backend() {
        return active(PdfBackendProvider.class).orElseThrow(() -> new IllegalStateException("No PDF backend"));
    }

    public <T extends PdfProvider> Optional<T> active(Class<T> type) {
        return providers.values().stream().filter(entry -> type.isInstance(entry.provider()))
                .max(Comparator.<PdfProviderEntry>comparingInt(entry -> entry.provider().priority()).thenComparingLong(PdfProviderEntry::order))
                .map(entry -> type.cast(entry.provider()));
    }

    private void refreshStatus() {
        if (status == null || document == null) return;
        String tool;
        PdfElementFactory factory = getActiveFactory();
        if (textOverlay != null) tool = "Digitando texto — Ctrl+Enter confirma, Esc cancela";
        else if (factory != null) tool = factory.title() + " — " + factory.tip();
        else tool = switch (activeTool) {
            case TOOL_AREA -> "Seleção de área";
            case TOOL_ERASER -> eraserMode == PdfEraserMode.BRUSH
                    ? String.format(Locale.ROOT, "Borracha (pincel %.0f pt) — passe sobre o que quer apagar", eraserSize)
                    : "Borracha (retângulo) — arraste sobre a área a apagar";
            default -> "Selecionar";
        };
        String info = null;
        if (!selection.isEmpty()) {
            Rectangle2D.Float bounds = selection.bounds();
            int count = selection.elements().size();
            info = (count == 0 ? "Área" : count == 1 ? "1 item" : count + " itens")
                    + (bounds == null ? "" : String.format(Locale.ROOT, "  •  %.0f × %.0f pt", bounds.width, bounds.height));
        }
        if (info == null && selectedPages.size() > 1) info = selectedPages.size() + " páginas selecionadas";
        status.update(currentPage, getPageCount(), tool, info, isDirty(), config.zoom());
    }

    @Override protected void onThemeChanged() {
        if (canvas == null) return;
        UiTokens.refresh();
        providerBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()));
        if (defaultRibbon != null) defaultRibbon.applyTheme();
        sidebar.applyTheme();
        status.applyTheme();
        canvas.repaint();
        repaint();
    }

    private void ensureOpen() { if (closed) throw new IllegalStateException("PdfEditor is closed"); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        workers.shutdownNow();
        renderer.close();
        for (PdfProviderEntry entry : List.copyOf(providers.values())) unwind(entry, null);
        providers.clear();
        try { document.close(); } catch (IOException error) { errorHandler.accept(error); }
    }
}
