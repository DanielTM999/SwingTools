package dtm.stools.component.panels.dock;

import java.awt.*;

public class DockDropContext {
    private final DockPanel dockPanel;
    private final DockEntry entry;
    private final DockRegion sourceRegion;
    private final DockRegion targetRegion;
    private final Point location;

    public DockDropContext(DockPanel dockPanel,
                           DockEntry entry,
                           DockRegion sourceRegion,
                           DockRegion targetRegion,
                           Point location) {
        this.dockPanel = dockPanel;
        this.entry = entry;
        this.sourceRegion = sourceRegion;
        this.targetRegion = targetRegion;
        this.location = location == null ? null : new Point(location);
    }

    public DockPanel getDockPanel() {
        return dockPanel;
    }

    public DockEntry getEntry() {
        return entry;
    }

    public DockRegion getSourceRegion() {
        return sourceRegion;
    }

    public DockRegion getTargetRegion() {
        return targetRegion;
    }

    public Point getLocation() {
        return location == null ? null : new Point(location);
    }
}
