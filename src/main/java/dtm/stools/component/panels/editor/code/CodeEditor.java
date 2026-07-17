package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.editor.code.gutter.layer.BookmarkLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.BreakpointLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.FoldingLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.GutterLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.LineMarkerLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.LineNumberLayer;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationPopup;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpPopup;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.listeners.CodeEditorStateListener;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.listeners.BookmarkChangeListener;
import dtm.stools.component.panels.editor.code.listeners.BreakpointChangeListener;
import dtm.stools.component.panels.editor.code.listeners.HoverListener;
import dtm.stools.component.panels.editor.code.listeners.LineChangeListener;
import dtm.stools.component.panels.editor.code.listeners.LineColorChangeListener;
import dtm.stools.component.panels.editor.code.listeners.SearchRequestListener;
import dtm.stools.component.panels.editor.code.prototype.Breakpoint;
import dtm.stools.component.panels.editor.code.prototype.LineColorInfo;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import dtm.stools.component.panels.editor.code.prototype.styles.BreakpointStyle;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.api.CodeAction;
import dtm.stools.component.panels.editor.code.api.CommandHandler;
import dtm.stools.component.panels.editor.code.api.DocumentSymbol;
import dtm.stools.component.panels.editor.code.api.Location;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeListener;
import dtm.stools.component.panels.editor.code.api.WordClickHandler;
import dtm.stools.component.panels.editor.code.api.WordHoverDecorator;
import dtm.stools.component.panels.editor.code.api.WordHoverListener;
import dtm.stools.component.panels.editor.code.api.WordHoverPainter;
import dtm.stools.component.panels.editor.code.api.WordHoverStyle;
import dtm.stools.component.panels.editor.code.search.SearchOptions;
import dtm.stools.component.panels.editor.code.search.SearchPanel;
import dtm.stools.component.panels.editor.code.api.CodeEditorState;
import dtm.stools.component.panels.editor.code.diagnostics.ErrorStripeClickListener;
import dtm.stools.component.panels.editor.code.diagnostics.InspectionWidgetClickListener;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CodeEditor extends BlockingPanel {

    private static final String ACTION_TOGGLE_BREAKPOINT = "codeEditor.toggleBreakpoint";

    @Getter
    private final CodeEditorTextArea textArea;

    @Getter
    private final CodeEditorScrollPane scrollPane;

    @Getter
    private final CodeEditorErrorStripe errorStripe;

    @Getter
    private CodeEditorInspectionWidget inspectionWidget;

    private JLayeredPane editorOverlay;

    @Getter
    private boolean searchEnabled = true;

    private final Map<String, Object> userProperties;

    @Getter
    private SearchPanelPosition searchPanelPosition = SearchPanelPosition.TOP;

    private Object currentMountedConstraint;

    private JDialog searchPopup;

    @Getter
    private KeyStroke toggleBreakpointKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);

    private static final Color DEFAULT_BREAKPOINT_LINE_COLOR = new Color(255, 80, 80, 40);

    @Getter
    private boolean breakpointLineHighlightEnabled = true;

    @Getter
    private Color breakpointLineColor = DEFAULT_BREAKPOINT_LINE_COLOR;

    public CodeEditor() {
        this(null, null);
    }

    public CodeEditor(CodeEditorScrollPane scrollPane) {
        this(null, scrollPane);
    }

    public CodeEditor(String initialText) {
        this(initialText, null);
    }

    public CodeEditor(String initialText, CodeEditorScrollPane scrollPane) {
        this.textArea = new CodeEditorTextArea(initialText);
        this.scrollPane = scrollPane != null ? scrollPane : new CodeEditorScrollPane(textArea);
        this.userProperties = new ConcurrentHashMap<>();
        setLayout(new BorderLayout());
        this.errorStripe = new CodeEditorErrorStripe(textArea, this.scrollPane);
        this.inspectionWidget = new CodeEditorInspectionWidget(textArea);
        add(buildEditorOverlay(), BorderLayout.CENTER);
        add(this.errorStripe, BorderLayout.EAST);
        getGutter().addLayer(new LineNumberLayer());
        getGutter().addLayer(new BreakpointLayer());
        getGutter().addLayer(new BookmarkLayer());
        getGutter().addLayer(new FoldingLayer());
        getGutter().addLayer(new LineMarkerLayer());
        textArea.addFoldStateListener(() -> {
            getGutter().revalidate();
            getGutter().repaint();
            if(scrollPane != null){
                scrollPane.revalidate();
                scrollPane.repaint();
            }
        });
        textArea.addDocumentEditListener(new DocumentEditListener() {
            @Override
            public void onTextChanged() {
                getGutter().repaint();
            }
        });
        textArea.addBookmarkListener(() -> {
            getGutter().revalidate();
            getGutter().repaint();
        });
        textArea.addSearchRequestListener((selected, replaceMode) -> {
            if (!searchEnabled) return;
            showSearchPanel(selected, replaceMode);
        });
        installBreakpointAction();
        getGutter().addBreakpointChangeListener(this::onBreakpointChanged);
    }

    private JComponent buildEditorOverlay() {
        editorOverlay = new JLayeredPane() {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                scrollPane.setBounds(0, 0, w, h);
                if (inspectionWidget != null && inspectionWidget.isVisible()) {
                    Dimension d = inspectionWidget.getPreferredSize();
                    int x = Math.max(0, w - d.width - inspectionWidget.getMarginRight());
                    inspectionWidget.setBounds(x, inspectionWidget.getMarginTop(), d.width, d.height);
                }
            }
        };
        editorOverlay.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
        editorOverlay.add(inspectionWidget, JLayeredPane.PALETTE_LAYER);
        return editorOverlay;
    }

    private void installBreakpointAction() {
        textArea.getActionMap().put(ACTION_TOGGLE_BREAKPOINT, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleBreakpoint(textArea.getCaretLine());
            }
        });
        bindBreakpointActionKey();
        addLineColorChangeListener(new LineColorChangeListener() {
            @Override
            public void onLineColorsCleared() {
                Runnable action = () -> {
                    Set<Integer> breakpointLines = getBreakpointLines();
                    textArea.setLinesColor(breakpointLines, breakpointLineColor);
                };
                if(SwingUtilities.isEventDispatchThread()){
                    action.run();
                    return;
                }
                SwingUtilities.invokeLater(action);
            }
        });
    }

    private void bindBreakpointActionKey() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        KeyStroke[] keys = inputMap.keys();
        if (keys != null) {
            for (KeyStroke key : keys) {
                if (ACTION_TOGGLE_BREAKPOINT.equals(inputMap.get(key))) {
                    inputMap.remove(key);
                }
            }
        }
        if (toggleBreakpointKeyStroke != null) {
            inputMap.put(toggleBreakpointKeyStroke, ACTION_TOGGLE_BREAKPOINT);
        }
    }

    public void setKeyboardBreakpointEnabled(boolean enabled) {
        if (enabled) {
            if (toggleBreakpointKeyStroke == null) {
                toggleBreakpointKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            }
        } else {
            toggleBreakpointKeyStroke = null;
        }

        bindBreakpointActionKey();
    }

    public void setSearchEnabled(boolean enabled) {
        this.searchEnabled = enabled;
        if (!enabled) {
            hideSearchPanel();
        }
    }

    public void setSearchPanelPosition(SearchPanelPosition position) {
        if (position == null) position = SearchPanelPosition.TOP;
        if (this.searchPanelPosition == position) return;
        this.searchPanelPosition = position;
        unmountSearchPanel();
    }

    public void hideSearchPanel() {
        unmountSearchPanel();
        textArea.hideSearchPanel();
    }

    public void setGoScroolToPositionEnabled(boolean goToPositionEnabled){
        if(scrollPane != null){
            scrollPane.setGoToPositionEnabled(goToPositionEnabled);
        }
    }

    public CodeEditorGutter getGutter() {
        return scrollPane.getGutter();
    }

    public CodeEditorMinimap getMinimap() {
        return scrollPane.getMinimap();
    }

    public void setMinimapVisibilityMode(CodeEditorMinimap.VisibilityMode mode) {
        if (scrollPane.getMinimap() != null) {
            scrollPane.getMinimap().setVisibilityMode(mode);
        }
    }

    public CodeEditorMinimap.VisibilityMode getMinimapVisibilityMode() {
        return scrollPane.getMinimap() != null
                ? scrollPane.getMinimap().getVisibilityMode()
                : CodeEditorMinimap.VisibilityMode.SCROLLBAR_PREVIEW;
    }

    public void setMinimapPreviewMirrorEnabled(boolean enabled) {
        if (scrollPane.getMinimap() != null) {
            scrollPane.getMinimap().setPreviewMirrorEnabled(enabled);
        }
    }

    public boolean isMinimapPreviewMirrorEnabled() {
        return scrollPane.getMinimap() != null
                && scrollPane.getMinimap().isPreviewMirrorEnabled();
    }

    public TextBuffer getBuffer() {
        return textArea.getBuffer();
    }

    public String getText() {
        return textArea.getBuffer().getText();
    }

    public void addGutterLayer(GutterLayer gutterLayer) {
        scrollPane.getGutter().addLayer(gutterLayer);
    }

    public boolean isHighlightCurrentLine() {
        return textArea.isHighlightCurrentLine();
    }

    public void setHighlightCurrentLine(boolean enabled) {
        textArea.setHighlightCurrentLine(enabled);
        textArea.repaint();
    }

    public Color getCurrentLineColor() {
        return textArea.getCurrentLineColor();
    }

    public void setCurrentLineColor(Color color) {
        textArea.setCurrentLineColor(color);
        textArea.repaint();
    }

    public void enableBreakpoint(boolean enabled) {
        getGutter().enableBreakpoint(enabled);
    }

    public void setBreakpointEnableOnClick(boolean enabled) {
        getGutter().setBreakpointEnableOnClick(enabled);
    }

    public boolean isBreakpointEnableOnClick() {
        return getGutter().isBreakpointEnableOnClick();
    }

    public void enableBreakpointEmptyLine(boolean enabled) {
        getGutter().enableBreakpointEmptyLine(enabled);
    }

    public void setBreakpointEmptyLineEnabled(boolean enabled) {
        getGutter().setBreakpointEmptyLineEnabled(enabled);
    }

    public boolean isBreakpointEmptyLineEnabled() {
        return getGutter().isBreakpointEmptyLineEnabled();
    }

    public void addBreakpoint(int line) {
        getGutter().addBreakpoint(line);
    }

    public void removeBreakpoint(int line) {
        getGutter().removeBreakpoint(line);
    }

    public void toggleBreakpoint(int line) {
        getGutter().toggleBreakpoint(line);
    }

    public void clearBreakpoints() {
        getGutter().clearBreakpoints();
    }

    public boolean hasBreakpoint(int line) {
        return getGutter().hasBreakpoint(line);
    }

    public void setBreakpointActive(int line, boolean active) {
        getGutter().setBreakpointActive(line, active);
    }

    public void toggleBreakpointActive(int line) {
        getGutter().toggleBreakpointActive(line);
    }

    public void deactivateBreakpoint(int line) {
        getGutter().deactivateBreakpoint(line);
    }

    public void deactivateAllBreakpoints() {
        getGutter().deactivateAllBreakpoints();
    }

    public void activateBreakpoint(int line) {
        getGutter().activateBreakpoint(line);
    }

    public void activateAllBreakpoints() {
        getGutter().activateAllBreakpoints();
    }

    public boolean isBreakpointActive(int line) {
        return getGutter().isBreakpointActive(line);
    }

    public void setBreakpointInactiveIcon(Icon icon) {
        getGutter().setBreakpointInactiveIcon(icon);
    }

    public boolean setBreakpointStyle(int line, BreakpointStyle style) {
        return getGutter().setBreakpointStyle(line, style);
    }

    public boolean setBreakpointInactiveStyle(int line, BreakpointStyle inactiveStyle) {
        return getGutter().setBreakpointInactiveStyle(line, inactiveStyle);
    }

    public boolean setBreakpointStyles(int line, BreakpointStyle style, BreakpointStyle inactiveStyle) {
        return getGutter().setBreakpointStyles(line, style, inactiveStyle);
    }

    public Set<Integer> getBreakpointLines() {
        return getGutter().getBreakpointLines();
    }

    public Set<Breakpoint> getBreakpoints() {
        return getGutter().getBreakpoints();
    }

    public void addBreakpointChangeListener(BreakpointChangeListener listener) {
        getGutter().addBreakpointChangeListener(listener);
    }

    public void removeBreakpointChangeListener(BreakpointChangeListener listener) {
        getGutter().removeBreakpointChangeListener(listener);
    }

    public void setToggleBreakpointKeyStroke(KeyStroke keyStroke) {
        this.toggleBreakpointKeyStroke = keyStroke;
        bindBreakpointActionKey();
    }

    public void setBreakpointLineHighlightEnabled(boolean enabled) {
        if (this.breakpointLineHighlightEnabled == enabled) {
            return;
        }
        this.breakpointLineHighlightEnabled = enabled;
        if (enabled) {
            for (Breakpoint breakpoint : getBreakpoints()) {
                paintBreakpointLine(breakpoint.getLine());
            }
        } else {
            for (Breakpoint breakpoint : getBreakpoints()) {
                removeBreakpointLine(breakpoint.getLine());
            }
        }
    }

    public void setBreakpointLineColor(Color color) {
        this.breakpointLineColor = color != null ? color : DEFAULT_BREAKPOINT_LINE_COLOR;
        if (breakpointLineHighlightEnabled) {
            for (Breakpoint breakpoint : getBreakpoints()) {
                paintBreakpointLine(breakpoint.getLine());
            }
        }
    }

    private void paintBreakpointLine(int line) {
        setLineColor(line, breakpointLineColor);
    }

    private void removeBreakpointLine(int line) {
        removeLineColor(line);
    }

    public void enableBookmark(boolean enabled) {
        getGutter().enableBookmark(enabled);
    }

    public boolean isBookmarkEnabled() {
        return getGutter().isBookmarkEnabled();
    }

    public void setBookmarkPreviewOnHoverEnabled(boolean enabled) {
        getGutter().setBookmarkPreviewOnHoverEnabled(enabled);
    }

    public boolean isBookmarkPreviewOnHoverEnabled() {
        return getGutter().isBookmarkPreviewOnHoverEnabled();
    }

    public void setBookmarkPreviewAlpha(float alpha) {
        getGutter().setBookmarkPreviewAlpha(alpha);
    }

    public float getBookmarkPreviewAlpha() {
        return getGutter().getBookmarkPreviewAlpha();
    }

    public void setBookmarkEnableOnClick(boolean enabled) {
        getGutter().setBookmarkEnableOnClick(enabled);
    }

    public boolean isBookmarkEnableOnClick() {
        return getGutter().isBookmarkEnableOnClick();
    }

    public void setEditorFont(Font editorFont) {
        textArea.setFont(editorFont);
    }

    public void addLineChangeListener(LineChangeListener listener) {
        textArea.addLineChangeListener(listener);
    }

    public void removeLineChangeListener(LineChangeListener listener) {
        textArea.removeLineChangeListener(listener);
    }

    public void addDocumentEditListener(DocumentEditListener listener) {
        textArea.addDocumentEditListener(listener);
    }

    public void removeDocumentEditListener(DocumentEditListener listener) {
        textArea.removeDocumentEditListener(listener);
    }

    public void addStateListener(CodeEditorStateListener listener) {
        textArea.addStateListener(listener);
    }

    public void removeStateListener(CodeEditorStateListener listener) {
        textArea.removeStateListener(listener);
    }

    public CodeEditorState getEditorState() {
        return textArea.getEditorState();
    }

    public void setText(String text) {
        textArea.setText(text);
        textArea.repaint();
    }

    public boolean isReadOnly() {
        return textArea.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        textArea.setReadOnly(readOnly);
    }

    public boolean isModified() {
        return textArea.isModified();
    }

    public void markClean() {
        textArea.markClean();
    }

    public void setSmartIndentEnabled(boolean enabled) {
        textArea.setSmartIndentEnabled(enabled);
    }

    public boolean isSmartIndentEnabled() {
        return textArea.isSmartIndentEnabled();
    }

    public void setTabSize(int tabSize) {
        textArea.setTabSize(tabSize);
    }

    public int getTabSize() {
        return textArea.getTabSize();
    }

    public void setUseSpacesForTab(boolean useSpacesForTab) {
        textArea.setUseSpacesForTab(useSpacesForTab);
    }

    public boolean isUseSpacesForTab() {
        return textArea.isUseSpacesForTab();
    }

    public void addProvider(CodeEditorProvider provider) {
        textArea.addProvider(provider);
    }

    public int getCaretLine() {
        return textArea.getCaretLine();
    }

    public int getCaretCol() {
        return textArea.getCaretCol();
    }

    public int getCaretOffset() {
        return textArea.getCaretOffset();
    }

    public int getSelectionStartLine() {
        return textArea.getSelectionStartLine();
    }

    public int getSelectionStartCol() {
        return textArea.getSelectionStartCol();
    }

    public boolean isSelectionActive() {
        return textArea.isSelectionActive();
    }

    public int getSelectionStartOffset() {
        return textArea.getSelectionStartOffset();
    }

    public int getSelectionEndOffset() {
        return textArea.getSelectionEndOffset();
    }

    public String getSelectedTextOrEmpty() {
        return textArea.getSelectedTextOrEmpty();
    }

    public void setCaretPosition(int line, int col) {
        textArea.setCaretPosition(line, col);
    }

    public void setSelection(int startLine, int startCol, int endLine, int endCol) {
        textArea.setSelection(startLine, startCol, endLine, endCol);
    }

    public void selectLine(int line) {
        textArea.selectLine(line);
    }

    public void putUserProperty(String key, Object value) {
        if (key == null || key.isBlank()) {
            return;
        }

        if (value == null) {
            userProperties.remove(key);
            return;
        }

        userProperties.put(key, value);
    }

    public Object getUserProperty(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        return userProperties.get(key);
    }

    public <T> T getUserProperty(String key, Class<T> type) {
        Object value = getUserProperty(key);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            return null;
        }

        return type.cast(value);
    }

    public boolean hasUserProperty(String key) {
        return key != null && userProperties.containsKey(key);
    }

    public Object removeUserProperty(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        return userProperties.remove(key);
    }

    private Border savedScrollPaneBorder;
    private Border savedTextAreaBorder;
    @Getter
    private boolean focusBorderEnabled = true;

    public void setFocusBorderEnabled(boolean enabled) {
        if (this.focusBorderEnabled == enabled) return;
        this.focusBorderEnabled = enabled;
        if (enabled) {
            scrollPane.putClientProperty("FlatLaf.style", null);
            textArea.putClientProperty("FlatLaf.style", null);
            scrollPane.setBorder(savedScrollPaneBorder != null
                    ? savedScrollPaneBorder
                    : UIManager.getBorder("ScrollPane.border"));
            textArea.setBorder(savedTextAreaBorder);
        } else {
            savedScrollPaneBorder = scrollPane.getBorder();
            savedTextAreaBorder = textArea.getBorder();
            String style = "focusWidth: 0; innerFocusWidth: 0; borderWidth: 0";
            scrollPane.putClientProperty("FlatLaf.style", style);
            textArea.putClientProperty("FlatLaf.style", style);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            textArea.setBorder(BorderFactory.createEmptyBorder());
        }
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    public boolean isFoldingEnabled() {
        return textArea.isFoldingEnabled();
    }

    public void setFoldingEnabled(boolean enabled) {
        textArea.setFoldingEnabled(enabled);
    }

    public void setFoldChevronVisibleOnHoverOnly(boolean enabled) {
        getGutter().setFoldChevronVisibleOnHoverOnly(enabled);
    }

    public boolean isFoldChevronVisibleOnHoverOnly() {
        return getGutter().isFoldChevronVisibleOnHoverOnly();
    }

    public boolean isFoldPreviewOnHoverEnabled() {
        return textArea.isFoldPreviewOnHoverEnabled();
    }

    public void setFoldPreviewOnHoverEnabled(boolean enabled) {
        textArea.setFoldPreviewOnHoverEnabled(enabled);
    }

    public int getFoldPreviewMaxLines() {
        return textArea.getFoldPreviewMaxLines();
    }

    public void setFoldPreviewMaxLines(int maxLines) {
        textArea.setFoldPreviewMaxLines(maxLines);
    }

    public int getFoldPreviewMaxColumns() {
        return textArea.getFoldPreviewMaxColumns();
    }

    public void setFoldPreviewMaxColumns(int maxColumns) {
        textArea.setFoldPreviewMaxColumns(maxColumns);
    }

    public void addFoldRule(FoldRule rule) {
        textArea.addFoldRule(rule);
    }

    public boolean removeFoldRule(FoldRule rule) {
        return textArea.removeFoldRule(rule);
    }

    public void setFoldRules(Collection<? extends FoldRule> rules) {
        textArea.setFoldRules(rules);
    }

    public void clearFoldRules() {
        textArea.clearFoldRules();
    }

    public List<FoldRule> getFoldRules() {
        return textArea.getFoldRules();
    }

    public void toggleFoldAtCaret() {
        textArea.toggleFoldAtCaret();
    }

    public void toggleFold(int bufferLine) {
        textArea.toggleFold(bufferLine);
    }

    public void foldAll() {
        textArea.foldAll();
    }

    public void unfoldAll() {
        textArea.unfoldAll();
    }

    public boolean isLineHidden(int bufferLine) {
        return textArea.isLineHidden(bufferLine);
    }

    public boolean isFoldAnchor(int bufferLine) {
        return textArea.isFoldAnchor(bufferLine);
    }

    public boolean isFoldAnchorLine(int bufferLine) {
        return textArea.isFoldAnchorLine(bufferLine);
    }

    public FoldRegion getFoldRegionStartingAt(int bufferLine) {
        return textArea.getFoldRegionStartingAt(bufferLine);
    }

    public int bufferLineToVisualLine(int bufferLine) {
        return textArea.bufferLineToVisualLine(bufferLine);
    }

    public int visualLineToBufferLine(int visualLine) {
        return textArea.visualLineToBufferLine(visualLine);
    }

    public int visualLineCount() {
        return textArea.visualLineCount();
    }

    public void addFoldStateListener(Runnable listener) {
        textArea.addFoldStateListener(listener);
    }

    public void removeFoldStateListener(Runnable listener) {
        textArea.removeFoldStateListener(listener);
    }

    public void addStyledRange(StyledRange range) {
        textArea.addStyledRange(range);
    }

    public List<StyledRange> getStyledRanges() {
        return textArea.getStyledRanges();
    }

    public TextStyle getStyleAt(int offset) {
        return textArea.getStyleAt(offset);
    }

    public TextStyle getStyleAtOffset(int offset) {
        return textArea.getStyleAtOffset(offset);
    }

    public TextStyle getStyleAt(int line, int col) {
        return textArea.getStyleAt(line, col);
    }

    public void removeStyledRange(StyledRange range) {
        textArea.removeStyledRange(range);
    }

    public void clearStyledRanges() {
        textArea.clearStyledRanges();
    }

    public void setLineColor(int line, Color color) {
        textArea.setLineColor(line, color);
    }

    public void setLineColor(int line, Color background, Color foreground) {
        textArea.setLineColor(line, background, foreground);
    }

    public void setLinesColor(int[] lines, Color color) {
        textArea.setLinesColor(lines, color);
    }

    public void setLinesColor(int[] lines, Color background, Color foreground) {
        textArea.setLinesColor(lines, background, foreground);
    }

    public void setLinesColor(Collection<Integer> lines, Color color) {
        textArea.setLinesColor(lines, color);
    }

    public void setLinesColor(Collection<Integer> lines, Color background, Color foreground) {
        textArea.setLinesColor(lines, background, foreground);
    }

    public void removeLineColor(int line) {
        textArea.removeLineColor(line);
    }

    public void clearLineColors() {
        textArea.clearLineColors();
    }

    public void addLineColorChangeListener(LineColorChangeListener listener) {
        textArea.addLineColorChangeListener(listener);
    }

    public void removeLineColorChangeListener(LineColorChangeListener listener) {
        textArea.removeLineColorChangeListener(listener);
    }

    public LineColorInfo getLineColor(int line) {
        return textArea.getLineColor(line);
    }

    public void setLineChangeMarker(int line, Color color) {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        if (layer == null) {
            layer = new LineMarkerLayer();
            getGutter().addLayer(layer);
        }
        layer.setLineColor(line, color);
        getGutter().repaint();
    }

    public void setLineChangeMarker(int line, Color color, LineMarkerLayer.Side side) {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        if (layer == null) {
            layer = new LineMarkerLayer();
            getGutter().addLayer(layer);
        }
        if (side != null) {
            layer.setSide(side);
        }
        layer.setLineColor(line, color);
        getGutter().repaint();
    }

    public void setLineChangeMarkerSide(LineMarkerLayer.Side side) {
        if (side == null) return;
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        if (layer == null) {
            layer = new LineMarkerLayer();
            getGutter().addLayer(layer);
        }
        layer.setSide(side);
        getGutter().repaint();
    }

    public LineMarkerLayer.Side getLineChangeMarkerSide() {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        return layer != null ? layer.getSide() : LineMarkerLayer.Side.LEFT;
    }

    public void removeLineChangeMarker(int line) {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        if (layer == null) return;
        layer.removeLineColor(line);
        getGutter().repaint();
    }

    public void clearLineChangeMarkers() {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        if (layer == null) return;
        layer.clearLineColors();
        getGutter().repaint();
    }

    public Color getLineChangeMarker(int line) {
        LineMarkerLayer layer = getGutter().getLayer(LineMarkerLayer.class);
        return layer != null ? layer.getLineColor(line) : null;
    }

    public void setShowIndentGuides(boolean show) {
        textArea.setShowIndentGuides(show);
    }

    public void addHoverListener(HoverListener l) {
        textArea.addHoverListener(l);
    }

    public void removeHoverListener(HoverListener l) {
        textArea.removeHoverListener(l);
    }

    public void addSearchRequestListener(SearchRequestListener l) {
        textArea.addSearchRequestListener(l);
    }

    public void removeSearchRequestListener(SearchRequestListener l) {
        textArea.removeSearchRequestListener(l);
    }

    public void setMoveLineUpKeyStroke(KeyStroke ks) {
        textArea.setMoveLineUpKeyStroke(ks);
    }

    public void setMoveLineDownKeyStroke(KeyStroke ks) {
        textArea.setMoveLineDownKeyStroke(ks);
    }

    public void setDuplicateLineUpKeyStroke(KeyStroke ks) {
        textArea.setDuplicateLineUpKeyStroke(ks);
    }

    public void setDuplicateLineDownKeyStroke(KeyStroke ks) {
        textArea.setDuplicateLineDownKeyStroke(ks);
    }

    public void setToggleFoldKeyStroke(KeyStroke ks) {
        textArea.setToggleFoldKeyStroke(ks);
    }

    public void setFindKeyStroke(KeyStroke ks) {
        textArea.setFindKeyStroke(ks);
    }

    public void setReplaceKeyStroke(KeyStroke ks) {
        textArea.setReplaceKeyStroke(ks);
    }

    public void setFindNextKeyStroke(KeyStroke ks) {
        textArea.setFindNextKeyStroke(ks);
    }

    public void setFindPrevKeyStroke(KeyStroke ks) {
        textArea.setFindPrevKeyStroke(ks);
    }

    public void setFormatKeyStroke(KeyStroke ks) {
        textArea.setFormatKeyStroke(ks);
    }

    public void setAutoCompleteKeyStroke(KeyStroke ks) {
        textArea.setAutoCompleteKeyStroke(ks);
    }

    public void setAddCaretBelowKeyStroke(KeyStroke ks) {
        textArea.setAddCaretBelowKeyStroke(ks);
    }

    public void setAddCaretAboveKeyStroke(KeyStroke ks) {
        textArea.setAddCaretAboveKeyStroke(ks);
    }

    public void setClearExtraCaretsKeyStroke(KeyStroke ks) {
        textArea.setClearExtraCaretsKeyStroke(ks);
    }

    public void moveLineUp() {
        textArea.moveLineUp();
    }

    public void moveLineDown() {
        textArea.moveLineDown();
    }

    public void duplicateLineUp() {
        textArea.duplicateLineUp();
    }

    public void duplicateLineDown() {
        textArea.duplicateLineDown();
    }

    public void formatDocument() {
        textArea.formatDocument();
    }

    public void formatSelection() {
        textArea.formatSelection();
    }

    public void format() {
        textArea.format();
    }

    public void refreshDiagnostics() {
        textArea.refreshDiagnostics();
    }

    public void setErrorStripeEnabled(boolean enabled) {
        errorStripe.setEnabled(enabled);
    }

    public boolean isErrorStripeEnabled() {
        return errorStripe.isEnabled();
    }

    public void setErrorStripeNavigateOnClick(boolean enabled) {
        errorStripe.setNavigateOnClick(enabled);
    }

    public boolean isErrorStripeNavigateOnClick() {
        return errorStripe.isNavigateOnClick();
    }

    public void setErrorStripeWidth(int width) {
        errorStripe.setStripeWidth(width);
    }

    public void setErrorStripeStatusIndicatorEnabled(boolean enabled) {
        errorStripe.setStatusIndicatorEnabled(enabled);
    }

    public boolean isErrorStripeStatusIndicatorEnabled() {
        return errorStripe.isStatusIndicatorEnabled();
    }

    public void addErrorStripeClickListener(ErrorStripeClickListener listener) {
        errorStripe.addClickListener(listener);
    }

    public void removeErrorStripeClickListener(ErrorStripeClickListener listener) {
        errorStripe.removeClickListener(listener);
    }

    public void setInspectionWidgetEnabled(boolean enabled) {
        inspectionWidget.setEnabled(enabled);
        revalidate();
        repaint();
    }

    public boolean isInspectionWidgetEnabled() {
        return inspectionWidget.isEnabled();
    }

    public void setInspectionWidgetNavigateOnClick(boolean enabled) {
        inspectionWidget.setNavigateOnClick(enabled);
    }

    public boolean isInspectionWidgetNavigateOnClick() {
        return inspectionWidget.isNavigateOnClick();
    }

    public void setInspectionWidgetCleanMode(CodeEditorInspectionWidget.CleanMode mode) {
        inspectionWidget.setCleanMode(mode);
    }

    public CodeEditorInspectionWidget.CleanMode getInspectionWidgetCleanMode() {
        return inspectionWidget.getCleanMode();
    }

    public void addInspectionWidgetClickListener(InspectionWidgetClickListener listener) {
        inspectionWidget.addClickListener(listener);
    }

    public void removeInspectionWidgetClickListener(InspectionWidgetClickListener listener) {
        inspectionWidget.removeClickListener(listener);
    }

    public void setInspectionWidget(CodeEditorInspectionWidget widget) {
        if (widget == null || widget == inspectionWidget) return;
        CodeEditorInspectionWidget previous = inspectionWidget;
        widget.inheritStateFrom(previous);
        if (editorOverlay != null) {
            editorOverlay.remove(previous);
            editorOverlay.add(widget, JLayeredPane.PALETTE_LAYER);
        }
        if (previous != null) previous.dispose();
        inspectionWidget = widget;
        if (editorOverlay != null) {
            editorOverlay.revalidate();
            editorOverlay.repaint();
        }
    }

    public void setInspectionWidgetFactory(InspectionWidgetFactory factory) {
        if (factory == null) return;
        setInspectionWidget(factory.create(textArea));
    }

    public void refreshInlayHints() {
        textArea.refreshInlayHints();
    }

    public void refreshCodeLenses() {
        textArea.refreshCodeLenses();
    }

    public void setCodeLensesEnabled(boolean enabled) {
        textArea.setCodeLensesEnabled(enabled);
    }

    public boolean isCodeLensesEnabled() {
        return textArea.isCodeLensesEnabled();
    }

    public void setCodeLensesAutoRunEnabled(boolean enabled) {
        textArea.setCodeLensesAutoRunEnabled(enabled);
    }

    public boolean isCodeLensesAutoRunEnabled() {
        return textArea.isCodeLensesAutoRunEnabled();
    }

    public void triggerAutoComplete() {
        textArea.triggerAutoComplete();
    }

    public boolean hasActiveSnippetSession() {
        return textArea.hasActiveSnippetSession();
    }

    public boolean snippetNextStop() {
        return textArea.snippetNextStop();
    }

    public boolean snippetPreviousStop() {
        return textArea.snippetPreviousStop();
    }

    public void clearSnippetSession() {
        textArea.clearSnippetSession();
    }

    public void setAutoCompletePopup(AutoCompletePopup popup) {
        textArea.setAutoCompletePopup(popup);
    }

    public void addAutoCompleteAcceptKeyStroke(KeyStroke ks) {
        textArea.addAutoCompleteAcceptKeyStroke(ks);
    }

    public void clearAutoCompleteAcceptKeyStrokes() {
        textArea.clearAutoCompleteAcceptKeyStrokes();
    }

    public void setHoverDocumentationPopup(HoverDocumentationPopup popup) {
        textArea.setHoverDocumentationPopup(popup);
    }

    public boolean isHoverDocumentationTextSelectionEnabled() {
        return textArea.isHoverDocumentationTextSelectionEnabled();
    }

    public void setHoverDocumentationTextSelectionEnabled(boolean enabled) {
        textArea.setHoverDocumentationTextSelectionEnabled(enabled);
    }

    public void hideHoverDocumentation() {
        textArea.hideHoverDocumentation();
    }

    public int getHoverDelay() {
        return textArea.getHoverDelay();
    }

    public void setHoverDelay(int hoverDelay) {
        textArea.setHoverDelay(hoverDelay);
    }

    public void setSignatureHelpProvider(SignatureHelpProvider provider) {
        textArea.setSignatureHelpProvider(provider);
    }

    public SignatureHelpProvider getSignatureHelpProvider() {
        return textArea.getSignatureHelpProvider();
    }

    public void setSignatureHelpPopup(SignatureHelpPopup popup) {
        textArea.setSignatureHelpPopup(popup);
    }

    public void triggerSignatureHelp() {
        textArea.triggerSignatureHelp();
    }

    public void hideSignatureHelp() {
        textArea.hideSignatureHelp();
    }

    public boolean isSignatureHelpVisible() {
        return textArea.isSignatureHelpVisible();
    }

    public void setSignatureHelpKeyStroke(KeyStroke ks) {
        textArea.setSignatureHelpKeyStroke(ks);
    }

    public void addExtraCaret(int line, int col) {
        textArea.addExtraCaret(line, col);
    }

    public void clearExtraCarets() {
        textArea.clearExtraCarets();
    }

    public boolean hasExtraCarets() {
        return textArea.hasExtraCarets();
    }

    public void addCaretBelow() {
        textArea.addCaretBelow();
    }

    public void addCaretAbove() {
        textArea.addCaretAbove();
    }

    public void searchUpdateQuery(String query, SearchOptions opts) {
        textArea.searchUpdateQuery(query, opts);
    }

    public void searchFindNext() {
        textArea.searchFindNext();
    }

    public void searchFindPrev() {
        textArea.searchFindPrev();
    }

    public void searchReplaceCurrent(String replacement) {
        textArea.searchReplaceCurrent(replacement);
    }

    public void searchReplaceAll(String replacement) {
        textArea.searchReplaceAll(replacement);
    }

    public boolean isSearchPanelVisible() {
        return textArea.isSearchPanelVisible();
    }

    public void setTokenizerProvider(TokenizerCodeEditorProvider p) {
        textArea.setTokenizerProvider(p);
    }

    public TokenizerCodeEditorProvider getTokenizerProvider() {
        return textArea.getTokenizerProvider();
    }

    public void setTokenClassifierProvider(TokenClassifierCodeEditorProvider p) {
        textArea.setTokenClassifierProvider(p);
    }

    public TokenClassifierCodeEditorProvider getTokenClassifierProvider() {
        return textArea.getTokenClassifierProvider();
    }

    public void setTokenColorProvider(TokenColorProvider p) {
        textArea.setTokenColorProvider(p);
    }

    public TokenColorProvider getTokenColorProvider() {
        return textArea.getTokenColorProvider();
    }

    public void setTokenRenderProvider(TokenRenderCodeEditorProvider p) {
        textArea.setTokenRenderProvider(p);
    }

    public TokenRenderCodeEditorProvider getTokenRenderProvider() {
        return textArea.getTokenRenderProvider();
    }

    public void setSyntaxHighlightEnabled(boolean enabled) {
        textArea.setSyntaxHighlightEnabled(enabled);
    }

    public boolean isSyntaxHighlightEnabled() {
        return textArea.isSyntaxHighlightEnabled();
    }

    public void applySyntaxHighlight() {
        textArea.applySyntaxHighlight();
    }

    public void setDiagnosticsAutoRunEnabled(boolean enabled) {
        textArea.setDiagnosticsAutoRunEnabled(enabled);
    }

    public boolean isDiagnosticsAutoRunEnabled() {
        return textArea.isDiagnosticsAutoRunEnabled();
    }

    public void setDiagnosticsDebounceMs(int ms) {
        textArea.setDiagnosticsDebounceMs(ms);
    }

    public int getDiagnosticsDebounceMs() {
        return textArea.getDiagnosticsDebounceMs();
    }

    public void setDefinitionLocationProvider(DefinitionLocationProvider p) {
        textArea.setDefinitionLocationProvider(p);
    }

    public DefinitionLocationProvider getDefinitionLocationProvider() {
        return textArea.getDefinitionLocationProvider();
    }

    public void setDefinitionProvider(DefinitionProvider p) {
        textArea.setDefinitionProvider(p);
    }

    public DefinitionProvider getDefinitionProvider() {
        return textArea.getDefinitionProvider();
    }

    public void setReferencesProvider(ReferencesProvider p) {
        textArea.setReferencesProvider(p);
    }

    public ReferencesProvider getReferencesProvider() {
        return textArea.getReferencesProvider();
    }

    public void setDocumentSymbolProvider(DocumentSymbolProvider p) {
        textArea.setDocumentSymbolProvider(p);
    }

    public DocumentSymbolProvider getDocumentSymbolProvider() {
        return textArea.getDocumentSymbolProvider();
    }

    public void setRenameProvider(RenameProvider p) {
        textArea.setRenameProvider(p);
    }

    public RenameProvider getRenameProvider() {
        return textArea.getRenameProvider();
    }

    public void setCodeActionProvider(CodeActionProvider p) {
        textArea.setCodeActionProvider(p);
    }

    public CodeActionProvider getCodeActionProvider() {
        return textArea.getCodeActionProvider();
    }

    public void setSelectionRangeProvider(SelectionRangeProvider p) {
        textArea.setSelectionRangeProvider(p);
    }

    public SelectionRangeProvider getSelectionRangeProvider() {
        return textArea.getSelectionRangeProvider();
    }

    public void setCommentProvider(CommentProvider p) {
        textArea.setCommentProvider(p);
    }

    public CommentProvider getCommentProvider() {
        return textArea.getCommentProvider();
    }

    public void setBracketMatcher(BracketMatcher m) {
        textArea.setBracketMatcher(m);
    }

    public BracketMatcher getBracketMatcher() {
        return textArea.getBracketMatcher();
    }

    public void setWordDetector(WordDetector d) {
        textArea.setWordDetector(d);
    }

    public WordDetector getWordDetector() {
        return textArea.getWordDetector();
    }

    public void setWordClickHandler(WordClickHandler handler) {
        textArea.setWordClickHandler(handler);
    }

    public WordClickHandler getWordClickHandler() {
        return textArea.getWordClickHandler();
    }

    public void addWordCaretChangeListener(WordCaretChangeListener listener) {
        textArea.addWordCaretChangeListener(listener);
    }

    public void removeWordCaretChangeListener(WordCaretChangeListener listener) {
        textArea.removeWordCaretChangeListener(listener);
    }

    public void setWordClickModifier(int modifier) {
        textArea.setWordClickModifier(modifier);
    }

    public int getWordClickModifier() {
        return textArea.getWordClickModifier();
    }

    public void setWordHoverListener(WordHoverListener listener) {
        textArea.setWordHoverListener(listener);
    }

    public WordHoverListener getWordHoverListener() {
        return textArea.getWordHoverListener();
    }

    public void setWordHoverEnabled(boolean enabled) {
        textArea.setWordHoverEnabled(enabled);
    }

    public boolean isWordHoverEnabled() {
        return textArea.isWordHoverEnabled();
    }

    public void setWordHoverStyle(WordHoverStyle style) {
        textArea.setWordHoverStyle(style);
    }

    public WordHoverStyle getWordHoverStyle() {
        return textArea.getWordHoverStyle();
    }

    public void setWordHoverPainter(WordHoverPainter painter) {
        textArea.setWordHoverPainter(painter);
    }

    public WordHoverPainter getWordHoverPainter() {
        return textArea.getWordHoverPainter();
    }

    public void setWordHoverDecorator(WordHoverDecorator decorator) {
        textArea.setWordHoverDecorator(decorator);
    }

    public WordHoverDecorator getWordHoverDecorator() {
        return textArea.getWordHoverDecorator();
    }

    public void setCommandHandler(CommandHandler h) {
        textArea.setCommandHandler(h);
    }

    public CommandHandler getCommandHandler() {
        return textArea.getCommandHandler();
    }

    public void setLocationOpener(java.util.function.Consumer<Location> opener) {
        textArea.setLocationOpener(opener);
    }

    public int applyEdits(java.util.List<TextEdit> edits) {
        return textArea.applyEdits(edits);
    }

    public void beginCompoundEdit() {
        textArea.beginCompoundEdit();
    }

    public void endCompoundEdit() {
        textArea.endCompoundEdit();
    }

    public void triggerGoToDefinition() {
        textArea.triggerGoToDefinition();
    }

    public void triggerFindReferences() {
        textArea.triggerFindReferences();
    }

    public void triggerRename() {
        textArea.triggerRename();
    }

    public void triggerCodeActions() {
        textArea.triggerCodeActions();
    }

    public void applyCodeAction(CodeAction action) {
        textArea.applyCodeAction(action);
    }

    public void addReferencesListener(java.util.function.Consumer<java.util.List<Location>> l) {
        textArea.addReferencesListener(l);
    }

    public void removeReferencesListener(java.util.function.Consumer<java.util.List<Location>> l) {
        textArea.removeReferencesListener(l);
    }

    public void toggleLineComment() {
        textArea.toggleLineComment();
    }

    public void toggleBlockComment() {
        textArea.toggleBlockComment();
    }

    public void extendSelection() {
        textArea.extendSelection();
    }

    public void shrinkSelection() {
        textArea.shrinkSelection();
    }

    public void pushNavigationHistory() {
        textArea.pushNavigationHistory();
    }

    public void navigateBack() {
        textArea.navigateBack();
    }

    public void navigateForward() {
        textArea.navigateForward();
    }

    public void clearNavigationHistory() {
        textArea.clearNavigationHistory();
    }

    public void openLocation(Location loc) {
        textArea.openLocation(loc);
    }

    public void toggleBookmarkAtCaret() {
        textArea.toggleBookmarkAtCaret();
    }

    public void addBookmark(int line) {
        textArea.addBookmark(line);
    }

    public void removeBookmark(int line) {
        textArea.removeBookmark(line);
    }

    public void toggleBookmark(int line) {
        textArea.toggleBookmark(line);
    }

    public void clearBookmarks() {
        textArea.clearBookmarks();
    }

    public void jumpToNextBookmark() {
        textArea.jumpToNextBookmark();
    }

    public void jumpToPreviousBookmark() {
        textArea.jumpToPreviousBookmark();
    }

    public boolean isBookmarked(int line) {
        return textArea.isBookmarked(line);
    }

    public java.util.SortedSet<Integer> getBookmarks() {
        return textArea.getBookmarks();
    }

    public void addBookmarkListener(Runnable r) {
        textArea.addBookmarkListener(r);
    }

    public void removeBookmarkListener(Runnable r) {
        textArea.removeBookmarkListener(r);
    }

    public void addBookmarkChangeListener(BookmarkChangeListener listener) {
        BookmarkLayer bookmark = getGutter().getLayer(BookmarkLayer.class);
        if (bookmark == null) {
            bookmark = new BookmarkLayer();
            getGutter().addLayer(bookmark);
        }
        bookmark.addBookmarkChangeListener(listener);
    }

    public void removeBookmarkChangeListener(BookmarkChangeListener listener) {
        BookmarkLayer bookmark = getGutter().getLayer(BookmarkLayer.class);
        if (bookmark != null) {
            bookmark.removeBookmarkChangeListener(listener);
        }
    }

    public int findMatchingBracket(int offset) {
        return textArea.findMatchingBracket(offset);
    }

    public CompletableFuture<Integer> findMatchingBracketAsync(int offset) {
        return textArea.findMatchingBracketAsync(offset);
    }

    public void jumpToMatchingBracket() {
        textArea.jumpToMatchingBracket();
    }

    public java.util.List<DocumentSymbol> getDocumentSymbols() {
        return textArea.getDocumentSymbols();
    }

    public CompletableFuture<List<DocumentSymbol>> getDocumentSymbolsAsync() {
        return textArea.getDocumentSymbolsAsync();
    }

    public void setGoToDefinitionKeyStroke(KeyStroke ks) {
        textArea.setGoToDefinitionKeyStroke(ks);
    }

    public void setFindReferencesKeyStroke(KeyStroke ks) {
        textArea.setFindReferencesKeyStroke(ks);
    }

    public void setRenameKeyStroke(KeyStroke ks) {
        textArea.setRenameKeyStroke(ks);
    }

    public void setCodeActionsKeyStroke(KeyStroke ks) {
        textArea.setCodeActionsKeyStroke(ks);
    }

    public void setToggleLineCommentKeyStroke(KeyStroke ks) {
        textArea.setToggleLineCommentKeyStroke(ks);
    }

    public void setToggleBlockCommentKeyStroke(KeyStroke ks) {
        textArea.setToggleBlockCommentKeyStroke(ks);
    }

    public void setExtendSelectionKeyStroke(KeyStroke ks) {
        textArea.setExtendSelectionKeyStroke(ks);
    }

    public void setShrinkSelectionKeyStroke(KeyStroke ks) {
        textArea.setShrinkSelectionKeyStroke(ks);
    }

    public void setNavigateBackKeyStroke(KeyStroke ks) {
        textArea.setNavigateBackKeyStroke(ks);
    }

    public void setNavigateForwardKeyStroke(KeyStroke ks) {
        textArea.setNavigateForwardKeyStroke(ks);
    }

    public void setToggleBookmarkKeyStroke(KeyStroke ks) {
        textArea.setToggleBookmarkKeyStroke(ks);
    }

    public void setNextBookmarkKeyStroke(KeyStroke ks) {
        textArea.setNextBookmarkKeyStroke(ks);
    }

    public void setPreviousBookmarkKeyStroke(KeyStroke ks) {
        textArea.setPreviousBookmarkKeyStroke(ks);
    }

    public void repaintBreakpointLines(){
        BreakpointLayer breakpointLayer = getGutter().getLayer(BreakpointLayer.class);
        if(breakpointLayer == null) return;
        breakpointLayer.getBreakpoints().forEach(this::onBreakpointChanged);
    }

    private void showSearchPanel(String selectedText, boolean replaceMode) {
        SearchPanel panel = textArea.getOrCreateSearchPanel();
        panel.setReplaceVisible(replaceMode && !textArea.isReadOnly());
        if (searchPanelPosition == SearchPanelPosition.POPUP) {
            mountPopup(panel);
        } else {
            mountInBorder(panel);
        }
        if (selectedText != null && !selectedText.isEmpty()) {
            panel.setQuery(selectedText);
        }
        panel.setVisible(true);
        panel.focusFindField();
        textArea.searchUpdateQuery(panel.getFindField().getText(), panel.getOptions());
        revalidate();
        repaint();
    }

    private void mountInBorder(SearchPanel panel) {
        Object constraint = switch (searchPanelPosition) {
            case BOTTOM -> BorderLayout.SOUTH;
            case LEFT -> BorderLayout.WEST;
            case RIGHT -> BorderLayout.EAST;
            default -> BorderLayout.NORTH;
        };
        if (currentMountedConstraint != null && !currentMountedConstraint.equals(constraint)) {
            unmountSearchPanel();
        }
        if (panel.getParent() != this) {
            Container p = panel.getParent();
            if (p != null) p.remove(panel);
            add(panel, constraint);
            currentMountedConstraint = constraint;
        }
        if (searchPopup != null) searchPopup.setVisible(false);
    }

    private void mountPopup(SearchPanel panel) {
        if (panel.getParent() != null && panel.getParent() != (searchPopup == null ? null : searchPopup.getContentPane())) {
            panel.getParent().remove(panel);
            currentMountedConstraint = null;
            revalidate();
            repaint();
        }
        if (searchPopup == null) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            searchPopup = new JDialog(owner);
            searchPopup.setUndecorated(true);
            searchPopup.setFocusableWindowState(true);
            searchPopup.setAlwaysOnTop(true);
        }
        if (panel.getParent() != searchPopup.getContentPane()) {
            searchPopup.getContentPane().removeAll();
            searchPopup.getContentPane().add(panel);
        }
        searchPopup.pack();
        if (isShowing()) {
            Point loc = getLocationOnScreen();
            int x = loc.x + getWidth() - searchPopup.getWidth() - 20;
            int y = loc.y + 20;
            searchPopup.setLocation(x, y);
        }
        searchPopup.setVisible(true);
    }

    private void unmountSearchPanel() {
        SearchPanel panel = textArea.getSearchPanel();
        if (panel != null) {
            Container p = panel.getParent();
            if (p != null) p.remove(panel);
        }
        if (searchPopup != null) searchPopup.setVisible(false);
        currentMountedConstraint = null;
        revalidate();
        repaint();
    }

    private void onBreakpointChanged(Breakpoint breakpoint){
        onBreakpointChanged(breakpoint, breakpoint.isActive());
    }

    private void onBreakpointChanged(Breakpoint breakpoint, boolean added){
        if (!breakpointLineHighlightEnabled || breakpoint == null) return;

        if (added && breakpoint.isActive()) {
            paintBreakpointLine(breakpoint.getLine());
        } else {
            removeBreakpointLine(breakpoint.getLine());
        }
    }
}
