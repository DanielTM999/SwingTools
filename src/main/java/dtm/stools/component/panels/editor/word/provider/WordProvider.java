package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.WordEditor;

public interface WordProvider {
    String id();
    default int priority() { return 0; }
    default ProviderRegistration attach(WordEditor editor) { return ()->{}; }
}
