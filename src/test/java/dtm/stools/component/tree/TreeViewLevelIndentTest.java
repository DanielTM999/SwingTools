package dtm.stools.component.tree;

import org.junit.jupiter.api.Test;

import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeViewLevelIndentTest {

    @Test
    void usesTheLookAndFeelIndentByDefault() {
        TreeView<Character> tree = treeWithThreeLevels();
        TreeNode<Character> child = (TreeNode<Character>) tree.rootNode.getChildAt(0);

        assertEquals(-1, tree.getLevelIndent());
        assertEquals(uiIndent(tree), levelOffset(tree, tree.rootNode, child));
    }

    @Test
    void configuresTheHorizontalOffsetForEachLevel() {
        TreeView<Character> tree = treeWithThreeLevels();
        TreeNode<Character> child = (TreeNode<Character>) tree.rootNode.getChildAt(0);
        TreeNode<Character> grandchild = (TreeNode<Character>) child.getChildAt(0);
        tree.setLevelIndent(8);

        assertEquals(8, tree.getLevelIndent());
        assertEquals(8, uiIndent(tree));
        assertEquals(8, levelOffset(tree, tree.rootNode, child));
        assertEquals(8, levelOffset(tree, child, grandchild));
    }

    @Test
    void preservesTheConfiguredIndentWhenTheUiChanges() {
        TreeView<Character> tree = treeWithThreeLevels();
        tree.setLevelIndent(9);
        tree.updateUI();

        assertEquals(9, tree.getLevelIndent());
        assertEquals(9, uiIndent(tree));
    }

    @Test
    void restoresTheLookAndFeelIndent() {
        TreeView<Character> tree = treeWithThreeLevels();
        int defaultIndent = uiIndent(tree);
        tree.setLevelIndent(6);
        tree.setLevelIndent(-1);

        assertEquals(-1, tree.getLevelIndent());
        assertEquals(defaultIndent, uiIndent(tree));
    }

    @Test
    void rejectsValuesBelowTheLookAndFeelSentinel() {
        TreeView<Character> tree = treeWithThreeLevels();
        assertThrows(IllegalArgumentException.class, () -> tree.setLevelIndent(-2));
    }

    private static int uiIndent(TreeView<?> tree) {
        BasicTreeUI treeUI = (BasicTreeUI) tree.getUI();
        return treeUI.getLeftChildIndent() + treeUI.getRightChildIndent();
    }

    private static int levelOffset(TreeView<?> tree, TreeNode<?> parent, TreeNode<?> child) {
        Rectangle parentBounds = tree.getPathBounds(new TreePath(parent.getPath()));
        Rectangle childBounds = tree.getPathBounds(new TreePath(child.getPath()));
        return childBounds.x - parentBounds.x;
    }

    private static TreeView<Character> treeWithThreeLevels() {
        TreeNode<Character> root = new TreeNode<>('r');
        TreeNode<Character> child = new TreeNode<>('c');
        child.addChild(new TreeNode<>('g'));
        root.addChild(child);
        TreeView<Character> tree = new TreeView<>(root);
        tree.expandAll();
        tree.setSize(400, 200);
        tree.doLayout();
        return tree;
    }
}
