package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class CodeEditorAutoDeactivateBreakpointExample {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorAutoDeactivateBreakpointExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor auto-deactivate breakpoint example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 680);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = buildEditor();
        JTextArea log = buildLog();

        // Auto-deactivate every breakpoint as soon as it is added.
        editor.addBreakpointChangeListener((breakpoint, added) -> {
            if (added && breakpoint.isActive()) {
                editor.deactivateBreakpoint(breakpoint.getLine());
            }
        });

        // Log what actually happens (added active / deactivated / removed).
        editor.addBreakpointChangeListener((breakpoint, added) -> {
            String action = added
                    ? (breakpoint.isActive() ? "added (active)" : "auto-deactivated")
                    : "removed";
            appendLog(log, breakpoint.getLine(), action);
        });

        // Both of these will land as inactive (outline-only) breakpoints.
        editor.addBreakpoint(2);
        editor.addBreakpoint(5);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                editor,
                new JScrollPane(log));
        split.setResizeWeight(0.72);
        split.setDividerLocation(760);

        frame.add(buildToolbar(editor), BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static CodeEditor buildEditor() {
        CodeEditor editor = new CodeEditor(SAMPLE_CODE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setFocusBorderEnabled(false);
        editor.setHighlightCurrentLine(true);
        editor.setCurrentLineColor(new Color(0x2A2F3A));

        CodeEditorGutter gutter = editor.getGutter();
        gutter.enableBreakpoint(true);
        gutter.setPreviewOnHoverEnabled(true);

        return editor;
    }

    private static JTextArea buildLog() {
        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        log.setText("""
                Adding a breakpoint auto-deactivates it (outline-only dot):
                - press F9 or click the gutter to add a breakpoint
                - it appears inactive immediately
                - use the toolbar to activate one or all of them

                """);
        return log;
    }

    private static JPanel buildToolbar(CodeEditor editor) {
        JPanel toolbar = new JPanel();

        JButton add = new JButton("Add breakpoint at caret");
        add.addActionListener(e -> editor.addBreakpoint(editor.getCaretLine()));
        toolbar.add(add);

        JButton activate = new JButton("Activate at caret");
        activate.addActionListener(e -> editor.activateBreakpoint(editor.getCaretLine()));
        toolbar.add(activate);

        JButton activateAll = new JButton("Activate all");
        activateAll.addActionListener(e -> editor.activateAllBreakpoints());
        toolbar.add(activateAll);

        JButton deactivateAll = new JButton("Deactivate all");
        deactivateAll.addActionListener(e -> editor.deactivateAllBreakpoints());
        toolbar.add(deactivateAll);

        JButton clear = new JButton("Clear breakpoints");
        clear.addActionListener(e -> editor.clearBreakpoints());
        toolbar.add(clear);

        return toolbar;
    }

    private static void appendLog(JTextArea log, int line, String action) {
        log.append("%s  breakpoint %s at line %d%n".formatted(
                LocalTime.now().format(TIME_FORMAT),
                action,
                line + 1));
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static final String SAMPLE_CODE = """
            public class AutoDeactivateDemo {
                public static void main(String[] args) {
                    var service = new Service();
                    service.start();

                    for (int i = 0; i < 3; i++) {
                        service.process("item-" + i);
                    }

                    service.stop();
                }
            }

            class Service {
                void start() {
                    System.out.println("start");
                }

                void process(String value) {
                    System.out.println(value);
                }

                void stop() {
                    System.out.println("stop");
                }
            }
            """;
}
