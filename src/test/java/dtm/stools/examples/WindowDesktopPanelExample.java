package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.window.WindowConfig;
import dtm.stools.component.panels.window.WindowDesktopPanel;
import dtm.stools.component.panels.window.WindowMinimizedBar;
import dtm.stools.component.panels.window.WindowSnapLayoutTrigger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class WindowDesktopPanelExample {
    private WindowDesktopPanelExample() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();

            WindowDesktopPanel desktop = new WindowDesktopPanel(true, host -> new WindowMinimizedBar()
                    .expandedHeight(44)
                    .collapseDelay(600));
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

            Icon folderIcon = FileSystemView
                    .getFileSystemView()
                    .getSystemIcon(new File(System.getProperty("user.home")));

            JTextArea editorContent = new JTextArea("""
                    Arraste esta janela interna ate o topo central do desktop.
                    Mova sobre uma vaga da barra e solte para abrir o Snap Assist.

                    Minimize as janelas para ver a barra inferior: o icone e
                    normalizado no botao, no menu de contexto e na barra de titulo.

                    Compare a margem da barra de titulo entre as janelas:
                    Propriedades usa 20/12, Assets usa o padrao 10/4 e
                    Margem zero encosta o icone na borda.
                    """);
            desktop.openWindow(new WindowConfig(
                            "editor", "Editor.java", new JScrollPane(editorContent))
                    .bounds(new Rectangle(40, 35, 620, 420)));

            desktop.openWindow(new WindowConfig("properties", "Propriedades", new JScrollPane(new JTree()))
                            .bounds(new Rectangle(690, 70, 300, 420))
                            .icon(folderIcon))
                    .style(style -> style
                            .titleBarInsets(0, 20, 0, 12)
                            .titleBarIconGap(12));

            desktop.openWindow(new WindowConfig("assets", "Assets (icone 64px)", new JScrollPane(new JList<>(
                            new String[]{"logo.png", "sprite.png", "background.jpg"})))
                    .bounds(new Rectangle(140, 480, 420, 200))
                    .icon(createLargeIcon()));

            desktop.openWindow(new WindowConfig("flush", "Margem zero", new JScrollPane(new JTextArea(
                            "titleBarInsets(0, 0, 0, 0)")))
                            .bounds(new Rectangle(600, 520, 380, 160))
                            .icon(folderIcon))
                    .style(style -> style.titleBarInsets(0, 0, 0, 0));
        });
    }

    private static Icon createLargeIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(0xE8B339));
        graphics.fillRoundRect(2, 12, 60, 44, 12, 12);
        graphics.setColor(new Color(0x5A4508));
        graphics.setStroke(new BasicStroke(4f));
        graphics.drawRoundRect(2, 12, 60, 44, 12, 12);
        graphics.dispose();
        return new ImageIcon(image);
    }
}
