package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textarea.diagnostic.DiagnosticSeverity;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnostic;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticAction;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticProvider;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import dtm.stools.models.SearchPanelMode;
import dtm.stools.utils.PaintUtils;
import lombok.Getter;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SmartTextArea extends JTextArea implements EventListenerComponent {

    public static final String LIMIT_REACHED = "smartTextAreaLimitReached";
    public static final String DIAGNOSTICS_CHANGED = "smartTextAreaDiagnosticsChanged";

    private final Set<String> validEvents = new HashSet<>();
    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    private final TextAreaUndoManager undoManager = new TextAreaUndoManager();
    private final UndoableEditListener undoListener = e -> undoManager.record(e.getEdit());
    private final LengthFilter lengthFilter = new LengthFilter();
    private final Highlighter.HighlightPainter currentLinePainter = new CurrentLinePainter();
    private final TextAreaDiagnostics diagnostics = new TextAreaDiagnostics(this);

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
    };

    @Getter
    private String placeholder = "";
    @Getter
    private Color placeholderColor;
    @Getter
    private int maxLength = -1;
    @Getter
    private boolean highlightCurrentLine = true;
    @Getter
    private Color currentLineColor;
    @Getter
    private boolean lineNumbersVisible = true;
    @Getter
    private boolean useSpacesForTab;
    @Getter
    private boolean autoIndent = true;
    @Getter
    private boolean contextMenuEnabled = true;
    @Getter
    private boolean editShortcutsEnabled = true;
    @Getter
    private boolean searchEnabled = true;
    @Getter
    private TextAreaResize resize = TextAreaResize.BOTH;
    @Getter
    private SearchPanelMode searchMode = SearchPanelMode.FLOATING;

    private boolean installed;
    private boolean wasEmpty = true;
    private boolean lineRefreshPending;
    private Rectangle paintedLine;
    private Object currentLineTag;
    private String valueOnFocusGain = "";
    private TextAreaContextMenu contextMenu;

    public SmartTextArea() {
        super();
        init();
    }

    public SmartTextArea(String text) {
        super(text);
        init();
    }

    public SmartTextArea(int rows, int columns) {
        super(rows, columns);
        init();
    }

    public SmartTextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        init();
    }

    public SmartTextArea(Document document) {
        super(document);
        init();
    }

    public SmartTextArea(Document document, String text, int rows, int columns) {
        super(document, text, rows, columns);
        init();
    }

    public SmartTextAreaScrollPane createScrollPane() {
        return new SmartTextAreaScrollPane(this);
    }

    public SmartTextArea setPlaceholder(String placeholder) {
        String previous = this.placeholder;
        this.placeholder = placeholder == null ? "" : placeholder;
        firePropertyChange("placeholder", previous, this.placeholder);
        repaint();
        return this;
    }

    public SmartTextArea setPlaceholderColor(Color placeholderColor) {
        this.placeholderColor = placeholderColor;
        repaint();
        return this;
    }

    public SmartTextArea setMaxLength(int maxLength) {
        int previous = this.maxLength;
        this.maxLength = maxLength < 0 ? -1 : maxLength;
        applyLengthFilter(getDocument());
        firePropertyChange("maxLength", previous, this.maxLength);
        return this;
    }

    public SmartTextArea setHighlightCurrentLine(boolean highlightCurrentLine) {
        boolean previous = this.highlightCurrentLine;
        this.highlightCurrentLine = highlightCurrentLine;
        firePropertyChange("highlightCurrentLine", previous, highlightCurrentLine);
        repaint();
        return this;
    }

    public SmartTextArea setCurrentLineColor(Color currentLineColor) {
        Color previous = this.currentLineColor;
        this.currentLineColor = currentLineColor;
        firePropertyChange("currentLineColor", previous, currentLineColor);
        repaint();
        return this;
    }

    public SmartTextArea setLineNumbersVisible(boolean lineNumbersVisible) {
        boolean previous = this.lineNumbersVisible;
        this.lineNumbersVisible = lineNumbersVisible;
        firePropertyChange("lineNumbersVisible", previous, lineNumbersVisible);
        return this;
    }

    public SmartTextArea setUseSpacesForTab(boolean useSpacesForTab) {
        this.useSpacesForTab = useSpacesForTab;
        return this;
    }

    public SmartTextArea setAutoIndent(boolean autoIndent) {
        this.autoIndent = autoIndent;
        return this;
    }

    public SmartTextArea setContextMenuEnabled(boolean contextMenuEnabled) {
        this.contextMenuEnabled = contextMenuEnabled;
        return this;
    }

    public SmartTextArea setEditShortcutsEnabled(boolean editShortcutsEnabled) {
        this.editShortcutsEnabled = editShortcutsEnabled;
        return this;
    }

    public SmartTextArea setSearchEnabled(boolean searchEnabled) {
        this.searchEnabled = searchEnabled;
        if (!searchEnabled) closeSearch();
        return this;
    }

    public SmartTextArea setSearchMode(SearchPanelMode searchMode) {
        SearchPanelMode previous = this.searchMode;
        this.searchMode = searchMode == null ? SearchPanelMode.FLOATING : searchMode;
        firePropertyChange("searchMode", previous, this.searchMode);
        return this;
    }

    public SmartTextArea setResize(TextAreaResize resize) {
        TextAreaResize previous = this.resize;
        this.resize = resize == null ? TextAreaResize.NONE : resize;
        firePropertyChange("resize", previous, this.resize);
        return this;
    }

    public SmartTextArea addDiagnosticProvider(TextDiagnosticProvider provider) {
        diagnostics.addProvider(provider);
        return this;
    }

    public SmartTextArea removeDiagnosticProvider(TextDiagnosticProvider provider) {
        diagnostics.removeProvider(provider);
        return this;
    }

    public List<TextDiagnosticProvider> getDiagnosticProviders() {
        return diagnostics.providers();
    }

    public SmartTextArea setDiagnostics(List<TextDiagnostic> manualDiagnostics) {
        diagnostics.setManual(manualDiagnostics);
        return this;
    }

    public List<TextDiagnostic> getDiagnostics() {
        return diagnostics.current();
    }

    public List<TextDiagnostic> getDiagnosticsAt(int offset) {
        return diagnostics.entriesAt(offset).stream().map(TextAreaDiagnosticEntry::live).filter(Objects::nonNull).toList();
    }

    public SmartTextArea clearDiagnostics() {
        diagnostics.clear();
        return this;
    }

    public void runDiagnostics() {
        diagnostics.runNow();
    }

    public SmartTextArea setDiagnosticsDelay(int millis) {
        diagnostics.setDelay(millis);
        return this;
    }

    public int getDiagnosticsDelay() {
        return diagnostics.getDelay();
    }

    public void nextDiagnostic() {
        diagnostics.select(true);
    }

    public void previousDiagnostic() {
        diagnostics.select(false);
    }

    public SmartTextArea clear() {
        if (getDocument().getLength() == 0) return this;
        setText("");
        dispachEvent(EventType.CLEAR, () -> "");
        return this;
    }

    @Override
    public void setText(String text) {
        beginCompoundEdit();
        try {
            super.setText(text);
        } finally {
            endCompoundEdit();
        }
    }

    @Override
    public void replaceSelection(String content) {
        boolean grouped = getSelectionStart() != getSelectionEnd();
        if (grouped) beginCompoundEdit();
        try {
            super.replaceSelection(content);
        } finally {
            if (grouped) endCompoundEdit();
        }
    }

    public void undo() {
        if (!isEditable() || !undoManager.canUndo()) return;
        undoManager.undo();
    }

    public void redo() {
        if (!isEditable() || !undoManager.canRedo()) return;
        undoManager.redo();
    }

    public boolean canUndo() {
        return undoManager != null && undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager != null && undoManager.canRedo();
    }

    public void discardAllEdits() {
        undoManager.discardAllEdits();
    }

    public void beginCompoundEdit() {
        if (undoManager != null) undoManager.beginGroup();
    }

    public void endCompoundEdit() {
        if (undoManager != null) undoManager.endGroup();
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public boolean openSearch() {
        return openSearchBar(false);
    }

    public boolean openReplace() {
        return openSearchBar(true);
    }

    public void closeSearch() {
        TextAreaSearchBar bar = searchBar();
        if (bar != null) bar.close();
    }

    public boolean isSearchOpen() {
        TextAreaSearchBar bar = searchBar();
        return bar != null && bar.isActive();
    }

    public void findNext() {
        TextAreaSearchBar bar = searchBar();
        if (bar != null && bar.isActive()) bar.findNext();
    }

    public void findPrevious() {
        TextAreaSearchBar bar = searchBar();
        if (bar != null && bar.isActive()) bar.findPrevious();
    }

    public void duplicateLines() {
        new TextAreaEditActions(this).duplicateLines();
    }

    public void moveLinesUp() {
        new TextAreaEditActions(this).moveLinesUp();
    }

    public void moveLinesDown() {
        new TextAreaEditActions(this).moveLinesDown();
    }

    public void deleteLines() {
        new TextAreaEditActions(this).deleteLines();
    }

    public void indentSelection() {
        new TextAreaEditActions(this).indent();
    }

    public void outdentSelection() {
        new TextAreaEditActions(this).outdent();
    }

    @Override
    public JPopupMenu getComponentPopupMenu() {
        if (!contextMenuEnabled) return null;
        JPopupMenu custom = super.getComponentPopupMenu();
        return custom != null ? custom : contextMenu;
    }

    @Override
    public void setDocument(Document document) {
        Document previous = getDocument();
        if (installed && previous != null) uninstallDocument(previous);
        super.setDocument(document);
        if (installed && document != null) installDocument(document);
    }

    @Override
    public void setHighlighter(Highlighter highlighter) {
        super.setHighlighter(highlighter);
        if (installed) {
            installCurrentLineHighlight();
            diagnostics.highlighterChanged();
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (installed && event != null) {
            List<TextDiagnostic> hits = diagnostics.at(event.getPoint());
            if (!hits.isEmpty()) return diagnosticTooltip(hits);
        }
        return super.getToolTipText(event);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (contextMenu != null) SwingUtilities.updateComponentTreeUI(contextMenu);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> dispachEvent(EventType.LOAD, () -> this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (placeholder.isEmpty() || getDocument().getLength() != 0) return;
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Insets insets = getInsets();
            FontMetrics metrics = getFontMetrics(getFont());
            Rectangle bounds = new Rectangle(insets.left, insets.top,
                    Math.max(0, getWidth() - insets.left - insets.right), metrics.getHeight());
            PaintUtils.drawPlaceholder(g2, placeholder, bounds, resolvePlaceholderColor(), getFont());
        } finally {
            g2.dispose();
        }
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if (eventType == null || eventType.isEmpty() || event == null) return () -> {};
        if (!validEvents.contains(eventType)) return () -> {};
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        List<Consumer<EventComponent>> registered = listeners.get(eventType);
        if (registered != null) registered.remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    protected void registerValidEvents(Set<String> events) {
        events.add(EventType.LOAD);
        events.add(EventType.CHANGE);
        events.add(EventType.INPUT);
        events.add(EventType.FOCUS);
        events.add(EventType.BLUR);
        events.add(EventType.CLEAR);
        events.add(EventType.RESIZE);
        events.add(LIMIT_REACHED);
        events.add(DIAGNOSTICS_CHANGED);
    }

    protected void dispachEvent(String eventType, Supplier<?> value) {
        dispachEvent(eventType, value, Map.of());
    }

    protected void dispachEvent(String eventType, Supplier<?> value, Map<String, Object> properties) {
        List<Consumer<EventComponent>> targets = listeners.get(eventType);
        if (targets == null || targets.isEmpty()) return;
        Component source = this;
        EventComponent event = new EventComponent() {
            @Override
            public Component getComponent() {
                return source;
            }

            @Override
            public Object getValue() {
                return value.get();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T tryGetValue() {
                try {
                    return (T) value.get();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public String getEventType() {
                return eventType;
            }

            @Override
            public Map<String, Object> getProperties() {
                return new HashMap<>(properties);
            }
        };
        targets.forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void diagnosticsChanged() {
        firePropertyChange("diagnostics", null, Boolean.TRUE);
        dispachEvent(DIAGNOSTICS_CHANGED, this::getDiagnostics);
    }

    void fireResized(Dimension size) {
        dispachEvent(EventType.RESIZE, () -> size);
    }

    Map<Integer, DiagnosticSeverity> diagnosticSeverityByLine() {
        return diagnostics.severityByLine();
    }

    List<TextAreaDiagnosticEntry> diagnosticEntriesAt(int offset) {
        return diagnostics.entriesAt(offset);
    }

    void applyDiagnosticAction(TextAreaDiagnosticEntry entry, TextDiagnosticAction action) {
        diagnostics.apply(entry, action);
    }

    Future<?> pendingDiagnostics() {
        return diagnostics.pending();
    }

    static String severityLabel(DiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> I18n.getText(SmartTextArea.class, "severity.error", "Erro");
            case WARNING -> I18n.getText(SmartTextArea.class, "severity.warning", "Aviso");
            case INFO -> I18n.getText(SmartTextArea.class, "severity.info", "Informação");
            case HINT -> I18n.getText(SmartTextArea.class, "severity.hint", "Dica");
        };
    }

    boolean isSearchAvailable() {
        return searchEnabled && searchBar() != null;
    }

    Color resolveCurrentLineColor() {
        if (currentLineColor != null) return currentLineColor;
        Color background = getBackground() != null ? getBackground() : UiTokens.background();
        return PaintUtils.blend(background, UiTokens.primary(), UiTokens.isDarkTheme() ? 0.16f : 0.12f);
    }

    Rectangle currentLineBounds() {
        try {
            int line = getLineOfOffset(Math.min(getCaretPosition(), getDocument().getLength()));
            int start = getLineStartOffset(line);
            int end = getLineEndOffset(line);
            if (line < getLineCount() - 1) end--;
            Rectangle2D first = modelToView2D(start);
            Rectangle2D last = modelToView2D(Math.max(start, end));
            if (first == null || last == null) return null;
            int top = (int) Math.floor(first.getY());
            int bottom = (int) Math.ceil(last.getMaxY());
            return new Rectangle(0, top, getWidth(), Math.max(1, bottom - top));
        } catch (BadLocationException ex) {
            return null;
        }
    }

    int getLineOfOffsetSafe(int offset) {
        try {
            return getLineOfOffset(Math.clamp(offset, 0, getDocument().getLength()));
        } catch (BadLocationException ex) {
            return 0;
        }
    }

    static int menuShortcutMask() {
        if (GraphicsEnvironment.isHeadless()) return InputEvent.CTRL_DOWN_MASK;
        try {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        } catch (RuntimeException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    private void init() {
        registerValidEvents(validEvents);
        contextMenu = new TextAreaContextMenu(this);
        installed = true;
        installDocument(getDocument());
        installCurrentLineHighlight();
        installKeyBindings();
        new TextAreaEditActions(this).install();
        installListeners();
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    private void installDocument(Document document) {
        document.addDocumentListener(documentListener);
        document.addUndoableEditListener(undoListener);
        applyLengthFilter(document);
        undoManager.discardAllEdits();
        diagnostics.documentReplaced();
        wasEmpty = document.getLength() == 0;
        refreshCurrentLine();
    }

    private void uninstallDocument(Document document) {
        document.removeDocumentListener(documentListener);
        document.removeUndoableEditListener(undoListener);
        if (document instanceof AbstractDocument abstractDocument && abstractDocument.getDocumentFilter() == lengthFilter) {
            abstractDocument.setDocumentFilter(null);
        }
    }

    private void applyLengthFilter(Document document) {
        if (!(document instanceof AbstractDocument abstractDocument)) return;
        if (maxLength >= 0) {
            abstractDocument.setDocumentFilter(lengthFilter);
        } else if (abstractDocument.getDocumentFilter() == lengthFilter) {
            abstractDocument.setDocumentFilter(null);
        }
    }

    private void installCurrentLineHighlight() {
        Highlighter highlighter = getHighlighter();
        if (highlighter == null) return;
        if (currentLineTag != null) {
            try {
                highlighter.removeHighlight(currentLineTag);
            } catch (RuntimeException ignored) {
            }
        }
        try {
            currentLineTag = highlighter.addHighlight(0, 0, currentLinePainter);
        } catch (BadLocationException ex) {
            currentLineTag = null;
        }
    }

    private void installKeyBindings() {
        InputMap input = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actions = getActionMap();
        int menu = menuShortcutMask();
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu), "smartTextArea.undo", this::undo, this::canUndo);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_Y, menu), "smartTextArea.redo", this::redo, this::canRedo);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu | InputEvent.SHIFT_DOWN_MASK), "smartTextArea.redo");
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "smartTextArea.find", this::openSearch, this::isSearchAvailable);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_H, menu), "smartTextArea.replace", this::openReplace,
                () -> isSearchAvailable() && isEditable());
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "smartTextArea.findNext", this::findNext, this::isSearchOpen);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "smartTextArea.findPrevious",
                this::findPrevious, this::isSearchOpen);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "smartTextArea.closeSearch", this::closeSearch, this::isSearchOpen);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "smartTextArea.nextDiagnostic", this::nextDiagnostic,
                () -> !diagnostics.current().isEmpty());
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK), "smartTextArea.previousDiagnostic",
                this::previousDiagnostic, () -> !diagnostics.current().isEmpty());
    }

    private void installListeners() {
        addCaretListener(e -> refreshCurrentLine());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshCurrentLine();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                valueOnFocusGain = getText();
                dispachEvent(EventType.FOCUS, SmartTextArea.this::getText);
            }

            @Override
            public void focusLost(FocusEvent e) {
                dispachEvent(EventType.BLUR, SmartTextArea.this::getText);
                String text = getText();
                if (!Objects.equals(text, valueOnFocusGain)) {
                    valueOnFocusGain = text;
                    dispachEvent(EventType.CHANGE, () -> text);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                moveCaretForPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                moveCaretForPopup(e);
            }
        });
    }

    private void moveCaretForPopup(MouseEvent e) {
        if (!e.isPopupTrigger() || getComponentPopupMenu() == null) return;
        if (!hasFocus()) requestFocusInWindow();
        int offset = viewToModel2D(e.getPoint());
        if (offset < 0) return;
        if (offset < getSelectionStart() || offset > getSelectionEnd()) setCaretPosition(offset);
    }

    private boolean openSearchBar(boolean replace) {
        if (!searchEnabled) return false;
        TextAreaSearchBar bar = searchBar();
        if (bar == null) return false;
        bar.open(replace);
        return true;
    }

    private TextAreaSearchBar searchBar() {
        SmartTextAreaScrollPane pane = (SmartTextAreaScrollPane) SwingUtilities.getAncestorOfClass(SmartTextAreaScrollPane.class, this);
        return pane != null && pane.getTextArea() == this ? pane.getSearchBar() : null;
    }

    private void documentChanged() {
        boolean empty = getDocument().getLength() == 0;
        if (empty != wasEmpty) {
            wasEmpty = empty;
            repaint();
        }
        refreshCurrentLine();
        diagnostics.documentChanged();
        dispachEvent(EventType.INPUT, this::getText, Map.of("length", getDocument().getLength()));
    }

    private void refreshCurrentLine() {
        if (lineRefreshPending) return;
        lineRefreshPending = true;
        SwingUtilities.invokeLater(() -> {
            lineRefreshPending = false;
            Rectangle next = highlightCurrentLine ? currentLineBounds() : null;
            if (Objects.equals(next, paintedLine)) return;
            if (paintedLine != null) repaint(paintedLine);
            if (next != null) repaint(next);
            paintedLine = next;
        });
    }

    private void limitReached() {
        dispachEvent(LIMIT_REACHED, this::getText, Map.of("maxLength", maxLength));
    }

    private String diagnosticTooltip(List<TextDiagnostic> hits) {
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < hits.size(); i++) {
            TextDiagnostic hit = hits.get(i);
            if (i > 0) html.append("<hr>");
            html.append("<b>").append(escape(severityLabel(hit.severity()))).append("</b>");
            if (hit.source() != null && !hit.source().isBlank()) html.append(" &middot; ").append(escape(hit.source()));
            html.append("<br>").append(escape(hit.message()).replace("\n", "<br>"));
        }
        return html.append("</html>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Color resolvePlaceholderColor() {
        return placeholderColor != null ? placeholderColor : UiTokens.muted();
    }

    private static void bind(InputMap input, ActionMap actions, KeyStroke key, String name, Runnable action, BooleanSupplier enabled) {
        input.put(key, name);
        actions.put(name, new TextAreaAction(action, enabled));
    }

    private final class CurrentLinePainter implements Highlighter.HighlightPainter {
        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent component) {
            if (!highlightCurrentLine) return;
            Rectangle line = currentLineBounds();
            if (line == null) return;
            g.setColor(resolveCurrentLineColor());
            g.fillRect(0, line.y, getWidth(), line.height);
        }
    }

    private final class LengthFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes) throws BadLocationException {
            String accepted = text == null ? "" : text;
            boolean reached = false;
            if (maxLength >= 0) {
                int available = Math.max(0, maxLength - (bypass.getDocument().getLength() - length));
                if (accepted.length() > available) {
                    accepted = accepted.substring(0, available);
                    reached = true;
                }
            }
            if (length > 0 || !accepted.isEmpty()) bypass.replace(offset, length, accepted, attributes);
            if (reached) limitReached();
        }
    }
}
