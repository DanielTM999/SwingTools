package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.editor.sheet.api.CalcMode;
import dtm.stools.component.panels.editor.sheet.api.ProviderRegistration;
import dtm.stools.component.panels.editor.sheet.api.SheetCellChangeListener;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.api.SheetSelectionListener;
import dtm.stools.component.panels.editor.sheet.api.SheetSession;
import dtm.stools.component.panels.editor.sheet.api.SheetSessionEvent;
import dtm.stools.component.panels.editor.sheet.api.SheetTask;
import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.CalcListener;
import dtm.stools.component.panels.editor.sheet.command.SheetChange;
import dtm.stools.component.panels.editor.sheet.command.SheetCommand;
import dtm.stools.component.panels.editor.sheet.config.SheetEditorConfig;
import dtm.stools.component.panels.editor.sheet.config.SheetServices;
import dtm.stools.component.panels.editor.sheet.controller.CellEditController;
import dtm.stools.component.panels.editor.sheet.controller.ClipboardController;
import dtm.stools.component.panels.editor.sheet.controller.DataController;
import dtm.stools.component.panels.editor.sheet.controller.FileController;
import dtm.stools.component.panels.editor.sheet.controller.FormatController;
import dtm.stools.component.panels.editor.sheet.controller.NavigationController;
import dtm.stools.component.panels.editor.sheet.controller.ObjectController;
import dtm.stools.component.panels.editor.sheet.controller.ReviewController;
import dtm.stools.component.panels.editor.sheet.controller.SheetCommandCatalog;
import dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry;
import dtm.stools.component.panels.editor.sheet.controller.SheetPopups;
import dtm.stools.component.panels.editor.sheet.controller.StructureController;
import dtm.stools.component.panels.editor.sheet.data.ConditionalEvaluator;
import dtm.stools.component.panels.editor.sheet.data.FilterEngine;
import dtm.stools.component.panels.editor.sheet.data.ValidationEvaluator;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Slicer;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;
import dtm.stools.component.panels.editor.sheet.provider.SheetCellRendererProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetCollaborationEvent;
import dtm.stools.component.panels.editor.sheet.provider.SheetCollaborationProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetConditionalRuleProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetContextMenuContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetExternalDataProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetFunctionProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetRibbonContributor;
import dtm.stools.component.panels.editor.sheet.provider.SheetToolbarContributor;
import dtm.stools.component.panels.editor.sheet.render.ChartData;
import dtm.stools.component.panels.editor.sheet.render.SheetRenderer;
import dtm.stools.component.panels.editor.sheet.ui.RibbonGroup;
import dtm.stools.component.panels.editor.sheet.ui.RibbonItem;
import dtm.stools.component.panels.editor.sheet.ui.SheetCanvas;
import dtm.stools.component.panels.editor.sheet.ui.SheetFormulaBar;
import dtm.stools.component.panels.editor.sheet.ui.SheetGeometry;
import dtm.stools.component.panels.editor.sheet.ui.SheetRibbon;
import dtm.stools.component.panels.editor.sheet.ui.SheetStatusBar;
import dtm.stools.component.panels.editor.sheet.ui.SheetTabBar;
import dtm.stools.configs.UiTokens;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class SheetEditor extends BlockingPanel implements AutoCloseable {
    public enum ExportFormat { XLSX, ODS, CSV, TSV, HTML, PDF, TXT }

    private final SheetServices services;
    private final SheetSession session;
    private final CalcEngine engine;
    private final ConditionalEvaluator conditional;
    private final ValidationEvaluator validation;
    private final SheetPopups popups;
    private final SheetCommandRegistry commands;
    private final NavigationController navigation;
    private final CellEditController editing;
    private final ClipboardController clipboard;
    private final FormatController format;
    private final StructureController structure;
    private final DataController data;
    private final ObjectController objects;
    private final ReviewController review;
    private final FileController files;
    private final SheetCanvas canvas;
    private final SheetFormulaBar formulaBar;
    private final SheetTabBar tabBar;
    private final SheetStatusBar statusBar;
    private final JScrollBar horizontal = new JScrollBar(JScrollBar.HORIZONTAL);
    private final JScrollBar vertical = new JScrollBar(JScrollBar.VERTICAL);
    private final JPanel north = new JPanel();
    private final JPanel ribbonHost = new JPanel(new BorderLayout());
    private final JPanel providerBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private final JLabel diagnostics = new JLabel();
    private final JPanel tabRow = new JPanel(new BorderLayout());
    private final JPanel bottom = new JPanel(new BorderLayout());
    private final List<SheetProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<String, List<AutoCloseable>> providerHooks = new HashMap<>();
    private final List<SheetCellChangeListener> cellListeners = new CopyOnWriteArrayList<>();
    private final List<SheetSelectionListener> selectionListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Double> zooms = new HashMap<>();
    private final Map<String, long[]> scrolls = new HashMap<>();
    private final List<ProviderRegistration> internal = new ArrayList<>();
    private final Timer statusTimer;
    private Timer recoveryTimer;
    private JComponent ribbon;
    private SheetRibbon defaultRibbon;
    private SheetEditorConfig config;
    private ValueParser parser;
    private Consumer<Throwable> errorHandler;
    private boolean updatingScroll, closed, applyingRemote;

    public SheetEditor() { this(SheetEditorConfig.defaults(), SheetServices.defaults()); }
    public SheetEditor(SheetEditorConfig config) { this(config, SheetServices.defaults()); }

    public SheetEditor(SheetEditorConfig config, SheetServices services) {
        this.config = Objects.requireNonNull(config);
        this.services = Objects.requireNonNull(services);
        session = new SheetSession(config.historyLimit());
        engine = services.calc();
        engine.setLocale(config.locale());
        engine.setIteration(config.iteration());
        engine.attach(session.getWorkbook());
        engine.setMode(config.calcMode());
        parser = new ValueParser(config.locale(), false);
        conditional = new ConditionalEvaluator(engine);
        validation = new ValidationEvaluator(engine);
        popups = new SheetPopups(this);
        errorHandler = error -> {
            System.getLogger(SheetEditor.class.getName()).log(System.Logger.Level.DEBUG, "Sheet editor error", error);
            firePropertyChange("error", null, error);
            popups.error(error);
        };
        session.setErrorHandler(this::reportError);
        commands = new SheetCommandRegistry(this);
        navigation = new NavigationController(this);
        editing = new CellEditController(this);
        clipboard = new ClipboardController(this);
        format = new FormatController(this);
        structure = new StructureController(this);
        data = new DataController(this);
        objects = new ObjectController(this);
        review = new ReviewController(this);
        files = new FileController(this);
        internal.add(session.addChangeListener(engine::onChange));
        internal.add(session.addChangeListener(this::changed));
        internal.add(session.addListener(this::sessionEvent));
        internal.add(engine.addListener(new CalcListener() {
            @Override public void valuesChanged(Map<String, List<CellRange>> changed) { valuesRecalculated(changed); }
        }));
        setLayout(new BorderLayout());
        canvas = services.uiFactory().createCanvas(this);
        canvas.setDark(UiTokens.isDarkTheme());
        formulaBar = services.uiFactory().createFormulaBar(this);
        tabBar = services.uiFactory().createTabBar(this);
        statusBar = services.uiFactory().createStatusBar(this);
        editing.install();
        SheetCommandCatalog.registerAll(this, commands);
        ribbon = services.uiFactory().createRibbon(this);
        defaultRibbon = ribbon instanceof SheetRibbon r ? r : null;
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        ribbonHost.add(ribbon, BorderLayout.CENTER);
        providerBar.setVisible(false);
        diagnostics.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        diagnostics.setVisible(false);
        north.add(ribbonHost);
        north.add(providerBar);
        north.add(diagnostics);
        north.add(formulaBar);
        for (Component c : north.getComponents()) ((JComponent) c).setAlignmentX(0f);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(canvas, BorderLayout.CENTER);
        center.add(vertical, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
        tabRow.add(tabBar, BorderLayout.CENTER);
        tabRow.add(horizontal, BorderLayout.EAST);
        bottom.add(tabRow, BorderLayout.NORTH);
        bottom.add(statusBar, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        horizontal.setUnitIncrement(24);
        vertical.setUnitIncrement(20);
        horizontal.addAdjustmentListener(e -> { if (!updatingScroll) canvas.setScroll(horizontal.getValue(), canvas.scrollY()); });
        vertical.addAdjustmentListener(e -> { if (!updatingScroll) canvas.setScroll(canvas.scrollX(), vertical.getValue()); });
        canvas.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { canvas.invalidateGeometry(); updateScrollBars(); editing.reposition(); }
        });
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                horizontal.setPreferredSize(new Dimension(Math.max(160, (int) (getWidth() * 0.42)), horizontal.getPreferredSize().height));
                tabRow.revalidate();
            }
        });
        statusTimer = new Timer(90, e -> refreshStatusNow());
        statusTimer.setRepeats(false);
        applyConfig();
        refreshAll();
    }

    public SheetServices getServices() { return services; }
    public SheetSession getSession() { return session; }
    public CalcEngine getEngine() { return engine; }
    public SheetEditorConfig getConfig() { return config; }
    public SheetCanvas getCanvas() { return canvas; }
    public SheetFormulaBar getFormulaBar() { return formulaBar; }
    public SheetTabBar getTabBar() { return tabBar; }
    public SheetStatusBar getStatusBar() { return statusBar; }
    public JComponent getRibbon() { return ribbon; }
    public SheetRibbon getDefaultRibbon() { return defaultRibbon; }
    public Map<String, Action> getCommands() { return commands.actions(); }
    public SheetCommandRegistry commandRegistry() { return commands; }
    public SheetPopups popups() { return popups; }
    public NavigationController navigation() { return navigation; }
    public CellEditController editing() { return editing; }
    public ClipboardController clipboard() { return clipboard; }
    public FormatController format() { return format; }
    public StructureController structure() { return structure; }
    public DataController data() { return data; }
    public ObjectController objects() { return objects; }
    public ReviewController review() { return review; }
    public FileController files() { return files; }

    public void setConfig(SheetEditorConfig value) {
        SheetEditorConfig old = config;
        config = Objects.requireNonNull(value);
        if (!old.locale().equals(value.locale())) { engine.setLocale(value.locale()); parser = new ValueParser(value.locale(), session.getWorkbook().properties().date1904()); }
        if (old.zoom() != value.zoom()) zooms.clear();
        applyConfig();
        refreshAll();
        firePropertyChange("config", old, value);
    }

    private void applyConfig() {
        session.setHistoryLimit(config.historyLimit());
        engine.setIteration(config.iteration());
        if (engine.mode() != config.calcMode()) engine.setMode(config.calcMode());
        ribbonHost.setVisible(config.ribbonVisible());
        formulaBar.setVisible(config.formulaBarVisible());
        tabBar.setVisible(config.tabsVisible());
        statusBar.setVisible(config.statusVisible());
        formulaBar.setEditable(!isReadOnlyView());
        canvas.invalidateGeometry();
        if (recoveryTimer != null) recoveryTimer.stop();
        recoveryTimer = null;
        if (config.autoRecoverSeconds() > 0) {
            recoveryTimer = new Timer((int) Math.min(Integer.MAX_VALUE, config.autoRecoverSeconds() * 1000L), e -> files.autoRecover());
            recoveryTimer.start();
        }
        revalidate();
        repaint();
    }

    public void setErrorHandler(Consumer<Throwable> handler) { errorHandler = Objects.requireNonNull(handler); }

    public void reportError(Throwable error) {
        if (error == null) return;
        if (SwingUtilities.isEventDispatchThread()) errorHandler.accept(error);
        else SwingUtilities.invokeLater(() -> errorHandler.accept(error));
    }

    public void run(Runnable action) {
        try { action.run(); } catch (RuntimeException failure) { reportError(failure); }
    }

    public boolean edit(String label, SheetCommand command) {
        if (isReadOnlyView()) { popups.info("Somente leitura", "A pasta de trabalho está aberta somente para leitura."); return false; }
        try {
            return session.execute(label, command);
        } catch (RuntimeException failure) {
            reportError(failure);
            return false;
        }
    }

    public boolean isReadOnlyView() { return config.readOnly() || session.isReadOnly(); }
    public boolean isReadOnly() { return isReadOnlyView(); }
    public void setReadOnly(boolean value) { session.setReadOnly(value); formulaBar.setEditable(!isReadOnlyView()); commands.refresh(); }

    public int activeSheetIndex() { return session.getActiveSheetIndex(); }
    public SheetWorksheet activeSheet() { return session.getActiveSheet(); }
    public SheetWorkbook getWorkbook() { return session.getWorkbook(); }
    public SheetSelection getSelection() { return session.getSelection(); }

    public SheetRenderer renderer() { return services.renderer(); }
    public NumberFormatter formatter() { return engine.formatter(); }
    public FormulaLocale formulaLocale() { return engine.formulaLocale(); }
    public ValueParser valueParser() {
        boolean d1904 = session.getWorkbook().properties().date1904();
        if (!parser.locale().equals(config.locale())) parser = new ValueParser(config.locale(), d1904);
        return parser;
    }
    public ConditionalEvaluator conditionalEvaluator() { return conditional; }
    public ValidationEvaluator validationEvaluator() { return validation; }
    public FilterEngine filterEngine() { return new FilterEngine(engine, formatter()); }
    public List<SheetCellRendererProvider> cellRenderers() { return providers(SheetCellRendererProvider.class); }
    public List<SheetConditionalRuleProvider> conditionalProviders() { return providers(SheetConditionalRuleProvider.class); }

    public <T> List<T> providers(Class<T> type) {
        List<T> list = new ArrayList<>();
        for (SheetProvider p : providers) if (type.isInstance(p)) list.add(type.cast(p));
        return list;
    }

    public List<SheetProvider> getProviders() { return List.copyOf(providers); }

    public ProviderRegistration addProvider(SheetProvider provider) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(provider.id(), "Provider id");
        if (providers.stream().anyMatch(p -> p.id().equals(provider.id()))) throw new IllegalArgumentException("Provider já registrado: " + provider.id());
        List<SheetProvider> sorted = new ArrayList<>(providers);
        sorted.add(provider);
        sorted.sort(Comparator.comparingInt(SheetProvider::priority).reversed());
        providers.clear();
        providers.addAll(sorted);
        List<AutoCloseable> hooks = new ArrayList<>();
        providerHooks.put(provider.id(), hooks);
        try {
            installProvider(provider, hooks);
            hooks.add(provider.attach(this));
        } catch (RuntimeException failure) {
            removeProvider(provider.id());
            throw failure;
        }
        commands.refresh();
        canvas.repaint();
        boolean[] closedFlag = {false};
        return () -> { if (!closedFlag[0]) { closedFlag[0] = true; removeProvider(provider.id()); } };
    }

    private void installProvider(SheetProvider provider, List<AutoCloseable> hooks) {
        if (provider instanceof SheetFunctionProvider f) {
            List<String> names = new ArrayList<>();
            for (var fn : f.functions()) { engine.functions().register(fn); names.add(fn.name()); }
            hooks.add(() -> { for (String n : names) engine.functions().unregister(n); engine.rebuild(); });
            engine.rebuild();
        }
        if (provider instanceof SheetExternalDataProvider x) {
            engine.setExternalDataProvider(x);
            hooks.add(() -> { if (engine.external() == x) engine.setExternalDataProvider(providers(SheetExternalDataProvider.class).stream().filter(o -> o != x).findFirst().orElse(null)); });
        }
        if (provider instanceof SheetCommandProvider c) {
            for (Map.Entry<String, Action> e : c.commands(this).entrySet()) hooks.add(commands.register(e.getKey(), e.getValue(), c.group()));
        }
        if (provider instanceof SheetToolbarContributor t) {
            JComponent bar = t.createToolbar(this);
            providerBar.add(bar);
            providerBar.setVisible(true);
            providerBar.revalidate();
            hooks.add(() -> { providerBar.remove(bar); providerBar.setVisible(providerBar.getComponentCount() > 0); providerBar.revalidate(); providerBar.repaint(); });
        }
        if (provider instanceof SheetRibbonContributor r && defaultRibbon != null) {
            String groupId = "provider." + provider.id();
            List<RibbonItem> items = r.commandIds(this).stream().map(RibbonItem::small).toList();
            defaultRibbon.addGroup(r.tab(), new RibbonGroup(groupId, r.group(), 10, "function", items));
            hooks.add(() -> defaultRibbon.removeGroup(r.tab(), groupId));
        }
    }

    public boolean removeProvider(String id) {
        SheetProvider found = providers.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
        if (found == null) return false;
        providers.remove(found);
        List<AutoCloseable> hooks = providerHooks.remove(id);
        if (hooks != null) for (int i = hooks.size() - 1; i >= 0; i--) {
            try { hooks.get(i).close(); } catch (Exception failure) { reportError(failure); }
        }
        commands.refresh();
        canvas.repaint();
        return true;
    }

    public void resetPopupProviders() { popups.resetProviders(); }

    public ProviderRegistration addCellChangeListener(SheetCellChangeListener listener) { cellListeners.add(Objects.requireNonNull(listener)); return () -> cellListeners.remove(listener); }
    public ProviderRegistration addSelectionListener(SheetSelectionListener listener) { selectionListeners.add(Objects.requireNonNull(listener)); return () -> selectionListeners.remove(listener); }
    public ProviderRegistration addSessionListener(Consumer<SheetSessionEvent> listener) { return session.addListener(listener); }
    public ProviderRegistration addCalcListener(CalcListener listener) { return engine.addListener(listener); }

    public ProviderRegistration registerCommand(String id, Action action) { return commands.register(id, action, "Personalizado"); }
    public boolean execute(String id) { return commands.execute(id); }

    public void setRibbon(JComponent component) {
        ribbonHost.removeAll();
        ribbon = component == null ? new JPanel() : component;
        ribbonHost.add(ribbon, BorderLayout.CENTER);
        ribbonHost.revalidate();
        ribbonHost.repaint();
    }

    public void restoreDefaultRibbon() { if (defaultRibbon != null) setRibbon(defaultRibbon); }

    public void setValue(String a1, Object value) { CellAddress a = CellAddress.parse(a1); setValue(activeSheetIndex(), a.row(), a.column(), value); }

    public void setValue(int sheet, int row, int column, Object value) {
        CellValue v = value instanceof CellValue cv ? cv : value == null ? CellValue.EMPTY : value instanceof Number n ? CellValue.of(n.doubleValue()) : value instanceof Boolean b ? CellValue.of(b) : null;
        if (v == null) { input(sheet, new CellAddress(row, column), String.valueOf(value)); return; }
        CellValue fv = v;
        edit("Editar célula", tx -> tx.updateCell(sheet, row, column, c -> c.withValue(fv)));
    }

    public void setFormula(String a1, String formula) {
        CellAddress a = CellAddress.parse(a1);
        String text = formula.startsWith("=") ? formula : "=" + formula;
        String canonical = Formulas.toCanonical(text, formulaLocale(), a, config.r1c1());
        int s = activeSheetIndex();
        edit("Fórmula", tx -> tx.updateCell(s, a.row(), a.column(), c -> c.withFormula(canonical, CellValue.EMPTY)));
    }

    public void input(String a1, String text) { input(activeSheetIndex(), CellAddress.parse(a1), text); }
    public void input(int sheet, CellAddress cell, String text) { editing.write(sheet, List.of(CellRange.of(cell)), cell, text, false); }

    public CellValue getValue(String a1) { return engine.valueAt(activeSheetIndex(), CellAddress.parse(a1)); }
    public CellValue getValue(int sheet, int row, int column) { return engine.valueAt(sheet, row, column); }
    public String getText(String a1) { return displayText(CellAddress.parse(a1)); }

    public String getFormula(String a1) {
        CellAddress a = CellAddress.parse(a1);
        SheetCell c = activeSheet().cell(a);
        return c.hasFormula() ? "=" + Formulas.toDisplay(c.formula(), formulaLocale(), a, config.r1c1()) : null;
    }

    public void setCellStyle(String range, UnaryOperator<CellStyle> change) {
        CellRange r = CellRange.parse(range);
        format.applyStyle(activeSheetIndex(), List.of(r), change, "Formatar");
    }

    public void newWorkbook() { load(SheetWorkbook.create()); files.setCurrentFile(null, null); }

    public void load(SheetWorkbook workbook) {
        editing.cancel();
        clipboard.clearSource();
        session.load(workbook);
    }

    public SheetWorkbook snapshot() { return session.getWorkbook().snapshot(); }

    public SheetTask<Path> open(Path path) { return files.open(path, false); }
    public SheetTask<Path> open(Path path, boolean discardChanges) { return files.open(path, discardChanges); }
    public SheetTask<Path> save() { return files.save(); }
    public SheetTask<Path> save(Path path) { return files.save(path); }
    public SheetTask<Path> export(Path path, ExportFormat format) { return files.export(path, format); }
    public Path getFile() { return files.currentFile(); }
    public boolean isDirty() { return session.isDirty(); }
    public void print() { files.print(); }

    public void undo() { if (!isReadOnlyView()) { editing.cancel(); run(session::undo); } }
    public void redo() { if (!isReadOnlyView()) { editing.cancel(); run(session::redo); } }
    public void recalculate() { engine.calculate(); canvas.repaint(); }
    public void recalculateAll() { engine.calculateFull(); canvas.repaint(); }

    public void select(String reference) { navigation.goTo(reference); }
    public void select(SheetSelection selection) { navigation.select(selection); }
    public void selectAll() { navigation.selectAll(); }

    public void activateSheet(int index) {
        if (index < 0 || index >= session.getWorkbook().sheetCount() || index == activeSheetIndex()) return;
        if (editing.isActive() && !editing.isFormulaPointMode() && !editing.commitIfActive()) return;
        objects.selected(null);
        session.setActiveSheet(index);
    }

    public void moveSheet(int from, int to) { structure.moveSheet(from, to); }
    public void renameSheet(int index, String name) { structure.renameSheet(index, name); }

    public double effectiveZoom() {
        SheetWorksheet ws = activeSheet();
        return zooms.computeIfAbsent(ws.id(), k -> ws.properties().zoom() != 1 ? ws.properties().zoom() : config.zoom());
    }

    public void setZoom(double value) {
        double z = Math.max(0.1, Math.min(4, value));
        zooms.put(activeSheet().id(), z);
        canvas.invalidateGeometry();
        canvas.repaint();
        updateScrollBars();
        editing.reposition();
        refreshStatus();
        firePropertyChange("zoom", null, z);
    }

    public Map<String, Double> sheetZooms() { return Map.copyOf(zooms); }

    public void setViewMode(SheetViewMode mode) {
        SheetWorksheet ws = activeSheet();
        ws.setProperties(ws.properties().withViewMode(mode));
        files.updatePageBreaks();
        refreshStatus();
        commands.refresh();
    }

    public void canvasScrolled() {
        scrolls.put(activeSheet().id(), new long[]{canvas.scrollX(), canvas.scrollY()});
        updateScrollBars();
        editing.reposition();
    }

    public CellRange copySource() { return clipboard.source(); }
    public int copySheet() { return clipboard.sourceSheet(); }
    public boolean showErrorIndicators() { return review.errorIndicators(); }
    public double[] sparklineData(Sparkline s) { return objects.sparklineData(s); }
    public boolean isTableColumnFiltered(SheetTable table, int column) { return data.isTableColumnFiltered(table, column); }
    public ChartData chartData(SheetChart chart) { return objects.chartData(chart); }
    public List<String> slicerItems(Slicer slicer) { return objects.slicerItems(slicer); }
    public void slicerClicked(Slicer slicer, Point point, Rectangle bounds) { objects.slicerClicked(slicer, point, bounds); }
    public void objectSelected(SheetObject object) { objects.selected(object); }
    public void previewObject(String id, ObjectAnchor anchor) { objects.preview(id, anchor); }
    public void commitObjectPreview(String id) { objects.commitPreview(id); }

    public boolean isEditing() { return editing.isActive(); }
    public boolean commitEditingIfActive() { return editing.commitIfActive(); }
    public boolean isFormulaPointMode() { return editing.isFormulaPointMode(); }
    public void insertReference(CellRange range, boolean extend) { editing.insertReference(range, extend); }
    public void pointFinished() { editing.pointFinished(); }
    public void startEditing(String initial, boolean replace) { editing.start(initial, replace); }
    public void startEditingFromFormulaBar(int caret) { editing.startFromFormulaBar(caret); }
    public void cancelEditing() { editing.cancel(); }
    public boolean commitEditing(int rowDelta, int columnDelta) { return editing.commit(rowDelta, columnDelta); }
    public void handleEditorKey(KeyEvent event, JTextPane source) { editing.handleKey(event, source); }

    public void autoFitColumns(List<Integer> columns) { structure.autoFitColumns(columns); }
    public void autoFitRows(List<Integer> rows) { structure.autoFitRows(rows); }
    public void previewColumnWidth(int index, int size) { structure.previewColumnWidth(index, size); }
    public void previewRowHeight(int index, int size) { structure.previewRowHeight(index, size); }
    public void commitColumnWidth(List<Integer> columns, int size, int original, int index) { structure.commitColumnWidth(columns, size, original, index); }
    public void commitRowHeight(List<Integer> rows, int size, int original, int index) { structure.commitRowHeight(rows, size, original, index); }

    public void fill(CellRange source, CellRange target, boolean alternate) { data.fill(source, target, alternate); }
    public void fillDownToAdjacent() { data.fillDownToAdjacent(); }
    public void moveRange(CellRange source, CellAddress target, boolean copy) { clipboard.moveRange(source, target, copy); }

    public void showFilterMenu(SheetTable table, int column, Rectangle anchor) { data.showFilterMenu(table, column, anchor); }
    public void showValidationList(CellAddress cell, Rectangle anchor) { data.showValidationList(cell, anchor); }

    public boolean isCheckbox(CellAddress cell) {
        return validation.find(activeSheetIndex(), cell.row(), cell.column()).map(DataValidation::type).filter(t -> t == ValidationType.CHECKBOX).isPresent();
    }

    public void toggleCheckbox(CellAddress cell) { data.toggleCheckbox(cell); }

    public boolean hasHyperlink(CellAddress cell) { return review.hasHyperlink(cell); }
    public boolean followLinksOnClick() { return review.followLinksOnClick(); }
    public void openHyperlink(CellAddress cell) { review.openHyperlink(cell); }
    public String tooltipFor(CellAddress cell) { return review.tooltipFor(cell); }

    public void showContextMenu(SheetContextMenuContext context, Component invoker, int x, int y) { popups.showContextMenu(context, invoker, x, y); }

    public String displayText(CellAddress cell) { return displayText(activeSheetIndex(), cell); }

    public String displayText(int sheet, CellAddress cell) {
        SheetWorksheet ws = session.getWorkbook().sheet(sheet);
        CellValue v = engine.valueAt(sheet, cell);
        CellStyle style = session.getWorkbook().style(ws.cell(cell).style());
        return formatter().format(v, style.numberFormat()).text();
    }

    public void goToOrDefine(String text) { navigation.goToOrDefine(text); }

    public void applyFontFamily(String family) { format.applyFontFamily(family); }
    public void applyFontSize(double size) { format.applyFontSize(size); }
    public void applyNumberFormat(String code) { format.applyNumberFormat(code); }
    public void applyFill(Integer argb) { format.applyFill(argb); }
    public void applyFontColor(Integer argb) { format.applyFontColor(argb); }

    public void refreshStatus() { if (!closed) statusTimer.restart(); }

    public void showDiagnostics(List<String> messages) {
        if (messages == null || messages.isEmpty()) { diagnostics.setVisible(false); return; }
        diagnostics.setText("<html>" + String.join("<br>", messages.stream().limit(4).map(m -> m.replace("&", "&amp;").replace("<", "&lt;")).toList()) + (messages.size() > 4 ? "<br>… +" + (messages.size() - 4) : "") + "</html>");
        diagnostics.setForeground(UiTokens.warning());
        diagnostics.setVisible(true);
        north.revalidate();
    }

    public void focusGrid() { canvas.requestFocusInWindow(); }

    public void ensureVisible(CellAddress cell) {
        canvas.scrollToCell(cell.row(), cell.column());
        canvas.repaint();
    }

    public void applyRemote(String label, SheetCommand command) {
        applyingRemote = true;
        try { session.applyRemote(label, command); } finally { applyingRemote = false; }
    }

    private void changed(SheetChange change) {
        String author = session.getAuthor();
        for (Map.Entry<String, List<CellRange>> e : change.touched().entrySet()) {
            SheetWorksheet ws = session.getWorkbook().sheetById(e.getKey());
            String name = ws == null ? e.getKey() : ws.name();
            for (SheetCellChangeListener l : cellListeners) {
                try { l.cellsChanged(name, e.getValue(), false); } catch (RuntimeException failure) { reportError(failure); }
            }
        }
        if (applyingRemote) return;
        SheetCollaborationEvent event = new SheetCollaborationEvent(change.id(), change.label(), author, change.touched(), change.structural());
        for (SheetCollaborationProvider p : providers(SheetCollaborationProvider.class)) {
            try { p.localChange(this, event); } catch (RuntimeException failure) { reportError(failure); }
        }
    }

    private void valuesRecalculated(Map<String, List<CellRange>> changed) {
        conditional.invalidate();
        if (closed) return;
        canvas.repaint();
        refreshStatus();
        for (Map.Entry<String, List<CellRange>> e : changed.entrySet()) {
            SheetWorksheet ws = session.getWorkbook().sheetById(e.getKey());
            String name = ws == null ? e.getKey() : ws.name();
            for (SheetCellChangeListener l : cellListeners) {
                try { l.cellsChanged(name, e.getValue(), true); } catch (RuntimeException failure) { reportError(failure); }
            }
        }
    }

    private void sessionEvent(SheetSessionEvent e) {
        if (closed) return;
        switch (e.kind()) {
            case LOAD -> {
                engine.attach(session.getWorkbook());
                parser = new ValueParser(config.locale(), session.getWorkbook().properties().date1904());
                zooms.clear();
                scrolls.clear();
                canvas.resetScroll();
                canvas.clearImageCache();
                conditional.invalidate();
                data.workbookLoaded();
                objects.workbookLoaded();
                showDiagnostics(session.getWorkbook().properties().diagnostics());
            }
            case SHEET -> {
                canvas.resetScroll();
                long[] s = scrolls.get(activeSheet().id());
                if (s != null) canvas.setScroll(s[0], s[1]);
                objects.selected(null);
                files.updatePageBreaks();
            }
            case CONTENT, VALUES -> conditional.invalidate();
            default -> { }
        }
        if (e.structural() || e.kind() == SheetSessionEvent.Kind.SHEET || e.kind() == SheetSessionEvent.Kind.LOAD) canvas.invalidateGeometry();
        if (e.kind() == SheetSessionEvent.Kind.CONTENT && e.structural()) canvas.clearImageCache();
        if (e.kind() == SheetSessionEvent.Kind.SELECTION || e.kind() == SheetSessionEvent.Kind.CONTENT && (e.label().startsWith("Desfazer") || e.label().startsWith("Refazer"))) {
            CellAddress a = session.getSelection().active();
            if (!editing.isActive()) canvas.scrollToCell(a.row(), a.column());
        }
        if (e.kind() == SheetSessionEvent.Kind.SELECTION || e.kind() == SheetSessionEvent.Kind.SHEET || e.kind() == SheetSessionEvent.Kind.LOAD) {
            int sheet = activeSheetIndex();
            SheetSelection sel = session.getSelection();
            for (SheetSelectionListener l : selectionListeners) {
                try { l.selectionChanged(sheet, sel); } catch (RuntimeException failure) { reportError(failure); }
            }
            for (SheetCollaborationProvider p : providers(SheetCollaborationProvider.class)) {
                try { p.selectionChanged(this, activeSheet().name(), sel.range().toA1()); } catch (RuntimeException failure) { reportError(failure); }
            }
        }
        refreshAll();
        firePropertyChange("session." + e.kind().name().toLowerCase(), null, e);
    }

    public void refreshAll() {
        if (closed) return;
        editing.showActiveCell();
        updateNameBox();
        tabBar.repaint();
        updateScrollBars();
        updateRibbon();
        commands.refresh();
        refreshStatus();
        formulaBar.setEditable(!isReadOnlyView());
        canvas.repaint();
    }

    public void updateNameBox() {
        SheetSelection sel = session.getSelection();
        String text = navigation.nameFor(sel);
        List<String> names = new ArrayList<>();
        session.getWorkbook().properties().names().stream().filter(n -> !n.hidden() && (n.sheetScope() == null || n.sheetScope() == activeSheetIndex())).forEach(n -> names.add(n.name()));
        for (SheetWorksheet ws : session.getWorkbook().sheets()) for (SheetTable t : ws.properties().tables()) names.add(t.name());
        formulaBar.setNameBoxText(text, names);
    }

    private void updateRibbon() {
        if (defaultRibbon == null) return;
        CellAddress a = session.getSelection().active();
        CellStyle s = session.getWorkbook().style(activeSheet().cell(a).style());
        defaultRibbon.updateState(s.fontFamily(), s.fontSize(), s.numberFormat());
        List<String> contextual = new ArrayList<>();
        if (data.tableAt(activeSheetIndex(), a) != null) contextual.add("table");
        if (objects.selectedObject() instanceof SheetChart) contextual.add("chart");
        if (objects.pivotAt(activeSheetIndex(), a) != null) contextual.add("pivot");
        defaultRibbon.setContextualTabs(contextual);
    }

    public void updateScrollBars() {
        if (closed || canvas.getWidth() <= 0) return;
        updatingScroll = true;
        try {
            SheetWorksheet ws = activeSheet();
            SheetGeometry g = canvas.geometry();
            CellRange used = engine.usedRange(activeSheetIndex());
            CellAddress active = session.getSelection().active();
            int lastRow = Math.max(used == null ? 0 : used.lastRow(), active.row()), lastCol = Math.max(used == null ? 0 : used.lastColumn(), active.column());
            long viewY = (long) Math.max(1, (canvas.getHeight() - g.bodyY()) / g.zoom()), viewX = (long) Math.max(1, (canvas.getWidth() - g.bodyX()) / g.zoom());
            long baseY = ws.rows().position(g.frozenRows()), baseX = ws.columns().position(g.frozenColumns());
            long maxY = Math.max(ws.rows().position(Math.min(ws.rows().count(), lastRow + 60)) - baseY, canvas.scrollY() + viewY * 2);
            long maxX = Math.max(ws.columns().position(Math.min(ws.columns().count(), lastCol + 12)) - baseX, canvas.scrollX() + viewX * 2);
            maxY = Math.min(maxY, ws.rows().totalSize() - baseY);
            maxX = Math.min(maxX, ws.columns().totalSize() - baseX);
            int ymax = (int) Math.min(Integer.MAX_VALUE - 1, Math.max(viewY, maxY));
            int xmax = (int) Math.min(Integer.MAX_VALUE - 1, Math.max(viewX, maxX));
            vertical.setValues((int) Math.min(canvas.scrollY(), ymax), (int) Math.min(viewY, ymax), 0, ymax);
            horizontal.setValues((int) Math.min(canvas.scrollX(), xmax), (int) Math.min(viewX, xmax), 0, xmax);
            vertical.setBlockIncrement((int) Math.max(20, viewY - 20));
            horizontal.setBlockIncrement((int) Math.max(40, viewX - 40));
        } finally {
            updatingScroll = false;
        }
    }

    private void refreshStatusNow() {
        if (closed) return;
        String mode = editing.modeText();
        if (engine.mode() == CalcMode.MANUAL && engine.hasPendingCalculation()) mode += "    Calcular";
        List<String> info = new ArrayList<>();
        if (isReadOnlyView()) info.add("Somente leitura");
        List<String> circular = engine.circularReferences();
        if (!circular.isEmpty()) info.add("Referências Circulares: " + circular.getFirst());
        String filterInfo = data.filterStatus();
        if (filterInfo != null) info.add(filterInfo);
        if (activeSheet().properties().protection().enabled()) info.add("Planilha protegida");
        statusBar.update(mode, String.join("    ", info), data.selectionAggregates(statusBar.shownAggregates()), effectiveZoom(), activeSheet().properties().viewMode());
    }

    @Override
    protected void onThemeChanged() {
        if (canvas == null) return;
        UiTokens.refresh();
        canvas.setDark(UiTokens.isDarkTheme());
        canvas.repaint();
        repaint();
    }

    @Override
    public void close() {
        if (closed) return;
        editing.cancel();
        closed = true;
        statusTimer.stop();
        if (recoveryTimer != null) recoveryTimer.stop();
        for (SheetProvider p : List.copyOf(providers)) removeProvider(p.id());
        for (ProviderRegistration r : internal) {
            try { r.close(); } catch (Exception ignored) { }
        }
        internal.clear();
        editing.dispose();
        popups.dispose();
        files.dispose();
        canvas.dispose();
    }

    public boolean isClosed() { return closed; }

    public void firePropertyChangeHook(String name, Object oldValue, Object newValue) { firePropertyChange(name, oldValue, newValue); }
}
