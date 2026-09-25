package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.io.SheetImportResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface SheetImportProvider extends SheetProvider {
    List<String> extensions();
    String description();
    SheetImportResult read(InputStream input) throws IOException;
}
