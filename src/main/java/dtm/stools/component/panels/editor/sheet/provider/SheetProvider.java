package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.ProviderRegistration;

public interface SheetProvider {
    String id();
    default int priority() { return 0; }
    default ProviderRegistration attach(SheetEditor editor) { return ProviderRegistration.none(); }
}
