package dtm.stools.component.panels.editor.pdf.provider;

import java.awt.Component;
import java.nio.file.Path;
import java.util.Optional;

public interface PdfFileDialogProvider extends PdfProvider {
    Optional<Path> chooseOpen(Component parent);
    Optional<Path> chooseSave(Component parent);
    default Optional<Path> chooseImage(Component parent) { return chooseOpen(parent); }
    default Optional<Path> chooseCertificate(Component parent) { return chooseOpen(parent); }
}
