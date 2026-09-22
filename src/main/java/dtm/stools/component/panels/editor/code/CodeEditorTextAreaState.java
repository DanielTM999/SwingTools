package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.CodeEditorState;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeListener;
import dtm.stools.component.panels.editor.code.api.WordClickHandler;
import dtm.stools.component.panels.editor.code.api.WordHoverDecorator;
import dtm.stools.component.panels.editor.code.api.WordHoverListener;
import dtm.stools.component.panels.editor.code.api.WordHoverPainter;
import dtm.stools.component.panels.editor.code.api.WordHoverStyle;
import dtm.stools.component.panels.editor.code.api.CommandHandler;
import dtm.stools.component.panels.editor.code.api.DocumentSymbol;
import dtm.stools.component.panels.editor.code.api.Location;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.component.panels.editor.code.ghost.GhostTextActivationMode;
import dtm.stools.component.panels.editor.code.ghost.GhostTextProvider;
import dtm.stools.component.panels.editor.code.codelens.CodeLens;
import dtm.stools.component.panels.editor.code.codelens.CodeLensProvider;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlight;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightPalette;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightProvider;
import dtm.stools.component.panels.editor.code.format.CodeFormatter;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationPopup;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpPopup;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.inlay.InlayHintProvider;
import dtm.stools.component.panels.editor.code.listeners.BookmarkChangeListener;
import dtm.stools.component.panels.editor.code.listeners.LineColorChangeListener;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.listeners.CodeEditorStateListener;
import dtm.stools.component.panels.editor.code.listeners.DiagnosticsChangeListener;
import dtm.stools.component.panels.editor.code.listeners.HoverListener;
import dtm.stools.component.panels.editor.code.listeners.LineChangeListener;
import dtm.stools.component.panels.editor.code.listeners.SearchRequestListener;
import dtm.stools.component.panels.editor.code.multicaret.Caret;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenClassifierProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenRenderProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenizerProvider;
import dtm.stools.component.panels.editor.code.search.SearchEngine;
import dtm.stools.component.panels.editor.code.search.SearchMatch;
import dtm.stools.component.panels.editor.code.search.SearchOptions;
import dtm.stools.component.panels.editor.code.search.SearchPanel;
import dtm.stools.component.panels.editor.code.utils.BracketHighlighter;
import dtm.stools.component.panels.editor.code.rename.InlineRenamePresenter;
import dtm.stools.component.panels.editor.code.rename.RenamePresenter;
import dtm.stools.i18n.I18n;
import lombok.Getter;
import lombok.Setter;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.logging.Logger;

public abstract class CodeEditorTextAreaState extends JComponent {

    protected record WordSpan(String word, int startCol, int endCol, int startOffset, int endOffset) {
    }

    protected StyledRange[] sortedStyledRanges;
    protected boolean styledRangesIndexDirty = true;
    protected Cursor codeLensPreviousCursor;
    protected boolean codeLensCursorActive;
    protected Cursor foldPlaceholderPreviousCursor;
    protected boolean foldPlaceholderCursorActive;
    protected final List<Consumer<List<Location>>> referencesListeners = new ArrayList<>();

    static String text(String key, String defaultValue) {
        return I18n.getText(CodeEditorTextArea.class, key, defaultValue);
    }

    static final int HOVER_DOCUMENTATION_HIDE_DELAY = 400;
    static final int HOVER_DOCUMENTATION_REACH_PADDING = 18;
    static final int SELECTED_TEXT_OCCURRENCES_DELAY = 75;

