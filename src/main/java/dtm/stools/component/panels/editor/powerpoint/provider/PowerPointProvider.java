package dtm.stools.component.panels.editor.powerpoint.provider;

import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;

public interface PowerPointProvider {
    String id();
    default int priority(){return 0;}
    default AutoCloseable attach(PowerPointEditor editor){return ()->{};}
}
