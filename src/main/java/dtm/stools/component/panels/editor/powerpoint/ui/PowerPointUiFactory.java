package dtm.stools.component.panels.editor.powerpoint.ui;

import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;
import dtm.stools.component.panels.editor.powerpoint.api.PowerPointSession;
import dtm.stools.component.panels.editor.powerpoint.render.PowerPointRenderer;
import dtm.stools.component.panels.editor.powerpoint.model.PptText;
import javax.swing.JComponent;

public interface PowerPointUiFactory {
    PowerPointCanvas createCanvas(PowerPointSession session,PowerPointRenderer renderer);
    default JComponent createRibbon(PowerPointEditor editor){return new PowerPointRibbon(editor);}
    default PptInlineTextEditor createTextEditor(PptText text,double scale){return new PptInlineTextEditor(text,scale);}
    static PowerPointUiFactory defaults(){return PowerPointCanvas::new;}
}
