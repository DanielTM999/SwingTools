package dtm.stools.component.tree;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeViewExpansionTest {

    @Test
    void keepsAHiddenRootExpandedWhenTheSnapshotDoesNotKnowIt() {
        TreeView<String> tree = hiddenRootTree();
        int rows = tree.getRowCount();
        assertTrue(rows > 0, "a arvore deveria comecar com linhas visiveis");

        tree.restoreExpansion(Set.of("id-de-outra-arvore"));

        assertEquals(rows, tree.getRowCount());
    }

    @Test
    void collapsesRegularNodesLeftOutOfTheSnapshot() {
        TreeView<String> tree = hiddenRootTree();
        tree.expandAll();
        assertEquals(2, tree.getRowCount());

        tree.restoreExpansion(Set.of());

        assertEquals(1, tree.getRowCount());
    }

    @Test
    void collapsesAVisibleRootLeftOutOfTheSnapshot() {
        TreeView<String> tree = hiddenRootTree();
        tree.setRootVisible(true);
        tree.expandAll();
        assertEquals(3, tree.getRowCount());

        tree.restoreExpansion(Set.of());

        assertEquals(1, tree.getRowCount());
    }

    private static TreeView<String> hiddenRootTree() {
        TreeView<String> tree = new TreeView<>();
        tree.setRootVisible(false);
        tree.setRoot(node("root", node("group", node("leaf"))));
        return tree;
    }

    @SafeVarargs
    private static TreeNode<String> node(String id, TreeNode<String>... children) {
        TreeNode<String> node = new TreeNode<>(id, id);
        node.setId(id);
        for (TreeNode<String> child : children) node.addChild(child);
        return node;
    }
}
