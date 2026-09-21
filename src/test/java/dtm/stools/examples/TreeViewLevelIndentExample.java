package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.tree.TreeNode;
import dtm.stools.component.tree.TreeView;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class TreeViewLevelIndentExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TreeView - recuo por nível");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 520);
        frame.setLocationRelativeTo(null);

        TreeView<String> tree = createTree();
        JPanel controls = createControls(tree);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(controls, BorderLayout.NORTH);
        content.add(new JScrollPane(tree), BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static JPanel createControls(TreeView<String> tree) {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner indent = new JSpinner(new SpinnerNumberModel(10, 0, 40, 1));
        JLabel currentValue = new JLabel();

        Runnable applyIndent = () -> {
            int value = (Integer) indent.getValue();
            tree.setLevelIndent(value);
            currentValue.setText(value + " px por nível");
        };
        indent.addChangeListener(event -> applyIndent.run());

        JButton restoreDefault = new JButton("Usar padrão do tema");
        restoreDefault.addActionListener(event -> {
            tree.setLevelIndent(-1);
            currentValue.setText("padrão do tema");
        });

        controls.add(new JLabel("Recuo:"));
        controls.add(indent);
        controls.add(currentValue);
        controls.add(restoreDefault);
        applyIndent.run();
        return controls;
    }

    private static TreeView<String> createTree() {
        TreeNode<String> root = new TreeNode<>("workspace", "workspace");
        TreeNode<String> current = root;
        String[] levels = {
                "projects", "swing-tools", "src", "main", "java",
                "dtm", "stools", "component", "tree"
        };

        for (String level : levels) {
            TreeNode<String> child = new TreeNode<>(level, level);
            current.addChild(child);
            current = child;
        }
        current.addChild(new TreeNode<>("TreeView.java", "TreeView.java"));
        current.addChild(new TreeNode<>("TreeNode.java", "TreeNode.java"));

        TreeView<String> tree = new TreeView<>(root);
        tree.expandAll();
        return tree;
    }
}
