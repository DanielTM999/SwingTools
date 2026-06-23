package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.CodeEditorMinimap;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class CodeEditorMinimapPreviewMirrorExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorMinimapPreviewMirrorExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor minimap preview mirror example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        CodeEditor editor = buildEditor();

        frame.add(buildToolbar(editor), BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static CodeEditor buildEditor() {
        CodeEditor editor = new CodeEditor(buildSampleCode());
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setFocusBorderEnabled(false);
        editor.setHighlightCurrentLine(true);
        editor.enableBreakpoint(true);
        editor.setCurrentLineColor(new Color(0x2A2F3A));

        editor.setMinimapVisibilityMode(CodeEditorMinimap.VisibilityMode.ALWAYS);

        editor.setLineChangeMarker(4, new Color(0x4FC3F7));
        editor.setLineChangeMarker(5, new Color(0x4FC3F7));
        editor.setLineChangeMarker(12, new Color(0xE57373));
        editor.setLineChangeMarker(30, new Color(0x81C784));

        editor.setLineColor(8, new Color(0x3A2E1E));
        editor.setLineColor(20, new Color(0x1E3A2E), new Color(0xA5D6A7));
        editor.setLineColor(21, new Color(0x1E3A2E), new Color(0xA5D6A7));
        editor.setLineColor(40, new Color(0x3A1E2E));

        return editor;
    }

    private static JPanel buildToolbar(CodeEditor editor) {
        JPanel toolbar = new JPanel();

        toolbar.add(new JLabel("Passe o mouse sobre o minimap / scrollbar para ver o preview."));

        JCheckBox mirror = new JCheckBox("Preview mirror (gutter + cores de linha)",
                editor.isMinimapPreviewMirrorEnabled());
        mirror.addActionListener(e ->
                editor.setMinimapPreviewMirrorEnabled(mirror.isSelected()));
        toolbar.add(mirror);

        return toolbar;
    }

    private static String buildSampleCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("public class MinimapPreviewMirrorDemo {\n\n");
        for (int i = 0; i < 30; i++) {
            sb.append("    public int compute").append(i).append("(int value) {\n");
            sb.append("        int result = value * ").append(i + 1).append(";\n");
            sb.append("        System.out.println(\"compute").append(i).append(" = \" + result);\n");
            sb.append("        return result;\n");
            sb.append("    }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
