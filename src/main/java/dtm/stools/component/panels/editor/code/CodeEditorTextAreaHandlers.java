package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import dtm.stools.component.panels.editor.code.ghost.GhostTextContext;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.prototype.LineColorInfo;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;

public abstract class CodeEditorTextAreaHandlers extends CodeEditorTextAreaInput {

    protected CodeEditorTextAreaHandlers(TextBuffer buffer) {
        super(buffer);
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
                String closingTag = closingMarkupTagFor(c);
                beginCompoundEdit();
                try {
                    if (closingTag == null) {
                        insertAtAllCarets(String.valueOf(c), overwriteMode, 1);
                    } else {
                        insertAtAllCarets(c + closingTag, false, 1);
                    }
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

            if (handleLinkedRenameKey(e)) {
                repaint();
                return;
            }

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
                if (hasGhostText() && matchesKeyStroke(e, ghostTextAcceptKeyStroke)) {
                    if (readOnly) {
                        clearGhostText();
                    } else {
                        acceptGhostText();
                    }
                    e.consume();
                    return;
                }
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
                    if (e.isAltDown() || e.isAltGraphDown() || e.isControlDown()
                            || e.isMetaDown() || e.isShiftDown()) {
                        return;
                    }
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

        protected String closingMarkupTagFor(char c) {
            if (!autoCloseMarkupTags || c != '>' || overwriteMode || !extraCarets.isEmpty()) {
                return null;
            }
            int offset = caretOffset();
            int length = buffer.length();
            return MarkupTagCloser.closingTagFor(
                    buffer.substring(Math.max(0, offset - MARKUP_TAG_SCAN_BEFORE), offset),
                    buffer.substring(offset, Math.min(length, offset + MARKUP_TAG_SCAN_AFTER)));
        }
    }

    private static final int MARKUP_TAG_SCAN_BEFORE = 8_192;
    private static final int MARKUP_TAG_SCAN_AFTER = 512;

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
            setFont(CodeEditorTextAreaHandlers.this.getFont());
            setOpaque(true);
            setBackground(bg);
            setForeground(fg);
        }

        private String hiddenExtraText() {
            return text("fold.preview.moreLines", "… (+{lines} more lines)")
                    .replace("{lines}", String.valueOf(hiddenExtra));
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
                maxWidth = Math.max(maxWidth, fm.stringWidth(hiddenExtraText()));
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
                    g2.drawString(hiddenExtraText(), padX, y + ascent);
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
            onLinkedRenameFocusLost(e);
            caretVisible = false;
            caretTimer.stop();
            stopGhostIdleTimer();
            repaint();
        }
    }

    protected void showFoldPreviewAt(int line, int mouseX, int mouseY) {
        if (!foldPreviewOnHoverEnabled) {
            hideFoldPreview();
            return;
        }
        if (!canShowPopups()) {
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

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (foldPreviewWindow != null && foldPreviewOwnerWindow != owner) {
            foldPreviewWindow.dispose();
            foldPreviewWindow = null;
        }
        if (foldPreviewWindow == null) {
            foldPreviewOwnerWindow = owner;
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

    protected abstract boolean handleLinkedRenameKey(KeyEvent e);

    protected abstract void onLinkedRenameFocusLost(FocusEvent e);
}
