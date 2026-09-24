package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.component.panels.editor.word.api.WordSession;
import dtm.stools.component.panels.editor.word.layout.WordLayoutEngine;
import dtm.stools.component.panels.editor.word.render.WordRenderer;

@FunctionalInterface
public interface WordUiFactory {
    WordCanvas createCanvas(WordSession session,WordLayoutEngine layout,WordRenderer renderer);
    static WordUiFactory defaults(){return WordCanvas::new;}
}
