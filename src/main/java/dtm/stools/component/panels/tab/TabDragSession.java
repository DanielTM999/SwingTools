package dtm.stools.component.panels.tab;

import javax.swing.*;
import java.awt.*;

class TabDragSession {
    private final TabEntry entry;
    private final JComponent header;
    private final Point startPoint;
    private Cursor originalCursor;
    private boolean started;
    private JLayeredPane dragLayer;
    private JComponent ghostComponent;
    private JComponent splitPreviewComponent;
    private Point ghostOffset;
    private JWindow detachedPreviewWindow;

    TabDragSession(TabEntry entry, JComponent header, Point startPoint) {
        this.entry = entry;
        this.header = header;
        this.startPoint = startPoint;
    }

    TabEntry getEntry() {
        return entry;
    }

    JComponent getHeader() {
        return header;
    }

    Point getStartPoint() {
        return startPoint;
    }

    Cursor getOriginalCursor() {
        return originalCursor;
    }

    void setOriginalCursor(Cursor originalCursor) {
        this.originalCursor = originalCursor;
    }

    boolean isStarted() {
        return started;
    }

    void setStarted(boolean started) {
        this.started = started;
        entry.setDragging(started);
    }

    JLayeredPane getDragLayer() {
        return dragLayer;
    }

    void setDragLayer(JLayeredPane dragLayer) {
        this.dragLayer = dragLayer;
    }

    JComponent getGhostComponent() {
        return ghostComponent;
    }

    void setGhostComponent(JComponent ghostComponent) {
        this.ghostComponent = ghostComponent;
    }

    JComponent getSplitPreviewComponent() {
        return splitPreviewComponent;
    }

    void setSplitPreviewComponent(JComponent splitPreviewComponent) {
        this.splitPreviewComponent = splitPreviewComponent;
    }

    Point getGhostOffset() {
        return ghostOffset;
    }

    void setGhostOffset(Point ghostOffset) {
        this.ghostOffset = ghostOffset;
    }

    JWindow getDetachedPreviewWindow() {
        return detachedPreviewWindow;
    }

    void setDetachedPreviewWindow(JWindow detachedPreviewWindow) {
        this.detachedPreviewWindow = detachedPreviewWindow;
    }
}
