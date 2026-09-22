package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.WordHoverContext;
import dtm.stools.component.panels.editor.code.api.WordHoverStyle;
import dtm.stools.component.panels.editor.code.codelens.CodeLens;
import dtm.stools.component.panels.editor.code.codelens.CodeLensItem;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightPalette;
import dtm.stools.component.panels.editor.code.provider.*;
import dtm.stools.component.panels.editor.code.inlay.InlayHint;
import dtm.stools.component.panels.editor.code.multicaret.Caret;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.search.SearchMatch;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.util.*;
import java.util.List;

public abstract class CodeEditorTextAreaRender extends CodeEditorTextAreaGeometry {

    protected CodeEditorTextAreaRender(TextBuffer buffer) {
        super(buffer);
    }

    int forEachLineRun(int line,
                       String lineText,
                       int renderLength,
                       Font baseFont,
                       FontMetrics defaultFm,
                       boolean includeInlayPush,
                       boolean includeGhostPush,
                       LineRunVisitor visitor) {
        int lineOffset = buffer.offsetOfLine(line);
        int x = TEXT_LEFT_MARGIN;
        int col = 0;
        int visualCol = 0;

        List<InlayHint> pushHints = pushInlayHintsForLine(line, defaultFm);
        int pushHintIndex = 0;

        int ghostPushCol = (line == ghostAnchorLine && isGhostVisibleAtAnchor()) ? ghostAnchorCol : -1;
        boolean ghostPushApplied = false;

        while (col < renderLength) {
            while (pushHintIndex < pushHints.size()
                    && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= col) {
                if (includeInlayPush) x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
                pushHintIndex++;
            }
            if (ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= col) {
                if (includeGhostPush) x += ghostFirstSegmentWidth(defaultFm);
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
            FontMetrics fm = fontMetricsFor(font);
            int runWidth = fm.stringWidth(run);

            if (!visitor.visit(col, runEnd, visualCol, run, style, font, fm, x, runWidth)) {
                return x;
            }

            x += runWidth;
            visualCol += run.length();
            col = runEnd;
        }

        while (pushHintIndex < pushHints.size()
                && inlayHintColumn(pushHints.get(pushHintIndex), lineText) <= renderLength) {
            if (includeInlayPush) x += inlayHintWidth(defaultFm, pushHints.get(pushHintIndex));
            pushHintIndex++;
        }
        if (includeGhostPush && ghostPushCol >= 0 && !ghostPushApplied && ghostPushCol <= renderLength) {
            x += ghostFirstSegmentWidth(defaultFm);
        }
        return x;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Graphics2D previousPaintGraphics = activePaintGraphics;
        Map<Font, FontMetrics> previousPaintFontMetrics = activePaintFontMetrics;
        try {
            activePaintGraphics = g2;
            activePaintFontMetrics = new HashMap<>();
            paintEditor(g2);
        } finally {
            activePaintGraphics = previousPaintGraphics;
            activePaintFontMetrics = previousPaintFontMetrics;
            g2.dispose();
        }
    }

    private void paintEditor(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        updatePaintFontRenderContext(g2.getFontRenderContext());

        g2.setColor(defaultStyle.getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        Font baseFont = getFont();
        FontMetrics defaultFm = g2.getFontMetrics(baseFont);
        int lineHeight = defaultFm.getHeight();

        Rectangle clip = g2.getClipBounds();
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
            if (lineColor != null && !lineColor.priority()) {
                if (lineColor.background() != null) {
                    g2.setColor(lineColor.background());
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

        paintDocumentHighlights(g2, defaultFm, lineHeight);
        paintSelectedTextOccurrences(g2, defaultFm, lineHeight);

        if (hasSelection() || hasExtraSelections()) {
            paintSelection(g2, defaultFm, lineHeight);
        }

        for (int i = firstVisibleLine; i <= lastVisibleLine && i < totalLines; i++) {
            if (isLineHidden(i)) continue;
            LineColorInfoInternal lineColor = lineColors.get(i);
            if (lineColor == null || !lineColor.priority() || lineColor.background() == null) {
                continue;
            }
            g2.setColor(lineColor.background());
            g2.fillRect(0, yOfBufferLine(i), getWidth(), lineHeight);
        }

        paintSearchMatches(g2, defaultFm);

        paintIndentGuides(g2, defaultFm, lineHeight);

        codeLensItemBounds.clear();
        for (int i = firstVisibleLine; i <= lastVisibleLine && i < totalLines; i++) {
            if (isLineHidden(i)) continue;
            if (hasCodeLens(i)) {
                paintCodeLensRow(g2, defaultFm, baseFont, i, yOfCodeLensRow(i), lineHeight);
            }
            String lineText = buffer.lineAt(i);
            int ly = yOfBufferLine(i);

            int renderLength = lineText.length();
            if (shouldHideTrailingOpenForFold(i)) {
                int idx = lineText.length() - 1;
                while (idx >= 0 && Character.isWhitespace(lineText.charAt(idx))) idx--;
                if (idx >= 0) renderLength = idx;
            }

            final int lineIndex = i;
            int x = forEachLineRun(
                    i,
                    lineText,
                    renderLength,
                    baseFont,
                    defaultFm,
                    true,
                    true,
                    (startCol, endCol, visualCol, run, style, font, fm, runX, runWidth) -> {
                        g2.setFont(font);

                        if (style.getBackground() != null && style.getBackground() != defaultStyle.getBackground()) {
                            g2.setColor(style.getBackground());
                            g2.fillRect(runX, ly, runWidth, lineHeight);
                        }

                        LineColorInfoInternal lineInfo = lineColors.get(lineIndex);
                        Color fg = style.getForeground();
                        if (lineInfo != null && lineInfo.foreground() != null) {
                            fg = lineInfo.foreground();
                        }
                        g2.setColor(fg);
                        g2.drawString(run, runX, ly + fm.getAscent());

                        if (style.isUnderline()) {
                            int uy = ly + fm.getAscent() + 1;
                            g2.drawLine(runX, uy, runX + runWidth, uy);
                        }
                        return true;
                    });

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

    private void updatePaintFontRenderContext(FontRenderContext currentContext) {
        if (currentContext.equals(lastPaintFontRenderContext)) return;
        lastPaintFontRenderContext = currentContext;
        invalidateGeometry();
        revalidate();
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

    protected void paintDocumentHighlights(Graphics2D g2, FontMetrics fm, int lineHeight) {
        List<ResolvedDocumentHighlight> highlights = resolvedDocumentHighlights;
        if (!documentHighlightsEnabled || highlights.isEmpty()) return;

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

        DocumentHighlightPalette palette = documentHighlightPalette;
        for (ResolvedDocumentHighlight highlight : highlights) {
            if (highlight.endOffset() <= visibleStartOffset) continue;
            if (highlight.startOffset() >= visibleEndOffset) break;
            paintSelectionRange(
                    g2,
                    fm,
                    lineHeight,
                    highlight.startOffset(),
                    highlight.endOffset(),
                    palette.colorFor(highlight.kind()));
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

            int x1 = baseVisualXForColumn(i, lineText, colStart, fm);
            int x2 = baseVisualXForColumn(i, lineText, colEnd, fm);

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
            int x = (i == 0) ? startX : TEXT_LEFT_MARGIN;
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
            g2.fillRect(cx, cy, CARET_WIDTH, lineHeight);
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
                int guideX = TEXT_LEFT_MARGIN + level * unit * charWidth;
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

    protected void drawGuide(Graphics2D g2, int x, int yTop, int lineHeight, Color color) {
        g2.setColor(color);
        g2.fillRect(x, yTop, 1, lineHeight);
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
            g2.fillRect(cx, cy, CARET_WIDTH, lineHeight);
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
                int x1 = baseVisualXForColumn(line, lineText, colStart, fm);
                int x2 = baseVisualXForColumn(line, lineText, colEnd, fm);
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
        paintCodeLensItems(g2, baseFont, lens,
                aboveCodeLensXStart(lens, bufferLine, defaultFm), yTop, lineHeight);
    }

    protected int aboveCodeLensXStart(CodeLens lens, int bufferLine, FontMetrics defaultFm) {
        if (lens.col() <= 0) return TEXT_LEFT_MARGIN;
        String lineText = buffer.lineAt(bufferLine);
        int safeCol = Math.min(lens.col(), lineText.length());
        return baseVisualXForColumn(bufferLine, lineText, safeCol, defaultFm);
    }

    protected void paintInlineCodeLens(Graphics2D g2, FontMetrics defaultFm, Font baseFont,
                                       int bufferLine, int yTop, int lineHeight) {
        CodeLens lens = inlineCodeLensAtLine(bufferLine);
        if (lens == null) return;
        paintCodeLensItems(g2, baseFont, lens,
                inlineCodeLensXStart(lens, bufferLine, defaultFm), yTop, lineHeight);
    }

    protected int inlineCodeLensXStart(CodeLens lens, int bufferLine, FontMetrics defaultFm) {
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
            xStart = baseVisualXForColumn(bufferLine, renderedLineText, renderedLineText.length(), defaultFm);
            atOrPastEnd = true;
        } else {
            int safeCol = Math.min(lens.col(), renderedLineText.length());
            xStart = baseVisualXForColumn(bufferLine, renderedLineText, safeCol, defaultFm);
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

        return xStart;
    }

    protected Font codeLensFont(Font baseFont) {
        return baseFont.deriveFont(Math.max(8f, baseFont.getSize2D() * codeLensFontScale));
    }

    protected Font codeLensItemFont(Font baseLensFont, CodeLensItem item) {
        int fontStyle = Font.PLAIN;
        if (item.isBold()) fontStyle |= Font.BOLD;
        if (item.isItalic()) fontStyle |= Font.ITALIC;
        return fontStyle == Font.PLAIN ? baseLensFont : baseLensFont.deriveFont(fontStyle);
    }

    protected List<CodeLensItemBounds> codeLensBoundsAtY(int yMouse) {
        if (!codeLensesEnabled || yMouse < 0) return List.of();
        ensureGeometry();
        int line = bufferLineAtY(yMouse);
        if (line < 0 || line >= buffer.lineCount() || isLineHidden(line)) return List.of();
        CodeLens above = aboveCodeLensAtLine(line);
        CodeLens inline = inlineCodeLensAtLine(line);
        if (above == null && inline == null) return List.of();

        Font baseFont = getFont();
        FontMetrics defaultFm = fontMetricsFor(baseFont);
        int lineHeight = defaultFm.getHeight();
        List<CodeLensItemBounds> bounds = new ArrayList<>();
        if (above != null) {
            bounds.addAll(layoutCodeLensItems(above, baseFont,
                    aboveCodeLensXStart(above, line, defaultFm),
                    yOfCodeLensRow(line), lineHeight));
        }
        if (inline != null) {
            bounds.addAll(layoutCodeLensItems(inline, baseFont,
                    inlineCodeLensXStart(inline, line, defaultFm),
                    yOfBufferLine(line), lineHeight));
        }
        return bounds;
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
            int baseX = glyphVisualXForColumn(line, lineText, col, fm)
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
        return measureVisualX(line, lineText, col, fm, true);
    }

    protected int glyphVisualXForColumn(int line, String lineText, int col, FontMetrics fm) {
        return measureVisualX(line, lineText, col, fm, false);
    }

    protected int measureVisualX(int line, String lineText, int col, FontMetrics fm, boolean includeInlayPush) {
        int safeCol = Math.min(Math.max(0, col), lineText.length());
        int[] resolved = {-1};
        int endX = forEachLineRun(
                line,
                lineText,
                lineText.length(),
                getFont(),
                fm,
                includeInlayPush,
                false,
                (startCol, endCol, visualCol, run, style, font, runFm, x, runWidth) -> {
                    if (safeCol >= endCol) return true;
                    if (safeCol <= startCol) {
                        resolved[0] = x;
                        return false;
                    }
                    String prefix = expandTabs(lineText.substring(startCol, safeCol), visualCol);
                    resolved[0] = x + runFm.stringWidth(prefix);
                    return false;
                });
        return resolved[0] >= 0 ? resolved[0] : endX;
    }

    public int visualXForColumn(int line, int col) {
        int safeLine = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        String lineText = buffer.lineAt(safeLine);
        return visualXForColumn(safeLine, lineText, col, fontMetricsFor(getFont()));
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
            int x = glyphVisualXForColumn(line, lineText, col, fm) + pushedWidth;
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
                int x1 = baseVisualXForColumn(line, lineText, colStart, fm);
                int x2 = baseVisualXForColumn(line, lineText, colEnd, fm);
                int ly = yOfBufferLine(line);
                g2.fillRect(x1, ly, Math.max(1, x2 - x1), lineHeight);
            }
        }
    }

    protected abstract int caretOffset();

    public abstract boolean hasSelection();

    protected abstract int getSelectionStart();

    protected abstract int getSelectionEnd();

    protected abstract boolean hasExtraSelections();

    protected abstract int extraSelectionStart(Caret c);

    protected abstract int extraSelectionEnd(Caret c);

    public abstract boolean isFoldAnchor(int bufferLine);

    protected abstract boolean shouldHideTrailingOpenForFold(int bufferLine);

    public abstract FoldRegion getFoldRegionStartingAt(int bufferLine);

    protected List<CodeLensItemBounds> layoutCodeLensItems(CodeLens lens, Font baseFont,
                                                          int xStart, int yTop, int lineHeight) {
        if (lens == null || lens.items().isEmpty()) return List.of();
        Font baseLensFont = codeLensFont(baseFont);
        List<CodeLensItemBounds> bounds = new ArrayList<>(lens.items().size());
        int x = xStart;
        for (int idx = 0; idx < lens.items().size(); idx++) {
            CodeLensItem item = lens.items().get(idx);
            if (item.getText() == null || item.getText().isEmpty()) continue;
            int w = fontMetricsFor(codeLensItemFont(baseLensFont, item)).stringWidth(item.getText());
            bounds.add(new CodeLensItemBounds(lens, idx, x, yTop, w, lineHeight));
            x += w;
            if (idx < lens.items().size() - 1) {
                x += codeLensItemSpacing;
            }
        }
        return bounds;
    }

    protected void paintCodeLensItems(Graphics2D g2, Font baseFont, CodeLens lens,
                                      int xStart, int yTop, int lineHeight) {
        List<CodeLensItemBounds> bounds = layoutCodeLensItems(lens, baseFont, xStart, yTop, lineHeight);
        if (bounds.isEmpty()) return;

        Font baseLensFont = codeLensFont(baseFont);
        FontMetrics fm = fontMetricsFor(baseLensFont);

        Color baseFg = defaultStyle.getForeground();
        Color defaultFg = codeLensForeground != null
                ? codeLensForeground
                : new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 150);

        int yBaseline = yTop + (lineHeight - fm.getHeight()) / 2 + fm.getAscent();

        for (int i = 0; i < bounds.size(); i++) {
            CodeLensItemBounds b = bounds.get(i);
            CodeLensItem item = lens.items().get(b.itemIndex);
            g2.setFont(codeLensItemFont(baseLensFont, item));

            Color fg = item.getForeground() != null ? item.getForeground() : defaultFg;
            g2.setColor(fg);
            g2.drawString(item.getText(), b.x, yBaseline);

            if (item.isUnderline()) {
                int uy = yBaseline + 1;
                g2.drawLine(b.x, uy, b.x + b.w, uy);
            }

            if (i < bounds.size() - 1) {
                int sep = Math.max(4, codeLensItemSpacing / 2);
                int sepY = yTop + lineHeight / 2;
                int sepX = b.x + b.w + sep;
                g2.setColor(new Color(baseFg.getRed(), baseFg.getGreen(), baseFg.getBlue(), 60));
                g2.drawLine(sepX, sepY - 3, sepX, sepY + 3);
            }
        }

        codeLensItemBounds.addAll(bounds);
        g2.setFont(baseFont);
    }

    protected abstract String[] findFoldSeparatorsForRegion(FoldRegion region);
}
