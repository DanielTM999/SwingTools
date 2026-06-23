package dtm.stools.component.tree;

import java.util.List;

public record TreeDropContext<T>(
        TreeView<T> tree,
        List<TreeNode<T>> draggedNodes,
        TreeNode<T> targetNode,
        int childIndex
) {
}
