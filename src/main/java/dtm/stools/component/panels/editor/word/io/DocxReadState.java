package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

final class DocxReadState {
    final Set<String> diagnostics = new LinkedHashSet<>();
    final Set<String> blocking = new LinkedHashSet<>();
    final Map<String,Map<String,String>> customData = new HashMap<>();
    final Map<String,WordImportResult.OriginalObject> originals = new HashMap<>();
    final Set<String> activeComments = new LinkedHashSet<>();
    WordResources resources = WordResources.EMPTY;
    WordStyleSheet styles = WordStyleSheet.defaults();
    WordNumbering numbering = WordNumbering.EMPTY;
    WordPageSettings settings = WordPageSettings.A4;
    String namespace = DocxNames.W;
    int objectCounter;
}
