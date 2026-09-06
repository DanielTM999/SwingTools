package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticsProvider;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeEditorDiagnosticsFloodExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorDiagnosticsFloodExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor diagnostics flood");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = new CodeEditor(SAMPLE_CODE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setFocusBorderEnabled(false);

        Random random = new Random();
        AtomicInteger count = new AtomicInteger(80);
        AtomicInteger callCounter = new AtomicInteger();
        JLabel statusLabel = new JLabel("calls: 0  |  last batch: 0");

        DiagnosticsProvider floodProvider = ctx -> {
            TextBuffer buffer = ctx.buffer();
            int n = count.get();
            int total = buffer.length();
            List<Diagnostic> out = new ArrayList<>(n);
            if (total <= 1) {
                return out;
            }
            DiagnosticSeverity[] severities = DiagnosticSeverity.values();
            for (int i = 0; i < n; i++) {
                int start = random.nextInt(total);
                int end = Math.min(total, start + 1 + random.nextInt(8));
                DiagnosticSeverity sev = severities[random.nextInt(severities.length)];
                out.add(Diagnostic.ofOffset(buffer, start, end, sev,
                        sev.name().toLowerCase() + " #" + i + " @" + start));
            }
            int c = callCounter.incrementAndGet();
            SwingUtilities.invokeLater(() ->
                    statusLabel.setText("calls: " + c + "  |  last batch: " + n));
            return out;
        };
        editor.addProvider(floodProvider);

        editor.setDiagnosticsAutoRunEnabled(true);
        editor.setDiagnosticsDebounceMs(300);

        frame.add(buildToolbar(editor, count, statusLabel), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static JPanel buildToolbar(CodeEditor editor, AtomicInteger count, JLabel statusLabel) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));

        toolbar.add(new JLabel("debounce (ms):"));
        JSpinner debounce = new JSpinner(new SpinnerNumberModel(
                editor.getDiagnosticsDebounceMs(), 0, 5000, 50));
        debounce.addChangeListener(e ->
                editor.setDiagnosticsDebounceMs((Integer) debounce.getValue()));
        toolbar.add(debounce);

        toolbar.add(new JLabel("diagnostics por chamada:"));
        JSlider slider = new JSlider(0, 1000, count.get());
        slider.setMajorTickSpacing(200);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        JLabel countLabel = new JLabel(String.valueOf(count.get()));
        slider.addChangeListener(e -> {
            count.set(slider.getValue());
            countLabel.setText(String.valueOf(slider.getValue()));
        });
        toolbar.add(slider);
        toolbar.add(countLabel);

        toolbar.add(statusLabel);
        return toolbar;
    }

    private static final String SAMPLE_CODE = """
            public class FloodDemo {
                public static void main(String[] args) {
                    var list = List.of("alpha", "beta", "gamma", "delta", "epsilon");
                    for (String value : list) {
                        System.out.println(value);
                    }

                    int total = 0;
                    for (int i = 0; i < 50; i++) {
                        total += compute(i);
                    }
                    System.out.println("total = " + total);
                }

                static int compute(int n) {
                    int acc = 0;
                    for (int i = 1; i <= n; i++) {
                        acc += (i * 31) ^ (i << 2);
                    }
                    return acc;
                }
            }
            """;
}
