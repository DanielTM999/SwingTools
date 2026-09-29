package dtm.stools.component.tree;

import org.junit.jupiter.api.Test;

import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeViewDropHighlightTest {

    @Test
    void importDataClearsTheHighlightEvenWhenTheDropIsRejected() {
        TreeView<String> tree = tree();
        tree.setExternalDropEnabled(true);
        tree.setDropHighlight(0);

        tree.getTransferHandler().importData(new TransferHandler.TransferSupport(tree, new StringSelection("x")));

        assertEquals(-1, tree.dropHighlightRow);
    }

    @Test
    void structuralChangesClearAStaleHighlightRow() {
        TreeView<String> tree = tree();
        tree.setDropHighlight(0);

        tree.getTreeModel().nodeStructureChanged(tree.getRootNode());

        assertEquals(-1, tree.dropHighlightRow);
    }

    private static TreeView<String> tree() {
        TreeView<String> tree = new TreeView<>();
        tree.setRootVisible(false);
        TreeNode<String> root = new TreeNode<>("root", "root");
        root.addChild(new TreeNode<>("a", "a"));
        tree.setRoot(root);
        return tree;
    }
}
