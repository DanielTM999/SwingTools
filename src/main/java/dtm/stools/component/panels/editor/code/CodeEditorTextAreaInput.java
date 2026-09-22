package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.WordCaretChangeEvent;
import dtm.stools.component.panels.editor.code.api.WordCaretChangeListener;
import dtm.stools.component.panels.editor.code.api.WordClickEvent;
import dtm.stools.component.panels.editor.code.api.WordHoverStyle;
import dtm.stools.component.panels.editor.code.codelens.CodeLensClickEvent;
import dtm.stools.component.panels.editor.code.codelens.CodeLensItem;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.listeners.HoverListener;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

public abstract class CodeEditorTextAreaInput extends CodeEditorTextAreaAnalysis {

    protected CodeEditorTextAreaInput(TextBuffer buffer) {
        super(buffer);
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

    protected void updateCodeLensHover(int mx, int my) {
        List<CodeLensItemBounds> candidates = codeLensBoundsAtY(my);
        if (candidates.isEmpty()) {
            restoreCodeLensCursor();
            setToolTipText(null);
            return;
        }
        for (CodeLensItemBounds b : candidates) {
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
        if (!codeLensesEnabled || e.getButton() != MouseEvent.BUTTON1) {
            return false;
        }
        int mx = e.getX();
        int my = e.getY();
        for (CodeLensItemBounds b : codeLensBoundsAtY(my)) {
            if (mx < b.x || mx > b.x + b.w || my < b.y || my > b.y + b.h) continue;
            if (b.itemIndex >= b.lens.items().size()) return false;
            CodeLensItem item = b.lens.items().get(b.itemIndex);
            if (item.getOnClick() == null) return false;
            try {
                item.getOnClick().accept(new CodeLensClickEvent(b.lens, item, b.lens.line(), e));
            } catch (Exception failure) {
                CODE_LENS_LOG.log(Level.WARNING, "code lens click failed", failure);
            }
            e.consume();
            return true;
        }
        return false;
    }

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

    protected boolean isFoldPlaceholderAt(int mouseX, int mouseY) {
        if (!foldingEnabled) return false;
        if (isInCodeLensRow(mouseY)) return false;
        FontMetrics fm = fontMetricsFor(getFont());
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
        int lineEndX = baseVisualXForColumn(line, renderedLineText, renderedLineText.length(), fm);
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
        int version = contextMenuVersion.incrementAndGet();
        if (contextMenuProvider == null) {
            JPopupMenu menu = createDefaultContextMenu();
            if (menu == null) return false;
            if (!canShowPopups()) return false;
            activeContextMenu = menu;
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
                SwingUtilities.invokeLater(() -> {
                    if (version != contextMenuVersion.get()) return;
                    if (!canShowPopups()) return;
                    activeContextMenu = popup;
                    popup.show(this, x, y);
                });
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







    public abstract void moveLineUp();

    public abstract void moveLineDown();

    public abstract void duplicateLineUp();

    public abstract void duplicateLineDown();



    protected abstract void showFoldPreviewAt(int line, int mouseX, int mouseY);
}
