package dtm.stools.component.tree;

public enum CheckState {
    UNCHECKED,
    CHECKED,
    INDETERMINATE;

    public CheckState toggle() {
        return this == CHECKED ? UNCHECKED : CHECKED;
    }
}
