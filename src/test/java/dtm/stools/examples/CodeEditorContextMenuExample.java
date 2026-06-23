package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.provider.ContextMenuProvider;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;

public class CodeEditorContextMenuExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorContextMenuExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor ContextMenuProvider example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        frame.add(buildDefaultEditor(), BorderLayout.WEST);
        frame.add(buildCustomEditor(), BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private static CodeEditor buildDefaultEditor() {
        CodeEditor editor = new CodeEditor("""
                // Menu padrão (sem provider).
                // Botão direito mostra Desfazer / Refazer / Recortar /
                // Copiar / Colar / Selecionar tudo.
                """);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        return editor;
    }

    private static CodeEditor buildCustomEditor() {
        CodeEditor editor = new CodeEditor("""
                // Menu customizado via ContextMenuProvider.
                // Selecione um trecho e clique com o botão direito.
                """);
        CodeEditorTextArea area = editor.getTextArea();
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        editor.addProvider((ContextMenuProvider) e -> {
            JPopupMenu menu = new JPopupMenu();

            JMenuItem mostrar = new JMenuItem("Mostrar seleção");
            mostrar.addActionListener(ev -> {
                String sel = area.getSelectedTextOrEmpty();
                JOptionPane.showMessageDialog(area,
                        sel.isEmpty() ? "(nada selecionado)" : sel);
            });
            menu.add(mostrar);

            menu.addSeparator();

            JPopupMenu defaults = area.createDefaultContextMenu();
            while (defaults.getComponentCount() > 0) {
                menu.add(defaults.getComponent(0));
            }
            return menu;
        });

        return editor;
    }
}
