package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.editor.code.gutter.layer.LineMarkerLayer;
import dtm.stools.component.panels.editor.code.listeners.DocumentEditListener;
import dtm.stools.component.panels.editor.code.prototype.LineColorInfo;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeEditorMinimap extends JComponent {

    @Getter
    @Setter
    private int minimapWidth = 45;

    @Getter
    @Setter
    private float scale = 0.12f;

    @Getter
    @Setter
    private int textAlpha = 180;

    @Getter
    @Setter
    private Color viewportColor = new Color(128, 128, 128, 50);

    @Getter
    @Setter
    private Color viewportBorderColor = new Color(128, 128, 128, 100);

    @Getter
    @Setter
    private Color hoverPreviewBorderColor = new Color(128, 128, 128, 150);

    @Getter
    @Setter
    private Color hoverPreviewBackground;

    @Getter
    @Setter
    private int hoverPreviewLines = 10;

    @Getter
    @Setter
    private float hoverPreviewScale = 0.65f;

    @Getter
    @Setter
    private boolean hoverPreviewEnabled = true;

    @Getter
    @Setter
    private boolean goToPositionEnabled = true;

    @Getter
    @Setter
    private boolean previewMirrorEnabled = true;

    public enum VisibilityMode {
        ALWAYS,
        SCROLLBAR_PREVIEW
    }

    @Getter
    private VisibilityMode visibilityMode = VisibilityMode.SCROLLBAR_PREVIEW;

    private final CodeEditorTextArea textArea;
    private final JScrollPane scrollPane;

    private BufferedImage cacheImage;
    private boolean cacheDirty = true;
    private boolean dragging = false;
    private final ExecutorService rebuildExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CodeEditorMinimap-Rebuild");
                t.setDaemon(true);
                return t;
            });
    private volatile Future<?> currentRebuild;
    private final AtomicInteger rebuildVersion = new AtomicInteger();

    private int hoverY = -1;
    private JWindow previewWindow;

    public CodeEditorMinimap(CodeEditorTextArea textArea, JScrollPane scrollPane) {
        this.textArea = textArea;
        this.scrollPane = scrollPane;
        setOpaque(true);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                hidePreview();
                scrollToY(e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) scrollToY(e.getY());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                hoverY = e.getY();
                if (hoverPreviewEnabled && !dragging) {
                    showPreview(e);
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverY = -1;
                hidePreview();
                repaint();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        textArea.addDocumentEditListener(new DocumentEditListener() {
            @Override public void onInsert(int offset, String text) {}
            @Override public void onDelete(int offset, String removed) {}
            @Override public void onTextChanged() { invalidateCache(); }
        });

        scrollPane.getViewport().addChangeListener(e -> repaint());

        JScrollBar vsb = scrollPane.getVerticalScrollBar();
        if (vsb != null) {
            MouseAdapter sbHandler = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (!isMinimapMasterEnabled()) return;
                    if (visibilityMode != VisibilityMode.SCROLLBAR_PREVIEW) return;
                    if (!hoverPreviewEnabled) return;
                    if (!isContentScrollable()) {
                        hidePreview();
                        return;
                    }
                    if (isOnScrollbarThumb(vsb, e.getY())) {
                        hidePreview();
                        return;
                    }
                    showPreviewFromScrollbar(vsb, e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hidePreview();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    hidePreview();
                    if(!goToPositionEnabled) return;
                    if (!isContentScrollable()) return;
                    int clickY = e.getY();
                    int height = vsb.getHeight();

                    BoundedRangeModel model = vsb.getModel();

                    int min = model.getMinimum();
                    int max = model.getMaximum();
                    int extent = model.getExtent();

                    float ratio = (float) clickY / height;

                    int range = max - min - extent;

                    int newValue = (int) (min + ratio * range);

                    vsb.setValue(newValue);
                }
            };
            vsb.addMouseListener(sbHandler);
            vsb.addMouseMotionListener(sbHandler);
        }

        applyVisibilityState();
    }

    public void setVisibilityMode(VisibilityMode mode) {
        if (mode == null) mode = VisibilityMode.SCROLLBAR_PREVIEW;
        this.visibilityMode = mode;
        applyVisibilityState();
    }

    public void refreshVisibilityState() {
        applyVisibilityState();
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void applyVisibilityState() {
        boolean masterEnabled = isMinimapMasterEnabled();
        boolean shouldShow = masterEnabled && (visibilityMode == VisibilityMode.ALWAYS);
        setVisible(shouldShow);

        if (shouldShow && scrollPane != null && getParent() != scrollPane) {
            scrollPane.add(this);
        }

        if (scrollPane != null) {
            scrollPane.invalidate();
            scrollPane.validate();
            scrollPane.repaint();
        }

        if (shouldShow) {

            cacheImage = null;
            cacheDirty = true;
            repaint();
        }
    }

    private boolean isMinimapMasterEnabled() {
        return !(scrollPane instanceof CodeEditorScrollPane sp) || sp.isMinimapEnabled();
    }

    private boolean isContentScrollable() {
        if (scrollPane == null) return false;
        JViewport vp = scrollPane.getViewport();
        if (vp == null) return false;
        return textArea.getPreferredSize().height > vp.getHeight();
    }

    private boolean isOnScrollbarThumb(JScrollBar sb, int mouseY) {
        int max = sb.getMaximum() - sb.getMinimum();
        int extent = sb.getVisibleAmount();
        if (max <= extent) return false;

        int trackH = sb.getHeight();
        int thumbH = Math.max(20, (int) ((float) extent / max * trackH));
        int value = sb.getValue() - sb.getMinimum();
        int thumbStart = (int) ((float) value / (max - extent) * (trackH - thumbH));
        int thumbEnd = thumbStart + thumbH;
        return mouseY >= thumbStart && mouseY <= thumbEnd;
    }

    private int lineAtScrollbarY(JScrollBar sb, int y) {
        int totalHeight = textArea.getPreferredSize().height;
        if (totalHeight <= 0 || sb.getHeight() <= 0) return 0;
        float ratio = (float) y / sb.getHeight();
        int yInTextArea = (int) (ratio * totalHeight);
        return textArea.bufferLineAtY(yInTextArea);
    }

    public void invalidateCache() {
        cacheDirty = true;
        repaint();
    }

    private void scrollToY(int mouseY) {
        JViewport viewport = scrollPane.getViewport();
        int maxScroll = textArea.getPreferredSize().height - viewport.getHeight();
        if (maxScroll <= 0) return;
        float ratio = (float) mouseY / getHeight();
        int scrollY = (int) (ratio * textArea.getPreferredSize().height - viewport.getHeight() / 2);
        scrollY = Math.clamp(scrollY, 0, maxScroll);
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, scrollY));
    }

    private int lineAtY(int mouseY) {
        int totalHeight = textArea.getPreferredSize().height;
        if (totalHeight <= 0) return 0;
        float ratio = (float) mouseY / getHeight();
        int yInTextArea = (int) (ratio * totalHeight);
        return textArea.bufferLineAtY(yInTextArea);
    }

    private void showPreview(MouseEvent e) {
        showPreviewAt(lineAtY(e.getY()), this, e.getY());
    }

    private void showPreviewFromScrollbar(JScrollBar sb, MouseEvent e) {
        showPreviewAt(lineAtScrollbarY(sb, e.getY()), sb, e.getY());
    }

    private CodeEditorGutter resolveGutter() {
        return (scrollPane instanceof CodeEditorScrollPane sp) ? sp.getGutter() : null;
    }

    private void showPreviewAt(int centerLine, Component anchor, int anchorY) {
        int halfLines = hoverPreviewLines / 2;
        int startLine = Math.max(0, centerLine - halfLines);
        int endLine = Math.min(textArea.getBuffer().lineCount() - 1, centerLine + halfLines);

        Font baseFont = textArea.getFont();
        float previewFontSize = Math.max(6f, baseFont.getSize2D() * hoverPreviewScale);
        Font previewFont = baseFont.deriveFont(previewFontSize);
        FontMetrics fm = textArea.getFontMetrics(previewFont);

        int lineHeight = fm.getHeight();
        int previewHeight = (endLine - startLine + 1) * lineHeight + 8;

        CodeEditorGutter gutter = previewMirrorEnabled ? resolveGutter() : null;
        boolean showLineNumbers = gutter != null && gutter.isLineNumberEnabled();
        LineMarkerLayer marker = gutter != null ? gutter.getLayer(LineMarkerLayer.class) : null;
        int stripeWidth = marker != null ? marker.getStripeWidth() + 2 : 0;
        int lineNumberWidth = showLineNumbers
                ? fm.stringWidth(Integer.toString(endLine + 1)) + 8 : 0;
        int gutterWidth = (gutter != null) ? stripeWidth + lineNumberWidth : 0;

        int textWidth = 4;
        for (int i = startLine; i <= endLine; i++) {
            textWidth = Math.max(textWidth,
                    fm.stringWidth(textArea.getBuffer().lineAt(i)) + 12);
        }
        textWidth = Math.min(textWidth, 400);
        int previewWidth = gutterWidth + textWidth;

        BufferedImage img = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color bg = hoverPreviewBackground != null ? hoverPreviewBackground : textArea.getDefaultStyle().getBackground();
        g2.setColor(bg);
        g2.fillRect(gutterWidth, 0, textWidth, previewHeight);

        TextStyle defaultStyle = textArea.getDefaultStyle();

        if (previewMirrorEnabled) {
            for (int i = startLine; i <= endLine; i++) {
                LineColorInfo lineColor = textArea.getLineColor(i);
                if (lineColor != null && lineColor.getBackgroundColor() != null) {
                    int y = (i - startLine) * lineHeight + 4;
                    g2.setColor(lineColor.getBackgroundColor());
                    g2.fillRect(gutterWidth, y, textWidth, lineHeight);
                }
            }
        }

        if (gutter != null && gutterWidth > 0) {
            g2.setColor(gutter.getBackground());
            g2.fillRect(0, 0, gutterWidth, previewHeight);
            for (int i = startLine; i <= endLine; i++) {
                int y = (i - startLine) * lineHeight + 4;
                if (marker != null) {
                    Color markerColor = marker.getLineColor(i);
                    if (markerColor != null) {
                        g2.setColor(markerColor);
                        g2.fillRect(0, y, Math.max(1, stripeWidth - 2), lineHeight);
                    }
                }
                if (showLineNumbers && gutter.getLineNumberColor() != null) {
                    String num = Integer.toString(i + 1);
                    g2.setFont(previewFont);
                    g2.setColor(gutter.getLineNumberColor());
                    g2.drawString(num,
                            gutterWidth - 4 - fm.stringWidth(num),
                            y + fm.getAscent());
                }
            }
            if (gutter.getBorderColor() != null) {
                g2.setColor(gutter.getBorderColor());
                g2.drawLine(gutterWidth - 1, 0, gutterWidth - 1, previewHeight);
            }
        }

        for (int i = startLine; i <= endLine; i++) {
            String lineText = textArea.getBuffer().lineAt(i);
            int lineOffset = textArea.getBuffer().offsetOfLine(i);
            int y = (i - startLine) * lineHeight + 4;
            float x = gutterWidth + 4;

            LineColorInfo lineColor = previewMirrorEnabled ? textArea.getLineColor(i) : null;
            Color lineFg = lineColor != null ? lineColor.getForegroundColor() : null;

            int col = 0;
            while (col < lineText.length()) {
                TextStyle style = getStyleAt(lineOffset + col);
                int runEnd = col + 1;
                while (runEnd < lineText.length() && getStyleAt(lineOffset + runEnd) == style) {
                    runEnd++;
                }

                String run = lineText.substring(col, runEnd);
                Font runFont = deriveFont(previewFont, style);
                g2.setFont(runFont);
                FontMetrics rfm = g2.getFontMetrics(runFont);

                Color fg = lineFg != null ? lineFg : style.getForeground();
                if (fg == null) fg = defaultStyle.getForeground();
                g2.setColor(fg);
                g2.drawString(run, x, y + rfm.getAscent());
                x += rfm.stringWidth(run);
                col = runEnd;
            }
        }

        g2.setColor(hoverPreviewBorderColor);
        g2.drawRect(0, 0, previewWidth - 1, previewHeight - 1);
        g2.dispose();

        if (previewWindow == null) {
            Window owner = SwingUtilities.getWindowAncestor(anchor);
            previewWindow = new JWindow(owner);
            previewWindow.setAlwaysOnTop(true);
        }

        JLabel label = new JLabel(new ImageIcon(img));
        previewWindow.getContentPane().removeAll();
        previewWindow.getContentPane().add(label);
        previewWindow.pack();

        Point screenPos;
        try {
            screenPos = anchor.getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            return;
        }
        int px = screenPos.x - previewWindow.getWidth() - 4;
        int py = screenPos.y + anchorY - previewHeight / 2;
        int pyMax = screenPos.y + Math.max(0, anchor.getHeight() - previewHeight);
        py = Math.clamp(py, screenPos.y, pyMax);
        previewWindow.setLocation(px, py);
        previewWindow.setVisible(true);
    }

    private void hidePreview() {
        if (previewWindow != null) {
            previewWindow.setVisible(false);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Color editorBg = textArea.getDefaultStyle().getBackground();
        boolean dark = (editorBg.getRed() * 299 + editorBg.getGreen() * 587 + editorBg.getBlue() * 114) / 1000 < 128;
        int delta = dark ? 12 : -10;
        Color minimapBg = new Color(
                clamp255(editorBg.getRed() + delta),
                clamp255(editorBg.getGreen() + delta),
                clamp255(editorBg.getBlue() + delta));
        g2.setColor(minimapBg);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(128, 128, 128, 80));
        g2.drawLine(0, 0, 0, getHeight());

        int totalHeight = textArea.getPreferredSize().height;
        if (totalHeight <= 0) return;

        float rawYScale = (float) getHeight() / totalHeight;
        float yScale = Math.min(scale, rawYScale);

        boolean sizeMismatch = cacheImage != null
                && (cacheImage.getWidth() != getWidth() || cacheImage.getHeight() != getHeight());

        if (cacheDirty || cacheImage == null || sizeMismatch) {
            scheduleRebuild();
        }

        if (cacheImage != null
                && cacheImage.getWidth() == getWidth()
                && cacheImage.getHeight() == getHeight()) {
            g2.drawImage(cacheImage, 0, 0, null);
        }

        if (hoverY >= 0 && !dragging) {
            int hoverHeight = Math.max(8, (int) (hoverPreviewLines
                    * textArea.getFontMetrics(textArea.getFont()).getHeight() * yScale));
            int hy = hoverY - hoverHeight / 2;
            g2.setColor(new Color(128, 128, 128, 30));
            g2.fillRect(0, hy, getWidth(), hoverHeight);
        }

        JViewport viewport = scrollPane.getViewport();
        int viewY = viewport.getViewPosition().y;
        int viewH = viewport.getHeight();

        int rectY = (int) (viewY * yScale);
        int rectH = Math.max(8, (int) (viewH * yScale));

        g2.setColor(viewportColor);
        g2.fillRect(0, rectY, getWidth(), rectH);
        g2.setColor(viewportBorderColor);
        g2.drawRect(0, rectY, getWidth() - 1, rectH);
    }

    private void scheduleRebuild() {
        final int targetW = Math.max(1, getWidth());
        final int targetH = Math.max(1, getHeight());
        final int totalAreaH = textArea.getPreferredSize().height;
        final int totalAreaW = textArea.getPreferredSize().width;
        if (totalAreaH <= 0) return;

        float rawYScale = (float) targetH / totalAreaH;
        final float yScale = Math.min(scale, rawYScale);
        final Font baseFont = textArea.getFont();
        final FontMetrics realFm = textArea.getFontMetrics(baseFont);
        final int charWidthFull = realFm.charWidth('m');
        final int realLineHeight = realFm.getHeight();
        final TextStyle defaultStyle = textArea.getDefaultStyle();
        final int alpha = textAlpha;

        final String snapText;
        final java.util.List<StyledRange> snapRanges;
        final int[] lineYSnapshot;
        try {
            snapText = textArea.getBuffer().getText();
            snapRanges = new java.util.ArrayList<>(textArea.getStyledRanges());

            int lineCount = textArea.getBuffer().lineCount();
            lineYSnapshot = new int[lineCount];
            for (int i = 0; i < lineCount; i++) {
                lineYSnapshot[i] = textArea.yOfBufferLine(i);
            }
        } catch (Exception ex) {
            return;
        }

        final int version = rebuildVersion.incrementAndGet();
        if (currentRebuild != null && !currentRebuild.isDone()) {
            currentRebuild.cancel(true);
        }

        currentRebuild = rebuildExecutor.submit(() -> {
            try {
                BufferedImage img = renderMinimapImage(targetW, targetH, yScale,
                        baseFont, charWidthFull, realLineHeight,
                        defaultStyle, alpha,
                        snapText, snapRanges, lineYSnapshot, totalAreaW);
                if (version != rebuildVersion.get()) return;
                SwingUtilities.invokeLater(() -> {
                    if (version != rebuildVersion.get()) return;
                    cacheImage = img;
                    cacheDirty = false;
                    repaint();
                });
            } catch (Exception ignored) {
            }
        });
    }

    private BufferedImage renderMinimapImage(
            int targetW, int targetH, float yScale,
            Font baseFont, int charWidthFull, int realLineHeight,
            TextStyle defaultStyle, int alpha,
            String text, java.util.List<StyledRange> ranges, int[] lineY, int totalAreaW) {

        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        float rawXScale = (float) (targetW - 4) / Math.max(1, totalAreaW);
        float xScale = Math.min(scale, rawXScale);
        float charWidthMini = charWidthFull * xScale;
        int rowHeight = Math.max(1, Math.round(realLineHeight * yScale));

        int step = Math.max(1, (int) Math.floor(1f / Math.max(0.0001f, realLineHeight * yScale)));

        StyledRange[] sortedRanges = ranges.toArray(new StyledRange[0]);
        java.util.Arrays.sort(sortedRanges,
                java.util.Comparator.comparingInt(StyledRange::getStartOffset));

        int lastDrawnYInt = Integer.MIN_VALUE;
        int textLen = text.length();

        int[] lineStarts = computeLineStarts(text);
        int lineCount = lineStarts.length;

        for (int i = 0; i < lineCount && i < lineY.length; i += step) {
            int lineStart = lineStarts[i];
            int lineEnd = (i + 1 < lineCount) ? lineStarts[i + 1] - 1 : textLen;
            if (lineEnd <= lineStart) continue;

            int yInt = Math.round(lineY[i] * yScale);
            if (yInt == lastDrawnYInt) continue;
            lastDrawnYInt = yInt;
            if (yInt > targetH) break;

            float x = 2f;
            int col = 0;
            int lineLen = lineEnd - lineStart;
            while (col < lineLen) {
                TextStyle style = styleAt(sortedRanges, lineStart + col, defaultStyle);
                int runEnd = col + 1;
                while (runEnd < lineLen
                        && styleAt(sortedRanges, lineStart + runEnd, defaultStyle) == style) {
                    runEnd++;
                }

                Color fg = style.getForeground();
                if (fg == null) fg = defaultStyle.getForeground();
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), alpha));

                float runWidth = (runEnd - col) * charWidthMini;
                int rectW = Math.max(1, Math.round(runWidth));
                g2.fillRect(Math.round(x), yInt, rectW, rowHeight);

                x += runWidth;
                col = runEnd;

                if (x > targetW) break;
            }
        }

        g2.dispose();
        return img;
    }

    private static int[] computeLineStarts(String text) {
        int n = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') n++;
        int[] starts = new int[n];
        starts[0] = 0;
        int idx = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') starts[idx++] = i + 1;
        }
        return starts;
    }

    private static TextStyle styleAt(StyledRange[] sorted, int offset, TextStyle def) {
        if (sorted.length == 0) return def;
        int lo = 0, hi = sorted.length - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid].getStartOffset() <= offset) {
                found = mid;
                lo = mid + 1;
            } else hi = mid - 1;
        }
        if (found >= 0) {
            StyledRange r = sorted[found];
            if (offset < r.getEndOffset()) return r.getStyle();
        }
        return def;
    }

    private TextStyle getStyleAt(int offset) {
        var ranges = textArea.getStyledRanges();
        for (int i = ranges.size() - 1; i >= 0; i--) {
            StyledRange r = ranges.get(i);
            if (offset >= r.getStartOffset() && offset < r.getEndOffset()) {
                return r.getStyle();
            }
        }
        return textArea.getDefaultStyle();
    }

    private Font deriveFont(Font base, TextStyle style) {
        int s = Font.PLAIN;
        if (style.isBold()) s |= Font.BOLD;
        if (style.isItalic()) s |= Font.ITALIC;
        return base.deriveFont(s);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(minimapWidth, scrollPane.getViewport().getHeight());
    }
}