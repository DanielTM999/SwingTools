package dtm.stools.component.panels.editor.powerpoint.ui;

import com.formdev.flatlaf.util.UIScale;
import dtm.stools.component.panels.editor.powerpoint.PowerPointEditor;
import dtm.stools.component.panels.editor.powerpoint.model.PptAnimation;
import dtm.stools.component.panels.editor.powerpoint.model.PptObject;
import dtm.stools.component.panels.editor.powerpoint.model.PptText;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Grouped, theme-aware ribbon with context tabs for the selected object. */
public final class PowerPointRibbon extends JPanel {
    private final JTabbedPane tabs=new JTabbedPane();
    private final List<AbstractButton> editingButtons=new ArrayList<>();
    private final List<JComponent> editingInputs=new ArrayList<>();
    private final List<JComboBox<String>> fontFamilies=new ArrayList<>();
    private final List<JSpinner> fontSizes=new ArrayList<>();
    private boolean updatingFonts;
    private final PowerPointEditor editor;
    private PptObject.Kind contextKind;
    private Component contextPage;

    public PowerPointRibbon(PowerPointEditor editor){
        super(new BorderLayout());this.editor=editor;setName("powerpoint.ribbon");
        setBorder(BorderFactory.createMatteBorder(0,0,1,0,UiTokens.border()));
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(5),UIScale.scale(12),UIScale.scale(4),UIScale.scale(12)));
        JLabel title=new JLabel("Apresentação");title.setFont(title.getFont().deriveFont(Font.BOLD,UIScale.scale(15f)));
        header.add(title,BorderLayout.WEST);
        JPanel quick=new JPanel(new FlowLayout(FlowLayout.TRAILING,2,0));quick.setOpaque(false);
        quick.add(small("Salvar",editor::chooseSave,false));quick.add(small("Desfazer",editor::undo,true));quick.add(small("Refazer",editor::redo,true));
        header.add(quick,BorderLayout.EAST);add(header,BorderLayout.NORTH);

        tabs.setName("powerpoint.ribbon.tabs");tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.putClientProperty("JTabbedPane.tabType","underlined");
        tabs.putClientProperty("JTabbedPane.tabHeight",UIScale.scale(32));
        tabs.addTab("Arquivo",page(group("Arquivo",large("Abrir",editor::chooseOpen,false),large("Salvar",editor::chooseSave,false),large("Compatibilidade",editor::showImportDiagnostics,false))));
        tabs.addTab("Página Inicial",page(
                group("Slides",large("Novo slide",editor::addSlide,true),stack(small("Duplicar",editor::duplicateSlide,true),small("Excluir",editor::removeSlide,true))),
                group("Área de transferência",large("Colar",editor::pasteObject,true),stack(small("Copiar",editor::copySelectedObject,false),small("Excluir objeto",editor::removeSelectedObject,true))),
                group("Texto",large("Editar texto",editor::editSelectedText,true)),textTools(),
                group("Organizar",stack(small("Trazer à frente",editor::bringSelectedToFront,true),small("Enviar ao fundo",editor::sendSelectedToBack,true)),
                        stack(small("Alinhar esquerda",()->editor.alignSelected("left"),true),small("Centralizar",()->editor.alignSelected("center"),true)))));
        tabs.addTab("Inserir",page(group("Texto",large("Caixa de texto",()->editor.insertText("Texto"),true)),
                group("Formas",large("Retângulo",()->editor.insertShape(PptObject.Kind.RECTANGLE),true),large("Elipse",()->editor.insertShape(PptObject.Kind.ELLIPSE),true),
                        stack(small("Arredondado",()->editor.insertShape(PptObject.Kind.ROUND_RECTANGLE),true),small("Losango",()->editor.insertShape(PptObject.Kind.DIAMOND),true))),
                group("Estrutura",large("Tabela",editor::chooseInsertTable,true),large("Conector",editor::insertConnector,true)),
                group("Mídia",large("Imagem",()->editor.chooseInsertMedia(PptObject.Kind.IMAGE),true),
                        stack(small("Áudio",()->editor.chooseInsertMedia(PptObject.Kind.AUDIO),true),small("Vídeo",()->editor.chooseInsertMedia(PptObject.Kind.VIDEO),true)))));
        tabs.addTab("Design",page(group("Temas",large("Claro",()->editor.applyTheme("light"),true),large("Escuro",()->editor.applyTheme("dark"),true),large("Azul",()->editor.applyTheme("blue"),true)),
                group("Cores",large("Fundo",editor::chooseBackground,true),large("Objeto",editor::chooseObjectFill,true))));
        tabs.addTab("Transições",page(group("Transição do slide",large("Corte",()->editor.setTransition("cut"),true),large("Esmaecer",()->editor.setTransition("fade"),true),
                large("Varrer",()->editor.setTransition("wipe"),true),large("Empurrar",()->editor.setTransition("push"),true))));
        JComboBox<PptAnimation.Effect> effects=new JComboBox<>(PptAnimation.Effect.values());effects.setMaximumSize(new Dimension(UIScale.scale(190),UIScale.scale(32)));
        tabs.addTab("Animações",page(group("Efeito",effects,large("Adicionar",()->editor.addAnimation((PptAnimation.Effect)effects.getSelectedItem()),true)),
                group("Linha do tempo",new JLabel("Edite início, duração e ordem no painel lateral"))));
        tabs.addTab("Apresentação",page(group("Reprodução",large("Apresentar  F5",editor::startPresentation,false),large("Parar  Esc",editor::stopPresentation,false))));
        tabs.addTab("Exibir",page(group("Painéis",large("Miniaturas",()->editor.setThumbnailsVisible(!editor.isThumbnailsVisible()),false),
                large("Propriedades",()->editor.setPropertiesVisible(!editor.isPropertiesVisible()),false))));
        tabs.setSelectedIndex(1);add(tabs,BorderLayout.CENTER);
    }

    public JTabbedPane tabs(){return tabs;}

    public void refreshSelection(PptObject.Kind kind,boolean readOnly){
        refreshFontState();
        for(AbstractButton button:editingButtons)button.setEnabled(!readOnly);for(JComponent input:editingInputs)input.setEnabled(!readOnly);
        if(kind==contextKind)return;
        if(contextPage!=null){int index=tabs.indexOfComponent(contextPage);if(index>=0)tabs.removeTabAt(index);contextPage=null;}
        contextKind=kind;
        if(kind==null)return;
        String title=switch(kind){case TEXT->"Formato de Texto";case RECTANGLE,ELLIPSE,ROUND_RECTANGLE,DIAMOND->"Formato de Forma";case TABLE->"Tabela";case CONNECTOR->"Conector";case IMAGE->"Formato de Imagem";case AUDIO,VIDEO->"Formato de Mídia";};
        contextPage=page(group("Objeto",large("Duplicar",editor::duplicateSelectedObject,true),large("Excluir",editor::removeSelectedObject,true)),
                group("Organizar",large("Girar 90°",()->editor.rotateSelectedBy(90),true),large("Alinhar",()->editor.alignSelected("center"),true)),
                group("Aparência",large(kind==PptObject.Kind.TEXT?"Editar texto":"Cor do objeto",
                        kind==PptObject.Kind.TEXT?editor::editSelectedText:editor::chooseObjectFill,true)));
        if(kind==PptObject.Kind.TABLE)contextPage=page(tableTools(),textTools());
        else if(kind==PptObject.Kind.CONNECTOR)contextPage=page(lineTools());
        tabs.addTab(title,contextPage);
        tabs.setForegroundAt(tabs.indexOfComponent(contextPage),accent());
        for(AbstractButton button:editingButtons)button.setEnabled(!readOnly);for(JComponent input:editingInputs)input.setEnabled(!readOnly);
    }
    public void refreshFontState(){
        PptText.Style style=editor.getSelectedTextStyle();if(style==null)return;updatingFonts=true;
        try{for(JComboBox<String> family:fontFamilies)family.setSelectedItem(style.family());double size=Math.max(1,Math.min(400,style.size()/editor.getTextPointScale()));for(JSpinner input:fontSizes)input.setValue(size);}finally{updatingFonts=false;}
    }

    private JButton small(String label,Runnable action,boolean editing){return button(label,action,editing,false);}
    private JComponent textTools(){
        JComboBox<String> family=new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        family.setSelectedItem("Arial");family.setPreferredSize(UIScale.scale(new Dimension(130,28)));family.setName("powerpoint.font.family");
        editingInputs.add(family);
        fontFamilies.add(family);
        family.addActionListener(e->{if(!updatingFonts)editor.formatSelectedText(s->new PptText.Style((String)family.getSelectedItem(),s.size(),s.bold(),s.italic(),s.underline(),s.color()));});
        JSpinner size=new JSpinner(new SpinnerNumberModel(18.,1.,400.,.5));size.setPreferredSize(UIScale.scale(new Dimension(70,28)));size.setName("powerpoint.font.size");size.setToolTipText("Tamanho em pontos");
        editingInputs.add(size);
        fontSizes.add(size);
        size.addChangeListener(e->{if(!updatingFonts)editor.setSelectedFontSizePoints(((Number)size.getValue()).doubleValue());});
        return group("Fonte",stack(family,size),stack(small("Negrito",()->editor.formatSelectedText(s->new PptText.Style(s.family(),s.size(),!s.bold(),s.italic(),s.underline(),s.color())),true),
                small("Itálico",()->editor.formatSelectedText(s->new PptText.Style(s.family(),s.size(),s.bold(),!s.italic(),s.underline(),s.color())),true)),
                stack(small("Sublinhar",()->editor.formatSelectedText(s->new PptText.Style(s.family(),s.size(),s.bold(),s.italic(),!s.underline(),s.color())),true),small("Cor do texto",editor::chooseTextColor,true)),
                stack(small("Esquerda",()->editor.alignSelectedText("l"),true),small("Centralizado",()->editor.alignSelectedText("ctr"),true),small("Direita",()->editor.alignSelectedText("r"),true)),stack(small("Layout do texto",editor::chooseTextLayout,true)));
    }
    private JComponent tableTools(){
        JSpinner width=new JSpinner(new SpinnerNumberModel(200.,1.,4000.,5)),height=new JSpinner(new SpinnerNumberModel(60.,1.,4000.,5));
        width.setPreferredSize(UIScale.scale(new Dimension(85,28)));height.setPreferredSize(UIScale.scale(new Dimension(85,28)));
        editingInputs.add(width);editingInputs.add(height);
        JSpinner borderWidth=new JSpinner(new SpinnerNumberModel(1.,0.,100.,.5));JComboBox<String> borderDash=new JComboBox<>(new String[]{"Contínuo","Tracejado","Pontilhado"});editingInputs.add(borderWidth);editingInputs.add(borderDash);
        return group("Tabela",stack(small("Inserir linha",editor::insertTableRow,true),small("Excluir linha",editor::deleteTableRow,true)),
                stack(small("Inserir coluna",editor::insertTableColumn,true),small("Excluir coluna",editor::deleteTableColumn,true)),
                stack(small("Mesclar seleção",editor::mergeTableCells,true),small("Separar células",editor::splitTableCells,true)),
                stack(small("Cor da célula",editor::chooseObjectFill,true),small("Cor da borda",editor::chooseCellBorderColor,true)),stack(borderWidth,borderDash,small("Aplicar bordas",()->editor.setSelectedCellBorder(((Number)borderWidth.getValue()).doubleValue(),new String[]{"solid","dash","dot"}[borderDash.getSelectedIndex()]),true)),stack(width,small("Largura da coluna",()->editor.setTableColumnWidth(((Number)width.getValue()).doubleValue()),true)),
                stack(height,small("Altura da linha",()->editor.setTableRowHeight(((Number)height.getValue()).doubleValue()),true)));
    }
    private JComponent lineTools(){
        JSpinner width=new JSpinner(new SpinnerNumberModel(2.,.1,100.,.5));
        JComboBox<String> dash=new JComboBox<>(new String[]{"Contínuo","Tracejado","Pontilhado"}),head=new JComboBox<>(new String[]{"Sem ponta","Triângulo","Seta","Losango","Círculo"}),tail=new JComboBox<>(new String[]{"Triângulo","Sem ponta","Seta","Losango","Círculo"});
        editingInputs.addAll(List.of(width,dash,head,tail));
        return group("Linha",stack(new JLabel("Espessura"),width),stack(new JLabel("Traçado"),dash),stack(new JLabel("Início"),head),stack(new JLabel("Fim"),tail),
                stack(small("Aplicar",()->editor.setSelectedStroke(((Number)width.getValue()).doubleValue(),new String[]{"solid","dash","dot"}[dash.getSelectedIndex()],new String[]{"none","triangle","arrow","diamond","oval"}[head.getSelectedIndex()],new String[]{"triangle","none","arrow","diamond","oval"}[tail.getSelectedIndex()]),true),small("Cor da linha",editor::chooseStrokeColor,true)));
    }
    private JButton large(String label,Runnable action,boolean editing){return button(label,action,editing,true);}
    private JButton button(String label,Runnable action,boolean editing,boolean large){
        JButton button=new JButton(label);button.setName("powerpoint.ribbon."+label.toLowerCase().replace(' ','_'));
        button.putClientProperty("JButton.buttonType","toolBarButton");button.setFocusable(false);
        button.setMargin(large?new Insets(8,10,8,10):new Insets(3,7,3,7));
        if(large){button.setIcon(new GlyphIcon(label));button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);button.setIconTextGap(UIScale.scale(4));
            button.setPreferredSize(new Dimension(Math.max(UIScale.scale(82),button.getPreferredSize().width+UIScale.scale(8)),UIScale.scale(68)));}
        button.addActionListener(e->action.run());if(editing)editingButtons.add(button);return button;
    }
    private static JComponent stack(JComponent... items){JPanel panel=new JPanel(new GridLayout(items.length,1,0,2));panel.setOpaque(false);for(JComponent item:items)panel.add(item);return panel;}
    private static JComponent group(String title,JComponent... items){
        JPanel content=new JPanel(new FlowLayout(FlowLayout.LEADING,3,2));content.setOpaque(false);
        for(JComponent item:items)content.add(item);
        JPanel group=new JPanel(new BorderLayout(0,4));group.setOpaque(false);
        group.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1,UiTokens.border()),BorderFactory.createEmptyBorder(4,6,3,8)));
        group.add(content,BorderLayout.CENTER);
        JLabel caption=new JLabel(title,SwingConstants.CENTER);caption.setForeground(UiTokens.muted());caption.setFont(caption.getFont().deriveFont(Math.max(10f,caption.getFont().getSize2D()-1)));
        group.add(caption,BorderLayout.SOUTH);return group;
    }
    private static JComponent page(JComponent... groups){
        JPanel row=new JPanel(new FlowLayout(FlowLayout.LEADING,0,0));row.setOpaque(false);
        for(JComponent group:groups)row.add(group);
        JScrollPane scroll=new JScrollPane(row,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getHorizontalScrollBar().setUnitIncrement(UIScale.scale(32));
        scroll.setPreferredSize(new Dimension(UIScale.scale(650),UIScale.scale(104)));
        return scroll;
    }

    private record GlyphIcon(String label) implements Icon {
        @Override public int getIconWidth(){return UIScale.scale(25);}
        @Override public int getIconHeight(){return UIScale.scale(24);}
        @Override public void paintIcon(Component component,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();
            try{
                double scale=getIconWidth()/25.0;g.translate(x,y);g.scale(scale,scale);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.isEnabled()?accent():UiTokens.muted());g.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                if(label.contains("Texto")||label.contains("texto")){g.drawString("T",8,18);return;}
                if(label.contains("Elipse")||label.contains("Claro")||label.contains("Escuro")||label.contains("Azul")){g.drawOval(4,3,17,17);return;}
                if(label.contains("Áudio")){g.drawString("♫",4,19);return;}
                if(label.contains("Vídeo")||label.contains("Apresentar")){g.drawRect(3,4,19,15);g.drawLine(10,8,10,15);g.drawLine(10,8,16,11);g.drawLine(10,15,16,11);return;}
                if(label.contains("Imagem")){g.drawRect(3,4,19,15);g.drawOval(16,7,2,2);g.drawLine(5,16,10,10);g.drawLine(10,10,14,14);g.drawLine(14,14,17,11);g.drawLine(17,11,21,16);return;}
                g.drawRoundRect(3,3,19,16,2,2);
                if(label.contains("Novo")||label.contains("Adicionar")){g.drawLine(10,11,16,11);g.drawLine(13,8,13,14);}
                else if(label.contains("Duplicar")){g.drawRect(6,6,16,15);}
                else if(label.contains("Girar")){g.drawArc(7,5,13,13,30,280);}
                else if(label.contains("Salvar")){g.drawRect(8,5,9,5);g.drawRect(8,13,9,6);}
                else if(label.contains("Abrir")){g.drawLine(5,15,19,15);}
            }finally{g.dispose();}
        }
    }
    private static Color accent(){return UiTokens.isDarkTheme()?new Color(0x83B9FF):new Color(0x2876C7);}
}
