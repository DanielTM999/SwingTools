package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import javax.swing.*;
import java.awt.*;

public final class WordTableBandingPanel extends WordPropertiesPanel<WordTableBanding> {
    private final JComboBox<String> preset=new JComboBox<>(new String[]{"Azul","Verde","Cinza","Personalizado"});
    private final ColorButton first=new ColorButton(0xDEEAF6,true),second=new ColorButton(0xFFFFFF,true);
    private final JSpinner bands=number(1,1,100,1);
    private final JCheckBox headers=new JCheckBox("Preservar as cores das linhas de cabeçalho",true);
    private final JComboBox<String> scope=new JComboBox<>(new String[]{"Tabela inteira","Intervalo de linhas"});
    private final JSpinner start,end;
    private final JLabel[] preview;
    private boolean updating;

    public WordTableBandingPanel(WordTable table) {
        int count=table.rows().size();
        start=number(1,1,count,1);end=number(count,1,count,1);preview=new JLabel[Math.min(6,count)];
        row("Aplicar em",scope);row("Linha inicial",start);row("Linha final",end);
        row(null,new JLabel("Linhas numeradas a partir de 1, incluindo o cabeçalho."));
        row("Paleta",preset);row("Primeira cor",first.withClear());row("Segunda cor",second.withClear());
        row("Alternar a cada quantas linhas",bands);row(null,headers);
        JPanel sample=new JPanel(new GridLayout(preview.length,1));
        for(int i=0;i<preview.length;i++) {
            JLabel label=new JLabel();
            label.setName("word.table.banding.preview."+i);
            label.setOpaque(true);label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xB8C4D0)),BorderFactory.createEmptyBorder(6,10,6,10)));
            preview[i]=label;sample.add(label);
        }
        row("Prévia",sample);row(null,new JLabel("As cores podem ser desfeitas com Ctrl+Z."));
        preset.addActionListener(e->{
            int i=preset.getSelectedIndex();if(i==3)return;
            int[] colors={0xDEEAF6,0xE2EFD9,0xE7E6E6};
            updating=true;try{first.setColor(colors[i]);second.setColor(0xFFFFFF);}finally{updating=false;}refreshPreview(table);
        });
        for(ColorButton button:new ColorButton[]{first,second})button.addPropertyChangeListener("color",e->{
            if(!updating)preset.setSelectedIndex(3);refreshPreview(table);
        });
        bands.addChangeListener(e->refreshPreview(table));headers.addActionListener(e->refreshPreview(table));
        scope.addActionListener(e->{start.setEnabled(scope.getSelectedIndex()==1);end.setEnabled(scope.getSelectedIndex()==1);refreshPreview(table);});
        start.addChangeListener(e->refreshPreview(table));end.addChangeListener(e->refreshPreview(table));
        scope.setName("word.table.banding.scope");start.setName("word.table.banding.start");end.setName("word.table.banding.end");
        start.setEnabled(false);end.setEnabled(false);
        first.setName("word.table.banding.first");second.setName("word.table.banding.second");bands.setName("word.table.banding.size");
        headers.setName("word.table.banding.headers");preset.setName("word.table.banding.preset");refreshPreview(table);
    }
    private void refreshPreview(WordTable table) {
        int from=scope.getSelectedIndex()==0?0:((Number)start.getValue()).intValue()-1;
        int to=scope.getSelectedIndex()==0?table.rows().size()-1:((Number)end.getValue()).intValue()-1;
        WordTable colored=from<=to?WordTableEditing.alternateRows(table,result()):table;
        int previewStart=Math.max(0,Math.min(table.rows().size()-preview.length,from-1));
        for(int i=0;i<preview.length;i++) {
            int row=previewStart+i;
            Integer rgb=colored.rows().get(row).cells().getFirst().fill();
            preview[i].setText("Linha "+(row+1)+(table.rows().get(row).header()?" (cabeçalho)":"")+"     |     Dados");
            Color background=new Color(rgb==null?0xFFFFFF:rgb);
            preview[i].setBackground(background);
            preview[i].setForeground(background.getRed()*0.299+background.getGreen()*0.587+background.getBlue()*0.114<145?Color.WHITE:Color.BLACK);
        }
    }
    @Override public String title(){return "Cores alternadas da tabela";}
    @Override public WordTableBanding result(){
        if(scope.getSelectedIndex()==0)return new WordTableBanding(first.color(),second.color(),((Number)bands.getValue()).intValue(),headers.isSelected());
        return new WordTableBanding(first.color(),second.color(),((Number)bands.getValue()).intValue(),headers.isSelected(),
                ((Number)start.getValue()).intValue()-1,((Number)end.getValue()).intValue()-1);
    }
}
