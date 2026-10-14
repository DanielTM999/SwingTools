package dtm.stools.component.inputfields.textarea;

public enum TextAreaResize {
    NONE,
    BOTH,
    HORIZONTAL,
    VERTICAL;

    public boolean allowsHorizontal() {
        return this == BOTH || this == HORIZONTAL;
    }

    public boolean allowsVertical() {
        return this == BOTH || this == VERTICAL;
    }
}
