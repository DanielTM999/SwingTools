package dtm.stools.component.menu.bar.config;

import dtm.stools.component.menu.bar.MenuBar;

import javax.swing.Icon;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuConfig {
    private final String id;
    private final String text;
    private Icon icon;
    private String iconId;
    private Integer mnemonic;
    private String tooltip;
    private boolean enabled = true;
    private Consumer<MenuBar.Menu> builder;

    public MenuConfig(String id, String text) {
        this.id = Objects.requireNonNullElse(id, "").trim();
        this.text = Objects.requireNonNullElse(text, "");
    }

    public MenuConfig icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public MenuConfig icon(String iconId) {
        this.iconId = iconId;
        return this;
    }

    public MenuConfig mnemonic(int mnemonic) {
        this.mnemonic = mnemonic;
        return this;
    }

    public MenuConfig mnemonic(char mnemonic) {
        return mnemonic(KeyEvent.getExtendedKeyCodeForChar(mnemonic));
    }

    public MenuConfig tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public MenuConfig enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MenuConfig build(Consumer<MenuBar.Menu> builder) {
        this.builder = builder;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getIconId() {
        return iconId;
    }

    public Integer getMnemonic() {
        return mnemonic;
    }

    public String getTooltip() {
        return tooltip;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Consumer<MenuBar.Menu> getBuilder() {
        return builder;
    }
}
