package dtm.stools.component.tree;

import java.awt.event.MouseEvent;
import java.util.List;

public record TreePopupContext<T>(
        TreeView<T> tree,
        TreeNode<T> node,
        List<TreeNode<T>> selectedNodes,
        MouseEvent mouseEvent
) {
}
