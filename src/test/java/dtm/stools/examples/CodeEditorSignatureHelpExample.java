package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.signature.ParameterInformation;
import dtm.stools.component.panels.editor.code.signature.SignatureHelp;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpContext;
import dtm.stools.component.panels.editor.code.signature.SignatureHelpProvider;
import dtm.stools.component.panels.editor.code.signature.SignatureInformation;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;

public class CodeEditorSignatureHelpExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorSignatureHelpExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor signature help example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 640);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        CodeEditor editor = new CodeEditor("""
                public class SignatureDemo {
                    void run() {
                        // Digite logo apos um dos nomes abaixo seguido de '(':
                        //   max(
                        //   substring(
                        //   format(
                        int m = max();
                        String s = "hello".substring();
                    }
                }
                """);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));

        Map<String, SignatureHelp> signatures = Map.of(
                "max", new SignatureHelp(List.of(
                        new SignatureInformation(
                                "max(int a, int b)",
                                "Retorna o maior entre dois inteiros.",
                                List.of(
                                        new ParameterInformation("int a", "primeiro valor"),
                                        new ParameterInformation("int b", "segundo valor")
                                )),
                        new SignatureInformation(
                                "max(double a, double b)",
                                "Retorna o maior entre dois doubles.",
                                List.of(
                                        new ParameterInformation("double a"),
                                        new ParameterInformation("double b")
                                ))
                ), 0),
                "substring", new SignatureHelp(List.of(
                        new SignatureInformation(
                                "substring(int beginIndex)",
                                "Subsequencia a partir de beginIndex ate o fim.",
                                List.of(new ParameterInformation("int beginIndex", "indice inicial (inclusivo)"))),
                        new SignatureInformation(
                                "substring(int beginIndex, int endIndex)",
                                "Subsequencia entre beginIndex (inclusivo) e endIndex (exclusivo).",
                                List.of(
                                        new ParameterInformation("int beginIndex", "indice inicial (inclusivo)"),
                                        new ParameterInformation("int endIndex", "indice final (exclusivo)")
                                ))
                ), 0),
                "format", new SignatureHelp(List.of(
                        new SignatureInformation(
                                "format(String fmt, Object... args)",
                                "Formata uma string usando o template e os argumentos informados.",
                                List.of(
                                        new ParameterInformation("String fmt", "template de formatacao"),
                                        new ParameterInformation("Object... args", "argumentos do template")
                                ))
                ), 0)
        );

        editor.addProvider((SignatureHelpProvider) context -> {
            String name = callNameBeforeCaret(context);
            SignatureHelp base = signatures.get(name);
            if (base == null) return null;
            int param = activeParameterIndex(context);
            return new SignatureHelp(base.signatures(), base.activeSignature(), param);
        });

        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static int activeParameterIndex(SignatureHelpContext context) {
        String text = context.textBeforeCaret();
        int depth = 0;
        int commas = 0;
        boolean insideCall = false;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') {
                if (depth == 0) {
                    insideCall = true;
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                commas++;
            }
        }
        return insideCall ? commas : 0;
    }

    private static String callNameBeforeCaret(SignatureHelpContext context) {
        String text = context.textBeforeCaret();
        int depth = 0;
        int openParen = -1;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') {
                if (depth == 0) {
                    openParen = i;
                    break;
                }
                depth--;
            }
        }
        if (openParen <= 0) return "";
        int end = openParen;
        int start = end;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        return text.substring(start, end);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
