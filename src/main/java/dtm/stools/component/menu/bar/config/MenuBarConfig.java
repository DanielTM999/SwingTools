package dtm.stools.component.menu.bar.config;

import java.util.Objects;
import java.util.function.Function;
import javax.swing.Icon;

public class MenuBarConfig {
    private MenuBarStyle style = MenuBarStyle.modern();
    private boolean customPaintingEnabled = true;
    private boolean popupDecorated = true;
    private boolean handCursorEnabled = true;
    private boolean sideComponentsTransparent = true;
    private Function<String, Icon> iconProvider;

    public static MenuBarConfig modern() {
        return new MenuBarConfig();
    }

    public static MenuBarConfig lookAndFeel() {
        return new MenuBarConfig()
                .customPaintingEnabled(false)
                .popupDecorated(false)
                .handCursorEnabled(false)
                .sideComponentsTransparent(false)
                .style(MenuBarStyle.auto());
    }

    public MenuBarConfig style(MenuBarStyle style) {
        this.style = style == null ? MenuBarStyle.modern() : style;
        return this;
    }

    public MenuBarConfig customPaintingEnabled(boolean customPaintingEnabled) {
        this.customPaintingEnabled = customPaintingEnabled;
        return this;
    }

    public MenuBarConfig popupDecorated(boolean popupDecorated) {
        this.popupDecorated = popupDecorated;
        return this;
    }

    public MenuBarConfig handCursorEnabled(boolean handCursorEnabled) {
        this.handCursorEnabled = handCursorEnabled;
        return this;
    }

    public MenuBarConfig sideComponentsTransparent(boolean sideComponentsTransparent) {
        this.sideComponentsTransparent = sideComponentsTransparent;
        return this;
    }

    public MenuBarConfig iconProvider(Function<String, Icon> iconProvider) {
        this.iconProvider = iconProvider;
        return this;
    }

    public MenuBarConfig useLookAndFeelColors() {
        return style(MenuBarStyle.modern());
    }

    public MenuBarConfig light() {
        return style(MenuBarStyle.light());
    }

    public MenuBarConfig dark() {
        return style(MenuBarStyle.dark());
    }

    public MenuBarConfig copy() {
        return new MenuBarConfig()
                .style(style.copy())
                .customPaintingEnabled(customPaintingEnabled)
                .popupDecorated(popupDecorated)
                .handCursorEnabled(handCursorEnabled)
                .sideComponentsTransparent(sideComponentsTransparent)
                .iconProvider(iconProvider);
    }

    public void copyTo(MenuBarConfig target) {
        Objects.requireNonNull(target, "target");
        target.style(style.copy());
        target.customPaintingEnabled(customPaintingEnabled);
        target.popupDecorated(popupDecorated);
        target.handCursorEnabled(handCursorEnabled);
        target.sideComponentsTransparent(sideComponentsTransparent);
        target.iconProvider(iconProvider);
    }

    public MenuBarStyle getStyle() {
        return style;
    }

    public boolean isCustomPaintingEnabled() {
        return customPaintingEnabled;
    }

    public boolean isPopupDecorated() {
        return popupDecorated;
    }

    public boolean isHandCursorEnabled() {
        return handCursorEnabled;
    }

    public boolean isSideComponentsTransparent() {
        return sideComponentsTransparent;
    }

    public Icon resolveIcon(String id) {
        return iconProvider == null || id == null || id.isBlank() ? null : iconProvider.apply(id);
    }

    public Function<String, Icon> getIconProvider() {
        return iconProvider;
    }
}
