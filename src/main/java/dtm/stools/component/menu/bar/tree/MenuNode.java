package dtm.stools.component.menu.bar.tree;

import dtm.stools.component.menu.bar.MenuBar;
import dtm.stools.component.menu.bar.event.MenuBarEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tree-shaped, mutable representation of a {@link MenuBar} menu structure.
 *
 * <p>Use it together with
 * {@link MenuBar#getMenuTree()},
 * {@link MenuBar#setMenuTree(MenuNode)} and
 * {@link MenuBar#updateMenuTree(Consumer)} to capture, manipulate and
 * reapply the menu structure as a single object.</p>
 */
@Getter
@Setter
@ToString(exclude = "parent")
public class MenuNode {

    public enum Type {
        ROOT, MENU, ITEM, CHECK, RADIO, SEPARATOR, SECTION
    }

    @Setter(AccessLevel.NONE)
    private final Type type;
    private String id;
    private String text;
    private Icon icon;
    private String iconId;
    private KeyStroke accelerator;
    private Integer mnemonic;
    private String tooltip;
    private boolean enabled = true;
    private boolean selected;
    private String radioGroup;
    private Action action;
    private Consumer<MenuBarEvent> clickHandler;
    private Consumer<MenuBarEvent> mouseEnterHandler;
    private Consumer<MenuBarEvent> mouseExitHandler;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final List<MenuNode> children = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private MenuNode parent;

    public MenuNode(Type type, String id, String text) {
        this.type = Objects.requireNonNull(type, "type");
        this.id = id == null ? "" : id;
        this.text = text == null ? "" : text;
    }

    public static MenuNode root() {
        return new MenuNode(Type.ROOT, "", "");
    }

    public static MenuNode menu(String id, String text) {
        return new MenuNode(Type.MENU, id, text);
    }

    public static MenuNode item(String id, String text) {
        return new MenuNode(Type.ITEM, id, text);
    }

    public static MenuNode check(String id, String text, boolean selected) {
        MenuNode node = new MenuNode(Type.CHECK, id, text);
        node.selected = selected;
        return node;
    }

    public static MenuNode radio(String id, String text, String group, boolean selected) {
        MenuNode node = new MenuNode(Type.RADIO, id, text);
        node.radioGroup = group;
        node.selected = selected;
        return node;
    }

    public static MenuNode separator() {
        return new MenuNode(Type.SEPARATOR, "", "");
    }

    public static MenuNode section(String text) {
        return new MenuNode(Type.SECTION, "", text);
    }

    public MenuNode add(MenuNode child) {
        if (child == null) return this;
        if (child.parent != null) child.parent.remove(child);
        child.parent = this;
        children.add(child);
        return this;
    }

    public MenuNode add(MenuNode... nodes) {
        if (nodes != null) {
            for (MenuNode n : nodes) add(n);
        }
        return this;
    }

    public MenuNode addAll(Iterable<MenuNode> nodes) {
        if (nodes != null) {
            for (MenuNode n : nodes) add(n);
        }
        return this;
    }

    public MenuNode addFirst(MenuNode child) {
        return insert(0, child);
    }

    public MenuNode addFirst(MenuNode... nodes) {
        if (nodes != null) {
            for (int i = nodes.length - 1; i >= 0; i--) insert(0, nodes[i]);
        }
        return this;
    }

    public MenuNode insert(int index, MenuNode child) {
        if (child == null) return this;
        if (child.parent != null) child.parent.remove(child);
        child.parent = this;
        int i = Math.max(0, Math.min(index, children.size()));
        children.add(i, child);
        return this;
    }

    public MenuNode remove(MenuNode child) {
        if (child != null && children.remove(child)) {
            child.parent = null;
        }
        return this;
    }

    public MenuNode removeById(String id) {
        if (id == null) return this;
        Iterator<MenuNode> it = children.iterator();
        while (it.hasNext()) {
            MenuNode c = it.next();
            if (id.equals(c.id)) {
                c.parent = null;
                it.remove();
            }
        }
        return this;
    }

    public MenuNode clearChildren() {
        for (MenuNode c : children) c.parent = null;
        children.clear();
        return this;
    }

    public MenuNode find(String id) {
        if (id == null) return null;
        for (MenuNode c : children) {
            if (id.equals(c.id)) return c;
        }
        return null;
    }

    public MenuNode findRecursive(String id) {
        if (id == null) return null;
        for (MenuNode c : children) {
            if (id.equals(c.id)) return c;
            MenuNode r = c.findRecursive(id);
            if (r != null) return r;
        }
        return null;
    }

    public MenuNode findByPath(String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        MenuNode current = this;
        for (String part : parts) {
            if (current == null) return null;
            current = current.find(part);
        }
        return current;
    }

    public MenuNode forEach(Consumer<MenuNode> visitor) {
        if (visitor == null) return this;
        visitor.accept(this);
        for (MenuNode c : new ArrayList<>(children)) c.forEach(visitor);
        return this;
    }

    public List<MenuNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public MenuNode id(String id) {
        this.id = id == null ? "" : id;
        return this;
    }

    public MenuNode text(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    public MenuNode icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public MenuNode iconId(String iconId) {
        this.iconId = iconId;
        return this;
    }

    public MenuNode accelerator(KeyStroke accelerator) {
        this.accelerator = accelerator;
        return this;
    }

    public MenuNode shortcut(String accelerator) {
        this.accelerator = accelerator == null ? null : KeyStroke.getKeyStroke(accelerator);
        return this;
    }

    public MenuNode mnemonic(Integer mnemonic) {
        this.mnemonic = mnemonic;
        return this;
    }

    public MenuNode tooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public MenuNode enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MenuNode selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public MenuNode radioGroup(String radioGroup) {
        this.radioGroup = radioGroup;
        return this;
    }

    public MenuNode action(Action action) {
        this.action = action;
        return this;
    }

    public MenuNode onClick(Consumer<MenuBarEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    public MenuNode onMouseEnter(Consumer<MenuBarEvent> handler) {
        this.mouseEnterHandler = handler;
        return this;
    }

    public MenuNode onMouseExit(Consumer<MenuBarEvent> handler) {
        this.mouseExitHandler = handler;
        return this;
    }
}
