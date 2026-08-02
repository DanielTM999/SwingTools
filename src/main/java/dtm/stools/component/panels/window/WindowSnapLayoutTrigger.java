package dtm.stools.component.panels.window;

/** Define onde o seletor de Snap Layouts pode ser acionado. */
public enum WindowSnapLayoutTrigger {
    DISABLED(false, false),
    MAXIMIZE_BUTTON(true, false),
    TOP_CENTER(false, true),
    BOTH(true, true);

    private final boolean maximizeButton;
    private final boolean topCenter;

    WindowSnapLayoutTrigger(boolean maximizeButton, boolean topCenter) {
        this.maximizeButton = maximizeButton;
        this.topCenter = topCenter;
    }

    public boolean supportsMaximizeButton() { return maximizeButton; }
    public boolean supportsTopCenter() { return topCenter; }
}
