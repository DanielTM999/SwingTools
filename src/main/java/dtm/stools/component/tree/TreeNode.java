package dtm.stools.component.tree;

import lombok.Getter;
import lombok.Setter;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class TreeNode<T> extends DefaultMutableTreeNode {

    @Getter @Setter
    private String id;

    @Getter
    private T data;

    @Getter @Setter
    private String label;

    @Getter @Setter
    private Icon icon;

    @Getter @Setter
    private String tooltip;

    @Getter @Setter
    private Color foreground;

    @Getter @Setter
    private Color background;

    @Getter @Setter
    private Font font;

    @Getter @Setter
    private boolean selectable = true;

    @Getter @Setter
    private boolean enabled = true;

    @Getter @Setter
    private Function<T, String> labelProvider;

    @Getter @Setter
    private Function<T, Icon> iconProvider;

    @Getter @Setter
    private boolean checkable = false;

    @Getter
    private CheckState checkState = CheckState.UNCHECKED;

    @Getter @Setter
    private boolean editable = false;

    @Getter @Setter
    private boolean draggable = true;

    @Getter @Setter
    private boolean dropTarget = true;

    @Getter @Setter
    private boolean lazy = false;

    @Getter @Setter
    private boolean alwaysParent = false;

    @Getter
    private boolean loaded = false;

    @Getter @Setter
    private TreeNodeProvider<T> childrenProvider;

    @Getter @Setter
    private boolean loading = false;

    @Getter @Setter
    private Function<TreeNode<T>, JPopupMenu> popupMenuProvider;

    public TreeNode(T data) {
        this(data, null);
    }

    public TreeNode(T data, String label) {
        super(data, true);
        this.data = data;
        this.id = UUID.randomUUID().toString();
        this.label = label;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setUserObject(Object userObject) {
        super.setUserObject(userObject);
        this.data = (T) userObject;
    }

    public void setData(T data) {
        this.data = data;
        super.setUserObject(data);
    }

    public String computeLabel() {
        if (label != null) return label;
        if (labelProvider != null) {
            try { return labelProvider.apply(data); } catch (Exception ignored) {}
        }
        return data == null ? "" : String.valueOf(data);
    }

    public Icon computeIcon() {
        if (icon != null) return icon;
        if (iconProvider != null) {
            try { return iconProvider.apply(data); } catch (Exception ignored) {}
        }
        return null;
    }

    public JPopupMenu computePopupMenu() {
        if (popupMenuProvider == null) return null;
        try { return popupMenuProvider.apply(this); } catch (Exception ignored) { return null; }
    }

    @Override
    public boolean isLeaf() {
        if (alwaysParent) return false;
        if (lazy && !loaded) return false;
        return getChildCount() == 0;
    }

    public boolean hasChildren() {
        if (lazy && !loaded) return true;
        return getChildCount() > 0;
    }

    public void setCheckState(CheckState state) {
        this.checkState = state == null ? CheckState.UNCHECKED : state;
    }

    public void toggleCheck() {
        setCheckState(checkState == CheckState.CHECKED ? CheckState.UNCHECKED : CheckState.CHECKED);
    }

    @SuppressWarnings("unchecked")
    public List<TreeNode<T>> getChildrenList() {
        List<TreeNode<T>> list = new ArrayList<>(getChildCount());
        for (int i = 0; i < getChildCount(); i++) {
            list.add((TreeNode<T>) getChildAt(i));
        }
        return list;
    }

    public TreeNode<T> addChild(TreeNode<T> child) {
        add(child);
        return child;
    }

    public TreeNode<T> addChild(T value) {
        TreeNode<T> child = new TreeNode<>(value);
        inheritProvidersTo(child);
        add(child);
        return child;
    }

    public void addChildren(Collection<? extends TreeNode<T>> children) {
        if (children == null) return;
        for (TreeNode<T> c : children) add(c);
    }

    public void replaceChildren(Collection<? extends TreeNode<T>> children) {
        removeAllChildren();
        addChildren(children);
    }

    public void markLoaded() {
        this.loaded = true;
        this.loading = false;
    }

    public void resetLoaded() {
        this.loaded = false;
        this.loading = false;
    }

    protected void inheritProvidersTo(TreeNode<T> child) {
        if (child.labelProvider == null) child.labelProvider = this.labelProvider;
        if (child.iconProvider == null) child.iconProvider = this.iconProvider;
        if (child.childrenProvider == null) child.childrenProvider = this.childrenProvider;
    }

    public void walk(java.util.function.Consumer<TreeNode<T>> visitor) {
        visitor.accept(this);
        for (TreeNode<T> c : getChildrenList()) c.walk(visitor);
    }

    public TreeNode<T> findFirst(Predicate<TreeNode<T>> matcher) {
        if (matcher.test(this)) return this;
        for (TreeNode<T> c : getChildrenList()) {
            TreeNode<T> found = c.findFirst(matcher);
            if (found != null) return found;
        }
        return null;
    }

    public List<TreeNode<T>> findAll(Predicate<TreeNode<T>> matcher) {
        List<TreeNode<T>> out = new ArrayList<>();
        walk(n -> { if (matcher.test(n)) out.add(n); });
        return out;
    }

    public Iterator<TreeNode<T>> iteratorDepthFirst() {
        return ((List<TreeNode<T>>) findAll(n -> true)).iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode<?> other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
