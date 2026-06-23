package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.menu.bar.MenuBar;
import dtm.stools.component.window.TitleMenuBar;

import javax.swing.*;
import java.awt.*;

public class TitleMenuBarExample {

    private static JFrame frame;
    private static JTextArea logArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            TitleMenuBar.configureLookAndFeelDefaults(new Color(0x20262B), new Color(0xDFE1E5));
            createAndShow();
        });
    }

    private static void createAndShow() {
        frame = new JFrame("TitleMenuBar - drag e double-click");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 600);
        frame.setLocationRelativeTo(null);

        MenuBar menuBar = createMenuBar();

        TitleMenuBar title = TitleMenuBar.install(frame, menuBar)
                .titleBarHeight(40)
                .gradient(new Color(0x20262B), new Color(0x334B3B), 0)
                .foreground(new Color(0xDFE1E5));

        JLabel centerLabel = new JLabel("Centro (espaco vazio em volta arrasta)");
        centerLabel.setForeground(new Color(0xDFE1E5));
        title.center(centerLabel);

        JButton trailingButton = new JButton("Acao");
        trailingButton.addActionListener(event -> log("Botao trailing clicado"));
        title.addTrailing(trailingButton);

        frame.setContentPane(createContent());
        frame.setVisible(true);
    }

    private static MenuBar createMenuBar() {
        MenuBar bar = new MenuBar();

        MenuBar.Menu file = bar.addMenu("file", "Arquivo");
        file.addItem("new", "Novo", item -> item.addActionListener(event -> log("Arquivo > Novo")));
        file.addItem("open", "Abrir", item -> item.addActionListener(event -> log("Arquivo > Abrir")));
        file.addSeparatorLine();
        file.addItem("exit", "Sair", item -> item.addActionListener(event -> frame.dispose()));

        MenuBar.Menu edit = bar.addMenu("edit", "Editar");
        edit.addItem("copy", "Copiar", item -> item.addActionListener(event -> log("Editar > Copiar")));
        edit.addItem("paste", "Colar", item -> item.addActionListener(event -> log("Editar > Colar")));

        MenuBar.Menu help = bar.addMenu("help", "Ajuda");
        help.addItem("about", "Sobre", item -> item.addActionListener(event ->
                JOptionPane.showMessageDialog(frame, "TitleMenuBar example")));

        return bar;
    }

    private static JComponent createContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setText("""
                Teste manual antes de instalar em producao:

                1. Clique em qualquer menu (Arquivo, Editar, Ajuda)
                   -> os itens devem abrir e disparar eventos no log abaixo.

                2. Clique no botao "Acao" na direita
                   -> deve logar "Botao trailing clicado".

                3. Arraste a janela segurando um ESPACO VAZIO da barra
                   (entre os menus, em volta do centro, ao lado do botao trailing)
                   -> a janela deve se mover.

                4. Double-click em um espaco vazio da barra
                   -> a janela deve alternar entre maximizada e normal.

                5. Maximize a janela e arraste a barra
                   -> deve restaurar e seguir o cursor.
                """);

        root.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return root;
    }

    private static void log(String message) {
        if (logArea != null) {
            logArea.append("\n> " + message);
        }
    }
}
