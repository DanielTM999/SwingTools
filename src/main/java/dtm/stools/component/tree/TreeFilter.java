package dtm.stools.component.tree;

@FunctionalInterface
public interface TreeFilter<T> {
    boolean accept(TreeNode<T> node);

    default TreeFilter<T> and(TreeFilter<T> other) {
        return n -> this.accept(n) && other.accept(n);
    }

    default TreeFilter<T> or(TreeFilter<T> other) {
        return n -> this.accept(n) || other.accept(n);
    }

    default TreeFilter<T> negate() {
        return n -> !this.accept(n);
    }

    static <T> TreeFilter<T> all() {
        return n -> true;
    }

    static <T> TreeFilter<T> none() {
        return n -> false;
    }
}
