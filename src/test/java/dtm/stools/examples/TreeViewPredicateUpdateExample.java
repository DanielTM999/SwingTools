package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.tree.TreeNode;
import dtm.stools.component.tree.TreeView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class TreeViewPredicateUpdateExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            createAndShow();
        });
    }

    private static void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TreeView Predicate Update Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(760, 520);
        frame.setLocationRelativeTo(null);

        TreeView<String> tree = createTree();
        JTextArea log = createLog();

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(createToolbar(frame, tree, log), BorderLayout.NORTH);
        root.add(new JScrollPane(tree), BorderLayout.CENTER);
        root.add(new JScrollPane(log), BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static TreeView<String> createTree() {
        TreeNode<String> root = new TreeNode<>("workspace", "workspace");
        TreeView<String> tree = new TreeView<>(root);

        TreeNode<String> src = tree.addNode(root, new TreeNode<>("src", "src"));
        tree.addNode(src, new TreeNode<>("src/Main.java", "Main.java"));
        tree.addNode(src, new TreeNode<>("src/AppService.java", "AppService.java"));
        tree.addNode(src, new TreeNode<>("src/App.css", "App.css"));

        TreeNode<String> docs = tree.addNode(root, new TreeNode<>("docs", "docs"));
        tree.addNode(docs, new TreeNode<>("docs/README.md", "README.md"));
        tree.addNode(docs, new TreeNode<>("docs/CHANGELOG.md", "CHANGELOG.md"));

        tree.expandAll();
        return tree;
    }

    private static JToolBar createToolbar(JFrame frame, TreeView<String> tree, JTextArea log) {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton styleJava = new JButton("Style .java");
        styleJava.addActionListener(e -> {
            int changed = tree.updateNodes(
                    node -> node.computeLabel().endsWith(".java"),
                    node -> {
                        node.setForeground(new Color(0x60A5FA));
                        node.setFont(tree.getFont().deriveFont(Font.BOLD));
                        node.setTooltip("Arquivo Java: " + node.getData());
                    }
            );
            log.append("Nodes .java alterados: " + changed + "\n");
        });

        JButton renameReadme = new JButton("Rename README");
        renameReadme.addActionListener(e -> {
            boolean changed = tree.updateFirstNode(
                    node -> "README.md".equals(node.computeLabel()),
                    node -> {
                        node.setLabel("README.md (docs)");
                        node.setBackground(new Color(0x3A2F16));
                        node.setForeground(new Color(0xFBBF24));
                        node.setData("docs/readme-updated");
                    }
            );
            log.append("README alterado: " + changed + "\n");
        });

        JButton mutateSelected = new JButton("Mutate selected");
        mutateSelected.addActionListener(e -> {
            TreeNode<String> selected = tree.getSelectedNode();
            if (selected == null) {
                JOptionPane.showMessageDialog(frame, "Selecione um node primeiro.");
                return;
            }

            boolean changed = tree.updateFirstNode(
                    node -> node == selected,
                    node -> {
                        node.setLabel(node.computeLabel() + " *");
                        node.setForeground(new Color(0x34D399));
                        node.setData("mutated:" + node.getData());
                    }
            );
            log.append("Selecionado alterado: " + changed + " -> " + selected.getData() + "\n");
        });

        JButton resetStyle = new JButton("Clear style");
        resetStyle.addActionListener(e -> {
            int changed = tree.updateNodes(
                    node -> true,
                    node -> {
                        node.setForeground(null);
                        node.setBackground(null);
                        node.setFont(null);
                        node.setTooltip(null);
                    }
            );
            log.append("Estilo limpo em nodes: " + changed + "\n");
        });

        toolbar.add(styleJava);
        toolbar.add(renameReadme);
        toolbar.add(mutateSelected);
        toolbar.add(resetStyle);
        return toolbar;
    }

    private static JTextArea createLog() {
        JTextArea log = new JTextArea(5, 20);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        return log;
    }
}
