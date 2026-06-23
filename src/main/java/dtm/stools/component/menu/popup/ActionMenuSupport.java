package dtm.stools.component.menu.popup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public interface ActionMenuSupport<T> {

    T item(String text, ActionListener actionListener);

    T item(String text, Icon icon, ActionListener actionListener);

    T item(String text, boolean enabled, ActionListener actionListener);

    T item(
            String text,
            Icon icon,
            boolean enabled,
            ActionListener actionListener
    );

    T item(Action action);

    T checkItem(
            String text,
            boolean selected,
            ActionListener actionListener
    );

    T checkItem(
            String text,
            Icon icon,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    );

    T radioItem(
            String text,
            ButtonGroup group,
            boolean selected,
            ActionListener actionListener
    );

    T radioItem(
            String text,
            Icon icon,
            ButtonGroup group,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    );

    T submenu(String text, Consumer<ActionMenu> builder);

    T submenu(String text, Icon icon, Consumer<ActionMenu> builder);

    T submenu(
            String text,
            Icon icon,
            boolean enabled,
            Consumer<ActionMenu> builder
    );

    T custom(Component component);

    T separator();
}