package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;

public class WindowStyle {
    private Color background;
    private Color titleBackground;
    private Color activeTitleBackground;
    private Color foreground;
    private Color inactiveForeground;
    private Color borderColor;
    private Color activeBorderColor;
    private Color shadowColor;
    private Color controlHoverBackground;
    private Color closeHoverBackground;
    private Font titleFont;
    private Cursor titleBarCursor = Cursor.getDefaultCursor();
    private Insets contentInsets = new Insets(0, 0, 0, 0);
    private int titleBarHeight = 36;
    private int arc = 12;
    private int shadowSize = 8;
    private int resizeHandleSize = 7;
    private int controlSize = 30;
    private float borderWidth = 1f;

    public WindowStyle() { installUiDefaults(); }

    protected void installUiDefaults() {
        background = color("Panel.background", new Color(0x2B2D30));
        titleBackground = color("Panel.background", new Color(0x34373B));
        activeTitleBackground = color("Component.focusColor", new Color(0x365880));
        foreground = color("Label.foreground", new Color(0xDFE1E5));
        inactiveForeground = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 175);
        borderColor = color("Component.borderColor", new Color(0x55585D));
        activeBorderColor = color("Component.focusColor", new Color(0x4B89DC));
        shadowColor = new Color(0, 0, 0, 95);
        controlHoverBackground = new Color(255, 255, 255, 30);
        closeHoverBackground = new Color(0xD94A4A);
        titleFont = UIManager.getFont("Label.font");
    }

    public WindowStyle background(Color value) { background = value; return this; }
    public WindowStyle titleBackground(Color value) { titleBackground = value; return this; }
    public WindowStyle activeTitleBackground(Color value) { activeTitleBackground = value; return this; }
    public WindowStyle foreground(Color value) { foreground = value; return this; }
    public WindowStyle inactiveForeground(Color value) { inactiveForeground = value; return this; }
    public WindowStyle borderColor(Color value) { borderColor = value; return this; }
    public WindowStyle activeBorderColor(Color value) { activeBorderColor = value; return this; }
    public WindowStyle shadowColor(Color value) { shadowColor = value; return this; }
    public WindowStyle controlHoverBackground(Color value) { controlHoverBackground = value; return this; }
    public WindowStyle closeHoverBackground(Color value) { closeHoverBackground = value; return this; }
    public WindowStyle titleFont(Font value) { titleFont = value; return this; }
    public WindowStyle titleBarCursor(Cursor value) {
        titleBarCursor = value == null ? Cursor.getDefaultCursor() : value;
        return this;
    }
    public WindowStyle contentInsets(Insets value) { contentInsets = copy(value); return this; }
    public WindowStyle titleBarHeight(int value) { titleBarHeight = Math.max(24, value); return this; }
    public WindowStyle arc(int value) { arc = Math.max(0, value); return this; }
    public WindowStyle shadowSize(int value) { shadowSize = Math.max(0, value); return this; }
    public WindowStyle resizeHandleSize(int value) { resizeHandleSize = Math.max(3, value); return this; }
    public WindowStyle controlSize(int value) { controlSize = Math.max(20, value); return this; }
    public WindowStyle borderWidth(float value) { borderWidth = Math.max(0f, value); return this; }

    public Color getBackground() { return background; }
    public Color getTitleBackground() { return titleBackground; }
    public Color getActiveTitleBackground() { return activeTitleBackground; }
    public Color getForeground() { return foreground; }
    public Color getInactiveForeground() { return inactiveForeground; }
    public Color getBorderColor() { return borderColor; }
    public Color getActiveBorderColor() { return activeBorderColor; }
    public Color getShadowColor() { return shadowColor; }
    public Color getControlHoverBackground() { return controlHoverBackground; }
    public Color getCloseHoverBackground() { return closeHoverBackground; }
    public Font getTitleFont() { return titleFont; }
    public Cursor getTitleBarCursor() { return titleBarCursor; }
    public Insets getContentInsets() { return copy(contentInsets); }
    public int getTitleBarHeight() { return titleBarHeight; }
    public int getArc() { return arc; }
    public int getShadowSize() { return shadowSize; }
    public int getResizeHandleSize() { return resizeHandleSize; }
    public int getControlSize() { return controlSize; }
    public float getBorderWidth() { return borderWidth; }

    public WindowStyle copy() {
        WindowStyle copy = new WindowStyle();
        copy.background = background; copy.titleBackground = titleBackground;
        copy.activeTitleBackground = activeTitleBackground; copy.foreground = foreground;
        copy.inactiveForeground = inactiveForeground; copy.borderColor = borderColor;
        copy.activeBorderColor = activeBorderColor; copy.shadowColor = shadowColor;
        copy.controlHoverBackground = controlHoverBackground; copy.closeHoverBackground = closeHoverBackground;
        copy.titleFont = titleFont; copy.contentInsets = copy(contentInsets);
        copy.titleBarCursor = titleBarCursor;
        copy.titleBarHeight = titleBarHeight; copy.arc = arc; copy.shadowSize = shadowSize;
        copy.resizeHandleSize = resizeHandleSize; copy.controlSize = controlSize; copy.borderWidth = borderWidth;
        return copy;
    }

    private static Color color(String key, Color fallback) {
        Color value = UIManager.getColor(key);
        return value == null ? fallback : value;
    }

    private static Insets copy(Insets value) {
        return value == null ? new Insets(0, 0, 0, 0) : new Insets(value.top, value.left, value.bottom, value.right);
    }
}
