package dtm.stools.component.menu.bar.config;

import dtm.stools.component.menu.bar.MenuBar;
import dtm.stools.component.menu.bar.event.MenuBarEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuItemConfig {
    private final String id;
    private final String text;
    private Icon icon;
    private String iconId;
    private KeyStroke accelerator;
    private Integer mnemonic;
    private String tooltip;
    private boolean enabled = true;
    private Action action;
    private Consumer<MenuBarEvent> clickHandler;
    private Consumer<MenuBar.Item> configurer;

    public MenuItemConfig(String id, String text) {
        this.id = Objects.requireNonNullElse(id, "").trim();
        this.text = Objects.requireNonNullElse(text, "");
    }

    public MenuItemConfig(Action action) {
        this(resolveActionId(action), resolveActionText(action));
        this.action = action;
        if (action != null) {
            Object iconValue = action.getValue(Action.SMALL_ICON);
            if (iconValue instanceof Icon actionIcon) this.icon = actionIcon;
            Object acceleratorValue = action.getValue(Action.ACCELERATOR_KEY);
            if (acceleratorValue instanceof KeyStroke keyStroke) this.accelerator = keyStroke;
            Object mnemonicValue = action.getValue(Action.MNEMONIC_KEY);
            if (mnemonicValue instanceof Integer key) this.mnemonic = key;
            Object tooltipValue = action.getValue(Action.SHORT_DESCRIPTION);
            if (tooltipValue != null) this.tooltip = tooltipValue.toString();
            this.enabled = action.isEnabled();
        }
    }

    public static MenuItemConfig fromAction(Action action) {
        return new MenuItemConfig(action);
    }

    public MenuItemConfig icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public MenuItemConfig icon(String iconId) {
        this.iconId = iconId;
        return this;
    }

    public MenuItemConfig accelerator(KeyStroke accelerator) {
        this.accelerator = accelerator;
        return this;
    }

    public MenuItemConfig shortcut(KeyStroke accelerator) {
        return accelerator(accelerator);
    }

    public MenuItemConfig shortcut(String accelerator) {
        return accelerator(KeyStroke.getKeyStroke(accelerator));
    }

    public MenuItemConfig mnemonic(int mnemonic) {
        this.mnemonic = mnemonic;
        return this;
    }

    public MenuItemConfig mnemonic(char mnemonic) {
        return mnemonic(KeyEvent.getExtendedKeyCodeForChar(mnemonic));
    }

    public MenuItemConfig tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public MenuItemConfig enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MenuItemConfig action(Action action) {
        this.action = action;
        return this;
    }

    public MenuItemConfig onClick(Consumer<MenuBarEvent> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public MenuItemConfig onAction(Consumer<ActionEvent> handler) {
        this.action = new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (handler != null) handler.accept(e);
            }
        };
        this.action.putValue(Action.ACTION_COMMAND_KEY, id);
        return this;
    }

    public MenuItemConfig configure(Consumer<MenuBar.Item> configurer) {
        this.configurer = configurer;
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

    public KeyStroke getAccelerator() {
        return accelerator;
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

    public Action getAction() {
        return action;
    }

    public Consumer<MenuBarEvent> getClickHandler() {
        return clickHandler;
    }

    public Consumer<MenuBar.Item> getConfigurer() {
        return configurer;
    }

    private static String resolveActionId(Action action) {
        if (action == null) return "";
        Object command = action.getValue(Action.ACTION_COMMAND_KEY);
        if (command != null && !command.toString().isBlank()) return command.toString();
        Object name = action.getValue(Action.NAME);
        return name == null ? "" : name.toString().toLowerCase().replaceAll("\\s+", "-");
    }

    private static String resolveActionText(Action action) {
        if (action == null) return "";
        Object name = action.getValue(Action.NAME);
        return name == null ? "" : name.toString();
    }
}
