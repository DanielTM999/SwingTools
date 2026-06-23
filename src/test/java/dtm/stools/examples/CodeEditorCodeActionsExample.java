package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.api.CodeAction;
import dtm.stools.component.panels.editor.code.api.Command;
import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.provider.CodeActionContext;
import dtm.stools.component.panels.editor.code.provider.CodeActionProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public class CodeEditorCodeActionsExample {

    private static final String SAMPLE_CODE = """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.Collections;

            public class CodeActionsDemo {
                public void run() {
                    System.out.println("debug");
                    int unused = 42;
                    String label = "sample";
                }
            }
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorCodeActionsExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor CodeActionProvider example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 640);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JLabel status = new JLabel("Alt+Enter abre Code Actions no caret atual.");
        CodeEditor editor = buildEditor(status);

        frame.add(buildToolbar(editor, status), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.add(status, BorderLayout.SOUTH);
        frame.setVisible(true);

        editor.refreshDiagnostics();
        editor.setCaretPosition(6, 12);
    }

    private static CodeEditor buildEditor(JLabel status) {
        CodeEditor editor = new CodeEditor(SAMPLE_CODE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setCodeActionsKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK));

        editor.addProvider(buildDiagnosticsProvider());
        editor.addProvider(buildCodeActionProvider());
        editor.setCommandHandler(command -> JOptionPane.showMessageDialog(
                editor,
                command.title() + "\n\nid: " + command.id()
                        + "\nargs: " + command.arguments(),
                "CommandHandler",
                JOptionPane.INFORMATION_MESSAGE));

        editor.getTextArea().addDocumentEditListener(new dtm.stools.component.panels.editor.code.listeners.DocumentEditListener() {
            @Override
            public void onTextChanged() {
                status.setText("Documento alterado. Diagnostics e Code Actions serao recalculados.");
            }
        });
        return editor;
    }

    private static JPanel buildToolbar(CodeEditor editor, JLabel status) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton actions = new JButton("Code Actions");
        actions.addActionListener(e -> {
            status.setText("Abrindo popup de Code Actions para o caret/selection atual.");
            editor.triggerCodeActions();
        });
        toolbar.add(actions);

        JButton applyConsoleFix = new JButton("Aplicar quick fix");
        applyConsoleFix.addActionListener(e -> {
            Range range = rangeOf(editor.getText(), "System.out.println");
            if (range == null) return;
            editor.applyCodeAction(new CodeAction(
                    "Trocar println por log",
                    CodeAction.CodeActionKind.QUICK_FIX,
                    List.of(TextEdit.replace(range, "log.info")),
                    null,
                    true));
            status.setText("Quick fix aplicado programaticamente via editor.applyCodeAction(...).");
            editor.refreshDiagnostics();
        });
        toolbar.add(applyConsoleFix);

        JButton selectLabel = new JButton("Selecionar label");
        selectLabel.addActionListener(e -> {
            Range range = rangeOf(editor.getText(), "\"sample\"");
            if (range == null) return;
            editor.setSelection(range.start().line(), range.start().col(), range.end().line(), range.end().col());
            status.setText("Selecao criada. Use Code Actions para executar o refactor de uppercase.");
        });
        toolbar.add(selectLabel);

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            editor.setText(SAMPLE_CODE);
            editor.refreshDiagnostics();
            editor.setCaretPosition(6, 12);
            status.setText("Exemplo reiniciado.");
        });
        toolbar.add(reset);

        return toolbar;
    }

    private static DiagnosticsProvider buildDiagnosticsProvider() {
        return context -> {
            TextBuffer buffer = context.buffer();
            String text = buffer.getText();
            List<Diagnostic> diagnostics = new ArrayList<>();

            int println = text.indexOf("System.out.println");
            if (println >= 0) {
                diagnostics.add(Diagnostic.ofOffset(
                        buffer,
                        println,
                        println + "System.out.println".length(),
                        DiagnosticSeverity.WARNING,
                        "Use logger em vez de console output",
                        "CodeActionsExample"));
            }

            int unused = text.indexOf("unused");
            if (unused >= 0) {
                diagnostics.add(Diagnostic.ofOffset(
                        buffer,
                        unused,
                        unused + "unused".length(),
                        DiagnosticSeverity.INFO,
                        "Variavel local nao utilizada",
                        "CodeActionsExample"));
            }

            return diagnostics;
        };
    }

    private static CodeActionProvider buildCodeActionProvider() {
        return context -> {
            String text = context.buffer();
            List<CodeAction> actions = new ArrayList<>();

            addConsoleQuickFix(context, text, actions);
            addRemoveUnusedQuickFix(context, text, actions);
            addUppercaseSelectionRefactor(context, text, actions);
            addOrganizeImportsAction(text, actions);

            actions.add(CodeAction.command(
                    "Mostrar contexto recebido pelo provider",
                    new Command(
                            "example.showCodeActionContext",
                            "Contexto do CodeActionProvider",
                            List.of(context.range(), context.diagnostics().size()))));

            return actions;
        };
    }

    private static void addConsoleQuickFix(CodeActionContext context, String text, List<CodeAction> actions) {
        Range range = rangeOf(text, "System.out.println");
        if (range == null || !intersects(text, context.range(), range)) return;

        actions.add(new CodeAction(
                "Quick fix: trocar println por log",
                CodeAction.CodeActionKind.QUICK_FIX,
                List.of(TextEdit.replace(range, "log.info")),
                null,
                true));
    }

    private static void addRemoveUnusedQuickFix(CodeActionContext context, String text, List<CodeAction> actions) {
        Range variableRange = rangeOf(text, "unused");
        if (variableRange == null || !intersects(text, context.range(), variableRange)) return;

        int lineStart = lineStartOffset(text, offsetOf(text, variableRange.start()));
        int nextLine = nextLineOffset(text, lineStart);
        actions.add(CodeAction.quickFix(
                "Quick fix: remover linha da variavel unused",
                List.of(TextEdit.delete(rangeOf(text, lineStart, nextLine)))));
    }

    private static void addUppercaseSelectionRefactor(CodeActionContext context, String text, List<CodeAction> actions) {
        Range selected = context.range();
        if (selected == null || selected.isEmpty()) return;

        String selection = text.substring(offsetOf(text, selected.start()), offsetOf(text, selected.end()));
        if (selection.isBlank()) return;

        actions.add(CodeAction.refactor(
                "Refactor: transformar selecao em uppercase",
                List.of(TextEdit.replace(selected, selection.toUpperCase()))));
    }

    private static void addOrganizeImportsAction(String text, List<CodeAction> actions) {
        Range importBlock = leadingImportBlockRange(text);
        if (importBlock == null) return;

        String organized = organizeImports(text.substring(
                offsetOf(text, importBlock.start()),
                offsetOf(text, importBlock.end())));

        actions.add(new CodeAction(
                "Source action: organizar imports",
                CodeAction.CodeActionKind.SOURCE_ORGANIZE_IMPORTS,
                List.of(TextEdit.replace(importBlock, organized)),
                null,
                false));
    }

    private static Range leadingImportBlockRange(String text) {
        String[] lines = text.split("\n", -1);
        int endLine = 0;
        while (endLine < lines.length && lines[endLine].startsWith("import ")) {
            endLine++;
        }
        if (endLine == 0) return null;
        return Range.of(0, 0, endLine, 0);
    }

    private static String organizeImports(String importBlock) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String line : importBlock.split("\n")) {
            if (line.startsWith("import ")) unique.add(line);
        }
        return unique.stream()
                .sorted(Comparator.naturalOrder())
                .reduce("", (out, line) -> out + line + "\n");
    }

    private static boolean intersects(String text, Range a, Range b) {
        if (a == null || b == null) return false;
        int aStart = offsetOf(text, a.start());
        int aEnd = offsetOf(text, a.end());
        int bStart = offsetOf(text, b.start());
        int bEnd = offsetOf(text, b.end());
        return aStart <= bEnd && bStart <= aEnd;
    }

    private static Range rangeOf(String text, String needle) {
        int start = text.indexOf(needle);
        if (start < 0) return null;
        return rangeOf(text, start, start + needle.length());
    }

    private static Range rangeOf(String text, int startOffset, int endOffset) {
        return new Range(positionOf(text, startOffset), positionOf(text, endOffset));
    }

    private static Position positionOf(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int line = 0;
        int col = 0;
        for (int i = 0; i < safeOffset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return Position.of(line, col);
    }

    private static int offsetOf(String text, Position position) {
        int line = 0;
        int col = 0;
        for (int i = 0; i < text.length(); i++) {
            if (line == position.line() && col == position.col()) return i;
            if (text.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return text.length();
    }

    private static int lineStartOffset(String text, int offset) {
        int start = Math.max(0, Math.min(offset, text.length()));
        while (start > 0 && text.charAt(start - 1) != '\n') start--;
        return start;
    }

    private static int nextLineOffset(String text, int offset) {
        int next = text.indexOf('\n', Math.max(0, Math.min(offset, text.length())));
        if (next < 0) return text.length();
        return Math.min(text.length(), next + 1);
    }
}
