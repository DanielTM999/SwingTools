package dtm.stools.component.menu.bar.action;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuAction extends AbstractAction {
    private final String id;
    private Consumer<ActionEvent> handler;

    public MenuAction(String id, String text) {
        super(Objects.requireNonNullElse(text, ""));
        this.id = Objects.requireNonNullElse(id, "").trim();
        putValue(Action.ACTION_COMMAND_KEY, this.id);
    }

    public static MenuAction of(String id, String text) {
        return new MenuAction(id, text);
    }

    public MenuAction icon(Icon icon) {
        putValue(Action.SMALL_ICON, icon);
        return this;
    }

    public MenuAction shortcut(KeyStroke shortcut) {
        putValue(Action.ACCELERATOR_KEY, shortcut);
        return this;
    }

    public MenuAction shortcut(String shortcut) {
        return shortcut(KeyStroke.getKeyStroke(shortcut));
    }

    public MenuAction tooltip(String tooltip) {
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        return this;
    }

    public MenuAction enabled(boolean enabled) {
        setEnabled(enabled);
        return this;
    }

    public MenuAction onAction(Consumer<ActionEvent> handler) {
        this.handler = handler;
        return this;
    }

    public String getId() {
        return id;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (handler != null) {
            handler.accept(e);
        }
    }
}
