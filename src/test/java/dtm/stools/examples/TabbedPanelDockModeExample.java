package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.tab.EventTabbedPanel;
import dtm.stools.component.panels.tab.TabEvent;
import dtm.stools.component.panels.tab.TabSeparatorFactory;
import dtm.stools.component.panels.tab.TabSplitPlacement;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;

public class TabbedPanelDockModeExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TabbedPanel Dock Mode Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = new TabbedPanel(JTabbedPane.TOP);
        tabs.setScrollableTabsEnabled(true);
        tabs.setDockModeEnabled(true);
        tabs.setSplitDropZoneSize(130);
        tabs.setReorderTabsWhileDragging(true);
        tabs.setTabListButtonVisible(true);
        tabs.setTabSeparatorFactory(TabSeparatorFactory.solid(
                new Color(0x2563EB),
                5,
                true
        ));

        JButton dockCurrentButton = new JButton("Separar atual");
        dockCurrentButton.addActionListener(event -> {
            TabbedPanel group = findCurrentGroup(tabs);
            if (group == null) return;

            String key = group.getCurrentKey();
            if (key != null && group.dockTab(key, TabSplitPlacement.RIGHT)) {
                System.out.println("Tab separada: " + key);
            }
        });

        JButton reattachAllButton = new JButton("Reencaixar tudo");
        reattachAllButton.addActionListener(event -> {
            int moved = tabs.reattachAllTabs();
            System.out.println("Tabs reencaixadas: " + moved);
        });

        tabs.addEventListener(EventTabbedPanel.TAB_SPLIT, event -> {
            TabEvent tabEvent = (TabEvent) event;
            TabSplitPlacement placement = (TabSplitPlacement) tabEvent.getProperties().get("placement");
            System.out.println("Tab split: " + tabEvent.getKey() + " -> " + placement);
        });

        tabs.addTab("editor", "Editor.java", createTextPanel("""
                public class Editor {
                    public void open() {
                        System.out.println("Dock mode enabled");
                    }
                }
                """));
        tabs.addTab("preview", "Preview", createPreviewPanel());
        tabs.addTab("console", "Console", createTextPanel("""
                Build started
                Compiling sources
                Ready
                """));
        tabs.addTab("tasks", "Tasks", createTasksPanel());

        JPanel content = new JPanel(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(dockCurrentButton);
        toolbar.add(reattachAllButton);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static TabbedPanel findCurrentGroup(TabbedPanel root) {
        for (TabbedPanel group : root.getDockGroups()) {
            if (group.getCurrentKey() != null) {
                return group;
            }
        }
        return root.getCurrentKey() == null ? null : root;
    }

    private static JComponent createTextPanel(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setEditable(false);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private static JComponent createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Preview");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JProgressBar progress = new JProgressBar(0, 100);
        progress.setValue(72);
        progress.setStringPainted(true);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JList<>(new String[]{
                "Layout loaded",
                "Components rendered",
                "Theme applied",
                "Waiting for input"
        })), BorderLayout.CENTER);
        panel.add(progress, BorderLayout.SOUTH);
        return panel;
    }

    private static JComponent createTasksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable table = new JTable(
                new Object[][]{
                        {"ST-101", "Implement dock mode", "Done"},
                        {"ST-102", "Add example", "Open"},
                        {"ST-103", "Compile project", "Pending"}
                },
                new Object[]{"ID", "Title", "Status"}
        );
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
