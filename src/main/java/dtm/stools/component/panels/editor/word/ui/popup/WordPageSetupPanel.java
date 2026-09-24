package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.WordPageSettings;
import javax.swing.*;

public final class WordPageSetupPanel extends WordPropertiesPanel<WordPageSettings> {
    private static final String[] SIZES = {"A4 (21 × 29,7 cm)","Carta (21,6 × 27,9 cm)","Ofício (21,6 × 35,6 cm)","A5 (14,8 × 21 cm)","Personalizado"};
    private static final float[][] DIMENSIONS = {{595.276f,841.89f},{612,792},{612,1008},{419.528f,595.276f}};
    private final WordPageSettings current;
    private final JComboBox<String> size = new JComboBox<>(SIZES);
    private final JComboBox<String> orientation = new JComboBox<>(new String[]{"Retrato","Paisagem"});
    private final JSpinner width, height, top, right, bottom, left, columns, spacing, header, footer, start;

    public WordPageSetupPanel(WordPageSettings settings) {
        this.current = settings;
        float w = Math.min(settings.width(),settings.height()), h = Math.max(settings.width(),settings.height());
        int match = 4; for (int i = 0; i < DIMENSIONS.length; i++) if (Math.abs(DIMENSIONS[i][0]-w) < 2 && Math.abs(DIMENSIONS[i][1]-h) < 2) match = i;
        size.setSelectedIndex(match); row("Tamanho do papel",size);
        orientation.setSelectedIndex(settings.landscape() ? 1 : 0); row("Orientação",orientation);
        width = row("Largura (cm)",number(cm(w),5,100,0.1)); height = row("Altura (cm)",number(cm(h),5,100,0.1));
        top = row("Margem superior (cm)",number(cm(settings.top()),0,20,0.1)); bottom = row("Margem inferior (cm)",number(cm(settings.bottom()),0,20,0.1));
        left = row("Margem esquerda (cm)",number(cm(settings.left()),0,20,0.1)); right = row("Margem direita (cm)",number(cm(settings.right()),0,20,0.1));
        columns = row("Colunas",number(settings.columns(),1,6,1)); spacing = row("Espaço entre colunas (cm)",number(cm(settings.columnSpacing()),0,5,0.1));
        header = row("Distância do cabeçalho (cm)",number(cm(settings.headerDistance()),0,10,0.1)); footer = row("Distância do rodapé (cm)",number(cm(settings.footerDistance()),0,10,0.1));
        start = row("Iniciar numeração em (0 = continuar)",number(settings.pageNumberStart(),0,9999,1));
        size.addActionListener(e -> { int i = size.getSelectedIndex(); if (i < DIMENSIONS.length) { width.setValue((double)cm(DIMENSIONS[i][0])); height.setValue((double)cm(DIMENSIONS[i][1])); } });
    }
    private static double cm(float pt) { return Math.round(pt/72*2.54*100)/100.0; }
    private static float pt(JSpinner s) { return value(s)/2.54f*72; }
    @Override public String title() { return "Configurar página"; }
    @Override public WordPageSettings result() {
        float w = pt(width), h = pt(height);
        boolean landscape = orientation.getSelectedIndex() == 1;
        float pw = landscape ? Math.max(w,h) : Math.min(w,h), ph = landscape ? Math.min(w,h) : Math.max(w,h);
        return new WordPageSettings(pw,ph,pt(top),pt(right),pt(bottom),pt(left),(int)value(columns),pt(spacing),pt(header),pt(footer),(int)value(start));
    }
    public WordPageSettings current() { return current; }
}
