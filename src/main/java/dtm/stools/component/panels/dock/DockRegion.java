package dtm.stools.component.panels.dock;

public enum DockRegion {
    CENTER,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    BOTTOM_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT;

    public boolean isLeftSide() {
        return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    public boolean isRightSide() {
        return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    public boolean isTopSide() {
        return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
    }

    public boolean isBottomSide() {
        return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }

    public boolean isHorizontalSplitRegion() {
        return isLeftSide() || isRightSide();
    }

    public boolean isVerticalSplitRegion() {
        return !isHorizontalSplitRegion();
    }
}
