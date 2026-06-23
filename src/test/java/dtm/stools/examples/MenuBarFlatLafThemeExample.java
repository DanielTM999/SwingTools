package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.menu.bar.MenuBar;
import dtm.stools.component.menu.bar.config.MenuBarStyle;

import javax.swing.*;
import java.awt.*;

public class MenuBarFlatLafThemeExample {

    private static JFrame frame;
    private static MenuBar menuBar;
    private static JTextArea logArea;
    private static JLabel menuBarForegroundLabel;
    private static JLabel menuForegroundLabel;
    private static JLabel labelForegroundLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        frame = new JFrame("MenuBar + FlatLaf setup dark/light");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(860, 540);
        frame.setLocationRelativeTo(null);

        menuBar = createMenuBar();
        frame.setJMenuBar(menuBar);
        frame.setContentPane(createContent());

        refreshUiManagerLabels();
        frame.setVisible(true);
    }

    private static MenuBar createMenuBar() {
        MenuBar bar = new MenuBar(MenuBarStyle.dark());

        MenuBar.Menu file = bar.addMenu("file", "Arquivo");
        file.addItem("new", "Novo");
        file.addItem("open", "Abrir");
        file.addSeparatorLine();
        file.addItem("exit", "Sair", item -> item.addActionListener(event -> frame.dispose()));

        MenuBar.Menu theme = bar.addMenu("theme", "Tema");
        theme.addItem("dark", "FlatDarkLaf.setup()", item ->
                item.addActionListener(event -> installDarkTheme()));
        theme.addItem("light", "FlatLightLaf.setup()", item ->
                item.addActionListener(event -> installLightTheme()));

        MenuBar.Menu help = bar.addMenu("help", "Ajuda");
        help.addItem("about", "Sobre", item -> item.addActionListener(event ->
                JOptionPane.showMessageDialog(frame, "Exemplo do MenuBar usando foreground do UIManager.")));

        return bar;
    }

    private static JComponent createContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton darkButton = new JButton("FlatDarkLaf.setup()");
        darkButton.addActionListener(event -> installDarkTheme());
        JButton lightButton = new JButton("FlatLightLaf.setup()");
        lightButton.addActionListener(event -> installLightTheme());
        controls.add(darkButton);
        controls.add(lightButton);

        JPanel values = new JPanel(new GridLayout(0, 1, 0, 4));
        values.setBorder(BorderFactory.createTitledBorder("UIManager foreground"));
        menuBarForegroundLabel = new JLabel();
        menuForegroundLabel = new JLabel();
        labelForegroundLabel = new JLabel();
        values.add(menuBarForegroundLabel);
        values.add(menuForegroundLabel);
        values.add(labelForegroundLabel);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        logArea.setText("""
                Use os botoes ou o menu Tema para alternar entre FlatDarkLaf.setup() e FlatLightLaf.setup().

                O MenuBarStyle.dark() e MenuBarStyle.light() nao definem mais foreground fixo.
                Assim, o texto do menu deve acompanhar o foreground resolvido pelo UIManager.
                """);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(controls, BorderLayout.NORTH);
        top.add(values, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return root;
    }

    private static void installDarkTheme() {
        FlatDarkLaf.setup();
        applyTheme("FlatDarkLaf.setup()", MenuBarStyle.dark());
    }

    private static void installLightTheme() {
        FlatLightLaf.setup();
        applyTheme("FlatLightLaf.setup()", MenuBarStyle.light());
    }

    private static void applyTheme(String themeName, MenuBarStyle style) {
        menuBar.setStyle(style);
        SwingUtilities.updateComponentTreeUI(frame);
        refreshUiManagerLabels();
        logArea.append("\nTema aplicado: " + themeName);
    }

    private static void refreshUiManagerLabels() {
        menuBarForegroundLabel.setText("MenuBar.foreground = " + colorToText(UIManager.getColor("MenuBar.foreground")));
        menuForegroundLabel.setText("Menu.foreground = " + colorToText(UIManager.getColor("Menu.foreground")));
        labelForegroundLabel.setText("Label.foreground = " + colorToText(UIManager.getColor("Label.foreground")));
    }

    private static String colorToText(Color color) {
        if (color == null) {
            return "null";
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
