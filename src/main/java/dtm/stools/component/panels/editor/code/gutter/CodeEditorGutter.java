package dtm.stools.component.panels.editor.code.gutter;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.gutter.layer.BookmarkLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.BreakpointLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.FoldingLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.GutterLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.LineMarkerLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.LineNumberLayer;
import dtm.stools.component.panels.editor.code.gutter.layer.TransferableLayer;
import dtm.stools.component.panels.editor.code.listeners.BookmarkChangeListener;
import dtm.stools.component.panels.editor.code.listeners.BreakpointChangeListener;
import dtm.stools.component.panels.editor.code.listeners.LineChangeListener;
import dtm.stools.component.panels.editor.code.prototype.Breakpoint;
import dtm.stools.component.panels.editor.code.prototype.styles.BreakpointStyle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.*;
import java.util.List;

public class CodeEditorGutter extends JComponent implements LineChangeListener {

    private final List<BreakpointListener> breakpointListeners = new ArrayList<>();
    private final BreakpointChangeListener breakpointRepaintListener = (breakpoint, added) -> repaint();

    @Getter
    private final CodeEditorTextArea textArea;

    public static final int FOLD_CHEVRON_WIDTH = 14;

    @Getter
    private boolean breakpointEnabled = false;

    @Getter
    private boolean bookmarkEnabled = false;

    @Getter
    private boolean lineNumberEnabled = true;

    private int baseGutterWidth = 42;

    @Getter
    private int leftMargin = 10;

    @Getter
    private Color borderColor = UIManager.getColor("Separator.foreground");

    @Getter
    private Color lineNumberColor = UIManager.getColor("Label.disabledForeground");

    private Font lineNumberFont;
    private boolean fontExplicitlySet;

    @Getter
    private final List<GutterLayer> layers = new ArrayList<>();

    public CodeEditorGutter(CodeEditorTextArea textArea) {
        this.textArea = textArea;
        setOpaque(true);
        setBackground(UIManager.getColor("Panel.background"));
        textArea.addLineChangeListener(this);
        addListeners();
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                clearTransientHoverStateAndRepaint();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        clearTransientHoverStateAndRepaint();
    }

    @Override
    public void removeNotify() {
        clearTransientHoverStateAndRepaint();
        super.removeNotify();
    }

    @Override
    public void onLinesInserted(int atLine, int count) {
        for (GutterLayer layer : layers) {
            layer.onLinesInserted(atLine, count);
        }
        repaint();
    }

    @Override
    public void onLinesRemoved(int atLine, int count) {
        for (GutterLayer layer : layers) {
            layer.onLinesRemoved(atLine, count);
        }
        repaint();
    }

    public int getGutterWidth() {
        return getLineNumberAreaWidth() + getBookmarkStripWidth() + getFoldStripWidth();
    }

    public int getFoldStripWidth() {
        return textArea.isFoldingEnabled() ? FOLD_CHEVRON_WIDTH : 0;
    }

    public int getFoldStripRightInset() {
        LineMarkerLayer layer = getLayer(LineMarkerLayer.class);
        if (layer == null || layer.getSide() != LineMarkerLayer.Side.RIGHT) return 0;
        return layer.getStripeOffsetX() + layer.getStripeWidth();
    }

    public int getBookmarkStripWidth() {
        if (!bookmarkEnabled) return 0;
        BookmarkLayer layer = getLayer(BookmarkLayer.class);
        return layer != null ? layer.getReservedWidth() : 0;
    }

    public int getLineCount() {
        return textArea.getBuffer().lineCount();
    }

    public int getLineNumberAreaWidth() {
        if (!lineNumberEnabled) return 0;
        int required = getRequiredLineNumberWidth();
        return isBreakpointOverlayActive() ? required : Math.max(baseGutterWidth, required);
    }

    public void setGutterWidth(int gutterWidth) {
        this.baseGutterWidth = gutterWidth;
        revalidate();
        repaint();
    }

    public void setLeftMargin(int leftMargin) {
        this.leftMargin = Math.max(0, leftMargin);
        revalidate();
        repaint();
    }

    private int getRequiredLineNumberWidth() {
        LineNumberLayer layer = getLayer(LineNumberLayer.class);
        if (layer == null) return 0;
        return layer.getRequiredWidth(this, getFontMetrics(getLineNumberFont()), getLineCount());
    }

