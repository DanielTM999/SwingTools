package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.window.WindowConfig;
import dtm.stools.component.panels.window.WindowDesktopPanel;
import dtm.stools.component.panels.window.WindowSnapLayoutTrigger;

import javax.swing.*;
import java.awt.*;

public final class WindowDesktopPanelExample {
    private WindowDesktopPanelExample() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            WindowDesktopPanel desktop = new WindowDesktopPanel();
            desktop.minimizedBarAutoHideEnabled(true);
            desktop.minimizedBarContextMenuEnabled(true);
            desktop.snapLayoutsEnabled(true)
                    .snapAssistEnabled(true)
                    .snapLayoutTrigger(WindowSnapLayoutTrigger.TOP_CENTER)
                    .snapLayoutHoverDelay(450);
            JFrame frame = new JFrame("SwingTools WindowPanel");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(desktop);
            frame.setSize(1100, 760);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JTextArea editorContent = new JTextArea("""
                    Arraste esta janela interna ate o topo central do desktop.
                    Mova sobre uma vaga da barra e solte para abrir o Snap Assist.
                    """);
            desktop.openWindow(new WindowConfig(
                            "editor", "Editor.java", new JScrollPane(editorContent))
                    .bounds(new Rectangle(40, 35, 620, 420)));
            desktop.openWindow(new WindowConfig("properties", "Propriedades", new JScrollPane(new JTree()))
                    .bounds(new Rectangle(690, 70, 300, 420)));
        });
    }
}
