package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

final class AutoCompletePopupExampleSupport {

    private static final String INITIAL_TEXT = """
            public class Demo {
                void run() {
                    System.
                }
            }
            """;

    private static final List<AutoCompleteItem> SUGGESTIONS = List.of(
            item("out", "out", "PrintStream", "Saída padrão do processo.", AutoCompleteItem.Kind.FIELD),
            item("err", "err", "PrintStream", "Fluxo de erro padrão do processo.", AutoCompleteItem.Kind.FIELD),
            item("println", "println()", "void", "Imprime o valor e adiciona uma nova linha.", AutoCompleteItem.Kind.METHOD),
            item("printf", "printf()", "PrintStream", "Imprime texto usando uma string de formatação.", AutoCompleteItem.Kind.METHOD),
            item("currentTimeMillis", "currentTimeMillis()", "long", "Retorna o horário atual em milissegundos.", AutoCompleteItem.Kind.METHOD),
            item("nanoTime", "nanoTime()", "long", "Retorna uma fonte de tempo de alta resolução.", AutoCompleteItem.Kind.METHOD),
            item("String", "String", "java.lang", "Classe que representa uma sequência imutável de caracteres.", AutoCompleteItem.Kind.CLASS),
            item("Math", "Math", "java.lang", "Funções matemáticas e constantes comuns.", AutoCompleteItem.Kind.CLASS),
            item("List", "List", "java.util", "Coleção ordenada de elementos.", AutoCompleteItem.Kind.INTERFACE),
            item("Map", "Map", "java.util", "Associa chaves a valores.", AutoCompleteItem.Kind.INTERFACE),
            item("MAX_VALUE", "MAX_VALUE", "int", "Maior valor representável por um inteiro.", AutoCompleteItem.Kind.CONSTANT),
            item("Override", "@Override", "annotation", "Indica que o método sobrescreve uma declaração herdada.", AutoCompleteItem.Kind.KEYWORD),
            item("java.util", "java.util", "module", "Pacote de coleções e utilitários da plataforma.", AutoCompleteItem.Kind.MODULE),
            item("demo.properties", "demo.properties", "file", "Arquivo de configuração do exemplo.", AutoCompleteItem.Kind.FILE),
            item("BLUE", "Color.BLUE", "Color", "Constante azul da classe Color.", AutoCompleteItem.Kind.ENUM_MEMBER),
            AutoCompleteItem.snippet("for", "for (int ${1:i} = 0; ${1:i} < ${2:size}; ${1:i}++) {\n    ${0}\n}",
                    "Cria um laço for indexado.")
    );

    private AutoCompletePopupExampleSupport() {
    }

    static void launch(String title, AutoCompletePopupFactory popupFactory) {
        FlatDarkLaf.setup();

        CodeEditor editor = new CodeEditor(INITIAL_TEXT);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setAutoCompletePopupFactory(popupFactory);
        editor.addProvider((AutoCompleteProvider) context -> filterSuggestions(context.prefix()));
        editor.getTextArea().setCaretPosition(2, 15);

        JLabel instructions = new JLabel("Pressione Ctrl+Espaço ou use o botão para abrir as sugestões.");
        JButton trigger = new JButton("Abrir sugestões");
        trigger.addActionListener(event -> {
            editor.getTextArea().requestFocusInWindow();
            editor.triggerAutoComplete();
        });

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        toolbar.add(instructions, BorderLayout.CENTER);
        toolbar.add(trigger, BorderLayout.EAST);

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        SwingUtilities.invokeLater(editor.getTextArea()::requestFocusInWindow);
    }

    private static List<AutoCompleteItem> filterSuggestions(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        if (normalizedPrefix.isEmpty()) return SUGGESTIONS;
        return SUGGESTIONS.stream()
                .filter(item -> item.label().toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }

    private static AutoCompleteItem item(String insertText,
                                         String label,
                                         String detail,
                                         String description,
                                         AutoCompleteItem.Kind kind) {
        return new AutoCompleteItem(insertText, label, detail, description, null, kind);
    }
}
