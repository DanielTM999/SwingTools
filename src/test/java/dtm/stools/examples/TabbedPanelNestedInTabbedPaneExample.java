package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.tab.EventTabbedPanel;
import dtm.stools.component.panels.tab.TabEntry;
import dtm.stools.component.panels.tab.TabEvent;
import dtm.stools.component.panels.tab.TabSeparatorFactory;
import dtm.stools.component.panels.tab.TabSplitPlacement;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TabbedPanelNestedInTabbedPaneExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TabbedPanel aninhado em JTabbedPane");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1180, 720);
        frame.setLocationRelativeTo(null);

        TabbedPanel editorTabs = buildEditorTabs();

        JTabbedPane hostTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        hostTabbedPane.addTab("Editor", editorTabs);
        hostTabbedPane.addTab("Logs", createTextPanel("aplicação iniciada\nworkbench pronto\n"));
        hostTabbedPane.addTab("Tarefas", createTasksPanel());

        JLabel visibleLabel = new JLabel("Visíveis: -");
        visibleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JButton splitRight = new JButton("Separar atual à direita");
        splitRight.addActionListener(event -> dockCurrent(editorTabs, TabSplitPlacement.RIGHT, visibleLabel));

        JButton splitBottom = new JButton("Separar atual abaixo");
        splitBottom.addActionListener(event -> dockCurrent(editorTabs, TabSplitPlacement.BOTTOM, visibleLabel));

        JButton reattachAll = new JButton("Reencaixar tudo");
        reattachAll.addActionListener(event -> {
            int moved = editorTabs.reattachAllTabs();
            System.out.println("Tabs reencaixadas: " + moved);
            refreshVisibleLabel(editorTabs, visibleLabel);
        });

        JButton refresh = new JButton("Atualizar visíveis");
        refresh.addActionListener(event -> refreshVisibleLabel(editorTabs, visibleLabel));

        editorTabs.addEventListener(EventTabbedPanel.TAB_SPLIT, event -> {
            TabEvent tabEvent = (TabEvent) event;
            TabSplitPlacement placement = (TabSplitPlacement) tabEvent.getProperties().get("placement");
            System.out.println("TAB_SPLIT " + tabEvent.getKey() + " -> " + placement);
            SwingUtilities.invokeLater(() -> refreshVisibleLabel(editorTabs, visibleLabel));
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(splitRight);
        toolbar.add(splitBottom);
        toolbar.add(reattachAll);
        toolbar.addSeparator();
        toolbar.add(refresh);
        toolbar.add(visibleLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.add(toolbar, BorderLayout.NORTH);
        content.add(hostTabbedPane, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> refreshVisibleLabel(editorTabs, visibleLabel));
    }

    private static TabbedPanel buildEditorTabs() {
        TabbedPanel tabs = new TabbedPanel(JTabbedPane.TOP);
        tabs.setScrollableTabsEnabled(true);
        tabs.setDockModeEnabled(true);
        tabs.setSplitDropZoneSize(130);
        tabs.setReorderTabsWhileDragging(true);
        tabs.setTabDragEnabled(true);
        tabs.setSplitTabsOnDragEnabled(true);
        tabs.setTabSplitEnabled(true);
        tabs.setTabListButtonVisible(true);
        tabs.setNewTabButtonVisible(false);
        tabs.setTabSeparatorFactory(TabSeparatorFactory.solid(new Color(0x2563EB), 5, false));

        tabs.addTab("editor.main", "Main.java", createTextPanel("""
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello dock mode");
                    }
                }
                """));
        tabs.addTab("editor.readme", "README.md", createTextPanel("""
                # SwingTools

                Painel com suporte a dock e split de abas.
                """));
        tabs.addTab("editor.notes", "Notes.txt", createTextPanel("""
                - validar split dentro de JTabbedPane
                - validar reencaixe consecutivo
                - validar getVisibleTabs()
                """));
        tabs.addTab("editor.preview", "Preview", createPreviewPanel());

        return tabs;
    }

    private static void dockCurrent(TabbedPanel root, TabSplitPlacement placement, JLabel visibleLabel) {
        TabbedPanel group = findCurrentGroup(root);
        if (group == null) return;

        String key = group.getCurrentKey();
        if (key == null) return;

        boolean ok = group.dockTab(key, placement);
        System.out.println("dockTab(" + key + ", " + placement + ") -> " + ok);
        refreshVisibleLabel(root, visibleLabel);
    }

    private static void refreshVisibleLabel(TabbedPanel root, JLabel label) {
        List<TabEntry> visible = root.getVisibleTabs();
        StringBuilder builder = new StringBuilder("Visíveis (" + visible.size() + "): ");
        for (int i = 0; i < visible.size(); i++) {
            if (i > 0) builder.append(", ");
            TabEntry entry = visible.get(i);
            builder.append(entry.getKey()).append('=').append(entry.getTitle());
        }
        label.setText(builder.toString());
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
        progress.setValue(58);
        progress.setStringPainted(true);

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JList<>(new String[]{
                "Inicializando ambiente",
                "Indexando workspace",
                "Aguardando input"
        })), BorderLayout.CENTER);
        panel.add(progress, BorderLayout.SOUTH);
        return panel;
    }

    private static JComponent createTasksPanel() {
        JTable table = new JTable(
                new Object[][]{
                        {"ST-201", "Corrigir split em JTabbedPane", "Done"},
                        {"ST-202", "Implementar getVisibleTabs", "Done"},
                        {"ST-203", "Adicionar example", "Open"}
                },
                new Object[]{"ID", "Título", "Status"}
        );
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
}
