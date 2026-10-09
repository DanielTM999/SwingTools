package dtm.stools.component.panels.editor.powerpoint.provider;

import java.awt.Color;
import java.awt.Component;
import java.util.Optional;

public interface PowerPointDialogProvider extends PowerPointProvider {
    Optional<String> editText(Component owner,String current);
    Optional<Color> chooseColor(Component owner,String title,Color current);
    boolean confirmDiscardChanges(Component owner);
    default Optional<java.awt.Dimension> chooseTableSize(Component owner){return Optional.of(new java.awt.Dimension(3,3));}
    default Optional<dtm.stools.component.panels.editor.powerpoint.model.PptText> editTextLayout(Component owner,dtm.stools.component.panels.editor.powerpoint.model.PptText text){return Optional.empty();}
    default void message(Component owner,String title,String message){
        dtm.stools.component.popup.ModernDialog.builder().title(title).message(message).option("OK",0).show(owner);
    }
}
