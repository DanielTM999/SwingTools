package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.WordParagraphStyle;
import javax.swing.*;

public final class WordParagraphPropertiesPanel extends WordPropertiesPanel<WordParagraphStyle> {
    private final WordParagraphStyle current;
    private final JComboBox<String> rule = new JComboBox<>(new String[]{"Múltiplo (linhas)","Exatamente (pt)","No mínimo (pt)"});
    private final JSpinner before,after,line,left,right,first;
    private final JCheckBox keepNext = new JCheckBox("Manter com o próximo"), keepLines = new JCheckBox("Manter linhas juntas"),
            widow = new JCheckBox("Controlar linhas viúvas e órfãs"), pageBreak = new JCheckBox("Quebra de página antes");

    public WordParagraphPropertiesPanel(WordParagraphStyle current) {
        this.current=current;
        before=row("Antes (pt)",number(current.before(),0,14400,1));
        after=row("Depois (pt)",number(current.after(),0,14400,1));
        rule.setSelectedIndex(current.lineSpacingRule().ordinal());row("Espaçamento entre linhas",rule);
        line=row("Valor",number(current.lineSpacing(),0.05,14400,0.05));
        left=row("Recuo esquerdo (pt)",number(current.leftIndent(),-14400,14400,1));
        right=row("Recuo direito (pt)",number(current.rightIndent(),-14400,14400,1));
        first=row("Primeira linha (pt)",number(current.firstLineIndent(),-14400,14400,1));
        check(keepNext,current.keepWithNext());check(keepLines,current.keepLines());check(widow,current.widowControl());check(pageBreak,current.pageBreakBefore());
        rule.addActionListener(e->{
            boolean auto=rule.getSelectedIndex()==0;
            double value=((Number)line.getValue()).doubleValue();
            line.setModel(new SpinnerNumberModel(auto?Math.max(0.5,Math.min(10,value)):Math.max(0.05,value),auto?0.5:0.05,auto?10:14400,auto?0.05:1));
        });
    }
    private void check(JCheckBox box,boolean selected){box.setOpaque(false);box.setSelected(selected);row(null,box);}
    @Override public String title(){return "Parágrafo";}
    @Override public WordParagraphStyle result(){
        return current.withSpacing(value(before),value(after),1)
                .withLineSpacing(WordParagraphStyle.LineSpacingRule.values()[rule.getSelectedIndex()],value(line))
                .withIndents(value(left),value(right),value(first)).withKeepWithNext(keepNext.isSelected())
                .withKeepLines(keepLines.isSelected()).withWidowControl(widow.isSelected()).withPageBreakBefore(pageBreak.isSelected());
    }
}
