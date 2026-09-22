package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.multicaret.Caret;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public abstract class CodeEditorTextAreaDocument extends CodeEditorTextAreaRender {

    protected CodeEditorTextAreaDocument(TextBuffer buffer) {
        super(buffer);
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
            abortLinkedRename();
            overwriteMode = false;
        }
        fireStateChangedIfNeeded();
        repaint();
    }

    protected void insertText(int offset, String text) {
        if (readOnly || text == null || text.isEmpty()) return;
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        int linesBefore = buffer.lineCount();
        int lineAtInsert = buffer.lineOfOffset(Math.min(offset, buffer.length()));
        buffer.insert(offset, text);
        onSnippetInsert(offset, text.length());
        onLinkedRenameInsert(offset, text.length());
        final String textFinal = text;
        documentEditListeners.forEach(l -> l.onInsert(offset, textFinal));
        documentEditListeners.forEach(DocumentEditListener::onTextChanged);
        fireStateChangedIfNeeded();
        int addedLines = buffer.lineCount() - linesBefore;
        if (addedLines > 0) {
            fireLinesInserted(lineAtInsert + 1, addedLines);
        }
        scheduleFoldRefresh();
    }

    protected void deleteText(int start, int end) {
        if (readOnly || start >= end) return;
        String removed = buffer.substring(start, end);
        int linesBefore = buffer.lineCount();
        int lineAtDelete = buffer.lineOfOffset(Math.min(start, buffer.length()));
        buffer.delete(start, end);
        onSnippetDelete(start, end);
        onLinkedRenameDelete(start, end);
        documentEditListeners.forEach(l -> l.onDelete(start, removed));
        documentEditListeners.forEach(DocumentEditListener::onTextChanged);
        fireStateChangedIfNeeded();
        int removedLines = linesBefore - buffer.lineCount();
        if (removedLines > 0) {
            fireLinesRemoved(lineAtDelete + 1, removedLines);
        }
        scheduleFoldRefresh();
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
        abortLinkedRename();
        int linesBefore = buffer.lineCount();
        TextBuffer.EditResult result = buffer.undoEdit();
        if (result.caretOffset() >= 0) {
            restoreEditorState(result.state(), result.caretOffset());
            replayHistoryStyledRanges(result.changes());
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
        abortLinkedRename();
        int linesBefore = buffer.lineCount();
        TextBuffer.EditResult result = buffer.redoEdit();
        if (result.caretOffset() >= 0) {
            restoreEditorState(result.state(), result.caretOffset());
            replayHistoryStyledRanges(result.changes());
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
        abortLinkedRename();
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
        if (!oldText.equals(newText)) {
            lastHighlightTokens = null;
            lastHighlightText = null;
            syntaxHighlightRescueAttempts = 0;
            applySyntaxHighlight();
        }
        fireStateChangedIfNeeded();
        revalidate();
        repaint();
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

    protected boolean clipboardHasText() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard()
                    .isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            return false;
        }
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

    public abstract void clearSnippetSession();

    protected abstract void onSnippetInsert(int offset, int insertedLen);

    protected abstract void onSnippetDelete(int start, int end);

    protected abstract void onLinkedRenameInsert(int offset, int insertedLen);

    protected abstract void onLinkedRenameDelete(int start, int end);

    protected abstract void abortLinkedRename();

    protected abstract void hideAutoCompletePopup();

    protected abstract void fireStateChangedIfNeeded();

    protected abstract void unfoldToRevealCaret();

    protected abstract void recomputeFoldRegions();

    protected abstract void recomputeFoldRegions(boolean preserveFoldedByLine);

    protected abstract void scheduleFoldRefresh();

    public abstract void applySyntaxHighlight();

    protected abstract void fireBookmarkChanged(int line, boolean added);

    protected abstract void fireBookmarksChanged();
}
