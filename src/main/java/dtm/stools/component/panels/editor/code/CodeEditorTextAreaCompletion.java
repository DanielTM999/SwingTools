package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteEditApplier;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import dtm.stools.component.panels.editor.code.autocomplete.SnippetExpansion;
import dtm.stools.component.panels.editor.code.ghost.GhostTextActivationMode;
import dtm.stools.component.panels.editor.code.ghost.GhostTextContext;
import dtm.stools.component.panels.editor.code.ghost.GhostTextProvider;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationContext;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationPopup;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.hover.HoverInfo;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.signature.SignatureHelp;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpContext;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpPopup;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.IntPredicate;

public abstract class CodeEditorTextAreaCompletion extends CodeEditorTextAreaDocument {

    protected CodeEditorTextAreaCompletion(TextBuffer buffer) {
        super(buffer);
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

    public void setAutoCompletePopupFactory(AutoCompletePopupFactory factory) {
        Objects.requireNonNull(factory, "factory");
        AutoCompletePopup popup = Objects.requireNonNull(factory.create(this), "factory returned null");
        if (this.autoCompletePopup != null) this.autoCompletePopup.hide();
        this.autoCompletePopup = popup;
        configureAutoCompletePopup(popup);
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
                if (!isShowing()) {
                    hideAutoCompletePopup();
                    return;
                }
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
        FontMetrics fm = fontMetricsFor(getFont());
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
        if (!canShowPopups()) {
            hideHoverDocumentation();
            return;
        }
        FontMetrics fm = fontMetricsFor(getFont());
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
        if (!canShowPopups()) {
            hideSignatureHelp();
            return;
        }
        FontMetrics fm = fontMetricsFor(getFont());
        int lineHeight = fm.getHeight();
        int safeLine = Math.max(0, Math.min(caretLine, buffer.lineCount() - 1));
        String lineText = buffer.lineAt(safeLine);
        int safeCol = Math.min(caretCol, lineText.length());
        int x = baseVisualXForColumn(safeLine, lineText, safeCol, fm);
        int yTop = yOfBufferLine(safeLine);
        int yBottom = yTop + lineHeight;
        SignatureHelpPopup popup = getOrCreateSignatureHelpPopup();
        popup.show(help, x, yTop, yBottom);
    }

    public void hideSignatureHelp() {
        signatureHelpVersion.incrementAndGet();
        if (signatureHelpPopup != null) signatureHelpPopup.hide();
    }



    public abstract boolean canShowPopups();
}
