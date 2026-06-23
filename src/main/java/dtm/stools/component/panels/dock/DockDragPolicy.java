package dtm.stools.component.panels.dock;

public interface DockDragPolicy {
    default boolean canStartDrag(DockEntry entry) {
        return entry != null && entry.isDraggable();
    }

    default boolean canDrop(DockDropContext context) {
        return context != null
                && context.getEntry() != null
                && context.getTargetRegion() != null
                && context.getEntry().canDropTo(context.getTargetRegion());
    }
}
