package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.defaults.AutoCompletePopupDefaults;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

public class RuntimeAutoCompletePopupDefaultsExample {

    private static final String INITIAL_TEXT = """
            public class Demo {
                void run() {
                    System.
                }
            }
            """;

    private static final List<AutoCompleteItem> SUGGESTIONS = List.of(
            item("out", "out", "PrintStream", AutoCompleteItem.Kind.FIELD),
            item("err", "err", "PrintStream", AutoCompleteItem.Kind.FIELD),
            item("println", "println()", "void", AutoCompleteItem.Kind.METHOD),
            item("printf", "printf()", "PrintStream", AutoCompleteItem.Kind.METHOD),
            item("String", "String", "java.lang", AutoCompleteItem.Kind.CLASS),
            item("List", "List", "java.util", AutoCompleteItem.Kind.INTERFACE),
            item("MAX_VALUE", "MAX_VALUE", "int", AutoCompleteItem.Kind.CONSTANT),
            AutoCompleteItem.snippet("for", "for (int ${1:i} = 0; ${1:i} < ${2:size}; ${1:i}++) {\n    ${0}\n}")
    );

    private static final Preset[] PRESETS = {
            new Preset("IntelliJ IDEA", AutoCompletePopupDefaults.intellij()),
            new Preset("Visual Studio Code", AutoCompletePopupDefaults.visualStudioCode()),
            new Preset("Eclipse", AutoCompletePopupDefaults.eclipse()),
            new Preset("NetBeans", AutoCompletePopupDefaults.netBeans())
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RuntimeAutoCompletePopupDefaultsExample::createAndShow);
    }

    private static void createAndShow() {
        FlatDarkLaf.setup();

        CodeEditor editor = new CodeEditor(INITIAL_TEXT);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.addProvider((AutoCompleteProvider) context -> filterSuggestions(context.prefix()));
        editor.getTextArea().setCaretPosition(2, 15);

        int[] currentIndex = {0};
        JLabel currentPreset = new JLabel();
        JButton switchPreset = new JButton();
        JButton openPopup = new JButton("Abrir autocomplete");

        Runnable updatePreset = () -> {
            Preset selected = PRESETS[currentIndex[0]];
            Preset next = PRESETS[(currentIndex[0] + 1) % PRESETS.length];
            editor.setAutoCompletePopupFactory(selected.factory());
            currentPreset.setText("Default atual: " + selected.name());
            switchPreset.setText("Trocar para " + next.name());
        };

        switchPreset.addActionListener(event -> {
            currentIndex[0] = (currentIndex[0] + 1) % PRESETS.length;
            updatePreset.run();
            showPopup(editor);
        });
        openPopup.addActionListener(event -> showPopup(editor));
        updatePreset.run();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(openPopup);
        actions.add(switchPreset);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        toolbar.add(currentPreset, BorderLayout.CENTER);
        toolbar.add(actions, BorderLayout.EAST);

        JFrame frame = new JFrame("AutoCompletePopup - troca em runtime");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setSize(960, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> showPopup(editor));
    }

    private static void showPopup(CodeEditor editor) {
        editor.getTextArea().requestFocusInWindow();
        SwingUtilities.invokeLater(editor::triggerAutoComplete);
    }

    private static List<AutoCompleteItem> filterSuggestions(String prefix) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return SUGGESTIONS;
        return SUGGESTIONS.stream()
                .filter(item -> item.label().toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private static AutoCompleteItem item(String insertText,
                                         String label,
                                         String detail,
                                         AutoCompleteItem.Kind kind) {
        return new AutoCompleteItem(insertText, label, detail, null, null, kind);
    }

    private record Preset(String name, AutoCompletePopupFactory factory) {
    }
}
