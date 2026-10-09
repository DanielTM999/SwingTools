package dtm.stools.component.panels.editor.powerpoint.ui.popup;

import dtm.stools.component.panels.editor.powerpoint.provider.PowerPointDialogProvider;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.component.popup.ModernComponentDialog;
import dtm.stools.component.panels.editor.powerpoint.model.PptText;
import dtm.stools.component.popup.ModernInputDialog;
import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public final class DefaultPowerPointDialogProvider implements PowerPointDialogProvider {
    @Override public String id(){return "powerpoint.dialog.default";}

    @Override public Optional<String> editText(Component owner,String current){
        JTextArea area=new JTextArea(current,8,32);
        area.setLineWrap(true);area.setWrapStyleWord(true);
        JScrollPane scroll=new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(420,190));
        return Optional.ofNullable(ModernInputDialog.builder().title("Editar texto").message("Texto do objeto")
                .input(scroll).valueSupplier(area::getText).enterConfirms(false).closeOnEsc(true).show(owner));
    }

    @Override public Optional<Color> chooseColor(Component owner,String title,Color current){
        JColorChooser chooser=new JColorChooser(current);
        return new PowerPointDialogActivity<>(owner,title,chooser,chooser::getColor).showResult();
    }

    @Override public boolean confirmDiscardChanges(Component owner){
        return ModernDialog.builder().title("Abrir apresentação").message("Descartar alterações não salvas?")
                .type(ModernDialog.Type.QUESTION).option("Descartar",JOptionPane.YES_OPTION)
                .option("Cancelar",JOptionPane.NO_OPTION).enterConfirms(false).closeOnEsc(true)
                .show(owner)==JOptionPane.YES_OPTION;
    }
    @Override public Optional<Dimension> chooseTableSize(Component owner){
        JPanel panel=new JPanel(new GridLayout(2,2,12,8));
        JSpinner rows=new JSpinner(new SpinnerNumberModel(3,1,100,1)),columns=new JSpinner(new SpinnerNumberModel(3,1,100,1));
        panel.add(new JLabel("Linhas"));panel.add(rows);panel.add(new JLabel("Colunas"));panel.add(columns);
        return Optional.ofNullable(ModernComponentDialog.<Dimension>builder().title("Inserir tabela").component(panel)
                .confirmText("Inserir").cancelText("Cancelar").result(context->new Dimension((int)columns.getValue(),(int)rows.getValue())).show(owner));
    }
    @Override public Optional<PptText> editTextLayout(Component owner,PptText text){
        JPanel panel=new JPanel(new GridLayout(7,2,12,8));JSpinner[] margins=new JSpinner[4];double[] values={text.left(),text.top(),text.right(),text.bottom()};String[] names={"Margem esquerda","Margem superior","Margem direita","Margem inferior"};
        for(int i=0;i<4;i++){margins[i]=new JSpinner(new SpinnerNumberModel(values[i],0.,1000.,1.));panel.add(new JLabel(names[i]));panel.add(margins[i]);}
        JComboBox<String> anchor=new JComboBox<>(new String[]{"Superior","Centralizado","Inferior"});anchor.setSelectedIndex("ctr".equals(text.anchor())?1:"b".equals(text.anchor())?2:0);panel.add(new JLabel("Alinhamento vertical"));panel.add(anchor);
        double before=text.paragraphs().isEmpty()?0:text.paragraphs().getFirst().before(),after=text.paragraphs().isEmpty()?0:text.paragraphs().getFirst().after();JSpinner spaceBefore=new JSpinner(new SpinnerNumberModel(before,0.,1000.,1.)),spaceAfter=new JSpinner(new SpinnerNumberModel(after,0.,1000.,1.));panel.add(new JLabel("Antes do parágrafo"));panel.add(spaceBefore);panel.add(new JLabel("Depois do parágrafo"));panel.add(spaceAfter);
        return Optional.ofNullable(ModernComponentDialog.<PptText>builder().title("Layout do texto").component(panel).confirmText("Aplicar").cancelText("Cancelar").result(context->new PptText(text.paragraphs().stream().map(p->new PptText.Paragraph(p.runs(),p.alignment(),((Number)spaceBefore.getValue()).doubleValue(),((Number)spaceAfter.getValue()).doubleValue(),p.lineSpacing())).toList(),((Number)margins[0].getValue()).doubleValue(),((Number)margins[1].getValue()).doubleValue(),((Number)margins[2].getValue()).doubleValue(),((Number)margins[3].getValue()).doubleValue(),new String[]{"t","ctr","b"}[anchor.getSelectedIndex()])).show(owner));
    }
}
