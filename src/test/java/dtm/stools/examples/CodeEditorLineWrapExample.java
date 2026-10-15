package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class CodeEditorLineWrapExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorLineWrapExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor line wrap");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(760, 560);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = new CodeEditor(SAMPLE_CODE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setFocusBorderEnabled(false);
        editor.setLineWrapEnabled(true);

        DiagnosticsProvider provider = ctx -> {
            TextBuffer buffer = ctx.buffer();
            List<Diagnostic> out = new ArrayList<>();
            for (int line = 0; line < buffer.lineCount(); line++) {
                String text = buffer.lineAt(line);
                int col = text.indexOf("deprecatedCall");
                if (col >= 0) {
                    out.add(new Diagnostic(line, col, line, col + "deprecatedCall".length(),
                            DiagnosticSeverity.WARNING, "chamada obsoleta"));
                }
            }
            return out;
        };
        editor.addProvider(provider);
        editor.setDiagnosticsAutoRunEnabled(true);

        JLabel status = new JLabel();
        editor.addStateListener(state -> status.setText(
                "linha " + (state.caretLine() + 1) + ", coluna " + (state.caretCol() + 1)));

        frame.add(buildToolbar(editor, status), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);

        editor.refreshDiagnostics();
    }

    private static JPanel buildToolbar(CodeEditor editor, JLabel status) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));

        JCheckBox wrap = new JCheckBox("quebra de linha automática", editor.isLineWrapEnabled());
        wrap.addActionListener(e -> editor.setLineWrapEnabled(wrap.isSelected()));
        toolbar.add(wrap);

        JCheckBox folding = new JCheckBox("folding", editor.isFoldingEnabled());
        folding.addActionListener(e -> editor.setFoldingEnabled(folding.isSelected()));
        toolbar.add(folding);

        toolbar.add(new JLabel("redimensione a janela para ver as linhas se reorganizarem"));
        toolbar.add(status);
        return toolbar;
    }

    private static final String SAMPLE_CODE = """
            public class WrapDemo {

                private static final String MESSAGE = "esta é uma string bem longa que serve para mostrar a quebra de linha automática acontecendo apenas na visualização, sem inserir nenhum caractere de nova linha no documento";

                public static void main(String[] args) {
                    System.out.println(MESSAGE + " — e ainda concatenamos mais um pedaço de texto para garantir que esta linha também ultrapasse a largura visível do editor");
                    int total = sum(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30);
                    legacy.deprecatedCall(total, "argumento", "outro argumento", "mais um argumento só para alongar a chamada");
                }

                static int sum(int... values) {
                    int acc = 0;
                    for (int value : values) {
                        acc += value;
                    }
                    return acc;
                }

                // Um comentário muito comprido também quebra de forma visual: o buffer continua com uma única linha, então números de linha, undo, busca e diagnósticos seguem funcionando normalmente.
            }
            """;
}
