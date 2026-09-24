package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.*;
import java.util.function.Supplier;

public final class WordShapePropertiesPanel extends WordPropertiesPanel<WordShape> {
    private final WordShape shape;
    private final JComboBox<WordShapeType> type = new JComboBox<>(WordShapeType.values());
    private final ColorButton fill, stroke, textColor;
    private final JSpinner strokeWidth, fontSize, width, height, rotation;
    private final JTextArea text = new JTextArea(3,28);
    private final JCheckBox arrow = new JCheckBox("Seta no final");
    private final JTextField alt = new JTextField();
    private final Supplier<WordPlacement> placement;

    public WordShapePropertiesPanel(WordShape shape) {
        this.shape = shape;
        if (shape.shapeType() == WordShapeType.GROUP) { type.setEnabled(false); row(null,new JLabel("Grupo com " + shape.children().size() + " formas")); }
        else { type.removeItem(WordShapeType.GROUP); type.setSelectedItem(shape.shapeType()); row("Forma",type); }
        fill = new ColorButton(shape.fill(),true); row("Preenchimento",fill.withClear());
        stroke = new ColorButton(shape.stroke(),true); row("Contorno",stroke.withClear());
        strokeWidth = row("Espessura do contorno (pt)",number(shape.strokeWidth(),0,50,0.25));
        arrow.setSelected(shape.arrowEnd()); arrow.setOpaque(false); row(null,arrow);
        text.setText(shape.text()); text.setLineWrap(true); text.setWrapStyleWord(true); row("Texto",new JScrollPane(text));
        fontSize = row("Tamanho do texto",number(shape.fontSize(),1,400,1));
        textColor = new ColorButton(shape.textColor(),false); row("Cor do texto",textColor);
        width = row("Largura (pt)",number(shape.width(),1,14400,1)); height = row("Altura (pt)",number(shape.height(),0,14400,1));
        rotation = row("Rotação (graus)",number(shape.rotation(),0,359,1));
        placement = placement(shape.placement());
        alt.setText(shape.altText()); row("Texto alternativo",alt);
        boolean group = shape.shapeType() == WordShapeType.GROUP;
        text.setEnabled(!group); fill.setEnabled(!group); stroke.setEnabled(!group);
    }
    @Override public String title() { return "Formatar forma"; }
    @Override public WordShape result() {
        WordShape s = shape;
        if (s.shapeType() != WordShapeType.GROUP) s = s.withShapeType((WordShapeType)type.getSelectedItem()).withColors(fill.color(),stroke.color(),value(strokeWidth)).withText(text.getText(),value(fontSize),textColor.color());
        return s.withArrowEnd(arrow.isSelected()).resize(value(width),value(height)).withRotation(value(rotation)).withPlacement(placement.get()).withAltText(alt.getText().strip());
    }
}
