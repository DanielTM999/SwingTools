package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.CodeAction;
import dtm.stools.component.panels.editor.code.api.CodeEditorState;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeListener;
import dtm.stools.component.panels.editor.code.api.DocumentSymbol;
import dtm.stools.component.panels.editor.code.api.Location;
import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.listeners.BookmarkChangeListener;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.listeners.CodeEditorStateListener;
import dtm.stools.component.panels.editor.code.listeners.HoverListener;
import dtm.stools.component.panels.editor.code.listeners.LineChangeListener;
import dtm.stools.component.panels.editor.code.multicaret.Caret;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.utils.PopupOwnerGuard;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class CodeEditorTextAreaActions extends CodeEditorTextAreaHandlers {

    protected CodeEditorTextAreaActions(TextBuffer buffer) {
        super(buffer);
    }

    protected void cancelAsyncWork() {
        highlightVersion.incrementAndGet();
        diagnosticsVersion.incrementAndGet();
        codeLensVersion.incrementAndGet();
        inlayHintVersion.incrementAndGet();
        hoverDocumentationVersion.incrementAndGet();
        documentSymbolVersion.incrementAndGet();
        documentHighlightVersion.incrementAndGet();
        autoCompleteVersion.incrementAndGet();
        ghostTextVersion.incrementAndGet();
        signatureHelpVersion.incrementAndGet();
        selectedTextOccurrencesVersion.incrementAndGet();

        cancelFuture(currentHighlightTask);
        cancelFuture(currentDiagnosticsTask);
        cancelFuture(currentInlayHintTask);
        cancelFuture(currentCodeLensTask);
        cancelFuture(currentDocumentHighlightTask);
        cancelFuture(currentSelectedTextOccurrencesTask);
        if (selectedTextOccurrencesTimer != null) {
            selectedTextOccurrencesTimer.stop();
        }
        if (documentHighlightDebounceTimer != null) {
            documentHighlightDebounceTimer.stop();
        }
        resolvedDocumentHighlights = List.of();
        CompletableFuture<List<AutoCompleteItem>> autoCompleteTask = currentAutoCompleteTask;
        if (autoCompleteTask != null && !autoCompleteTask.isDone()) {
            autoCompleteTask.cancel(true);
        }

        if (diagnosticsDebounceTimer != null) diagnosticsDebounceTimer.stop();
        if (syntaxHighlightDebounceTimer != null) syntaxHighlightDebounceTimer.stop();
        if (syntaxHighlightWatchdogTimer != null) syntaxHighlightWatchdogTimer.stop();
        if (foldingDebounceTimer != null) foldingDebounceTimer.stop();
        if (ghostTextIdleTimer != null) ghostTextIdleTimer.stop();
        if (hoverTimer != null) hoverTimer.stop();
        if (hoverDocumentationHideTimer != null) hoverDocumentationHideTimer.stop();

        dismissTransientUi();
    }

    public void dismissTransientUi() {
        if (!hasTransientUiVisible()) {
            hoverLine = -1;
            hoverCol = -1;
            return;
        }
        hoverDocumentationVersion.incrementAndGet();
        signatureHelpVersion.incrementAndGet();
        autoCompleteVersion.incrementAndGet();
        ghostTextVersion.incrementAndGet();
        contextMenuVersion.incrementAndGet();
        if (hoverTimer != null) hoverTimer.stop();
        if (hoverDocumentationHideTimer != null) hoverDocumentationHideTimer.stop();
        if (ghostTextIdleTimer != null) ghostTextIdleTimer.stop();
        hoverLine = -1;
        hoverCol = -1;
        hideActiveContextMenu();
        hideAutoCompletePopup();
        hideHoverDocumentation();
        hideSignatureHelp();
        hideFoldPreview();
        clearGhostText();
    }

    protected boolean hasTransientUiVisible() {
        return (autoCompletePopup != null && autoCompletePopup.isVisible())
                || (hoverDocumentationPopup != null && hoverDocumentationPopup.isVisible())
                || (signatureHelpPopup != null && signatureHelpPopup.isVisible())
                || (foldPreviewWindow != null && foldPreviewWindow.isVisible())
                || (activeContextMenu != null && activeContextMenu.isVisible())
                || hasGhostText();
    }

    protected void hideActiveContextMenu() {
        JPopupMenu menu = activeContextMenu;
        activeContextMenu = null;
        if (menu != null && menu.isVisible()) {
            menu.setVisible(false);
        }
    }

    protected void disposeTransientWindows() {
        foldPreviewLine = -1;
        if (foldPreviewWindow != null) {
            foldPreviewWindow.getContentPane().removeAll();
            foldPreviewWindow.dispose();
            foldPreviewWindow = null;
        }
        foldPreviewOwnerWindow = null;
        if (hoverDocumentationPopup != null) hoverDocumentationPopup.dispose();
    }

    public boolean canShowPopups() {
        return PopupOwnerGuard.canShow(this);
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
            scheduleDocumentHighlightsRefresh();
            fireWordCaretChangeEvent(state.caretLine(), state.caretCol());
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
        if (!canShowPopups()) return;
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
        activeContextMenu = menu;
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
        if (last != null && last.line() == entry.line() && last.col() == entry.col()) return;
        navBackStack.push(entry);
        while (navBackStack.size() > navigationHistoryLimit) navBackStack.pollLast();
        navForwardStack.clear();
    }

    public void navigateBack() {
        if (navBackStack.isEmpty()) return;
        NavigationEntry current = new NavigationEntry(caretLine, caretCol);
        NavigationEntry target = navBackStack.pop();
        navForwardStack.push(current);
        setCaretPosition(target.line(), target.col());
        clearSelection();
        scrollToCaret();
        repaint();
    }

    public void navigateForward() {
        if (navForwardStack.isEmpty()) return;
        NavigationEntry current = new NavigationEntry(caretLine, caretCol);
        NavigationEntry target = navForwardStack.pop();
        navBackStack.push(current);
        setCaretPosition(target.line(), target.col());
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

    protected void scheduleDocumentSymbolsRefresh() {
        documentSymbolsDebounceTimer = restartDebounce(documentSymbolsDebounceTimer, documentSymbolsDebounceMs, this::refreshDocumentSymbolsAsync);
    }

    public CompletableFuture<List<DocumentSymbol>> refreshDocumentSymbolsAsync() {
        stopDebounce(documentSymbolsDebounceTimer);
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
