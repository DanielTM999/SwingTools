package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.WordPageSettings;
import dtm.stools.component.panels.editor.word.model.WordSectionProperties;
import javax.swing.*;

public final class WordPageSetupPanel extends WordPropertiesPanel<WordPageSettings> {
    private static final String[] SIZES = {"A4 (21 × 29,7 cm)","Carta (21,6 × 27,9 cm)","Ofício (21,6 × 35,6 cm)","A5 (14,8 × 21 cm)","Personalizado"};
    private static final float[][] DIMENSIONS = {{595.276f,841.89f},{612,792},{612,1008},{419.528f,595.276f}};
    private final WordPageSettings current;
    private final JComboBox<String> size = new JComboBox<>(SIZES);
    private final JComboBox<String> orientation = new JComboBox<>(new String[]{"Retrato","Paisagem"});
    private final JComboBox<String> breakType=new JComboBox<>(new String[]{"Próxima página","Contínua","Página par","Página ímpar","Próxima coluna"});
    private final JSpinner width, height, top, right, bottom, left, columns, spacing, header, footer, start;

    public WordPageSetupPanel(WordPageSettings settings) {
        this.current = settings;
        float w = Math.min(settings.width(),settings.height()), h = Math.max(settings.width(),settings.height());
        int match = 4; for (int i = 0; i < DIMENSIONS.length; i++) if (Math.abs(DIMENSIONS[i][0]-w) < 2 && Math.abs(DIMENSIONS[i][1]-h) < 2) match = i;
        size.setSelectedIndex(match); row("Tamanho do papel",size);
        orientation.setSelectedIndex(settings.landscape() ? 1 : 0); row("Orientação",orientation);
        width = row("Largura (cm)",number(cm(w),0,508,0.1)); height = row("Altura (cm)",number(cm(h),0,508,0.1));
        top = row("Margem superior (cm)",number(cm(settings.top()),0,508,0.1)); bottom = row("Margem inferior (cm)",number(cm(settings.bottom()),0,508,0.1));
        left = row("Margem esquerda (cm)",number(cm(settings.left()),0,508,0.1)); right = row("Margem direita (cm)",number(cm(settings.right()),0,508,0.1));
        columns = row("Colunas",number(settings.columns(),1,10,1)); spacing = row("Espaço entre colunas (cm)",number(cm(settings.columnSpacing()),0,508,0.1));
        header = row("Distância do cabeçalho (cm)",number(cm(settings.headerDistance()),0,508,0.1)); footer = row("Distância do rodapé (cm)",number(cm(settings.footerDistance()),0,508,0.1));
        start = row("Iniciar numeração em (0 = continuar)",number(settings.pageNumberStart(),0,Integer.MAX_VALUE,1));
        breakType.setSelectedIndex(settings.section().breakType().ordinal());row("Início da seção",breakType);
        size.addActionListener(e -> { int i = size.getSelectedIndex(); if (i < DIMENSIONS.length) { width.setValue((double)cm(DIMENSIONS[i][0])); height.setValue((double)cm(DIMENSIONS[i][1])); } });
    }
    private static double cm(float pt) { return Math.round(pt/72*2.54*100)/100.0; }
    private static float pt(JSpinner s,float original) { return Math.abs(value(s)-cm(original))<0.00001 ? original : value(s)/2.54f*72; }
    @Override
    public String title() { return "Configurar página"; }
    @Override
    public WordPageSettings result() {
        float w = pt(width,Math.min(current.width(),current.height())), h = pt(height,Math.max(current.width(),current.height()));
        boolean landscape = orientation.getSelectedIndex() == 1;
        float pw = landscape ? Math.max(w,h) : Math.min(w,h), ph = landscape ? Math.min(w,h) : Math.max(w,h);
        return new WordPageSettings(pw,ph,pt(top,current.top()),pt(right,current.right()),pt(bottom,current.bottom()),pt(left,current.left()),(int)value(columns),pt(spacing,current.columnSpacing()),pt(header,current.headerDistance()),pt(footer,current.footerDistance()),((Number)start.getValue()).intValue(),
                current.section().withBreakType(WordSectionProperties.BreakType.values()[breakType.getSelectedIndex()]));
    }
    public WordPageSettings current() { return current; }
}
