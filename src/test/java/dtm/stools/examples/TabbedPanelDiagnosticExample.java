package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;

public class TabbedPanelDiagnosticExample {

    private static final Color ERROR = new Color(0xF87171);
    private static final Color WARNING = new Color(0xFBBF24);
    private static final Color INFO = new Color(0x60A5FA);

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
        JFrame frame = new JFrame("TabbedPanel Diagnostic Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = new TabbedPanel();

        tabs.addTab(
                "program",
                "Program.cs",
                createEditorMock("""
                        using System;

                        Console.WriteLine("Hello World")
                        """)
        );

        tabs.addTab(
                "service",
                "UserService.cs",
                createEditorMock("""
                        public class UserService
                        {
                            // TODO: validar entrada
                        }
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

        tabs.setTabDiagnostic("program", ERROR);
        tabs.setTabDiagnostic("service", WARNING);

        frame.setContentPane(tabs);
        frame.add(createControlBar(tabs), BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static JComponent createControlBar(TabbedPanel tabs) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton error = new JButton("Marcar erro (Program.cs)");
        error.addActionListener(e -> tabs.setTabDiagnostic("program", ERROR));

        JButton warning = new JButton("Marcar aviso (UserService.cs)");
        warning.addActionListener(e -> tabs.setTabDiagnostic("service", WARNING));

        JButton info = new JButton("Marcar info (README.md)");
        info.addActionListener(e -> tabs.setTabDiagnostic("readme", INFO));

        JButton clear = new JButton("Limpar todos");
        clear.addActionListener(e -> {
            tabs.clearTabDiagnostic("program");
            tabs.clearTabDiagnostic("service");
            tabs.clearTabDiagnostic("readme");
        });

        bar.add(error);
        bar.add(warning);
        bar.add(info);
        bar.add(clear);

        return bar;
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