    static Color createDefaultSelectedTextOccurrencesColor() {
        Color base = UIManager.getColor("TextArea.selectionBackground");
        if (base == null) {
            base = new Color(51, 153, 255);
        }
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(base.getAlpha(), 64));
    }

    protected static final Map<Character, Character> DEFAULT_PAIRS = Map.of(
            '(', ')',
            '{', '}',
            '[', ']',
            '"', '"',
            '\'', '\''
    );

    @Getter
    @Setter
    protected Map<Character, Character> autoClosePairsMap = new HashMap<>(DEFAULT_PAIRS);

    protected final BracketHighlighter bracketHighlighter;

    @Getter
    protected final TextBuffer buffer;

    protected final List<HoverListener> hoverListeners = new ArrayList<>();
    protected final List<LineChangeListener> lineChangeListeners = new ArrayList<>();
    protected final List<DocumentEditListener> documentEditListeners = new ArrayList<>();
    protected final List<CodeEditorStateListener> stateListeners = new ArrayList<>();
    protected final List<WordCaretChangeListener> wordCaretChangeListeners = new ArrayList<>();
    protected CodeEditorState lastState;
    protected int lastWordCaretChangeOffset;
    protected int lastMouseX = -1;
    protected int lastMouseY = -1;

    @Getter
    protected int tabSize = 4;

    @Getter
    protected boolean useSpacesForTab = true;

    @Getter
    @Setter
    protected boolean copyPasteEnabled = true;

    @Getter
    protected boolean readOnly = false;

    @Getter
    @Setter
    protected int caretScrollLeftMargin = 8;

    @Getter
    @Setter
    protected int caretScrollRightMargin = 48;

    @Getter
    @Setter
    protected TextStyle defaultStyle = TextStyle.builder().build();

    @Getter
    @Setter
    protected Color selectionColor;

    @Getter
    protected boolean highlightSelectedTextOccurrences = true;

    @Getter
    protected Color selectedTextOccurrencesColor = createDefaultSelectedTextOccurrencesColor();

    protected volatile int[] selectedTextOccurrenceOffsets = new int[0];
    protected volatile Future<?> currentSelectedTextOccurrencesTask;
    protected final AtomicInteger selectedTextOccurrencesVersion = new AtomicInteger();
    protected ExecutorService selectedTextOccurrencesExecutor = createSelectedTextOccurrencesExecutor();
    protected Timer selectedTextOccurrencesTimer;

    @Getter
    @Setter
    protected boolean autoClosePairs = true;

    @Getter
    protected boolean showIndentGuides = true;

    @Getter
    @Setter
    protected boolean highlightCurrentLine = true;

    @Getter
    @Setter
    protected Color currentLineColor;

    @Getter
    @Setter
    protected boolean stripBlankLines = true;

    @Getter
    protected boolean smartIndentEnabled = true;

    @Getter
    protected boolean overwriteMode = false;

    @Getter
    protected KeyStroke moveLineUpKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke moveLineDownKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke duplicateLineUpKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP,
            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    @Getter
    protected KeyStroke duplicateLineDownKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    protected record LineColorInfoInternal(Color background, Color foreground, boolean priority) {}

    protected static final String ACTION_MOVE_LINE_UP = "codeEditor.moveLineUp";
    protected static final String ACTION_MOVE_LINE_DOWN = "codeEditor.moveLineDown";
    protected static final String ACTION_DUPLICATE_LINE_UP = "codeEditor.duplicateLineUp";
    protected static final String ACTION_DUPLICATE_LINE_DOWN = "codeEditor.duplicateLineDown";
    protected static final String ACTION_TOGGLE_FOLD = "codeEditor.toggleFold";
    protected static final String ACTION_AUTO_COMPLETE = "codeEditor.autoComplete";
    protected static final String ACTION_SIGNATURE_HELP = "codeEditor.signatureHelp";
    protected static final String ACTION_FORMAT = "codeEditor.format";
    protected static final String ACTION_FIND = "codeEditor.find";
    protected static final String ACTION_REPLACE = "codeEditor.replace";
    protected static final String ACTION_FIND_NEXT = "codeEditor.findNext";
    protected static final String ACTION_FIND_PREV = "codeEditor.findPrev";
    protected static final String ACTION_ADD_CARET_BELOW = "codeEditor.addCaretBelow";
    protected static final String ACTION_ADD_CARET_ABOVE = "codeEditor.addCaretAbove";
    protected static final String ACTION_GO_TO_DEFINITION = "codeEditor.goToDefinition";
    protected static final String ACTION_FIND_REFERENCES = "codeEditor.findReferences";
    protected static final String ACTION_RENAME = "codeEditor.rename";
    protected static final String ACTION_CODE_ACTIONS = "codeEditor.codeActions";
    protected static final String ACTION_TOGGLE_LINE_COMMENT = "codeEditor.toggleLineComment";
    protected static final String ACTION_TOGGLE_BLOCK_COMMENT = "codeEditor.toggleBlockComment";
    protected static final String ACTION_EXTEND_SELECTION = "codeEditor.extendSelection";
    protected static final String ACTION_SHRINK_SELECTION = "codeEditor.shrinkSelection";
    protected static final String ACTION_NAV_BACK = "codeEditor.navigateBack";
    protected static final String ACTION_NAV_FORWARD = "codeEditor.navigateForward";
    protected static final String ACTION_TOGGLE_BOOKMARK = "codeEditor.toggleBookmark";
    protected static final String ACTION_NEXT_BOOKMARK = "codeEditor.nextBookmark";
    protected static final String ACTION_PREV_BOOKMARK = "codeEditor.previousBookmark";

    @Getter
    @Setter
    protected AutoCompleteProvider autoCompleteProvider;

    @Getter
    protected AutoCompletePopup autoCompletePopup;

    protected List<int[]> snippetStops;
    protected int snippetFinalCaret = -1;
    protected int snippetStart = -1;
    protected int snippetEnd = -1;
    protected int snippetIndex = -1;

    @Getter
    @Setter
    protected boolean autoCompleteOnTyping = false;

    @Setter
    protected java.util.function.IntPredicate autoCompleteTypingTrigger;

    @Getter
    protected KeyStroke autoCompleteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
            InputEvent.CTRL_DOWN_MASK);

    @Getter
    @Setter
    protected List<KeyStroke> autoCompleteAcceptKeyStrokes = new ArrayList<>(List.of(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
    ));

    @Getter
    @Setter
    protected KeyStroke autoCompleteDismissKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    @Getter
    @Setter
    protected KeyStroke autoCompleteNextKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);

    @Getter
    @Setter
    protected KeyStroke autoCompletePrevKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);

    @Getter
    @Setter
    protected KeyStroke autoCompletePageDownKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);

    @Getter
    @Setter
    protected KeyStroke autoCompletePageUpKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);

    @Getter
    @Setter
    protected GhostTextProvider ghostTextProvider;

    @Getter
    @Setter
    protected boolean ghostTextEnabled = true;

    @Getter
    protected GhostTextActivationMode ghostTextActivationMode = GhostTextActivationMode.BOTH;

    @Getter
    @Setter
    protected int ghostTextCaretIdleDelay = 2000;

    @Getter
    @Setter
    protected KeyStroke ghostTextAcceptKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);

    @Getter
    @Setter
    protected Color ghostTextColor;

    protected String ghostText;
    protected int ghostAnchorLine = -1;
    protected int ghostAnchorCol = -1;
    protected int ghostAnchorOffset = -1;

    protected final AtomicInteger ghostTextVersion = new AtomicInteger();

    protected Timer ghostTextIdleTimer;

    protected boolean ghostIdleConsumed = false;
    protected int ghostIdleLastLine = -1;
    protected int ghostIdleLastCol = -1;

    @Getter
    @Setter
    protected CodeFormatter codeFormatter;

    @Getter
    protected KeyStroke formatKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    @Getter
    @Setter
    protected DiagnosticsProvider diagnosticsProvider;

    @Getter
    protected final List<Diagnostic> diagnostics = new ArrayList<>();

    final List<DiagnosticsChangeListener> diagnosticsChangeListeners = new CopyOnWriteArrayList<>();

    volatile List<Diagnostic> lastDiagnostics;
    volatile String lastDiagnosticsText;
    PendingHighlightEdit pendingDiagnosticsEdit;

    @Getter
    @Setter
    protected boolean diagnosticsRenderingEnabled = true;

    @Getter
    @Setter
    protected boolean diagnosticsAutoRunEnabled = true;

    @Getter
    @Setter
    protected int diagnosticsDebounceMs = 300;

    Timer diagnosticsDebounceTimer;

    @Getter
    @Setter
    protected int inlayHintsDebounceMs = 300;

    Timer inlayHintsDebounceTimer;

    @Getter
    @Setter
    protected int documentSymbolsDebounceMs = 300;

    Timer documentSymbolsDebounceTimer;

    @Getter
    protected DocumentHighlightProvider documentHighlightProvider;

    @Getter
    protected boolean documentHighlightsEnabled = true;

    @Getter
    protected int documentHighlightDebounceMs = 200;

    @Getter
    protected DocumentHighlightPalette documentHighlightPalette = DocumentHighlightPalette.defaults();

    Timer documentHighlightDebounceTimer;
    protected volatile Future<?> currentDocumentHighlightTask;
    protected final AtomicInteger documentHighlightVersion = new AtomicInteger();
    protected volatile List<ResolvedDocumentHighlight> resolvedDocumentHighlights = List.of();

    protected record ResolvedDocumentHighlight(
            int startOffset,
            int endOffset,
            DocumentHighlight.Kind kind
    ) {
    }

    @Getter
    @Setter
    protected int codeLensesDebounceMs = 500;

    Timer codeLensesDebounceTimer;

    @Getter
    @Setter
    protected TokenizerCodeEditorProvider tokenizerProvider = new DefaultTokenizerProvider();

    volatile Collection<Token> lastHighlightTokens;
    volatile String lastHighlightText;
    record PendingHighlightEdit(int offset, int removedLength, String insertedText) {}

    @Getter
    @Setter
    protected TokenClassifierCodeEditorProvider tokenClassifierProvider = new DefaultTokenClassifierProvider();

    @Getter
    @Setter
    protected TokenColorProvider tokenColorProvider = new DefaultTokenColorProvider();

    @Getter
    @Setter
    protected TokenRenderCodeEditorProvider tokenRenderProvider = new DefaultTokenRenderProvider();

    @Getter
    @Setter
    protected boolean syntaxHighlightEnabled = true;

    @Getter
    @Setter
    protected int syntaxHighlightDebounceMs = 75;

    Timer syntaxHighlightDebounceTimer;

    protected static final int MAX_SYNTAX_HIGHLIGHT_RESCUES = 3;

    Timer syntaxHighlightWatchdogTimer;

    protected int syntaxHighlightRescueAttempts;

    @Getter
    @Setter
    protected int foldingDebounceMs = 120;

    Timer foldingDebounceTimer;

    protected volatile Future<?> currentHighlightTask;

    protected volatile Future<?> currentDiagnosticsTask;

    protected final AtomicInteger highlightVersion = new AtomicInteger();

    protected final AtomicInteger diagnosticsVersion = new AtomicInteger();

    protected final AtomicInteger codeLensVersion = new AtomicInteger();

    protected final AtomicInteger inlayHintVersion = new AtomicInteger();

    protected final AtomicInteger hoverDocumentationVersion = new AtomicInteger();

    protected final AtomicInteger documentSymbolVersion = new AtomicInteger();

    protected ExecutorService highlightExecutor = createHighlightExecutor();

    protected ExecutorService diagnosticsExecutor = createDiagnosticsExecutor();

    protected ExecutorService providerExecutor = createProviderExecutor();

    protected ExecutorService wordCaretEventExecutor = createWordCaretEventExecutor();

    @Getter
    @Setter
    protected InlayHintProvider inlayHintProvider;

    @Getter
    protected final List<InlayHint> inlayHints = new ArrayList<>();

    protected volatile Future<?> currentInlayHintTask;

    @Getter
    @Setter
    protected boolean inlayHintsEnabled = true;

    @Getter
    @Setter
    protected CodeLensProvider codeLensProvider;

    @Getter
    protected final List<CodeLens> codeLenses = new ArrayList<>();

    @Getter
    protected boolean codeLensesEnabled = true;

    public void setCodeLensesEnabled(boolean codeLensesEnabled) {
        this.codeLensesEnabled = codeLensesEnabled;
        invalidateGeometry();
        revalidate();
        repaint();
    }

    @Getter
    @Setter
    protected boolean codeLensesAutoRunEnabled = true;

    @Getter
    @Setter
    protected Color codeLensForeground;

    @Getter
    @Setter
    protected float codeLensFontScale = 0.85f;

    @Getter
    @Setter
    protected int codeLensItemSpacing = 16;

    protected volatile Future<?> currentCodeLensTask;

    protected ExecutorService codeLensExecutor = createCodeLensExecutor();

    protected volatile CompletableFuture<List<AutoCompleteItem>> currentAutoCompleteTask;

    protected final AtomicInteger autoCompleteVersion = new AtomicInteger();

    protected ExecutorService autoCompleteExecutor = createAutoCompleteExecutor();

    protected static final class CodeLensItemBounds {
        final CodeLens lens;
        final int itemIndex;
        final int x;
        final int y;
        final int w;
        final int h;
        CodeLensItemBounds(CodeLens lens, int itemIndex, int x, int y, int w, int h) {
            this.lens = lens;
            this.itemIndex = itemIndex;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    protected final List<CodeLensItemBounds> codeLensItemBounds = new ArrayList<>();

    protected static final Logger CODE_LENS_LOG = Logger.getLogger(CodeEditorTextArea.class.getName());

    protected final List<DocumentSymbol> documentSymbols = new ArrayList<>();

    protected static final int TEXT_LEFT_MARGIN = 4;
    protected static final int CARET_WIDTH = 2;
    protected static final int MAX_REMEASURED_LINES = 64;

    transient Graphics2D activePaintGraphics;
    transient Map<Font, FontMetrics> activePaintFontMetrics;
    FontRenderContext lastPaintFontRenderContext;

    protected boolean geometryCacheDirty = true;
    protected int[] cachedLineY;
    protected int[] cachedVisibleLines;
    protected int cachedTotalHeight;
    protected int cachedLineHeight;
    protected int cachedLensRowCountValue;
    protected int cachedLineCount;
    protected int cachedMaxLineWidth = -1;
    protected int cachedIndentUnit = -1;
    protected BitSet cachedHiddenLines;
    protected Map<Integer, CodeLens> cachedAboveLensByLine;
    protected Map<Integer, CodeLens> cachedInlineLensByLine;

    @Getter
    @Setter
    protected HoverDocumentationProvider hoverDocumentationProvider;

    @Getter
    protected HoverDocumentationPopup hoverDocumentationPopup;
    protected boolean hoverDocumentationTextSelectionEnabled = true;
    protected Rectangle hoverDocumentationTransitionBounds;

    @Getter
    @Setter
    protected SignatureHelpProvider signatureHelpProvider;

    @Getter
    protected SignatureHelpPopup signatureHelpPopup;

    protected final AtomicInteger signatureHelpVersion = new AtomicInteger();

    @Getter
    protected KeyStroke signatureHelpKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_P,
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    @Getter
    @Setter
    protected KeyStroke signatureHelpDismissKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    @Getter
    @Setter
    protected KeyStroke signatureHelpNextKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
            InputEvent.CTRL_DOWN_MASK);

    @Getter
    @Setter
    protected KeyStroke signatureHelpPrevKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP,
            InputEvent.CTRL_DOWN_MASK);

    @Getter
    @Setter
    protected boolean multiCaretEnabled = true;

    protected final List<Caret> extraCarets = new ArrayList<>();

    @Getter
    protected KeyStroke addCaretBelowKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke addCaretAboveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    @Setter
    @Getter
    protected KeyStroke clearExtraCaretsKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    protected final SearchEngine searchEngine = new SearchEngine();

    @Setter
    @Getter
    protected SearchPanel searchPanel;

    @Getter
    protected String searchQuery = "";

    @Getter
    protected SearchOptions searchOptions = new SearchOptions();

    @Getter
    protected final List<SearchMatch> searchMatches = new ArrayList<>();

    protected final List<SearchRequestListener> searchRequestListeners = new ArrayList<>();

    @Getter
    protected int searchCurrentIndex = -1;

    @Getter
    @Setter
    protected Color searchHighlightColor;

    @Getter
    @Setter
    protected Color searchCurrentHighlightColor;

    @Getter
    protected KeyStroke findKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);

    @Getter
    protected KeyStroke replaceKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);

    @Getter
    protected KeyStroke findNextKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);

    @Getter
    protected KeyStroke findPrevKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);

    @Getter
    protected boolean foldingEnabled = false;

    protected final List<FoldRule> foldRules = new ArrayList<>();
    protected List<FoldRegion> foldRegions = new ArrayList<>();
    protected final List<Runnable> foldStateListeners = new ArrayList<>();
    protected boolean suppressFoldRestore = false;

    @Getter
    protected KeyStroke toggleFoldKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, InputEvent.CTRL_DOWN_MASK);

    @Getter
    @Setter
    protected Color foldPlaceholderColor;

    @Getter
    @Setter
    protected String foldPlaceholder = " … ";

    @Getter
    @Setter
    protected boolean foldPlaceholderWithSeparators = true;

    @Getter
    @Setter
    protected boolean foldPreviewOnHoverEnabled = true;

    @Getter
    @Setter
    protected int foldPreviewMaxLines = 15;

    @Getter
    @Setter
    protected int foldPreviewMaxColumns = 120;

    protected JWindow foldPreviewWindow;
    protected Window foldPreviewOwnerWindow;
    protected int foldPreviewLine = -1;

    protected JPopupMenu activeContextMenu;
    protected final AtomicInteger contextMenuVersion = new AtomicInteger();

    public record NavigationEntry(int line, int col) {}

    protected Timer hoverTimer;
    protected Timer hoverDocumentationHideTimer;
    protected int hoverLine = -1;
    protected int hoverCol = -1;

    @Getter
    protected int hoverDelay = 800;

    @Getter
    protected final List<StyledRange> styledRanges = new ArrayList<>();
    protected final Map<Integer, LineColorInfoInternal> lineColors = new HashMap<>();

    @Getter
    @Setter
    protected WordDetector wordDetector = WordDetector.defaultDetector();

    @Getter
    @Setter
    protected WordClickHandler wordClickHandler;

    @Getter
    @Setter
    protected int wordClickModifier = InputEvent.CTRL_DOWN_MASK;

    @Getter
    @Setter
    protected WordHoverListener wordHoverListener;

    @Getter
    @Setter
    protected boolean wordHoverEnabled = true;

    @Getter
    @Setter
    protected WordHoverStyle wordHoverStyle = WordHoverStyle.defaultStyle();

    @Getter
    @Setter
    protected WordHoverDecorator wordHoverDecorator;

    @Getter
    @Setter
    protected WordHoverPainter wordHoverPainter;

    protected WordHoverStyle wordHoverActiveStyle;

    protected int wordHoverLine = -1;
    protected int wordHoverStartCol = -1;
    protected int wordHoverEndCol = -1;
    protected Cursor wordHoverPreviousCursor;
    protected int wordHoverLastMouseX = -1;
    protected int wordHoverLastMouseY = -1;

    @Getter
    @Setter
    protected CommandHandler commandHandler;

    @Getter
    @Setter
    protected Consumer<Location> locationOpener;

    protected final Deque<NavigationEntry> navBackStack = new ArrayDeque<>();
    protected final Deque<NavigationEntry> navForwardStack = new ArrayDeque<>();

    @Getter
    @Setter
    protected int navigationHistoryLimit = 50;

    @Getter
    protected final SortedSet<Integer> bookmarks = new TreeSet<>();

    protected final List<Runnable> bookmarkListeners = new ArrayList<>();
    protected final List<BookmarkChangeListener> bookmarkChangeListeners = new ArrayList<>();
    protected final List<LineColorChangeListener> lineColorChangeListeners = new ArrayList<>();

    @Getter
    protected KeyStroke goToDefinitionKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);

    @Getter
    protected KeyStroke findReferencesKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke renameKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK);

    @Getter
    protected KeyStroke codeActionsKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke toggleLineCommentKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK);

    @Getter
    protected KeyStroke toggleBlockCommentKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    @Getter
    protected KeyStroke extendSelectionKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);

    @Getter
    protected KeyStroke shrinkSelectionKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    @Getter
    protected KeyStroke navigateBackKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke navigateForwardKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

    @Getter
    protected KeyStroke toggleBookmarkKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

    @Getter
    protected KeyStroke nextBookmarkKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK);

    @Getter
    protected KeyStroke previousBookmarkKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F8, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);

    @Getter @Setter
    protected DefinitionLocationProvider definitionLocationProvider;

    @Getter @Setter
    protected DefinitionProvider definitionProvider;

    @Getter @Setter
    protected ReferencesProvider referencesProvider;

    @Getter @Setter
    protected DocumentSymbolProvider documentSymbolProvider;

    @Getter @Setter
    protected RenameProvider renameProvider;

    @Getter @Setter
    protected RenamePresenter renamePresenter = new InlineRenamePresenter();

    @Getter @Setter
    protected CodeActionProvider codeActionProvider;

    @Getter @Setter
    protected SelectionRangeProvider selectionRangeProvider;

    @Getter @Setter
    protected CommentProvider commentProvider;

    @Getter @Setter
    protected ContextMenuProvider contextMenuProvider;

    @Getter @Setter
    protected boolean contextMenuEnabled = true;

    @Getter
    protected BracketMatcher bracketMatcher = BracketMatcher.defaultMatcher();

    protected final Deque<Range> selectionExpansionStack = new ArrayDeque<>();
    protected List<Range> selectionChainCache = Collections.emptyList();
    protected int selectionChainIndex = -1;

    @Getter
    protected int caretLine = 0;
    @Getter
    protected int caretCol = 0;
    protected int desiredCaretCol = -1;
    protected int inlayInteractionLine = -1;
    protected int inlayInteractionCol = -1;
    @Getter
    protected int selectionStartLine = -1;
    @Getter
    protected int selectionStartCol = -1;
    protected int cleanBufferVersion;
    protected boolean caretVisible = true;
    protected Timer caretTimer;

    protected record ExtraCaretState(int caretOffset, int selectionAnchorOffset) {}
    protected record EditorState(int caretOffset, int selectionAnchorOffset, List<ExtraCaretState> extraCarets) {}
    protected record CaretDeleteOp(int originalOffset, int start, int end, boolean primary) {}

    protected CodeEditorTextAreaState(TextBuffer buffer) {
        this.buffer = buffer;
        this.bracketHighlighter = createBracketHighlighter(buffer);
    }

    protected abstract BracketHighlighter createBracketHighlighter(TextBuffer buffer);

    public abstract void invalidateGeometry();

    protected void ensureExecutorsStarted() {
        getHighlightExecutor();
        getDiagnosticsExecutor();
        getProviderExecutor();
        getWordCaretEventExecutor();
        getCodeLensExecutor();
        getAutoCompleteExecutor();
        getSelectedTextOccurrencesExecutor();
    }

    void cancelFuture(Future<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    protected synchronized ExecutorService getHighlightExecutor() {
        if (!isExecutorActive(highlightExecutor)) {
            highlightExecutor = createHighlightExecutor();
        }
        return highlightExecutor;
    }

    protected synchronized ExecutorService getDiagnosticsExecutor() {
        if (!isExecutorActive(diagnosticsExecutor)) {
            diagnosticsExecutor = createDiagnosticsExecutor();
        }
        return diagnosticsExecutor;
    }

    protected synchronized ExecutorService getProviderExecutor() {
        if (!isExecutorActive(providerExecutor)) {
            providerExecutor = createProviderExecutor();
        }
        return providerExecutor;
    }

    protected synchronized ExecutorService getWordCaretEventExecutor() {
        if (!isExecutorActive(wordCaretEventExecutor)) {
            wordCaretEventExecutor = createWordCaretEventExecutor();
        }
        return wordCaretEventExecutor;
    }

    protected synchronized ExecutorService getCodeLensExecutor() {
        if (!isExecutorActive(codeLensExecutor)) {
            codeLensExecutor = createCodeLensExecutor();
        }
        return codeLensExecutor;
    }

    protected synchronized ExecutorService getAutoCompleteExecutor() {
        if (!isExecutorActive(autoCompleteExecutor)) {
            autoCompleteExecutor = createAutoCompleteExecutor();
        }
        return autoCompleteExecutor;
    }

    protected synchronized ExecutorService getSelectedTextOccurrencesExecutor() {
        if (!isExecutorActive(selectedTextOccurrencesExecutor)) {
            selectedTextOccurrencesExecutor = createSelectedTextOccurrencesExecutor();
        }
        return selectedTextOccurrencesExecutor;
    }

    boolean isExecutorActive(ExecutorService executor) {
        return executor != null && !executor.isShutdown() && !executor.isTerminated();
    }

    ExecutorService createHighlightExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1), r -> {
                    Thread t = new Thread(r, "CodeEditorTextArea-Highlight");
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.DiscardOldestPolicy());
        return executor;
    }

    ExecutorService createDiagnosticsExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-Diagnostics");
            t.setDaemon(true);
            return t;
        });
    }

    ExecutorService createProviderExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-Provider");
            t.setDaemon(true);
            return t;
        });
    }

    ExecutorService createWordCaretEventExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-WordCaretEvent");
            t.setDaemon(true);
            return t;
        });
    }

    ExecutorService createCodeLensExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-CodeLens");
            t.setDaemon(true);
            return t;
        });
    }

    ExecutorService createAutoCompleteExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-AutoComplete");
            t.setDaemon(true);
            return t;
        });
    }

    ExecutorService createSelectedTextOccurrencesExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-SelectedTextOccurrences");
            t.setDaemon(true);
            return t;
        });
    }

    protected void shutdownExecutors() {
        shutdownExecutor(highlightExecutor);
        shutdownExecutor(diagnosticsExecutor);
        shutdownExecutor(providerExecutor);
        shutdownExecutor(wordCaretEventExecutor);
        shutdownExecutor(codeLensExecutor);
        shutdownExecutor(autoCompleteExecutor);
        shutdownExecutor(selectedTextOccurrencesExecutor);
    }

    void shutdownExecutor(ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

}
