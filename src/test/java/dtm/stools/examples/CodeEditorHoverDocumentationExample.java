package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.hover.HoverInfo;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;

public class CodeEditorHoverDocumentationExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorHoverDocumentationExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor hover documentation example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 640);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        CodeEditor editor = new CodeEditor("""
                public class HoverDemo {
                    private final CodeEditor editor = new CodeEditor();

                    public void configureHoverDocumentation() {
                        editor.addProvider(new HoverDocumentationProvider() {
                            @Override
                            public HoverInfo provideHover(HoverDocumentationContext context) {
                                return HoverInfo.markdown("Hover documentation");
                            }
                        });
                    }
                }
                """);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.addProvider((HoverDocumentationProvider) context -> {
            String word = wordAt(context.buffer().lineAt(context.line()), context.col());
            return switch (word) {
                case "CodeEditor" -> HoverInfo.markdown("""
                        **CodeEditor**

                        High-level Swing component that wraps `CodeEditorTextArea`, gutter, scroll pane and editor providers.

                        Move the mouse from this word down to this popup. The popup should remain visible long enough to enter it.
                        """);
                case "HoverDocumentationProvider" -> HoverInfo.markdown("""
                        **HoverDocumentationProvider**

                        Provides documentation for the current line, column and offset.

                        This example keeps the popup interactive while the mouse crosses the transition area between the text and the popup.
                        """);
                case "HoverInfo" -> HoverInfo.markdown("""
                        **HoverInfo**

                        Represents hover content as plain text, HTML or Markdown.

                        The rendered popup can be inspected with the mouse without disappearing before the pointer reaches it.
                        """);
                default -> null;
            };
        });

        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static String wordAt(String line, int col) {
        if (line == null || line.isEmpty()) return "";
        int probe = Math.min(Math.max(0, col), line.length() - 1);
        if (!isWordChar(line.charAt(probe)) && probe > 0 && isWordChar(line.charAt(probe - 1))) {
            probe--;
        }
        if (!isWordChar(line.charAt(probe))) return "";

        int start = probe;
        while (start > 0 && isWordChar(line.charAt(start - 1))) start--;

        int end = probe + 1;
        while (end < line.length() && isWordChar(line.charAt(end))) end++;

        return line.substring(start, end);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
