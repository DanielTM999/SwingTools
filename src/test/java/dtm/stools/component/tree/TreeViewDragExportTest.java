package dtm.stools.component.tree;

import org.junit.jupiter.api.Test;

import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeViewDragExportTest {

    @Test
    void withoutExportProviderTheDragStaysInternalOnly() throws Exception {
        TreeView<String> tree = tree();
        TreeView<String>.TreeViewTransferHandler handler = handler(tree);

        Transferable transferable = handler.createTransferable(tree);

        assertEquals(TransferHandler.MOVE, handler.getSourceActions(tree));
        assertArrayEquals(new DataFlavor[]{TreeView.TREE_NODE_ARRAY_FLAVOR}, transferable.getTransferDataFlavors());
        assertFalse(transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
    }

    @Test
    void exportProviderFlavorsAreOfferedAlongsideTheInternalFlavor() throws Exception {
        TreeView<String> tree = tree();
        tree.setDragExportProvider(nodes -> new StringSelection(
                nodes.stream().map(TreeNode::computeLabel).collect(Collectors.joining("\n"))));
        TreeView<String>.TreeViewTransferHandler handler = handler(tree);

        Transferable transferable = handler.createTransferable(tree);

        assertEquals(TransferHandler.COPY_OR_MOVE, handler.getSourceActions(tree));
        assertTrue(transferable.isDataFlavorSupported(TreeView.TREE_NODE_ARRAY_FLAVOR));
        assertTrue(transferable.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals("a", transferable.getTransferData(DataFlavor.stringFlavor));
        TreeNode<?>[] nodes = (TreeNode<?>[]) transferable.getTransferData(TreeView.TREE_NODE_ARRAY_FLAVOR);
        assertEquals("a", nodes[0].getData());
    }

    @Test
    void failingExportProviderFallsBackToInternalDrag() throws Exception {
        TreeView<String> tree = tree();
        tree.setDragExportProvider(nodes -> {
            throw new IllegalStateException();
        });

        Transferable transferable = handler(tree).createTransferable(tree);

        assertEquals(List.of(TreeView.TREE_NODE_ARRAY_FLAVOR), List.of(transferable.getTransferDataFlavors()));
    }

    @SuppressWarnings("unchecked")
    private static TreeView<String>.TreeViewTransferHandler handler(TreeView<String> tree) {
        return (TreeView<String>.TreeViewTransferHandler) tree.getTransferHandler();
    }

    private static TreeView<String> tree() {
        TreeView<String> tree = new TreeView<>();
        tree.setRootVisible(false);
        TreeNode<String> root = new TreeNode<>("root", "root");
        TreeNode<String> child = new TreeNode<>("a", "a");
        root.addChild(child);
        tree.setRoot(root);
        tree.setDragAndDropEnabled(true);
        tree.selectNode(child);
        return tree;
    }
}
