package dtm.stools.component.tree;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface TreeNodeProvider<T> {

    List<TreeNode<T>> getChildren(TreeNode<T> parent) throws Exception;

    default boolean isAsync() {
        return false;
    }

    default boolean hasChildren(TreeNode<T> parent) {
        return true;
    }

    static <T> TreeNodeProvider<T> empty() {
        return parent -> Collections.emptyList();
    }
}
