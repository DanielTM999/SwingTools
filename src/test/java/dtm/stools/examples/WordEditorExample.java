package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.api.WordSelection;
import dtm.stools.component.panels.editor.word.io.WordFileRecoveryStore;
import dtm.stools.component.panels.editor.word.io.pdf.WordPdfExportProvider;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.provider.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class WordEditorExample {
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            FlatLightLaf.setup();
            JFrame frame=new JFrame("SwingTools • Word");
            WordEditor editor=new WordEditor();
            editor.setErrorHandler(error->JOptionPane.showMessageDialog(frame,error.getMessage()==null?error.toString():error.getMessage(),"Documento",JOptionPane.ERROR_MESSAGE));
            editor.putClientProperty("word.openLinks",true);
            editor.setDocument(demoDocument());
            editor.setNavigationVisible(true);
            editor.addProvider(new WordPdfExportProvider());
            editor.addProvider(new SignatureBlock());
            editor.addProvider(new ExampleTools());
            Path recovery=Path.of(System.getProperty("java.io.tmpdir"),"swingtools-word-recovery");
            editor.enableRecovery(new WordFileRecoveryStore(recovery),Duration.ofSeconds(30),"exemplo");
            frame.add(editor,BorderLayout.CENTER);frame.setSize(1360,900);frame.setLocationRelativeTo(null);frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){editor.close();}});
            frame.setVisible(true);
        });
    }

    public static WordDocument demoDocument(){
        WordStyleSheet styles=WordStyleSheet.defaults();
        WordResource picture=picture();
        List<WordBlock> blocks=new ArrayList<>();
        blocks.add(styled(styles,"Title","Relatório de projeto SwingTools"));
        blocks.add(styled(styles,"Subtitle","Documento demonstrativo do WordEditor"));
        blocks.add(WordTableOfContents.create());
        blocks.add(styled(styles,"Heading1","Visão geral"));
        WordTextStyle normal=styles.resolveText(null);
        blocks.add(paragraph(styles,null,new WordRun("Este documento reúne ",normal),new WordRun("texto formatado",normal.withBold(true).withColor(0x2F5496)),new WordRun(", ",normal),
                new WordRun("itálico",normal.withItalic(true)),new WordRun(", ",normal),new WordRun("realce",normal.withHighlight(0xFFFF00)),new WordRun(", fórmulas como H",normal),
                new WordRun("2",normal.withVerticalAlign(WordTextStyle.VerticalAlign.SUBSCRIPT)),new WordRun("O e E = mc",normal),new WordRun("2",normal.withVerticalAlign(WordTextStyle.VerticalAlign.SUPERSCRIPT)),
                new WordRun(" e um ",normal),new WordRun("link para o site do Java",normal.withLink("https://dev.java").withColor(0x0563C1).withUnderline(true)),new WordRun(". Olá, ${nome}!",normal),
                new WordObjectRun(new WordNoteReference(WordIds.next(),"f1"),normal)));
        WordListDefinition bullets=WordListDefinition.bullets("1"),numbers=WordListDefinition.numbered("2");
        blocks.add(listItem(styles,"Estilos nomeados e herdados",bullets,0));
        blocks.add(listItem(styles,"Listas com níveis",bullets,0));
        blocks.add(listItem(styles,"Subnível com marcador próprio",bullets,1));
        blocks.add(listItem(styles,"Planejar a entrega",numbers,0));
        blocks.add(listItem(styles,"Validar com o documento de exemplo",numbers,0));
        blocks.add(styled(styles,"Heading2","Imagem incorporada"));
        blocks.add(paragraph(styles,null,new WordRun("Clique na imagem para selecioná-la e arraste as alças para redimensionar: ",normal),
                new WordObjectRun(WordImage.of(picture.id(),220,124).withAltText("Paisagem ilustrativa gerada pelo exemplo"),normal)));
        blocks.add(new WordParagraph(UUID.randomUUID(),List.of(new WordRun("Figura ",styles.resolveText("Caption")),new WordObjectRun(WordField.of(WordField.Kind.SEQ,"Figura"),styles.resolveText("Caption")),
                new WordRun(": paisagem incorporada ao documento",styles.resolveText("Caption"))),styles.resolveParagraph("Caption"),List.of("FiguraPaisagem"),null));
        blocks.add(styled(styles,"Heading2","Tabela editável"));
        blocks.add(table(styles));
        blocks.add(paragraph(styles,null,new WordRun("Use Tab para mudar de célula e arraste as bordas verticais para ajustar as colunas. A ",normal),
                new WordObjectRun(WordField.of(WordField.Kind.REF,"FiguraPaisagem"),normal),new WordRun(" aparece acima.",normal)));
        blocks.add(styled(styles,"Heading2","Gráfico com dados editáveis"));
        blocks.add(paragraph(styles,null,new WordObjectRun(WordChart.sample().resize(400,230).withAltText("Vendas por trimestre"),normal)));
        blocks.add(paragraph(styles,null,new WordRun("Clique duas vezes no gráfico para editar séries, categorias, cores, legenda e eixos.",normal)));
        blocks.add(styled(styles,"Heading1","Revisão").withStyle(styles.resolveParagraph("Heading1").withPageBreakBefore(true)));
        Instant now=Instant.now().truncatedTo(ChronoUnit.MINUTES);
        blocks.add(paragraph(styles,null,new WordRun("Este trecho possui um ",normal),new WordRun("comentário com resposta",normal.withComment("c1")),new WordRun(" e alterações rastreadas: ",normal),
                new WordRun("texto removido",normal.withRevision(new WordRevision(WordRevision.Type.DELETE,"Ana",now))),new WordRun(" ",normal),
                new WordRun("texto inserido",normal.withRevision(new WordRevision(WordRevision.Type.INSERT,"Ana",now))),new WordRun(".",normal)));
        blocks.add(styled(styles,"Heading1","Diagramas, formas e equações"));
        WordShape box=WordShape.of(WordShapeType.ROUNDED_RECTANGLE,150,60).withText("Forma flutuante com texto",10,0xffffff).withPlacement(WordPlacement.floating(300,0,WordPlacement.Wrap.SQUARE));
        blocks.add(paragraph(styles,null,new WordObjectRun(box,normal),new WordRun("As formas podem ficar em linha ou flutuar sobre a página com contorno do texto. Este parágrafo contorna a forma à direita, "
                +"demonstrando o posicionamento flutuante. Selecione a forma e arraste-a para outra posição, ou altere a disposição na aba Forma.",normal)));
        blocks.add(paragraph(styles,null,new WordObjectRun(WordDiagram.of(WordDiagramLayout.BASIC_PROCESS,List.of("Planejar","Executar","Verificar","Agir")).resize(420,120),normal)));
        blocks.add(new WordParagraph(UUID.randomUUID(),List.of(new WordObjectRun(WordEquation.parse("x = (-b ± √(b^2-4a c))/(2a)",true),normal)),styles.resolveParagraph(null).withAlignment(WordParagraphStyle.Alignment.CENTER)));
        blocks.add(styled(styles,"Heading2","Formulário"));
        blocks.add(paragraph(styles,null,new WordRun("Nome: ",normal),new WordObjectRun(WordFormField.text("Nome"),normal),new WordRun("   Aceito os termos ",normal),
                new WordObjectRun(WordFormField.checkbox("Termos",false),normal),new WordRun("   Setor: ",normal),new WordObjectRun(WordFormField.dropdown("Setor",List.of("Vendas","Suporte","Engenharia")),normal)));
        blocks.add(paragraph(styles,null,new WordRun("Fim do documento demonstrativo.",normal)));
        WordNote note=new WordNote("f1",WordNote.Kind.FOOTNOTE,List.of(new WordParagraph(UUID.randomUUID(),List.of(new WordRun("Nota de rodapé do exemplo, posicionada no fim da página.",styles.resolveText("FootnoteText"))),styles.resolveParagraph("FootnoteText"))));
        WordComment comment=new WordComment("c1","Ana",null,now,"Revisar a redação deste trecho.",null,false);
        WordComment reply=new WordComment("c2","Bruno",null,now,"Ajustado na próxima versão.","c1",false);
        WordTextStyle headerStyle=styles.resolveText("Header").withSize(9).withColor(0x595959);
        WordHeaders headers=WordHeaders.EMPTY
                .with(WordHeaders.Kind.HEADER,List.of(new WordParagraph(UUID.randomUUID(),List.of(new WordRun("SwingTools • Documento demonstrativo",headerStyle)),styles.resolveParagraph("Header").withAlignment(WordParagraphStyle.Alignment.RIGHT))))
                .with(WordHeaders.Kind.FOOTER,List.of(new WordParagraph(UUID.randomUUID(),List.of(new WordRun("Página ",headerStyle),new WordObjectRun(WordField.of(WordField.Kind.PAGE,""),headerStyle),
                        new WordRun(" de ",headerStyle),new WordObjectRun(WordField.of(WordField.Kind.NUM_PAGES,""),headerStyle)),styles.resolveParagraph("Footer").withAlignment(WordParagraphStyle.Alignment.CENTER))));
        WordParts parts=WordParts.EMPTY.withResources(WordResources.EMPTY.with(picture)).withNumbering(WordNumbering.of(List.of(bullets,numbers)))
                .withNote(note).withComment(comment).withComment(reply).withHeaders(headers);
        return new WordDocument(blocks,WordPageSettings.A4,parts);
    }
    private static WordParagraph styled(WordStyleSheet styles,String id,String text){return new WordParagraph(UUID.randomUUID(),List.of(new WordRun(text,styles.resolveText(id))),styles.resolveParagraph(id));}
    private static WordParagraph paragraph(WordStyleSheet styles,String id,WordInline... runs){return new WordParagraph(UUID.randomUUID(),List.of(runs),styles.resolveParagraph(id));}
    private static WordParagraph listItem(WordStyleSheet styles,String text,WordListDefinition list,int level){
        WordListLevel l=list.level(level);
        return new WordParagraph(UUID.randomUUID(),List.of(new WordRun(text,styles.resolveText("ListParagraph"))),styles.resolveParagraph("ListParagraph").withList(new WordListRef(list.id(),level)).withIndents(l.indent(),0,-l.hanging()).withSpacing(0,2,1.15f));
    }
    private static WordTable table(WordStyleSheet styles){
        String[][] values={{"Recurso","Situação","Responsável"},{"Tabelas","Concluído","Equipe A"},{"Imagens","Concluído","Equipe B"},{"Gráficos","Em revisão","Equipe C"}};
        WordTextStyle text=styles.resolveText(null),head=text.withBold(true).withColor(0xFFFFFF);
        List<WordTableRow> rows=new ArrayList<>();
        for(int r=0;r<values.length;r++){
            List<WordTableCell> cells=new ArrayList<>();
            for(String value:values[r]){
                WordTableCell cell=WordTableCell.of(List.of(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(value,r==0?head:text)),WordParagraphStyle.DEFAULT.withSpacing(0,0,1))));
                cells.add(r==0?cell.withFill(0x2F5496):r%2==0?cell.withFill(0xDEEAF6):cell);
            }
            rows.add(WordTableRow.of(cells).withHeader(r==0));
        }
        return new WordTable(UUID.randomUUID(),rows,List.of(150f,150f,150f),WordBorder.DEFAULT,WordTable.Alignment.LEFT,5.4f,"TableGrid",List.of());
    }
    private static WordResource picture(){
        BufferedImage image=new BufferedImage(440,248,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0,0,new Color(0x7FB3E6),0,248,new Color(0xFCE7C8)));g.fillRect(0,0,440,248);
            g.setColor(new Color(0xFFD166));g.fill(new Ellipse2D.Double(320,40,60,60));
            g.setColor(new Color(0x3A6B47));Path2D hills=new Path2D.Double();hills.moveTo(0,248);hills.curveTo(90,120,170,210,250,150);hills.curveTo(320,100,390,180,440,140);hills.lineTo(440,248);hills.closePath();g.fill(hills);
            g.setColor(new Color(0x2B5236));Path2D front=new Path2D.Double();front.moveTo(0,248);front.curveTo(120,190,200,240,300,200);front.curveTo(360,180,410,220,440,210);front.lineTo(440,248);front.closePath();g.fill(front);
        }finally{g.dispose();}
        try{ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return WordResource.of(out.toByteArray(),"image/png");}
        catch(IOException e){throw new IllegalStateException(e);}
    }

    public static final class SignatureBlock implements WordBlockProvider {
        public String id(){return "example.signature";}
        public String objectType(){return "example.signature";}
        public String displayName(){return "assinatura";}
        public WordCustomObject createObject(WordEditor editor){return WordCustomObject.of("example.signature",Map.of("name",System.getProperty("user.name","Responsável"),"role","Aprovação"),220,70).withAltText("Bloco de assinatura");}
        public void paint(Graphics2D g,WordCustomObject object,Rectangle2D.Float b){
            g.setColor(new Color(0xF8FAFC));g.fill(b);g.setColor(new Color(0x94A3B8));g.draw(b);
            g.setColor(new Color(0x334155));g.draw(new Line2D.Float(b.x+12,b.y+b.height-26,b.x+b.width-12,b.y+b.height-26));
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));g.drawString(object.data().getOrDefault("name",""),b.x+12,b.y+b.height-12);
            g.setFont(new Font(Font.SANS_SERIF,Font.ITALIC,9));g.drawString(object.data().getOrDefault("role",""),b.x+12,b.y+14);
        }
        public Optional<WordCustomObject> edit(Component owner,WordCustomObject object){
            String name=JOptionPane.showInputDialog(owner,"Nome do signatário",object.data().get("name"));
            return name==null?Optional.empty():Optional.of(object.withData(Map.of("name",name,"role",object.data().getOrDefault("role",""))));
        }
    }

    private static final class ExampleTools implements WordCommandProvider,WordToolbarContributor {
        private WordEditor editor;
        private boolean customPopups,customFonts;
        public String id(){return "example.tools";}
        public Map<String,Action> commands(WordEditor editor){
            this.editor=editor;
            return Map.of("example.fill",new AbstractAction("Preencher ${nome}"){public void actionPerformed(ActionEvent e){editor.run(()->editor.fillTemplate(Map.of("nome","Ana")));}});
        }
        public JComponent createToolbar(WordEditor editor){
            JToolBar bar=new JToolBar();bar.setFloatable(false);
            bar.add(button("Inserir imagem",()->editor.getCommands().get("word.insert.image").actionPerformed(null)));
            bar.add(button("Inserir tabela 3×3",()->editor.getObjects().insertTable(3,3)));
            bar.add(button("Inserir gráfico",()->editor.getObjects().insertChart(WordChartType.LINE_MARKERS)));
            bar.add(button("Propriedades do objeto",editor::showObjectProperties));
            bar.addSeparator();
            bar.add(button("Localizar/substituir",editor::showSearch));
            bar.add(button("Paleta",editor::showCommandPalette));
            JToggleButton popups=new JToggleButton("Popups personalizados");
            popups.addActionListener(e->editor.run(()->{
                customPopups=popups.isSelected();
                if(customPopups){editor.setSearchPopupProvider(new CompactSearch());editor.setCommandPaletteProvider(new MenuPalette());}
                else editor.resetPopupProviders();
            }));
            bar.add(popups);
            JToggleButton fonts=new JToggleButton("Fontes: sistema");
            fonts.addActionListener(e->editor.run(()->{
                customFonts=fonts.isSelected();
                if(customFonts){editor.setAvailableFonts(List.of("Arial","Verdana","Georgia","Courier New","Arial"));fonts.setText("Fontes: lista personalizada");}
                else{editor.resetAvailableFonts();fonts.setText("Fontes: sistema");}
            }));
            bar.add(fonts);
            bar.addSeparator();
            bar.add(button("Salvar",()->editor.getCommands().get("word.save").actionPerformed(null)));
            bar.add(button("Reabrir",()->editor.getCurrentFile().ifPresentOrElse(p->editor.open(p,true),()->{throw new IllegalStateException("Salve o documento antes de reabrir");})));
            bar.add(button("Exportar PDF",()->editor.getCommands().get("word.export.pdf").actionPerformed(null)));
            bar.add(button("Exportar HTML",()->editor.getCommands().get("word.export.html").actionPerformed(null)));
            bar.add(button("Assinatura",()->editor.getCommands().get("word.insert.example.signature").actionPerformed(null)));
            JPanel tools=new JPanel(new FlowLayout(FlowLayout.TRAILING,4,2));
            JButton demo=new JButton("Demonstração ▾");
            demo.addActionListener(e->{JPopupMenu popup=new JPopupMenu();bar.setLayout(new GridLayout(0,2,8,8));
                bar.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));popup.add(bar);popup.show(demo,0,demo.getHeight());});
            tools.add(demo);return tools;
        }
        private JButton button(String text,Runnable action){JButton b=new JButton(text);b.setFocusable(false);b.addActionListener(e->editor.run(action));return b;}
    }

    private static final class CompactSearch implements WordSearchPopupProvider {
        public String id(){return "example.search";}
        public WordPopupHandle show(WordSearchContext context){
            Window owner=SwingUtilities.getWindowAncestor(context.owner());
            JDialog dialog=new JDialog(owner,"Busca compacta",Dialog.ModalityType.MODELESS);
            JTextField query=new JTextField(context.initialQuery(),18);JLabel count=new JLabel();
            JButton next=new JButton("→");next.addActionListener(e->{context.findNext(query.getText(),WordSearchOptions.DEFAULT);count.setText(context.count(query.getText(),WordSearchOptions.DEFAULT)+"");});
            JPanel p=new JPanel(new FlowLayout());p.add(new JLabel("Buscar:"));p.add(query);p.add(next);p.add(count);
            dialog.add(p);dialog.pack();dialog.setLocationRelativeTo(owner);
            dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){context.closed();}});
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dialog.setVisible(true);
            return new WordPopupHandle(){public boolean isOpen(){return dialog.isDisplayable();}public void toFront(){dialog.toFront();}public void close(){dialog.dispose();}};
        }
    }
    private static final class MenuPalette implements WordCommandPaletteProvider {
        public String id(){return "example.palette";}
        public WordPopupHandle show(WordCommandPaletteContext context){
            JPopupMenu menu=new JPopupMenu("Comandos");
            context.commands().stream().filter(WordCommandEntry::enabled).limit(40).forEach(c->{JMenuItem item=new JMenuItem(c.group()+" › "+c.name());item.addActionListener(e->context.execute(c.id()));menu.add(item);});
            menu.show(context.owner(),40,40);
            return new WordPopupHandle(){public boolean isOpen(){return menu.isVisible();}public void toFront(){}public void close(){menu.setVisible(false);}};
        }
    }
}
