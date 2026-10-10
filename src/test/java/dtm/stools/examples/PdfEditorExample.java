package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.pdf.PdfEditor;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.nio.file.Path;

public final class PdfEditorExample {
    private PdfEditorExample() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            JFrame frame = new JFrame("SwingTools PDF Editor");
            PdfEditor editor = new PdfEditor();
            frame.setLayout(new BorderLayout());
            frame.add(editor, BorderLayout.CENTER);
            frame.setSize(1100, 750);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent event) { editor.close(); }
            });
            frame.setVisible(true);
            if (args.length > 0) editor.open(Path.of(args[0]));
        });
    }
}
