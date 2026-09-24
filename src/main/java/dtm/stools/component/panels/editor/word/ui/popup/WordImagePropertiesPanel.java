package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.*;
import java.util.function.Supplier;

public final class WordImagePropertiesPanel extends WordPropertiesPanel<WordImage> {
    private final WordImage image;
    private final JSpinner width, height, rotation, cropLeft, cropTop, cropRight, cropBottom;
    private final JCheckBox lock = new JCheckBox("Manter proporção");
    private final JTextArea alt = new JTextArea(3,28);
    private final Supplier<WordPlacement> placement;

    public WordImagePropertiesPanel(WordImage image) {
        this.image = image;
        width = row("Largura (pt)",number(image.width(),1,14400,1));
        height = row("Altura (pt)",number(image.height(),1,14400,1));
        lock.setSelected(image.lockAspectRatio()); lock.setOpaque(false); row(null,lock);
        float ratio = image.height()/image.width();
        boolean[] syncing = {false};
        width.addChangeListener(e -> { if (lock.isSelected() && !syncing[0]) { syncing[0] = true; height.setValue((double)Math.max(1,value(width)*ratio)); syncing[0] = false; } });
        height.addChangeListener(e -> { if (lock.isSelected() && !syncing[0]) { syncing[0] = true; width.setValue((double)Math.max(1,value(height)/ratio)); syncing[0] = false; } });
        rotation = row("Rotação (graus)",number(image.rotation(),0,359,1));
        cropLeft = row("Recorte esquerdo (%)",number(image.crop().left()*100,0,45,1));
        cropTop = row("Recorte superior (%)",number(image.crop().top()*100,0,45,1));
        cropRight = row("Recorte direito (%)",number(image.crop().right()*100,0,45,1));
        cropBottom = row("Recorte inferior (%)",number(image.crop().bottom()*100,0,45,1));
        placement = placement(image.placement());
        alt.setText(image.altText()); alt.setLineWrap(true); alt.setWrapStyleWord(true);
        row("Texto alternativo",new JScrollPane(alt));
    }
    @Override public String title() { return "Propriedades da imagem"; }
    @Override public WordImage result() {
        return image.resize(value(width),value(height)).withLockAspectRatio(lock.isSelected()).withRotation(value(rotation))
                .withCrop(new WordCrop(value(cropLeft)/100,value(cropTop)/100,value(cropRight)/100,value(cropBottom)/100))
                .withPlacement(placement.get()).withAltText(alt.getText().strip());
    }
}