    public boolean isBreakpointOverlayActive() {
        BreakpointLayer layer = getLayer(BreakpointLayer.class);
        return layer != null && layer.isEffectiveOverlay(baseGutterWidth);
    }

    @Override
    public void setBackground(Color background) {
        super.setBackground(background);
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        fontExplicitlySet = true;
        revalidate();
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setLineNumberColor(Color lineNumberColor) {
        this.lineNumberColor = lineNumberColor;
        repaint();
    }

    public Font getLineNumberFont() {
        if (lineNumberFont != null) {
            return lineNumberFont;
        }

        Font font = fontExplicitlySet ? getFont() : null;
        if (font != null) {
            return font;
        }

        font = textArea != null ? textArea.getFont() : null;
        if (font != null) {
            return font;
        }

        font = UIManager.getFont("TextArea.font");
        if (font != null) {
            return font;
        }

        font = UIManager.getFont("Label.font");
        if (font != null) {
            return font;
        }

        font = getFont();
        if (font != null) {
            return font;
        }

        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    public void setLineNumberFont(Font lineNumberFont) {
        this.lineNumberFont = lineNumberFont;
        revalidate();
        repaint();
    }

    public void enableBreakpoint(boolean enabled) {
        this.breakpointEnabled = enabled;
        repaint();
    }

    public void enableBookmark(boolean enabled) {
        this.bookmarkEnabled = enabled;
        revalidate();
        repaint();
    }

    public void enableLineNumber(boolean enabled) {
        this.lineNumberEnabled = enabled;
        repaint();
    }

    public void setPreviewOnHoverEnabled(boolean enabled) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) {
            bp = new BreakpointLayer();
            addLayer(bp);
        }
        bp.setPreviewOnHoverEnabled(enabled);
        if (!enabled) {
            bp.clearHover();
        }
        repaint();
    }

