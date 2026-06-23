package dtm.stools.examples;

import dtm.stools.component.menu.popup.ActionPopupMenu;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class ActionPopupMenuExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ActionPopupMenuExample::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("ActionPopupMenu Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 450);
        frame.setLocationRelativeTo(null);

        JTextArea textArea = new JTextArea();
        textArea.setText("""
                Clique com o botão direito aqui para abrir o ActionPopupMenu.

                Esse exemplo mostra:
                - item normal
                - item com ícone
                - item desabilitado
                - submenu
                - checkbox
                - radio button
                - separador
                - tamanho preferido do popup
                - style no root
                - submenu com style próprio
                - submenu herdando style do root
                """);

        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        textArea.setComponentPopupMenu(createPopupMenu(frame, textArea));

        frame.setContentPane(new JScrollPane(textArea));
        frame.setVisible(true);
    }

    private static JPopupMenu createPopupMenu(JFrame frame, JTextArea textArea) {
        Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
        Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
        Icon infoIcon = UIManager.getIcon("OptionPane.informationIcon");
        Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");

        ButtonGroup themeGroup = new ButtonGroup();

        Path examplePath = Path.of("C:/projetos/exemplo/Main.java");

        return ActionPopupMenu.create()
                .popupSize(260, 260)

                .background(new Color(35, 35, 38))
                .foreground(new Color(230, 230, 230))
                .selectionBackground(new Color(70, 70, 76))
                .selectionForeground(Color.WHITE)
                .enableRootStyleForChildren()

                .item("Abrir", fileIcon, e ->
                        showMessage(frame, "Abrir", "Abrindo: " + examplePath)
                )

                .item("Abrir pasta", folderIcon, e ->
                        showMessage(frame, "Abrir pasta", "Abrindo pasta do arquivo.")
                )

                .item("Salvar", e -> {
                    textArea.append("\nArquivo salvo.");
                    showMessage(frame, "Salvar", "Arquivo salvo com sucesso.");
                })

                .item("Item desabilitado", false, e ->
                        showMessage(frame, "Desabilitado", "Isso não deve executar.")
                )

                .separator()

                .submenu("Criar", folderIcon, menu -> menu
                        .background(new Color(25, 45, 70))
                        .foreground(new Color(235, 245, 255))
                        .selectionBackground(new Color(45, 95, 140))
                        .selectionForeground(Color.WHITE)

                        .item("Novo arquivo", fileIcon, e ->
                                showMessage(frame, "Novo arquivo", "Criando novo arquivo.")
                        )
                        .item("Nova pasta", folderIcon, e ->
                                showMessage(frame, "Nova pasta", "Criando nova pasta.")
                        )
                        .separator()
                        .item("Novo projeto", e ->
                                showMessage(frame, "Novo projeto", "Criando novo projeto.")
                        )

                        .submenu("Templates", templateMenu -> templateMenu
                                .item("Java", e ->
                                        showMessage(frame, "Template", "Template Java.")
                                )
                                .item("C#", e ->
                                        showMessage(frame, "Template", "Template C#.")
                                )
                        )
                )

                .submenu("Mais opções", infoIcon, menu -> menu
                        .checkItem("Habilitar logs", true, e ->
                                textArea.append("\nLogs alterados.")
                        )
                        .separator()
                        .radioItem("Tema claro", themeGroup, true, e ->
                                textArea.append("\nTema claro selecionado.")
                        )
                        .radioItem("Tema escuro", themeGroup, false, e ->
                                textArea.append("\nTema escuro selecionado.")
                        )
                )

                .submenu("Sem style próprio", menu -> menu
                        .item("Esse submenu herda o root", e ->
                                showMessage(frame, "Style", "Esse submenu usa o style do root.")
                        )
                        .submenu("Filho também sem style", child -> child
                                .item("Também herda o root", e ->
                                        showMessage(frame, "Style", "Esse também usa o style do root.")
                                )
                        )
                )

                .separator()

                .item("Copiar path", e -> {
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(
                                    new java.awt.datatransfer.StringSelection(examplePath.toString()),
                                    null
                            );

                    textArea.append("\nPath copiado: " + examplePath);
                })

                .item("Informações", warningIcon, e ->
                        showMessage(frame, "Informações", "Exemplo de ActionPopupMenu funcionando.")
                );
    }

    private static void showMessage(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}