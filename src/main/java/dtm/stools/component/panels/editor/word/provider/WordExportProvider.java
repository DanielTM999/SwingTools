package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.layout.WordLayout;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import java.io.*;

public interface WordExportProvider extends WordProvider {
    String extension();
    void export(WordDocument document,WordLayout layout,OutputStream output)throws IOException;
}
