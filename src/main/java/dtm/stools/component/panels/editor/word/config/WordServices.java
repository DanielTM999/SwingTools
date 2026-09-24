package dtm.stools.component.panels.editor.word.config;

import dtm.stools.component.panels.editor.word.io.DocxCodec;
import dtm.stools.component.panels.editor.word.layout.WordLayoutEngine;
import dtm.stools.component.panels.editor.word.render.WordRenderer;
import dtm.stools.component.panels.editor.word.ui.WordUiFactory;
import java.util.Objects;

public record WordServices(DocxCodec docx,WordLayoutEngine layout,WordRenderer renderer,WordUiFactory uiFactory) {
    public WordServices {Objects.requireNonNull(docx);Objects.requireNonNull(layout);Objects.requireNonNull(renderer);Objects.requireNonNull(uiFactory);}
    public static WordServices defaults(){return new WordServices(new DocxCodec(),new WordLayoutEngine(),new WordRenderer(),WordUiFactory.defaults());}
}
