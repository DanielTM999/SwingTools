package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.utils.BracketHighlighter;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

public class CodeEditorTextArea extends CodeEditorTextAreaRename {

    public CodeEditorTextArea() {
        this(new TextBuffer());
    }

    public CodeEditorTextArea(String initialText) {
        this(new TextBuffer(initialText != null ? initialText.replace("\r\n", "\n").replace("\r", "\n") : ""));
    }

    protected CodeEditorTextArea(TextBuffer buffer) {
        super(buffer);
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
        addHierarchyListener(e -> {
            long flags = e.getChangeFlags();
            if ((flags & (HierarchyEvent.SHOWING_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) == 0) return;
            if (!isShowing() || !isDisplayable()) {
                dismissTransientUi();
            }
        });
        bracketHighlighter.setLineToVisualMapper(this::bufferLineToVisualLine);
        bracketHighlighter.setLineToYMapper(this::yOfBufferLine);
        bracketHighlighter.setLineHiddenPredicate(this::isLineHidden);
        bracketHighlighter.setColumnToXMapper(this::visualXForColumn);
        setupMoveLineActions();
        installIdeActions();
        documentEditListeners.add(new DocumentEditListener() {
            @Override
            public void onInsert(int offset, String text) {
                clearGhostText();
                suppressHoverWhileEditing();
                PendingHighlightEdit edit = new PendingHighlightEdit(offset, 0, text);
                pendingDiagnosticsEdit = edit;
                shiftStyledRangesForEdit(offset, 0, text.length());
            }

            @Override
            public void onDelete(int offset, String removed) {
                clearGhostText();
                suppressHoverWhileEditing();
                PendingHighlightEdit edit = new PendingHighlightEdit(offset, removed.length(), "");
                pendingDiagnosticsEdit = edit;
                shiftStyledRangesForEdit(offset, removed.length(), 0);
            }

            @Override
            public void onTextChanged() {
                refreshSearchOnTextChange();
                scheduleSelectedTextOccurrencesRefresh();
                scheduleDocumentHighlightsRefresh();
                selectionChainCache = Collections.emptyList();
                selectionChainIndex = -1;
                scheduleSyntaxHighlight();
                invalidateGeometry();
                if (diagnosticsAutoRunEnabled) {
                    scheduleDiagnosticsRefresh();
                }
                if (codeLensesAutoRunEnabled) {
                    scheduleCodeLensesRefresh();
                }
                if (inlayHintsEnabled && inlayHintProvider != null) {
                    scheduleInlayHintsRefresh();
                }
                if (documentSymbolProvider != null) {
                    scheduleDocumentSymbolsRefresh();
                }
            }
        });
        applySyntaxHighlight();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        lastPaintFontRenderContext = null;
        invalidateGeometry();
        ensureExecutorsStarted();
        scheduleSelectedTextOccurrencesRefresh();
        scheduleDocumentHighlightsRefresh();
        if (isSyntaxHighlightStale()) applySyntaxHighlight();
        hideActiveContextMenu();
    }


    @Override
    public void removeNotify() {
        cancelAsyncWork();
        shutdownExecutors();
        disposeTransientWindows();
        super.removeNotify();
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
}