    public boolean isPreviewOnHoverEnabled() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null && bp.isPreviewOnHoverEnabled();
    }

    public void setPreviewAlpha(float alpha) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) {
            bp = new BreakpointLayer();
            addLayer(bp);
        }
        bp.setPreviewAlpha(alpha);
        repaint();
    }

    public float getPreviewAlpha() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null ? bp.getPreviewAlpha() : 0f;
    }

    public void setBreakpointEnableOnClick(boolean enabled) {
        getOrCreateBreakpointLayer().setEnableOnClick(enabled);
    }

    public boolean isBreakpointEnableOnClick() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp == null || bp.isEnableOnClick();
    }

    public void enableBreakpointEmptyLine(boolean enabled) {
        getOrCreateBreakpointLayer().enableBreakpointEmptyLine(enabled);
    }

    public void setBreakpointEmptyLineEnabled(boolean enabled) {
        enableBreakpointEmptyLine(enabled);
    }

    public boolean isBreakpointEmptyLineEnabled() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp == null || bp.isEnableBreakpointEmptyLine();
    }

    public void setBookmarkPreviewOnHoverEnabled(boolean enabled) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark == null) {
            bookmark = new BookmarkLayer();
            addLayer(bookmark);
        }
        bookmark.setPreviewOnHoverEnabled(enabled);
        if (!enabled) {
            bookmark.clearHover();
        }
        repaint();
    }

    public boolean isBookmarkPreviewOnHoverEnabled() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmark != null && bookmark.isPreviewOnHoverEnabled();
    }

    public void setBookmarkPreviewAlpha(float alpha) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark == null) {
            bookmark = new BookmarkLayer();
            addLayer(bookmark);
        }
        bookmark.setPreviewAlpha(alpha);
        repaint();
    }

    public float getBookmarkPreviewAlpha() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmark != null ? bookmark.getPreviewAlpha() : 0f;
    }

    public void setBookmarkEnableOnClick(boolean enabled) {
        getOrCreateBookmarkLayer().setEnableOnClick(enabled);
    }

    public boolean isBookmarkEnableOnClick() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmark != null && bookmark.isEnableOnClick();
    }

    public void setFoldChevronVisibleOnHoverOnly(boolean enabled) {
        FoldingLayer layer = getLayer(FoldingLayer.class);
        if (layer == null) {
            layer = new FoldingLayer();
            addLayer(layer);
        }
        layer.setChevronVisibleOnHoverOnly(enabled);
        if (!enabled) {
            layer.clearHover();
        }
        repaint();
    }

    public boolean isFoldChevronVisibleOnHoverOnly() {
        FoldingLayer layer = getLayer(FoldingLayer.class);
        return layer != null && layer.isChevronVisibleOnHoverOnly();
    }

    public void addLayer(GutterLayer layer) {
        if (layer == null) return;
        Iterator<GutterLayer> iterator = layers.iterator();
        while (iterator.hasNext()) {
            GutterLayer existing = iterator.next();
            boolean sameLayerType = layer.getClass().isAssignableFrom(existing.getClass()) || existing.getClass().isAssignableFrom(layer.getClass());
            if (sameLayerType) {
                if (existing instanceof TransferableLayer from && layer instanceof TransferableLayer to) {
                    to.receiveTransferableListeners(from.getTransferableListeners());
                }
                if (existing instanceof BookmarkLayer bookmark) {
                    bookmark.detachTextArea();
                }
                iterator.remove();
            }
        }
        if (layer instanceof BookmarkLayer bookmark) {
            bookmark.attachTextArea(textArea);
        }
        if (layer instanceof BreakpointLayer breakpoint) {
            breakpoint.addBreakpointChangeListener(breakpointRepaintListener);
        }
        layers.add(layer);
        revalidate();
        repaint();
    }

    public void removeLayer(GutterLayer layer) {
        if (layer instanceof BookmarkLayer bookmark) {
            bookmark.detachTextArea();
        }
        layers.remove(layer);
        repaint();
    }

    public <T extends GutterLayer> T getLayer(Class<T> type) {
        return layers.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    public Set<Integer> getBreakpointLines() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null ? bp.getBreakpointLines() : Collections.emptySet();
    }

    public Set<Breakpoint> getBreakpoints() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null ? bp.getBreakpoints() : Collections.emptySet();
    }

    public void addBreakpoint(int line) {
        BreakpointLayer bp = getOrCreateBreakpointLayer();
        if (!isBreakpointAddAllowedAtLine(bp, line)) return;
        bp.addBreakpoint(line);
    }

    public void removeBreakpoint(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.removeBreakpoint(line);
    }

    public void toggleBreakpoint(int line) {
        BreakpointLayer bp = getOrCreateBreakpointLayer();
        if (!isBreakpointToggleAllowedAtLine(bp, line)) return;
        bp.toggleBreakpoint(line);
    }

    public void clearBreakpoints() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.clearBreakpoints();
    }

    public boolean hasBreakpoint(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null && bp.hasBreakpoint(line);
    }

    public void setBreakpointActive(int line, boolean active) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.setBreakpointActive(line, active);
    }

    public void toggleBreakpointActive(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.toggleBreakpointActive(line);
    }

    public void deactivateBreakpoint(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.deactivateBreakpoint(line);
    }

    public void deactivateAllBreakpoints() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.deactivateAllBreakpoints();
    }

    public void activateBreakpoint(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.activateBreakpoint(line);
    }

    public void activateAllBreakpoints() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) return;
        bp.activateAllBreakpoints();
    }

    public boolean isBreakpointActive(int line) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        return bp != null && bp.isBreakpointActive(line);
    }

    public void setBreakpointInactiveIcon(Icon icon) {
        getOrCreateBreakpointLayer().setInactiveIcon(icon);
        repaint();
    }

    public boolean setBreakpointStyle(int line, BreakpointStyle style) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        boolean changed = bp != null && bp.setBreakpointStyle(line, style);
        if (changed) repaint();
        return changed;
    }

    public boolean setBreakpointInactiveStyle(int line, BreakpointStyle inactiveStyle) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        boolean changed = bp != null && bp.setBreakpointInactiveStyle(line, inactiveStyle);
        if (changed) repaint();
        return changed;
    }

    public boolean setBreakpointStyles(int line, BreakpointStyle style, BreakpointStyle inactiveStyle) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        boolean changed = bp != null && bp.setBreakpointStyles(line, style, inactiveStyle);
        if (changed) repaint();
        return changed;
    }

    public void addBreakpointChangeListener(BreakpointChangeListener listener) {
        getOrCreateBreakpointLayer().addBreakpointChangeListener(listener);
    }

    public void removeBreakpointChangeListener(BreakpointChangeListener listener) {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp != null) {
            bp.removeBreakpointChangeListener(listener);
        }
    }

    public Set<Integer> getBookmarkLines() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmark != null ? bookmark.getBookmarks() : Collections.emptySortedSet();
    }

    public void addBookmark(int line) {
        getOrCreateBookmarkLayer().addBookmark(line);
        repaint();
    }

    public void removeBookmark(int line) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark == null) return;
        bookmark.removeBookmark(line);
        repaint();
    }

    public void toggleBookmark(int line) {
        getOrCreateBookmarkLayer().toggleBookmark(line);
        repaint();
    }

    public boolean hasBookmark(int line) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmark != null && bookmark.hasBookmark(line);
    }

    public void addBookmarkChangeListener(BookmarkChangeListener listener) {
        getOrCreateBookmarkLayer().addBookmarkChangeListener(listener);
    }

    public void removeBookmarkChangeListener(BookmarkChangeListener listener) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark != null) {
            bookmark.removeBookmarkChangeListener(listener);
        }
    }

    public void addBreakpointListener(BreakpointListener listener) {
        breakpointListeners.add(listener);
    }

    public void removeBreakpointListener(BreakpointListener listener) {
        breakpointListeners.remove(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        validateTransientHoverState();

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        int lineCount = textArea.getBuffer().lineCount();
        int lineHeight = getLineHeight();

        Rectangle clip = g.getClipBounds();
        int firstLine = clip != null ? textArea.bufferLineAtY(clip.y) : 0;
        int lastLine  = clip != null ? textArea.bufferLineAtY(clip.y + clip.height) : lineCount - 1;
        firstLine = Math.max(0, firstLine);
        lastLine  = Math.min(lineCount - 1, lastLine);

        for (int line = firstLine; line <= lastLine; line++) {
            if (textArea.isLineHidden(line)) continue;
            int y = textArea.yOfBufferLine(line);

            for (GutterLayer layer : layers) {
                if (layer instanceof BreakpointLayer bp && !breakpointEnabled) continue;
                if (layer instanceof BookmarkLayer && !bookmarkEnabled) continue;
                if (layer instanceof LineNumberLayer && !lineNumberEnabled) continue;
                layer.paint(g, this, line, 0, y, getGutterWidth(), lineHeight);
            }
        }

        g.setColor(borderColor);
        g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getGutterWidth(), textArea.getPreferredSize().height);
    }

    private void fireBreakpointToggled(int line, boolean active) {
        breakpointListeners.forEach(l -> l.onBreakpointToggled(line, active));
    }

    private BreakpointLayer getOrCreateBreakpointLayer() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp == null) {
            bp = new BreakpointLayer();
            addLayer(bp);
        }
        return bp;
    }

    private BookmarkLayer getOrCreateBookmarkLayer() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark == null) {
            bookmark = new BookmarkLayer();
            addLayer(bookmark);
        }
        return bookmark;
    }

    private int getLineHeight() {
        Font font = textArea.getFont();
        return textArea.getFontMetrics(font).getHeight();
    }

    private int lineAtGutterY(int y) {
        if (y < 0) return -1;
        int total = textArea.getBuffer().lineCount();
        if (total <= 0) return -1;

        int line = textArea.bufferLineAtY(y);
        if (line < 0 || line >= total || textArea.isLineHidden(line)) {
            return -1;
        }

        int lineY = textArea.yOfBufferLine(line);
        int lineHeight = getLineHeight();
        return y >= lineY && y < lineY + lineHeight ? line : -1;
    }

    private boolean isBreakpointToggleAllowedAtLine(BreakpointLayer bp, int line) {
        if (!isValidLineIndex(line)) return false;
        return bp.hasBreakpoint(line) || isBreakpointAddAllowedAtLine(bp, line);
    }

    private boolean isBreakpointAddAllowedAtLine(BreakpointLayer bp, int line) {
        if (!isValidLineIndex(line)) return false;
        return bp.isEnableBreakpointEmptyLine() || !isBlankLine(line);
    }

    private boolean isValidLineIndex(int line) {
        return line >= 0 && line < textArea.getBuffer().lineCount();
    }

    private boolean isBlankLine(int line) {
        return textArea.getBuffer().lineAt(line).trim().isEmpty();
    }

    private void addListeners() {
        textArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (textArea.isInCodeLensRow(e.getY())) return;
                int line = lineAtGutterY(e.getY());
                if (line < 0) {
                    clearTransientHoverStateAndRepaint();
                    return;
                }

                LineMarkerLayer marker = getLayer(LineMarkerLayer.class);
                if (marker != null && marker.hasLineColor(line) && marker.isLineMarkerClick(CodeEditorGutter.this, e.getX())) {
                    marker.onMouseClick(e, line);
                    repaint();
                    return;
                }

                if (textArea.isFoldingEnabled() && isFoldChevronClick(e.getX())) {
                    if (textArea.isFoldAnchorLine(line)) {
                        textArea.toggleFold(line);
                    }
                    repaint();
                    return;
                }

                if (breakpointEnabled) {
                    BreakpointLayer bp = getLayer(BreakpointLayer.class);
                    if (bp != null && bp.isEnableOnClick() && !isBookmarkClick(e.getX())) {
                        if (!isBreakpointToggleAllowedAtLine(bp, line)) {
                            return;
                        }
                        bp.onMouseClick(e, line);
                        fireBreakpointToggled(line, bp.hasBreakpoint(line));
                        repaint();
                        return;
                    }
                }

                if (bookmarkEnabled) {
                    BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
                    if (bookmark != null && bookmark.isBookmarkClick(CodeEditorGutter.this, e.getX())) {
                        bookmark.onMouseClick(e, line);
                        clearBreakpointHover();
                        repaint();
                        return;
                    }
                }

                for (GutterLayer layer : layers) {
                    if (layer instanceof BreakpointLayer) continue;
                    if (layer instanceof BookmarkLayer) continue;
                    if (layer instanceof LineNumberLayer && !lineNumberEnabled) continue;
                    layer.onMouseClick(e, line);
                    repaint();
                    break;
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearTransientHoverStateAndRepaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (updateLineMarkerHover(e)) {
                    return;
                }
                if (updateFoldHover(e)) {
                    return;
                }
                if (updateBookmarkHover(e)) {
                    return;
                }
                if (!breakpointEnabled) {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }
                BreakpointLayer bp = getLayer(BreakpointLayer.class);
                if (bp == null || !bp.isPreviewOnHoverEnabled()) {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }
                if (textArea.isFoldingEnabled() && isFoldChevronClick(e.getX())) {
                    setCursor(Cursor.getDefaultCursor());
                    if (bp.getHoverLine() != -1) {
                        bp.clearHover();
                        repaint();
                    }
                    return;
                }
                int line = lineAtGutterY(e.getY());
                int total = textArea.getBuffer().lineCount();
                if (line < 0 || line >= total) {
                    setCursor(Cursor.getDefaultCursor());
                    if (bp.getHoverLine() != -1) {
                        bp.clearHover();
                        repaint();
                    }
                    return;
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                if (bp.hasBreakpoint(line)) {
                    if (bp.getHoverLine() != -1) {
                        bp.clearHover();
                        repaint();
                    }
                    return;
                }
                if (!isBreakpointAddAllowedAtLine(bp, line)) {
                    if (bp.getHoverLine() != -1) {
                        bp.clearHover();
                        repaint();
                    }
                    return;
                }
                if (line != bp.getHoverLine()) {
                    bp.setHoverLine(line);
                    repaint();
                }
            }
        });
    }

    public boolean isFoldChevronClick(int mouseX) {
        int strip = getFoldStripWidth();
        if (strip <= 0) return false;
        int right = getGutterWidth() - 1 - getFoldStripRightInset();
        int left = right - strip;
        return mouseX >= left && mouseX <= right;
    }

    private boolean isBookmarkClick(int mouseX) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        return bookmarkEnabled && bookmark != null && bookmark.isBookmarkClick(this, mouseX);
    }

    private boolean updateLineMarkerHover(MouseEvent e) {
        LineMarkerLayer marker = getLayer(LineMarkerLayer.class);
        if (marker == null || !marker.isLineMarkerClick(this, e.getX())) {
            return false;
        }
        int line = lineAtGutterY(e.getY());
        int total = textArea.getBuffer().lineCount();
        if (line < 0 || line >= total || !marker.hasLineColor(line)) {
            return false;
        }
        clearBreakpointHover();
        clearBookmarkHover();

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return true;
    }

    private boolean updateFoldHover(MouseEvent e) {
        FoldingLayer fold = getLayer(FoldingLayer.class);
        if (fold == null || !textArea.isFoldingEnabled() || !isFoldChevronClick(e.getX())) {
            if (fold != null && fold.getHoverLine() != -1) {
                fold.clearHover();
                repaint();
            }
            return false;
        }

        clearBreakpointHover();
        clearBookmarkHover();

        int line = lineAtGutterY(e.getY());
        int total = textArea.getBuffer().lineCount();
        boolean foldAnchor = line >= 0 && line < total && textArea.isFoldAnchorLine(line);
        setCursor(foldAnchor
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        int nextHover = foldAnchor ? line : -1;
        if (fold.getHoverLine() != nextHover) {
            fold.setHoverLine(nextHover);
            repaint();
        }
        return true;
    }

    private boolean updateBookmarkHover(MouseEvent e) {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (!bookmarkEnabled || bookmark == null || !bookmark.isBookmarkClick(this, e.getX())) {
            if (bookmark != null && bookmark.getHoverLine() != -1) {
                bookmark.clearHover();
                repaint();
            }
            return false;
        }
        if (textArea.isFoldingEnabled() && isFoldChevronClick(e.getX())) {
            setCursor(Cursor.getDefaultCursor());
            if (bookmark.getHoverLine() != -1) {
                bookmark.clearHover();
                repaint();
            }
            return true;
        }
        int line = lineAtGutterY(e.getY());
        int total = textArea.getBuffer().lineCount();
        if (line < 0 || line >= total) {
            setCursor(Cursor.getDefaultCursor());
            if (bookmark.getHoverLine() != -1) {
                bookmark.clearHover();
                repaint();
            }
            return true;
        }
        clearBreakpointHover();
        setCursor(Cursor.getDefaultCursor());
        if (!bookmark.isPreviewOnHoverEnabled() || textArea.isBookmarked(line)) {
            if (bookmark.getHoverLine() != -1) {
                bookmark.clearHover();
                repaint();
            }
            return true;
        }
        if (line != bookmark.getHoverLine()) {
            bookmark.setHoverLine(line);
            repaint();
        }
        return true;
    }

    private void clearBreakpointHover() {
        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp != null && bp.getHoverLine() != -1) {
            bp.clearHover();
            repaint();
        }
    }

    private void clearBookmarkHover() {
        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark != null && bookmark.getHoverLine() != -1) {
            bookmark.clearHover();
            repaint();
        }
    }

    private void validateTransientHoverState() {
        Point mouse = getMousePositionSafely();
        if (mouse == null) {
            clearTransientHoverState();
            return;
        }

        int mouseLine = lineAtGutterY(mouse.y);
        boolean changed = false;

        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp != null && bp.getHoverLine() != -1 && bp.getHoverLine() != mouseLine) {
            bp.clearHover();
            changed = true;
        }

        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark != null && bookmark.getHoverLine() != -1 && bookmark.getHoverLine() != mouseLine) {
            bookmark.clearHover();
            changed = true;
        }

        FoldingLayer fold = getLayer(FoldingLayer.class);
        if (fold != null && fold.getHoverLine() != -1 && fold.getHoverLine() != mouseLine) {
            fold.clearHover();
            changed = true;
        }

        if (changed) {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void clearTransientHoverStateAndRepaint() {
        if (clearTransientHoverState()) {
            repaint();
        }
    }

    private boolean clearTransientHoverState() {
        boolean changed = false;

        BreakpointLayer bp = getLayer(BreakpointLayer.class);
        if (bp != null && bp.getHoverLine() != -1) {
            bp.clearHover();
            changed = true;
        }

        BookmarkLayer bookmark = getLayer(BookmarkLayer.class);
        if (bookmark != null && bookmark.getHoverLine() != -1) {
            bookmark.clearHover();
            changed = true;
        }

        FoldingLayer fold = getLayer(FoldingLayer.class);
        if (fold != null && fold.getHoverLine() != -1) {
            fold.clearHover();
            changed = true;
        }

        if (changed) {
            setCursor(Cursor.getDefaultCursor());
        }

        return changed;
    }

    private Point getMousePositionSafely() {
        if (!isShowing()) return null;

        try {
            return getMousePosition();
        } catch (HeadlessException ignored) {
            return null;
        }
    }
}
