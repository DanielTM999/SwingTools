package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.tree.TreeNode;
import dtm.stools.component.tree.TreeView;
import dtm.stools.utils.ImageUtils;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;

public class TreeViewSvgFolderIconExample {

    private static final String FOLDER_CLOSED = "drawables/folder-min-closed.svg";
    private static final String FOLDER_OPEN = "drawables/folder-min-open.svg";

    private static Icon folderClosedIcon;
    private static Icon folderOpenIcon;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { FlatDarkLaf.setup(); } catch (Exception ignored) {}
            reloadFolderIcons();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TreeView com ícones SVG via ImageUtils");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);

        TreeView<String> tree = buildTree();
        attachFolderIconToggle(tree);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(buildToolbar(tree), BorderLayout.NORTH);
        root.add(new JScrollPane(tree), BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static TreeView<String> buildTree() {
        TreeNode<String> root = newFolder("workspace", "workspace");
        TreeView<String> tree = new TreeView<>(root);

        TreeNode<String> src = tree.addNode(root, newFolder("src", "src"));
        tree.addNode(src, newFolder("src/main", "main"));
        tree.addNode(src, newFolder("src/test", "test"));

        TreeNode<String> docs = tree.addNode(root, newFolder("docs", "docs"));
        tree.addNode(docs, newFolder("docs/api", "api"));
        tree.addNode(docs, newFolder("docs/guides", "guides"));

        tree.addNode(root, newFolder("build", "build"));

        tree.expandAll();
        return tree;
    }

    private static TreeNode<String> newFolder(String id, String label) {
        TreeNode<String> node = new TreeNode<>(id, label);
        node.setIcon(folderClosedIcon);
        return node;
    }

    private static void attachFolderIconToggle(TreeView<String> tree) {
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                swapIcon(tree, event.getPath(), folderOpenIcon);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                swapIcon(tree, event.getPath(), folderClosedIcon);
            }
        });

        for (TreeNode<String> node : tree.getRootNode().getChildrenList()) {
            applyIconRecursively(tree, node);
        }
    }

    @SuppressWarnings("unchecked")
    private static void swapIcon(TreeView<String> tree, TreePath path, Icon icon) {
        Object last = path.getLastPathComponent();
        if (last instanceof TreeNode<?> n) {
            ((TreeNode<String>) n).setIcon(icon);
            tree.repaint();
        }
    }

    private static void applyIconRecursively(TreeView<String> tree, TreeNode<String> node) {
        TreePath path = new TreePath(node.getPath());
        node.setIcon(tree.isExpanded(path) ? folderOpenIcon : folderClosedIcon);
        for (TreeNode<String> child : node.getChildrenList()) {
            applyIconRecursively(tree, child);
        }
    }

    private static JToolBar buildToolbar(TreeView<String> tree) {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton toDark = new JButton("L&F Dark");
        toDark.addActionListener(e -> switchLaf(true, tree));

        JButton toLight = new JButton("L&F Light");
        toLight.addActionListener(e -> switchLaf(false, tree));

        bar.add(toDark);
        bar.add(toLight);
        return bar;
    }

    private static void switchLaf(boolean dark, TreeView<String> tree) {
        try {
            if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(tree));
        } catch (Exception ignored) {}

        reloadFolderIcons();
        tree.getRootNode().walk(node -> {
            TreePath path = new TreePath(node.getPath());
            node.setIcon(tree.isExpanded(path) ? folderOpenIcon : folderClosedIcon);
        });
        tree.repaint();
    }

    private static void reloadFolderIcons() {
        Color fg = UIManager.getColor("Tree.foreground");
        if (fg == null) fg = UIManager.getColor("Label.foreground");
        if (fg == null) fg = Color.BLACK;

        folderClosedIcon = ImageUtils
                .getColoredIconByResource(TreeViewSvgFolderIconExample.class, FOLDER_CLOSED, fg)
                .orElseThrow();
        folderOpenIcon = ImageUtils
                .getColoredIconByResource(TreeViewSvgFolderIconExample.class, FOLDER_OPEN, fg)
                .orElseThrow();
    }
}
