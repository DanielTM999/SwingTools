package dtm.stools.component.menu.popup;

import dtm.stools.component.menu.popup.style.ActionMenuStyle;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActionMenu implements ActionMenuSupport<ActionMenu> {

    @Getter
    private final JMenu menu;

    @Getter
    private ActionMenuStyle actionMenuStyle = new ActionMenuStyle();

    @Getter
    private final ActionMenuStyle rootStyle;

    @Getter
    private final boolean useRootStyleForChildren;

    private Integer preferredPopupWidth;
    private Integer preferredPopupHeight;

    public ActionMenu(JMenu menu) {
        this(menu, null, false);
    }

    public ActionMenu(JMenu menu, ActionMenuStyle rootStyle, boolean useRootStyleForChildren) {
        this.menu = Objects.requireNonNull(menu, "menu não pode ser null");
        this.rootStyle = rootStyle;
        this.useRootStyleForChildren = useRootStyleForChildren;
    }

    public static ActionMenu of(JMenu menu) {
        return new ActionMenu(menu);
    }

    public ActionMenu style(ActionMenuStyle style) {
        this.actionMenuStyle = style != null ? style : new ActionMenuStyle();
        applyStyleToTree();
        return this;
    }

    public ActionMenu background(Color color) {
        this.actionMenuStyle.background(color);
        applyStyleToTree();
        return this;
    }

    public ActionMenu foreground(Color color) {
        this.actionMenuStyle.foreground(color);
        applyStyleToTree();
        return this;
    }

    public ActionMenu selectionBackground(Color color) {
        this.actionMenuStyle.selectionBackground(color);
        applyStyleToTree();
        return this;
    }

    public ActionMenu selectionForeground(Color color) {
        this.actionMenuStyle.selectionForeground(color);
        applyStyleToTree();
        return this;
    }

    public ActionMenu popupSize(int width, int height) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        applyPopupPreferredSize();

        return this;
    }

    public ActionMenu preferredPopupWidth(int width) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        applyPopupPreferredSize();

        return this;
    }

    public ActionMenu preferredPopupHeight(int height) {
        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        applyPopupPreferredSize();

        return this;
    }

    protected void applyPopupPreferredSize() {
        JPopupMenu popupMenu = menu.getPopupMenu();

        popupMenu.setPreferredSize(null);

        if (preferredPopupWidth == null && preferredPopupHeight == null) {
            popupMenu.revalidate();
            popupMenu.repaint();
            return;
        }

        Dimension preferredSize = popupMenu.getPreferredSize();

        int width = preferredSize.width;
        int height = preferredSize.height;

        if (preferredPopupWidth != null) {
            width = Math.max(preferredPopupWidth, preferredSize.width);
        }

        if (preferredPopupHeight != null) {
            height = Math.max(preferredPopupHeight, preferredSize.height);
        }

        popupMenu.setPreferredSize(new Dimension(width, height));
        popupMenu.revalidate();
        popupMenu.repaint();
    }

    protected void refreshMenuLayout() {
        if (preferredPopupWidth != null || preferredPopupHeight != null) {
            applyPopupPreferredSize();
            return;
        }

        menu.revalidate();
        menu.repaint();

        JPopupMenu popupMenu = menu.getPopupMenu();
        popupMenu.revalidate();
        popupMenu.repaint();
    }

    @Override
    public ActionMenu item(String text, ActionListener actionListener) {
        return item(text, null, true, actionListener);
    }

    @Override
    public ActionMenu item(String text, Icon icon, ActionListener actionListener) {
        return item(text, icon, true, actionListener);
    }

    @Override
    public ActionMenu item(String text, boolean enabled, ActionListener actionListener) {
        return item(text, null, enabled, actionListener);
    }

    @Override
    public ActionMenu item(
            String text,
            Icon icon,
            boolean enabled,
            ActionListener actionListener
    ) {
        JMenuItem item = new JMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, resolveStyle());

        menu.add(item);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu item(Action action) {
        Objects.requireNonNull(action, "action não pode ser null");

        JMenuItem item = new JMenuItem(action);
        applyStyle(item, resolveStyle());

        menu.add(item);

        refreshMenuLayout();

        return this;
    }

    public ActionMenu item(JMenuItem item) {
        Objects.requireNonNull(item, "item não pode ser null");

        applyStyle(item, resolveStyle());

        menu.add(item);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu checkItem(
            String text,
            boolean selected,
            ActionListener actionListener
    ) {
        return checkItem(text, null, selected, true, actionListener);
    }

    @Override
    public ActionMenu checkItem(
            String text,
            Icon icon,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    ) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setSelected(selected);
        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, resolveStyle());

        menu.add(item);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu radioItem(
            String text,
            ButtonGroup group,
            boolean selected,
            ActionListener actionListener
    ) {
        return radioItem(text, null, group, selected, true, actionListener);
    }

    @Override
    public ActionMenu radioItem(
            String text,
            Icon icon,
            ButtonGroup group,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    ) {
        Objects.requireNonNull(group, "group não pode ser null");

        JRadioButtonMenuItem item = new JRadioButtonMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setSelected(selected);
        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, resolveStyle());

        group.add(item);
        menu.add(item);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu submenu(String text, Consumer<ActionMenu> builder) {
        return submenu(text, null, true, builder);
    }

    @Override
    public ActionMenu submenu(String text, Icon icon, Consumer<ActionMenu> builder) {
        return submenu(text, icon, true, builder);
    }

    @Override
    public ActionMenu submenu(
            String text,
            Icon icon,
            boolean enabled,
            Consumer<ActionMenu> builder
    ) {
        JMenu subMenu = new JMenu(requireText(text));

        if (icon != null) {
            subMenu.setIcon(icon);
        }

        subMenu.setEnabled(enabled);

        ActionMenu childMenu = new ActionMenu(subMenu, rootStyle, useRootStyleForChildren);

        applyStyle(subMenu, childMenu.resolveStyle());

        if (builder != null) {
            builder.accept(childMenu);
        }

        menu.add(subMenu);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu custom(Component component) {
        Objects.requireNonNull(component, "component não pode ser null");

        applyStyle(component, resolveStyle());

        menu.add(component);

        refreshMenuLayout();

        return this;
    }

    @Override
    public ActionMenu separator() {
        menu.addSeparator();

        refreshMenuLayout();

        return this;
    }

    public ActionMenu when(boolean condition, Consumer<ActionMenu> builder) {
        if (condition && builder != null) {
            builder.accept(this);
        }

        return this;
    }

    public ActionMenu when(
            Supplier<Boolean> condition,
            Consumer<ActionMenu> builder
    ) {
        if (
                condition != null &&
                        Boolean.TRUE.equals(condition.get()) &&
                        builder != null
        ) {
            builder.accept(this);
        }

        return this;
    }

    protected ActionMenuStyle resolveStyle() {
        if (!isEmpty(actionMenuStyle)) {
            return actionMenuStyle;
        }

        if (useRootStyleForChildren && !isEmpty(rootStyle)) {
            return rootStyle;
        }

        return null;
    }

    protected void applyStyleToTree() {
        ActionMenuStyle style = resolveStyle();

        applyStyle(menu, style);

        for (Component component : menu.getMenuComponents()) {
            applyStyleRecursive(component, style);
        }

        menu.revalidate();
        menu.repaint();

        JPopupMenu popupMenu = menu.getPopupMenu();
        popupMenu.revalidate();
        popupMenu.repaint();
    }

    protected void applyStyleRecursive(Component component, ActionMenuStyle style) {
        applyStyle(component, style);

        if (component instanceof JMenu menu) {
            for (Component child : menu.getMenuComponents()) {
                applyStyleRecursive(child, style);
            }
        }
    }

    protected void applyStyle(Component component, ActionMenuStyle style) {
        if (component == null || isEmpty(style)) {
            return;
        }

        if (style.getBackground() != null) {
            component.setBackground(style.getBackground());
        }

        if (style.getForeground() != null) {
            component.setForeground(style.getForeground());
        }

        if (component instanceof JMenuItem menuItem) {
            applySelectionStyle(menuItem, style);
        }

        if (component instanceof JMenu menu) {
            JPopupMenu popupMenu = menu.getPopupMenu();

            if (style.getBackground() != null) {
                popupMenu.setBackground(style.getBackground());
            }

            if (style.getForeground() != null) {
                popupMenu.setForeground(style.getForeground());
            }
        }
    }

    protected boolean isEmpty(ActionMenuStyle style) {
        return style == null
                || (
                style.getBackground() == null
                        && style.getForeground() == null
                        && style.getSelectionBackground() == null
                        && style.getSelectionForeground() == null
        );
    }

    protected String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text não pode ser null ou vazio");
        }

        return text;
    }

    protected void applySelectionStyle(JMenuItem menuItem, ActionMenuStyle style) {
        if (menuItem == null || isEmpty(style)) {
            return;
        }

        StringBuilder flatLafStyle = new StringBuilder();

        if (style.getSelectionBackground() != null) {
            flatLafStyle.append("selectionBackground: ")
                    .append(toHex(style.getSelectionBackground()))
                    .append(";");
        }

        if (style.getSelectionForeground() != null) {
            flatLafStyle.append("selectionForeground: ")
                    .append(toHex(style.getSelectionForeground()))
                    .append(";");
        }

        if (!flatLafStyle.isEmpty()) {
            menuItem.putClientProperty("FlatLaf.style", flatLafStyle.toString());
        }
    }

    protected String toHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }
}