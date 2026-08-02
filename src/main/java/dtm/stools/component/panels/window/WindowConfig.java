package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class WindowConfig {
    private final String key;
    private String title;
    private Component content;
    private Icon icon;
    private Rectangle bounds;
    private Dimension minimumSize = new Dimension(220, 140);
    private Insets maximizedInsets;
    private boolean movable = true;
    private boolean resizable = true;
    private boolean closable = true;
    private boolean minimizable = true;
    private boolean maximizable = true;
    private boolean modal;
    private boolean closeOnEscape = true;
    private boolean snapEnabled = true;
    private Boolean snapLayoutsEnabled;
    private WindowSnapLayoutTrigger snapLayoutTrigger;
    private WindowCloseOperation closeOperation = WindowCloseOperation.HIDE;
    private WindowStyle style;

    public WindowConfig(String key, String title, Component content) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Window key cannot be blank");
        this.key = key;
        this.title = title == null ? "" : title;
        this.content = Objects.requireNonNull(content, "content");
    }

    public WindowConfig title(String title) { this.title = title == null ? "" : title; return this; }
    public WindowConfig content(Component content) { this.content = Objects.requireNonNull(content, "content"); return this; }
    public WindowConfig icon(Icon icon) { this.icon = icon; return this; }
    public WindowConfig bounds(Rectangle bounds) { this.bounds = copy(bounds); return this; }
    public WindowConfig minimumSize(Dimension minimumSize) {
        this.minimumSize = minimumSize == null ? new Dimension(220, 140) : new Dimension(minimumSize);
        return this;
    }
    public WindowConfig maximizedInsets(Insets insets) {
        maximizedInsets = copy(insets);
        return this;
    }
    public WindowConfig movable(boolean movable) { this.movable = movable; return this; }
    public WindowConfig resizable(boolean resizable) { this.resizable = resizable; return this; }
    public WindowConfig closable(boolean closable) { this.closable = closable; return this; }
    public WindowConfig minimizable(boolean minimizable) { this.minimizable = minimizable; return this; }
    public WindowConfig maximizable(boolean maximizable) { this.maximizable = maximizable; return this; }
    public WindowConfig modal(boolean modal) { this.modal = modal; return this; }
    public WindowConfig closeOnEscape(boolean closeOnEscape) { this.closeOnEscape = closeOnEscape; return this; }
    public WindowConfig snapEnabled(boolean snapEnabled) { this.snapEnabled = snapEnabled; return this; }
    public WindowConfig snapLayoutsEnabled(boolean enabled) { this.snapLayoutsEnabled = enabled; return this; }
    public WindowConfig inheritSnapLayoutsEnabled() { this.snapLayoutsEnabled = null; return this; }
    public WindowConfig snapLayoutTrigger(WindowSnapLayoutTrigger trigger) {
        this.snapLayoutTrigger = Objects.requireNonNull(trigger); return this;
    }
    public WindowConfig inheritSnapLayoutTrigger() { this.snapLayoutTrigger = null; return this; }
    public WindowConfig closeOperation(WindowCloseOperation operation) {
        this.closeOperation = Objects.requireNonNull(operation, "operation"); return this;
    }
    public WindowConfig style(WindowStyle style) { this.style = style; return this; }

    public String getKey() { return key; }
    public String getTitle() { return title; }
    public Component getContent() { return content; }
    public Icon getIcon() { return icon; }
    public Rectangle getBounds() { return copy(bounds); }
    public Dimension getMinimumSize() { return new Dimension(minimumSize); }
    public Insets getMaximizedInsets() { return copy(maximizedInsets); }
    public boolean isMovable() { return movable; }
    public boolean isResizable() { return resizable; }
    public boolean isClosable() { return closable; }
    public boolean isMinimizable() { return minimizable; }
    public boolean isMaximizable() { return maximizable; }
    public boolean isModal() { return modal; }
    public boolean isCloseOnEscape() { return closeOnEscape; }
    public boolean isSnapEnabled() { return snapEnabled; }
    public Boolean getSnapLayoutsEnabled() { return snapLayoutsEnabled; }
    public WindowSnapLayoutTrigger getSnapLayoutTrigger() { return snapLayoutTrigger; }
    public WindowCloseOperation getCloseOperation() { return closeOperation; }
    public WindowStyle getStyle() { return style; }

    private static Rectangle copy(Rectangle rectangle) {
        return rectangle == null ? null : new Rectangle(rectangle);
    }

    private static Insets copy(Insets insets) {
        return insets == null ? null : new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }
}
