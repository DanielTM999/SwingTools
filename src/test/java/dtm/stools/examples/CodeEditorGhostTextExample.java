package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.ghost.GhostTextContext;
import dtm.stools.component.panels.editor.code.ghost.GhostTextProvider;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;


public class CodeEditorGhostTextExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorGhostTextExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor ghost text example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 640);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        CodeEditor editor = new CodeEditor("""
                public class GhostDemo {

                    // Digite no fim de uma linha um dos gatilhos: sout, for, if, main
                    // Tab aceita a sugestao fantasma. Esc / digitar / clicar descartam.
                    void run() {
                        sout
                    }

                    // estas linhas existem so para voce ver o "empurrao" do bloco multilinha
                    int a = 1;
                    int b = 2;
                    int c = 3;
                }
                """);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));

        // Gatilho -> texto sugerido. A sugestao comeca com espaco para continuar logo apos a
        // palavra digitada. O '\n' torna o ghost multilinha; a indentacao (8/12 espacos) casa
        // com o nivel do corpo do metodo no conteudo de exemplo.
        Map<String, String> snippets = new LinkedHashMap<>();
        snippets.put("sout", "System.out.println();");
        snippets.put("for", " (int i = 0; i < length; i++) {\n            \n        }");
        snippets.put("if", " (condition) {\n            \n        }");
        snippets.put("main", " public static void main(String[] args) {\n            \n        }");

        editor.addProvider((GhostTextProvider) context -> {
            String trigger = wordBeforeCaret(context);
            return trigger.isEmpty() ? null : snippets.get(trigger);
        });

        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /** Extrai o identificador imediatamente antes do cursor na linha atual. */
    private static String wordBeforeCaret(GhostTextContext context) {
        String line = context.currentLine();
        int col = Math.min(context.caretCol(), line.length());
        int start = col;
        while (start > 0 && isWordChar(line.charAt(start - 1))) {
            start--;
        }
        return line.substring(start, col);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
