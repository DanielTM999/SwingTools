package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.codelens.CodeLens;
import dtm.stools.component.panels.editor.code.codelens.CodeLensItem;
import dtm.stools.component.panels.editor.code.codelens.CodeLensPlacement;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.listeners.LineColorChangeListener;
import dtm.stools.component.panels.editor.code.prototype.LineColorInfo;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public abstract class CodeEditorTextAreaGeometry extends CodeEditorTextAreaState {

    protected CodeEditorTextAreaGeometry(TextBuffer buffer) {
        super(buffer);
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

    protected void replayHistoryStyledRanges(List<TextBuffer.AppliedChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        for (TextBuffer.AppliedChange change : changes) {
            int removedLength = change.removedText() == null ? 0 : change.removedText().length();
            int insertedLength = change.insertedText() == null ? 0 : change.insertedText().length();
            if (removedLength == 0 && insertedLength == 0) {
                continue;
            }
            shiftStyledRangesForEdit(change.offset(), removedLength, insertedLength);
        }

        clearGhostText();
        suppressHoverWhileEditing();
        invalidateStyledRangesIndex();
    }

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
            lineColors.put(line, new LineColorInfoInternal(background, foreground, false));
            fireLineColorAdded(line, background, foreground);
        }
        repaint();
    }

    public void setLinesColor(Collection<Integer> lines, Color color, Color foreground) {
        if(lines == null) return;
        for(int line : lines){
            lineColors.put(line, new LineColorInfoInternal(color, foreground, false));
            fireLineColorAdded(line, color, foreground);
        }
        repaint();
    }

    public void setLineColor(int line, Color background, Color foreground) {
        lineColors.put(line, new LineColorInfoInternal(background, foreground, false));
        fireLineColorAdded(line, background, foreground);
        repaint();
    }

    public void setPriorityLineColor(int line, Color color) {
        setPriorityLineColor(line, color, null);
    }

    public void setPriorityLineColor(int line, Color background, Color foreground) {
        lineColors.put(line, new LineColorInfoInternal(background, foreground, true));
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

    public LineColorInfo getLineColor(int line) {
        LineColorInfoInternal colorInfoInternal = lineColors.get(line);
        if (colorInfoInternal == null) return null;
        return new LineColorInfo() {
            @Override
            public Color getBackgroundColor() {
                return colorInfoInternal.background();
            }

            @Override
            public Color getForegroundColor() {
                return colorInfoInternal.foreground();
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
        FontMetrics fm = fontMetricsFor(getFont());
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
        Rectangle caretBounds = new Rectangle(cx, cy - extra, CARET_WIDTH, lineHeight + extra);

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

    protected int[] positionFromPoint(int mx, int my) {
        FontMetrics fm = fontMetricsFor(getFont());
        int line = bufferLineAtY(my);
        String lineText = buffer.lineAt(line);
        InlayHint transparentHint = mouseTransparentInlayHintAt(line, mx, fm);
        if (transparentHint != null) {
            return new int[]{line, inlayHintColumn(transparentHint, lineText)};
        }

        int[] best = {0, Math.abs(mx - (visualXForColumn(line, lineText, 0, fm)))};
        int endX = forEachLineRun(
                line,
                lineText,
                lineText.length(),
                getFont(),
                fm,
                true,
                false,
                (startCol, endCol, visualCol, run, style, font, runFm, x, runWidth) -> {
                    for (int c = startCol; c <= endCol; c++) {
                        int cx = (c == startCol)
                                ? x
                                : x + runFm.stringWidth(expandTabs(lineText.substring(startCol, c), visualCol));
                        int dist = Math.abs(mx - (cx + ghostPushWidthBeforeColumn(line, c, fm)));
                        if (dist < best[1]) {
                            best[1] = dist;
                            best[0] = c;
                        }
                    }
                    return true;
                });
        int endDist = Math.abs(mx - (endX + ghostPushWidthBeforeColumn(line, lineText.length(), fm)));
        if (endDist < best[1]) {
            best[1] = endDist;
            best[0] = lineText.length();
        }
        return new int[]{line, best[0]};
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

    protected FontMetrics fontMetricsFor(Font font) {
        Graphics2D graphics = activePaintGraphics;
        Map<Font, FontMetrics> paintMetrics = activePaintFontMetrics;
        if (graphics != null && paintMetrics != null) {
            return paintMetrics.computeIfAbsent(font, graphics::getFontMetrics);
        }
        return getFontMetrics(font);
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        lastPaintFontRenderContext = null;
        invalidateGeometry();
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
        FontMetrics fm = fontMetricsFor(getFont());
        Container parent = getParent();
        int height = (parent instanceof JViewport) ? parent.getHeight() : getHeight();
        return Math.max(1, height / fm.getHeight());
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
            int currentLh = fontMetricsFor(getFont()).getHeight();
            if (currentLh == cachedLineHeight && cachedLineCount == buffer.lineCount()) {
                return;
            }
        }
        rebuildGeometryCache();
    }

    protected void rebuildGeometryCache() {
        int lh = fontMetricsFor(getFont()).getHeight();
        int n = buffer.lineCount();

        BitSet hidden = computeHiddenLines(n);
        Map<Integer, CodeLens> aboveMap = new LinkedHashMap<>();
        Map<Integer, CodeLens> inlineMap = new LinkedHashMap<>();
        if (codeLensesEnabled && !codeLenses.isEmpty()) {
            for (CodeLens lens : codeLenses) {
                int line = lens.line();
                if (line < 0 || line >= n) continue;
                if (lens.items().isEmpty()) continue;
                if (lens.placement() == CodeLensPlacement.ABOVE) {
                    aboveMap.merge(line, lens, CodeEditorTextArea::mergeCodeLenses);
                } else if (lens.placement() == CodeLensPlacement.INLINE) {
                    inlineMap.merge(line, lens, CodeEditorTextArea::mergeCodeLenses);
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
        FontMetrics fm = fontMetricsFor(getFont());
        int n = buffer.lineCount();
        int[] approximate = new int[n];
        int maxApproximate = 0;
        for (int i = 0; i < n; i++) {
            approximate[i] = textWidth(fm, buffer.lineAt(i), 0) + pushedInlayWidthForLine(i, fm);
            if (approximate[i] > maxApproximate) maxApproximate = approximate[i];
        }

        if (styledRanges.isEmpty() && (!inlayHintsEnabled || inlayHints.isEmpty()) && !hasGhostText()) {
            cachedMaxLineWidth = maxApproximate + TEXT_LEFT_MARGIN;
            return cachedMaxLineWidth;
        }

        int candidateThreshold = maxApproximate - maxApproximate / 4;
        int[] candidates = new int[Math.min(n, MAX_REMEASURED_LINES)];
        int candidateCount = 0;
        for (int i = 0; i < n; i++) {
            if (approximate[i] < candidateThreshold) continue;
            if (candidateCount < candidates.length) {
                int at = candidateCount++;
                while (at > 0 && approximate[candidates[at - 1]] < approximate[i]) {
                    candidates[at] = candidates[at - 1];
                    at--;
                }
                candidates[at] = i;
            } else if (approximate[i] > approximate[candidates[candidateCount - 1]]) {
                int at = candidateCount - 1;
                while (at > 0 && approximate[candidates[at - 1]] < approximate[i]) {
                    candidates[at] = candidates[at - 1];
                    at--;
                }
                candidates[at] = i;
            }
        }

        int max = maxApproximate + TEXT_LEFT_MARGIN;
        for (int c = 0; c < candidateCount; c++) {
            int i = candidates[c];
            String lineText = buffer.lineAt(i);
            int w = baseVisualXForColumn(i, lineText, lineText.length(), fm);
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

    protected static CodeLens mergeCodeLenses(CodeLens first, CodeLens second) {
        List<CodeLensItem> items = new ArrayList<>(first.items());
        items.addAll(second.items());
        return new CodeLens(first.line(), first.col() >= 0 ? first.col() : second.col(),
                first.placement(), items);
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
    public Dimension getPreferredSize() {
        FontMetrics fm = fontMetricsFor(getFont());
        int lineHeight = fm.getHeight();

        int width = getMaxLineWidth();
        int bottomPadding = lineHeight * 5;

        return new Dimension(
                width + CARET_WIDTH + caretScrollRightMargin,
                totalContentHeight() + bottomPadding
        );
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

    protected boolean isDarkBackground(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 128;
    }

    protected int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public abstract boolean hasGhostText();

    protected abstract void scheduleGhostIdleTimer();

    protected abstract int ghostReservedRows();

    public abstract boolean clearGhostText();

    abstract int forEachLineRun(int line,
                       String lineText,
                       int renderLength,
                       Font baseFont,
                       FontMetrics defaultFm,
                       boolean includeInlayPush,
                       boolean includeGhostPush,
                       LineRunVisitor visitor);

    public abstract boolean isLineHidden(int bufferLine);

    protected abstract void suppressHoverWhileEditing();

    protected abstract int pushedInlayWidthForLine(int line, FontMetrics fm);

    protected abstract int inlayHintColumn(InlayHint hint, String lineText);

    protected abstract int visualXForColumn(int line, String lineText, int col, FontMetrics fm);

    protected abstract int baseVisualXForColumn(int line, String lineText, int col, FontMetrics fm);

    public abstract int visualXForColumn(int line, int col);

    protected abstract int ghostPushWidthBeforeColumn(int line, int col, FontMetrics fm);

    protected abstract InlayHint mouseTransparentInlayHintAt(int line, int mouseX, FontMetrics fm);

    protected abstract int clampOffset(int offset);


}
