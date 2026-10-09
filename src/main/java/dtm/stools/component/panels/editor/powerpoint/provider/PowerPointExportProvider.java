package dtm.stools.component.panels.editor.powerpoint.provider;

import dtm.stools.component.panels.editor.powerpoint.model.Presentation;
import java.io.IOException;
import java.io.OutputStream;

public interface PowerPointExportProvider extends PowerPointProvider {
    String extension();
    void export(Presentation snapshot,OutputStream output)throws IOException;
}
