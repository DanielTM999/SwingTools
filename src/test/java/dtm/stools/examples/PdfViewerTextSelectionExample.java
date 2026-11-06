package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.pdf.PdfEditor;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public final class PdfViewerTextSelectionExample {
    private PdfViewerTextSelectionExample() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            PdfEditor editor = new PdfEditor();
            JFrame frame = new JFrame("PDF: seleção de texto e busca");
            JLabel help = new JLabel("Visualizar: arraste para selecionar texto, dê dois cliques para uma palavra e use Ctrl+C. Buscar: Ctrl+F e Enter.");
            help.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            frame.add(help, BorderLayout.NORTH);
            frame.add(editor, BorderLayout.CENTER);
            frame.setSize(1200, 820);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) { editor.close(); }
            });
            if (args.length > 0) editor.open(Path.of(args[0]));
            else {
                try {
                    editor.addText(0, "Selecione o texto deste PDF", 72, 760, 22);
                    editor.addText(0, "Arraste sobre esta frase para selecionar e copiar o texto.", 72, 710, 14);
                    editor.addText(0, "A busca destaca todas as ocorrências de PDF em amarelo.", 72, 675, 14);
                    editor.addText(0, "O modo Selecionar continua permitindo selecionar objetos do PDF.", 72, 640, 14);
                    editor.insertBlankPage(1);
                    editor.addText(1, "Outra ocorrência de PDF na segunda página.", 72, 760, 18);
                    editor.setCurrentPage(0);
                    editor.getSidebar().search("PDF");
                } catch (Exception error) { throw new IllegalStateException(error); }
            }
            frame.setVisible(true);
        });
    }
}
