package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.dock.DockConfig;
import dtm.stools.component.panels.dock.DockPanel;
import dtm.stools.component.panels.dock.DockRegion;
import dtm.stools.component.panels.tab.TabbedPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DockPanelExample {

    private static final Color APP_BACKGROUND = new Color(0x1E222A);
    private static final Color PANEL_BACKGROUND = new Color(0x252A33);
    private static final Color PANEL_ALT_BACKGROUND = new Color(0x20242C);
    private static final Color BORDER_COLOR = new Color(0x353B45);
    private static final Color TEXT_COLOR = new Color(0xD7DAE0);
    private static final Color MUTED_TEXT_COLOR = new Color(0x9AA4B2);
    private static final Color ACCENT_COLOR = new Color(0x7AA2F7);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DockPanelExample::showDockPanelExample);
    }

    private static void showDockPanelExample() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("DockPanel - bottom full width");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 620));
        frame.setSize(1280, 760);
        frame.setLocationRelativeTo(null);

        DockPanel dock = createDockPanel();
        addExampleDocks(dock);

        frame.setContentPane(dock);
        frame.setVisible(true);
    }

    private static DockPanel createDockPanel() {
        DockPanel dock = new DockPanel((dockPanel, region) -> {
            TabbedPanel group = new TabbedPanel(JTabbedPane.TOP);
            group.setScrollableTabsEnabled(true);
            group.setTabMenuEnabled(false);
            group.setCloseButtonsVisible(false);
            group.setTabListButtonVisible(region == DockRegion.CENTER);
            group.setTabStyle(
                    MUTED_TEXT_COLOR,
                    TEXT_COLOR,
                    PANEL_ALT_BACKGROUND,
                    PANEL_BACKGROUND
            );
            group.getTabbedPane().setBackground(PANEL_ALT_BACKGROUND);
            group.getTabbedPane().setForeground(TEXT_COLOR);
            group.setBackground(PANEL_BACKGROUND);
            return group;
        });

        dock.setDockHorizontalDropEnabled(true);
        dock.setSplitOneTouchExpandableEnabled(false);
        dock.setDockDragLockedRegions(DockRegion.CENTER);
        dock.setRegionPreferredSize(DockRegion.TOP_LEFT, new Dimension(300, 260));
        dock.setRegionPreferredSize(DockRegion.BOTTOM_LEFT, new Dimension(300, 220));
        dock.setRegionPreferredSize(DockRegion.RIGHT, new Dimension(280, 1));
        dock.setRegionPreferredSize(DockRegion.BOTTOM, new Dimension(1, 230));
        return dock;
    }

    private static void addExampleDocks(DockPanel dock) {
        dock.addDock(new DockConfig("project", "Project", scroll(projectTree()))
                .topLeft()
                .closable(false)
                .tabHeaderVisible(false));

        dock.addDock(new DockConfig("structure", "Structure", scroll(list(
                "Main",
                "createDockPanel()",
                "addExampleDocks()",
                "projectTree()",
                "editor()",
                "terminal()"
        )))
                .bottomLeft()
                .closable(false));

        dock.addDock(new DockConfig("editor", "Main.java", editor())
                .center()
                .closable(false));

        dock.addDock(new DockConfig("right", "Application", panel("Run configuration", list(
                "DockPanelExample",
                "Java 21",
                "Working directory: SwingTools",
                "Use classpath of module"
        )))
                .right()
                .closable(false));

        dock.addDock(new DockConfig("terminal", "Terminal", terminal())
                .bottom()
                .closable(false)
                .allowedDropRegions(DockRegion.BOTTOM, DockRegion.BOTTOM_RIGHT));
    }

    private static JTree projectTree() {
        var root = new javax.swing.tree.DefaultMutableTreeNode("SwingTools");
        var src = new javax.swing.tree.DefaultMutableTreeNode("src");
        var main = new javax.swing.tree.DefaultMutableTreeNode("main");
        var java = new javax.swing.tree.DefaultMutableTreeNode("java");
        var dtm = new javax.swing.tree.DefaultMutableTreeNode("dtm.stools");
        var dock = new javax.swing.tree.DefaultMutableTreeNode("component.panels.dock");

        dock.add(new javax.swing.tree.DefaultMutableTreeNode("DockPanel.java"));
        dock.add(new javax.swing.tree.DefaultMutableTreeNode("DockConfig.java"));
        dock.add(new javax.swing.tree.DefaultMutableTreeNode("DockRegion.java"));
        dtm.add(new javax.swing.tree.DefaultMutableTreeNode("Main.java"));
        dtm.add(dock);
        java.add(dtm);
        main.add(java);
        src.add(main);
        root.add(src);
        root.add(new javax.swing.tree.DefaultMutableTreeNode("pom.xml"));

        JTree tree = new JTree(root);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setBackground(PANEL_BACKGROUND);
        tree.setForeground(TEXT_COLOR);
        tree.setBorder(new EmptyBorder(8, 8, 8, 8));
        expandAll(tree);
        return tree;
    }

    private static JTextArea editor() {
        JTextArea area = new JTextArea();
        area.setText("""
                public class Main {
                    public static void main(String[] args) {
                        SwingUtilities.invokeLater(Main::showDockPanelExample);
                    }

                    private static void addExampleDocks(DockPanel dock) {
                        dock.addDock(new DockConfig("structure", "Structure", structure())
                                .bottomLeft());

                        dock.addDock(new DockConfig("terminal", "Terminal", terminal())
                                .bottom());
                    }
                }
                """);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        area.setBackground(new Color(0x1F2430));
        area.setForeground(TEXT_COLOR);
        area.setCaretColor(TEXT_COLOR);
        area.setBorder(new EmptyBorder(16, 18, 16, 18));
        return area;
    }

    private static JComponent terminal() {
        JTextArea area = new JTextArea();
        area.setText("""
                Windows PowerShell
                PS C:\\Users\\danie\\Documents\\development\\java\\SwingTools> mvn -q -DskipTests -Dnative.build.skip=true compile

                [DockPanel]
                BOTTOM_LEFT fica no trilho lateral.
                BOTTOM fica por ultimo e ocupa a largura inteira da janela.
                """);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setBackground(new Color(0x181B21));
        area.setForeground(new Color(0xB8C0CC));
        area.setBorder(new EmptyBorder(12, 14, 12, 14));
        return scroll(area);
    }

    private static JComponent panel(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel label = new JLabel(title);
        label.setForeground(TEXT_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13F));

        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JList<String> list(String... items) {
        JList<String> list = new JList<>(items);
        list.setBackground(PANEL_BACKGROUND);
        list.setForeground(TEXT_COLOR);
        list.setSelectionBackground(ACCENT_COLOR);
        list.setSelectionForeground(Color.WHITE);
        list.setBorder(new EmptyBorder(8, 8, 8, 8));
        return list;
    }

    private static JScrollPane scroll(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        scrollPane.setBackground(PANEL_BACKGROUND);
        return scrollPane;
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

}
