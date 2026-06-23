package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.gutter.CodeEditorGutter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

public class CodeEditorMarkerEventsExample {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorMarkerEventsExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor marker events example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 680);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = buildEditor();
        JTextArea log = buildLog();

        editor.addBreakpointChangeListener((breakpoint, added) ->
                appendLog(log, "breakpoint", breakpoint.getLine(),
                        added ? (breakpoint.isActive() ? "added" : "deactivated") : "removed"));
        editor.addBookmarkChangeListener((line, added) ->
                appendLog(log, "bookmark", line, added));

        editor.addBreakpoint(5);
        editor.addBookmark(9);

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
        gutter.enableBookmark(true);

        gutter.setPreviewOnHoverEnabled(true);
        gutter.setBookmarkPreviewOnHoverEnabled(true);

        return editor;
    }

    private static JTextArea buildLog() {
        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        log.setText("""
                Events:
                - press F9 or click the gutter to toggle a breakpoint
                - press F11 or use the toolbar to toggle a bookmark at the caret line
                - bookmark clicks are disabled by default and can be enabled below

                """);
        return log;
    }

    private static JPanel buildToolbar(CodeEditor editor) {
        JPanel toolbar = new JPanel();

        JButton breakpoint = new JButton("Toggle breakpoint at caret");
        breakpoint.addActionListener(e -> editor.toggleBreakpoint(editor.getCaretLine()));
        toolbar.add(breakpoint);

        JCheckBox breakpointClick = new JCheckBox("Breakpoint click", true);
        breakpointClick.addActionListener(e -> editor.setBreakpointEnableOnClick(breakpointClick.isSelected()));
        toolbar.add(breakpointClick);

        JButton bookmark = new JButton("Toggle bookmark at caret");
        bookmark.addActionListener(e -> editor.toggleBookmark(editor.getCaretLine()));
        toolbar.add(bookmark);

        JCheckBox bookmarkClick = new JCheckBox("Bookmark click");
        bookmarkClick.addActionListener(e -> editor.setBookmarkEnableOnClick(bookmarkClick.isSelected()));
        toolbar.add(bookmarkClick);

        JButton clear = new JButton("Clear markers");
        clear.addActionListener(e -> {
            editor.clearBreakpoints();
            editor.clearBookmarks();
        });
        toolbar.add(clear);

        return toolbar;
    }

    private static void appendLog(JTextArea log, String marker, int line, boolean added) {
        appendLog(log, marker, line, added ? "added" : "removed");
    }

    private static void appendLog(JTextArea log, String marker, int line, String action) {
        log.append("%s  %s %s at line %d%n".formatted(
                LocalTime.now().format(TIME_FORMAT),
                marker,
                action,
                line + 1));
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static final String SAMPLE_CODE = """
            public class MarkerEventsDemo {
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
