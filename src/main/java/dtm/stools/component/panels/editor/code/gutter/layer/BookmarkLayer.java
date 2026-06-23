package dtm.stools.component.panels.editor.code.gutter.layer;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;
import dtm.stools.component.panels.editor.code.listeners.BookmarkChangeListener;
import dtm.stools.component.panels.editor.code.prototype.styles.BookmarkStyle;
import lombok.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookmarkLayer implements GutterLayer, TransferableLayer {

    @Builder.Default
    private final List<BookmarkChangeListener> bookmarkListeners = new ArrayList<>();

    private CodeEditorTextArea textArea;

    private BookmarkChangeListener textAreaListener;

    @Setter
    @Builder.Default
    private BookmarkStyle defaultStyle = BookmarkStyle.builder().build();

    @Getter
    @Setter
    @Builder.Default
    private boolean enableOnClick = false;

    @Getter
    @Setter
    @Builder.Default
    private boolean previewOnHoverEnabled = false;

    @Getter
    @Setter
    @Builder.Default
    private float previewAlpha = 0.35f;

    @Getter
    @Setter
    @Builder.Default
    private int slotWidth = 14;

    @Getter
    @Setter
    @Builder.Default
    private int insidePadding = 2;

    @Getter
    @Setter
    private int hoverLine = -1;

    public void clearHover() {
        hoverLine = -1;
    }

    public int getReservedWidth() {
        return Math.max(0, slotWidth + insidePadding);
    }

    public void attachTextArea(CodeEditorTextArea textArea) {
        if (this.textArea == textArea) return;
        detachTextArea();
        this.textArea = textArea;
        if (this.textArea != null) {
            if (textAreaListener == null) {
                textAreaListener = this::fireBookmarkChanged;
            }
            this.textArea.addBookmarkChangeListener(textAreaListener);
        }
    }

    public void detachTextArea() {
        if (textArea != null && textAreaListener != null) {
            textArea.removeBookmarkChangeListener(textAreaListener);
        }
        textArea = null;
    }

    public void addBookmark(int line) {
        if (textArea != null) {
            textArea.addBookmark(line);
        }
    }

    public void removeBookmark(int line) {
        if (textArea != null) {
            textArea.removeBookmark(line);
        }
    }

    public void toggleBookmark(int line) {
        if (textArea != null) {
            textArea.toggleBookmark(line);
        }
    }

    public boolean hasBookmark(int line) {
        return textArea != null && textArea.isBookmarked(line);
    }

    public SortedSet<Integer> getBookmarks() {
        if (textArea == null) {
            return Collections.emptySortedSet();
        }
        return Collections.unmodifiableSortedSet(new TreeSet<>(textArea.getBookmarks()));
    }

    public void addBookmarkChangeListener(BookmarkChangeListener listener) {
        if (listener != null) {
            bookmarkListeners.add(listener);
        }
    }

    public void removeBookmarkChangeListener(BookmarkChangeListener listener) {
        bookmarkListeners.remove(listener);
    }

    @Override
    public List<Object> getTransferableListeners() {
        return new ArrayList<>(bookmarkListeners);
    }

    @Override
    public void receiveTransferableListeners(List<Object> listeners) {
        if (listeners == null) return;
        for (Object listener : listeners) {
            if (listener instanceof BookmarkChangeListener bookmarkListener
                    && !bookmarkListeners.contains(bookmarkListener)) {
                bookmarkListeners.add(bookmarkListener);
            }
        }
    }

    public void fireBookmarkChanged(int line, boolean added) {
        for (BookmarkChangeListener listener : List.copyOf(bookmarkListeners)) {
            listener.onBookmarkChanged(line, added);
        }
    }

    public boolean isBookmarkClick(CodeEditorGutter gutter, int mouseX) {
        int left = getSlotLeft(gutter);
        return mouseX >= left && mouseX <= left + slotWidth;
    }

    @Override
    public void paint(Graphics g, CodeEditorGutter gutter, int line, int x, int y, int width, int height) {
        CodeEditorTextArea textArea = this.textArea != null ? this.textArea : gutter.getTextArea();
        if (textArea == null) return;

        boolean marked = textArea.isBookmarked(line);
        boolean preview = !marked && previewOnHoverEnabled && hoverLine == line;
        if (!marked && !preview) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (preview) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, previewAlpha));
            }

            if (defaultStyle.getIcon() != null) {
                int ix = getSlotLeft(gutter) + (slotWidth - defaultStyle.getIcon().getIconWidth()) / 2;
                int iy = y + (height - defaultStyle.getIcon().getIconHeight()) / 2;
                defaultStyle.getIcon().paintIcon(null, g2, ix, iy);
                return;
            }

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.max(7, Math.min(slotWidth - 2, height - 4));
            int bx = getSlotLeft(gutter) + (slotWidth - size) / 2;
            int by = y + (height - size) / 2;
            int notch = Math.max(3, size / 3);

            Polygon bookmark = new Polygon();
            bookmark.addPoint(bx, by);
            bookmark.addPoint(bx + size, by);
            bookmark.addPoint(bx + size, by + size);
            bookmark.addPoint(bx + size / 2, by + size - notch);
            bookmark.addPoint(bx, by + size);

            g2.setColor(defaultStyle.getColor());
            g2.fillPolygon(bookmark);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void onMouseClick(MouseEvent e, int line) {
        if (!enableOnClick) return;
        CodeEditorTextArea target = textArea;
        if (target == null && e.getComponent() instanceof CodeEditorGutter gutter) {
            target = gutter.getTextArea();
        }
        if (target != null) {
            target.toggleBookmark(line);
        }
    }

    private int getSlotLeft(CodeEditorGutter gutter) {
        int right = gutter.getGutterWidth() - gutter.getFoldStripWidth();
        return right - getReservedWidth();
    }
}
