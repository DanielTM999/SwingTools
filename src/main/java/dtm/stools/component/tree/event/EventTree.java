package dtm.stools.component.tree.event;

import dtm.stools.component.tree.CheckState;
import dtm.stools.component.tree.TreeNode;

import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.List;

public interface EventTree<T> {

    TreeNode<T> getNode();

    default TreePath getPath() {
        return null;
    }

    default List<TreeNode<T>> getSelected() {
        return List.of();
    }

    default Object getOldValue() {
        return null;
    }

    default Object getNewValue() {
        return null;
    }

    default CheckState getCheckState() {
        return null;
    }

    default MouseEvent getMouseEvent() {
        return null;
    }
}
