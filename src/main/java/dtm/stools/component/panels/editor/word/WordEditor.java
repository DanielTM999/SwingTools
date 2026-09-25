package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.config.*;
import dtm.stools.component.panels.editor.word.controller.*;
import dtm.stools.component.panels.editor.word.editing.WordMailMerge;
import dtm.stools.component.panels.editor.word.editing.WordTemplates;
import dtm.stools.component.panels.editor.word.io.*;
import dtm.stools.component.panels.editor.word.layout.WordLayout;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.provider.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import dtm.stools.component.panels.editor.word.ui.*;
import dtm.stools.component.panels.editor.word.ui.popup.WordHeaderFooterPanel;
import dtm.stools.component.panels.editor.word.ui.popup.WordPageSetupPanel;
import dtm.stools.component.panels.editor.word.ui.popup.WordColors;


import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WordEditor extends BlockingPanel implements AutoCloseable {
    public enum ExportFormat { HTML, TEXT }
    private final WordSession session=new WordSession();
    private final WordServices services;
    private final WordCanvas canvas;
    private final JScrollPane scrollPane;
    private final JPanel north=new JPanel(new BorderLayout()),providerBar=new JPanel(new FlowLayout(FlowLayout.LEADING,4,0));
    private final JPanel ribbonHost=new JPanel(new BorderLayout());
    private final JMenuBar compactMenu=new JMenuBar();
    private final JLabel status=new JLabel(),diagnostics=new JLabel();
    private final DefaultListModel<String> outlineModel=new DefaultListModel<>();
    private final JList<String> outline=new JList<>(outlineModel);
    private final List<Integer> outlineOffsets=new ArrayList<>();
    private boolean updatingOutline;
    private final WordNavigationPanel navigation=new WordNavigationPanel(outline,this::closeNavigation);
    private final LinkedHashMap<String,Action> commands=new LinkedHashMap<>();
    private final Map<String,String> commandGroups=new HashMap<>();
    private final Map<String,Registration> providers=new LinkedHashMap<>();
    private final WordTaskRunner runner=new WordTaskRunner();
    private final WordPopupController popups=new WordPopupController();
    private final WordSearchController search;
    private final WordFileController files;
    private final WordObjectController objects;
    private final WordDocumentController documentTools;
    private final WordReviewController review;
    private final WordFontCatalog fonts=new WordFontCatalog();
    private final WordCommentsPanel commentsPanel;
    private final ProviderRegistration sessionListener;
    private final Runnable fontListener;
    private JComponent ribbon;
    private WordRibbon defaultRibbon;
    private WordEditorConfig config;
    private boolean closed,applyingRemote,commentsVisible,screenActive;
    private WordDocument lastDocument;
    private Consumer<Throwable> errorHandler=error->firePropertyChange("error",null,error);

    public WordEditor(){this(WordEditorConfig.defaults(),WordServices.defaults());}
    public WordEditor(WordEditorConfig config){this(config,WordServices.defaults());}
    public WordEditor(WordEditorConfig config,WordServices services){
        this.services=Objects.requireNonNull(services);this.config=Objects.requireNonNull(config);
        session.setErrorHandler(error->errorHandler.accept(error));
        runner.setErrorHandler(error->errorHandler.accept(error));
        search=new WordSearchController(session);
        files=new WordFileController(session,services,runner,this::getObjectRegistry);
        objects=new WordObjectController(session,this::getObjectRegistry);
        documentTools=new WordDocumentController(session);
        review=new WordReviewController(session);
        setLayout(new BorderLayout());
        canvas=services.uiFactory().createCanvas(session,services.layout(),services.renderer());
        canvas.setErrorHandler(error->errorHandler.accept(error));
        canvas.setObjectHandler(box->run(()->activateObject(box)));
        canvas.setLinkHandler(link->run(()->openLink(link)));
        canvas.setRegionHandler(region->run(()->editHeaderFooter(region==WordLayout.Region.FOOTER)));
        canvas.setPasteHandler(this::pasteSpecial);
        canvas.setTransferHandler(new DropHandler());
        scrollPane=new JScrollPane(canvas);scrollPane.setBorder(BorderFactory.createEmptyBorder());scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        add(scrollPane,BorderLayout.CENTER);
        commentsPanel=new WordCommentsPanel(review,this::ask,error->errorHandler.accept(error));commentsPanel.setVisible(false);commentsPanel.setCloseAction(()->{setCommentsVisible(false);canvas.requestFocusInWindow();});
        add(commentsPanel,BorderLayout.EAST);
        registerBuiltins();
        defaultRibbon=new WordRibbon(this);ribbon=defaultRibbon;
        JMenu viewMenu=new JMenu("Exibir");viewMenu.setMnemonic(KeyEvent.VK_E);viewMenu.setName("word.view.menu");
        JMenuItem showTools=new JMenuItem(commands.get("word.focus"));showTools.setName("word.tools.toggle");viewMenu.add(showTools);
        compactMenu.setName("word.compact.menu");compactMenu.add(viewMenu);
        ribbonHost.add(compactMenu,BorderLayout.NORTH);ribbonHost.add(ribbon,BorderLayout.CENTER);
        north.add(ribbonHost,BorderLayout.NORTH);north.add(providerBar,BorderLayout.CENTER);
        diagnostics.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));diagnostics.setVisible(false);north.add(diagnostics,BorderLayout.SOUTH);add(north,BorderLayout.NORTH);
        add(navigation,BorderLayout.WEST);
        outline.getAccessibleContext().setAccessibleName("Navegação por títulos");
        status.setOpaque(true);status.setBorder(BorderFactory.createEmptyBorder(7,16,7,16));add(status,BorderLayout.SOUTH);
        outline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outline.addListSelectionListener(e->{if(!updatingOutline&&!e.getValueIsAdjusting())navigateOutline(false);});
        outline.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                int index=outline.locationToIndex(e.getPoint());
                if(SwingUtilities.isLeftMouseButton(e)&&index>=0&&outline.getCellBounds(index,index).contains(e.getPoint()))navigateOutline(true);
            }
        });
        outline.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"word.outline.activate");
        outline.getActionMap().put("word.outline.activate",new AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){navigateOutline(true);}
        });
        lastDocument=session.getDocument();
        sessionListener=session.addListener(this::sessionChanged);
        fontListener=fonts.addListener(this::refreshState);
        files.addListener(()->{refreshDiagnostics();refreshState();});
        canvas.addPropertyChangeListener("layoutSnapshot",e->refreshState());
        canvas.bind("word.find",KeyStroke.getKeyStroke(KeyEvent.VK_F,InputEvent.CTRL_DOWN_MASK),this::showSearch);
        canvas.bind("word.replace",KeyStroke.getKeyStroke(KeyEvent.VK_H,InputEvent.CTRL_DOWN_MASK),this::showSearch);
        canvas.bind("word.commands",KeyStroke.getKeyStroke(KeyEvent.VK_P,InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK),this::showCommandPalette);
        canvas.bind("word.save",KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK),()->run(this::chooseSave));
        canvas.bind("word.link",KeyStroke.getKeyStroke(KeyEvent.VK_K,InputEvent.CTRL_DOWN_MASK),()->run(this::promptLink));
        canvas.bind("word.properties",KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.ALT_DOWN_MASK),this::showObjectProperties);
        canvas.addMouseListener(new MouseAdapter(){private void popup(MouseEvent e){if(e.isPopupTrigger())showContextMenu(e.getX(),e.getY());}@Override public void mousePressed(MouseEvent e){popup(e);}@Override public void mouseReleased(MouseEvent e){popup(e);}});
        applyConfig();refreshOutline();refreshState();onThemeChanged();
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0)updateScreenLifecycle();});
    }

    private void updateScreenLifecycle(){
        boolean showing=!closed&&isShowing();
        if(screenActive==showing)return;
        screenActive=showing;
        if(showing){canvas.resumeVisualWork();SwingUtilities.invokeLater(()->{if(screenActive&&isShowing())canvas.resumeVisualWork();});}
        else{defaultRibbon.closePopups();popups.dismissTransient();canvas.pauseVisualWork();}
    }
    @Override public void addNotify(){super.addNotify();updateScreenLifecycle();}
    @Override public void removeNotify(){if(screenActive){screenActive=false;defaultRibbon.closePopups();popups.dismissTransient();canvas.pauseVisualWork();}super.removeNotify();}

    public WordSession getSession(){return session;}
    public WordDocument getDocument(){return session.getDocument();}
    public WordCanvas getCanvas(){return canvas;}
    public JScrollPane getScrollPane(){return scrollPane;}
    public WordEditorConfig getConfig(){return config;}
    public WordRibbon getDefaultRibbon(){return defaultRibbon;}
    public WordObjectController getObjects(){return objects;}
    public WordDocumentController getDocumentTools(){return documentTools;}
    public WordReviewController getReview(){return review;}
    public WordFileController getFiles(){return files;}
    public WordSearchController getSearch(){return search;}
    public WordPopupController getPopups(){return popups;}
    public WordCommentsPanel getCommentsPanel(){return commentsPanel;}
    public WordObjectRegistry getObjectRegistry(){return canvas.getRenderer().getObjectRegistry();}
    public List<String> getDiagnostics(){return files.origin().map(WordImportResult::diagnostics).orElse(List.of());}
    public Map<String,String> getFontSubstitutions(){
        Map<String,String> substitutions=new LinkedHashMap<>();
        for(var p:getDocument().paragraphs())for(var run:p.runs()){
            String requested=run.style().family(),actual=run.style().font().getFamily();
            if(!requested.equalsIgnoreCase(actual))substitutions.put(requested,actual);
        }
        return Collections.unmodifiableMap(substitutions);
    }
    public Optional<Path> getCurrentFile(){return files.currentFile();}
    public Map<String,Action> getCommands(){return Collections.unmodifiableMap(new LinkedHashMap<>(commands));}
    public boolean isDirty(){return session.isDirty();}
    public boolean isReadOnly(){return session.isReadOnly();}
    public boolean isClosed(){return closed;}
    public void setErrorHandler(Consumer<Throwable> handler){errorHandler=Objects.requireNonNull(handler);}
    public void run(Runnable action){try{ensureOpen();action.run();}catch(RuntimeException error){errorHandler.accept(error);}}

    public void setConfig(WordEditorConfig value){ensureOpen();config=Objects.requireNonNull(value);applyConfig();}
    public void setReadOnly(boolean value){configure(value,config.ribbonVisible(),config.navigationVisible(),config.statusVisible(),config.zoom(),config.viewMode());}
    public void setRibbonVisible(boolean value){configure(config.readOnly(),value,config.navigationVisible(),config.statusVisible(),config.zoom(),config.viewMode());}
    private void closeNavigation(){setNavigationVisible(false);canvas.requestFocusInWindow();}
    public void setNavigationVisible(boolean value){configure(config.readOnly(),config.ribbonVisible(),value,config.statusVisible(),config.zoom(),config.viewMode());}
    public void setStatusVisible(boolean value){configure(config.readOnly(),config.ribbonVisible(),config.navigationVisible(),value,config.zoom(),config.viewMode());}
    public void setZoom(double value){configure(config.readOnly(),config.ribbonVisible(),config.navigationVisible(),config.statusVisible(),Math.max(.25,Math.min(4,value)),config.viewMode());}
    public void setViewMode(WordViewMode value){configure(config.readOnly(),config.ribbonVisible(),config.navigationVisible(),config.statusVisible(),config.zoom(),value);}
    public void setCommentsVisible(boolean value){commentsVisible=value;commentsPanel.setVisible(value);if(value)commentsPanel.refresh();revalidate();refreshState();}
    private void configure(boolean readOnly,boolean ribbonVisible,boolean navigationVisible,boolean statusVisible,double zoom,WordViewMode mode){
        setConfig(new WordEditorConfig(readOnly,ribbonVisible,navigationVisible,statusVisible,zoom,mode,config.locale(),config.historyLimit()));
    }
    private void applyConfig(){
        session.setReadOnly(config.readOnly()||files.origin().map(o->!o.isEditable()).orElse(false));session.setHistoryLimit(config.historyLimit());
        ribbon.setVisible(config.ribbonVisible());providerBar.setVisible(config.ribbonVisible());navigation.setVisible(config.navigationVisible());status.setVisible(config.statusVisible());
        compactMenu.setVisible(!config.ribbonVisible());
        canvas.setZoom(config.zoom());canvas.setViewMode(config.viewMode());commentsPanel.setReadOnly(session.isReadOnly());revalidate();repaint();refreshState();
    }
    public void setRibbon(JComponent value){ensureOpen();Objects.requireNonNull(value);ribbonHost.remove(ribbon);ribbon=value;ribbonHost.add(ribbon,BorderLayout.CENTER);ribbon.setVisible(config.ribbonVisible());revalidate();}

    public void setDocument(WordDocument document){
        ensureOpen();files.replaced();diagnostics.setVisible(false);
        session.load(document);lastDocument=document;applyConfig();
    }
    public void setText(String text){setDocument(WordDocument.fromText(text));}
    public String getText(){return getDocument().text();}
    public void insertText(String text){ensureOpen();session.replaceSelection(text);}
    public void fillTemplate(Map<String,String> values){ensureOpen();documentTools.fillTemplate(values);}

    public void setAvailableFonts(List<String> families){fonts.setAvailable(families);}
    public List<String> getAvailableFonts(){return fonts.available();}
    public void resetAvailableFonts(){fonts.reset();}
    public void refreshSystemFonts(){fonts.refreshSystem();}
    public boolean isCustomFontList(){return fonts.isCustom();}
    public void applyFontFamily(String family){
        run(()->{if(!fonts.allows(family))throw new IllegalArgumentException("Fonte fora da lista permitida: "+family);session.formatSelection(s->s.withFamily(family));canvas.requestFocusInWindow();});
    }
    public void applyFontSize(float size){run(()->{if(session.getInsertionStyle().size()!=size)session.formatSelection(s->s.withSize(size));});}
    public void applyStyle(String styleId){run(()->{documentTools.applyStyle(styleId);canvas.requestFocusInWindow();});}

    public void setSearchPopupProvider(WordSearchPopupProvider provider){ensureOpen();popups.setSearch(provider);}
    public void setCommandPaletteProvider(WordCommandPaletteProvider provider){ensureOpen();popups.setPalette(provider);}
    public void setDialogProvider(WordDialogProvider provider){ensureOpen();popups.setDialog(provider);}
    public WordDialogProvider getDialogProvider(){return popups.dialogProvider();}
    public void setFileDialogProvider(WordFileDialogProvider provider){ensureOpen();popups.setFiles(provider);}
    public void setConfirmationProvider(WordConfirmationProvider provider){ensureOpen();popups.setConfirmation(provider);}
    public void setObjectPropertiesProvider(WordObjectPropertiesProvider provider){ensureOpen();popups.setProperties(provider);}
    public void resetPopupProviders(){popups.reset();}
    public WordSearchPopupProvider getSearchPopupProvider(){return popups.searchProvider();}
    public WordCommandPaletteProvider getCommandPaletteProvider(){return popups.paletteProvider();}
    public WordFileDialogProvider getFileDialogProvider(){return popups.fileProvider();}
    public WordConfirmationProvider getConfirmationProvider(){return popups.confirmationProvider();}
    public WordObjectPropertiesProvider getObjectPropertiesProvider(){return popups.propertiesProvider();}

    public WordTask<WordSuggestion> requestSuggestion(String providerId,String instruction,boolean entireDocument){
        ensureOpen();Registration registration=providers.get(providerId);
        if(registration==null||!(registration.provider instanceof WordAiProvider ai))throw new IllegalArgumentException("AI provider not registered: "+providerId);
        if(instruction==null||instruction.isBlank())throw new IllegalArgumentException("Instruction is required");
        WordSelection range=entireDocument?new WordSelection(0,getDocument().length()):session.getSelection();
        String original=getText().substring(range.start(),range.end());long revision=session.getRevision();
        WordTask<WordSuggestion> task=runner.task();WordAiProvider.Request request=new WordAiProvider.Request(instruction,original,config.locale(),task::isCancelled);
        task.attach(runner.submit(()->{
            try{
                task.progress(10);CompletionStage<String> response=Objects.requireNonNull(ai.suggest(request));
                response.whenComplete((replacement,error)->{
                    if(error!=null){runner.fail(task,error);return;}
                    if(replacement==null||replacement.length()>1_000_000){runner.fail(task,new IllegalArgumentException("Invalid AI response"));return;}
                    SwingUtilities.invokeLater(()->task.commit(()->{ensureOpen();if(session.getRevision()!=revision||providers.get(providerId)!=registration)throw new IllegalStateException("Stale AI response");return new WordSuggestion(revision,range,original,replacement);}));
                });
            }catch(Throwable error){runner.fail(task,error);}
        }));return task;
    }
    public void applySuggestion(WordSuggestion suggestion){
        ensureOpen();Objects.requireNonNull(suggestion);
        if(session.getRevision()!=suggestion.revision()||suggestion.range().end()>getDocument().length()
                ||!getText().substring(suggestion.range().start(),suggestion.range().end()).equals(suggestion.original()))throw new IllegalStateException("Suggestion no longer matches the document");
        int start=suggestion.range().start(),end=suggestion.range().end();String replacement=WordDocument.normalize(suggestion.replacement());
        session.execute("Aplicar sugestão",d->d.replace(start,end,replacement,d.styleAt(start)),new WordSelection(start+replacement.length(),start+replacement.length()));
    }
    public void applyRemoteDocument(WordDocument document,String label){
        ensureOpen();Objects.requireNonNull(document);
        applyingRemote=true;
        try{session.execute(label==null?"Alteração remota":label,d->document,new WordSelection(Math.min(session.getSelection().start(),document.length()),Math.min(session.getSelection().start(),document.length())));}
        catch(IllegalArgumentException e){session.execute(label==null?"Alteração remota":label,d->document,new WordSelection(0,0));}
        finally{applyingRemote=false;}
    }

    public ProviderRegistration addProvider(WordProvider provider){
        ensureOpen();Objects.requireNonNull(provider);String id=Objects.requireNonNull(provider.id());
        if(id.isBlank()||providers.containsKey(id))throw new IllegalArgumentException("Duplicate or empty provider ID: "+id);
        Map<String,Action> contributions=new LinkedHashMap<>(provider instanceof WordCommandProvider p?Map.copyOf(p.commands(this)):Map.of());
        List<ProviderRegistration> cleanups=new ArrayList<>();
        if(provider instanceof WordBlockProvider block){
            String type=block.objectType();
            contributions.put("word.insert."+type,new AbstractAction("Inserir "+block.displayName()){@Override public void actionPerformed(ActionEvent e){run(()->{if(session.isReadOnly())return;WordCustomObject object=block.createObject(WordEditor.this);objects.insert(object);});}});
        }
        for(String key:contributions.keySet())if(key.isBlank()||commands.containsKey(key))throw new IllegalArgumentException("Duplicate command: "+key);
        if(popups.handles(provider))cleanups.add(popups.register(provider));
        try{
            if(provider instanceof WordBlockProvider block)cleanups.add(getObjectRegistry().register(block.objectType(),(g,o,b,d)->block.paint(g,(WordCustomObject)o,b)));
        }catch(RuntimeException e){cleanups.forEach(ProviderRegistration::close);throw e;}
        JComponent toolbar=provider instanceof WordToolbarContributor p?p.createToolbar(this):null;
        ProviderRegistration cleanup=Objects.requireNonNull(provider.attach(this));
        Registration registration=new Registration(provider,cleanup,contributions,toolbar,cleanups);providers.put(id,registration);
        contributions.forEach((k,v)->{commands.put(k,v);commandGroups.put(k,"Extensões");});
        rebuildProviderBar();refreshState();canvas.repaint();return registration;
    }
    public Optional<WordProvider> getProvider(String id){return Optional.ofNullable(providers.get(id)).map(r->r.provider);}
    private void rebuildProviderBar(){providerBar.removeAll();providers.values().stream().sorted(Comparator.comparingInt((Registration r)->r.provider.priority()).reversed().thenComparing(r->r.provider.id())).forEach(r->{if(r.toolbar!=null)providerBar.add(r.toolbar);});providerBar.revalidate();providerBar.repaint();}
    private final class Registration implements ProviderRegistration {
        final WordProvider provider;final ProviderRegistration cleanup;final Map<String,Action> contributions;final JComponent toolbar;final List<ProviderRegistration> internal;boolean removed;
        Registration(WordProvider p,ProviderRegistration c,Map<String,Action> a,JComponent t,List<ProviderRegistration> internal){provider=p;cleanup=c;contributions=a;toolbar=t;this.internal=internal;}
        public void close(){
            if(removed)return;removed=true;providers.remove(provider.id(),this);contributions.forEach((key,value)->commands.remove(key,value));
            try{for(ProviderRegistration r:internal)r.close();cleanup.close();}finally{rebuildProviderBar();refreshState();canvas.repaint();}
        }
    }

    private void action(String id,String name,String group,boolean edit,Runnable runnable){
        Action a=new AbstractAction(name){@Override public void actionPerformed(ActionEvent e){if(!isEnabled()||edit&&session.isReadOnly())return;try{ensureOpen();runnable.run();canvas.requestFocusInWindow();}catch(Exception error){errorHandler.accept(error);}}};
        a.putValue("word.edit",edit);commands.put(id,a);commandGroups.put(id,group);
    }
    private void registerBuiltins(){
        String f="Arquivo",h="Início",i="Inserir",l="Layout",r="Referências",v="Revisão",x="Exibir",t="Tabela",o="Objeto";
        action("word.new","Novo",f,false,()->{if(confirmDiscard())setDocument(WordDocument.empty());});
        action("word.open","Abrir…",f,false,this::chooseOpen);action("word.save","Salvar",f,false,this::chooseSave);action("word.saveAs","Salvar como…",f,false,this::chooseSaveAs);
        action("word.export.html","Exportar HTML…",f,false,()->chooseExport(ExportFormat.HTML));
        action("word.export.text","Exportar texto…",f,false,()->chooseExport(ExportFormat.TEXT));
        action("word.export.pdf","Exportar PDF…",f,false,this::choosePdf);
        action("word.print","Imprimir…",f,false,this::print);
        action("word.versions","Versões…",f,true,this::chooseVersion);
        action("word.recover","Recuperar…",f,false,this::recoverNow);
        action("word.template","Preencher variáveis…",f,true,this::promptTemplate);
        action("word.mailmerge","Mala direta (CSV)…",f,false,this::mailMerge);
        action("word.undo","Desfazer",h,true,session::undo);action("word.redo","Refazer",h,true,session::redo);
        action("word.copy","Copiar",h,false,canvas::copy);action("word.cut","Recortar",h,true,canvas::cut);action("word.paste","Colar",h,true,canvas::paste);
        action("word.bold","N",h,true,()->session.formatSelection(s->s.withBold(!s.bold())));
        action("word.italic","I",h,true,()->session.formatSelection(s->s.withItalic(!s.italic())));
        action("word.underline","S",h,true,()->session.formatSelection(s->s.withUnderline(!s.underline())));
        action("word.strike","abc",h,true,()->session.formatSelection(s->s.withStrike(!s.strike())));
        action("word.superscript","x²",h,true,()->session.formatSelection(s->s.withVerticalAlign(s.verticalAlign()==WordTextStyle.VerticalAlign.SUPERSCRIPT?WordTextStyle.VerticalAlign.BASELINE:WordTextStyle.VerticalAlign.SUPERSCRIPT)));
        action("word.subscript","x₂",h,true,()->session.formatSelection(s->s.withVerticalAlign(s.verticalAlign()==WordTextStyle.VerticalAlign.SUBSCRIPT?WordTextStyle.VerticalAlign.BASELINE:WordTextStyle.VerticalAlign.SUBSCRIPT)));
        action("word.color","Cor",h,true,()->{WordColors.show(this,"Cor do texto",new Color(session.getInsertionStyle().color()),c->run(()->session.formatSelection(s->s.withColor(c.getRGB()&0xffffff))),null);});
        action("word.highlight","Realce",h,true,()->WordColors.show(this,"Realce",Color.YELLOW,c->run(()->session.formatSelection(s->s.withHighlight(c.getRGB()&0xffffff))),()->run(()->session.formatSelection(s->s.withHighlight(null)))));
        action("word.clearFormat","Limpar formatação",h,true,()->{WordTextStyle base=getDocument().styles().resolveText(getDocument().paragraphAt(session.getSelection().start()).style().styleId());session.formatSelection(s->base.withLink(s.link()).withRevision(s.revision()).withComments(s.comments()));});
        action("word.bullets","• Marcadores",h,true,()->documentTools.toggleList(false));
        action("word.numbering","1. Numeração",h,true,()->documentTools.toggleList(true));
        action("word.indent.more","Aumentar recuo",h,true,()->documentTools.indent(1));
        action("word.indent.less","Diminuir recuo",h,true,()->documentTools.indent(-1));
        action("word.list.restart","Reiniciar em 1",h,true,()->documentTools.restartNumbering(1));
        action("word.list.continue","Continuar numeração",h,true,documentTools::continueNumbering);
        for(var alignment:WordParagraphStyle.Alignment.values())action("word.align."+alignment.name(),switch(alignment){case LEFT->"Esquerda";case CENTER->"Centro";case RIGHT->"Direita";case JUSTIFY->"Justificar";},h,true,()->session.formatParagraphs(s->s.withAlignment(alignment)));
        for(float spacing:new float[]{1f,1.15f,1.5f,2f})action("word.spacing."+(spacing==1.15f?"115":spacing==1.5f?"15":Integer.toString((int)spacing)),"Espaçamento "+spacing,h,true,()->session.formatParagraphs(s->s.withSpacing(s.before(),s.after(),spacing)));
        action("word.shading","Sombreamento",h,true,()->{WordColors.show(this,"Sombreamento do parágrafo",Color.WHITE,c->run(()->session.formatParagraphs(s->s.withShading(c.getRGB()&0xffffff))),()->run(()->session.formatParagraphs(s->s.withShading(null))));});
        action("word.style.update","Atualizar estilo",h,true,()->documentTools.updateStyleFromSelection(Optional.ofNullable(getDocument().paragraphAt(session.getSelection().start()).style().styleId()).orElse(WordStyleSheet.NORMAL)));
        action("word.style.new","Novo estilo…",h,true,()->{String name=ask("Novo estilo","Nome do estilo");if(name!=null)documentTools.createStyle(name);});
        action("word.find","Localizar",h,false,this::showSearch);action("word.palette","Comandos",h,false,this::showCommandPalette);
        action("word.insert.table","Tabela…",i,true,()->{String size=ask("Inserir tabela","Linhas x colunas (ex.: 3x4)");if(size!=null){String[] p=size.toLowerCase(Locale.ROOT).split("[x×*, ]+");objects.insertTable(Integer.parseInt(p[0].strip()),Integer.parseInt(p[1].strip()));}});
        action("word.insert.image","Imagem…",i,true,this::chooseImage);
        action("word.insert.chart","Gráfico",i,true,()->objects.insertChart(WordChartType.COLUMN_CLUSTERED));
        action("word.insert.textbox","Caixa de texto",i,true,()->objects.insertShape(WordShapeType.TEXT_BOX));
        action("word.insert.equation","Equação",i,true,()->{objects.insertEquation("x = (-b ± √(b^2-4a c))/(2a)",true);showObjectProperties();});
        action("word.insert.diagram","Diagrama",i,true,()->objects.insertDiagram(WordDiagramLayout.BASIC_PROCESS,"Planejar\nExecutar\nVerificar\nAgir"));
        action("word.insert.link","Link…",i,true,this::promptLink);
        action("word.link.remove","Remover link",i,true,objects::removeLink);
        action("word.insert.bookmark","Indicador…",i,true,()->{String name=ask("Indicador","Nome do indicador");if(name!=null)documentTools.addBookmark(name.strip());});
        action("word.insert.crossref","Referência cruzada…",i,true,this::promptCrossReference);
        action("word.insert.pagebreak","Quebra de página",i,true,documentTools::insertPageBreak);
        action("word.insert.columnbreak","Quebra de coluna",i,true,documentTools::insertColumnBreak);
        action("word.insert.sectionbreak","Quebra de seção",i,true,documentTools::insertSectionBreak);
        action("word.insert.field.page","Número de página",i,true,()->documentTools.insertField(WordField.Kind.PAGE,""));
        action("word.insert.field.date","Data",i,true,()->documentTools.insertField(WordField.Kind.DATE,"dd/MM/yyyy"));
        action("word.insert.form.text","Campo de texto",i,true,()->objects.insertFormField(WordFormField.Kind.TEXT,"Texto"));
        action("word.insert.form.checkbox","Caixa de seleção",i,true,()->objects.insertFormField(WordFormField.Kind.CHECKBOX,"Seleção"));
        action("word.insert.form.dropdown","Lista suspensa",i,true,()->objects.insertFormField(WordFormField.Kind.DROPDOWN,"Lista"));
        action("word.insert.form.date","Seletor de data",i,true,()->objects.insertFormField(WordFormField.Kind.DATE,"Data"));
        action("word.block.save","Salvar seleção como bloco…",i,false,()->{String name=ask("Bloco reutilizável","Nome do bloco");if(name!=null)documentTools.saveBuildingBlock(name);});
        action("word.page.setup","Configurar página…",l,true,this::pageSetup);
        action("word.page.portrait","Retrato",l,true,()->updatePage(s->s.withOrientation(false)));
        action("word.page.landscape","Paisagem",l,true,()->updatePage(s->s.withOrientation(true)));
        action("word.page.a4","A4",l,true,()->updatePage(s->sized(s,595.276f,841.89f)));
        action("word.page.letter","Carta",l,true,()->updatePage(s->sized(s,612,792)));
        action("word.page.legal","Ofício",l,true,()->updatePage(s->sized(s,612,1008)));
        action("word.page.a5","A5",l,true,()->updatePage(s->sized(s,419.528f,595.276f)));
        action("word.margins.normal","Margens normais",l,true,()->updatePage(s->s.withMargins(70.866f,70.866f,70.866f,70.866f)));
        action("word.margins.narrow","Margens estreitas",l,true,()->updatePage(s->s.withMargins(36,36,36,36)));
        action("word.margins.wide","Margens largas",l,true,()->updatePage(s->s.withMargins(72,144,72,144)));
        for(int c=1;c<=3;c++){int n=c;action("word.page.columns."+c,c+(c==1?" coluna":" colunas"),l,true,()->updatePage(s->s.withColumns(n,s.columnSpacing())));}
        action("word.header","Cabeçalho…",l,true,()->editHeaderFooter(false));
        action("word.footer","Rodapé…",l,true,()->editHeaderFooter(true));
        action("word.toc","Sumário",r,true,documentTools::insertTableOfContents);
        action("word.footnote","Nota de rodapé…",r,true,()->{String text=ask("Nota de rodapé","Texto da nota");if(text!=null)documentTools.insertFootnote(text,WordNote.Kind.FOOTNOTE);});
        action("word.endnote","Nota de fim…",r,true,()->{String text=ask("Nota de fim","Texto da nota");if(text!=null)documentTools.insertFootnote(text,WordNote.Kind.ENDNOTE);});
        action("word.caption","Legenda…",r,true,()->{String text=ask("Inserir legenda","Descrição (rótulo: Figura)");if(text!=null)documentTools.insertCaption(session.getDocument().tableAt(session.getSelection().caret()).isPresent()?"Tabela":"Figura",text);});
        action("word.comment.new","Novo comentário…",v,true,()->{String text=ask("Novo comentário","Comentário");if(text!=null){review.addComment(text);setCommentsVisible(true);}});
        action("word.comments.panel","Painel de comentários",v,false,()->setCommentsVisible(!commentsVisible));
        action("word.track","Controlar alterações",v,true,()->review.setTrackChanges(!session.isTrackChanges()));
        action("word.author","Autor…",v,false,()->{String name=ask("Autor das revisões",session.getAuthor());if(name!=null&&!name.isBlank())session.setAuthor(name);});
        action("word.change.accept","Aceitar",v,true,review::acceptAtSelection);action("word.change.reject","Rejeitar",v,true,review::rejectAtSelection);
        action("word.change.acceptAll","Aceitar todas",v,true,review::acceptAll);action("word.change.rejectAll","Rejeitar todas",v,true,review::rejectAll);
        action("word.change.next","Próxima alteração",v,false,review::nextChange);
        action("word.compare","Comparar com arquivo…",v,false,this::compareWithFile);
        action("word.navigation","Navegação",x,false,()->setNavigationVisible(!config.navigationVisible()));
        action("word.focus","Ocultar ferramentas",x,false,()->setRibbonVisible(!config.ribbonVisible()));
        action("word.view.pages","Layout de impressão",x,false,()->setViewMode(WordViewMode.PRINT_LAYOUT));
        action("word.view.continuous","Contínuo",x,false,()->setViewMode(WordViewMode.CONTINUOUS));
        action("word.zoom.in","Zoom +",x,false,()->setZoom(config.zoom()+0.1));action("word.zoom.out","Zoom −",x,false,()->setZoom(config.zoom()-0.1));
        action("word.table.row.above","Linha acima",t,true,()->objects.insertRow(false));action("word.table.row.below","Linha abaixo",t,true,()->objects.insertRow(true));
        action("word.table.column.left","Coluna à esquerda",t,true,()->objects.insertColumn(false));action("word.table.column.right","Coluna à direita",t,true,()->objects.insertColumn(true));
        action("word.table.row.delete","Excluir linhas",t,true,objects::deleteRows);action("word.table.column.delete","Excluir colunas",t,true,objects::deleteColumns);
        action("word.table.delete","Excluir tabela",t,true,objects::deleteTable);
        action("word.table.merge","Mesclar células",t,true,objects::mergeCells);action("word.table.split","Dividir célula",t,true,objects::splitCell);
        action("word.table.distribute","Distribuir colunas",t,true,objects::distributeColumns);
        action("word.table.header","Repetir cabeçalho",t,true,objects::toggleHeaderRow);
        action("word.table.fill","Preenchimento…",t,true,()->{WordColors.show(this,"Preenchimento das células",new Color(0xDEEAF6),c->run(()->objects.fillCells(c.getRGB()&0xffffff)),()->run(()->objects.fillCells(null)));});
        action("word.table.valign.top","Topo",t,true,()->objects.alignCells(WordTableCell.VerticalAlign.TOP));
        action("word.table.valign.center","Meio",t,true,()->objects.alignCells(WordTableCell.VerticalAlign.CENTER));
        action("word.table.valign.bottom","Base",t,true,()->objects.alignCells(WordTableCell.VerticalAlign.BOTTOM));
        action("word.table.properties","Propriedades da tabela…",t,false,this::showObjectProperties);
        action("word.object.properties","Propriedades…",o,false,this::showObjectProperties);
        action("word.chart.data","Editar dados…",o,false,this::showObjectProperties);
        action("word.equation.edit","Editar equação…",o,false,this::showObjectProperties);
        action("word.object.delete","Excluir objeto",o,true,objects::deleteSelectedObject);
        action("word.image.replace","Substituir imagem…",o,true,this::chooseReplacementImage);
        action("word.object.rotate","Girar 90°",o,true,()->objects.updateSelectedObject("Girar",x2->x2.withRotation(x2.rotation()+90)));
        action("word.object.alt","Texto alternativo…",o,true,()->{String text=ask("Texto alternativo",session.getSelectedObject().map(WordInlineObject::altText).orElse(""));if(text!=null)objects.updateSelectedObject("Texto alternativo",x2->x2.withAltText(text));});
        for(WordPlacement.Wrap wrap:WordPlacement.Wrap.values())action("word.object.wrap."+switch(wrap){case INLINE->"inline";case SQUARE->"square";case TIGHT->"tight";case TOP_AND_BOTTOM->"topbottom";case BEHIND_TEXT->"behind";case IN_FRONT_OF_TEXT->"front";},
                switch(wrap){case INLINE->"Alinhado com o texto";case SQUARE->"Quadrado";case TIGHT->"Justo";case TOP_AND_BOTTOM->"Superior e inferior";case BEHIND_TEXT->"Atrás do texto";case IN_FRONT_OF_TEXT->"Na frente do texto";},o,true,()->objects.setWrap(wrap));
        action("word.object.forward","Trazer para frente",o,true,()->objects.changeOrder(1));action("word.object.backward","Enviar para trás",o,true,()->objects.changeOrder(-1));
        action("word.shape.group","Agrupar formas",o,true,objects::groupShapesInParagraph);action("word.shape.ungroup","Desagrupar",o,true,objects::ungroupSelected);
    }
    private static WordPageSettings sized(WordPageSettings s,float w,float h){boolean landscape=s.landscape();return s.withSize(landscape?h:w,landscape?w:h);}
    private void updatePage(java.util.function.UnaryOperator<WordPageSettings> operation){documentTools.setSectionSettings(operation.apply(documentTools.sectionSettings(session.getSelection().start())));}

    private void sessionChanged(WordSession.Event e){
        if(e.change()==WordSession.Change.DOCUMENT){
            refreshOutline();
            WordDocument before=lastDocument;lastDocument=session.getDocument();
            if(!applyingRemote&&!"Open".equals(e.label()))for(Registration r:List.copyOf(providers.values()))if(r.provider instanceof WordCollaborationProvider c)
                try{c.localChange(new WordCollaborationEvent(e.revision(),e.label(),session.getAuthor(),before,lastDocument));}catch(RuntimeException error){errorHandler.accept(error);}
            if(commentsVisible)commentsPanel.refresh();
        }
        if(e.change()==WordSession.Change.SELECTION)syncOutlineSelection();
        refreshState();firePropertyChange("sessionEvent",null,e);
    }
    private void refreshState(){
        if(closed)return;
        commands.values().forEach(a->{if(Boolean.TRUE.equals(a.getValue("word.edit")))a.setEnabled(!session.isReadOnly());});
        if(commands.containsKey("word.undo"))commands.get("word.undo").setEnabled(session.canUndo());
        if(commands.containsKey("word.redo"))commands.get("word.redo").setEnabled(session.canRedo());
        if(commands.containsKey("word.export.pdf"))commands.get("word.export.pdf").setEnabled(pdfExporter().isPresent());
        if(commands.containsKey("word.recover"))commands.get("word.recover").setEnabled(files.isRecoveryEnabled());
        Action track=commands.get("word.track");if(track!=null)track.putValue(Action.NAME,session.isTrackChanges()?"Controlar alterações ✓":"Controlar alterações");
        Action focus=commands.get("word.focus");if(focus!=null)focus.putValue(Action.NAME,config.ribbonVisible()?"Ocultar ferramentas":"Mostrar ferramentas");
        boolean table=session.getContentSelection() instanceof WordCellSelection||getDocument().tableAt(session.getSelection().caret()).isPresent();
        boolean object=session.getSelectedObject().isPresent();
        for(var entry:commands.entrySet()){
            String group=commandGroups.get(entry.getKey());
            if("Tabela".equals(group)&&!entry.getKey().equals("word.table.properties"))entry.getValue().setEnabled(table&&!session.isReadOnly());
            if("Tabela".equals(group)&&entry.getKey().equals("word.table.properties"))entry.getValue().setEnabled(table);
            if("Objeto".equals(group))entry.getValue().setEnabled(object&&(!Boolean.TRUE.equals(entry.getValue().getValue("word.edit"))||!session.isReadOnly()));
        }
        WordTextStyle insertion=session.getInsertionStyle();
        selected("word.bold",insertion.bold());selected("word.italic",insertion.italic());selected("word.underline",insertion.underline());selected("word.strike",insertion.strike());
        selected("word.superscript",insertion.verticalAlign()==WordTextStyle.VerticalAlign.SUPERSCRIPT);selected("word.subscript",insertion.verticalAlign()==WordTextStyle.VerticalAlign.SUBSCRIPT);
        selected("word.highlight",insertion.highlight()!=null);selected("word.navigation",config.navigationVisible());selected("word.comments.panel",commentsVisible);selected("word.track",session.isTrackChanges());
        selected("word.view.pages",config.viewMode()==WordViewMode.PRINT_LAYOUT);selected("word.view.continuous",config.viewMode()!=WordViewMode.PRINT_LAYOUT);
        WordListRef listRef=getDocument().paragraphAt(session.getSelection().start()).style().list();
        WordListLevel.Format listFormat=listRef==null?WordListLevel.Format.NONE:getDocument().parts().numbering().get(listRef.listId()).map(list->list.level(listRef.level()).format()).orElse(WordListLevel.Format.NONE);
        selected("word.bullets",listFormat==WordListLevel.Format.BULLET);selected("word.numbering",listFormat!=WordListLevel.Format.NONE&&listFormat!=WordListLevel.Format.BULLET);
        for(var alignment:WordParagraphStyle.Alignment.values())selected("word.align."+alignment.name(),getDocument().paragraphAt(session.getSelection().start()).style().alignment()==alignment);
        String text=getText().replace("￼"," ").strip();int words=text.isEmpty()?0:text.split("\\s+").length;
        int pages=canvas.getLayoutSnapshot()==null?1:canvas.getLayoutSnapshot().pages().size();
        String name=files.currentFile().map(p->p.getFileName().toString()).orElse("Documento sem título");
        if(defaultRibbon!=null){defaultRibbon.setTitle(name+(session.isDirty()?" •":""));defaultRibbon.update();}
        status.setText(name+"  •  "+(session.isDirty()?"Alterações não salvas":"Salvo")+"  |  "+pages+" página(s)  |  "+words+" palavras  |  "+(session.isReadOnly()?"Somente leitura":"Edição")
                +(session.isTrackChanges()?"  |  Controle de alterações ativo":"")+"  |  "+Math.round(config.zoom()*100)+"%");
    }
    private void selected(String id,boolean value){Action action=commands.get(id);if(action!=null)action.putValue(Action.SELECTED_KEY,value);}
    private void refreshDiagnostics(){
        Optional<WordImportResult> origin=files.origin();
        if(origin.isEmpty()){diagnostics.setVisible(false);return;}
        WordImportResult r=origin.get();
        if(!r.isEditable()){diagnostics.setText("Visualização protegida: "+String.join("; ",r.blockingReasons()));diagnostics.setForeground(UiTokens.warning());diagnostics.setVisible(true);}
        else if(!r.diagnostics().isEmpty()){diagnostics.setText(r.diagnostics().size()+" aviso(s) de importação — conteúdo desconhecido é preservado ao salvar");diagnostics.setForeground(UiTokens.muted());diagnostics.setVisible(true);}
        else diagnostics.setVisible(false);
        diagnostics.setToolTipText("<html>"+String.join("<br>",r.diagnostics().stream().limit(30).map(s->s.replace("<","&lt;")).toList())+"</html>");
        applyConfig();
    }
    private void refreshOutline(){
        updatingOutline=true;
        try {
            outline.clearSelection();outlineModel.clear();outlineOffsets.clear();
            var doc=getDocument();
            for(int i=0;i<doc.paragraphs().size();i++){var p=doc.paragraphs().get(i);if(p.style().headingLevel()>0&&!p.plainText().isBlank()){outlineModel.addElement("  ".repeat(p.style().headingLevel()-1)+p.plainText());outlineOffsets.add(doc.paragraphStart(i));}}
        } finally {updatingOutline=false;}
        syncOutlineSelection();
    }
    private void syncOutlineSelection(){
        int selected=-1,caret=session.getSelection().caret();
        for(int i=0;i<outlineOffsets.size()&&outlineOffsets.get(i)<=caret;i++)selected=i;
        updatingOutline=true;
        try {
            if(selected<0)outline.clearSelection();else outline.setSelectedIndex(selected);
            if(selected>=0)outline.ensureIndexIsVisible(selected);
        } finally {updatingOutline=false;}
    }
    private void navigateOutline(boolean focusDocument){
        int index=outline.getSelectedIndex();
        if(index<0||index>=outlineOffsets.size())return;
        int offset=outlineOffsets.get(index);
        session.setSelection(offset,offset);
        canvas.revealOffsetAtTop(offset);
        if(focusDocument)canvas.requestFocusInWindow();
    }
    @Override protected void onThemeChanged(){if(north==null)return;UiTokens.refresh();north.setBackground(UiTokens.surface());providerBar.setBackground(UiTokens.surface());status.setBackground(UiTokens.surface());status.setForeground(UiTokens.muted());if(defaultRibbon!=null)defaultRibbon.onThemeChanged();commentsPanel.setBackground(UiTokens.surface());repaint();}
    private void showContextMenu(int x,int y){
        JPopupMenu menu=new JPopupMenu();for(String id:List.of("word.cut","word.copy","word.paste"))menu.add(commands.get(id));
        if(session.getSelectedObject().isPresent()){menu.addSeparator();menu.add(commands.get("word.object.properties"));menu.add(commands.get("word.object.delete"));}
        if(getDocument().tableAt(session.getSelection().caret()).isPresent()||session.getContentSelection() instanceof WordCellSelection){
            menu.addSeparator();for(String id:List.of("word.table.row.below","word.table.column.right","word.table.merge","word.table.split","word.table.properties"))menu.add(commands.get(id));
        }
        menu.addSeparator();menu.add(commands.get("word.insert.link"));menu.add(commands.get("word.comment.new"));
        menu.addSeparator();menu.add(commands.get("word.focus"));
        for(Registration r:providers.values())if(r.provider instanceof WordContextMenuProvider p)try{p.contribute(this,menu);}catch(Exception error){errorHandler.accept(error);}
        menu.show(canvas,x,y);
    }

    public Optional<WordSelection> find(String query,boolean matchCase,boolean regex,int from){
        if(query==null||query.isEmpty())return Optional.empty();
        WordSearchOptions options=new WordSearchOptions(matchCase,false,regex);
        return search.matches(query,options).stream().filter(s->s.start()>=from).findFirst();
    }
    public int replaceAll(String query,String replacement,boolean matchCase){ensureOpen();if(query==null||query.isEmpty())return 0;return search.replaceAll(query,replacement,new WordSearchOptions(matchCase,false,false));}
    public WordPopupHandle showSearch(){
        try{ensureOpen();return popups.showSearch(()->search.context(this,()->{}));}catch(RuntimeException error){errorHandler.accept(error);return WordPopupHandle.closed();}
    }
    public WordPopupHandle showCommandPalette(){
        try{ensureOpen();return popups.showPalette(this::paletteContext);}catch(RuntimeException error){errorHandler.accept(error);return WordPopupHandle.closed();}
    }
    private WordCommandPaletteContext paletteContext(){
        return new WordCommandPaletteContext(){
            public Component owner(){return WordEditor.this;}
            public Locale locale(){return config.locale();}
            public List<WordCommandEntry> commands(){List<WordCommandEntry> list=new ArrayList<>();commands.forEach((id,a)->list.add(new WordCommandEntry(id,String.valueOf(a.getValue(Action.NAME)),commandGroups.getOrDefault(id,""),a.isEnabled())));return list;}
            public boolean execute(String id){Action a=commands.get(id);if(a==null||!a.isEnabled())return false;if(Boolean.TRUE.equals(a.getValue("word.edit"))&&session.isReadOnly())return false;a.actionPerformed(new ActionEvent(WordEditor.this,ActionEvent.ACTION_PERFORMED,"palette"));return true;}
            public void closed(){}
        };
    }
    public WordPopupHandle showObjectProperties(){
        try{
            ensureOpen();
            if(session.getSelectedObject().orElse(null) instanceof WordCustomObject custom){editCustomObject(custom);return WordPopupHandle.closed();}
            if(session.getSelectedObject().isEmpty()&&objects.currentTable().isEmpty())throw new IllegalStateException("Selecione um objeto ou posicione o cursor em uma tabela");
            return popups.showProperties(()->objects.propertiesContext(this,config.locale(),()->{}));
        }catch(RuntimeException error){errorHandler.accept(error);return WordPopupHandle.closed();}
    }
    private void editCustomObject(WordCustomObject custom){
        Optional<WordBlockProvider> provider=providers.values().stream().map(r->r.provider).filter(p->p instanceof WordBlockProvider b&&b.objectType().equals(custom.customType())).map(p->(WordBlockProvider)p).findFirst();
        if(provider.isEmpty())throw new IllegalStateException("O provider do objeto \""+custom.customType()+"\" não está registrado; os dados são preservados");
        if(session.isReadOnly())return;
        int offset=session.getObjectSelection().orElseThrow().offset();
        provider.get().edit(this,custom).ifPresent(updated->session.replaceObject(offset,updated.withData(provider.get().decode(provider.get().encode(updated))),"Editar objeto"));
    }
    private void activateObject(WordLayout.ObjectBox box){
        if(box.object() instanceof WordFormField field){
            if(session.isReadOnly())return;
            int offset=box.offset();
            if(field.kind()==WordFormField.Kind.CHECKBOX){session.replaceObject(offset,field.withChecked(!field.checked()),"Marcar campo");return;}
            session.selectObject(offset);
        }
        showObjectProperties();
    }
    private void openLink(String link){
        if(link.startsWith("#")){
            Map<String,Integer> pages=canvas.getLayoutSnapshot()==null?Map.of():canvas.getLayoutSnapshot().bookmarkPages();
            if(documentTools.navigateTo(link,pages).isEmpty())throw new IllegalArgumentException("Destino não encontrado: "+link);
            canvas.requestFocusInWindow();return;
        }
        firePropertyChange("linkActivated",null,link);
        if(!Boolean.TRUE.equals(getClientProperty("word.openLinks")))return;
        try{if(Desktop.isDesktopSupported()&&link.matches("(?i)(https?|mailto):.*"))Desktop.getDesktop().browse(URI.create(link));}catch(IOException|IllegalArgumentException e){errorHandler.accept(e);}
    }
    private boolean pasteSpecial(Transferable content){
        try{
            if(content.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                @SuppressWarnings("unchecked") List<File> list=(List<File>)content.getTransferData(DataFlavor.javaFileListFlavor);
                boolean any=false;for(File f:list)if(WordResource.contentTypeFor(f.getName()).startsWith("image/")){objects.insertImage(f.toPath());any=true;}
                return any;
            }
            if(content.isDataFlavorSupported(DataFlavor.imageFlavor)&&!content.isDataFlavorSupported(DataFlavor.stringFlavor)){objects.insertImage((Image)content.getTransferData(DataFlavor.imageFlavor),"Imagem colada");return true;}
            if(content.isDataFlavorSupported(DataFlavor.imageFlavor)&&content.isDataFlavorSupported(DataFlavor.stringFlavor)&&((String)content.getTransferData(DataFlavor.stringFlavor)).isBlank()){objects.insertImage((Image)content.getTransferData(DataFlavor.imageFlavor),"Imagem colada");return true;}
        }catch(Exception e){errorHandler.accept(e);return true;}
        return false;
    }
    private final class DropHandler extends TransferHandler {
        @Override public boolean canImport(TransferSupport support){return !session.isReadOnly()&&(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)||support.isDataFlavorSupported(DataFlavor.imageFlavor)||support.isDataFlavorSupported(DataFlavor.stringFlavor));}
        @Override public boolean importData(TransferSupport support){
            if(!canImport(support))return false;
            try{
                if(support.isDrop()){int offset=canvas.hitTest(support.getDropLocation().getDropPoint());session.setSelection(offset,offset);}
                canvas.paste(support.getTransferable());return true;
            }catch(Exception e){errorHandler.accept(e);return false;}
        }
    }

    public WordTask<WordImportResult> open(Path path){return open(path,false);}
    public WordTask<WordImportResult> open(Path path,boolean discardUnsaved){ensureOpen();WordTask<WordImportResult> task=files.open(path,discardUnsaved);task.completion().thenRun(()->SwingUtilities.invokeLater(()->{lastDocument=getDocument();refreshDiagnostics();}));return task;}
    public WordTask<Path> save(Path path){ensureOpen();return files.save(path);}
    public WordTask<Path> export(Path path,ExportFormat format){
        ensureOpen();WordObjectRegistry registry=getObjectRegistry();
        return files.exportText(path,d->format==ExportFormat.HTML?WordTextExporter.html(d,registry):WordTextExporter.text(d));
    }
    public WordTask<Path> export(Path path,String providerId){
        ensureOpen();Registration registration=providers.get(providerId);
        if(registration==null||!(registration.provider instanceof WordExportProvider exporter))throw new IllegalArgumentException("Export provider not registered: "+providerId);
        return files.export(path,exporter);
    }
    public void enableRecovery(WordRecoveryStore store,Duration interval,String key){ensureOpen();files.enableRecovery(store,interval,key);refreshState();}
    public void disableRecovery(){files.disableRecovery();refreshState();}
    public WordTask<WordImportResult> recover(WordRecoveryStore store,String key){ensureOpen();return files.recover(store,key);}
    public List<WordVersion> getVersions(){return files.versions();}
    public WordVersion createVersion(String label){return files.recordVersion(getDocument(),files.currentFile().orElse(null),label);}
    public void restoreVersion(String id){ensureOpen();files.restoreVersion(id);}
    public BufferedImage renderPage(WordDocument document,int pageIndex,double dpi){
        if(!Double.isFinite(dpi)||dpi<36||dpi>600)throw new IllegalArgumentException("DPI must be 36..600");var layout=services.layout().layout(document);var page=layout.pages().get(pageIndex);
        int width=(int)Math.ceil(page.width()*dpi/72),height=(int)Math.ceil(page.height()*dpi/72);
        if((long)width*height>50_000_000)throw new IllegalArgumentException("Page raster too large");
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{g.scale(dpi/72,dpi/72);services.renderer().paintPage(g,page,null,null);}finally{g.dispose();}return image;
    }
    public Printable printable(WordDocument document){
        var layout=services.layout().layout(document);
        return (graphics,format,pageIndex)->{if(pageIndex<0||pageIndex>=layout.pages().size())return Printable.NO_SUCH_PAGE;var page=layout.pages().get(pageIndex);Graphics2D g=(Graphics2D)graphics.create();
            try{g.translate(format.getImageableX(),format.getImageableY());double factor=Math.min(format.getImageableWidth()/page.width(),format.getImageableHeight()/page.height());g.scale(factor,factor);services.renderer().paintPage(g,page,null,null);}finally{g.dispose();}return Printable.PAGE_EXISTS;};
    }

    private Optional<WordExportProvider> pdfExporter(){return providers.values().stream().map(r->r.provider).filter(p->p instanceof WordExportProvider e&&"pdf".equalsIgnoreCase(e.extension())).map(p->(WordExportProvider)p).findFirst();}
    private boolean confirmDiscard(){return !session.isDirty()||popups.confirm(new WordConfirmationRequest(this,WordConfirmationRequest.Kind.DISCARD_CHANGES,"Documento","Descartar alterações não salvas?","Descartar"));}
    private boolean confirmReplace(Path target,String title){return !Files.exists(target)||popups.confirm(new WordConfirmationRequest(this,WordConfirmationRequest.Kind.REPLACE_FILE,title,"Substituir o arquivo existente?","Substituir"));}
    private Optional<Path> choose(WordFileDialogRequest.Mode mode,String title,String suggested,String description,String... extensions){
        return popups.chooseFile(new WordFileDialogRequest(this,mode,title,suggested,description,List.of(extensions),files.currentFile().map(Path::getParent).orElse(null)));
    }
    private void chooseOpen(){if(!confirmDiscard())return;choose(WordFileDialogRequest.Mode.OPEN,"Abrir documento","","Documento Word (*.docx)","docx").ifPresent(p->open(p,true));}
    private void chooseSave(){if(files.currentFile().isPresent()){save(files.currentFile().get());return;}chooseSaveAs();}
    private void chooseSaveAs(){choose(WordFileDialogRequest.Mode.SAVE,"Salvar documento","Documento.docx","Documento Word (*.docx)","docx").ifPresent(target->{if(confirmReplace(target,"Salvar"))save(target);});}
    private void chooseExport(ExportFormat format){
        String ext=format==ExportFormat.HTML?"html":"txt";
        choose(WordFileDialogRequest.Mode.EXPORT,"Exportar","Documento."+ext,format==ExportFormat.HTML?"Página HTML":"Texto",ext).ifPresent(target->{if(confirmReplace(target,"Exportar"))export(target,format);});
    }
    private void choosePdf(){
        WordExportProvider exporter=pdfExporter().orElseThrow(()->new IllegalStateException("Registre um WordExportProvider com extensão pdf (ex.: WordPdfExportProvider)"));
        choose(WordFileDialogRequest.Mode.EXPORT,"Exportar PDF","Documento.pdf","Documento PDF","pdf").ifPresent(target->{if(confirmReplace(target,"Exportar"))export(target,exporter.id());});
    }
    private void chooseImage(){
        choose(WordFileDialogRequest.Mode.IMAGE,"Inserir imagem","","Imagens","png","jpg","jpeg","gif","bmp").ifPresent(p->{try{objects.insertImage(p);}catch(IOException e){throw new IllegalStateException(e.getMessage(),e);}});
    }
    private void chooseReplacementImage(){
        choose(WordFileDialogRequest.Mode.IMAGE,"Substituir imagem","","Imagens","png","jpg","jpeg","gif","bmp").ifPresent(p->{try{objects.replaceImage(p);}catch(IOException e){throw new IllegalStateException(e.getMessage(),e);}});
    }
    private void compareWithFile(){
        choose(WordFileDialogRequest.Mode.OPEN,"Comparar com documento original","","Documento Word (*.docx)","docx").ifPresent(p->{
            WordDocument revised=getDocument();
            WordTask<WordImportResult> task=runner.task();
            task.attach(runner.submit(()->{try(var in=Files.newInputStream(p)){WordImportResult original=services.docx().read(in);SwingUtilities.invokeLater(()->task.complete(original));}catch(Throwable e){runner.fail(task,e);}}));
            task.completion().thenAccept(original->SwingUtilities.invokeLater(()->run(()->{if(confirmDiscard())setDocument(review.compare(original.document(),revised));})));
        });
    }
    private void print(){
        PrinterJob job=PrinterJob.getPrinterJob();job.setPrintable(printable(getDocument()));
        if(job.printDialog())runner.submit(()->{try{job.print();}catch(PrinterException e){SwingUtilities.invokeLater(()->errorHandler.accept(e));}});
    }
    private void chooseVersion(){
        List<WordVersion> versions=getVersions();
        if(versions.isEmpty())throw new IllegalStateException("Nenhuma versão registrada ainda; salve o documento para criar versões");
        JComboBox<String> list=new JComboBox<>();
        for(WordVersion v:versions.reversed())list.addItem(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(java.time.ZoneId.systemDefault()).format(v.savedAt())+"  "+v.label());
        String chosen=form("word.versions.dialog","Histórico de versões","Restaurar uma versão cria uma nova alteração que pode ser desfeita.",list,()->(String)list.getSelectedItem(),"Restaurar",false,true).orElse(null);
        if(chosen!=null)restoreVersion(versions.reversed().get(list.getSelectedIndex()).id());
    }
    private void recoverNow(){if(!files.isRecoveryEnabled())throw new IllegalStateException("Recuperação não configurada");files.snapshotForRecovery();}
    private void promptTemplate(){
        Set<String> variables=WordTemplates.variables(getDocument());
        if(variables.isEmpty())throw new IllegalStateException("O documento não possui variáveis ${nome}");
        Map<String,String> values=new LinkedHashMap<>();
        for(String name:variables){String value=ask("Preencher modelo","Valor para ${"+name+"}");if(value==null)return;values.put(name,value);}
        fillTemplate(values);
    }
    private void mailMerge(){
        choose(WordFileDialogRequest.Mode.OPEN,"Dados da mala direta","","Planilha CSV","csv").ifPresent(p->{
            try{WordDocument merged=WordMailMerge.combined(getDocument(),WordMailMerge.parseCsv(Files.readString(p,StandardCharsets.UTF_8)));if(confirmDiscard())setDocument(merged);}
            catch(IOException e){throw new IllegalStateException(e.getMessage(),e);}
        });
    }
    private void promptLink(){
        String current=session.getSelection().isEmpty()?"":Optional.ofNullable(getDocument().styleAt(session.getSelection().start()).link()).orElse("");
        String link=ask("Inserir link","Endereço (https://…) ou #indicador",current);
        if(link!=null)objects.setLink(link);
    }
    private void promptCrossReference(){
        List<String> marks=documentTools.bookmarks();
        if(marks.isEmpty())throw new IllegalStateException("Crie um indicador (ou uma legenda) antes de inserir a referência cruzada");
        JComboBox<String> list=new JComboBox<>(marks.toArray(String[]::new));
        String chosen=form("word.crossref.dialog","Referência cruzada","Inserir o texto do indicador:",list,()->(String)list.getSelectedItem(),"Inserir",false,true).orElse(null);
        if(chosen!=null)documentTools.insertCrossReference(chosen,false);
    }
    private void pageSetup(){
        WordPageSetupPanel panel=new WordPageSetupPanel(documentTools.sectionSettings(session.getSelection().start()));
        Object result=form("word.page.dialog",panel.title(),"",panel,panel::result,"Aplicar",session.isReadOnly(),false).orElse(null);
        if(result instanceof WordPageSettings settings)documentTools.setSectionSettings(settings);
    }
    public void editHeaderFooter(boolean footer){
        ensureOpen();
        WordHeaderFooterPanel panel=new WordHeaderFooterPanel(getDocument().parts().headers(),footer);
        Object result=form("word.headerFooter.dialog",footer?"Rodapé":"Cabeçalho","",panel,panel::result,"Aplicar",session.isReadOnly(),false).orElse(null);
        if(session.isReadOnly()||!(result instanceof WordHeaderFooterPanel.Result r))return;
        documentTools.setHeaderFooterText(r.kind(),r.text(),r.alignment(),r.pageNumber());
        documentTools.setHeaderOptions(r.differentFirst(),r.differentOddEven());
    }
    public String ask(String title,String message){return ask(title,message,"");}
    public String ask(String title,String message,String initial){
        JTextField field=new JTextField(initial==null?"":initial,28);
        return form("word.input.dialog",title,message,field,field::getText,"Confirmar",false,true).orElse(null);
    }
    private <T> Optional<T> form(String id,String title,String message,JComponent content,java.util.function.Supplier<T> result,String confirm,boolean readOnly,boolean enterConfirms){
        ensureOpen();return popups.showDialog(new WordDialogRequest<>(this,id,title,message,content,result,value->{},confirm,readOnly,enterConfirms));
    }
    private void ensureOpen(){if(closed)throw new IllegalStateException("Editor is closed");}
    @Override public void close(){
        if(closed)return;closed=true;
        screenActive=false;
        defaultRibbon.closePopups();
        popups.close();runner.close();files.close();
        for(Registration r:List.copyOf(providers.values()))try{r.close();}catch(Exception error){errorHandler.accept(error);}
        sessionListener.close();fontListener.run();canvas.close();
    }
}
