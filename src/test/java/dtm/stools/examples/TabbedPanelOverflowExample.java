package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.tab.TabOverflowMode;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TabbedPanelOverflowExample {

    private static final String[] INITIAL_FILES = {
            "README.md",
            "pom.xml",
            "Application.java",
            "MainWindow.java",
            "EditorController.java",
            "ProjectExplorer.java",
            "SearchService.java",
            "SettingsPanel.java",
            "TerminalView.java",
            "ThemeManager.java",
            "KeyboardShortcuts.java",
            "PluginRegistry.java",
            "HttpClient.java",
            "UserPreferences.java"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TabbedPanel - Overflow moderno");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 620);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = new TabbedPanel();
        tabs.setTabScrollButtonSize(28)
                .setTabScrollButtonArc(8)
                .setTabScrollButtonStrokeWidth(1.8f);

        AtomicInteger fileSequence = new AtomicInteger(INITIAL_FILES.length + 1);
        for (int index = 0; index < INITIAL_FILES.length; index++) {
            addEditorTab(tabs, INITIAL_FILES[index], index);
        }

        tabs.setPinned("file-0", true);
        tabs.setDirty("file-3", true);
        tabs.setBadge("file-6", "3");
        tabs.switchTo("file-8");

        JButton addTab = new JButton("Nova aba");
        addTab.addActionListener(event -> {
            int sequence = fileSequence.getAndIncrement();
            String key = "file-" + sequence;
            String title = "Untitled" + sequence + ".java";
            tabs.addTab(key, title, createEditor(title, sequence));
            tabs.switchTo(key);
        });

        JButton closeCurrent = new JButton("Fechar atual");
        closeCurrent.addActionListener(event -> tabs.closeCurrentTab());

        JComboBox<TabOverflowMode> overflowMode = new JComboBox<>(TabOverflowMode.values());
        overflowMode.setSelectedItem(tabs.getTabOverflowMode());
        overflowMode.setToolTipText("MENU é o padrão; SCROLL_BUTTONS mantém os chevrons opcionais");
        overflowMode.addActionListener(event -> tabs.setTabOverflowMode(
                (TabOverflowMode) overflowMode.getSelectedItem()
        ));

        JLabel hint = new JLabel("Clique nos três pontos para ver as abas ocultas.");

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(addTab);
        toolbar.add(closeCurrent);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Overflow: "));
        toolbar.add(overflowMode);
        toolbar.addSeparator();
        toolbar.add(hint);

        JPanel content = new JPanel(new BorderLayout());
        content.add(toolbar, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static void addEditorTab(TabbedPanel tabs, String title, int index) {
        tabs.addTab("file-" + index, title, createEditor(title, index + 1));
    }

    private static JComponent createEditor(String title, int line) {
        JTextArea editor = new JTextArea("""
                // %s
                public final class Example {
                    public void render() {
                        System.out.println("Modern tab overflow #%d");
                    }
                }
                """.formatted(title, line));
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JScrollPane scrollPane = new JScrollPane(editor);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }
}
