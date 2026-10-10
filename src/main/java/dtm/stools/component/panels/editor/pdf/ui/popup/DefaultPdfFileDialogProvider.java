package dtm.stools.component.panels.editor.pdf.ui.popup;

import dtm.stools.component.inputfields.osfilepicker.DeFilter;
import dtm.stools.component.inputfields.osfilepicker.OsFilePicker;
import dtm.stools.component.panels.editor.pdf.provider.PdfFileDialogProvider;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class DefaultPdfFileDialogProvider implements PdfFileDialogProvider {
    private static final DeFilter PDF = DeFilter.of("Documento PDF (*.pdf)", "pdf");
    private static final DeFilter IMAGES = DeFilter.of("Imagens (*.png, *.jpg, *.gif, *.bmp)", "png", "jpg", "jpeg", "gif", "bmp");
    private static final DeFilter CERTIFICATES = DeFilter.of("Certificado PKCS#12 (*.p12, *.pfx)", "p12", "pfx");

    private File directory;
    private boolean nativeAvailable = true;

    @Override
    public String id() { return "pdf.file.default"; }
    @Override
    public Optional<Path> chooseOpen(Component parent) { return open(parent, "Abrir PDF", PDF); }
    @Override
    public Optional<Path> chooseImage(Component parent) { return open(parent, "Inserir imagem", IMAGES); }
    @Override
    public Optional<Path> chooseCertificate(Component parent) { return open(parent, "Escolher certificado", CERTIFICATES); }

    @Override
    public Optional<Path> chooseSave(Component parent) {
        File selected = null;
        if (nativeAvailable) {
            try { selected = OsFilePicker.saveFile("Salvar PDF", initial(), "documento.pdf", PDF); }
            catch (LinkageError | RuntimeException error) { nativeAvailable = false; }
        }
        if (!nativeAvailable) selected = swing(parent, PDF, true);
        if (selected == null) return Optional.empty();
        directory = selected.getParentFile();
        Path path = selected.toPath();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
            path = path.resolveSibling(path.getFileName() + ".pdf");
        return Optional.of(path);
    }

    private Optional<Path> open(Component parent, String title, DeFilter filter) {
        File selected = null;
        if (nativeAvailable) {
            try { selected = OsFilePicker.openFile(title, initial(), filter); }
            catch (LinkageError | RuntimeException error) { nativeAvailable = false; }
        }
        if (!nativeAvailable) selected = swing(parent, filter, false);
        if (selected == null) return Optional.empty();
        directory = selected.getParentFile();
        return Optional.of(selected.toPath());
    }

    private File initial() {
        return directory != null ? directory : new File(System.getProperty("user.home"));
    }

    private File swing(Component parent, DeFilter filter, boolean save) {
        JFileChooser chooser = new JFileChooser(initial());
        chooser.setFileFilter(new FileNameExtensionFilter(filter.name(), filter.ext()));
        int result = save ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
}
