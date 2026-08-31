package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.menu.bar.CollapsibleMenuBar;
import dtm.stools.component.panels.window.WindowConfig;
import dtm.stools.component.panels.window.WindowDesktopPanel;
import dtm.stools.component.panels.window.WindowMenuBarPlacement;
import dtm.stools.component.panels.window.WindowPosition;

import javax.swing.*;
import java.awt.*;

/** Demonstrates an actual CollapsibleMenuBar inside a centered WindowPanel. */
public final class WindowConfigCollapsibleMenuBarExample {
    private WindowConfigCollapsibleMenuBarExample() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();

            WindowDesktopPanel desktop = new WindowDesktopPanel();
            JFrame frame = new JFrame("WindowConfig + CollapsibleMenuBar");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(desktop);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            JTextArea editor = new JTextArea();
            editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            editor.setText("""
                    Esta WindowPanel abriu no centro do WindowDesktopPanel.

                    O CollapsibleMenuBar ocupa toda a area livre da barra de titulo.
                    O icone continua visivel e o texto do titulo cede lugar ao menu.
                    Use o botao da barra para recolher ou exibir os menus.
                    """);

            CollapsibleMenuBar menuBar = createMenuBar(desktop, editor);
            JButton centerAction = createCenterAction(editor);
            desktop.openWindow(new WindowConfig(
                            "centered-editor", "Editor centralizado", new JScrollPane(editor))
                    .bounds(new Rectangle(0, 0, 640, 420))
                    .position(WindowPosition.CENTER)
                    .icon(UIManager.getIcon("FileView.computerIcon"))
                    .menuBar(menuBar, WindowMenuBarPlacement.TITLE_BAR)
                    .menuBarFollowTitleBarColor(true)
                    .titleBarCenter(centerAction));
        });
    }

    private static CollapsibleMenuBar createMenuBar(WindowDesktopPanel desktop, JTextArea editor) {
        CollapsibleMenuBar bar = new CollapsibleMenuBar();
        bar.setCollapseButtonVisibleWhenExpanded(true);
        bar.setAutoCollapseAfterMenuClose(true);

        bar.setCollapsed(true);
        bar.addMenu("file", "Arquivo")
                .addItem("new", "Novo", item -> item.addActionListener(event -> editor.setText("")))
                .addItem("close", "Fechar janela", item -> item.addActionListener(
                        event -> desktop.closeWindow("centered-editor")));

        bar.addMenu("edit", "Editar")
                .addItem("select-all", "Selecionar tudo",
                        item -> item.addActionListener(event -> editor.selectAll()));

        bar.addMenu("view", "Exibir")
                .addItem("toggle-menu", "Recolher menu",
                        item -> item.addActionListener(event -> bar.setCollapsed(true)));

        bar.addMenu("help", "Ajuda")
                .addItem("about", "Sobre", item -> item.addActionListener(event ->
                        JOptionPane.showMessageDialog(desktop,
                                "CollapsibleMenuBar dentro de uma WindowPanel centralizada.")));

        return bar;
    }

    private static JButton createCenterAction(JTextArea editor) {
        JButton button = new JButton("\u25B6  Arquivo atual");
        button.setFocusable(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        button.addActionListener(event -> editor.append("\nExecutando arquivo atual..."));
        return button;
    }
}
