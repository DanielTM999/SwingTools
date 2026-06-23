package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;

public class TabbedPanelExample {

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
        JFrame frame = new JFrame("TabbedPanel Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = new TabbedPanel();

        tabs.addTab(
                "console-app",
                "ConsoleApp1",
                createEditorMock("""
                        <Solution>
                            <Project Path="ConsoleApp1/ConsoleApp1.csproj" />
                        </Solution>
                        """)
        );

        tabs.addTab(
                "program",
                "Program.cs",
                createEditorMock("""
                        using System;

                        Console.WriteLine("Hello World");
                        """)
        );

        tabs.addTab(
                "readme",
                "README.md",
                createEditorMock("""
                        # ConsoleApp1

                        Projeto de exemplo.
                        """)
        );

        tabs.setTabTitleForeground("program", new Color(0xF87171));

        frame.setContentPane(tabs);
        frame.setVisible(true);
    }

    private static JComponent createEditorMock(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setText(text);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        return scrollPane;
    }
}