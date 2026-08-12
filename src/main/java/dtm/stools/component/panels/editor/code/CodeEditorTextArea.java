package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.CodeAction;
import dtm.stools.component.panels.editor.code.api.CodeEditorState;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeEvent;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeListener;
import dtm.stools.component.panels.editor.code.api.WordClickEvent;
import dtm.stools.component.panels.editor.code.api.WordClickHandler;
import dtm.stools.component.panels.editor.code.api.WordHoverContext;
import dtm.stools.component.panels.editor.code.api.WordHoverDecorator;
import dtm.stools.component.panels.editor.code.api.WordHoverListener;
import dtm.stools.component.panels.editor.code.api.WordHoverPainter;
import dtm.stools.component.panels.editor.code.api.WordHoverStyle;
import dtm.stools.component.panels.editor.code.api.CommandHandler;
import dtm.stools.component.panels.editor.code.api.DocumentSymbol;
import dtm.stools.component.panels.editor.code.api.Location;
import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteEditApplier;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import dtm.stools.component.panels.editor.code.autocomplete.SnippetExpansion;
import dtm.stools.component.panels.editor.code.ghost.GhostTextActivationMode;
import dtm.stools.component.panels.editor.code.ghost.GhostTextContext;
import dtm.stools.component.panels.editor.code.ghost.GhostTextProvider;
import dtm.stools.component.panels.editor.code.codelens.CodeLens;
import dtm.stools.component.panels.editor.code.codelens.CodeLensClickEvent;
import dtm.stools.component.panels.editor.code.codelens.CodeLensContext;
import dtm.stools.component.panels.editor.code.codelens.CodeLensItem;
import dtm.stools.component.panels.editor.code.codelens.CodeLensPlacement;
import dtm.stools.component.panels.editor.code.codelens.CodeLensProvider;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsChange;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsContext;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.format.CodeFormatter;
import dtm.stools.component.panels.editor.code.format.FormatContext;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationContext;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationPopup;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.hover.HoverInfo;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.signature.SignatureHelp;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpContext;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpPopup;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.inlay.InlayHintContext;
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
import dtm.stools.component.panels.editor.code.prototype.LineColorInfo;
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
import dtm.stools.i18n.I18n;
import lombok.Getter;
import lombok.Setter;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public class CodeEditorTextArea extends JComponent {

    private static String text(String key, String defaultValue) {
        return I18n.getText(CodeEditorTextArea.class, key, defaultValue);
    }

    private static final int HOVER_DOCUMENTATION_HIDE_DELAY = 400;
    private static final int HOVER_DOCUMENTATION_REACH_PADDING = 18;
    private static final int SELECTED_TEXT_OCCURRENCES_DELAY = 75;

    private static Color createDefaultSelectedTextOccurrencesColor() {
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

    protected record LineColorInfoInternal(Color background, Color foreground) {}

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

    private final List<DiagnosticsChangeListener> diagnosticsChangeListeners = new CopyOnWriteArrayList<>();

    private volatile List<Diagnostic> lastDiagnostics;
    private volatile String lastDiagnosticsText;
    private PendingHighlightEdit pendingDiagnosticsEdit;

    @Getter
    @Setter
    protected boolean diagnosticsRenderingEnabled = true;

    @Getter
    @Setter
    protected boolean diagnosticsAutoRunEnabled = true;

    @Getter
    @Setter
    protected int diagnosticsDebounceMs = 300;

    private Timer diagnosticsDebounceTimer;

    @Getter
    @Setter
    protected TokenizerCodeEditorProvider tokenizerProvider = new DefaultTokenizerProvider();

    private volatile Collection<Token> lastHighlightTokens;
    private volatile String lastHighlightText;
    private PendingHighlightEdit pendingHighlightEdit;

    private record PendingHighlightEdit(int offset, int removedLength, String insertedText) {}

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

    protected final List<DocumentSymbol> documentSymbols = new ArrayList<>();

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
    protected int foldPreviewLine = -1;

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

    public CodeEditorTextArea() {
        this(new TextBuffer());
    }

    public CodeEditorTextArea(String initialText) {
        this(new TextBuffer(initialText != null ? initialText.replace("\r\n", "\n").replace("\r", "\n") : ""));
    }

    protected CodeEditorTextArea(TextBuffer buffer) {
        this.buffer = buffer;
        this.cleanBufferVersion = buffer.getVersion();
        this.lastWordCaretChangeOffset = caretOffset();
        setFocusable(true);
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setFocusTraversalKeysEnabled(false);
        setupCaretBlink();
        addKeyListener(createKeyHandler());
        MouseAdapter mh = createMouseHandler();
        addMouseListener(mh);
        addMouseMotionListener((MouseMotionListener) mh);
        addFocusListener(createFocusHandler());
        setupHover();
        bracketHighlighter = createBracketHighlighter(buffer);
        bracketHighlighter.setLineToVisualMapper(this::bufferLineToVisualLine);
        bracketHighlighter.setLineToYMapper(this::yOfBufferLine);
        bracketHighlighter.setLineHiddenPredicate(this::isLineHidden);
        setupMoveLineActions();
        installIdeActions();
        documentEditListeners.add(new DocumentEditListener() {
            @Override
            public void onInsert(int offset, String text) {
                clearGhostText();
                suppressHoverWhileEditing();
                shiftStyledRangesForEdit(offset, 0, text.length());
                PendingHighlightEdit edit = new PendingHighlightEdit(offset, 0, text);
                pendingHighlightEdit = edit;
                pendingDiagnosticsEdit = edit;
            }

            @Override
            public void onDelete(int offset, String removed) {
                clearGhostText();
                suppressHoverWhileEditing();
                shiftStyledRangesForEdit(offset, removed.length(), 0);
                PendingHighlightEdit edit = new PendingHighlightEdit(offset, removed.length(), "");
                pendingHighlightEdit = edit;
                pendingDiagnosticsEdit = edit;
            }

            @Override
            public void onTextChanged() {
                refreshSearchOnTextChange();
                scheduleSelectedTextOccurrencesRefresh();
                selectionChainCache = Collections.emptyList();
                selectionChainIndex = -1;
                applySyntaxHighlight();
                invalidateGeometry();
                if (diagnosticsAutoRunEnabled) {
                    scheduleDiagnosticsRefresh();
                }
                if (codeLensesAutoRunEnabled) {
                    refreshCodeLensesAsync();
                }
                if (inlayHintsEnabled && inlayHintProvider != null) {
                    refreshInlayHints();
                }
                if (documentSymbolProvider != null) {
                    refreshDocumentSymbolsAsync();
                }
            }
        });
        applySyntaxHighlight();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ensureExecutorsStarted();
        scheduleSelectedTextOccurrencesRefresh();
    }

    @Override
    public void removeNotify() {
        cancelAsyncWork();
        shutdownExecutors();
        super.removeNotify();
    }

    protected void ensureExecutorsStarted() {
        getHighlightExecutor();
        getDiagnosticsExecutor();
        getProviderExecutor();
        getWordCaretEventExecutor();
        getCodeLensExecutor();
        getAutoCompleteExecutor();
        getSelectedTextOccurrencesExecutor();
    }

    protected void cancelAsyncWork() {
        highlightVersion.incrementAndGet();
        diagnosticsVersion.incrementAndGet();
        codeLensVersion.incrementAndGet();
        inlayHintVersion.incrementAndGet();
        hoverDocumentationVersion.incrementAndGet();
        documentSymbolVersion.incrementAndGet();
        autoCompleteVersion.incrementAndGet();
        ghostTextVersion.incrementAndGet();
        signatureHelpVersion.incrementAndGet();
        selectedTextOccurrencesVersion.incrementAndGet();

        cancelFuture(currentHighlightTask);
        cancelFuture(currentDiagnosticsTask);
        cancelFuture(currentInlayHintTask);
        cancelFuture(currentCodeLensTask);
        cancelFuture(currentSelectedTextOccurrencesTask);
        if (selectedTextOccurrencesTimer != null) {
            selectedTextOccurrencesTimer.stop();
        }
        CompletableFuture<List<AutoCompleteItem>> autoCompleteTask = currentAutoCompleteTask;
        if (autoCompleteTask != null && !autoCompleteTask.isDone()) {
            autoCompleteTask.cancel(true);
        }

        if (diagnosticsDebounceTimer != null) diagnosticsDebounceTimer.stop();
        if (ghostTextIdleTimer != null) ghostTextIdleTimer.stop();
        if (hoverTimer != null) hoverTimer.stop();
        if (hoverDocumentationHideTimer != null) hoverDocumentationHideTimer.stop();

        hideAutoCompletePopup();
        clearGhostText();
        hideHoverDocumentation();
        hideSignatureHelp();
    }

    private void cancelFuture(Future<?> future) {
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

    private boolean isExecutorActive(ExecutorService executor) {
        return executor != null && !executor.isShutdown() && !executor.isTerminated();
    }

    private ExecutorService createHighlightExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-Highlight");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createDiagnosticsExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-Diagnostics");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createProviderExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-Provider");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createWordCaretEventExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-WordCaretEvent");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createCodeLensExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-CodeLens");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createAutoCompleteExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CodeEditorTextArea-AutoComplete");
            t.setDaemon(true);
            return t;
        });
    }

    private ExecutorService createSelectedTextOccurrencesExecutor() {
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

    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    protected void refreshSearchOnTextChange() {
        if (searchQuery == null || searchQuery.isEmpty()) return;
        int previousIndex = searchCurrentIndex;
        int anchorOffset = -1;
        if (previousIndex >= 0 && previousIndex < searchMatches.size()) {
            anchorOffset = searchMatches.get(previousIndex).startOffset();
        }
        searchMatches.clear();
        searchMatches.addAll(searchEngine.findAll(buffer, searchQuery, searchOptions));
        if (searchMatches.isEmpty()) {
            searchCurrentIndex = -1;
        } else {
            int from = anchorOffset >= 0 ? anchorOffset : caretOffset();
            SearchMatch next = searchEngine.findNext(searchMatches, from, searchOptions.isWrapAround());
            searchCurrentIndex = (next != null) ? searchMatches.indexOf(next) : 0;
        }
        updateSearchPanelCount();
        repaint();
    }

    protected KeyAdapter createKeyHandler() {
        return new KeyHandler();
    }

    protected MouseAdapter createMouseHandler() {
        return new MouseHandler();
    }

    protected FocusAdapter createFocusHandler() {
        return new FocusHandler();
    }

    protected BracketHighlighter createBracketHighlighter(TextBuffer buffer) {
        return new BracketHighlighter(buffer);
    }

    protected AutoCompletePopup createAutoCompletePopup() {
        AutoCompletePopup popup = new AutoCompletePopup(this);
        configureAutoCompletePopup(popup);
        return popup;
    }

    protected AutoCompletePopup getOrCreateAutoCompletePopup() {
        if (autoCompletePopup == null) autoCompletePopup = createAutoCompletePopup();
        return autoCompletePopup;
    }

    public void setAutoCompletePopup(AutoCompletePopup popup) {
        if (this.autoCompletePopup != null) this.autoCompletePopup.hide();
        this.autoCompletePopup = popup;
        configureAutoCompletePopup(this.autoCompletePopup);
    }

    protected void configureAutoCompletePopup(AutoCompletePopup popup) {
        if (popup != null) {
            popup.setAcceptHandler(this::applyAutoCompleteSelection);
        }
    }

    public void addAutoCompleteAcceptKeyStroke(KeyStroke ks) {
        if (ks != null && !autoCompleteAcceptKeyStrokes.contains(ks)) autoCompleteAcceptKeyStrokes.add(ks);
    }

    public void clearAutoCompleteAcceptKeyStrokes() {
        autoCompleteAcceptKeyStrokes.clear();
    }

    protected boolean isAutoCompleteAccept(KeyEvent e) {
        for (KeyStroke ks : autoCompleteAcceptKeyStrokes) {
            if (matchesKeyStroke(e, ks)) return true;
        }
        return false;
    }

    protected String computeWordPrefix(int offset) {
        int start = offset;
        while (start > 0) {
            char c = buffer.charAt(start - 1);
            if (!(Character.isLetterOrDigit(c) || c == '_')) break;
            start--;
        }
        return buffer.substring(start, offset);
    }

    public void triggerAutoComplete() {
        triggerAutoComplete(CompletionContext.TriggerKind.EXPLICIT);
    }

    protected boolean shouldAttemptTypingTrigger(char c) {
        IntPredicate trigger = autoCompleteTypingTrigger;
        if (trigger == null) {
            return Character.isLetterOrDigit(c) || c == '_';
        }
        return trigger.test(c);
    }

    protected void triggerAutoComplete(CompletionContext.TriggerKind kind) {
        if (autoCompleteProvider == null) return;
        AutoCompleteProvider provider = autoCompleteProvider;
        int caretOff = caretOffset();
        String prefix = computeWordPrefix(caretOff);
        int insertOff = caretOff - prefix.length();
        CompletionContext ctx = createAutoCompleteContext(caretOff, caretLine, caretCol, prefix, insertOff, kind);
        if (kind == CompletionContext.TriggerKind.TYPING) {
            getAutoCompleteExecutor().submit(() -> {
                boolean shouldTrigger;
                try {
                    shouldTrigger = provider.shouldAutoTrigger(ctx);
                } catch (Exception ignored) {
                    shouldTrigger = false;
                }
                if (!shouldTrigger) return;
                SwingUtilities.invokeLater(() -> showAutoCompleteLoadingAndRequest(provider, ctx, prefix, insertOff));
            });
        } else {
            showAutoCompleteLoadingAndRequest(provider, ctx, prefix, insertOff);
        }
    }

    protected void showAutoCompleteLoadingAndRequest(AutoCompleteProvider provider,
                                                     CompletionContext ctx,
                                                     String prefix,
                                                     int insertOff) {
        if (provider != autoCompleteProvider) return;
        AutoCompletePopup p = getOrCreateAutoCompletePopup();
        Point pt = caretScreenPoint();
        p.showLoading(pt.x, pt.y, prefix, insertOff);
        requestAutoComplete(provider, ctx, pt);
    }

    protected CompletionContext createAutoCompleteContext(int caretOff,
                                                          int caretLine,
                                                          int caretCol,
                                                          String prefix,
                                                          int insertOff,
                                                          CompletionContext.TriggerKind kind) {
        TextBuffer snapshot = new TextBuffer(buffer.getText());
        return new CompletionContext(snapshot, caretOff, caretLine, caretCol, prefix, insertOff, kind);
    }

    protected void requestAutoComplete(AutoCompleteProvider provider, CompletionContext ctx, Point popupPoint) {
        int request = autoCompleteVersion.incrementAndGet();
        CompletableFuture<List<AutoCompleteItem>> previous = currentAutoCompleteTask;
        if (previous != null && !previous.isDone()) previous.cancel(true);

        ExecutorService executor = getAutoCompleteExecutor();
        executor.submit(() -> {
            CompletableFuture<List<AutoCompleteItem>> task;
            try {
                task = provider.getSuggestionsAsync(ctx, executor);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(this::hideAutoCompletePopup);
                return;
            }
            if (task == null) {
                SwingUtilities.invokeLater(this::hideAutoCompletePopup);
                return;
            }
            currentAutoCompleteTask = task;
            task.whenComplete((items, error) -> SwingUtilities.invokeLater(() -> {
                if (request != autoCompleteVersion.get() || currentAutoCompleteTask != task) return;
                if (error != null || task.isCancelled()) {
                    hideAutoCompletePopup();
                    return;
                }
                if (items == null || items.isEmpty()) {
                    hideAutoCompletePopup();
                    return;
                }
                AutoCompletePopup p = getOrCreateAutoCompletePopup();
                p.show(items, popupPoint.x, popupPoint.y, ctx.prefix(), ctx.prefixOffset());
            }));
        });
    }

    protected Point caretScreenPoint() {
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight();
        String lineText = buffer.lineAt(caretLine);
        int cx = baseVisualXForColumn(caretLine, lineText, caretCol, fm);
        int cy = yOfBufferLine(caretLine) + lineHeight;
        return new Point(cx, cy);
    }

    protected void applyAutoCompleteSelection() {
        if (readOnly) {
            hideAutoCompletePopup();
            return;
        }
        if (autoCompletePopup == null || !autoCompletePopup.isVisible()) return;
        if (autoCompletePopup.isLoading()) return;
        AutoCompleteItem item = autoCompletePopup.getSelectedItem();
        if (item == null) {
            autoCompletePopup.hide();
            return;
        }
        int insertOff = autoCompletePopup.getTriggerOffset();
        int caretOff = caretOffset();
        beginCompoundEdit();
        try {
            clearSnippetSession();
            AutoCompleteEditApplier.Plan editPlan = applyLeadingAdditionalEdits(item, insertOff, caretOff);
            insertOff += editPlan.leadingDelta();
            caretOff += editPlan.leadingDelta();
            int prefixLen = Math.max(0, caretOff - insertOff);
            if (prefixLen > 0) {
                deleteText(insertOff, caretOff);
            }
            int mainLen;
            if (item.isSnippet()) {
                SnippetExpansion.Result result = SnippetExpansion.expand(item.insertText());
                insertText(insertOff, result.text());
                mainLen = result.text().length();
                startSnippetSession(insertOff, result);
            } else {
                String text = item.insertText();
                insertText(insertOff, text);
                mainLen = text == null ? 0 : text.length();
                setCaretFromOffset(insertOff + mainLen);
                clearSelection();
            }
            applyTrailingAdditionalEdits(editPlan, editPlan.leadingDelta() + (mainLen - prefixLen));
        } finally {
            endCompoundEdit();
        }
        autoCompletePopup.hide();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    private AutoCompleteEditApplier.Plan applyLeadingAdditionalEdits(AutoCompleteItem item, int insertOff, int caretOff) {
        AutoCompleteEditApplier.Plan plan = AutoCompleteEditApplier.plan(resolveAdditionalEdits(item), insertOff, caretOff);
        for (AutoCompleteEditApplier.ResolvedEdit edit : plan.leading()) {
            if (edit.end() > edit.start()) {
                deleteText(edit.start(), edit.end());
            }
            insertText(edit.start(), edit.newText());
        }
        return plan;
    }

    private void applyTrailingAdditionalEdits(AutoCompleteEditApplier.Plan plan, int shift) {
        for (AutoCompleteEditApplier.ResolvedEdit edit : plan.trailing()) {
            int start = edit.start() + shift;
            int end = edit.end() + shift;
            if (end > start) {
                deleteText(start, end);
            }
            insertText(start, edit.newText());
        }
    }

    private List<AutoCompleteEditApplier.ResolvedEdit> resolveAdditionalEdits(AutoCompleteItem item) {
        List<TextEdit> edits = item == null ? null : item.additionalTextEdits();
        if (edits == null || edits.isEmpty()) {
            return List.of();
        }
        List<AutoCompleteEditApplier.ResolvedEdit> resolved = new ArrayList<>(edits.size());
        for (TextEdit edit : edits) {
            if (edit == null || edit.range() == null) {
                continue;
            }
            int start = offsetOfPosition(edit.range().start());
            int end = offsetOfPosition(edit.range().end());
            resolved.add(new AutoCompleteEditApplier.ResolvedEdit(start, end, edit.newText()));
        }
        return resolved;
    }

    private int offsetOfPosition(Position position) {
        if (position == null) {
            return 0;
        }
        int base = buffer.offsetOfLine(position.line());
        int off = base + Math.max(0, position.col());
        return Math.max(0, Math.min(off, buffer.length()));
    }

    public boolean hasGhostText() {
        return ghostText != null && !ghostText.isEmpty();
    }

    public void setGhostTextActivationMode(GhostTextActivationMode mode) {
        this.ghostTextActivationMode = mode == null ? GhostTextActivationMode.BOTH : mode;
        if (this.ghostTextActivationMode == GhostTextActivationMode.DISABLED) {
            stopGhostIdleTimer();
            clearGhostText();
        }
    }

    protected boolean isGhostTextActive() {
        return ghostTextEnabled && ghostTextActivationMode != GhostTextActivationMode.DISABLED;
    }

    protected boolean isGhostTypingActivation() {
        return ghostTextActivationMode == GhostTextActivationMode.TYPING
                || ghostTextActivationMode == GhostTextActivationMode.BOTH;
    }

    protected boolean isGhostCaretIdleActivation() {
        return ghostTextActivationMode == GhostTextActivationMode.CARET_IDLE
                || ghostTextActivationMode == GhostTextActivationMode.BOTH;
    }

    protected void stopGhostIdleTimer() {
        if (ghostTextIdleTimer != null) {
            ghostTextIdleTimer.stop();
        }
    }

    protected void scheduleGhostIdleTimer() {
        if (!isGhostTextActive() || !isGhostCaretIdleActivation()
                || ghostTextProvider == null || readOnly
                || hasSelection() || hasGhostText()
                || !isFocusOwner()) {
            stopGhostIdleTimer();
            return;
        }

        if (caretLine != ghostIdleLastLine || caretCol != ghostIdleLastCol) {
            ghostIdleLastLine = caretLine;
            ghostIdleLastCol = caretCol;
            ghostIdleConsumed = false;
        }

        if (ghostIdleConsumed) {
            stopGhostIdleTimer();
            return;
        }
        if (ghostTextIdleTimer == null) {
            ghostTextIdleTimer = new Timer(ghostTextCaretIdleDelay, e -> fireGhostIdle());
            ghostTextIdleTimer.setRepeats(false);
        } else {
            ghostTextIdleTimer.setInitialDelay(ghostTextCaretIdleDelay);
        }
        ghostTextIdleTimer.restart();
    }

    protected void fireGhostIdle() {

        ghostIdleConsumed = true;
        ghostIdleLastLine = caretLine;
        ghostIdleLastCol = caretCol;
        requestGhostText(GhostTextContext.TriggerKind.CARET_IDLE);
    }

    protected int ghostReservedRows() {
        if (!hasGhostText()) return 0;
        int newlines = 0;
        for (int i = 0; i < ghostText.length(); i++) {
            if (ghostText.charAt(i) == '\n') newlines++;
        }
        return newlines;
    }

    public boolean clearGhostText() {
        if (!hasGhostText()) return false;
        boolean multiline = ghostReservedRows() > 0;
        ghostText = null;
        ghostAnchorLine = -1;
        ghostAnchorCol = -1;
        ghostAnchorOffset = -1;
        if (multiline) {
            invalidateGeometry();
            revalidate();
        }
        repaint();
        return true;
    }

    protected void setActiveGhostText(String text, int line, int col, int offset) {
        ghostText = text;
        ghostAnchorLine = line;
        ghostAnchorCol = col;
        ghostAnchorOffset = offset;
        if (ghostReservedRows() > 0) {
            invalidateGeometry();
            revalidate();
        }
        repaint();
    }

    public void triggerGhostText() {
        requestGhostText(GhostTextContext.TriggerKind.EXPLICIT);
    }

    protected void requestGhostText(GhostTextContext.TriggerKind kind) {
        if (!isGhostTextActive() || ghostTextProvider == null || readOnly) return;

        if (hasSelection() || hasGhostText()) return;

        GhostTextProvider provider = ghostTextProvider;
        int caretOff = caretOffset();
        int anchorLine = caretLine;
        int anchorCol = caretCol;
        GhostTextContext ctx = new GhostTextContext(
                new TextBuffer(buffer.getText()), caretOff, anchorLine, anchorCol, kind);

        int request = ghostTextVersion.incrementAndGet();
        ExecutorService executor = getAutoCompleteExecutor();
        executor.submit(() -> {
            CompletableFuture<String> task;
            try {
                task = provider.getGhostTextAsync(ctx, executor);
            } catch (Exception ex) {
                return;
            }
            if (task == null) return;
            task.whenComplete((text, error) -> SwingUtilities.invokeLater(() -> {
                if (request != ghostTextVersion.get()) return;
                if (error != null || text == null || text.isEmpty()) return;

                if (caretLine != anchorLine || caretCol != anchorCol) return;
                if (hasSelection()) return;
                setActiveGhostText(text, anchorLine, anchorCol, caretOffset());
            }));
        });
    }

    protected boolean acceptGhostText() {
        if (!hasGhostText() || readOnly) return false;
        String text = ghostText;
        int offset = ghostAnchorOffset;

        ghostText = null;
        ghostAnchorLine = -1;
        ghostAnchorCol = -1;
        ghostAnchorOffset = -1;
        ghostTextVersion.incrementAndGet();
        beginCompoundEdit();
        try {
            insertText(offset, text);
            setCaretFromOffset(offset + text.length());
            clearSelection();
        } finally {
            endCompoundEdit();
        }
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
        return true;
    }

    protected void startSnippetSession(int baseOffset, SnippetExpansion.Result result) {
        if (result == null) return;
        snippetStart = baseOffset;
        snippetEnd = baseOffset + result.text().length();
        snippetFinalCaret = baseOffset + result.finalCaret();
        snippetStops = new ArrayList<>();
        for (SnippetExpansion.Stop s : result.stops()) {
            snippetStops.add(new int[]{baseOffset + s.start(), s.length()});
        }
        if (snippetStops.isEmpty()) {
            snippetIndex = -1;
            setCaretFromOffset(snippetFinalCaret);
            clearSelection();
            clearSnippetSession();
            return;
        }
        snippetIndex = 0;
        selectSnippetStop(0);
    }

    protected void selectSnippetStop(int index) {
        if (snippetStops == null || index < 0 || index >= snippetStops.size()) return;
        int[] stop = snippetStops.get(index);
        int start = stop[0];
        int len = stop[1];
        if (len > 0) {
            int sLine = buffer.lineOfOffset(start);
            int sCol = start - buffer.offsetOfLine(sLine);
            int eLine = buffer.lineOfOffset(start + len);
            int eCol = (start + len) - buffer.offsetOfLine(eLine);
            setSelection(sLine, sCol, eLine, eCol);
        } else {
            setCaretFromOffset(start);
            clearSelection();
        }
    }

    public boolean hasActiveSnippetSession() {
        return snippetStops != null && !snippetStops.isEmpty();
    }

    protected boolean selectionMatchesCurrentSnippetStop() {
        if (!hasActiveSnippetSession()) return false;
        if (snippetIndex < 0 || snippetIndex >= snippetStops.size()) return false;
        if (!hasSelection()) return false;
        int[] stop = snippetStops.get(snippetIndex);
        int start = stop[0];
        int end = start + stop[1];
        return getSelectionStart() == start && getSelectionEnd() == end;
    }

    public boolean snippetNextStop() {
        if (!hasActiveSnippetSession()) return false;
        int next = snippetIndex + 1;
        if (next >= snippetStops.size()) {
            int finalAt = Math.max(0, Math.min(snippetFinalCaret, buffer.length()));
            setCaretFromOffset(finalAt);
            clearSelection();
            clearSnippetSession();
            return true;
        }
        snippetIndex = next;
        selectSnippetStop(next);
        return true;
    }

    public boolean snippetPreviousStop() {
        if (!hasActiveSnippetSession()) return false;
        int prev = snippetIndex - 1;
        if (prev < 0) return false;
        snippetIndex = prev;
        selectSnippetStop(prev);
        return true;
    }

    public void clearSnippetSession() {
        snippetStops = null;
        snippetFinalCaret = -1;
        snippetStart = -1;
        snippetEnd = -1;
        snippetIndex = -1;
    }

    protected void onSnippetInsert(int offset, int insertedLen) {
        if (snippetStops == null) return;
        if (offset > snippetEnd) return;
        for (int[] s : snippetStops) {
            int start = s[0];
            int end = start + s[1];
            if (offset <= start) {
                s[0] = start + insertedLen;
            } else if (offset <= end) {
                s[1] = s[1] + insertedLen;
            }
        }
        if (offset <= snippetFinalCaret) snippetFinalCaret += insertedLen;
        if (offset <= snippetEnd) snippetEnd += insertedLen;
    }

    protected void onSnippetDelete(int start, int end) {
        if (snippetStops == null) return;
        int delta = end - start;
        if (start >= snippetEnd) return;
        for (int[] s : snippetStops) {
            int sStart = s[0];
            int sEnd = sStart + s[1];
            if (end <= sStart) {
                s[0] = sStart - delta;
            } else if (start >= sEnd) {
            } else {
                int newStart = Math.min(sStart, start);
                int newEnd = Math.max(sEnd, end) - delta;
                if (newEnd < newStart) newEnd = newStart;
                s[0] = newStart;
                s[1] = newEnd - newStart;
            }
        }
        if (end <= snippetFinalCaret) snippetFinalCaret -= delta;
        else if (start < snippetFinalCaret) snippetFinalCaret = start;
        if (end <= snippetEnd) snippetEnd -= delta;
        else if (start < snippetEnd) snippetEnd = start;
    }

    protected void hideAutoCompletePopup() {
        autoCompleteVersion.incrementAndGet();
        CompletableFuture<List<AutoCompleteItem>> task = currentAutoCompleteTask;
        if (task != null && !task.isDone()) task.cancel(true);
        if (autoCompletePopup != null) autoCompletePopup.hide();
    }

    protected boolean isAutoCompleteVisible() {
        return autoCompletePopup != null && autoCompletePopup.isVisible();
    }

    protected void refreshAutoCompleteIfVisible() {
        if (!isAutoCompleteVisible() || autoCompleteProvider == null) return;
        int caretOff = caretOffset();
        int insertOff = autoCompletePopup.getTriggerOffset();
        if (caretOff < insertOff) {
            autoCompletePopup.hide();
            return;
        }
        String prefix = buffer.substring(insertOff, caretOff);
        CompletionContext ctx = createAutoCompleteContext(
                caretOff, caretLine, caretCol, prefix, insertOff,
                CompletionContext.TriggerKind.TYPING);
        Point pt = caretScreenPoint();
        autoCompletePopup.showLoading(pt.x, pt.y, prefix, insertOff);
        requestAutoComplete(autoCompleteProvider, ctx, pt);
    }

    public int getCaretOffset() {
        return caretOffset();
    }

    public boolean isSelectionActive() {
        return hasSelection();
    }

    public int getSelectionStartOffset() {
        return hasSelection() ? getSelectionStart() : -1;
    }

    public int getSelectionEndOffset() {
        return hasSelection() ? getSelectionEnd() : -1;
    }

    public String getSelectedTextOrEmpty() {
        return getSelectedText();
    }

    public void setHighlightSelectedTextOccurrences(boolean enabled) {
        if (highlightSelectedTextOccurrences == enabled) return;
        highlightSelectedTextOccurrences = enabled;
        scheduleSelectedTextOccurrencesRefresh();
    }

    public void setSelectedTextOccurrencesColor(Color color) {
        if (Objects.equals(selectedTextOccurrencesColor, color)) return;
        selectedTextOccurrencesColor = color;
        repaint();
    }

    public void setCaretPosition(int line, int col) {
        caretLine = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        caretCol = Math.max(0, Math.min(col, buffer.lineAt(caretLine).length()));
        desiredCaretCol = -1;
        clearSelection();
        scrollToCaret();
        resetCaretBlink();
        fireStateChangedIfNeeded();
        repaint();
    }

    public void setSelection(int startLine, int startCol, int endLine, int endCol) {
        selectionStartLine = Math.max(0, Math.min(startLine, buffer.lineCount() - 1));
        selectionStartCol = Math.max(0, Math.min(startCol, buffer.lineAt(selectionStartLine).length()));
        caretLine = Math.max(0, Math.min(endLine, buffer.lineCount() - 1));
        caretCol = Math.max(0, Math.min(endCol, buffer.lineAt(caretLine).length()));
        desiredCaretCol = -1;
        scrollToCaret();
        resetCaretBlink();
        fireStateChangedIfNeeded();
        repaint();
    }

    public void selectLine(int line) {
        line = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        selectionStartLine = line;
        selectionStartCol = 0;
        caretLine = line;
        caretCol = buffer.lineAt(line).length();
        desiredCaretCol = -1;
        scrollToCaret();
        resetCaretBlink();
        fireStateChangedIfNeeded();
        repaint();
    }

    protected void setupMoveLineActions() {
        getActionMap().put(ACTION_MOVE_LINE_UP, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                moveLineUp();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
            }
        });
        getActionMap().put(ACTION_MOVE_LINE_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                moveLineDown();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
            }
        });
        getActionMap().put(ACTION_DUPLICATE_LINE_UP, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                duplicateLineUp();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
            }
        });
        getActionMap().put(ACTION_DUPLICATE_LINE_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                duplicateLineDown();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
            }
        });
        getActionMap().put(ACTION_TOGGLE_FOLD, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFoldAtCaret();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
            }
        });
        getActionMap().put(ACTION_AUTO_COMPLETE, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                triggerAutoComplete(CompletionContext.TriggerKind.EXPLICIT);
            }
        });
        getActionMap().put(ACTION_SIGNATURE_HELP, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerSignatureHelp();
            }
        });
        getActionMap().put(ACTION_FORMAT, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (readOnly) return;
                format();
            }
        });
        getActionMap().put(ACTION_FIND, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireSearchRequested(false);
            }
        });
        getActionMap().put(ACTION_REPLACE, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireSearchRequested(!readOnly);
            }
        });
        getActionMap().put(ACTION_FIND_NEXT, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchFindNext();
            }
        });
        getActionMap().put(ACTION_FIND_PREV, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchFindPrev();
            }
        });
        getActionMap().put(ACTION_ADD_CARET_BELOW, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (multiCaretEnabled) addCaretBelow();
            }
        });
        getActionMap().put(ACTION_ADD_CARET_ABOVE, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (multiCaretEnabled) addCaretAbove();
            }
        });
        rebindMoveLineKeys();
    }

    protected void rebindMoveLineKeys() {
        InputMap im = getInputMap(WHEN_FOCUSED);
        KeyStroke[] keys = im.keys();
        if (keys != null) {
            for (KeyStroke k : keys) {
                Object action = im.get(k);
                if (ACTION_MOVE_LINE_UP.equals(action) || ACTION_MOVE_LINE_DOWN.equals(action)
                        || ACTION_DUPLICATE_LINE_UP.equals(action) || ACTION_DUPLICATE_LINE_DOWN.equals(action)
                        || ACTION_TOGGLE_FOLD.equals(action) || ACTION_AUTO_COMPLETE.equals(action)
                        || ACTION_SIGNATURE_HELP.equals(action)
                        || ACTION_FORMAT.equals(action) || ACTION_FIND.equals(action)
                        || ACTION_REPLACE.equals(action) || ACTION_FIND_NEXT.equals(action)
                        || ACTION_FIND_PREV.equals(action) || ACTION_ADD_CARET_BELOW.equals(action)
                        || ACTION_ADD_CARET_ABOVE.equals(action)) {
                    im.remove(k);
                }
            }
        }
        if (duplicateLineUpKeyStroke != null) im.put(duplicateLineUpKeyStroke, ACTION_DUPLICATE_LINE_UP);
        if (duplicateLineDownKeyStroke != null) im.put(duplicateLineDownKeyStroke, ACTION_DUPLICATE_LINE_DOWN);
        if (moveLineUpKeyStroke != null) im.put(moveLineUpKeyStroke, ACTION_MOVE_LINE_UP);
        if (moveLineDownKeyStroke != null) im.put(moveLineDownKeyStroke, ACTION_MOVE_LINE_DOWN);
        if (toggleFoldKeyStroke != null) im.put(toggleFoldKeyStroke, ACTION_TOGGLE_FOLD);
        if (autoCompleteKeyStroke != null) im.put(autoCompleteKeyStroke, ACTION_AUTO_COMPLETE);
        if (signatureHelpKeyStroke != null) im.put(signatureHelpKeyStroke, ACTION_SIGNATURE_HELP);
        if (formatKeyStroke != null) im.put(formatKeyStroke, ACTION_FORMAT);
        if (findKeyStroke != null) im.put(findKeyStroke, ACTION_FIND);
        if (replaceKeyStroke != null) im.put(replaceKeyStroke, ACTION_REPLACE);
        if (findNextKeyStroke != null) im.put(findNextKeyStroke, ACTION_FIND_NEXT);
        if (findPrevKeyStroke != null) im.put(findPrevKeyStroke, ACTION_FIND_PREV);
        if (addCaretBelowKeyStroke != null) im.put(addCaretBelowKeyStroke, ACTION_ADD_CARET_BELOW);
        if (addCaretAboveKeyStroke != null) im.put(addCaretAboveKeyStroke, ACTION_ADD_CARET_ABOVE);
    }

    public void setFormatKeyStroke(KeyStroke ks) {
        this.formatKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setFindKeyStroke(KeyStroke ks) {
        this.findKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setReplaceKeyStroke(KeyStroke ks) {
        this.replaceKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setFindNextKeyStroke(KeyStroke ks) {
        this.findNextKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setFindPrevKeyStroke(KeyStroke ks) {
        this.findPrevKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setAddCaretBelowKeyStroke(KeyStroke ks) {
        this.addCaretBelowKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setAddCaretAboveKeyStroke(KeyStroke ks) {
        this.addCaretAboveKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setAutoCompleteKeyStroke(KeyStroke ks) {
        this.autoCompleteKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setSignatureHelpKeyStroke(KeyStroke ks) {
        this.signatureHelpKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setMoveLineUpKeyStroke(KeyStroke ks) {
        this.moveLineUpKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setMoveLineDownKeyStroke(KeyStroke ks) {
        this.moveLineDownKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setDuplicateLineUpKeyStroke(KeyStroke ks) {
        this.duplicateLineUpKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void setDuplicateLineDownKeyStroke(KeyStroke ks) {
        this.duplicateLineDownKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void addStyledRange(StyledRange range) {
        styledRanges.add(range);
        invalidateStyledRangesIndex();
        repaint();
    }

    public void removeStyledRange(StyledRange range) {
        styledRanges.remove(range);
        invalidateStyledRangesIndex();
        repaint();
    }

    public void clearStyledRanges() {
        styledRanges.clear();
        invalidateStyledRangesIndex();
        repaint();
    }

    public void replaceStyledRanges(Collection<StyledRange> ranges) {
        styledRanges.clear();
        if (ranges != null) styledRanges.addAll(ranges);
        invalidateStyledRangesIndex();
        repaint();
    }

    protected void shiftStyledRangesForEdit(int offset, int removedLength, int insertedLength) {
        if (styledRanges.isEmpty()) return;
        int delta = insertedLength - removedLength;
        int removedEnd = offset + removedLength;
        List<StyledRange> shifted = new ArrayList<>(styledRanges.size());
        for (StyledRange range : styledRanges) {
            int start = range.getStartOffset();
            int end = range.getEndOffset();
            if (end <= offset) {
                shifted.add(range);
                continue;
            }
            int newStart = (start >= removedEnd) ? start + delta : Math.min(start, offset);
            int newEnd = (end >= removedEnd) ? end + delta : offset;
            if (newEnd <= newStart) continue;
            shifted.add(newStart == start && newEnd == end
                    ? range
                    : new StyledRange(range.getStyle(), newStart, newEnd));
        }
        styledRanges.clear();
        styledRanges.addAll(shifted);
        invalidateStyledRangesIndex();
    }

    protected StyledRange[] sortedStyledRanges;
    protected boolean styledRangesIndexDirty = true;

    protected void invalidateStyledRangesIndex() {
        styledRangesIndexDirty = true;
    }

    protected void ensureStyledRangesIndex() {
        if (!styledRangesIndexDirty && sortedStyledRanges != null) return;
        sortedStyledRanges = styledRanges.toArray(new StyledRange[0]);
        Arrays.sort(sortedStyledRanges,
                Comparator.comparingInt(StyledRange::getStartOffset));
        styledRangesIndexDirty = false;
    }

    public void setLineColor(int line, Color color) {
        setLineColor(line, color, null);
    }

    public void setLinesColor(int[] lines, Color color) {
        setLinesColor(lines, color, null);
    }

    public void setLinesColor(Collection<Integer> lines, Color color) {
        setLinesColor(lines, color, null);
    }

    public void setLinesColor(int[] lines, Color background, Color foreground) {
        if(lines == null) return;
        for(int line : lines){
            lineColors.put(line, new LineColorInfoInternal(background, foreground));
            fireLineColorAdded(line, background, foreground);
        }
        repaint();
    }

    public void setLinesColor(Collection<Integer> lines, Color color, Color foreground) {
        if(lines == null) return;
        for(int line : lines){
            lineColors.put(line, new LineColorInfoInternal(color, foreground));
            fireLineColorAdded(line, color, foreground);
        }
        repaint();
    }

    public void setLineColor(int line, Color background, Color foreground) {
        lineColors.put(line, new LineColorInfoInternal(background, foreground));
        fireLineColorAdded(line, background, foreground);
        repaint();
    }

    public void removeLineColor(int line) {
        if (lineColors.remove(line) != null) {
            fireLineColorRemoved(line);
        }
        repaint();
    }

    public void clearLineColors() {
        if (lineColors.isEmpty()) return;
        lineColors.clear();
        fireLineColorsCleared();
        repaint();
    }

    public void addLineColorChangeListener(LineColorChangeListener l) {
        if (l != null) {
            lineColorChangeListeners.add(l);
        }
    }

    public void removeLineColorChangeListener(LineColorChangeListener l) {
        lineColorChangeListeners.remove(l);
    }

    protected void fireLineColorAdded(int line, Color background, Color foreground) {
        for (LineColorChangeListener listener : List.copyOf(lineColorChangeListeners)) {
            listener.onLineColorAdded(line, background, foreground);
        }
    }

    protected void fireLineColorRemoved(int line) {
        for (LineColorChangeListener listener : List.copyOf(lineColorChangeListeners)) {
            listener.onLineColorRemoved(line);
        }
    }

    protected void fireLineColorsCleared() {
        for (LineColorChangeListener listener : List.copyOf(lineColorChangeListeners)) {
            listener.onLineColorsCleared();
        }
    }

    public void addHoverListener(HoverListener l) {
        hoverListeners.add(l);
    }

    public void removeHoverListener(HoverListener l) {
        hoverListeners.remove(l);
    }

    public void addLineChangeListener(LineChangeListener listener) {
        lineChangeListeners.add(listener);
    }

    public void removeLineChangeListener(LineChangeListener listener) {
        lineChangeListeners.remove(listener);
    }

    public void addDocumentEditListener(DocumentEditListener listener) {
        documentEditListeners.add(listener);
    }

    public void removeDocumentEditListener(DocumentEditListener listener) {
        documentEditListeners.remove(listener);
    }

    public void addWordCaretChangeListener(WordCaretChangeListener listener) {
        if (listener != null) {
            wordCaretChangeListeners.add(listener);
        }
    }

    public void removeWordCaretChangeListener(WordCaretChangeListener listener) {
        wordCaretChangeListeners.remove(listener);
    }

    public void addStateListener(CodeEditorStateListener listener) {
        if (listener == null) return;
        stateListeners.add(listener);
        listener.onStateChanged(getEditorState());
    }

    public void removeStateListener(CodeEditorStateListener listener) {
        stateListeners.remove(listener);
    }

    public CodeEditorState getEditorState() {
        int startLine = -1;
        int startCol = -1;
        int endLine = -1;
        int endCol = -1;
        int startOffset = -1;
        int endOffset = -1;
        if (hasSelection()) {
            startOffset = getSelectionStart();
            endOffset = getSelectionEnd();
            startLine = buffer.lineOfOffset(startOffset);
            startCol = startOffset - buffer.offsetOfLine(startLine);
            endLine = buffer.lineOfOffset(endOffset);
            endCol = endOffset - buffer.offsetOfLine(endLine);
        }
        List<Integer> extraOffsets = new ArrayList<>(extraCarets.size());
        for (Caret c : extraCarets) {
            int line = Math.max(0, Math.min(c.line, buffer.lineCount() - 1));
            extraOffsets.add(buffer.offsetOfLine(line) + Math.min(c.col, buffer.lineAt(line).length()));
        }
        return new CodeEditorState(
                isModified(),
                buffer.canUndo(),
                buffer.canRedo(),
                caretLine,
                caretCol,
                caretOffset(),
                hasSelection(),
                startLine,
                startCol,
                endLine,
                endCol,
                startOffset,
                endOffset,
                extraCarets.size(),
                List.copyOf(extraOffsets),
                overwriteMode,
                tabSize,
                useSpacesForTab,
                getIndentString(),
                smartIndentEnabled,
                showIndentGuides,
                hasActiveSnippetSession()
        );
    }

    protected void fireStateChangedIfNeeded() {
        CodeEditorState state = getEditorState();
        if (state.equals(lastState)) return;
        CodeEditorState previousState = lastState;
        lastState = state;
        if (previousState == null
                || previousState.selectionActive() != state.selectionActive()
                || previousState.selectionStartOffset() != state.selectionStartOffset()
                || previousState.selectionEndOffset() != state.selectionEndOffset()) {
            scheduleSelectedTextOccurrencesRefresh();
        }
        for (CodeEditorStateListener listener : stateListeners) {
            listener.onStateChanged(state);
        }
        if (lastWordCaretChangeOffset != state.caretOffset()) {
            lastWordCaretChangeOffset = state.caretOffset();
            fireWordCaretChangeEvent(state.caretLine(), state.caretCol());
        }
    }

    protected void scheduleSelectedTextOccurrencesRefresh() {
        selectedTextOccurrencesVersion.incrementAndGet();
        selectedTextOccurrenceOffsets = new int[0];
        cancelFuture(currentSelectedTextOccurrencesTask);
        if (selectedTextOccurrencesTimer != null) {
            selectedTextOccurrencesTimer.stop();
        }
        if (!highlightSelectedTextOccurrences || !hasSelection()) {
            repaint();
            return;
        }
        if (selectedTextOccurrencesTimer == null) {
            selectedTextOccurrencesTimer = new Timer(
                    SELECTED_TEXT_OCCURRENCES_DELAY,
                    e -> submitSelectedTextOccurrencesRefresh());
            selectedTextOccurrencesTimer.setRepeats(false);
        }
        selectedTextOccurrencesTimer.restart();
        repaint();
    }

    protected void submitSelectedTextOccurrencesRefresh() {
        if (!highlightSelectedTextOccurrences || !hasSelection()) return;
        int version = selectedTextOccurrencesVersion.get();
        int bufferVersion = buffer.getVersion();
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        String selectedText = buffer.substring(selectionStart, selectionEnd);
        if (selectedText.isBlank()) return;
        String textSnapshot = buffer.getText();
        currentSelectedTextOccurrencesTask = getSelectedTextOccurrencesExecutor().submit(() -> {
            int[] matches = findSelectedTextOccurrences(
                    textSnapshot,
                    selectedText,
                    selectionStart,
                    selectionEnd);
            if (Thread.currentThread().isInterrupted()) return;
            SwingUtilities.invokeLater(() -> {
                if (version != selectedTextOccurrencesVersion.get()) return;
                if (!highlightSelectedTextOccurrences || bufferVersion != buffer.getVersion()) return;
                if (!hasSelection()
                        || selectionStart != getSelectionStart()
                        || selectionEnd != getSelectionEnd()) {
                    return;
                }
                selectedTextOccurrenceOffsets = matches;
                repaint();
            });
        });
    }

    protected static int[] findSelectedTextOccurrences(
            String text,
            String selectedText,
            int selectionStart,
            int selectionEnd) {
        if (text == null || selectedText == null || selectedText.isBlank()) {
            return new int[0];
        }
        boolean identifier = isIdentifierText(selectedText);
        int[] matches = new int[16];
        int size = 0;
        int from = 0;
        while (from <= text.length() - selectedText.length()) {
            if (Thread.currentThread().isInterrupted()) {
                return new int[0];
            }
            int start = text.indexOf(selectedText, from);
            if (start < 0) break;
            int end = start + selectedText.length();
            if ((start != selectionStart || end != selectionEnd)
                    && (!identifier || hasIdentifierBoundaries(text, start, end))) {
                if (size + 2 > matches.length) {
                    matches = Arrays.copyOf(matches, matches.length << 1);
                }
                matches[size++] = start;
                matches[size++] = end;
            }
            from = end;
        }
        return size == matches.length ? matches : Arrays.copyOf(matches, size);
    }

    protected static boolean isIdentifierText(String text) {
        if (text.isEmpty() || !Character.isJavaIdentifierStart(text.charAt(0))) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            if (!Character.isJavaIdentifierPart(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected static boolean hasIdentifierBoundaries(String text, int start, int end) {
        return (start == 0 || !Character.isJavaIdentifierPart(text.charAt(start - 1)))
                && (end == text.length() || !Character.isJavaIdentifierPart(text.charAt(end)));
    }

    public void setTabSize(int tabSize) {
        this.tabSize = Math.max(1, tabSize);
        cachedIndentUnit = -1;
        fireStateChangedIfNeeded();
        revalidate();
        repaint();
    }

    public void setUseSpacesForTab(boolean useSpacesForTab) {
        this.useSpacesForTab = useSpacesForTab;
        fireStateChangedIfNeeded();
    }

    public void setSmartIndentEnabled(boolean smartIndentEnabled) {
        this.smartIndentEnabled = smartIndentEnabled;
        fireStateChangedIfNeeded();
    }

    public void setShowIndentGuides(boolean showIndentGuides) {
        this.showIndentGuides = showIndentGuides;
        fireStateChangedIfNeeded();
        repaint();
    }

    public void setReadOnly(boolean readOnly) {
        if (this.readOnly == readOnly) return;
        this.readOnly = readOnly;
        if (readOnly) {
            hideAutoCompletePopup();
            clearSnippetSession();
            overwriteMode = false;
        }
        fireStateChangedIfNeeded();
        repaint();
    }

    protected void insertText(int offset, String text) {
        if (readOnly || text == null || text.isEmpty()) return;
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        List<int[]> oldFoldedAnchors = (foldingEnabled && !suppressFoldRestore) ? captureFoldedAnchorOffsets() : null;
        int linesBefore = buffer.lineCount();
        int lineAtInsert = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        buffer.insert(offset, text);
        onSnippetInsert(offset, text.length());
        final String textFinal = text;
        documentEditListeners.forEach(l -> l.onInsert(offset, textFinal));
        documentEditListeners.forEach(DocumentEditListener::onTextChanged);
        fireStateChangedIfNeeded();
        int addedLines = buffer.lineCount() - linesBefore;
        if (addedLines > 0) {
            fireLinesInserted(lineAtInsert + 1, addedLines);
        }
        if (foldingEnabled) {
            recomputeFoldRegions(suppressFoldRestore);
            if (!suppressFoldRestore) {
                restoreFoldedByOffsets(oldFoldedAnchors, offset, 0, text.length());
            }
        }
    }

    protected void deleteText(int start, int end) {
        if (readOnly || start >= end) return;
        List<int[]> oldFoldedAnchors = (foldingEnabled && !suppressFoldRestore) ? captureFoldedAnchorOffsets() : null;
        String removed = buffer.substring(start, end);
        int linesBefore = buffer.lineCount();
        int lineAtDelete = buffer.lineOfOffset(Math.min(start, buffer.length()));
        buffer.delete(start, end);
        onSnippetDelete(start, end);
        documentEditListeners.forEach(l -> l.onDelete(start, removed));
        documentEditListeners.forEach(DocumentEditListener::onTextChanged);
        fireStateChangedIfNeeded();
        int removedLines = linesBefore - buffer.lineCount();
        if (removedLines > 0) {
            fireLinesRemoved(lineAtDelete + 1, removedLines);
        }
        if (foldingEnabled) {
            recomputeFoldRegions(suppressFoldRestore);
            if (!suppressFoldRestore) {
                restoreFoldedByOffsets(oldFoldedAnchors, start, end - start, 0);
            }
        }
    }

    protected List<int[]> captureFoldedAnchorOffsets() {
        List<int[]> list = new ArrayList<>();
        for (FoldRegion r : foldRegions) {
            if (r.folded()) {
                list.add(new int[]{buffer.offsetOfLine(r.startLine()), r.endLine() - r.startLine()});
            }
        }
        return list;
    }

    protected void restoreFoldedByOffsets(List<int[]> oldAnchors, int editStart, int removedLen, int insertedLen) {
        if (oldAnchors == null || oldAnchors.isEmpty()) return;
        int delta = insertedLen - removedLen;
        for (int[] anchor : oldAnchors) {
            int oldOff = anchor[0];
            int span = anchor[1];
            int newOff;
            if (oldOff < editStart) {
                newOff = oldOff;
            } else if (oldOff >= editStart + removedLen) {
                newOff = oldOff + delta;
            } else {

                continue;
            }
            newOff = Math.max(0, Math.min(newOff, buffer.length()));
            int newLine = buffer.lineOfOffset(newOff);

            for (int i = 0; i < foldRegions.size(); i++) {
                FoldRegion r = foldRegions.get(i);
                if (r.startLine() == newLine && r.endLine() - r.startLine() == span) {
                    if (!r.folded()) foldRegions.set(i, r.withFolded(true));
                    break;
                }
            }
        }
    }

    protected void fireLinesInserted(int atLine, int count) {
        shiftBookmarksOnInsert(atLine, count);
        lineChangeListeners.forEach(l -> l.onLinesInserted(atLine, count));
    }

    protected void fireLinesRemoved(int atLine, int count) {
        shiftBookmarksOnRemove(atLine, count);
        lineChangeListeners.forEach(l -> l.onLinesRemoved(atLine, count));
    }

    protected void shiftBookmarksOnInsert(int atLine, int count) {
        if (bookmarks.isEmpty() || count <= 0) return;
        SortedSet<Integer> updated = new TreeSet<>();
        List<int[]> moved = new ArrayList<>();
        boolean changed = false;
        for (Integer line : bookmarks) {
            if (line >= atLine) {
                int newLine = line + count;
                updated.add(newLine);
                moved.add(new int[]{line, newLine});
                changed = true;
            } else {
                updated.add(line);
            }
        }
        if (!changed) return;
        bookmarks.clear();
        bookmarks.addAll(updated);
        for (int[] move : moved) {
            fireBookmarkChanged(move[0], false);
            fireBookmarkChanged(move[1], true);
        }
        fireBookmarksChanged();
    }

    protected void shiftBookmarksOnRemove(int atLine, int count) {
        if (bookmarks.isEmpty() || count <= 0) return;
        SortedSet<Integer> updated = new TreeSet<>();
        List<Integer> removed = new ArrayList<>();
        List<int[]> moved = new ArrayList<>();
        boolean changed = false;
        for (Integer line : bookmarks) {
            if (line >= atLine && line < atLine + count) {
                removed.add(line);
                changed = true;
            } else if (line >= atLine + count) {
                int newLine = line - count;
                updated.add(newLine);
                moved.add(new int[]{line, newLine});
                changed = true;
            } else {
                updated.add(line);
            }
        }
        if (!changed) return;
        bookmarks.clear();
        bookmarks.addAll(updated);
        removed.forEach(line -> fireBookmarkChanged(line, false));
        for (int[] move : moved) {
            fireBookmarkChanged(move[0], false);
            fireBookmarkChanged(move[1], true);
        }
        fireBookmarksChanged();
    }

    public LineColorInfo getLineColor(int line) {
        LineColorInfoInternal colorInfoInternal = lineColors.get(line);
        if (colorInfoInternal == null) return null;
        return new LineColorInfo() {
            @Override
            public Color getBackgroundColor() {
                return colorInfoInternal.background;
            }

            @Override
            public Color getForegroundColor() {
                return colorInfoInternal.foreground;
            }
        };
    }

    protected void setupCaretBlink() {
        caretTimer = new Timer(500, e -> {
            caretVisible = !caretVisible;
            repaint();
        });
        caretTimer.start();
    }

    protected void resetCaretBlink() {
        caretVisible = true;
        caretTimer.restart();
        scheduleGhostIdleTimer();
        repaint();
    }

    protected void scrollToCaret() {
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight();

        String lineText = buffer.lineAt(caretLine);

        int cx = baseVisualXForColumn(
                caretLine,
                lineText,
                caretCol,
                fm
        );

        int cy = yOfBufferLine(caretLine);
        int extra = hasCodeLens(caretLine) ? lineHeight : 0;
        Rectangle caretBounds = new Rectangle(cx, cy - extra, 2, lineHeight + extra);

        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport == null) {
            scrollRectToVisible(caretBounds);
            return;
        }

        Point currentPosition = viewport.getViewPosition();
        Point targetPosition = calculateCaretScrollPosition(
                currentPosition,
                viewport.getExtentSize(),
                caretBounds
        );
        if (!targetPosition.equals(currentPosition)) {
            viewport.setViewPosition(targetPosition);
        }
    }

    /**
     * Keeps the viewport still while the caret remains inside its navigation margins.
     * This prevents ordinary arrow-key movement from also behaving like a scroll command.
     */
    protected Point calculateCaretScrollPosition(Point viewPosition,
                                                 Dimension extentSize,
                                                 Rectangle caretBounds) {
        int extentWidth = Math.max(0, extentSize.width);
        int extentHeight = Math.max(0, extentSize.height);
        if (extentWidth == 0 || extentHeight == 0) {
            return new Point(viewPosition);
        }

        int horizontalMarginSpace = Math.max(0, extentWidth - caretBounds.width);
        int leftMargin = Math.min(Math.max(0, caretScrollLeftMargin), horizontalMarginSpace / 2);
        int rightMargin = Math.min(Math.max(0, caretScrollRightMargin), horizontalMarginSpace - leftMargin);

        int x = viewPosition.x;
        int leftBoundary = viewPosition.x + leftMargin;
        int rightBoundary = viewPosition.x + extentWidth - rightMargin;
        if (caretBounds.x < leftBoundary) {
            x = caretBounds.x - leftMargin;
        } else if (caretBounds.x + caretBounds.width > rightBoundary) {
            x = caretBounds.x + caretBounds.width + rightMargin - extentWidth;
        }

        int y = viewPosition.y;
        if (caretBounds.y < viewPosition.y) {
            y = caretBounds.y;
        } else if (caretBounds.y + caretBounds.height > viewPosition.y + extentHeight) {
            y = caretBounds.y + caretBounds.height - extentHeight;
        }

        int maxX = Math.max(0, getWidth() - extentWidth);
        int maxY = Math.max(0, getHeight() - extentHeight);
        return new Point(
                Math.max(0, Math.min(x, maxX)),
                Math.max(0, Math.min(y, maxY))
        );
    }

    protected int caretOffset() {
        return buffer.offsetOfLine(caretLine) + caretCol;
    }

    protected void clampCaret() {
        caretLine = Math.max(0, Math.min(caretLine, buffer.lineCount() - 1));
        caretCol = Math.max(0, Math.min(caretCol, buffer.lineAt(caretLine).length()));
    }

    protected void setCaretFromOffset(int offset) {
        offset = Math.max(0, Math.min(offset, buffer.length()));
        caretLine = buffer.lineOfOffset(offset);
        caretCol = offset - buffer.offsetOfLine(caretLine);
        desiredCaretCol = -1;
    }

    protected EditorState captureEditorState() {
        int anchorOffset = selectionStartLine >= 0
                ? buffer.offsetOfLine(selectionStartLine) + selectionStartCol
                : -1;
        List<ExtraCaretState> extraStates = new ArrayList<>(extraCarets.size());
        for (Caret c : extraCarets) {
            int line = Math.max(0, Math.min(c.line, buffer.lineCount() - 1));
            int caretOffset = buffer.offsetOfLine(line) + Math.min(c.col, buffer.lineAt(line).length());
            int extraAnchorOffset = -1;
            if (c.anchorLine >= 0) {
                int anchorLine = Math.max(0, Math.min(c.anchorLine, buffer.lineCount() - 1));
                extraAnchorOffset = buffer.offsetOfLine(anchorLine)
                        + Math.min(c.anchorCol, buffer.lineAt(anchorLine).length());
            }
            extraStates.add(new ExtraCaretState(caretOffset, extraAnchorOffset));
        }
        return new EditorState(caretOffset(), anchorOffset, extraStates);
    }

    protected void restoreEditorState(Object state, int fallbackOffset) {
        if (state instanceof EditorState editorState) {
            setCaretFromOffset(editorState.caretOffset());
            if (editorState.selectionAnchorOffset() >= 0) {
                int anchor = Math.max(0, Math.min(editorState.selectionAnchorOffset(), buffer.length()));
                int line = buffer.lineOfOffset(anchor);
                selectionStartLine = line;
                selectionStartCol = anchor - buffer.offsetOfLine(line);
            } else {
                clearSelection();
            }
            extraCarets.clear();
            for (ExtraCaretState extra : editorState.extraCarets()) {
                if (extra == null) continue;
                int off = Math.max(0, Math.min(extra.caretOffset(), buffer.length()));
                int line = buffer.lineOfOffset(off);
                Caret c = new Caret(line, off - buffer.offsetOfLine(line));
                if (extra.selectionAnchorOffset() >= 0) {
                    int anchor = Math.max(0, Math.min(extra.selectionAnchorOffset(), buffer.length()));
                    c.anchorLine = buffer.lineOfOffset(anchor);
                    c.anchorCol = anchor - buffer.offsetOfLine(c.anchorLine);
                }
                extraCarets.add(c);
            }
        } else if (fallbackOffset >= 0) {
            setCaretFromOffset(fallbackOffset);
            clearSelection();
            extraCarets.clear();
        }
        clampCaret();
    }

    protected void updateLastEditState() {
        buffer.updateNextUndoAfterState(captureEditorState());
    }

    public boolean hasSelection() {
        return selectionStartLine >= 0 && (selectionStartLine != caretLine || selectionStartCol != caretCol);
    }

    protected int selectionStartOffset() {
        return buffer.offsetOfLine(selectionStartLine) + selectionStartCol;
    }

    protected int getSelectionStart() {
        return Math.min(selectionStartOffset(), caretOffset());
    }

    protected int getSelectionEnd() {
        return Math.max(selectionStartOffset(), caretOffset());
    }

    protected String getSelectedText() {
        if (!hasSelection()) return "";
        return buffer.substring(getSelectionStart(), getSelectionEnd());
    }

    protected List<CaretDeleteOp> selectedCaretOps() {
        List<CaretDeleteOp> ops = new ArrayList<>();
        if (hasSelection()) {
            ops.add(new CaretDeleteOp(caretOffset(), getSelectionStart(), getSelectionEnd(), true));
        }
        for (Caret c : extraCarets) {
            if (c.hasSelection()) {
                ops.add(new CaretDeleteOp(extraCaretOffset(c), extraSelectionStart(c), extraSelectionEnd(c), false));
            }
        }
        ops = filterOverlappingCaretOps(ops);
        ops.removeIf(op -> op.end() <= op.start());
        ops.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return ops;
    }

    protected String getClipboardSelectedText() {
        List<CaretDeleteOp> ops = selectedCaretOps();
        if (ops.isEmpty()) return "";
        List<String> parts = new ArrayList<>(ops.size());
        for (CaretDeleteOp op : ops) {
            parts.add(buffer.substring(op.start(), op.end()));
        }
        return String.join("\n", parts);
    }

    protected boolean hasExtraSelections() {
        for (Caret c : extraCarets) {
            if (c.hasSelection()) return true;
        }
        return false;
    }

    protected int extraCaretOffset(Caret c) {
        int line = Math.max(0, Math.min(c.line, buffer.lineCount() - 1));
        return buffer.offsetOfLine(line) + Math.min(c.col, buffer.lineAt(line).length());
    }

    protected int extraSelectionAnchorOffset(Caret c) {
        if (c.anchorLine < 0) return extraCaretOffset(c);
        int line = Math.max(0, Math.min(c.anchorLine, buffer.lineCount() - 1));
        return buffer.offsetOfLine(line) + Math.min(c.anchorCol, buffer.lineAt(line).length());
    }

    protected int extraSelectionStart(Caret c) {
        return Math.min(extraSelectionAnchorOffset(c), extraCaretOffset(c));
    }

    protected int extraSelectionEnd(Caret c) {
        return Math.max(extraSelectionAnchorOffset(c), extraCaretOffset(c));
    }

    protected void deleteSelection() {
        if (!hasSelection()) return;
        int start = getSelectionStart();
        int end = getSelectionEnd();
        deleteText(start, end);
        setCaretFromOffset(start);
        clearSelection();
    }

    protected void clearSelection() {
        selectionStartLine = -1;
        selectionStartCol = -1;
        fireStateChangedIfNeeded();
    }

    protected void clearExtraSelections() {
        boolean changed = false;
        for (Caret c : extraCarets) {
            if (c.anchorLine >= 0) {
                c.clearSelection();
                changed = true;
            }
        }
        if (changed) fireStateChangedIfNeeded();
    }

    protected void startSelectionIfNeeded() {
        if (selectionStartLine < 0) {
            selectionStartLine = caretLine;
            selectionStartCol = caretCol;
        }
    }

    protected void startExtraSelectionsIfNeeded() {
        for (Caret c : extraCarets) {
            c.startSelectionIfNeeded();
        }
    }

    protected void selectAll() {
        selectionStartLine = 0;
        selectionStartCol = 0;
        caretLine = buffer.lineCount() - 1;
        caretCol = buffer.lineAt(caretLine).length();
        fireStateChangedIfNeeded();
    }

    protected void copyToClipboard() {
        String selectedText = getClipboardSelectedText();
        if (selectedText.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(selectedText), null);
    }

    protected void pasteFromClipboard() {
        if (readOnly) return;
        try {
            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            if (text == null) return;
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            int offset;
            beginCompoundEdit();
            try {
                if (hasSelection() || hasExtraSelections()) {
                    replaceSelectionsAtCarets(text, text.length());
                    offset = caretOffset() - text.length();
                } else {
                    insertAtAllCarets(text, false, text.length());
                    offset = caretOffset() - text.length();
                }
            } finally {
                endCompoundEdit();
            }
            updateLastEditState();
        } catch (Exception ignored) {
        }
    }

    protected void performUndo() {
        if (readOnly) return;
        int linesBefore = buffer.lineCount();
        TextBuffer.EditResult result = buffer.undoEdit();
        if (result.caretOffset() >= 0) {
            restoreEditorState(result.state(), result.caretOffset());
            documentEditListeners.forEach(DocumentEditListener::onTextChanged);
            fireStateChangedIfNeeded();
            int delta = buffer.lineCount() - linesBefore;
            if (delta > 0) fireLinesInserted(0, delta);
            else if (delta < 0) fireLinesRemoved(0, -delta);
            if (foldingEnabled) {
                recomputeFoldRegions();
                unfoldToRevealCaret();
            }
        }
    }

    protected void performRedo() {
        if (readOnly) return;
        int linesBefore = buffer.lineCount();
        TextBuffer.EditResult result = buffer.redoEdit();
        if (result.caretOffset() >= 0) {
            restoreEditorState(result.state(), result.caretOffset());
            documentEditListeners.forEach(DocumentEditListener::onTextChanged);
            fireStateChangedIfNeeded();
            int delta = buffer.lineCount() - linesBefore;
            if (delta > 0) fireLinesInserted(0, delta);
            else if (delta < 0) fireLinesRemoved(0, -delta);
            if (foldingEnabled) {
                recomputeFoldRegions();
                unfoldToRevealCaret();
            }
        }
    }

    public void setText(String text) {
        String newText = text == null ? "" : text.replace("\r\n", "\n").replace("\r", "\n");
        String oldText = buffer.getText();
        int oldLineCount = buffer.lineCount();
        buffer.setText(newText);
        setCaretFromOffset(0);
        clearSelection();
        clearExtraCarets();
        clearSnippetSession();
        if (foldingEnabled) recomputeFoldRegions();
        if (!oldText.isEmpty()) documentEditListeners.forEach(l -> l.onDelete(0, oldText));
        if (!newText.isEmpty()) documentEditListeners.forEach(l -> l.onInsert(0, newText));
        if (!oldText.equals(newText)) {
            documentEditListeners.forEach(DocumentEditListener::onTextChanged);
            int delta = buffer.lineCount() - oldLineCount;
            if (delta > 0) fireLinesInserted(0, delta);
            else if (delta < 0) fireLinesRemoved(0, -delta);
        }
        markClean();
        fireStateChangedIfNeeded();
        revalidate();
        repaint();
    }

    protected int[] positionFromPoint(int mx, int my) {
        FontMetrics fm = getFontMetrics(getFont());
        int line = bufferLineAtY(my);
        String lineText = buffer.lineAt(line);
        InlayHint transparentHint = mouseTransparentInlayHintAt(line, mx, fm);
        if (transparentHint != null) {
            return new int[]{line, inlayHintColumn(transparentHint, lineText)};
        }

        int bestCol = 0;
        int bestDist = Math.abs(mx - visualXForColumn(line, lineText, 0, fm));

        for (int i = 1; i <= lineText.length(); i++) {
            int dist = Math.abs(mx - visualXForColumn(line, lineText, i, fm));
            if (dist < bestDist) {
                bestDist = dist;
                bestCol = i;
            }
        }
        return new int[]{line, bestCol};
    }

    protected String expandTabs(String text, int startColumn) {
        if (text == null || text.indexOf('\t') < 0) {
            return text;
        }

        StringBuilder builder = new StringBuilder(text.length() + tabSize);
        int column = startColumn;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\t') {
                int spaces = tabSize - (column % tabSize);
                for (int s = 0; s < spaces; s++) {
                    builder.append(' ');
                }
                column += spaces;
            } else {
                builder.append(c);
                column++;
            }
        }

        return builder.toString();
    }

    protected int textWidth(FontMetrics fm, String text, int startColumn) {
        return fm.stringWidth(expandTabs(text, startColumn));
    }

    protected String getIndentString() {
        return useSpacesForTab ? " ".repeat(tabSize) : "\t";
    }

    protected int getIndentAdvance() {
        return useSpacesForTab ? tabSize : 1;
    }

    protected String getLeadingWhitespace(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
        return line.substring(0, i);
    }

    protected int getVisibleLines() {
        FontMetrics fm = getFontMetrics(getFont());
        Container parent = getParent();
        int height = (parent instanceof JViewport) ? parent.getHeight() : getHeight();
        return Math.max(1, height / fm.getHeight());
    }

    protected void indentSelection(boolean outdent) {
        int startOff = getSelectionStart();
        int endOff = getSelectionEnd();
        int startLine = buffer.lineOfOffset(startOff);
        int endLine = buffer.lineOfOffset(endOff);

        beginCompoundEdit();
        try {
            for (int i = startLine; i <= endLine; i++) {
                if (outdent) {
                    outdentLine(i);
                } else {
                    int off = buffer.offsetOfLine(i);
                    insertText(off, getIndentString());
                }
            }
        } finally {
            endCompoundEdit();
        }

        selectionStartLine = startLine;
        selectionStartCol = 0;
        caretLine = endLine;
        caretCol = buffer.lineAt(endLine).length();
    }

    protected void outdentLine(int line) {
        String text = buffer.lineAt(line);
        int off = buffer.offsetOfLine(line);
        if (useSpacesForTab) {
            int remove = 0;
            while (remove < tabSize && remove < text.length() && text.charAt(remove) == ' ') remove++;
            if (remove > 0) deleteText(off, off + remove);
        } else {
            if (!text.isEmpty() && text.charAt(0) == '\t') deleteText(off, off + 1);
        }
    }

    protected void outdentCurrentLine() {
        outdentLine(caretLine);
        clampCaret();
    }

    protected Font deriveFont(Font base, TextStyle style) {
        int s = Font.PLAIN;
        if (style.isBold()) s |= Font.BOLD;
        if (style.isItalic()) s |= Font.ITALIC;
        return base.deriveFont(s);
    }

    public TextStyle getStyleAt(int line, int col) {
        int safeLine = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        String lineText = buffer.lineAt(safeLine);
        int safeCol = Math.max(0, Math.min(col, lineText.length()));
        return getStyleAt(buffer.offsetOfLine(safeLine) + safeCol);
    }

    public TextStyle getStyleAtOffset(int offset) {
        return getStyleAt(offset);
    }

    public TextStyle getStyleAt(int offset) {
        offset = clampOffset(offset);
        if (styledRanges.isEmpty()) return defaultStyle;
        ensureStyledRangesIndex();
        StyledRange[] arr = sortedStyledRanges;
        int n = arr.length;

        int lo = 0, hi = n - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (arr[mid].getStartOffset() <= offset) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        for (int i = found; i >= 0; i--) {
            StyledRange r = arr[i];
            if (offset < r.getEndOffset()) {
                if (offset >= r.getStartOffset()) {
                    return r.getStyle();
                }
            }

            if (r.getEndOffset() <= offset && r.getStartOffset() <= offset
                    && (i == 0 || arr[i - 1].getEndOffset() <= offset)) {
                break;
            }
        }
        return defaultStyle;
    }

    protected static int charClass(char c) {
        if (c == '_' || Character.isLetterOrDigit(c)) return 1;
        if (Character.isWhitespace(c)) return 0;
        return 2;
    }

    protected void moveWordLeft() {
        clampCaret();
        if (caretLine == 0 && caretCol == 0) return;

        if (caretCol == 0) {
            int nl = caretLine - 1;
            while (nl > 0 && isLineHidden(nl)) nl--;
            caretLine = nl;
            caretCol = buffer.lineAt(caretLine).length();
            return;
        }

        String line = buffer.lineAt(caretLine);
        int col = caretCol - 1;

        while (col > 0 && charClass(line.charAt(col)) == 0) col--;

        if (col >= 0 && col < line.length()) {
            int cls = charClass(line.charAt(col));
            while (col > 0 && charClass(line.charAt(col - 1)) == cls) col--;
        }

        caretCol = col;
    }

    protected boolean matchesKeyStroke(KeyEvent e, KeyStroke ks) {
        if (ks == null) return false;
        if (ks.getKeyCode() != e.getKeyCode()) return false;
        int mask = InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK
                | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK
                | InputEvent.ALT_GRAPH_DOWN_MASK;
        return (e.getModifiersEx() & mask) == (ks.getModifiers() & mask);
    }

    protected int[] getBlockRange(int line) {
        if (foldingEnabled) {
            for (FoldRegion r : foldRegions) {
                if (r.startLine() == line && r.folded()) {
                    return new int[]{r.startLine(), r.endLine()};
                }
            }
        }
        return new int[]{line, line};
    }

    protected int[] getMoveBlockRange(int line) {
        int[] range = getBlockRange(line);
        if (foldingEnabled) {
            int end = range[1];
            while (end + 1 < buffer.lineCount() && isLineHidden(end + 1)) end++;
            range[1] = end;
        }
        return range;
    }

    protected List<int[]> foldedRegionsWithin(int startLine, int endLine) {
        List<int[]> list = new ArrayList<>();
        if (!foldingEnabled) return list;
        for (FoldRegion r : foldRegions) {
            if (r.folded() && r.startLine() >= startLine && r.endLine() <= endLine) {
                list.add(new int[]{r.startLine() - startLine, r.endLine() - startLine});
            }
        }
        return list;
    }

    protected void refoldRelative(List<int[]> spans, int newStart) {
        for (int[] s : spans) {
            refoldAt(newStart + s[0], newStart + s[1]);
        }
    }

    protected int offsetOfLineEnd(int line) {
        return buffer.offsetOfLine(line) + buffer.lineAt(line).length();
    }

    protected void refoldAt(int startLine, int endLine) {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.startLine() == startLine && r.endLine() == endLine) {
                if (!r.folded()) foldRegions.set(i, r.withFolded(true));
                return;
            }
        }
    }

    public void moveLineUp() {
        if (readOnly) return;
        int[] cur = getMoveBlockRange(caretLine);
        int curStart = cur[0], curEnd = cur[1];
        if (curStart <= 0) return;

        int prevVisible = curStart - 1;
        while (prevVisible >= 0 && isLineHidden(prevVisible)) prevVisible--;
        if (prevVisible < 0) return;

        int[] prev = getMoveBlockRange(prevVisible);
        int prevStart = prev[0], prevEnd = prev[1];

        List<int[]> curFolds = foldedRegionsWithin(curStart, curEnd);
        List<int[]> prevFolds = foldedRegionsWithin(prevStart, prevEnd);

        int savedCol = caretCol;
        int curTextStart = buffer.offsetOfLine(curStart);
        int curTextEnd = offsetOfLineEnd(curEnd);
        String curText = buffer.substring(curTextStart, curTextEnd);

        int prevTextStart = buffer.offsetOfLine(prevStart);
        int prevTextEnd = offsetOfLineEnd(prevEnd);
        String prevText = buffer.substring(prevTextStart, prevTextEnd);

        beginCompoundEdit();
        try {
            deleteText(prevTextStart, curTextEnd);
            insertText(prevTextStart, curText + "\n" + prevText);
        } finally {
            endCompoundEdit();
        }

        int curDelta = prevEnd - prevStart + 1;
        int newCurStart = curStart - curDelta;
        int newPrevStart = newCurStart + (curEnd - curStart + 1);

        if (foldingEnabled) {
            refoldRelative(curFolds, newCurStart);
            refoldRelative(prevFolds, newPrevStart);
        }

        caretLine = newCurStart + (caretLine - curStart);
        caretCol = Math.min(savedCol, buffer.lineAt(caretLine).length());
        unfoldToRevealCaret();
        clearSelection();
        updateLastEditState();
    }

    public void moveLineDown() {
        if (readOnly) return;
        int[] cur = getMoveBlockRange(caretLine);
        int curStart = cur[0], curEnd = cur[1];
        if (curEnd >= buffer.lineCount() - 1) return;

        int nextVisible = curEnd + 1;
        while (nextVisible < buffer.lineCount() && isLineHidden(nextVisible)) nextVisible++;
        if (nextVisible >= buffer.lineCount()) return;

        int[] next = getMoveBlockRange(nextVisible);
        int nextStart = next[0], nextEnd = next[1];

        List<int[]> curFolds = foldedRegionsWithin(curStart, curEnd);
        List<int[]> nextFolds = foldedRegionsWithin(nextStart, nextEnd);

        int savedCol = caretCol;
        int curTextStart = buffer.offsetOfLine(curStart);
        int curTextEnd = offsetOfLineEnd(curEnd);
        String curText = buffer.substring(curTextStart, curTextEnd);

        int nextTextStart = buffer.offsetOfLine(nextStart);
        int nextTextEnd = offsetOfLineEnd(nextEnd);
        String nextText = buffer.substring(nextTextStart, nextTextEnd);

        beginCompoundEdit();
        try {
            deleteText(curTextStart, nextTextEnd);
            insertText(curTextStart, nextText + "\n" + curText);
        } finally {
            endCompoundEdit();
        }

        int nextDelta = nextEnd - nextStart + 1;
        int newNextStart = curStart;
        int newCurStart = curStart + nextDelta;

        if (foldingEnabled) {
            refoldRelative(nextFolds, newNextStart);
            refoldRelative(curFolds, newCurStart);
        }

        caretLine = newCurStart + (caretLine - curStart);
        caretCol = Math.min(savedCol, buffer.lineAt(caretLine).length());
        unfoldToRevealCaret();
        clearSelection();
        updateLastEditState();
    }

    public void duplicateLineDown() {
        if (readOnly) return;
        if (hasSelection()) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            String selected = buffer.substring(start, end);
            int startLine = buffer.lineOfOffset(start);
            int startCol = start - buffer.offsetOfLine(startLine);
            String indent = startCol > 0 ? getLeadingWhitespace(buffer.lineAt(startLine)) : "";
            int endLine = buffer.lineOfOffset(end);
            int insertAt = offsetOfLineEnd(endLine);
            insertText(insertAt, "\n" + indent + selected);
            int newStart = insertAt + 1 + indent.length();
            int newEnd = newStart + selected.length();
            selectionStartLine = buffer.lineOfOffset(newStart);
            selectionStartCol = newStart - buffer.offsetOfLine(selectionStartLine);
            setCaretFromOffset(newEnd);
            return;
        }

        int[] cur = getMoveBlockRange(caretLine);
        int curStart = cur[0], curEnd = cur[1];
        List<int[]> curFolds = foldedRegionsWithin(curStart, curEnd);

        int savedCol = caretCol;
        int curTextStart = buffer.offsetOfLine(curStart);
        int curTextEnd = offsetOfLineEnd(curEnd);
        String curText = buffer.substring(curTextStart, curTextEnd);

        insertText(curTextEnd, "\n" + curText);

        int blockSize = curEnd - curStart + 1;
        int copyStart = curEnd + 1;

        if (foldingEnabled) {
            refoldRelative(curFolds, curStart);
            refoldRelative(curFolds, copyStart);
        }

        caretLine = copyStart + (caretLine - curStart);
        caretCol = Math.min(savedCol, buffer.lineAt(caretLine).length());
        unfoldToRevealCaret();
        clearSelection();
    }

    public void duplicateLineUp() {
        if (readOnly) return;
        if (hasSelection()) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            String selected = buffer.substring(start, end);
            int startLine = buffer.lineOfOffset(start);
            int startCol = start - buffer.offsetOfLine(startLine);
            String indent = startCol > 0 ? getLeadingWhitespace(buffer.lineAt(startLine)) : "";
            int insertAt = buffer.offsetOfLine(startLine);
            insertText(insertAt, indent + selected + "\n");
            int newStart = insertAt + indent.length();
            int newEnd = newStart + selected.length();
            selectionStartLine = buffer.lineOfOffset(newStart);
            selectionStartCol = newStart - buffer.offsetOfLine(selectionStartLine);
            setCaretFromOffset(newEnd);
            return;
        }

        int[] cur = getMoveBlockRange(caretLine);
        int curStart = cur[0], curEnd = cur[1];
        List<int[]> curFolds = foldedRegionsWithin(curStart, curEnd);

        int savedCol = caretCol;
        int curTextStart = buffer.offsetOfLine(curStart);
        int curTextEnd = offsetOfLineEnd(curEnd);
        String curText = buffer.substring(curTextStart, curTextEnd);

        insertText(curTextStart, curText + "\n");

        int blockSize = curEnd - curStart + 1;

        if (foldingEnabled) {
            refoldRelative(curFolds, curStart);
            refoldRelative(curFolds, curStart + blockSize);
        }

        caretCol = Math.min(savedCol, buffer.lineAt(caretLine).length());
        unfoldToRevealCaret();
        clearSelection();
    }

    protected void moveWordRight() {
        clampCaret();
        String line = buffer.lineAt(caretLine);

        if (caretCol >= line.length()) {
            if (caretLine < buffer.lineCount() - 1) {
                int nl = caretLine + 1;
                while (nl < buffer.lineCount() && isLineHidden(nl)) nl++;
                if (nl < buffer.lineCount()) {
                    caretLine = nl;
                    caretCol = 0;
                }
            }
            return;
        }

        int col = caretCol;
        int cls = charClass(line.charAt(col));

        if (cls == 0) {
            while (col < line.length() && charClass(line.charAt(col)) == 0) col++;
        } else {
            while (col < line.length() && charClass(line.charAt(col)) == cls) col++;
        }

        caretCol = col;
    }

    protected void moveCaretLeft(Caret c) {
        c.desiredCol = -1;
        if (c.col > 0) {
            c.col--;
        } else if (c.line > 0) {
            int nl = c.line - 1;
            while (nl > 0 && isLineHidden(nl)) nl--;
            c.line = nl;
            c.col = buffer.lineAt(c.line).length();
        }
    }

    protected void moveCaretRight(Caret c) {
        c.desiredCol = -1;
        if (c.col < buffer.lineAt(c.line).length()) {
            c.col++;
        } else if (c.line < buffer.lineCount() - 1) {
            int nl = c.line + 1;
            while (nl < buffer.lineCount() && isLineHidden(nl)) nl++;
            if (nl < buffer.lineCount()) {
                c.line = nl;
                c.col = 0;
            }
        }
    }

    protected void moveCaretUp(Caret c) {
        if (c.desiredCol < 0) c.desiredCol = c.col;
        int nl = c.line - 1;
        while (nl >= 0 && isLineHidden(nl)) nl--;
        if (nl >= 0) {
            c.line = nl;
            c.col = Math.min(c.desiredCol, buffer.lineAt(c.line).length());
        }
    }

    protected void moveCaretDown(Caret c) {
        if (c.desiredCol < 0) c.desiredCol = c.col;
        int nl = c.line + 1;
        while (nl < buffer.lineCount() && isLineHidden(nl)) nl++;
        if (nl < buffer.lineCount()) {
            c.line = nl;
            c.col = Math.min(c.desiredCol, buffer.lineAt(c.line).length());
        }
    }

    protected void moveCaretWordLeft(Caret c) {
        c.desiredCol = -1;
        if (c.line == 0 && c.col == 0) return;
        if (c.col == 0) {
            int nl = c.line - 1;
            while (nl > 0 && isLineHidden(nl)) nl--;
            c.line = nl;
            c.col = buffer.lineAt(c.line).length();
            return;
        }
        String line = buffer.lineAt(c.line);
        int col = c.col - 1;
        while (col > 0 && charClass(line.charAt(col)) == 0) col--;
        if (col >= 0 && col < line.length()) {
            int cls = charClass(line.charAt(col));
            while (col > 0 && charClass(line.charAt(col - 1)) == cls) col--;
        }
        c.col = col;
    }

    protected void moveCaretWordRight(Caret c) {
        c.desiredCol = -1;
        String line = buffer.lineAt(c.line);
        if (c.col >= line.length()) {
            if (c.line < buffer.lineCount() - 1) {
                int nl = c.line + 1;
                while (nl < buffer.lineCount() && isLineHidden(nl)) nl++;
                if (nl < buffer.lineCount()) {
                    c.line = nl;
                    c.col = 0;
                }
            }
            return;
        }
        int col = c.col;
        int cls = charClass(line.charAt(col));
        if (cls == 0) {
            while (col < line.length() && charClass(line.charAt(col)) == 0) col++;
        } else {
            while (col < line.length() && charClass(line.charAt(col)) == cls) col++;
        }
        c.col = col;
    }

    protected void moveExtraCarets(int keyCode, boolean ctrl) {
        if (extraCarets.isEmpty()) return;
        int visibleLines = (keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN)
                ? getVisibleLines()
                : 0;
        for (Caret c : extraCarets) {
            c.line = Math.max(0, Math.min(c.line, buffer.lineCount() - 1));
            c.col = Math.max(0, Math.min(c.col, buffer.lineAt(c.line).length()));
            switch (keyCode) {
                case KeyEvent.VK_LEFT -> {
                    if (ctrl) moveCaretWordLeft(c);
                    else moveCaretLeft(c);
                }
                case KeyEvent.VK_RIGHT -> {
                    if (ctrl) moveCaretWordRight(c);
                    else moveCaretRight(c);
                }
                case KeyEvent.VK_UP -> moveCaretUp(c);
                case KeyEvent.VK_DOWN -> moveCaretDown(c);
                case KeyEvent.VK_HOME -> {
                    c.desiredCol = -1;
                    if (ctrl) c.line = 0;
                    c.col = 0;
                }
                case KeyEvent.VK_END -> {
                    c.desiredCol = -1;
                    if (ctrl) {
                        int nl = buffer.lineCount() - 1;
                        while (nl > 0 && isLineHidden(nl)) nl--;
                        c.line = nl;
                    }
                    c.col = buffer.lineAt(c.line).length();
                }
                case KeyEvent.VK_PAGE_UP -> {
                    if (c.desiredCol < 0) c.desiredCol = c.col;
                    int nl = Math.max(0, c.line - visibleLines);
                    while (nl > 0 && isLineHidden(nl)) nl--;
                    c.line = nl;
                    c.col = Math.min(c.desiredCol, buffer.lineAt(c.line).length());
                }
                case KeyEvent.VK_PAGE_DOWN -> {
                    if (c.desiredCol < 0) c.desiredCol = c.col;
                    int nl = Math.min(buffer.lineCount() - 1, c.line + visibleLines);
                    while (nl < buffer.lineCount() - 1 && isLineHidden(nl)) nl++;
                    while (nl > 0 && isLineHidden(nl)) nl--;
                    c.line = nl;
                    c.col = Math.min(c.desiredCol, buffer.lineAt(c.line).length());
                }
                default -> {
                }
            }
        }
        removeDuplicateExtraCarets();
    }

    protected void removeDuplicateExtraCarets() {
        if (extraCarets.isEmpty()) return;
        Set<Caret> seen = new LinkedHashSet<>();
        for (Caret c : new ArrayList<>(extraCarets)) {
            if (c.line == caretLine && c.col == caretCol) continue;
            seen.add(c);
        }
        extraCarets.clear();
        extraCarets.addAll(seen);
    }

    public void setFoldingEnabled(boolean enabled) {
        this.foldingEnabled = enabled;
        if (!enabled) {
            foldRegions = new ArrayList<>();
        } else {
            recomputeFoldRegions();
        }
        invalidateGeometry();
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    public void addFoldStateListener(Runnable listener) {
        if (listener != null) foldStateListeners.add(listener);
    }

    public void removeFoldStateListener(Runnable listener) {
        foldStateListeners.remove(listener);
    }

    protected void fireFoldStateChanged() {
        invalidateGeometry();
        for (Runnable r : foldStateListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    public void addFoldRule(FoldRule rule) {
        if (rule == null) return;
        foldRules.add(rule);
        refreshFoldRegionsAfterRuleChange();
    }

    public boolean removeFoldRule(FoldRule rule) {
        if (rule == null) return false;
        boolean removed = foldRules.remove(rule);
        if (removed) refreshFoldRegionsAfterRuleChange();
        return removed;
    }

    public void setFoldRules(Collection<? extends FoldRule> rules) {
        foldRules.clear();
        if (rules != null) {
            for (FoldRule rule : rules) {
                if (rule != null) foldRules.add(rule);
            }
        }
        refreshFoldRegionsAfterRuleChange();
    }

    public void clearFoldRules() {
        foldRules.clear();
        foldRegions = new ArrayList<>();
        invalidateGeometry();
        revalidate();
        repaint();
    }

    public List<FoldRule> getFoldRules() {
        return Collections.unmodifiableList(foldRules);
    }

    public List<FoldRegion> getFoldRegions() {
        return Collections.unmodifiableList(foldRegions);
    }

    protected void refreshFoldRegionsAfterRuleChange() {
        recomputeFoldRegions();
        invalidateGeometry();
        revalidate();
        repaint();
    }

    public void setToggleFoldKeyStroke(KeyStroke ks) {
        this.toggleFoldKeyStroke = ks;
        rebindMoveLineKeys();
    }

    public void toggleFoldAtCaret() {
        FoldRegion target = findEnclosingRegion(caretLine);
        if (target != null) toggleFoldRegion(target);
    }

    public void toggleFold(int bufferLine) {
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) {
                toggleFoldRegion(r);
                return;
            }
        }
    }

    public void foldAll() {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (!r.folded()) foldRegions.set(i, r.withFolded(true));
        }
        ensureCaretVisible();
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    public void unfoldAll() {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.folded()) foldRegions.set(i, r.withFolded(false));
        }
        fireFoldStateChanged();
        revalidate();
        repaint();
    }

    protected FoldRegion findEnclosingRegion(int line) {
        FoldRegion best = null;
        for (FoldRegion r : foldRegions) {
            if (r.contains(line)) {
                if (best == null || (r.endLine() - r.startLine()) < (best.endLine() - best.startLine())) {
                    best = r;
                }
            }
        }
        return best;
    }

    protected void toggleFoldRegion(FoldRegion target) {
        for (int i = 0; i < foldRegions.size(); i++) {
            FoldRegion r = foldRegions.get(i);
            if (r.startLine() == target.startLine() && r.endLine() == target.endLine()) {
                boolean newFolded = !r.folded();
                foldRegions.set(i, r.withFolded(newFolded));
                if (newFolded && caretLine > r.startLine() && caretLine <= r.endLine()) {
                    caretLine = r.startLine();
                    caretCol = Math.min(caretCol, buffer.lineAt(caretLine).length());
                    clearSelection();
                }
                fireFoldStateChanged();
                revalidate();
                repaint();
                return;
            }
        }
    }

    protected void unfoldToRevealCaret() {
        if (!foldingEnabled) return;
        boolean changed = false;
        FoldRegion hiding;
        while ((hiding = findFoldingRegionHiding(caretLine)) != null) {
            boolean unfolded = false;
            for (int i = 0; i < foldRegions.size(); i++) {
                FoldRegion r = foldRegions.get(i);
                if (r.startLine() == hiding.startLine() && r.endLine() == hiding.endLine() && r.folded()) {
                    foldRegions.set(i, r.withFolded(false));
                    unfolded = true;
                    changed = true;
                    break;
                }
            }
            if (!unfolded) break;
        }
        if (changed) fireFoldStateChanged();
    }

    protected void ensureCaretVisible() {
        if (!foldingEnabled) return;
        while (isLineHidden(caretLine)) {
            FoldRegion r = findFoldingRegionHiding(caretLine);
            if (r == null) break;
            caretLine = r.startLine();
        }
        caretCol = Math.min(caretCol, buffer.lineAt(caretLine).length());
    }

    protected FoldRegion findFoldingRegionHiding(int line) {
        for (FoldRegion r : foldRegions) {
            if (r.folded() && line > r.startLine() && line <= r.endLine()) return r;
        }
        return null;
    }

    protected void recomputeFoldRegions() {
        recomputeFoldRegions(true);
    }

    protected void recomputeFoldRegions(boolean preserveFoldedByLine) {
        if (!foldingEnabled || foldRules.isEmpty()) {
            foldRegions = new ArrayList<>();
            return;
        }
        List<FoldRegion> newRegions = new ArrayList<>();
        int lineCount = buffer.lineCount();
        for (FoldRule rule : foldRules) {
            computeRegionsForRule(rule, lineCount, newRegions);
        }
        if (preserveFoldedByLine) {
            for (int i = 0; i < newRegions.size(); i++) {
                FoldRegion nr = newRegions.get(i);
                for (FoldRegion old : foldRegions) {
                    if (old.startLine() == nr.startLine() && old.endLine() == nr.endLine() && old.folded()) {
                        newRegions.set(i, nr.withFolded(true));
                        break;
                    }
                }
            }
        }
        foldRegions = newRegions;
    }

    protected void computeRegionsForRule(FoldRule rule, int lineCount, List<FoldRegion> out) {
        if (rule instanceof FoldRule.Pair(char open, char close)) {
            computePairRegions(String.valueOf(open), String.valueOf(close), lineCount, out);
        } else if (rule instanceof FoldRule.StringPair(String open, String close)) {
            computePairRegions(open, close, lineCount, out);
        } else if (rule instanceof FoldRule.Section s) {
            String prefix = s.markerPrefix();
            if (prefix == null || prefix.isEmpty()) return;
            List<Integer> markers = new ArrayList<>();
            for (int line = 0; line < lineCount; line++) {
                if (buffer.lineAt(line).trim().startsWith(prefix)) markers.add(line);
            }
            for (int i = 0; i < markers.size(); i++) {
                int start = markers.get(i);
                int end = (i + 1 < markers.size()) ? markers.get(i + 1) - 1 : lineCount - 1;
                while (end > start && buffer.lineAt(end).trim().isEmpty()) end--;
                if (end > start) out.add(new FoldRegion(start, end, false));
            }
        } else if (rule instanceof FoldRule.Custom custom) {
            List<FoldRegion> regions = custom.provider().compute(new FoldRule.Context(lineCount, buffer::lineAt));
            if (regions == null) return;
            for (FoldRegion region : regions) {
                if (region == null) continue;
                int start = region.startLine();
                int end = region.endLine();
                if (start >= 0 && start < lineCount && end > start && end < lineCount) {
                    out.add(new FoldRegion(start, end, false));
                }
            }
        }
    }

    protected void computePairRegions(String open, String close, int lineCount, List<FoldRegion> out) {
        Deque<Integer> stack = new ArrayDeque<>();
        for (int line = 0; line < lineCount; line++) {
            String text = buffer.lineAt(line);
            int col = 0;
            while (col < text.length()) {
                int openAt = text.indexOf(open, col);
                int closeAt = text.indexOf(close, col);
                if (openAt < 0 && closeAt < 0) break;
                if (openAt >= 0 && (closeAt < 0 || openAt <= closeAt)) {
                    stack.push(line);
                    col = openAt + open.length();
                } else {
                    if (!stack.isEmpty()) {
                        int sLine = stack.pop();
                        if (line > sLine) out.add(new FoldRegion(sLine, line, false));
                    }
                    col = closeAt + close.length();
                }
            }
        }
    }

    public boolean isLineHidden(int bufferLine) {
        if (!foldingEnabled) return false;
        if (!geometryCacheDirty && cachedHiddenLines != null
                && bufferLine >= 0 && bufferLine < cachedLineCount) {
            return cachedHiddenLines.get(bufferLine);
        }
        for (FoldRegion r : foldRegions) {
            if (r.folded() && bufferLine > r.startLine() && bufferLine <= r.endLine()) return true;
        }
        return false;
    }

    public boolean isFoldAnchor(int bufferLine) {
        if (!foldingEnabled) return false;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine && r.folded()) return true;
        }
        return false;
    }

    protected String[] findFoldSeparatorsForRegion(FoldRegion region) {
        if (region == null || foldRules.isEmpty()) return null;
        String startLineText = buffer.lineAt(region.startLine());
        String endLineText = buffer.lineAt(region.endLine());
        for (FoldRule rule : foldRules) {
            if (rule instanceof FoldRule.Pair(char openChar, char closeChar)) {
                String open = String.valueOf(openChar);
                String close = String.valueOf(closeChar);
                if (startLineText.contains(open) && endLineText.contains(close)) {
                    return new String[]{open, close};
                }
            } else if (rule instanceof FoldRule.StringPair(String open, String close)) {
                if (startLineText.contains(open) && endLineText.contains(close)) {
                    return new String[]{open, close};
                }
            }
        }
        return null;
    }

    protected boolean shouldHideTrailingOpenForFold(int bufferLine) {
        if (!foldPlaceholderWithSeparators || !isFoldAnchor(bufferLine)) return false;
        String[] sep = findFoldSeparatorsForRegion(getFoldRegionStartingAt(bufferLine));
        if (sep == null || sep[0].isEmpty()) return false;
        String text = buffer.lineAt(bufferLine).stripTrailing();
        return text.endsWith(sep[0]);
    }

    public boolean isFoldAnchorLine(int bufferLine) {
        if (!foldingEnabled) return false;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) return true;
        }
        return false;
    }

    public FoldRegion getFoldRegionStartingAt(int bufferLine) {
        if (!foldingEnabled) return null;
        for (FoldRegion r : foldRegions) {
            if (r.startLine() == bufferLine) return r;
        }
        return null;
    }

    public int bufferLineToVisualLine(int bufferLine) {
        if (!foldingEnabled) return bufferLine;
        int visual = 0;
        int max = Math.min(bufferLine, buffer.lineCount());
        for (int i = 0; i < max; i++) {
            if (!isLineHidden(i)) visual++;
        }
        return visual;
    }

    public int visualLineToBufferLine(int visualLine) {
        if (!foldingEnabled) return Math.max(0, Math.min(visualLine, buffer.lineCount() - 1));
        int count = 0;
        for (int i = 0; i < buffer.lineCount(); i++) {
            if (!isLineHidden(i)) {
                if (count == visualLine) return i;
                count++;
            }
        }
        return Math.max(0, buffer.lineCount() - 1);
    }

    public int visualLineCount() {
        if (!foldingEnabled) return buffer.lineCount();
        int count = 0;
        for (int i = 0; i < buffer.lineCount(); i++) {
            if (!isLineHidden(i)) count++;
        }
        return Math.max(1, count);
    }

    public void invalidateGeometry() {
        geometryCacheDirty = true;
    }

    protected void ensureGeometry() {
        if (!geometryCacheDirty) {
            int currentLh = getFontMetrics(getFont()).getHeight();
            if (currentLh == cachedLineHeight && cachedLineCount == buffer.lineCount()) {
                return;
            }
        }
        rebuildGeometryCache();
    }

    protected void rebuildGeometryCache() {
        int lh = getFontMetrics(getFont()).getHeight();
        int n = buffer.lineCount();

        BitSet hidden = computeHiddenLines(n);
        Map<Integer, CodeLens> aboveMap = new HashMap<>();
        Map<Integer, CodeLens> inlineMap = new HashMap<>();
        if (codeLensesEnabled && !codeLenses.isEmpty()) {
            for (CodeLens lens : codeLenses) {
                int line = lens.line();
                if (line < 0 || line >= n) continue;
                if (lens.items().isEmpty()) continue;
                if (lens.placement() == CodeLensPlacement.ABOVE) {
                    aboveMap.putIfAbsent(line, lens);
                } else if (lens.placement() == CodeLensPlacement.INLINE) {
                    inlineMap.putIfAbsent(line, lens);
                }
            }
        }

        int[] lineY = new int[n];
        int[] visible = new int[n];
        int visibleCount = 0;
        int y = 0;
        int lensRows = 0;

        for (int i = 0; i < n; i++) {
            if (hidden.get(i)) {
                lineY[i] = y;
                continue;
            }
            if (aboveMap.containsKey(i)) {
                y += lh;
                lensRows++;
            }
            lineY[i] = y;
            visible[visibleCount++] = i;
            y += lh;
            if (hasGhostText() && i == ghostAnchorLine) {
                y += ghostReservedRows() * lh;
            }
        }

        int[] visibleTrimmed = new int[visibleCount];
        System.arraycopy(visible, 0, visibleTrimmed, 0, visibleCount);

        cachedLineY = lineY;
        cachedVisibleLines = visibleTrimmed;
        cachedTotalHeight = y;
        cachedLineHeight = lh;
        cachedLensRowCountValue = lensRows;
        cachedLineCount = n;
        cachedMaxLineWidth = -1;
        cachedIndentUnit = -1;
        cachedHiddenLines = hidden;
        cachedAboveLensByLine = aboveMap;
        cachedInlineLensByLine = inlineMap;
        geometryCacheDirty = false;
    }

    protected int getMaxLineWidth() {
        ensureGeometry();
        if (cachedMaxLineWidth >= 0) return cachedMaxLineWidth;
        FontMetrics fm = getFontMetrics(getFont());
        int max = 0;
        int n = buffer.lineCount();
        for (int i = 0; i < n; i++) {
            int w = textWidth(fm, buffer.lineAt(i), 0) + pushedInlayWidthForLine(i, fm);
            if (w > max) max = w;
        }
        cachedMaxLineWidth = max;
        return max;
    }

    protected BitSet computeHiddenLines(int lineCount) {
        BitSet set = new BitSet(lineCount);
        if (!foldingEnabled) return set;
        for (FoldRegion r : foldRegions) {
            if (!r.folded()) continue;
            int from = Math.max(0, r.startLine() + 1);
            int to = Math.min(lineCount - 1, r.endLine());
            for (int i = from; i <= to; i++) set.set(i);
        }
        return set;
    }

    public boolean hasCodeLens(int bufferLine) {
        ensureGeometry();
        return cachedAboveLensByLine != null && cachedAboveLensByLine.containsKey(bufferLine);
    }

    public CodeLens codeLensAtLine(int bufferLine) {
        ensureGeometry();
        if (cachedAboveLensByLine != null) {
            CodeLens a = cachedAboveLensByLine.get(bufferLine);
            if (a != null) return a;
        }
        if (cachedInlineLensByLine != null) {
            return cachedInlineLensByLine.get(bufferLine);
        }
        return null;
    }

    public CodeLens aboveCodeLensAtLine(int bufferLine) {
        ensureGeometry();
        return cachedAboveLensByLine == null ? null : cachedAboveLensByLine.get(bufferLine);
    }

    public CodeLens inlineCodeLensAtLine(int bufferLine) {
        ensureGeometry();
        return cachedInlineLensByLine == null ? null : cachedInlineLensByLine.get(bufferLine);
    }

    public int codeLensRowCount() {
        ensureGeometry();
        return cachedLensRowCountValue;
    }

    public int yOfBufferLine(int bufferLine) {
        ensureGeometry();
        if (bufferLine < 0) return 0;
        if (bufferLine >= cachedLineY.length) return cachedTotalHeight;
        return cachedLineY[bufferLine];
    }

    public int yOfCodeLensRow(int bufferLine) {
        if (!hasCodeLens(bufferLine)) return -1;
        ensureGeometry();
        return cachedLineY[bufferLine] - cachedLineHeight;
    }

    public int bufferLineAtY(int yMouse) {
        ensureGeometry();
        int n = cachedVisibleLines.length;
        if (n == 0) return 0;
        if (yMouse <= 0) return cachedVisibleLines[0];
        int lh = cachedLineHeight;
        int last = cachedVisibleLines[n - 1];
        if (yMouse >= cachedLineY[last] + lh) return last;

        int lo = 0, hi = n - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int line = cachedVisibleLines[mid];
            int end = cachedLineY[line] + lh;
            if (end <= yMouse) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return cachedVisibleLines[lo];
    }

    public boolean isInCodeLensRow(int yMouse) {
        if (yMouse < 0) return false;
        ensureGeometry();
        int line = bufferLineAtY(yMouse);
        if (!hasCodeLens(line)) return false;
        return yMouse < cachedLineY[line];
    }

    public int totalContentHeight() {
        ensureGeometry();
        return cachedTotalHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(defaultStyle.getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font baseFont = getFont();
        FontMetrics defaultFm = g2.getFontMetrics(baseFont);
        int lineHeight = defaultFm.getHeight();

        Rectangle clip = g.getClipBounds();
        int clipMinY = clip != null ? clip.y : 0;
        int clipMaxY = clip != null ? clip.y + clip.height : getHeight();
        int firstVisibleLine = clip != null ? bufferLineAtY(clipMinY) : 0;
        int lastVisibleLine  = clip != null ? bufferLineAtY(clipMaxY) : buffer.lineCount() - 1;
        firstVisibleLine = Math.max(0, firstVisibleLine);
        lastVisibleLine  = Math.min(buffer.lineCount() - 1, lastVisibleLine);

        int totalLines = buffer.lineCount();
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < totalLines; i++) {
            if (isLineHidden(i)) continue;
            int ly = yOfBufferLine(i);

            LineColorInfoInternal lineColor = lineColors.get(i);
            if (lineColor != null) {
                if (lineColor.background != null) {
                    g2.setColor(lineColor.background);
                }
                g2.fillRect(0, ly, getWidth(), lineHeight);
            }
        }

        if (highlightCurrentLine) {
            int ly = yOfBufferLine(caretLine);
            if (currentLineColor != null) {
                g2.setColor(currentLineColor);
            } else {
                Color base = defaultStyle.getBackground();
                int shift = isDarkBackground(base) ? 20 : -15;
                g2.setColor(new Color(
                        clamp(base.getRed() + shift),
                        clamp(base.getGreen() + shift),
                        clamp(base.getBlue() + shift)
                ));
            }
            g2.fillRect(0, ly, getWidth(), lineHeight);
        }

        paintSelectedTextOccurrences(g2, defaultFm, lineHeight);

        if (hasSelection() || hasExtraSelections()) {
            paintSelection(g2, defaultFm, lineHeight);
        }

        paintSearchMatches(g2, defaultFm);

        paintIndentGuides(g2, defaultFm, lineHeight);

        codeLensItemBounds.clear();
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < totalLines; i++) {
            if (isLineHidden(i)) continue;
            if (hasCodeLens(i)) {
                paintCodeLensRow(g2, defaultFm, baseFont, i, yOfCodeLensRow(i), lineHeight);
            }
            int lineOffset = buffer.offsetOfLine(i);
            String lineText = buffer.lineAt(i);
            int ly = yOfBufferLine(i);
            int x = 4;

            int renderLength = lineText.length();
            if (shouldHideTrailingOpenForFold(i)) {
                int idx = lineText.length() - 1;
                while (idx >= 0 && Character.isWhitespace(lineText.charAt(idx))) idx--;
                if (idx >= 0) renderLength = idx;
            }

            int col = 0;
            int visualCol = 0;
            List<InlayHint> pushHints = pushInlayHintsForLine(i, defaultFm);
            int pushHintIndex = 0;

            int ghostPushCol = (i == ghostAnchorLine && isGhostVisibleAtAnchor()) ? ghostAnchorCol : -1;
            boolean ghostPushApplied = false;
            while (col < renderLength) {
                while (pushHintIndex < pushHints.size() && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= col) {
                    x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
                    pushHintIndex++;
                }
                if (ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= col) {
                    x += ghostFirstSegmentWidth(defaultFm);
                    ghostPushApplied = true;
                }

                TextStyle style = getStyleAt(lineOffset + col);
                int runEnd = col + 1;
                while (runEnd < renderLength && getStyleAt(lineOffset + runEnd) == style) {
                    runEnd++;
                }
                if (pushHintIndex < pushHints.size()) {
                    int pushCol = inlayHintColumn(pushHints.get(pushHintIndex), lineText);
                    if (pushCol > col && pushCol < runEnd) {
                        runEnd = pushCol;
                    }
                }

                if (ghostPushCol > col && ghostPushCol < runEnd) {
                    runEnd = ghostPushCol;
                }

                String run = expandTabs(lineText.substring(col, runEnd), visualCol);
                Font font = deriveFont(baseFont, style);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                int runWidth = fm.stringWidth(run);

                if (style.getBackground() != null && style.getBackground() != defaultStyle.getBackground()) {
                    g2.setColor(style.getBackground());
                    g2.fillRect(x, ly, runWidth, lineHeight);
                }

                LineColorInfoInternal lineInfo = lineColors.get(i);
                Color fg = style.getForeground();
                if (lineInfo != null && lineInfo.foreground != null) {
                    fg = lineInfo.foreground;
                }
                g2.setColor(fg);
                g2.drawString(run, x, ly + fm.getAscent());

                if (style.isUnderline()) {
                    int uy = ly + fm.getAscent() + 1;
                    g2.drawLine(x, uy, x + runWidth, uy);
                }

                x += runWidth;
                visualCol += run.length();
                col = runEnd;
            }
            while (pushHintIndex < pushHints.size() && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= renderLength) {
                x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
                pushHintIndex++;
            }
            if (ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= renderLength) {
                x += ghostFirstSegmentWidth(defaultFm);
                ghostPushApplied = true;
            }

            if (isFoldAnchor(i)) {
                g2.setFont(baseFont);
                FontMetrics fm = g2.getFontMetrics(baseFont);
                String pillText = foldPlaceholder == null ? "…" : foldPlaceholder.trim();
                if (pillText.isEmpty()) pillText = "…";
                if (foldPlaceholderWithSeparators) {
                    String[] sep = findFoldSeparatorsForRegion(getFoldRegionStartingAt(i));
                    if (sep != null) pillText = sep[0] + pillText + sep[1];
                }
                int hPad = 6;
                int textW = fm.stringWidth(pillText);
                int pillX = x + 6;
                int pillH = lineHeight - 4;
                int pillY = ly + 2;
                int pillW = textW + hPad * 2;

                Color baseFg = defaultStyle.getForeground();
                Color pillBg = new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 28);
                Color pillBorder = new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 110);

                Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pillBg);
                g2.fillRoundRect(pillX, pillY, pillW, pillH, 8, 8);
                g2.setColor(pillBorder);
                g2.drawRoundRect(pillX, pillY, pillW, pillH, 8, 8);
                if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);

                Color placeholder = foldPlaceholderColor != null
                        ? foldPlaceholderColor
                        : new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 200);
                g2.setColor(placeholder);
                g2.drawString(pillText, pillX + hPad, ly + fm.getAscent());
            }
            paintInlineCodeLens(g2, defaultFm, baseFont, i, ly, lineHeight);
        }

        paintWordHover(g2, defaultFm, lineHeight);

        int caretOffset = caretOffset();

        bracketHighlighter.paint(
                g2,
                defaultFm,
                caretOffset,
                lineHeight
        );

        paintDiagnostics(g2, defaultFm);
        paintInlayHints(g2, defaultFm);
        paintGhostText(g2, defaultFm, lineHeight);

        if (isFocusOwner() && caretVisible) {
            g2.setFont(baseFont);
            paintCaret(g2, defaultFm);
            paintExtraCarets(g2, defaultFm);
        }
    }

    protected void paintSelection(Graphics2D g2, FontMetrics fm, int lineHeight) {
        if (hasSelection()) {
            paintSelectionRange(g2, fm, lineHeight, getSelectionStart(), getSelectionEnd());
        }
        for (Caret c : extraCarets) {
            if (!c.hasSelection()) continue;
            int start = extraSelectionStart(c);
            int end = extraSelectionEnd(c);
            if (start != end) paintSelectionRange(g2, fm, lineHeight, start, end);
        }
    }

    protected void paintSelectionRange(Graphics2D g2, FontMetrics fm, int lineHeight, int startOff, int endOff) {
        paintSelectionRange(g2, fm, lineHeight, startOff, endOff, selectionColor);
    }

    protected void paintSelectedTextOccurrences(Graphics2D g2, FontMetrics fm, int lineHeight) {
        int[] offsets = selectedTextOccurrenceOffsets;
        if (!highlightSelectedTextOccurrences || offsets.length == 0) return;

        Rectangle clip = g2.getClipBounds();
        int firstVisibleLine = clip != null ? bufferLineAtY(clip.y) : 0;
        int lastVisibleLine = clip != null
                ? bufferLineAtY(clip.y + clip.height)
                : buffer.lineCount() - 1;
        firstVisibleLine = Math.max(0, firstVisibleLine);
        lastVisibleLine = Math.min(buffer.lineCount() - 1, lastVisibleLine);
        int visibleStartOffset = buffer.offsetOfLine(firstVisibleLine);
        int visibleEndOffset = lastVisibleLine + 1 < buffer.lineCount()
                ? buffer.offsetOfLine(lastVisibleLine + 1)
                : buffer.length();

        int low = 0;
        int high = offsets.length / 2;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (offsets[middle * 2 + 1] <= visibleStartOffset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        Color color = selectedTextOccurrencesColor != null
                ? selectedTextOccurrencesColor
                : selectionColor;
        for (int i = low * 2; i < offsets.length; i += 2) {
            int start = offsets[i];
            if (start >= visibleEndOffset) break;
            paintSelectionRange(
                    g2,
                    fm,
                    lineHeight,
                    start,
                    offsets[i + 1],
                    color);
        }
    }

    protected void paintSelectionRange(
            Graphics2D g2,
            FontMetrics fm,
            int lineHeight,
            int startOff,
            int endOff,
            Color color) {
        int startLine = buffer.lineOfOffset(startOff);
        int endLine = buffer.lineOfOffset(endOff);

        if (color != null) {
            g2.setColor(color);
        } else {
            Color uiColor = UIManager.getColor("TextArea.selectionBackground");
            g2.setColor(uiColor != null ? uiColor : new Color(51, 153, 255, 80));
        }

        Rectangle clip = g2.getClipBounds();
        int firstLine = clip != null ? Math.max(startLine, bufferLineAtY(clip.y)) : startLine;
        int lastLine = clip != null ? Math.min(endLine, bufferLineAtY(clip.y + clip.height)) : endLine;
        for (int i = firstLine; i <= lastLine; i++) {
            if (isLineHidden(i)) continue;
            String lineText = buffer.lineAt(i);
            int lineOffset = buffer.offsetOfLine(i);
            int ly = yOfBufferLine(i);

            int colStart = (i == startLine) ? startOff - lineOffset : 0;
            int colEnd = (i == endLine) ? endOff - lineOffset : lineText.length();

            int x1 = 4 + textWidth(fm, lineText.substring(0, colStart), 0);
            int x2 = 4 + textWidth(fm, lineText.substring(0, colEnd), 0);

            if (i != endLine && colEnd == lineText.length()) {
                x2 += fm.charWidth(' ');
            }

            g2.fillRect(x1, ly, x2 - x1, lineHeight);
        }
    }

    protected void paintGhostText(Graphics2D g2, FontMetrics fm, int lineHeight) {
        if (!hasGhostText()) return;

        if (ghostAnchorLine != caretLine || ghostAnchorCol != caretCol) return;

        Color color = ghostTextColor;
        if (color == null) {
            Color fg = defaultStyle.getForeground();
            color = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 110);
        }

        Font baseFont = getFont();
        g2.setFont(baseFont);
        g2.setColor(color);

        String lineText = buffer.lineAt(caretLine);

        int startX = baseVisualXForColumn(caretLine, lineText, caretCol, fm);
        int y = yOfBufferLine(caretLine);
        int ascent = fm.getAscent();

        String[] segments = ghostText.split("\n", -1);
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            int x = (i == 0) ? startX : 4;
            int segY = y + i * lineHeight;
            if (!seg.isEmpty()) {
                g2.drawString(seg, x, segY + ascent);
            }
        }
    }

    protected void paintCaret(Graphics2D g2, FontMetrics fm) {
        int lineHeight = fm.getHeight();
        String lineText = buffer.lineAt(caretLine);

        int cx = baseVisualXForColumn(caretLine, lineText, caretCol, fm);
        int cy = yOfBufferLine(caretLine);

        g2.setColor(defaultStyle.getForeground());
        if (overwriteMode) {
            int charW = (caretCol < lineText.length())
                    ? fm.charWidth(lineText.charAt(caretCol))
                    : fm.charWidth(' ');
            Composite original = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g2.fillRect(cx, cy, charW, lineHeight);
            g2.setComposite(original);
        } else {
            g2.fillRect(cx, cy, 2, lineHeight);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight();

        int width = getMaxLineWidth();
        int bottomPadding = lineHeight * 5;

        return new Dimension(
                width + caretScrollRightMargin + 8,
                totalContentHeight() + bottomPadding
        );
    }

    protected class KeyHandler extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();

            if (c == KeyEvent.CHAR_UNDEFINED) return;
            if (c == '\n' || c == '\r') return;
            if (c < 0x20 && c != '\t') return;
            if (c == '\u007F') return;
            if (e.isControlDown() || e.isMetaDown()) return;
            if (e.isAltDown() && !e.isAltGraphDown()) return;
            if (c == '\t') {
                e.consume();
                return;
            }
            if (readOnly) {
                e.consume();
                return;
            }

            if (autoClosePairs && isClosingChar(c)) {
                int offset = caretOffset();
                if (offset < buffer.length()) {
                    char next = buffer.charAt(offset);
                    if (next == c) {
                        caretCol++;
                        scrollToCaret();
                        resetCaretBlink();
                        repaint();
                        handleSignatureHelpAfterTyping(c);
                        return;
                    }
                }
            }

            if (autoClosePairs && autoClosePairsMap.containsKey(c)) {
                char close = autoClosePairsMap.get(c);

                if (hasSelection() || hasExtraSelections()) {
                    wrapSelectionsAtCarets(c, close);
                } else {
                    beginCompoundEdit();
                    try {
                        insertAtAllCarets("" + c + close, false, 1);
                    } finally {
                        endCompoundEdit();
                    }
                    updateLastEditState();
                }

                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                handleSignatureHelpAfterTyping(c);
                return;
            }

            if (hasSelection() || hasExtraSelections()) {
                replaceSelectionsAtCarets(String.valueOf(c), 1);
            } else {
                beginCompoundEdit();
                try {
                    insertAtAllCarets(String.valueOf(c), overwriteMode, 1);
                } finally {
                    endCompoundEdit();
                }
            }

            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
            fireStateChangedIfNeeded();

            if (isAutoCompleteVisible()) {
                refreshAutoCompleteIfVisible();
            } else if (autoCompleteOnTyping && autoCompleteProvider != null && shouldAttemptTypingTrigger(c)) {
                triggerAutoComplete(CompletionContext.TriggerKind.TYPING);
            }

            if (isGhostTextActive() && isGhostTypingActivation() && ghostTextProvider != null) {
                requestGhostText(GhostTextContext.TriggerKind.TYPING);
            }

            handleSignatureHelpAfterTyping(c);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            boolean ctrl = e.isControlDown() || e.isMetaDown();
            boolean shift = e.isShiftDown();

            if (isNavigationKey(e.getKeyCode())) {
                clearInlayInteraction();
                clearGhostText();
                hideHoverDocumentation();
            }

            updateWordHover(e.getModifiersEx());

            if (!extraCarets.isEmpty() && matchesKeyStroke(e, clearExtraCaretsKeyStroke)
                    && !isAutoCompleteVisible()) {
                clearExtraCarets();
                e.consume();
                repaint();
                return;
            }

            if (isAutoCompleteVisible()) {
                if (isAutoCompleteAccept(e)) {
                    if (readOnly) {
                        hideAutoCompletePopup();
                        e.consume();
                        return;
                    }
                    applyAutoCompleteSelection();
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, autoCompleteDismissKeyStroke)) {
                    hideAutoCompletePopup();
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, autoCompleteNextKeyStroke)) {
                    autoCompletePopup.moveSelection(1);
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, autoCompletePrevKeyStroke)) {
                    autoCompletePopup.moveSelection(-1);
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, autoCompletePageDownKeyStroke)) {
                    autoCompletePopup.moveSelection(10);
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, autoCompletePageUpKeyStroke)) {
                    autoCompletePopup.moveSelection(-10);
                    e.consume();
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_HOME, KeyEvent.VK_END ->
                            hideAutoCompletePopup();
                }
            }

            if (isSignatureHelpVisible()) {
                if (matchesKeyStroke(e, signatureHelpNextKeyStroke)) {
                    signatureHelpPopup.nextSignature();
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, signatureHelpPrevKeyStroke)) {
                    signatureHelpPopup.previousSignature();
                    e.consume();
                    return;
                }
                if (matchesKeyStroke(e, signatureHelpDismissKeyStroke)) {
                    hideSignatureHelp();
                    e.consume();
                    return;
                }
            }

            if (matchesKeyStroke(e, duplicateLineUpKeyStroke)) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                duplicateLineUp();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                e.consume();
                return;
            }
            if (matchesKeyStroke(e, duplicateLineDownKeyStroke)) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                duplicateLineDown();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                e.consume();
                return;
            }
            if (matchesKeyStroke(e, moveLineUpKeyStroke)) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                moveLineUp();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                e.consume();
                return;
            }
            if (matchesKeyStroke(e, moveLineDownKeyStroke)) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                moveLineDown();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                e.consume();
                return;
            }

            if (ctrl) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_C -> {
                        if (copyPasteEnabled) {
                            copyToClipboard();
                        }
                        return;
                    }
                    case KeyEvent.VK_V -> {
                        if (readOnly) {
                            e.consume();
                            return;
                        }
                        if (copyPasteEnabled) {
                            pasteFromClipboard();
                            scrollToCaret();
                            resetCaretBlink();
                            revalidate();
                            repaint();
                        }
                        return;
                    }
                    case KeyEvent.VK_X -> {
                        if (readOnly) {
                            e.consume();
                            return;
                        }
                        if (copyPasteEnabled) {
                            copyToClipboard();
                            deleteSelectionsAtCarets();
                            scrollToCaret();
                            resetCaretBlink();
                            revalidate();
                            repaint();
                        }
                        return;
                    }
                    case KeyEvent.VK_A -> {
                        selectAll();
                        repaint();
                        return;
                    }
                    case KeyEvent.VK_Z -> {
                        if (readOnly) {
                            e.consume();
                            return;
                        }
                        if (shift) performRedo();
                        else performUndo();
                        scrollToCaret();
                        resetCaretBlink();
                        revalidate();
                        repaint();
                        return;
                    }
                    case KeyEvent.VK_Y -> {
                        if (readOnly) {
                            e.consume();
                            return;
                        }
                        performRedo();
                        scrollToCaret();
                        resetCaretBlink();
                        revalidate();
                        repaint();
                        return;
                    }
                }
            }

            if (hasGhostText() && !isAutoCompleteVisible() && matchesKeyStroke(e, ghostTextAcceptKeyStroke)) {
                if (readOnly) {
                    clearGhostText();
                } else {
                    acceptGhostText();
                }
                e.consume();
                return;
            }

            if (hasGhostText() && e.getKeyCode() == KeyEvent.VK_ESCAPE && !isAutoCompleteVisible()) {
                clearGhostText();
                e.consume();
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_TAB && hasActiveSnippetSession() && !isAutoCompleteVisible()
                    && (!hasSelection() || selectionMatchesCurrentSnippetStop())) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                if (shift) {
                    snippetPreviousStop();
                } else {
                    snippetNextStop();
                }
                e.consume();
                scrollToCaret();
                resetCaretBlink();
                revalidate();
                repaint();
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE && hasActiveSnippetSession() && !isAutoCompleteVisible()) {
                clearSnippetSession();
                e.consume();
                repaint();
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                if (readOnly) {
                    e.consume();
                    return;
                }
                if (hasSelection()) {
                    indentSelection(shift);
                    e.consume();
                    scrollToCaret();
                    resetCaretBlink();
                    revalidate();
                    repaint();
                    return;
                } else if (shift) {
                    outdentCurrentLine();
                    e.consume();
                    scrollToCaret();
                    resetCaretBlink();
                    revalidate();
                    repaint();
                    return;
                } else {
                    String indent = getIndentString();
                    beginCompoundEdit();
                    try {
                        insertAtAllCarets(indent, false, getIndentAdvance());
                    } finally {
                        endCompoundEdit();
                    }
                    e.consume();
                    scrollToCaret();
                    resetCaretBlink();
                    revalidate();
                    repaint();
                    return;
                }
            }

            if (shift && isNavigationKey(e.getKeyCode())) {
                startSelectionIfNeeded();
                startExtraSelectionsIfNeeded();
            } else if (isNavigationKey(e.getKeyCode())) {
                clearSelection();
                clearExtraSelections();
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_INSERT -> {
                    if (readOnly) {
                        e.consume();
                        return;
                    }
                    overwriteMode = !overwriteMode;
                    resetCaretBlink();
                }
                case KeyEvent.VK_ENTER -> {
                    if (readOnly) {
                        e.consume();
                        return;
                    }
                    beginCompoundEdit();
                    try {
                        if (hasSelection()) {
                            deleteSelection();
                        }

                        String currentLine = buffer.lineAt(caretLine);
                        String indent = getLeadingWhitespace(currentLine);

                        FoldRegion foldedAnchor = null;
                        if (foldingEnabled && caretCol >= currentLine.length()) {
                            for (FoldRegion r : foldRegions) {
                                if (r.startLine() == caretLine && r.folded()) {
                                    foldedAnchor = r;
                                    break;
                                }
                            }
                        }

                        int offset;
                        if (foldedAnchor != null) {
                            int endLine = foldedAnchor.endLine();
                            offset = buffer.offsetOfLine(endLine) + buffer.lineAt(endLine).length();
                        } else {
                            offset = caretOffset();
                        }

                        String beforeCaret = currentLine.substring(0, Math.min(caretCol, currentLine.length())).trim();
                        String afterCaret = currentLine.substring(Math.min(caretCol, currentLine.length())).trim();
                        boolean increaseIndent = smartIndentEnabled && !beforeCaret.isEmpty()
                                && "{[(".indexOf(beforeCaret.charAt(beforeCaret.length() - 1)) >= 0;
                        boolean closePair = increaseIndent && !afterCaret.isEmpty()
                                && "}])".indexOf(afterCaret.charAt(0)) >= 0;
                        String childIndent = increaseIndent ? indent + getIndentString() : indent;
                        if (closePair) {
                            insertText(offset, "\n" + childIndent + "\n" + indent);
                            setCaretFromOffset(offset + 1 + childIndent.length());
                        } else {
                            insertText(offset, "\n" + childIndent);
                            setCaretFromOffset(offset + 1 + childIndent.length());
                        }
                        clearSelection();
                    } finally {
                        endCompoundEdit();
                    }
                    updateLastEditState();

                    e.consume();
                }
                case KeyEvent.VK_BACK_SPACE -> {
                    if (readOnly) {
                        e.consume();
                        return;
                    }
                    if (deleteAtCarets(true)) {
                        scrollToCaret();
                        resetCaretBlink();
                        revalidate();
                        repaint();
                        return;
                    } else if (hasSelection()) {
                        deleteSelection();
                    } else {
                        clearSelection();
                        int offset = caretOffset();
                        if (offset > 0) {

                            if (stripBlankLines && caretLine > 0) {
                                String lineText = buffer.lineAt(caretLine);
                                if (lineText.replace("\r", "").trim().isEmpty()) {
                                    int start = buffer.offsetOfLine(caretLine) - 1;
                                    int end = buffer.offsetOfLine(caretLine) + lineText.length();
                                    deleteText(start, end);
                                    caretLine--;
                                    caretCol = buffer.lineAt(caretLine).length();
                                    unfoldToRevealCaret();
                                    scrollToCaret();
                                    resetCaretBlink();
                                    revalidate();
                                    repaint();
                                    return;
                                }
                            }

                            if (useSpacesForTab && caretCol >= tabSize) {
                                String lineText = buffer.lineAt(caretLine);
                                String before = lineText.substring(0, caretCol);
                                if (before.endsWith(" ".repeat(tabSize)) && before.trim().isEmpty()) {
                                    deleteText(offset - tabSize, offset);
                                    caretCol -= tabSize;
                                    scrollToCaret();
                                    resetCaretBlink();
                                    revalidate();
                                    repaint();
                                    return;
                                }
                            }
                            if (caretCol > 0) {
                                caretCol--;
                                deleteText(offset - 1, offset);
                            } else {
                                caretLine--;
                                caretCol = buffer.lineAt(caretLine).length();
                                deleteText(offset - 1, offset);

                                unfoldToRevealCaret();
                            }
                        }
                    }
                }
                case KeyEvent.VK_DELETE -> {
                    if (readOnly) {
                        e.consume();
                        return;
                    }
                    if (deleteAtCarets(false)) {
                        scrollToCaret();
                        resetCaretBlink();
                        revalidate();
                        repaint();
                        return;
                    } else if (hasSelection()) {
                        deleteSelection();
                    } else {
                        int offset = caretOffset();
                        if (offset < buffer.length()) {
                            deleteText(offset, offset + 1);
                        }
                    }
                }
                case KeyEvent.VK_LEFT -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    desiredCaretCol = -1;
                    if (ctrl) {
                        moveWordLeft();
                    } else {
                        if (caretCol > 0) {
                            caretCol--;
                        } else if (caretLine > 0) {
                            int nl = caretLine - 1;
                            while (nl > 0 && isLineHidden(nl)) nl--;
                            caretLine = nl;
                            caretCol = buffer.lineAt(caretLine).length();
                        }
                    }
                }
                case KeyEvent.VK_RIGHT -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    desiredCaretCol = -1;
                    if (ctrl) {
                        moveWordRight();
                    } else {
                        if (caretCol < buffer.lineAt(caretLine).length()) {
                            caretCol++;
                        } else if (caretLine < buffer.lineCount() - 1) {
                            int nl = caretLine + 1;
                            while (nl < buffer.lineCount() && isLineHidden(nl)) nl++;
                            if (nl < buffer.lineCount()) {
                                caretLine = nl;
                                caretCol = 0;
                            }
                        }
                    }
                }
                case KeyEvent.VK_UP -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    if (desiredCaretCol < 0) desiredCaretCol = caretCol;
                    int nl = caretLine - 1;
                    while (nl >= 0 && isLineHidden(nl)) {
                        nl--;
                    }
                    if (nl >= 0) {
                        caretLine = nl;
                        caretCol = Math.min(desiredCaretCol, buffer.lineAt(caretLine).length());
                    }
                }
                case KeyEvent.VK_DOWN -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    if (desiredCaretCol < 0) desiredCaretCol = caretCol;
                    int nl = caretLine + 1;
                    while (nl < buffer.lineCount() && isLineHidden(nl)) {
                        nl++;
                    }
                    if (nl < buffer.lineCount()) {
                        caretLine = nl;
                        caretCol = Math.min(desiredCaretCol, buffer.lineAt(caretLine).length());
                    }
                }
                case KeyEvent.VK_HOME -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    desiredCaretCol = -1;
                    if (ctrl) caretLine = 0;
                    caretCol = 0;
                }
                case KeyEvent.VK_END -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    desiredCaretCol = -1;
                    if (ctrl) {
                        int nl = buffer.lineCount() - 1;
                        while (nl > 0 && isLineHidden(nl)) nl--;
                        caretLine = nl;
                    }
                    caretCol = buffer.lineAt(caretLine).length();
                }
                case KeyEvent.VK_PAGE_UP -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    if (desiredCaretCol < 0) desiredCaretCol = caretCol;
                    int visibleLines = getVisibleLines();
                    int nl = Math.max(0, caretLine - visibleLines);
                    while (nl > 0 && isLineHidden(nl)) nl--;
                    caretLine = nl;
                    caretCol = Math.min(desiredCaretCol, buffer.lineAt(caretLine).length());
                }
                case KeyEvent.VK_PAGE_DOWN -> {
                    moveExtraCarets(e.getKeyCode(), ctrl);
                    if (desiredCaretCol < 0) desiredCaretCol = caretCol;
                    int visibleLines = getVisibleLines();
                    int nl = Math.min(buffer.lineCount() - 1, caretLine + visibleLines);
                    while (nl < buffer.lineCount() - 1 && isLineHidden(nl)) nl++;
                    while (nl > 0 && isLineHidden(nl)) nl--;
                    caretLine = nl;
                    caretCol = Math.min(desiredCaretCol, buffer.lineAt(caretLine).length());
                }
                default -> {
                    return;
                }
            }

            // The editor handled this key. Do not let an ancestor JScrollPane handle
            // the same arrow key again as a unit-scroll command.
            e.consume();
            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
            fireStateChangedIfNeeded();

            if (isAutoCompleteVisible()
                    && (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE)) {
                refreshAutoCompleteIfVisible();
            }

            if (isSignatureHelpVisible()) {
                refreshSignatureHelpIfVisible();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            updateWordHover(e.getModifiersEx());
        }

        protected boolean isNavigationKey(int keyCode) {
            return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT
                    || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN
                    || keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END
                    || keyCode == KeyEvent.VK_PAGE_UP || keyCode == KeyEvent.VK_PAGE_DOWN;
        }

        protected boolean isClosingChar(char c) {
            return autoClosePairsMap.containsValue(c);
        }
    }

    protected void paintWordHover(Graphics2D g2, FontMetrics fm, int lineHeight) {
        if (!wordHoverEnabled) {
            return;
        }
        if (wordHoverLine < 0 || wordHoverStartCol < 0 || wordHoverEndCol <= wordHoverStartCol) {
            return;
        }
        if (isLineHidden(wordHoverLine)) {
            return;
        }
        String lineText = buffer.lineAt(wordHoverLine);
        if (wordHoverEndCol > lineText.length()) {
            return;
        }

        WordHoverStyle style = wordHoverActiveStyle != null ? wordHoverActiveStyle : wordHoverStyle;
        if (style == null && wordHoverPainter == null) {
            return;
        }

        int yTop = yOfBufferLine(wordHoverLine);
        int xStart = visualXForColumn(wordHoverLine, lineText, wordHoverStartCol, fm);
        int xEnd = visualXForColumn(wordHoverLine, lineText, wordHoverEndCol, fm);

        int lineOffset = buffer.offsetOfLine(wordHoverLine);
        String word = lineText.substring(wordHoverStartCol, wordHoverEndCol);

        WordHoverContext ctx = new WordHoverContext(
                wordHoverLine, wordHoverStartCol, wordHoverEndCol,
                word, lineOffset + wordHoverStartCol, lineOffset + wordHoverEndCol,
                xStart, xEnd, yTop, lineHeight,
                getFont(),
                defaultStyle.getForeground(), defaultStyle.getBackground(),
                style
        );

        if (wordHoverPainter != null) {
            wordHoverPainter.paint(g2, fm, ctx);
            return;
        }

        paintDefaultWordHover(g2, fm, ctx);
    }

    protected void paintDefaultWordHover(Graphics2D g2, FontMetrics fm, WordHoverContext ctx) {
        WordHoverStyle style = ctx.style();
        if (style == null) {
            return;
        }

        int width = ctx.xEnd() - ctx.xStart();

        if (style.getBackground() != null) {
            g2.setColor(style.getBackground());
            g2.fillRect(ctx.xStart(), ctx.yTop(), width, ctx.lineHeight());
        }

        boolean restyled = style.isBold() || style.isItalic() || style.getForeground() != null;
        if (restyled) {
            if (style.getBackground() == null) {
                g2.setColor(ctx.defaultBackground());
                g2.fillRect(ctx.xStart(), ctx.yTop(), width, ctx.lineHeight());
            }
            int fontStyle = Font.PLAIN;
            if (style.isBold()) fontStyle |= Font.BOLD;
            if (style.isItalic()) fontStyle |= Font.ITALIC;
            Font hoverFont = ctx.baseFont().deriveFont(fontStyle);
            FontMetrics hfm = g2.getFontMetrics(hoverFont);
            g2.setFont(hoverFont);
            Color fg = style.getForeground() != null ? style.getForeground() : ctx.defaultForeground();
            g2.setColor(fg);
            g2.drawString(ctx.word(), ctx.xStart(), ctx.yTop() + hfm.getAscent());
            g2.setFont(ctx.baseFont());
        }

        if (style.isUnderline()) {
            Color underColor = style.getUnderlineColor() != null
                    ? style.getUnderlineColor()
                    : (style.getForeground() != null ? style.getForeground() : ctx.defaultForeground());
            int thick = Math.max(1, style.getUnderlineThickness());
            int uy = ctx.yTop() + fm.getAscent() + 1;
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(thick));
            g2.setColor(underColor);
            g2.drawLine(ctx.xStart(), uy, ctx.xEnd(), uy);
            g2.setStroke(old);
        }

        if (style.isBox()) {
            Color bxColor = style.getBoxColor() != null
                    ? style.getBoxColor()
                    : (style.getForeground() != null ? style.getForeground() : ctx.defaultForeground());
            int thick = Math.max(1, style.getBoxThickness());
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(thick));
            g2.setColor(bxColor);
            g2.drawRect(ctx.xStart(), ctx.yTop(), width - 1, ctx.lineHeight() - 1);
            g2.setStroke(old);
        }
    }

    protected void paintIndentGuides(Graphics2D g2, FontMetrics fm, int lineHeight) {
        if (!showIndentGuides) return;
        Color base = UIManager.getColor("TextArea.foreground");
        if (base == null) base = Color.GRAY;

        Color guideColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), 40);

        int charWidth = fm.charWidth(' ');

        Rectangle clip = g2.getClipBounds();
        int firstLine = clip != null ? bufferLineAtY(clip.y) : 0;
        int lastLine  = clip != null ? bufferLineAtY(clip.y + clip.height) : buffer.lineCount() - 1;
        firstLine = Math.max(0, firstLine);
        lastLine  = Math.min(buffer.lineCount() - 1, lastLine);

        int unit = Math.max(1, tabSize);

        for (int i = firstLine; i <= lastLine; i++) {
            if (isLineHidden(i)) continue;

            int indentColumns = guideIndentColumnsAt(i, unit);
            if (indentColumns < unit) continue;

            int levels = indentColumns / unit;
            int yTop = yOfBufferLine(i);

            for (int level = 0; level < levels; level++) {
                int guideX = 4 + level * unit * charWidth;
                drawGuide(g2, guideX, yTop, lineHeight, guideColor);
            }
        }
    }

    protected int guideIndentColumnsAt(int line, int unit) {
        String text = buffer.lineAt(line);
        if (!text.isBlank()) {
            return (indentColumnsOf(text) / unit) * unit;
        }
        int prev = neighborGuideIndent(line, -1, unit);
        int next = neighborGuideIndent(line, 1, unit);
        if (prev < 0 && next < 0) return 0;
        if (prev < 0) return next;
        if (next < 0) return prev;
        return Math.max(prev, next);
    }

    protected int neighborGuideIndent(int line, int step, int unit) {
        int n = buffer.lineCount();
        for (int i = line + step; i >= 0 && i < n; i += step) {
            String text = buffer.lineAt(i);
            if (text.isBlank()) continue;
            return (indentColumnsOf(text) / unit) * unit;
        }
        return -1;
    }

    protected int indentUnit() {
        if (cachedIndentUnit > 0) {
            return cachedIndentUnit;
        }

        int unit = 0;
        int n = buffer.lineCount();

        for (int i = 0; i < n; i++) {
            String text = buffer.lineAt(i);
            if (text.isBlank()) continue;

            int indent = indentColumnsOf(text);
            if (indent > 0 && (unit == 0 || indent < unit)) {
                unit = indent;
            }
        }

        cachedIndentUnit = unit > 0 ? unit : Math.max(1, tabSize);
        return cachedIndentUnit;
    }

    protected int indentColumnsOf(String text) {
        return expandTabs(getLeadingWhitespace(text), 0).length();
    }

    protected void drawGuide(Graphics2D g2, int x, int yTop, int lineHeight, Color color) {
        g2.setColor(color);
        g2.fillRect(x, yTop, 1, lineHeight);
    }

    protected boolean isDarkBackground(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 128;
    }

    protected int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public void setHoverDelay(int hoverDelay) {
        this.hoverDelay = Math.max(0, hoverDelay);
        if (hoverTimer != null) {
            hoverTimer.setInitialDelay(this.hoverDelay);
            hoverTimer.setDelay(this.hoverDelay);
        }
    }

    protected void setupHover() {
        hoverTimer = new Timer(hoverDelay, e -> fireHoverEvent());
        hoverTimer.setRepeats(false);
        hoverDocumentationHideTimer = new Timer(HOVER_DOCUMENTATION_HIDE_DELAY, e -> {
            if (hoverDocumentationPopup == null || !hoverDocumentationPopup.isMouseInside()) {
                hideHoverDocumentation();
            }
        });
        hoverDocumentationHideTimer.setRepeats(false);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateLastMousePosition(e);
                cancelHoverDocumentationHide();
                int[] pos = positionFromPoint(e.getX(), e.getY());

                if (pos[0] != hoverLine || pos[1] != hoverCol) {
                    hoverLine = pos[0];
                    hoverCol = pos[1];
                    hoverTimer.restart();
                    if (isInHoverDocumentationTransition(e.getX(), e.getY())) {
                        scheduleHoverDocumentationHide();
                    } else {
                        hideHoverDocumentation();
                    }
                }

                wordHoverLastMouseX = e.getX();
                wordHoverLastMouseY = e.getY();
                if (updateFoldPlaceholderHover(e.getX(), e.getY())) {
                    clearWordHover();
                    restoreCodeLensCursor();
                    return;
                }
                updateWordHover(e.getModifiersEx());
                updateCodeLensHover(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                updateLastMousePosition(e);
                cancelHoverDocumentationHide();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearLastMousePosition();
                hoverTimer.stop();
                hoverLine = -1;
                hoverCol = -1;
                scheduleHoverDocumentationHide();

                wordHoverLastMouseX = -1;
                wordHoverLastMouseY = -1;
                restoreFoldPlaceholderCursor();
                hideFoldPreview();
                clearWordHover();
            }
        });
    }

    protected void updateWordHover(int modifiersEx) {
        if (wordHoverLastMouseX < 0 || wordHoverLastMouseY < 0) {
            clearWordHover();
            return;
        }

        boolean active = wordClickHandler != null || wordHoverListener != null;
        if (!active) {
            clearWordHover();
            return;
        }

        int mask = wordClickModifier;
        if (mask == 0 || (modifiersEx & mask) != mask) {
            clearWordHover();
            return;
        }

        int[] pos = positionFromPoint(wordHoverLastMouseX, wordHoverLastMouseY);
        int line = pos[0];
        int col = pos[1];

        String lineText = buffer.lineAt(line);
        int probe = Math.min(col, lineText.length() - 1);
        if (probe < 0 || !wordDetector.isWordChar(lineText.charAt(probe))) {
            clearWordHover();
            return;
        }

        int startCol = probe;
        int endCol = probe;
        while (startCol > 0 && wordDetector.isWordChar(lineText.charAt(startCol - 1))) {
            startCol--;
        }
        while (endCol < lineText.length() && wordDetector.isWordChar(lineText.charAt(endCol))) {
            endCol++;
        }

        if (line == wordHoverLine && startCol == wordHoverStartCol && endCol == wordHoverEndCol) {
            return;
        }

        int lineOffset = buffer.offsetOfLine(line);
        String word = lineText.substring(startCol, endCol);
        WordClickEvent ev = new WordClickEvent(word, line, startCol,
                lineOffset + startCol, lineOffset + endCol, null);

        WordHoverStyle resolved = wordHoverDecorator != null
                ? wordHoverDecorator.decorate(ev)
                : wordHoverStyle;

        if (resolved == WordHoverStyle.DEFAULT) {
            resolved = wordHoverStyle;
        }

        if (resolved == null) {
            clearWordHover();
            return;
        }

        if (wordHoverLine >= 0 && wordHoverListener != null) {
            wordHoverListener.onExit();
        }

        wordHoverLine = line;
        wordHoverStartCol = startCol;
        wordHoverEndCol = endCol;
        wordHoverActiveStyle = resolved;

        if (wordHoverEnabled) {
            if (wordHoverPreviousCursor == null) {
                wordHoverPreviousCursor = getCursor();
            }
            Cursor hoverCursor = resolved.getCursor();
            if (hoverCursor != null) {
                setCursor(hoverCursor);
            }
        }

        if (wordHoverListener != null) {
            wordHoverListener.onEnter(ev);
        }

        repaint();
    }

    protected void clearWordHover() {
        if (wordHoverLine < 0) {
            if (wordHoverPreviousCursor != null) {
                setCursor(wordHoverPreviousCursor);
                wordHoverPreviousCursor = null;
            }
            wordHoverActiveStyle = null;
            return;
        }
        wordHoverLine = -1;
        wordHoverStartCol = -1;
        wordHoverEndCol = -1;
        wordHoverActiveStyle = null;
        if (wordHoverPreviousCursor != null) {
            setCursor(wordHoverPreviousCursor);
            wordHoverPreviousCursor = null;
        }
        if (wordHoverListener != null) {
            wordHoverListener.onExit();
        }
        repaint();
    }

    protected void fireHoverEvent() {
        if (hoverLine < 0) return;

        int offset = buffer.offsetOfLine(hoverLine) + hoverCol;

        for (HoverListener l : hoverListeners) {
            l.onHover(hoverLine, hoverCol, offset);
        }

        if (hoverDocumentationProvider != null) {
            showHoverDocumentation(hoverLine, hoverCol);
        }
    }

    protected Cursor codeLensPreviousCursor;
    protected boolean codeLensCursorActive;

    protected void updateCodeLensHover(int mx, int my) {
        if (!codeLensesEnabled || codeLensItemBounds.isEmpty()) {
            restoreCodeLensCursor();
            setToolTipText(null);
            return;
        }
        for (CodeLensItemBounds b : codeLensItemBounds) {
            if (mx < b.x || mx > b.x + b.w || my < b.y || my > b.y + b.h) continue;
            if (b.itemIndex >= b.lens.items().size()) break;
            CodeLensItem item = b.lens.items().get(b.itemIndex);
            setToolTipText(item.getTooltip());
            if (item.getOnClick() != null) {
                if (!codeLensCursorActive) {
                    codeLensPreviousCursor = getCursor();
                    codeLensCursorActive = true;
                }
                Cursor c = item.getCursor() != null
                        ? item.getCursor()
                        : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                setCursor(c);
            } else {
                restoreCodeLensCursor();
            }
            return;
        }
        restoreCodeLensCursor();
        setToolTipText(null);
    }

    protected void restoreCodeLensCursor() {
        if (codeLensCursorActive) {
            setCursor(codeLensPreviousCursor);
            codeLensCursorActive = false;
            codeLensPreviousCursor = null;
        }
    }

    protected boolean handleCodeLensClick(MouseEvent e) {
        if (!codeLensesEnabled || codeLensItemBounds.isEmpty()) {
            return false;
        }
        if (e.getButton() != MouseEvent.BUTTON1) {
            return false;
        }
        int mx = e.getX();
        int my = e.getY();
        for (CodeLensItemBounds b : codeLensItemBounds) {
            if (mx < b.x || mx > b.x + b.w || my < b.y || my > b.y + b.h) continue;
            if (b.itemIndex >= b.lens.items().size()) return false;
            CodeLensItem item = b.lens.items().get(b.itemIndex);
            if (item.getOnClick() == null) return false;
            try {
                item.getOnClick().accept(new CodeLensClickEvent(b.lens, item, b.lens.line(), e));
            } catch (Exception ignored) {
            }
            e.consume();
            return true;
        }
        return false;
    }

    protected Cursor foldPlaceholderPreviousCursor;
    protected boolean foldPlaceholderCursorActive;

    protected boolean updateFoldPlaceholderHover(int mouseX, int mouseY) {
        if (!isFoldPlaceholderAt(mouseX, mouseY)) {
            restoreFoldPlaceholderCursor();
            hideFoldPreview();
            return false;
        }
        if (!foldPlaceholderCursorActive) {
            foldPlaceholderPreviousCursor = getCursor();
            foldPlaceholderCursorActive = true;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int line = bufferLineAtY(mouseY);
        showFoldPreviewAt(line, mouseX, mouseY);
        return true;
    }

    protected void restoreFoldPlaceholderCursor() {
        if (!foldPlaceholderCursorActive) return;
        setCursor(foldPlaceholderPreviousCursor != null
                ? foldPlaceholderPreviousCursor
                : Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        foldPlaceholderCursorActive = false;
        foldPlaceholderPreviousCursor = null;
    }

    protected void showFoldPreviewAt(int line, int mouseX, int mouseY) {
        if (!foldPreviewOnHoverEnabled) {
            hideFoldPreview();
            return;
        }
        FoldRegion region = getFoldRegionStartingAt(line);
        if (region == null || !region.folded()) {
            hideFoldPreview();
            return;
        }
        if (foldPreviewLine == line && foldPreviewWindow != null && foldPreviewWindow.isVisible()) {
            return;
        }
        foldPreviewLine = line;

        int startLine = region.startLine();
        int endLine = Math.min(buffer.lineCount() - 1, region.endLine());
        int totalLines = endLine - startLine + 1;
        int shownLines = Math.min(totalLines, Math.max(1, foldPreviewMaxLines));
        int commonIndent = computeCommonIndent(startLine, endLine);

        TextStyle defaultStyle = getDefaultStyle();
        Color bg = defaultStyle != null && defaultStyle.getBackground() != null
                ? defaultStyle.getBackground()
                : UIManager.getColor("TextArea.background");
        if (bg == null) bg = Color.WHITE;
        Color fg = defaultStyle != null && defaultStyle.getForeground() != null
                ? defaultStyle.getForeground()
                : UIManager.getColor("TextArea.foreground");
        if (fg == null) fg = Color.BLACK;

        int hiddenExtra = totalLines - shownLines;
        FoldPreviewComponent preview = new FoldPreviewComponent(
                startLine, shownLines, commonIndent, hiddenExtra, bg, fg);

        Color borderColor = resolveFoldPreviewBorderColor(bg);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(bg);
        content.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        content.add(preview, BorderLayout.CENTER);

        if (foldPreviewWindow == null) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            foldPreviewWindow = new JWindow(owner);
            foldPreviewWindow.setFocusable(false);
            foldPreviewWindow.setAlwaysOnTop(true);
        }
        foldPreviewWindow.getContentPane().removeAll();
        foldPreviewWindow.getContentPane().add(content);
        foldPreviewWindow.pack();

        Point screen;
        try {
            screen = getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            return;
        }
        int px = screen.x + mouseX + 14;
        int py = screen.y + mouseY + 18;
        Dimension size = foldPreviewWindow.getSize();
        Rectangle bounds = getGraphicsConfiguration() != null
                ? getGraphicsConfiguration().getBounds()
                : new Rectangle(0, 0, Toolkit.getDefaultToolkit().getScreenSize().width,
                        Toolkit.getDefaultToolkit().getScreenSize().height);
        if (px + size.width > bounds.x + bounds.width) {
            px = Math.max(bounds.x, screen.x + mouseX - size.width - 4);
        }
        if (py + size.height > bounds.y + bounds.height) {
            py = Math.max(bounds.y, screen.y + mouseY - size.height - 4);
        }
        foldPreviewWindow.setLocation(px, py);
        foldPreviewWindow.setVisible(true);
    }

    protected void hideFoldPreview() {
        foldPreviewLine = -1;
        if (foldPreviewWindow != null && foldPreviewWindow.isVisible()) {
            foldPreviewWindow.setVisible(false);
        }
    }

    protected int computeCommonIndent(int startLine, int endLine) {
        int min = Integer.MAX_VALUE;
        for (int i = startLine; i <= endLine; i++) {
            String text = buffer.lineAt(i);
            if (text.isEmpty()) continue;
            int idx = 0;
            while (idx < text.length() && Character.isWhitespace(text.charAt(idx))) idx++;
            if (idx == text.length()) continue;
            if (idx < min) min = idx;
            if (min == 0) return 0;
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    protected Color resolveFoldPreviewBorderColor(Color bg) {
        int luma = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        int delta = luma < 128 ? 60 : -60;
        int r = Math.max(0, Math.min(255, bg.getRed() + delta));
        int g = Math.max(0, Math.min(255, bg.getGreen() + delta));
        int b = Math.max(0, Math.min(255, bg.getBlue() + delta));
        return new Color(r, g, b);
    }

    protected class FoldPreviewComponent extends JComponent {
        private final int startLine;
        private final int shownLines;
        private final int commonIndent;
        private final int hiddenExtra;
        private final Color previewBg;
        private final Color previewFg;
        private final int padX = 8;
        private final int padY = 6;

        FoldPreviewComponent(int startLine, int shownLines, int commonIndent,
                              int hiddenExtra, Color bg, Color fg) {
            this.startLine = startLine;
            this.shownLines = shownLines;
            this.commonIndent = commonIndent;
            this.hiddenExtra = hiddenExtra;
            this.previewBg = bg;
            this.previewFg = fg;
            setFont(CodeEditorTextArea.this.getFont());
            setOpaque(true);
            setBackground(bg);
            setForeground(fg);
        }

        private String prepareLine(int absoluteLine) {
            String text = buffer.lineAt(absoluteLine);
            if (commonIndent > 0 && text.length() >= commonIndent) {
                String head = text.substring(0, commonIndent);
                if (head.trim().isEmpty()) {
                    text = text.substring(commonIndent);
                }
            }
            if (text.length() > foldPreviewMaxColumns) {
                text = text.substring(0, foldPreviewMaxColumns) + " …";
            }
            return text;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int lineHeight = fm.getHeight();
            int rows = shownLines + (hiddenExtra > 0 ? 1 : 0);
            int maxWidth = 0;
            for (int i = 0; i < shownLines; i++) {
                String text = prepareLine(startLine + i);
                maxWidth = Math.max(maxWidth, fm.stringWidth(text));
            }
            if (hiddenExtra > 0) {
                maxWidth = Math.max(maxWidth, fm.stringWidth("… (+" + hiddenExtra + " more lines)"));
            }
            int width = maxWidth + padX * 2;
            int height = rows * lineHeight + padY * 2;
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(previewBg);
                g2.fillRect(0, 0, getWidth(), getHeight());

                Font baseFont = getFont();
                FontMetrics fm = g2.getFontMetrics(baseFont);
                int lineHeight = fm.getHeight();
                int ascent = fm.getAscent();

                for (int i = 0; i < shownLines; i++) {
                    int absoluteLine = startLine + i;
                    int y = padY + i * lineHeight;
                    drawStyledLine(g2, baseFont, absoluteLine, padX, y + ascent, lineHeight);
                }

                if (hiddenExtra > 0) {
                    int y = padY + shownLines * lineHeight;
                    Font italic = baseFont.deriveFont(Font.ITALIC);
                    g2.setFont(italic);
                    Color disabled = UIManager.getColor("Label.disabledForeground");
                    g2.setColor(disabled != null ? disabled : new Color(previewFg.getRed(),
                            previewFg.getGreen(), previewFg.getBlue(), 140));
                    g2.drawString("… (+" + hiddenExtra + " more lines)", padX, y + ascent);
                }
            } finally {
                g2.dispose();
            }
        }

        private void drawStyledLine(Graphics2D g2, Font baseFont, int absoluteLine,
                                     int xStart, int baseline, int lineHeight) {
            String originalLine = buffer.lineAt(absoluteLine);
            int lineOffset = buffer.offsetOfLine(absoluteLine);
            int skip = 0;
            if (commonIndent > 0 && originalLine.length() >= commonIndent) {
                String head = originalLine.substring(0, commonIndent);
                if (head.trim().isEmpty()) {
                    skip = commonIndent;
                }
            }
            int maxLen = Math.min(originalLine.length(), skip + foldPreviewMaxColumns);
            boolean truncated = originalLine.length() > skip + foldPreviewMaxColumns;

            LineColorInfo lineColor = getLineColor(absoluteLine);
            if (lineColor != null && lineColor.getBackgroundColor() != null) {
                g2.setColor(lineColor.getBackgroundColor());
                g2.fillRect(0, baseline - g2.getFontMetrics(baseFont).getAscent(),
                        getWidth(), lineHeight);
            }
            Color lineFg = lineColor != null ? lineColor.getForegroundColor() : null;

            float x = xStart;
            int col = skip;
            while (col < maxLen) {
                TextStyle style = getStyleAt(lineOffset + col);
                int runEnd = col + 1;
                while (runEnd < maxLen && getStyleAt(lineOffset + runEnd) == style) {
                    runEnd++;
                }
                String run = originalLine.substring(col, runEnd);
                Font runFont = deriveStyledFont(baseFont, style);
                g2.setFont(runFont);
                FontMetrics rfm = g2.getFontMetrics(runFont);

                Color fg = lineFg != null ? lineFg
                        : (style != null && style.getForeground() != null
                                ? style.getForeground() : previewFg);
                g2.setColor(fg);
                g2.drawString(run, x, baseline);
                if (style != null && style.isUnderline()) {
                    int uy = baseline + 1;
                    g2.drawLine((int) x, uy, (int) (x + rfm.stringWidth(run)), uy);
                }
                x += rfm.stringWidth(run);
                col = runEnd;
            }
            if (truncated) {
                g2.setFont(baseFont);
                Color disabled = UIManager.getColor("Label.disabledForeground");
                g2.setColor(disabled != null ? disabled : previewFg);
                g2.drawString(" …", x, baseline);
            }
        }

        private Font deriveStyledFont(Font base, TextStyle style) {
            if (style == null) return base;
            int s = Font.PLAIN;
            if (style.isBold()) s |= Font.BOLD;
            if (style.isItalic()) s |= Font.ITALIC;
            return s == Font.PLAIN ? base : base.deriveFont(s);
        }
    }

    protected boolean isFoldPlaceholderAt(int mouseX, int mouseY) {
        if (!foldingEnabled) return false;
        if (isInCodeLensRow(mouseY)) return false;
        FontMetrics fm = getFontMetrics(getFont());
        int line = bufferLineAtY(mouseY);
        if (!isFoldAnchor(line)) return false;
        String lineText = buffer.lineAt(line);
        String pillText = (foldPlaceholder == null || foldPlaceholder.trim().isEmpty())
                ? "\u2026" : foldPlaceholder.trim();
        if (foldPlaceholderWithSeparators) {
            String[] sep = findFoldSeparatorsForRegion(getFoldRegionStartingAt(line));
            if (sep != null) pillText = sep[0] + pillText + sep[1];
        }
        String renderedLineText = lineText;
        if (shouldHideTrailingOpenForFold(line)) {
            int idx = lineText.length() - 1;
            while (idx >= 0 && Character.isWhitespace(lineText.charAt(idx))) idx--;
            if (idx >= 0) renderedLineText = lineText.substring(0, idx);
        }
        int hPad = 6;
        int lineEndX = 4 + textWidth(fm, renderedLineText, 0);
        int pillStart = lineEndX + 6;
        int pillEnd = pillStart + fm.stringWidth(pillText) + hPad * 2;
        return mouseX >= pillStart && mouseX <= pillEnd;
    }

    protected boolean handleFoldPlaceholderClick(int mouseX, int mouseY) {
        if (!isFoldPlaceholderAt(mouseX, mouseY)) return false;
        int line = bufferLineAtY(mouseY);
        hideFoldPreview();
        toggleFold(line);
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
        return true;
    }

    protected class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            updateLastMousePosition(e);
            requestFocusInWindow();
            hideAutoCompletePopup();
            hideSignatureHelp();
            hideHoverDocumentation();
            clearGhostText();
            if (handlePopupTrigger(e)) return;
            if (isPopupButton(e) && contextMenuEnabled && pointInsideSelection(e.getX(), e.getY())) {
                return;
            }
            if (handleCodeLensClick(e)) return;
            if (handleFoldPlaceholderClick(e.getX(), e.getY())) return;
            int[] pos = positionFromPoint(e.getX(), e.getY());
            rememberInlayInteraction(pos[0], pos[1]);
            boolean multiAdd = multiCaretEnabled && e.isAltDown() && (e.isControlDown() || e.isMetaDown());
            if (multiAdd) {
                addExtraCaret(pos[0], pos[1]);
                scrollToCaret();
                resetCaretBlink();
                return;
            }
            if (handleWordClick(e, pos[0], pos[1])) {
                return;
            }
            if (!e.isShiftDown()) clearExtraCarets();
            if (e.isShiftDown()) {
                startSelectionIfNeeded();
                caretLine = pos[0];
                caretCol = pos[1];
                desiredCaretCol = -1;
            } else {
                clearSelection();
                caretLine = pos[0];
                caretCol = pos[1];
                desiredCaretCol = -1;
            }
            scrollToCaret();
            resetCaretBlink();
            fireStateChangedIfNeeded();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateLastMousePosition(e);
            int[] pos = positionFromPoint(e.getX(), e.getY());
            rememberInlayInteraction(pos[0], pos[1]);
            startSelectionIfNeeded();
            caretLine = pos[0];
            caretCol = pos[1];
            desiredCaretCol = -1;
            scrollToCaret();
            resetCaretBlink();
            repaint();
            fireStateChangedIfNeeded();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateLastMousePosition(e);
            handlePopupTrigger(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            updateLastMousePosition(e);
            if (e.getClickCount() == 2) {
                String lineText = buffer.lineAt(caretLine);
                if (caretCol >= lineText.length()) return;
                int cls = charClass(lineText.charAt(caretCol));
                if (cls == 0) return;
                int start = caretCol;
                int end = caretCol;
                while (start > 0 && charClass(lineText.charAt(start - 1)) == cls) start--;
                while (end < lineText.length() && charClass(lineText.charAt(end)) == cls) end++;
                if (start != end) {
                    selectionStartLine = caretLine;
                    selectionStartCol = start;
                    caretCol = end;
                    fireStateChangedIfNeeded();
                    repaint();
                }
            }
        }
    }

    protected void updateLastMousePosition(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    protected void clearLastMousePosition() {
        lastMouseX = -1;
        lastMouseY = -1;
    }

    protected boolean isPopupButton(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON3
                || (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0;
    }

    protected boolean pointInsideSelection(int x, int y) {
        if (!hasSelection()) return false;
        int[] pos = positionFromPoint(x, y);
        int offset = buffer.offsetOfLine(pos[0]) + pos[1];
        return offset >= getSelectionStart() && offset <= getSelectionEnd();
    }

    protected boolean handlePopupTrigger(MouseEvent e) {
        if (!e.isPopupTrigger()) return false;
        if (!contextMenuEnabled) return false;
        if (contextMenuProvider == null) {
            JPopupMenu menu = createDefaultContextMenu();
            if (menu == null) return false;
            menu.show(this, e.getX(), e.getY());
            return true;
        }
        ContextMenuProvider provider = contextMenuProvider;
        int x = e.getX();
        int y = e.getY();
        getProviderExecutor().submit(() -> {
            JPopupMenu menu;
            try {
                menu = provider.getPopupMenu(e);
            } catch (Exception ignored) {
                menu = null;
            }
            final JPopupMenu popup = menu;
            if (popup != null) {
                SwingUtilities.invokeLater(() -> popup.show(this, x, y));
            }
        });
        return true;
    }

    public JPopupMenu createDefaultContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        boolean hasSel = hasSelection();
        boolean hasClipboardText = clipboardHasText();
        boolean canPaste = copyPasteEnabled && !readOnly && hasClipboardText;
        boolean canCopy = copyPasteEnabled && hasSel;
        boolean canCut = canCopy && !readOnly;

        JMenuItem undo = new JMenuItem(text("menu.undo", "Desfazer"));
        undo.setEnabled(!readOnly && buffer.canUndo());
        undo.addActionListener(ev -> {
            if (readOnly) return;
            performUndo();
            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
        });
        menu.add(undo);

        JMenuItem redo = new JMenuItem(text("menu.redo", "Refazer"));
        redo.setEnabled(!readOnly && buffer.canRedo());
        redo.addActionListener(ev -> {
            if (readOnly) return;
            performRedo();
            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
        });
        menu.add(redo);

        menu.addSeparator();

        JMenuItem cut = new JMenuItem(text("menu.cut", "Recortar"));
        cut.setEnabled(canCut);
        cut.addActionListener(ev -> {
            if (readOnly) return;
            copyToClipboard();
            deleteSelectionsAtCarets();
            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
        });
        menu.add(cut);

        JMenuItem copy = new JMenuItem(text("menu.copy", "Copiar"));
        copy.setEnabled(canCopy);
        copy.addActionListener(ev -> copyToClipboard());
        menu.add(copy);

        JMenuItem paste = new JMenuItem(text("menu.paste", "Colar"));
        paste.setEnabled(canPaste);
        paste.addActionListener(ev -> {
            pasteFromClipboard();
            scrollToCaret();
            resetCaretBlink();
            revalidate();
            repaint();
        });
        menu.add(paste);

        menu.addSeparator();

        JMenuItem selectAll = new JMenuItem(text("menu.selectAll", "Selecionar tudo"));
        selectAll.addActionListener(ev -> {
            selectAll();
            repaint();
        });
        menu.add(selectAll);

        return menu;
    }

    protected boolean clipboardHasText() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard()
                    .isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return false;
        }
    }

    protected void fireWordCaretChangeEvent(int line, int col) {
        if (wordCaretChangeListeners.isEmpty()) return;

        WordCaretChangeEvent event = createWordCaretChangeEvent(line, col);
        if (event == null) {
            return;
        }
        List<WordCaretChangeListener> listeners = List.copyOf(wordCaretChangeListeners);
        getWordCaretEventExecutor().submit(() -> {
            for (WordCaretChangeListener listener : listeners) {
                try {
                    listener.onWordCaretChanged(event);
                } catch (Exception ignored) {}
            }
        });
    }

    protected WordCaretChangeEvent createWordCaretChangeEvent(int line, int col) {
        if (line < 0 || line >= buffer.lineCount()) {
            return null;
        }
        WordSpan span = findWordSpan(line, col);
        if (span == null) {
            int safeCol = Math.max(0, Math.min(col, buffer.lineAt(line).length()));
            int caretOffset = buffer.offsetOfLine(line) + safeCol;
            return new WordCaretChangeEvent(
                    "",
                    line,
                    safeCol,
                    safeCol,
                    safeCol,
                    caretOffset,
                    caretOffset,
                    caretOffset,
                    lastMouseX,
                    lastMouseY
            );
        }
        return new WordCaretChangeEvent(
                span.word(),
                line,
                col,
                span.startCol(),
                span.endCol(),
                buffer.offsetOfLine(line) + col,
                span.startOffset(),
                span.endOffset(),
                lastMouseX,
                lastMouseY
        );
    }

    protected record WordSpan(String word, int startCol, int endCol, int startOffset, int endOffset) {
    }

    protected WordSpan findWordSpan(int line, int col) {
        if (line < 0 || line >= buffer.lineCount()) {
            return null;
        }

        String lineText = buffer.lineAt(line);
        int lineOffset = buffer.offsetOfLine(line);
        int length = lineText.length();

        int probe = -1;
        if (col >= 0 && col < length && wordDetector.isWordChar(lineText.charAt(col))) {
            probe = col;
        } else if (col > 0 && col - 1 < length && wordDetector.isWordChar(lineText.charAt(col - 1))) {
            probe = col - 1;
        }
        if (probe < 0) {
            return null;
        }

        int startCol = probe;
        int endCol = probe;
        while (startCol > 0 && wordDetector.isWordChar(lineText.charAt(startCol - 1))) {
            startCol--;
        }
        while (endCol < lineText.length() && wordDetector.isWordChar(lineText.charAt(endCol))) {
            endCol++;
        }

        String word = lineText.substring(startCol, endCol);
        int startOffset = lineOffset + startCol;
        int endOffset = lineOffset + endCol;

        return new WordSpan(word, startCol, endCol, startOffset, endOffset);
    }

    protected boolean handleWordClick(MouseEvent e, int line, int col) {
        if (wordClickHandler == null) {
            return false;
        }
        int mask = wordClickModifier;
        if (mask == 0) {
            return false;
        }
        if ((e.getModifiersEx() & mask) != mask) {
            return false;
        }
        if (e.getButton() != MouseEvent.BUTTON1) {
            return false;
        }

        String lineText = buffer.lineAt(line);
        int lineOffset = buffer.offsetOfLine(line);

        int probe = Math.min(col, lineText.length() - 1);
        if (probe < 0 || !wordDetector.isWordChar(lineText.charAt(probe))) {
            return false;
        }

        int startCol = probe;
        int endCol = probe;
        while (startCol > 0 && wordDetector.isWordChar(lineText.charAt(startCol - 1))) {
            startCol--;
        }
        while (endCol < lineText.length() && wordDetector.isWordChar(lineText.charAt(endCol))) {
            endCol++;
        }

        String word = lineText.substring(startCol, endCol);
        int startOffset = lineOffset + startCol;
        int endOffset = lineOffset + endCol;

        WordClickEvent event = new WordClickEvent(word, line, startCol,
                startOffset, endOffset, e);
        wordClickHandler.onWordClick(event);
        e.consume();
        return true;
    }

    protected class FocusHandler extends FocusAdapter {
        @Override
        public void focusGained(FocusEvent e) {
            caretVisible = true;
            caretTimer.restart();
            scheduleGhostIdleTimer();
            repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            caretVisible = false;
            caretTimer.stop();
            stopGhostIdleTimer();
            repaint();
        }
    }

    public void formatDocument() {
        if (readOnly) return;
        if (codeFormatter == null) return;
        int caretOff = caretOffset();
        String sourceText = buffer.getText();
        CodeFormatter formatter = codeFormatter;
        FormatContext ctx = new FormatContext(new TextBuffer(sourceText), 0, sourceText.length(),
                tabSize, useSpacesForTab, FormatContext.Scope.DOCUMENT);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return formatter.format(ctx);
                    } catch (Exception ignored) {
                        return null;
                    }
                }, getProviderExecutor())
                .thenAccept(formatted -> SwingUtilities.invokeLater(() ->
                        applyFormattedDocument(sourceText, caretOff, formatted)));
    }

    protected void applyFormattedDocument(String sourceText, int caretOff, String formatted) {
        if (formatted == null) return;
        formatted = formatted.replace("\r\n", "\n").replace("\r", "\n");
        if (!buffer.getText().equals(sourceText)) return;
        if (formatted.equals(buffer.getText())) return;
        beginCompoundEdit();
        try {
            deleteText(0, buffer.length());
            insertText(0, formatted);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(Math.min(caretOff, buffer.length()));
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void formatSelection() {
        if (readOnly) return;
        if (codeFormatter == null || !hasSelection()) return;
        int start = getSelectionStart();
        int end = getSelectionEnd();
        String sourceText = buffer.getText();
        CodeFormatter formatter = codeFormatter;
        FormatContext ctx = new FormatContext(new TextBuffer(sourceText), start, end,
                tabSize, useSpacesForTab, FormatContext.Scope.SELECTION);
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return formatter.format(ctx);
                    } catch (Exception ignored) {
                        return null;
                    }
                }, getProviderExecutor())
                .thenAccept(formatted -> SwingUtilities.invokeLater(() ->
                        applyFormattedSelection(sourceText, start, end, formatted)));
    }

    protected void applyFormattedSelection(String sourceText, int start, int end, String formatted) {
        if (formatted == null) return;
        formatted = formatted.replace("\r\n", "\n").replace("\r", "\n");
        if (!buffer.getText().equals(sourceText)) return;
        beginCompoundEdit();
        try {
            deleteText(start, end);
            insertText(start, formatted);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(start + formatted.length());
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void format(){
        if(hasSelection()){
            formatSelection();
            return;
        }
        formatDocument();
    }

    public void refreshDiagnostics() {
        if (diagnosticsDebounceTimer != null) diagnosticsDebounceTimer.stop();
        refreshDiagnosticsAsync();
    }

    public void addDiagnosticsChangeListener(DiagnosticsChangeListener listener) {
        if (listener != null) diagnosticsChangeListeners.add(listener);
    }

    public void removeDiagnosticsChangeListener(DiagnosticsChangeListener listener) {
        if (listener != null) diagnosticsChangeListeners.remove(listener);
    }

    protected void fireDiagnosticsChanged() {
        if (diagnosticsChangeListeners.isEmpty()) return;
        List<Diagnostic> snapshot = List.copyOf(diagnostics);
        for (DiagnosticsChangeListener listener : diagnosticsChangeListeners) {
            listener.onDiagnosticsChanged(snapshot);
        }
    }

    protected void scheduleDiagnosticsRefresh() {
        if (diagnosticsDebounceMs <= 0) {
            refreshDiagnosticsAsync();
            return;
        }
        if (diagnosticsDebounceTimer == null) {
            diagnosticsDebounceTimer = new Timer(diagnosticsDebounceMs, e -> refreshDiagnosticsAsync());
            diagnosticsDebounceTimer.setRepeats(false);
        } else {
            diagnosticsDebounceTimer.setInitialDelay(diagnosticsDebounceMs);
            diagnosticsDebounceTimer.setDelay(diagnosticsDebounceMs);
        }
        diagnosticsDebounceTimer.restart();
    }

    public void refreshDiagnosticsAsync() {
        if (diagnosticsProvider == null) {
            diagnostics.clear();
            lastDiagnostics = null;
            lastDiagnosticsText = null;
            pendingDiagnosticsEdit = null;
            fireDiagnosticsChanged();
            repaint();
            return;
        }

        int version = diagnosticsVersion.incrementAndGet();

        if (currentDiagnosticsTask != null && !currentDiagnosticsTask.isDone()) {
            currentDiagnosticsTask.cancel(true);
        }

        final DiagnosticsProvider provider = diagnosticsProvider;
        final String textSnapshot = buffer.getText();
        final TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        final PendingHighlightEdit edit = pendingDiagnosticsEdit;
        pendingDiagnosticsEdit = null;
        final List<Diagnostic> prevDiagnostics = lastDiagnostics;
        final String prevText = lastDiagnosticsText;

        currentDiagnosticsTask = getDiagnosticsExecutor().submit(() -> {
            try {
                List<Diagnostic> list;
                if (provider.supportsIncremental()
                        && isConsistentEdit(edit, prevText, textSnapshot)
                        && prevDiagnostics != null) {
                    DiagnosticsChange change = new DiagnosticsChange(
                            bufferSnapshot, prevText, textSnapshot,
                            edit.offset(), edit.removedLength(), edit.insertedText(),
                            prevDiagnostics);
                    list = provider.getDiagnostics(change);
                } else {
                    list = provider.getDiagnostics(new DiagnosticsContext(bufferSnapshot));
                }

                if (version != diagnosticsVersion.get()) return;

                final List<Diagnostic> snapshot = list != null
                        ? List.copyOf(list)
                        : List.of();
                lastDiagnostics = snapshot;
                lastDiagnosticsText = textSnapshot;

                SwingUtilities.invokeLater(() -> {
                    if (version != diagnosticsVersion.get()) return;

                    diagnostics.clear();
                    diagnostics.addAll(snapshot);
                    fireDiagnosticsChanged();
                    repaint();
                });

            } catch (Exception ignored) {}
        });
    }

    public void refreshCodeLenses() {
        refreshCodeLensesAsync();
    }

    public void refreshCodeLensesAsync() {
        if (codeLensProvider == null) {
            codeLenses.clear();
            revalidate();
            repaint();
            return;
        }

        int version = codeLensVersion.incrementAndGet();

        if (currentCodeLensTask != null && !currentCodeLensTask.isDone()) {
            currentCodeLensTask.cancel(true);
        }

        final CodeLensProvider provider = codeLensProvider;
        final String textSnapshot = buffer.getText();
        final CodeLensContext ctx = new CodeLensContext(new TextBuffer(textSnapshot));

        currentCodeLensTask = getCodeLensExecutor().submit(() -> {
            try {
                List<CodeLens> list = provider.getCodeLenses(ctx);

                if (version != codeLensVersion.get()) return;

                SwingUtilities.invokeLater(() -> {
                    if (version != codeLensVersion.get()) return;

                    codeLenses.clear();
                    if (list != null) codeLenses.addAll(list);
                    invalidateGeometry();
                    revalidate();
                    repaint();
                });

            } catch (Exception ignored) {}
        });
    }

    public void applySyntaxHighlight() {
        if (!syntaxHighlightEnabled) return;
        if (tokenizerProvider == null || tokenClassifierProvider == null
                || tokenColorProvider == null || tokenRenderProvider == null) {
            return;
        }
        final int version = highlightVersion.incrementAndGet();
        final String textSnapshot = buffer.getText();
        final TokenizerCodeEditorProvider tokenizer = tokenizerProvider;
        final TokenClassifierCodeEditorProvider classifier = tokenClassifierProvider;
        final TokenColorProvider colorProvider = tokenColorProvider;
        final TokenRenderCodeEditorProvider renderer = tokenRenderProvider;

        final PendingHighlightEdit edit = pendingHighlightEdit;
        pendingHighlightEdit = null;
        final Collection<Token> prevTokens = lastHighlightTokens;
        final String prevText = lastHighlightText;

        if (currentHighlightTask != null && !currentHighlightTask.isDone()) {
            currentHighlightTask.cancel(true);
        }
        currentHighlightTask = getHighlightExecutor().submit(() -> {
            try {
                Collection<Token> tokens;
                if (tokenizer.supportsIncremental()
                        && isConsistentEdit(edit, prevText, textSnapshot)
                        && prevTokens != null) {
                    TokenizeChange change = new TokenizeChange(
                            prevText, textSnapshot,
                            edit.offset(), edit.removedLength(), edit.insertedText(),
                            prevTokens);
                    tokens = tokenizer.tokenize(change, classifier);
                } else {
                    tokens = tokenizer.tokenize(textSnapshot, classifier);
                }
                if (tokens == null) tokens = Collections.emptyList();
                final Collection<Token> snapshot = List.copyOf(tokens);
                SwingUtilities.invokeLater(() -> {
                    if (version != highlightVersion.get()) return;
                    if (!buffer.getText().equals(textSnapshot)) return;
                    lastHighlightTokens = snapshot;
                    lastHighlightText = textSnapshot;
                    renderer.render(snapshot, colorProvider, CodeEditorTextArea.this);
                });
            } catch (Exception ignored) {
            }
        });
    }

    private static boolean isConsistentEdit(PendingHighlightEdit edit, String prevText, String newText) {
        if (edit == null || prevText == null) return false;
        int offset = edit.offset();
        int removedEnd = offset + edit.removedLength();
        if (offset < 0 || removedEnd > prevText.length()) return false;
        String reconstructed = prevText.substring(0, offset)
                + edit.insertedText()
                + prevText.substring(removedEnd);
        return reconstructed.equals(newText);
    }

    public void addProvider(CodeEditorProvider provider) {
        if (provider == null) return;
        if (provider instanceof TokenizerCodeEditorProvider p) setTokenizerProvider(p);
        if (provider instanceof TokenClassifierCodeEditorProvider p) setTokenClassifierProvider(p);
        if (provider instanceof TokenColorProvider p) setTokenColorProvider(p);
        if (provider instanceof TokenRenderCodeEditorProvider p) setTokenRenderProvider(p);
        if (provider instanceof AutoCompleteProvider p) setAutoCompleteProvider(p);
        if (provider instanceof CodeFormatter p) setCodeFormatter(p);
        if (provider instanceof DiagnosticsProvider p) setDiagnosticsProvider(p);
        if (provider instanceof DefinitionProvider p) setDefinitionProvider(p);
        if (provider instanceof InlayHintProvider p) {
            setInlayHintProvider(p);
            if (inlayHintsEnabled) refreshInlayHints();
        }
        if (provider instanceof CodeLensProvider p) {
            setCodeLensProvider(p);
            if (codeLensesAutoRunEnabled) refreshCodeLensesAsync();
        }
        if (provider instanceof HoverDocumentationProvider p) setHoverDocumentationProvider(p);
        if (provider instanceof SignatureHelpProvider p) setSignatureHelpProvider(p);
        if (provider instanceof DefinitionLocationProvider p) setDefinitionLocationProvider(p);
        if (provider instanceof ReferencesProvider p) setReferencesProvider(p);
        if (provider instanceof DocumentSymbolProvider p) {
            setDocumentSymbolProvider(p);
            refreshDocumentSymbolsAsync();
        }
        if (provider instanceof RenameProvider p) setRenameProvider(p);
        if (provider instanceof CodeActionProvider p) setCodeActionProvider(p);
        if (provider instanceof SelectionRangeProvider p) setSelectionRangeProvider(p);
        if (provider instanceof CommentProvider p) setCommentProvider(p);
        if (provider instanceof ContextMenuProvider p) setContextMenuProvider(p);
        if (provider instanceof BracketMatcher p) setBracketMatcher(p);
        if (provider instanceof WordDetector p) setWordDetector(p);
        if (provider instanceof GhostTextProvider p) setGhostTextProvider(p);
    }

    public void refreshInlayHints() {
        int version = inlayHintVersion.incrementAndGet();
        if (currentInlayHintTask != null && !currentInlayHintTask.isDone()) {
            currentInlayHintTask.cancel(true);
        }
        if (inlayHintProvider == null) {
            inlayHints.clear();
            invalidateGeometry();
            revalidate();
            repaint();
            return;
        }

        final InlayHintProvider provider = inlayHintProvider;
        final String textSnapshot = buffer.getText();
        final TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        final InlayHintContext ctx = new InlayHintContext(
                bufferSnapshot,
                0,
                Math.max(0, bufferSnapshot.lineCount() - 1));

        currentInlayHintTask = getProviderExecutor().submit(() -> {
            try {
                List<InlayHint> list = provider.getInlayHints(ctx);
                final List<InlayHint> snapshot = list != null ? List.copyOf(list) : List.of();
                SwingUtilities.invokeLater(() -> {
                    if (version != inlayHintVersion.get()) return;
                    if (!buffer.getText().equals(textSnapshot)) return;
                    inlayHints.clear();
                    inlayHints.addAll(snapshot);
                    invalidateGeometry();
                    revalidate();
                    repaint();
                });
            } catch (Exception ignored) {
            }
        });
    }

    protected HoverDocumentationPopup createHoverDocumentationPopup() {
        HoverDocumentationPopup popup = new HoverDocumentationPopup(this);
        configureHoverDocumentationPopup(popup);
        return popup;
    }

    protected HoverDocumentationPopup getOrCreateHoverDocumentationPopup() {
        if (hoverDocumentationPopup == null) {
            hoverDocumentationPopup = createHoverDocumentationPopup();
        }
        return hoverDocumentationPopup;
    }

    public void setHoverDocumentationPopup(HoverDocumentationPopup popup) {
        if (this.hoverDocumentationPopup != null) this.hoverDocumentationPopup.hide();
        this.hoverDocumentationPopup = popup;
        configureHoverDocumentationPopup(this.hoverDocumentationPopup);
    }

    public boolean isHoverDocumentationTextSelectionEnabled() {
        return hoverDocumentationTextSelectionEnabled;
    }

    public void setHoverDocumentationTextSelectionEnabled(boolean enabled) {
        hoverDocumentationTextSelectionEnabled = enabled;
        if (hoverDocumentationPopup != null) {
            hoverDocumentationPopup.setTextSelectionEnabled(enabled);
        }
    }

    protected void showHoverDocumentation(int line, int col) {
        if (hoverDocumentationProvider == null) return;
        int version = hoverDocumentationVersion.incrementAndGet();
        String textSnapshot = buffer.getText();
        TextBuffer bufferSnapshot = new TextBuffer(textSnapshot);
        int safeLine = Math.max(0, Math.min(line, bufferSnapshot.lineCount() - 1));
        int offset = bufferSnapshot.offsetOfLine(safeLine) + Math.min(col, bufferSnapshot.lineAt(safeLine).length());
        HoverDocumentationContext ctx = new HoverDocumentationContext(bufferSnapshot, safeLine, col, offset);
        HoverDocumentationProvider provider = hoverDocumentationProvider;
        getProviderExecutor().submit(() -> {
            HoverInfo info;
            try {
                info = provider.provideHover(ctx);
            } catch (Exception ignored) {
                info = null;
            }
            final HoverInfo hoverInfo = info;
            SwingUtilities.invokeLater(() -> showHoverDocumentationResult(
                    version, textSnapshot, safeLine, col, hoverInfo));
        });
    }

    protected void showHoverDocumentationResult(int version, String textSnapshot, int line, int col, HoverInfo info) {
        if (version != hoverDocumentationVersion.get()) return;
        if (!buffer.getText().equals(textSnapshot)) return;
        if (info == null) {
            hideHoverDocumentation();
            return;
        }
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight();
        String lineText = buffer.lineAt(line);
        int safeCol = Math.min(col, lineText.length());
        int x = visualXForColumn(line, lineText, safeCol, fm);
        int y = yOfBufferLine(line) + lineHeight;
        HoverDocumentationPopup popup = getOrCreateHoverDocumentationPopup();
        popup.show(info, x, y);
        updateHoverDocumentationTransitionBounds(x, y - lineHeight, lineHeight, popup);
    }

    public void hideHoverDocumentation() {
        cancelHoverDocumentationHide();
        hoverDocumentationTransitionBounds = null;
        if (hoverDocumentationPopup != null) hoverDocumentationPopup.hide();
    }

    protected void suppressHoverWhileEditing() {
        if (hoverTimer != null) hoverTimer.stop();
        hoverDocumentationVersion.incrementAndGet();
        hoverLine = -1;
        hoverCol = -1;
        hideHoverDocumentation();
    }

    protected void configureHoverDocumentationPopup(HoverDocumentationPopup popup) {
        if (popup != null) {
            popup.setMouseExitHandler(this::scheduleHoverDocumentationHide);
            popup.setTextSelectionEnabled(hoverDocumentationTextSelectionEnabled);
        }
    }

    protected void scheduleHoverDocumentationHide() {
        if (hoverDocumentationHideTimer == null) {
            hideHoverDocumentation();
            return;
        }
        hoverDocumentationHideTimer.restart();
    }

    protected void cancelHoverDocumentationHide() {
        if (hoverDocumentationHideTimer != null && hoverDocumentationHideTimer.isRunning()) {
            hoverDocumentationHideTimer.stop();
        }
    }

    protected void updateHoverDocumentationTransitionBounds(
            int anchorX,
            int anchorY,
            int lineHeight,
            HoverDocumentationPopup popup
    ) {
        Rectangle popupBounds = popup != null ? popup.getBoundsInOwner() : null;
        if (popupBounds == null) {
            hoverDocumentationTransitionBounds = null;
            return;
        }
        Rectangle anchorBounds = new Rectangle(anchorX, anchorY, 1, Math.max(1, lineHeight));
        hoverDocumentationTransitionBounds = popupBounds.union(anchorBounds);
        hoverDocumentationTransitionBounds.grow(HOVER_DOCUMENTATION_REACH_PADDING, HOVER_DOCUMENTATION_REACH_PADDING);
    }

    protected boolean isInHoverDocumentationTransition(int x, int y) {
        return hoverDocumentationPopup != null
                && hoverDocumentationPopup.isVisible()
                && hoverDocumentationTransitionBounds != null
                && hoverDocumentationTransitionBounds.contains(x, y);
    }

    protected SignatureHelpPopup createSignatureHelpPopup() {
        return new SignatureHelpPopup(this);
    }

    protected SignatureHelpPopup getOrCreateSignatureHelpPopup() {
        if (signatureHelpPopup == null) {
            signatureHelpPopup = createSignatureHelpPopup();
        }
        return signatureHelpPopup;
    }

    public void setSignatureHelpPopup(SignatureHelpPopup popup) {
        if (this.signatureHelpPopup != null) this.signatureHelpPopup.hide();
        this.signatureHelpPopup = popup;
    }

    public boolean isSignatureHelpVisible() {
        return signatureHelpPopup != null && signatureHelpPopup.isVisible();
    }

    public void triggerSignatureHelp() {
        triggerSignatureHelp('\0');
    }

    protected void triggerSignatureHelp(char triggerChar) {
        SignatureHelpContext.TriggerKind kind = triggerChar == '\0'
                ? SignatureHelpContext.TriggerKind.INVOKED
                : SignatureHelpContext.TriggerKind.TRIGGER_CHARACTER;
        requestSignatureHelp(kind, triggerChar);
    }

    protected void refreshSignatureHelpIfVisible() {
        if (!isSignatureHelpVisible() || signatureHelpProvider == null) return;
        requestSignatureHelp(SignatureHelpContext.TriggerKind.CONTENT_CHANGE, '\0');
    }

    protected void handleSignatureHelpAfterTyping(char c) {
        if (signatureHelpProvider == null) return;
        if (isSignatureHelpTriggerOrRetrigger(c)) {
            triggerSignatureHelp(c);
        } else if (isSignatureHelpVisible()) {
            refreshSignatureHelpIfVisible();
        }
    }

    protected boolean isSignatureHelpTriggerOrRetrigger(char c) {
        SignatureHelpProvider provider = signatureHelpProvider;
        if (provider == null) return false;
        try {
            Set<Character> triggers = provider.getTriggerCharacters();
            if (triggers != null && triggers.contains(c)) return true;
            Set<Character> retriggers = provider.getRetriggerCharacters();
            return retriggers != null && retriggers.contains(c);
        } catch (Exception ignored) {
            return false;
        }
    }

    protected void requestSignatureHelp(SignatureHelpContext.TriggerKind kind, char triggerChar) {
        SignatureHelpProvider provider = signatureHelpProvider;
        if (provider == null) return;

        int version = signatureHelpVersion.incrementAndGet();
        int caretOff = caretOffset();
        int line = caretLine;
        int col = caretCol;
        boolean retrigger = isSignatureHelpVisible();
        SignatureHelp active = signatureHelpPopup != null ? signatureHelpPopup.getSignatureHelp() : null;
        TextBuffer snapshot = new TextBuffer(buffer.getText());
        SignatureHelpContext ctx = new SignatureHelpContext(
                snapshot, caretOff, line, col, kind, triggerChar, retrigger, active);

        ExecutorService executor = getProviderExecutor();
        executor.submit(() -> {
            CompletableFuture<SignatureHelp> task;
            try {
                task = provider.provideSignatureHelpAsync(ctx, executor);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(this::hideSignatureHelp);
                return;
            }
            if (task == null) {
                SwingUtilities.invokeLater(this::hideSignatureHelp);
                return;
            }
            task.whenComplete((help, error) -> SwingUtilities.invokeLater(() -> {
                if (version != signatureHelpVersion.get()) return;
                if (error != null) {
                    hideSignatureHelp();
                    return;
                }
                showSignatureHelpResult(version, help);
            }));
        });
    }

    protected void showSignatureHelpResult(int version, SignatureHelp help) {
        if (version != signatureHelpVersion.get()) return;
        if (help == null || help.isEmpty()) {
            hideSignatureHelp();
            return;
        }
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight();
        int safeLine = Math.max(0, Math.min(caretLine, buffer.lineCount() - 1));
        String lineText = buffer.lineAt(safeLine);
        int safeCol = Math.min(caretCol, lineText.length());
        int x = 4 + textWidth(fm, lineText.substring(0, safeCol), 0);
        int yTop = yOfBufferLine(safeLine);
        int yBottom = yTop + lineHeight;
        SignatureHelpPopup popup = getOrCreateSignatureHelpPopup();
        popup.show(help, x, yTop, yBottom);
    }

    public void hideSignatureHelp() {
        signatureHelpVersion.incrementAndGet();
        if (signatureHelpPopup != null) signatureHelpPopup.hide();
    }

    public List<Caret> getExtraCarets() {
        return Collections.unmodifiableList(extraCarets);
    }

    public void addExtraCaret(int line, int col) {
        if (!multiCaretEnabled) return;
        int bl = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        int bc = Math.max(0, Math.min(col, buffer.lineAt(bl).length()));
        if (bl == caretLine && bc == caretCol) return;
        for (Caret c : extraCarets) if (c.line == bl && c.col == bc) return;
        extraCarets.add(new Caret(bl, bc));
        fireStateChangedIfNeeded();
        repaint();
    }

    public void clearExtraCarets() {
        if (extraCarets.isEmpty()) return;
        extraCarets.clear();
        fireStateChangedIfNeeded();
        repaint();
    }

    public boolean hasExtraCarets() {
        return !extraCarets.isEmpty();
    }

    public void addCaretBelow() {
        int target = caretLine + 1;
        while (target < buffer.lineCount() && isLineHidden(target)) target++;
        if (target >= buffer.lineCount()) return;
        int col = Math.min(caretCol, buffer.lineAt(target).length());
        addExtraCaret(target, col);
    }

    public void addCaretAbove() {
        int target = caretLine - 1;
        while (target >= 0 && isLineHidden(target)) target--;
        if (target < 0) return;
        int col = Math.min(caretCol, buffer.lineAt(target).length());
        addExtraCaret(target, col);
    }

    protected void insertAtExtraCarets(String text) {
        if (readOnly) return;
        if (extraCarets.isEmpty() || text == null || text.isEmpty()) return;
        insertAtAllCarets(text, false, text.length());
    }

    protected void insertAtAllCarets(String text, boolean overwrite, int caretAdvance) {
        if (readOnly) return;
        if (text == null || text.isEmpty()) return;
        int originalLength = buffer.length();
        int primaryOffset = caretOffset();
        List<Integer> originalExtraOffsets = new ArrayList<>();
        List<Integer> originalOffsets = new ArrayList<>();
        originalOffsets.add(primaryOffset);
        for (Caret c : extraCarets) {
            Caret copy = c.copy();
            copy.line = Math.max(0, Math.min(copy.line, buffer.lineCount() - 1));
            copy.col = Math.max(0, Math.min(copy.col, buffer.lineAt(copy.line).length()));
            int offset = buffer.offsetOfLine(copy.line) + copy.col;
            if (originalOffsets.contains(offset)) continue;
            originalExtraOffsets.add(offset);
            originalOffsets.add(offset);
        }

        List<Integer> editOffsets = new ArrayList<>(originalOffsets);
        editOffsets.sort(Collections.reverseOrder());
        Map<Integer, Integer> deltaByOffset = new HashMap<>();
        for (int offset : editOffsets) {
            int line = buffer.lineOfOffset(Math.max(0, Math.min(offset, originalLength)));
            int col = offset - buffer.offsetOfLine(line);
            int deleteLen = overwrite && offset < originalLength && col < buffer.lineAt(line).length() ? 1 : 0;
            if (deleteLen > 0) deleteText(offset, offset + deleteLen);
            insertText(offset, text);
            deltaByOffset.put(offset, text.length() - deleteLen);
        }

        setCaretFromOffset(caretTargetOffset(primaryOffset, editOffsets, deltaByOffset, caretAdvance));
        extraCarets.clear();
        for (int originalOffset : originalExtraOffsets) {
            int newOff = caretTargetOffset(originalOffset, editOffsets, deltaByOffset, caretAdvance);
            int line = buffer.lineOfOffset(newOff);
            extraCarets.add(new Caret(line, newOff - buffer.offsetOfLine(line)));
        }
        removeDuplicateExtraCarets();
    }

    protected boolean replaceSelectionsAtCarets(String text, int caretAdvance) {
        if (readOnly) return false;
        if (!hasSelection() && !hasExtraSelections()) return false;
        List<CaretDeleteOp> ops = new ArrayList<>();
        if (hasSelection()) {
            ops.add(new CaretDeleteOp(caretOffset(), getSelectionStart(), getSelectionEnd(), true));
        } else {
            int offset = caretOffset();
            ops.add(new CaretDeleteOp(offset, offset, offset, true));
        }
        for (Caret c : extraCarets) {
            if (!c.hasSelection()) {
                int offset = extraCaretOffset(c);
                ops.add(new CaretDeleteOp(offset, offset, offset, false));
                continue;
            }
            int start = extraSelectionStart(c);
            int end = extraSelectionEnd(c);
            ops.add(new CaretDeleteOp(extraCaretOffset(c), start, end, false));
        }
        ops.sort((a, b) -> Integer.compare(b.start(), a.start()));
        List<CaretDeleteOp> filtered = filterOverlappingCaretOps(ops);
        filtered.sort((a, b) -> Integer.compare(b.start(), a.start()));
        beginCompoundEdit();
        try {
            for (CaretDeleteOp op : filtered) {
                if (op.start() != op.end()) deleteText(op.start(), op.end());
                if (!text.isEmpty()) insertText(op.start(), text);
            }
        } finally {
            endCompoundEdit();
        }
        CaretDeleteOp primaryOp = filtered.stream().filter(CaretDeleteOp::primary).findFirst().orElse(null);
        int primaryTarget = primaryOp != null ? primaryOp.start() : caretOffset();
        setCaretFromOffset(adjustOffsetAfterReplacements(primaryTarget, filtered, text.length(), caretAdvance));
        clearSelection();
        extraCarets.clear();
        for (CaretDeleteOp op : filtered) {
            if (op.primary()) continue;
            int off = adjustOffsetAfterReplacements(op.start(), filtered, text.length(), caretAdvance);
            off = Math.max(0, Math.min(off, buffer.length()));
            int line = buffer.lineOfOffset(off);
            extraCarets.add(new Caret(line, off - buffer.offsetOfLine(line)));
        }
        removeDuplicateExtraCarets();
        updateLastEditState();
        fireStateChangedIfNeeded();
        return true;
    }

    protected boolean deleteSelectionsAtCarets() {
        if (readOnly) return false;
        List<CaretDeleteOp> selectedOps = selectedCaretOps();
        if (selectedOps.isEmpty()) return false;

        int primaryOffset = caretOffset();
        List<Integer> extraOffsets = new ArrayList<>(extraCarets.size());
        for (Caret c : extraCarets) {
            extraOffsets.add(extraCaretOffset(c));
        }

        List<CaretDeleteOp> descendingOps = new ArrayList<>(selectedOps);
        descendingOps.sort((a, b) -> Integer.compare(b.start(), a.start()));
        beginCompoundEdit();
        try {
            for (CaretDeleteOp op : descendingOps) {
                deleteText(op.start(), op.end());
            }
        } finally {
            endCompoundEdit();
        }

        CaretDeleteOp primaryOp = selectedOps.stream().filter(CaretDeleteOp::primary).findFirst().orElse(null);
        int primaryTarget = primaryOp != null
                ? adjustOffsetAfterDeletes(primaryOp.start(), selectedOps)
                : adjustOffsetAfterDeletes(primaryOffset, selectedOps);
        setCaretFromOffset(primaryTarget);
        clearSelection();

        extraCarets.clear();
        for (int offset : extraOffsets) {
            CaretDeleteOp selectedOp = selectedOps.stream()
                    .filter(op -> !op.primary() && offset >= op.start() && offset <= op.end())
                    .findFirst()
                    .orElse(null);
            int target = selectedOp != null
                    ? adjustOffsetAfterDeletes(selectedOp.start(), selectedOps)
                    : adjustOffsetAfterDeletes(offset, selectedOps);
            target = Math.max(0, Math.min(target, buffer.length()));
            int line = buffer.lineOfOffset(target);
            extraCarets.add(new Caret(line, target - buffer.offsetOfLine(line)));
        }
        removeDuplicateExtraCarets();
        updateLastEditState();
        fireStateChangedIfNeeded();
        return true;
    }

    protected boolean wrapSelectionsAtCarets(char open, char close) {
        if (readOnly) return false;
        if (!hasSelection() && !hasExtraSelections()) return false;
        List<CaretDeleteOp> ops = new ArrayList<>();
        if (hasSelection()) {
            ops.add(new CaretDeleteOp(caretOffset(), getSelectionStart(), getSelectionEnd(), true));
        } else {
            int offset = caretOffset();
            ops.add(new CaretDeleteOp(offset, offset, offset, true));
        }
        for (Caret c : extraCarets) {
            if (c.hasSelection()) {
                ops.add(new CaretDeleteOp(extraCaretOffset(c), extraSelectionStart(c), extraSelectionEnd(c), false));
            } else {
                int offset = extraCaretOffset(c);
                ops.add(new CaretDeleteOp(offset, offset, offset, false));
            }
        }
        List<CaretDeleteOp> filtered = filterOverlappingCaretOps(ops);
        filtered.sort((a, b) -> Integer.compare(b.start(), a.start()));
        Map<CaretDeleteOp, Integer> advanceByOp = new HashMap<>();

        beginCompoundEdit();
        try {
            for (CaretDeleteOp op : filtered) {
                String selected = op.start() == op.end() ? "" : buffer.substring(op.start(), op.end());
                String replacement = "" + open + selected + close;
                if (op.start() != op.end()) deleteText(op.start(), op.end());
                insertText(op.start(), replacement);
                advanceByOp.put(op, 1 + selected.length());
            }
        } finally {
            endCompoundEdit();
        }

        CaretDeleteOp primaryOp = filtered.stream().filter(CaretDeleteOp::primary).findFirst().orElse(null);
        if (primaryOp != null) {
            setCaretFromOffset(adjustOffsetAfterReplacements(
                    primaryOp.start(), filtered, advanceByOp.getOrDefault(primaryOp, 1)));
        }
        clearSelection();
        extraCarets.clear();
        for (CaretDeleteOp op : filtered) {
            if (op.primary()) continue;
            int off = adjustOffsetAfterReplacements(op.start(), filtered, advanceByOp.getOrDefault(op, 1));
            off = Math.max(0, Math.min(off, buffer.length()));
            int line = buffer.lineOfOffset(off);
            extraCarets.add(new Caret(line, off - buffer.offsetOfLine(line)));
        }
        removeDuplicateExtraCarets();
        updateLastEditState();
        fireStateChangedIfNeeded();
        return true;
    }

    protected List<CaretDeleteOp> filterOverlappingCaretOps(List<CaretDeleteOp> ops) {
        List<CaretDeleteOp> ordered = new ArrayList<>(ops);
        ordered.sort((a, b) -> {
            int cmp = Integer.compare(a.start(), b.start());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(b.end(), a.end());
            if (cmp != 0) return cmp;
            return Boolean.compare(b.primary(), a.primary());
        });
        List<CaretDeleteOp> filtered = new ArrayList<>();
        int coveredEnd = -1;
        for (CaretDeleteOp op : ordered) {
            if (op.start() < coveredEnd) continue;
            filtered.add(op);
            coveredEnd = Math.max(coveredEnd, op.end());
        }
        return filtered;
    }

    protected int adjustOffsetAfterReplacements(int offset, List<CaretDeleteOp> ops,
                                                int insertedLen, int caretAdvance) {
        int adjusted = offset;
        for (CaretDeleteOp op : ops) {
            int removedLen = op.end() - op.start();
            int delta = insertedLen - removedLen;
            if (op.start() < offset) {
                adjusted += delta;
            }
        }
        return Math.max(0, Math.min(buffer.length(), adjusted + caretAdvance));
    }

    protected int adjustOffsetAfterReplacements(int offset, List<CaretDeleteOp> ops, int caretAdvance) {
        int adjusted = offset;
        for (CaretDeleteOp op : ops) {
            int removedLen = op.end() - op.start();
            int insertedLen = removedLen + 2;
            int delta = insertedLen - removedLen;
            if (op.start() < offset) {
                adjusted += delta;
            }
        }
        return Math.max(0, Math.min(buffer.length(), adjusted + caretAdvance));
    }

    protected int caretTargetOffset(int originalOffset, List<Integer> editOffsets,
                                    Map<Integer, Integer> deltaByOffset, int caretAdvance) {
        int shiftBefore = 0;
        for (int editOffset : editOffsets) {
            if (editOffset < originalOffset) {
                shiftBefore += deltaByOffset.getOrDefault(editOffset, 0);
            }
        }
        return Math.max(0, Math.min(buffer.length(), originalOffset + shiftBefore + caretAdvance));
    }

    protected boolean deleteAtCarets(boolean backspace) {
        if (readOnly) return false;
        if (extraCarets.isEmpty()) return false;
        int primaryOffset = caretOffset();
        List<CaretDeleteOp> ops = new ArrayList<>(extraCarets.size() + 1);
        if (hasSelection()) {
            ops.add(new CaretDeleteOp(primaryOffset, getSelectionStart(), getSelectionEnd(), true));
        } else {
            addCaretDeleteOp(ops, primaryOffset, backspace, true);
        }
        for (Caret c : extraCarets) {
            if (c.hasSelection()) {
                ops.add(new CaretDeleteOp(extraCaretOffset(c), extraSelectionStart(c), extraSelectionEnd(c), false));
            } else {
                addCaretDeleteOp(ops, extraCaretOffset(c), backspace, false);
            }
        }
        if (ops.isEmpty()) return false;
        List<CaretDeleteOp> filtered = filterOverlappingCaretOps(ops);
        filtered.removeIf(op -> op.end() <= op.start());
        filtered.sort((a, b) -> Integer.compare(b.start(), a.start()));
        if (filtered.isEmpty()) return false;

        CaretDeleteOp primaryOp = filtered.stream().filter(CaretDeleteOp::primary).findFirst().orElse(null);
        int primaryTarget = primaryOp != null ? primaryOp.start() : primaryOffset;
        beginCompoundEdit();
        try {
            for (CaretDeleteOp op : filtered) {
                deleteText(op.start(), op.end());
            }
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(adjustOffsetAfterDeletes(primaryTarget, filtered));
        extraCarets.clear();
        for (CaretDeleteOp op : filtered) {
            if (op.primary()) continue;
            int adjusted = adjustOffsetAfterDeletes(op.start(), filtered);
            int line = buffer.lineOfOffset(adjusted);
            extraCarets.add(new Caret(line, adjusted - buffer.offsetOfLine(line)));
        }
        clearSelection();
        removeDuplicateExtraCarets();
        updateLastEditState();
        fireStateChangedIfNeeded();
        return true;
    }

    protected void addCaretDeleteOp(List<CaretDeleteOp> ops, int offset, boolean backspace, boolean primary) {
        if (backspace) {
            if (offset <= 0) return;
            ops.add(new CaretDeleteOp(offset, offset - 1, offset, primary));
        } else {
            if (offset >= buffer.length()) return;
            ops.add(new CaretDeleteOp(offset, offset, offset + 1, primary));
        }
    }

    protected int deleteTargetOffset(int offset, boolean backspace) {
        return backspace && offset > 0 ? offset - 1 : offset;
    }

    protected int adjustOffsetAfterDeletes(int offset, List<CaretDeleteOp> ops) {
        int adjusted = offset;
        for (CaretDeleteOp op : ops) {
            int len = op.end() - op.start();
            if (op.end() <= offset) {
                adjusted -= len;
            } else if (op.start() < offset) {
                adjusted -= offset - op.start();
            }
        }
        return Math.max(0, Math.min(buffer.length(), adjusted));
    }

    protected void paintExtraCarets(Graphics2D g2, FontMetrics fm) {
        if (extraCarets.isEmpty() || !isFocusOwner() || !caretVisible) return;
        int lineHeight = fm.getHeight();
        g2.setColor(defaultStyle.getForeground());
        for (Caret c : extraCarets) {
            if (isLineHidden(c.line)) continue;
            String lineText = buffer.lineAt(c.line);
            int cx = baseVisualXForColumn(c.line, lineText, c.col, fm);
            int cy = yOfBufferLine(c.line);
            g2.fillRect(cx, cy, 2, lineHeight);
        }
    }

    protected void paintDiagnostics(Graphics2D g2, FontMetrics fm) {
        if (!diagnosticsRenderingEnabled || diagnostics.isEmpty()) return;
        int lineHeight = fm.getHeight();
        for (Diagnostic d : diagnostics) {
            int startLine = d.startLine();
            int endLine = d.endLine();
            if (endLine < startLine) endLine = startLine;
            for (int line = startLine; line <= endLine; line++) {
                if (line < 0 || line >= buffer.lineCount()) continue;
                if (isLineHidden(line)) continue;
                String lineText = buffer.lineAt(line);
                int colStart = (line == startLine) ? Math.max(0, d.startCol()) : 0;
                int colEnd = (line == endLine) ? Math.max(colStart, Math.min(d.endCol(), lineText.length())) : lineText.length();
                if (colEnd <= colStart) colEnd = Math.min(colStart + 1, lineText.length());
                int x1 = 4 + textWidth(fm, lineText.substring(0, Math.min(colStart, lineText.length())), 0);
                int x2 = 4 + textWidth(fm, lineText.substring(0, Math.min(colEnd, lineText.length())), 0);
                if (x2 <= x1) x2 = x1 + fm.charWidth(' ');
                int ly = yOfBufferLine(line) + fm.getAscent() + 1;
                g2.setColor(d.effectiveColor());
                drawSquiggly(g2, x1, x2, ly);
            }
        }
    }

    protected void drawSquiggly(Graphics2D g2, int x1, int x2, int y) {
        int amp = 2;
        int step = 2;
        int x = x1;
        boolean up = true;
        while (x < x2) {
            int nx = Math.min(x + step, x2);
            g2.drawLine(x, up ? y : y + amp, nx, up ? y + amp : y);
            up = !up;
            x = nx;
        }
    }

    protected void paintCodeLensRow(Graphics2D g2, FontMetrics defaultFm, Font baseFont,
                                    int bufferLine, int yTop, int lineHeight) {
        CodeLens lens = aboveCodeLensAtLine(bufferLine);
        if (lens == null) return;

        int xStart = 4;
        if (lens.col() > 0) {
            String lineText = buffer.lineAt(bufferLine);
            int safeCol = Math.min(lens.col(), lineText.length());
            xStart = 4 + textWidth(defaultFm, lineText.substring(0, safeCol), 0);
        }

        paintCodeLensItems(g2, baseFont, lens, xStart, yTop, lineHeight);
    }

    protected void paintInlineCodeLens(Graphics2D g2, FontMetrics defaultFm, Font baseFont,
                                       int bufferLine, int yTop, int lineHeight) {
        CodeLens lens = inlineCodeLensAtLine(bufferLine);
        if (lens == null) return;

        String lineText = buffer.lineAt(bufferLine);
        String renderedLineText = lineText;
        if (shouldHideTrailingOpenForFold(bufferLine)) {
            int idx = lineText.length() - 1;
            while (idx >= 0 && Character.isWhitespace(lineText.charAt(idx))) idx--;
            if (idx >= 0) renderedLineText = lineText.substring(0, idx);
        }
        int xStart;
        boolean atOrPastEnd;
        if (lens.col() < 0) {
            xStart = 4 + textWidth(defaultFm, renderedLineText, 0);
            atOrPastEnd = true;
        } else {
            int safeCol = Math.min(lens.col(), renderedLineText.length());
            xStart = 4 + textWidth(defaultFm, renderedLineText.substring(0, safeCol), 0);
            atOrPastEnd = safeCol >= lineText.length();
        }

        if (atOrPastEnd && foldingEnabled && isFoldAnchor(bufferLine)) {
            String pillText = (foldPlaceholder == null || foldPlaceholder.trim().isEmpty())
                    ? "…" : foldPlaceholder.trim();
            if (foldPlaceholderWithSeparators) {
                String[] sep = findFoldSeparatorsForRegion(getFoldRegionStartingAt(bufferLine));
                if (sep != null) pillText = sep[0] + pillText + sep[1];
            }
            int hPad = 6;
            int pillW = defaultFm.stringWidth(pillText) + hPad * 2;
            xStart += 6 + pillW;
        }

        if (lens.col() < 0) {
            xStart += codeLensItemSpacing;
        }

        paintCodeLensItems(g2, baseFont, lens, xStart, yTop, lineHeight);
    }

    protected void paintCodeLensItems(Graphics2D g2, Font baseFont, CodeLens lens,
                                      int xStart, int yTop, int lineHeight) {
        if (lens.items().isEmpty()) return;

        float lensFontSize = Math.max(8f, baseFont.getSize2D() * codeLensFontScale);
        Font baseLensFont = baseFont.deriveFont(lensFontSize);
        FontMetrics fm = g2.getFontMetrics(baseLensFont);

        Color baseFg = defaultStyle.getForeground();
        Color defaultFg = codeLensForeground != null
                ? codeLensForeground
                : new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 150);

        int x = xStart;
        int yBaseline = yTop + (lineHeight - fm.getHeight()) / 2 + fm.getAscent();

        for (int idx = 0; idx < lens.items().size(); idx++) {
            CodeLensItem item = lens.items().get(idx);
            if (item.getText() == null || item.getText().isEmpty()) continue;

            int fontStyle = Font.PLAIN;
            if (item.isBold()) fontStyle |= Font.BOLD;
            if (item.isItalic()) fontStyle |= Font.ITALIC;
            Font itemFont = baseLensFont.deriveFont(fontStyle);
            FontMetrics ifm = g2.getFontMetrics(itemFont);
            g2.setFont(itemFont);

            Color fg = item.getForeground() != null ? item.getForeground() : defaultFg;
            g2.setColor(fg);
            g2.drawString(item.getText(), x, yBaseline);

            int w = ifm.stringWidth(item.getText());
            if (item.isUnderline()) {
                int uy = yBaseline + 1;
                g2.drawLine(x, uy, x + w, uy);
            }

            codeLensItemBounds.add(new CodeLensItemBounds(lens, idx, x, yTop, w, lineHeight));

            x += w;
            if (idx < lens.items().size() - 1) {
                int sep = Math.max(4, codeLensItemSpacing / 2);
                int sepY = yTop + lineHeight / 2;
                g2.setColor(new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 60));
                g2.drawLine(x + sep, sepY - 3, x + sep, sepY + 3);
                x += codeLensItemSpacing;
            }
        }

        g2.setFont(baseFont);
    }

    protected void paintInlayHints(Graphics2D g2, FontMetrics fm) {
        if (!inlayHintsEnabled || inlayHints.isEmpty()) return;
        int lineHeight = fm.getHeight();
        Color baseFg = defaultStyle.getForeground();
        Color defaultHintBg = new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 24);
        Color defaultHintFg = new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 170);
        Map<Integer, Integer> pushedWidthByLine = new HashMap<>();
        for (InlayHint hint : sortedVisibleInlayHints()) {
            if (hint.text() == null || hint.text().isEmpty()) continue;
            int line = hint.line();
            if (line < 0 || line >= buffer.lineCount()) continue;
            if (isLineHidden(line)) continue;
            String lineText = buffer.lineAt(line);
            int col = inlayHintColumn(hint, lineText);
            int baseX = 4 + textWidth(fm, lineText.substring(0, col), 0)
                    + pushedWidthByLine.getOrDefault(line, 0);
            int ly = yOfBufferLine(line);
            int padL = inlayHintPaddingLeft(hint);
            int w = inlayHintWidth(fm, hint);
            int h = lineHeight - 2;
            Color bg = hint.backgroundColor() != null ? hint.backgroundColor() : defaultHintBg;
            Color fg = hint.foregroundColor() != null ? hint.foregroundColor() : defaultHintFg;
            g2.setColor(bg);
            g2.fillRoundRect(baseX, ly + 1, w, h, 6, 6);
            g2.setColor(fg);
            g2.drawString(hint.text(), baseX + padL, ly + fm.getAscent());
            if (hint.pushText()) {
                pushedWidthByLine.merge(line, w, Integer::sum);
            }
        }
    }

    protected List<InlayHint> sortedVisibleInlayHints() {
        if (!inlayHintsEnabled || inlayHints.isEmpty()) return List.of();
        List<InlayHint> sorted = new ArrayList<>(inlayHints);
        sorted.removeIf(this::shouldHideInlayHintForCaretOrSelection);
        sorted.sort(Comparator
                .comparingInt(InlayHint::line)
                .thenComparingInt(InlayHint::col));
        return sorted;
    }

    protected List<InlayHint> pushInlayHintsForLine(int line, FontMetrics fm) {
        if (!inlayHintsEnabled || inlayHints.isEmpty()) return List.of();
        List<InlayHint> hints = new ArrayList<>();
        for (InlayHint hint : inlayHints) {
            if (hint == null || !hint.pushText() || hint.line() != line) continue;
            if (hint.text() == null || hint.text().isEmpty()) continue;
            if (shouldHideInlayHintForCaretOrSelection(hint)) continue;
            hints.add(hint);
        }
        hints.sort(Comparator.comparingInt(InlayHint::col));
        return hints;
    }

    protected boolean shouldHideInlayHintForCaretOrSelection(InlayHint hint) {
        if (hint == null || !hint.hideOnCaretOrSelection()) return false;
        if (hint.line() < 0 || hint.line() >= buffer.lineCount()) return false;
        int col = Math.min(Math.max(0, hint.col()), buffer.lineAt(hint.line()).length());
        int offset = buffer.offsetOfLine(hint.line()) + col;
        if (inlayInteractionTouchesRegion(hint.line(), col)) return true;
        if (hasSelection() && selectionTouchesInlayRegion(getSelectionStart(), getSelectionEnd(), hint.line(), offset)) {
            return true;
        }
        for (Caret caret : extraCarets) {
            if (caret.hasSelection()) {
                int start = extraSelectionStart(caret);
                int end = extraSelectionEnd(caret);
                if (selectionTouchesInlayRegion(start, end, hint.line(), offset)) return true;
            }
        }
        return false;
    }

    protected void rememberInlayInteraction(int line, int col) {
        inlayInteractionLine = line;
        inlayInteractionCol = col;
    }

    protected void clearInlayInteraction() {
        inlayInteractionLine = -1;
        inlayInteractionCol = -1;
    }

    protected boolean inlayInteractionTouchesRegion(int hintLine, int hintCol) {
        return inlayInteractionLine == hintLine && inlayInteractionCol >= hintCol;
    }

    protected boolean selectionTouchesInlayRegion(int selectionStart, int selectionEnd, int hintLine, int hintOffset) {
        if (selectionStart == selectionEnd) return false;
        int lineEndOffset = buffer.offsetOfLine(hintLine) + buffer.lineAt(hintLine).length();
        return selectionStart <= lineEndOffset && selectionEnd >= hintOffset;
    }

    protected int pushedInlayWidthForLine(int line, FontMetrics fm) {
        int width = 0;
        for (InlayHint hint : pushInlayHintsForLine(line, fm)) {
            width += inlayHintWidth(fm, hint);
        }
        return width;
    }

    protected int inlayHintWidth(FontMetrics fm, InlayHint hint) {
        return fm.stringWidth(hint.text())
                + inlayHintPaddingLeft(hint)
                + inlayHintPaddingRight(hint);
    }

    protected int inlayHintPaddingLeft(InlayHint hint) {
        return hint.paddingLeft() ? 4 : 0;
    }

    protected int inlayHintPaddingRight(InlayHint hint) {
        return hint.paddingRight() ? 4 : 0;
    }

    protected int inlayHintColumn(InlayHint hint, String lineText) {
        return Math.min(Math.max(0, hint.col()), lineText.length());
    }

    protected int visualXForColumn(int line, String lineText, int col, FontMetrics fm) {
        return baseVisualXForColumn(line, lineText, col, fm)
                + ghostPushWidthBeforeColumn(line, col, fm);
    }

    protected int baseVisualXForColumn(int line, String lineText, int col, FontMetrics fm) {
        int safeCol = Math.min(Math.max(0, col), lineText.length());
        int x = 4 + textWidth(fm, lineText.substring(0, safeCol), 0);
        for (InlayHint hint : pushInlayHintsForLine(line, fm)) {
            if (inlayHintColumn(hint, lineText) <= safeCol) {
                x += inlayHintWidth(fm, hint);
            }
        }
        return x;
    }

    protected int ghostPushWidthBeforeColumn(int line, int col, FontMetrics fm) {
        if (!isGhostVisibleAtAnchor()) return 0;
        if (line != ghostAnchorLine) return 0;
        if (col < ghostAnchorCol) return 0;
        return ghostFirstSegmentWidth(fm);
    }

    protected boolean isGhostVisibleAtAnchor() {
        return hasGhostText() && ghostAnchorLine == caretLine && ghostAnchorCol == caretCol;
    }

    protected int ghostFirstSegmentWidth(FontMetrics fm) {
        if (!hasGhostText()) return 0;
        int nl = ghostText.indexOf('\n');
        String firstSegment = nl < 0 ? ghostText : ghostText.substring(0, nl);
        if (firstSegment.isEmpty()) return 0;
        return fm.stringWidth(firstSegment);
    }

    protected InlayHint mouseTransparentInlayHintAt(int line, int mouseX, FontMetrics fm) {
        if (!inlayHintsEnabled || inlayHints.isEmpty()) return null;
        if (line < 0 || line >= buffer.lineCount()) return null;
        String lineText = buffer.lineAt(line);
        int pushedWidth = 0;
        for (InlayHint hint : sortedVisibleInlayHints()) {
            if (hint.line() != line) continue;
            int col = inlayHintColumn(hint, lineText);
            int x = 4 + textWidth(fm, lineText.substring(0, col), 0) + pushedWidth;
            int w = inlayHintWidth(fm, hint);
            if (hint.mouseTransparent() && mouseX >= x && mouseX <= x + w) {
                return hint;
            }
            if (hint.pushText()) {
                pushedWidth += w;
            }
        }
        return null;
    }

    protected void paintSearchMatches(Graphics2D g2, FontMetrics fm) {
        if (searchMatches.isEmpty()) return;
        int lineHeight = fm.getHeight();
        Color highlight = searchHighlightColor != null
                ? searchHighlightColor : new Color(255, 220, 0, 90);
        Color current = searchCurrentHighlightColor != null
                ? searchCurrentHighlightColor : new Color(255, 140, 0, 140);
        for (int i = 0; i < searchMatches.size(); i++) {
            SearchMatch match = searchMatches.get(i);
            int startLine = buffer.lineOfOffset(match.startOffset());
            int endLine = buffer.lineOfOffset(match.endOffset());
            Color c = (i == searchCurrentIndex) ? current : highlight;
            g2.setColor(c);
            for (int line = startLine; line <= endLine; line++) {
                if (isLineHidden(line)) continue;
                int lineOff = buffer.offsetOfLine(line);
                String lineText = buffer.lineAt(line);
                int colStart = (line == startLine) ? match.startOffset() - lineOff : 0;
                int colEnd = (line == endLine) ? match.endOffset() - lineOff : lineText.length();
                int x1 = 4 + textWidth(fm, lineText.substring(0, Math.min(colStart, lineText.length())), 0);
                int x2 = 4 + textWidth(fm, lineText.substring(0, Math.min(colEnd, lineText.length())), 0);
                int ly = yOfBufferLine(line);
                g2.fillRect(x1, ly, Math.max(1, x2 - x1), lineHeight);
            }
        }
    }

    public void searchUpdateQuery(String query, SearchOptions opts) {
        this.searchQuery = query == null ? "" : query;
        if (opts != null) this.searchOptions = opts;
        searchMatches.clear();
        searchCurrentIndex = -1;
        if (!searchQuery.isEmpty()) {
            searchMatches.addAll(searchEngine.findAll(buffer, searchQuery, searchOptions));
            if (!searchMatches.isEmpty()) {
                SearchMatch next = searchEngine.findNext(searchMatches, caretOffset(), searchOptions.isWrapAround());
                if (next != null) searchCurrentIndex = searchMatches.indexOf(next);
                else searchCurrentIndex = 0;
            }
        }
        updateSearchPanelCount();
        repaint();
    }

    protected void updateSearchPanelCount() {
        if (searchPanel != null) searchPanel.updateMatchCount(searchCurrentIndex, searchMatches.size());
    }

    public void searchFindNext() {
        if (searchMatches.isEmpty()) return;
        int from = (searchCurrentIndex >= 0)
                ? searchMatches.get(searchCurrentIndex).endOffset() : caretOffset();
        SearchMatch m = searchEngine.findNext(searchMatches, from, searchOptions.isWrapAround());
        if (m == null) return;
        searchCurrentIndex = searchMatches.indexOf(m);
        selectSearchMatch(m);
    }

    public void searchFindPrev() {
        if (searchMatches.isEmpty()) return;
        int from = (searchCurrentIndex >= 0)
                ? searchMatches.get(searchCurrentIndex).startOffset() : caretOffset();
        SearchMatch m = searchEngine.findPrev(searchMatches, from, searchOptions.isWrapAround());
        if (m == null) return;
        searchCurrentIndex = searchMatches.indexOf(m);
        selectSearchMatch(m);
    }

    protected void selectSearchMatch(SearchMatch m) {
        int startLine = buffer.lineOfOffset(m.startOffset());
        int endLine = buffer.lineOfOffset(m.endOffset());
        selectionStartLine = startLine;
        selectionStartCol = m.startOffset() - buffer.offsetOfLine(startLine);
        caretLine = endLine;
        caretCol = m.endOffset() - buffer.offsetOfLine(endLine);
        updateSearchPanelCount();
        scrollToCaret();
        resetCaretBlink();
        repaint();
    }

    public void searchReplaceCurrent(String replacement) {
        if (readOnly) return;
        if (searchMatches.isEmpty() || searchCurrentIndex < 0) return;
        SearchMatch m = searchMatches.get(searchCurrentIndex);
        String repl = replacement == null ? "" : replacement;
        beginCompoundEdit();
        try {
            deleteText(m.startOffset(), m.endOffset());
            insertText(m.startOffset(), repl);
        } finally {
            endCompoundEdit();
        }
        setCaretFromOffset(m.startOffset() + repl.length());
        clearSelection();
        updateLastEditState();
        searchUpdateQuery(searchQuery, searchOptions);
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void searchReplaceAll(String replacement) {
        if (readOnly) return;
        if (searchMatches.isEmpty()) return;
        String repl = replacement == null ? "" : replacement;
        List<SearchMatch> snapshot = new ArrayList<>(searchMatches);
        beginCompoundEdit();
        try {
            for (int i = snapshot.size() - 1; i >= 0; i--) {
                SearchMatch m = snapshot.get(i);
                deleteText(m.startOffset(), m.endOffset());
                insertText(m.startOffset(), repl);
            }
        } finally {
            endCompoundEdit();
        }
        searchUpdateQuery(searchQuery, searchOptions);
        clearSelection();
        updateLastEditState();
        scrollToCaret();
        resetCaretBlink();
        revalidate();
        repaint();
    }

    public void addSearchRequestListener(SearchRequestListener listener) {
        if (listener != null) searchRequestListeners.add(listener);
    }

    public void removeSearchRequestListener(SearchRequestListener listener) {
        searchRequestListeners.remove(listener);
    }

    protected void fireSearchRequested(boolean replaceMode) {
        replaceMode = replaceMode && !readOnly;
        String selected = hasSelection() ? getSelectedText() : "";
        if (selected.contains("\n")) selected = "";
        for (SearchRequestListener l : searchRequestListeners) {
            try {
                l.onSearchRequested(selected, replaceMode);
            } catch (Exception ignored) {
            }
        }
    }

    protected SearchPanel createSearchPanel() {
        return new SearchPanel(this);
    }

    protected SearchPanel getOrCreateSearchPanel() {
        if (searchPanel == null) searchPanel = createSearchPanel();
        return searchPanel;
    }

    public void showSearchPanel(boolean replaceMode) {
        SearchPanel panel = getOrCreateSearchPanel();
        panel.setReplaceVisible(replaceMode && !readOnly);
        String initial = hasSelection() ? getSelectedText() : "";
        if (!initial.isEmpty() && !initial.contains("\n")) {
            panel.setQuery(initial);
        }
        panel.setVisible(true);
        panel.focusFindField();
        searchUpdateQuery(panel.getFindField().getText(), panel.getOptions());
    }

    public void hideSearchPanel() {
        if (searchPanel != null) searchPanel.setVisible(false);
        searchMatches.clear();
        searchCurrentIndex = -1;
        searchQuery = "";
        requestFocusInWindow();
        repaint();
    }

    public boolean isSearchPanelVisible() {
        return searchPanel != null && searchPanel.isVisible();
    }

    public void setBracketMatcher(BracketMatcher matcher) {
        this.bracketMatcher = matcher != null ? matcher : BracketMatcher.defaultMatcher();
    }

    public void setGoToDefinitionKeyStroke(KeyStroke ks) {
        this.goToDefinitionKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setFindReferencesKeyStroke(KeyStroke ks) {
        this.findReferencesKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setRenameKeyStroke(KeyStroke ks) {
        this.renameKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setCodeActionsKeyStroke(KeyStroke ks) {
        this.codeActionsKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setToggleLineCommentKeyStroke(KeyStroke ks) {
        this.toggleLineCommentKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setToggleBlockCommentKeyStroke(KeyStroke ks) {
        this.toggleBlockCommentKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setExtendSelectionKeyStroke(KeyStroke ks) {
        this.extendSelectionKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setShrinkSelectionKeyStroke(KeyStroke ks) {
        this.shrinkSelectionKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setNavigateBackKeyStroke(KeyStroke ks) {
        this.navigateBackKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setNavigateForwardKeyStroke(KeyStroke ks) {
        this.navigateForwardKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setToggleBookmarkKeyStroke(KeyStroke ks) {
        this.toggleBookmarkKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setNextBookmarkKeyStroke(KeyStroke ks) {
        this.nextBookmarkKeyStroke = ks;
        rebindIdeActionKeys();
    }

    public void setPreviousBookmarkKeyStroke(KeyStroke ks) {
        this.previousBookmarkKeyStroke = ks;
        rebindIdeActionKeys();
    }

    protected void installIdeActions() {
        getActionMap().put(ACTION_GO_TO_DEFINITION, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerGoToDefinition();
            }
        });
        getActionMap().put(ACTION_FIND_REFERENCES, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerFindReferences();
            }
        });
        getActionMap().put(ACTION_RENAME, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerRename();
            }
        });
        getActionMap().put(ACTION_CODE_ACTIONS, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                triggerCodeActions();
            }
        });
        getActionMap().put(ACTION_TOGGLE_LINE_COMMENT, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleLineComment();
            }
        });
        getActionMap().put(ACTION_TOGGLE_BLOCK_COMMENT, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleBlockComment();
            }
        });
        getActionMap().put(ACTION_EXTEND_SELECTION, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                extendSelection();
            }
        });
        getActionMap().put(ACTION_SHRINK_SELECTION, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                shrinkSelection();
            }
        });
        getActionMap().put(ACTION_NAV_BACK, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateBack();
            }
        });
        getActionMap().put(ACTION_NAV_FORWARD, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                navigateForward();
            }
        });
        getActionMap().put(ACTION_TOGGLE_BOOKMARK, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleBookmarkAtCaret();
            }
        });
        getActionMap().put(ACTION_NEXT_BOOKMARK, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                jumpToNextBookmark();
            }
        });
        getActionMap().put(ACTION_PREV_BOOKMARK, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                jumpToPreviousBookmark();
            }
        });
        rebindIdeActionKeys();
    }

    protected void rebindIdeActionKeys() {
        InputMap im = getInputMap(WHEN_FOCUSED);
        KeyStroke[] keys = im.keys();
        if (keys != null) {
            for (KeyStroke k : keys) {
                Object action = im.get(k);
                if (ACTION_GO_TO_DEFINITION.equals(action) || ACTION_FIND_REFERENCES.equals(action)
                        || ACTION_RENAME.equals(action) || ACTION_CODE_ACTIONS.equals(action)
                        || ACTION_TOGGLE_LINE_COMMENT.equals(action) || ACTION_TOGGLE_BLOCK_COMMENT.equals(action)
                        || ACTION_EXTEND_SELECTION.equals(action) || ACTION_SHRINK_SELECTION.equals(action)
                        || ACTION_NAV_BACK.equals(action) || ACTION_NAV_FORWARD.equals(action)
                        || ACTION_TOGGLE_BOOKMARK.equals(action) || ACTION_NEXT_BOOKMARK.equals(action)
                        || ACTION_PREV_BOOKMARK.equals(action)) {
                    im.remove(k);
                }
            }
        }
        if (goToDefinitionKeyStroke != null) im.put(goToDefinitionKeyStroke, ACTION_GO_TO_DEFINITION);
        if (findReferencesKeyStroke != null) im.put(findReferencesKeyStroke, ACTION_FIND_REFERENCES);
        if (renameKeyStroke != null) im.put(renameKeyStroke, ACTION_RENAME);
        if (codeActionsKeyStroke != null) im.put(codeActionsKeyStroke, ACTION_CODE_ACTIONS);
        if (toggleLineCommentKeyStroke != null) im.put(toggleLineCommentKeyStroke, ACTION_TOGGLE_LINE_COMMENT);
        if (toggleBlockCommentKeyStroke != null) im.put(toggleBlockCommentKeyStroke, ACTION_TOGGLE_BLOCK_COMMENT);
        if (extendSelectionKeyStroke != null) im.put(extendSelectionKeyStroke, ACTION_EXTEND_SELECTION);
        if (shrinkSelectionKeyStroke != null) im.put(shrinkSelectionKeyStroke, ACTION_SHRINK_SELECTION);
        if (navigateBackKeyStroke != null) im.put(navigateBackKeyStroke, ACTION_NAV_BACK);
        if (navigateForwardKeyStroke != null) im.put(navigateForwardKeyStroke, ACTION_NAV_FORWARD);
        if (toggleBookmarkKeyStroke != null) im.put(toggleBookmarkKeyStroke, ACTION_TOGGLE_BOOKMARK);
        if (nextBookmarkKeyStroke != null) im.put(nextBookmarkKeyStroke, ACTION_NEXT_BOOKMARK);
        if (previousBookmarkKeyStroke != null) im.put(previousBookmarkKeyStroke, ACTION_PREV_BOOKMARK);
    }

    public int applyEdits(List<TextEdit> edits) {
        if (readOnly) return 0;
        if (edits == null || edits.isEmpty()) return 0;
        List<TextEdit> sorted = new ArrayList<>(edits);
        sorted.sort((a, b) -> {
            int la = a.range().start().line();
            int lb = b.range().start().line();
            if (la != lb) return Integer.compare(lb, la);
            return Integer.compare(b.range().start().col(), a.range().start().col());
        });
        beginCompoundEdit();
        int applied = 0;
        try {
            for (TextEdit edit : sorted) {
                if (edit == null || edit.range() == null) continue;
                int start = clampOffset(offsetOf(edit.range().start()));
                int end = clampOffset(offsetOf(edit.range().end()));
                if (end < start) {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }
                String newText = edit.newText() == null ? "" : edit.newText();
                if (end > start) deleteText(start, end);
                if (!newText.isEmpty()) insertText(start, newText);
                applied++;
            }
        } finally {
            endCompoundEdit();
        }
        if (applied > 0) {
            clampCaret();
            if (foldingEnabled) recomputeFoldRegions();
            scrollToCaret();
            repaint();
        }
        return applied;
    }

    public void beginCompoundEdit() {
        buffer.beginCompound(captureEditorState());
    }

    public void endCompoundEdit() {
        buffer.endCompound(captureEditorState());
    }

    public boolean isModified() {
        return buffer.getVersion() != cleanBufferVersion;
    }

    public void markClean() {
        cleanBufferVersion = buffer.getVersion();
        fireStateChangedIfNeeded();
    }

    protected int offsetOf(Position p) {
        if (p == null) return 0;
        int line = Math.max(0, Math.min(p.line(), buffer.lineCount() - 1));
        String text = buffer.lineAt(line);
        int col = Math.max(0, Math.min(p.col(), text.length()));
        return buffer.offsetOfLine(line) + col;
    }

    protected int clampOffset(int offset) {
        return Math.max(0, Math.min(offset, buffer.length()));
    }

    protected Position positionOf(int offset) {
        int off = clampOffset(offset);
        int line = buffer.lineOfOffset(off);
        int col = off - buffer.offsetOfLine(line);
        return new Position(line, col);
    }

    public Range caretRange() {
        Position p = new Position(caretLine, caretCol);
        if (hasSelection()) {
            Position s = new Position(selectionStartLine, selectionStartCol);

            if (selectionStartLine < caretLine
                    || (selectionStartLine == caretLine && selectionStartCol < caretCol)) {
                return new Range(s, p);
            }
            return new Range(p, s);
        }
        return new Range(p, p);
    }

    public void triggerGoToDefinition() {
        if (definitionLocationProvider == null && definitionProvider == null) return;
        DefinitionLocationProvider definitionLocationProvider = this.definitionLocationProvider;
        DefinitionProvider definitionProvider = this.definitionProvider;
        String textSnapshot = buffer.getText();
        DefinitionContext ctx = new DefinitionContext(textSnapshot, new Position(caretLine, caretCol), caretOffset());
        getProviderExecutor().submit(() -> {
            if(definitionLocationProvider == null) return;
            List<Location> locations;
            try {
                locations = definitionLocationProvider.findDefinitions(ctx);
            } catch (Exception ex) {
                locations = Collections.emptyList();
            }
            final List<Location> snapshot = locations != null ? List.copyOf(locations) : List.of();
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                if (!snapshot.isEmpty()) openLocation(snapshot.getFirst());
            });
        });
        getProviderExecutor().submit(() -> {
            if(definitionProvider == null) return;
            definitionProvider.onDefinitionsRequest(ctx);
        });
    }

    public void triggerFindReferences() {
        if (referencesProvider == null) return;
        ReferencesProvider provider = referencesProvider;
        String textSnapshot = buffer.getText();
        DefinitionContext ctx = new DefinitionContext(textSnapshot,
                new Position(caretLine, caretCol), caretOffset());
        getProviderExecutor().submit(() -> {
            List<Location> refs;
            try {
                refs = provider.findReferences(ctx);
            } catch (Exception ex) {
                refs = Collections.emptyList();
            }
            final List<Location> snapshot = refs != null ? List.copyOf(refs) : List.of();
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                if (!snapshot.isEmpty()) fireReferences(snapshot);
            });
        });
    }

    protected final List<Consumer<List<Location>>> referencesListeners = new ArrayList<>();

    public void addReferencesListener(Consumer<List<Location>> l) {
        if (l != null) referencesListeners.add(l);
    }

    public void removeReferencesListener(Consumer<List<Location>> l) {
        referencesListeners.remove(l);
    }

    protected void fireReferences(List<Location> refs) {
        for (var l : referencesListeners) {
            try {
                l.accept(refs);
            } catch (Exception ignored) {}
        }
    }

    public void triggerRename() {
        if (readOnly) return;
        if (renameProvider == null) return;
        String current = currentWordAtCaret();
        String prompt = (current == null || current.isEmpty())
                ? text("rename.prompt.empty", "Rename to:")
                : text("rename.prompt.current", "Rename '{current}' to:")
                        .replace("{current}", current);
        String newName = JOptionPane.showInputDialog(this, prompt, current);
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty()) return;
        RenameProvider provider = renameProvider;
        String textSnapshot = buffer.getText();
        RenameContext ctx = new RenameContext(textSnapshot,
                new Position(caretLine, caretCol), caretOffset(), newName);
        getProviderExecutor().submit(() -> {
            List<TextEdit> edits;
            try {
                edits = provider.computeRenameEdits(ctx);
            } catch (Exception ex) {
                edits = Collections.emptyList();
            }
            final List<TextEdit> snapshot = edits != null ? List.copyOf(edits) : List.of();
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                if (!snapshot.isEmpty()) applyEdits(snapshot);
            });
        });
    }

    public void triggerCodeActions() {
        if (codeActionProvider == null) return;
        CodeActionProvider provider = codeActionProvider;
        String textSnapshot = buffer.getText();
        Range range = caretRange();
        List<Diagnostic> intersecting = new ArrayList<>();
        for (Diagnostic d : diagnostics) {
            if (diagnosticIntersects(d, range)) intersecting.add(d);
        }
        CodeActionContext ctx = new CodeActionContext(textSnapshot, range, intersecting);
        getProviderExecutor().submit(() -> {
            List<CodeAction> actions;
            try {
                actions = provider.getCodeActions(ctx);
            } catch (Exception ex) {
                actions = Collections.emptyList();
            }
            final List<CodeAction> snapshot = actions != null ? List.copyOf(actions) : List.of();
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                if (!snapshot.isEmpty()) showCodeActionsPopup(snapshot);
            });
        });
    }

    protected boolean diagnosticIntersects(Diagnostic d, Range r) {
        if (d == null || r == null) return false;
        int dStart = buffer.offsetOfLine(Math.max(0, Math.min(d.startLine(), buffer.lineCount() - 1)))
                + d.startCol();
        int dEnd = buffer.offsetOfLine(Math.max(0, Math.min(d.endLine(), buffer.lineCount() - 1)))
                + d.endCol();
        int rStart = offsetOf(r.start());
        int rEnd = offsetOf(r.end());
        return dStart <= rEnd && dEnd >= rStart;
    }

    protected void showCodeActionsPopup(List<CodeAction> actions) {
        JPopupMenu menu = new JPopupMenu();
        for (CodeAction action : actions) {
            JMenuItem item = new JMenuItem(action.title());
            item.addActionListener(ev -> applyCodeAction(action));
            if (action.preferred()) {
                java.awt.Font f = item.getFont();
                if (f != null) item.setFont(f.deriveFont(java.awt.Font.BOLD));
            }
            menu.add(item);
        }
        Point p = caretScreenPoint();
        if (p == null) p = new Point(0, 0);
        menu.show(this, p.x, p.y);
    }

    public void applyCodeAction(CodeAction action) {
        if (action == null) return;
        if (readOnly && !action.edits().isEmpty()) return;
        if (!action.edits().isEmpty()) applyEdits(action.edits());
        if (action.command() != null && commandHandler != null) {
            try {
                commandHandler.execute(action.command());
            } catch (Exception ignored) {
            }
        }
    }

    protected String currentWordAtCaret() {
        int off = caretOffset();
        int start = off;
        int end = off;
        while (start > 0 && wordDetector.isWordChar(buffer.charAt(start - 1))) start--;
        while (end < buffer.length() && wordDetector.isWordChar(buffer.charAt(end))) end++;
        if (end <= start) return "";
        return buffer.substring(start, end);
    }

    public void toggleLineComment() {
        if (readOnly) return;
        if (commentProvider == null) return;
        CommentProvider provider = commentProvider;
        String textSnapshot = buffer.getText();
        getProviderExecutor().submit(() -> {
            String prefix;
            try {
                prefix = provider.lineCommentPrefix();
            } catch (Exception ignored) {
                prefix = null;
            }
            final String resolvedPrefix = prefix;
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                applyLineCommentToggle(resolvedPrefix);
            });
        });
    }

    protected void applyLineCommentToggle(String prefix) {
        if (readOnly) return;
        if (prefix == null || prefix.isEmpty()) return;
        int startLine, endLine;
        if (hasSelection()) {
            int a = Math.min(selectionStartLine, caretLine);
            int b = Math.max(selectionStartLine, caretLine);
            startLine = a;
            endLine = b;
        } else {
            startLine = endLine = caretLine;
        }
        boolean allCommented = true;
        for (int i = startLine; i <= endLine; i++) {
            String t = buffer.lineAt(i);
            String trimmed = t.replaceFirst("^\\s*", "");
            if (trimmed.isEmpty()) continue;
            if (!trimmed.startsWith(prefix)) {
                allCommented = false;
                break;
            }
        }
        beginCompoundEdit();
        try {
            for (int i = endLine; i >= startLine; i--) {
                String line = buffer.lineAt(i);
                int lineOffset = buffer.offsetOfLine(i);
                if (allCommented) {
                    int idx = line.indexOf(prefix);
                    if (idx >= 0) {
                        int deleteEnd = lineOffset + idx + prefix.length();
                        if (deleteEnd < buffer.length() && buffer.charAt(deleteEnd) == ' ') deleteEnd++;
                        deleteText(lineOffset + idx, deleteEnd);
                    }
                } else {
                    if (line.trim().isEmpty()) continue;
                    int leading = 0;
                    while (leading < line.length() && Character.isWhitespace(line.charAt(leading))) leading++;
                    insertText(lineOffset + leading, prefix + " ");
                }
            }
        } finally {
            endCompoundEdit();
        }
        repaint();
    }

    public void toggleBlockComment() {
        if (readOnly) return;
        if (commentProvider == null) return;
        CommentProvider provider = commentProvider;
        String textSnapshot = buffer.getText();
        getProviderExecutor().submit(() -> {
            String[] delim;
            try {
                delim = provider.blockCommentDelimiters();
            } catch (Exception ignored) {
                delim = null;
            }
            final String[] resolvedDelim = delim;
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                applyBlockCommentToggle(resolvedDelim);
            });
        });
    }

    protected void applyBlockCommentToggle(String[] delim) {
        if (readOnly) return;
        if (delim == null || delim.length < 2 || delim[0] == null || delim[1] == null) {
            toggleLineComment();
            return;
        }
        if (!hasSelection()) return;
        int start = Math.min(selectionStartOffset(), caretOffset());
        int end = Math.max(selectionStartOffset(), caretOffset());
        String selected = buffer.substring(start, end);
        beginCompoundEdit();
        try {
            if (selected.startsWith(delim[0]) && selected.endsWith(delim[1])) {
                deleteText(end - delim[1].length(), end);
                deleteText(start, start + delim[0].length());
            } else {
                insertText(end, delim[1]);
                insertText(start, delim[0]);
            }
        } finally {
            endCompoundEdit();
        }
        repaint();
    }

    public void extendSelection() {
        int off = caretOffset();
        if (selectionChainCache.isEmpty() || selectionChainIndex < 0) {
            if (selectionRangeProvider != null) {
                computeSelectionChainAsync(off);
                return;
            }
            selectionChainCache = defaultSelectionChain(off);
            selectionChainIndex = -1;
        }
        if (selectionChainCache.isEmpty()) return;
        if (selectionChainIndex + 1 < selectionChainCache.size()) {
            selectionChainIndex++;
            applySelectionFromChain();
        }
    }

    public void shrinkSelection() {
        if (selectionChainCache.isEmpty() || selectionChainIndex <= 0) {
            clearSelection();
            selectionChainCache = Collections.emptyList();
            selectionChainIndex = -1;
            repaint();
            return;
        }
        selectionChainIndex--;
        applySelectionFromChain();
    }

    protected void applySelectionFromChain() {
        if (selectionChainIndex < 0 || selectionChainIndex >= selectionChainCache.size()) return;
        Range r = selectionChainCache.get(selectionChainIndex);
        setSelection(r.start().line(), r.start().col(), r.end().line(), r.end().col());
        repaint();
    }

    protected List<Range> computeSelectionChain(int offset) {
        return defaultSelectionChain(offset);
    }

    protected void computeSelectionChainAsync(int offset) {
        SelectionRangeProvider provider = selectionRangeProvider;
        String textSnapshot = buffer.getText();
        getProviderExecutor().submit(() -> {
            List<Range> chain;
            try {
                chain = provider.getSelectionRanges(textSnapshot, offset);
            } catch (Exception ignored) {
                chain = null;
            }
            final List<Range> snapshot = chain != null && !chain.isEmpty()
                    ? List.copyOf(chain)
                    : null;
            SwingUtilities.invokeLater(() -> {
                if (!buffer.getText().equals(textSnapshot)) return;
                selectionChainCache = snapshot != null ? snapshot : defaultSelectionChain(offset);
                selectionChainIndex = -1;
                if (!selectionChainCache.isEmpty()) {
                    selectionChainIndex++;
                    applySelectionFromChain();
                }
            });
        });
    }

    protected List<Range> defaultSelectionChain(int offset) {
        List<Range> out = new ArrayList<>();

        int start = offset, end = offset;
        while (start > 0 && wordDetector.isWordChar(buffer.charAt(start - 1))) start--;
        while (end < buffer.length() && wordDetector.isWordChar(buffer.charAt(end))) end++;
        if (end > start) out.add(new Range(positionOf(start), positionOf(end)));

        int line = buffer.lineOfOffset(offset);
        int lineStart = buffer.offsetOfLine(line);
        int lineEnd = lineStart + buffer.lineAt(line).length();
        Range lineRange = new Range(positionOf(lineStart), positionOf(lineEnd));
        if (out.isEmpty() || !containsRange(lineRange, out.get(out.size() - 1))) out.add(lineRange);

        int pStart = line, pEnd = line;
        while (pStart > 0 && !buffer.lineAt(pStart - 1).trim().isEmpty()) pStart--;
        while (pEnd < buffer.lineCount() - 1 && !buffer.lineAt(pEnd + 1).trim().isEmpty()) pEnd++;
        int pStartOff = buffer.offsetOfLine(pStart);
        int pEndOff = buffer.offsetOfLine(pEnd) + buffer.lineAt(pEnd).length();
        Range pRange = new Range(positionOf(pStartOff), positionOf(pEndOff));
        if (!containsRange(pRange, out.get(out.size() - 1))) out.add(pRange);

        Range all = new Range(positionOf(0), positionOf(buffer.length()));
        if (!containsRange(all, out.get(out.size() - 1))) out.add(all);
        return out;
    }

    protected static boolean containsRange(Range outer, Range inner) {
        if (outer == null || inner == null) return false;
        return outer.start().line() == inner.start().line()
                && outer.start().col() == inner.start().col()
                && outer.end().line() == inner.end().line()
                && outer.end().col() == inner.end().col();
    }

    public void pushNavigationHistory() {
        NavigationEntry entry = new NavigationEntry(caretLine, caretCol);
        NavigationEntry last = navBackStack.peek();
        if (last != null && last.line == entry.line && last.col == entry.col) return;
        navBackStack.push(entry);
        while (navBackStack.size() > navigationHistoryLimit) navBackStack.pollLast();
        navForwardStack.clear();
    }

    public void navigateBack() {
        if (navBackStack.isEmpty()) return;
        NavigationEntry current = new NavigationEntry(caretLine, caretCol);
        NavigationEntry target = navBackStack.pop();
        navForwardStack.push(current);
        setCaretPosition(target.line, target.col);
        clearSelection();
        scrollToCaret();
        repaint();
    }

    public void navigateForward() {
        if (navForwardStack.isEmpty()) return;
        NavigationEntry current = new NavigationEntry(caretLine, caretCol);
        NavigationEntry target = navForwardStack.pop();
        navBackStack.push(current);
        setCaretPosition(target.line, target.col);
        clearSelection();
        scrollToCaret();
        repaint();
    }

    public void clearNavigationHistory() {
        navBackStack.clear();
        navForwardStack.clear();
    }

    public void openLocation(Location location) {
        if (location == null || location.range() == null) return;
        if (!location.isLocal()) {
            if (locationOpener != null) locationOpener.accept(location);
            return;
        }
        pushNavigationHistory();
        Range r = location.range();
        setSelection(r.start().line(), r.start().col(), r.end().line(), r.end().col());
        scrollToCaret();
        repaint();
    }

    public void toggleBookmarkAtCaret() {
        toggleBookmark(caretLine);
    }

    public void addBookmark(int line) {
        if (line < 0 || line >= buffer.lineCount()) return;
        if (!bookmarks.add(line)) return;
        fireBookmarkChanged(line, true);
        fireBookmarksChanged();
        repaint();
    }

    public void removeBookmark(int line) {
        if (!bookmarks.remove(line)) return;
        fireBookmarkChanged(line, false);
        fireBookmarksChanged();
        repaint();
    }

    public void toggleBookmark(int line) {
        if (line < 0 || line >= buffer.lineCount()) return;
        boolean added;
        if (bookmarks.remove(line)) {
            added = false;
        } else {
            bookmarks.add(line);
            added = true;
        }
        fireBookmarkChanged(line, added);
        fireBookmarksChanged();
        repaint();
    }

    public void clearBookmarks() {
        if (bookmarks.isEmpty()) return;
        SortedSet<Integer> removed = new TreeSet<>(bookmarks);
        bookmarks.clear();
        removed.forEach(line -> fireBookmarkChanged(line, false));
        fireBookmarksChanged();
        repaint();
    }

    public void jumpToNextBookmark() {
        if (bookmarks.isEmpty()) return;
        Integer next = ((TreeSet<Integer>) bookmarks).higher(caretLine);
        if (next == null) next = bookmarks.first();
        pushNavigationHistory();
        setCaretPosition(next, 0);
        scrollToCaret();
        repaint();
    }

    public void jumpToPreviousBookmark() {
        if (bookmarks.isEmpty()) return;
        Integer prev = ((TreeSet<Integer>) bookmarks).lower(caretLine);
        if (prev == null) prev = bookmarks.last();
        pushNavigationHistory();
        setCaretPosition(prev, 0);
        scrollToCaret();
        repaint();
    }

    public void addBookmarkListener(Runnable l) {
        if (l != null) {
            bookmarkListeners.add(l);
        }
    }

    public void removeBookmarkListener(Runnable l) {
        bookmarkListeners.remove(l);
    }

    public void addBookmarkChangeListener(BookmarkChangeListener l) {
        if (l != null) {
            bookmarkChangeListeners.add(l);
        }
    }

    public void removeBookmarkChangeListener(BookmarkChangeListener l) {
        bookmarkChangeListeners.remove(l);
    }

    protected void fireBookmarkChanged(int line, boolean added) {
        for (BookmarkChangeListener listener : List.copyOf(bookmarkChangeListeners)) {
            listener.onBookmarkChanged(line, added);
        }
    }

    protected void fireBookmarksChanged() {
        for (Runnable r : bookmarkListeners) {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isBookmarked(int line) {
        return bookmarks.contains(line);
    }

    public int findMatchingBracket(int offset) {
        if (bracketMatcher == null) {
            return -1;
        }
        try {
            return bracketMatcher.findMatch(buffer.getText(), offset);
        } catch (Exception ex) {
            return -1;
        }
    }

    public CompletableFuture<Integer> findMatchingBracketAsync(int offset) {
        if (bracketMatcher == null) {
            return CompletableFuture.completedFuture(-1);
        }
        BracketMatcher matcher = bracketMatcher;
        String textSnapshot = buffer.getText();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return matcher.findMatch(textSnapshot, offset);
            } catch (Exception ignored) {
                return -1;
            }
        }, getProviderExecutor());
    }

    public void jumpToMatchingBracket() {
        int offset = caretOffset();
        String textSnapshot = buffer.getText();
        findMatchingBracketAsync(offset).thenAccept(match -> SwingUtilities.invokeLater(() -> {
            if (!buffer.getText().equals(textSnapshot)) return;
            if (match == null || match < 0) return;
            pushNavigationHistory();
            setCaretFromOffset(match);
            clearSelection();
            scrollToCaret();
            repaint();
        }));
    }

    public List<DocumentSymbol> getDocumentSymbols() {
        if (documentSymbolProvider == null) return Collections.emptyList();
        refreshDocumentSymbolsAsync();
        return List.copyOf(documentSymbols);
    }

    public CompletableFuture<List<DocumentSymbol>> getDocumentSymbolsAsync() {
        return refreshDocumentSymbolsAsync();
    }

    public CompletableFuture<List<DocumentSymbol>> refreshDocumentSymbolsAsync() {
        if (documentSymbolProvider == null) {
            documentSymbols.clear();
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        int version = documentSymbolVersion.incrementAndGet();
        DocumentSymbolProvider provider = documentSymbolProvider;
        String textSnapshot = buffer.getText();
        CompletableFuture<List<DocumentSymbol>> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<DocumentSymbol> list = provider.getDocumentSymbols(textSnapshot);
                return list == null ? Collections.emptyList() : List.copyOf(list);
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        }, getProviderExecutor());
        future.thenAccept(symbols -> SwingUtilities.invokeLater(() -> {
            if (version != documentSymbolVersion.get()) return;
            if (!buffer.getText().equals(textSnapshot)) return;
            documentSymbols.clear();
            documentSymbols.addAll(symbols);
        }));
        return future;
    }
}
