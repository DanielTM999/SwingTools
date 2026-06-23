package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.CodeEditorInspectionWidget;
import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;


public class CodeEditorErrorStripeExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorErrorStripeExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor error stripe");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 720);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = new CodeEditor(SAMPLE_CODE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setFocusBorderEnabled(false);


        DiagnosticsProvider provider = ctx -> {
            TextBuffer buffer = ctx.buffer();
            List<Diagnostic> out = new ArrayList<>();
            for (int line = 0; line < buffer.lineCount(); line++) {
                String text = buffer.lineAt(line);
                addIfPresent(out, line, text, "BUG", DiagnosticSeverity.ERROR, "possível bug aqui");
                addIfPresent(out, line, text, "TODO", DiagnosticSeverity.WARNING, "pendência não resolvida");
                addIfPresent(out, line, text, "note", DiagnosticSeverity.INFO, "anotação informativa");
            }
            return out;
        };
        editor.addProvider(provider);
        editor.setDiagnosticsAutoRunEnabled(true);
        editor.setDiagnosticsDebounceMs(150);

        JLabel status = new JLabel("clique na barra à direita ou no widget de inspeção (topo)");
        editor.addErrorStripeClickListener(event ->
                status.setText("marcador clicado: " + event.diagnostic().severity()
                        + " na linha " + (event.line() + 1)
                        + " — " + event.diagnostic().message()));

        editor.setInspectionWidgetNavigateOnClick(false);
        editor.addInspectionWidgetClickListener(event -> {
            List<Diagnostic> diags = event.diagnostics();
            String first = diags.isEmpty() ? "" : " — 1º: " + diags.get(0).message();
            status.setText("widget clicado: " + diags.size() + " diagnóstico(s)" + first);
        });

        frame.add(buildToolbar(editor, status), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);

        editor.refreshDiagnostics();
    }

    private static void addIfPresent(List<Diagnostic> out, int line, String text,
                                     String token, DiagnosticSeverity severity, String message) {
        int col = text.indexOf(token);
        if (col >= 0) {
            out.add(new Diagnostic(line, col, line, col + token.length(), severity, message));
        }
    }

    private static JPanel buildToolbar(CodeEditor editor, JLabel status) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));

        JCheckBox enabled = new JCheckBox("error stripe", editor.isErrorStripeEnabled());
        enabled.addActionListener(e -> editor.setErrorStripeEnabled(enabled.isSelected()));
        toolbar.add(enabled);

        JCheckBox navigate = new JCheckBox("navegar ao clicar", editor.isErrorStripeNavigateOnClick());
        navigate.addActionListener(e -> editor.setErrorStripeNavigateOnClick(navigate.isSelected()));
        toolbar.add(navigate);

        JCheckBox widget = new JCheckBox("inspection widget", editor.isInspectionWidgetEnabled());
        widget.addActionListener(e -> editor.setInspectionWidgetEnabled(widget.isSelected()));
        toolbar.add(widget);

        // demonstra a factory/herança: troca só a aparência mantendo o comportamento
        JButton custom = new JButton("widget custom");
        custom.addActionListener(e -> editor.setInspectionWidgetFactory(CustomInspectionWidget::new));
        toolbar.add(custom);

        // default HIDE: sem diagnósticos o widget some. Marque para mostrar o check verde.
        JCheckBox showOk = new JCheckBox("check verde quando limpo",
                editor.getInspectionWidgetCleanMode() == CodeEditorInspectionWidget.CleanMode.SHOW_OK);
        showOk.addActionListener(e -> editor.setInspectionWidgetCleanMode(
                showOk.isSelected()
                        ? CodeEditorInspectionWidget.CleanMode.SHOW_OK
                        : CodeEditorInspectionWidget.CleanMode.HIDE));
        toolbar.add(showOk);

        toolbar.add(status);
        return toolbar;
    }

    private static final String SAMPLE_CODE = """
            public class Demo {
                public static void main(String[] args) {
                    // TODO: validar argumentos de entrada
                    int total = compute(10);
                    System.out.println("total = " + total);
                }

                static int compute(int n) {
                    int acc = 0;
                    for (int i = 1; i <= n; i++) {
                        acc += i; // BUG: deveria multiplicar por 31
                    }
                    // note: revisar overflow para valores grandes
                    return acc;
                }

                static void unused() {
                    // TODO: remover método morto
                    int x = 0;
                    x = x + 1; // BUG: efeito colateral inútil
                }

                static String describe() {
                    // note: usado apenas em logs
                    return "demo";
                }
            }
            """;

    /**
     * Subclasse que muda apenas a aparência (fundo). Toda a lógica de contagem,
     * navegação e evento de clique vem da classe base — e os ouvintes já
     * registrados são preservados ao trocar via setInspectionWidgetFactory.
     */
    private static class CustomInspectionWidget extends CodeEditorInspectionWidget {
        CustomInspectionWidget(CodeEditorTextArea textArea) {
            super(textArea);
        }

        @Override
        protected void paintWidgetBackground(Graphics2D g2) {
            g2.setColor(new Color(40, 44, 60, 230));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(new Color(120, 140, 220, 160));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        }
    }
}
