package dtm.stools.component.menu.bar.tree;

import dtm.stools.component.menu.bar.MenuBar;

import java.util.function.Consumer;

public class MenuTreeEditor {

    private final MenuBar bar;
    private final MenuNode tree;

    public MenuTreeEditor(MenuBar bar) {
        this.bar = bar;
        this.tree = bar.getMenuTree();
    }

    public MenuNode tree() {
        return tree;
    }

    public MenuBar bar() {
        return bar;
    }

    public MenuTreeEditor mutate(Consumer<MenuNode> mutator) {
        if (mutator != null) mutator.accept(tree);
        return this;
    }

    public MenuNode find(String id) {
        return tree.find(id);
    }

    public MenuNode findRecursive(String id) {
        return tree.findRecursive(id);
    }

    public MenuNode findByPath(String path) {
        return tree.findByPath(path);
    }

    public MenuTreeEditor add(MenuNode child) {
        tree.add(child);
        return this;
    }

    public MenuTreeEditor add(MenuNode... nodes) {
        tree.add(nodes);
        return this;
    }

    public MenuTreeEditor addFirst(MenuNode child) {
        tree.addFirst(child);
        return this;
    }

    public MenuTreeEditor addFirst(MenuNode... nodes) {
        tree.addFirst(nodes);
        return this;
    }

    public MenuTreeEditor insert(int index, MenuNode child) {
        tree.insert(index, child);
        return this;
    }

    public MenuTreeEditor remove(MenuNode child) {
        tree.remove(child);
        return this;
    }

    public MenuTreeEditor removeById(String id) {
        tree.removeById(id);
        return this;
    }

    public MenuTreeEditor clearAll() {
        tree.clearChildren();
        return this;
    }

    public MenuBar commit() {
        return bar.setMenuTree(tree);
    }

    public MenuBar discard() {
        return bar;
    }
}
