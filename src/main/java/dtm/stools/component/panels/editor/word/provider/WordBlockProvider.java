package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.model.WordCustomObject;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Optional;

public interface WordBlockProvider extends WordProvider {
    String objectType();
    String displayName();
    WordCustomObject createObject(WordEditor editor);
    void paint(Graphics2D graphics, WordCustomObject object, Rectangle2D.Float bounds);
    default Optional<WordCustomObject> edit(Component owner, WordCustomObject object) { return Optional.empty(); }
    default Map<String,String> decode(Map<String,String> stored) { return stored; }
    default Map<String,String> encode(WordCustomObject object) { return object.data(); }
    default String exportText(WordCustomObject object) { return object.plainText(); }
}
