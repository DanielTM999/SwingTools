package dtm.stools.component.menu.popup;

import dtm.stools.component.menu.popup.style.ActionMenuStyle;
import dtm.stools.component.menu.popup.style.BorderFactorySupplier;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActionPopupMenu extends JPopupMenu implements ActionMenuSupport<ActionPopupMenu> {

    private Integer preferredPopupWidth;
    private Integer preferredPopupHeight;

    @Getter
    private ActionMenuStyle actionMenuStyle = new ActionMenuStyle();

    @Getter
    private boolean useRootStyleForChildren = false;

    public ActionPopupMenu() {
        super();
    }

    public ActionPopupMenu(String label) {
        super(label);
    }

    public static ActionPopupMenu create() {
        return new ActionPopupMenu();
    }

    public static ActionPopupMenu create(String label) {
        return new ActionPopupMenu(label);
    }

    public ActionPopupMenu useRootStyleForChildren(boolean useRootStyleForChildren) {
        this.useRootStyleForChildren = useRootStyleForChildren;
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu enableRootStyleForChildren() {
        return useRootStyleForChildren(true);
    }

    public ActionPopupMenu disableRootStyleForChildren() {
        return useRootStyleForChildren(false);
    }

    public ActionPopupMenu style(ActionMenuStyle style) {
        this.actionMenuStyle = style != null ? style : new ActionMenuStyle();
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu background(Color color) {
        this.actionMenuStyle.background(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu foreground(Color color) {
        this.actionMenuStyle.foreground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu selectionBackground(Color color) {
        this.actionMenuStyle.selectionBackground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu selectionForeground(Color color) {
        this.actionMenuStyle.selectionForeground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu popupSize(int width, int height) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu preferredPopupWidth(int width) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu preferredPopupHeight(int height) {
        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu minPopupSize(int width, int height) {
        setMinimumSize(new Dimension(width, height));
        return this;
    }

    public ActionPopupMenu maxPopupSize(int width, int height) {
        setMaximumSize(new Dimension(width, height));
        return this;
    }

    public ActionPopupMenu preferredPopupSize(Dimension size) {
        Objects.requireNonNull(size, "size não pode ser null");

        this.preferredPopupWidth = size.width > 0 ? size.width : null;
        this.preferredPopupHeight = size.height > 0 ? size.height : null;

        revalidate();
        repaint();

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();

        int width = preferredSize.width;
        int height = preferredSize.height;

        if (preferredPopupWidth != null) {
            width = Math.max(preferredPopupWidth, preferredSize.width);
        }

        if (preferredPopupHeight != null) {
            height = Math.max(preferredPopupHeight, preferredSize.height);
        }

        return new Dimension(width, height);
    }

    public ActionPopupMenu border(Border border) {
        setBorder(border);
        return this;
    }

    public ActionPopupMenu border(BorderFactorySupplier supplier) {
        if (supplier != null) {
            setBorder(supplier.create());
        }

        return this;
    }

    @Override
    public ActionPopupMenu item(String text, ActionListener actionListener) {
        return item(text, null, true, actionListener);
    }

    @Override
    public ActionPopupMenu item(String text, Icon icon, ActionListener actionListener) {
        return item(text, icon, true, actionListener);
    }

    @Override
    public ActionPopupMenu item(String text, boolean enabled, ActionListener actionListener) {
        return item(text, null, enabled, actionListener);
    }

    @Override
    public ActionPopupMenu item(
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

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu item(Action action) {
        Objects.requireNonNull(action, "action não pode ser null");

        JMenuItem item = new JMenuItem(action);
        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    public ActionPopupMenu item(JMenuItem item) {
        Objects.requireNonNull(item, "item não pode ser null");

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu checkItem(
            String text,
            boolean selected,
            ActionListener actionListener
    ) {
        return checkItem(text, null, selected, true, actionListener);
    }

    @Override
    public ActionPopupMenu checkItem(
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

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu radioItem(
            String text,
            ButtonGroup group,
            boolean selected,
            ActionListener actionListener
    ) {
        return radioItem(text, null, group, selected, true, actionListener);
    }

    @Override
    public ActionPopupMenu radioItem(
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

        applyStyle(item, actionMenuStyle);

        group.add(item);
        add(item);

        return this;
    }

    @Override
    public ActionPopupMenu submenu(String text, Consumer<ActionMenu> builder) {
        return submenu(text, null, true, builder);
    }

    @Override
    public ActionPopupMenu submenu(
            String text,
            Icon icon,
            Consumer<ActionMenu> builder
    ) {
        return submenu(text, icon, true, builder);
    }

    @Override
    public ActionPopupMenu submenu(
            String text,
            Icon icon,
            boolean enabled,
            Consumer<ActionMenu> builder
    ) {
        JMenu menu = new JMenu(requireText(text));

        if (icon != null) {
            menu.setIcon(icon);
        }

        menu.setEnabled(enabled);

        ActionMenu actionMenu = new ActionMenu(menu, actionMenuStyle, useRootStyleForChildren);

        applyStyle(menu, actionMenu.resolveStyle());

        if (builder != null) {
            builder.accept(actionMenu);
        }

        add(menu);
        return this;
    }

    @Override
    public ActionPopupMenu custom(Component component) {
        Objects.requireNonNull(component, "component não pode ser null");

        applyStyle(component, actionMenuStyle);

        add(component);
        return this;
    }

    @Override
    public ActionPopupMenu separator() {
        addSeparator();
        return this;
    }

    public ActionPopupMenu when(boolean condition, Consumer<ActionPopupMenu> builder) {
        if (condition && builder != null) {
            builder.accept(this);
        }

        return this;
    }

    public ActionPopupMenu when(
            Supplier<Boolean> condition,
            Consumer<ActionPopupMenu> builder
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

    public ActionPopupMenu showAt(Component invoker, int x, int y) {
        show(invoker, x, y);
        return this;
    }

    public ActionPopupMenu showAt(Component invoker, Point point) {
        Objects.requireNonNull(point, "point não pode ser null");

        show(invoker, point.x, point.y);
        return this;
    }

    protected void applyStyleToTree() {
        applyStyle(this, actionMenuStyle);

        for (Component component : getComponents()) {
            applyStyleRecursive(component, actionMenuStyle);
        }

        revalidate();
        repaint();
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