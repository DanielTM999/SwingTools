package dtm.stools.component.panels.editor.powerpoint.config;

import dtm.stools.component.panels.editor.powerpoint.io.PptxCodec;
import dtm.stools.component.panels.editor.powerpoint.render.PowerPointRenderer;
import dtm.stools.component.panels.editor.powerpoint.ui.PowerPointUiFactory;
import java.util.Objects;

public record PowerPointServices(PptxCodec pptx,PowerPointRenderer renderer,PowerPointUiFactory uiFactory) {
    public PowerPointServices {Objects.requireNonNull(pptx);Objects.requireNonNull(renderer);Objects.requireNonNull(uiFactory);}
    public static PowerPointServices defaults(){return new PowerPointServices(new PptxCodec(),new PowerPointRenderer(),PowerPointUiFactory.defaults());}
}
