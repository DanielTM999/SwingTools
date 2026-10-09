package dtm.stools.component.panels.editor.powerpoint;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.editor.powerpoint.api.*;
import dtm.stools.component.panels.editor.powerpoint.config.*;
import dtm.stools.component.panels.editor.powerpoint.io.PptxCodec;
import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.powerpoint.provider.*;
import dtm.stools.component.panels.editor.powerpoint.ui.*;
import dtm.stools.component.panels.editor.powerpoint.ui.popup.*;
import dtm.stools.configs.UiTokens;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PowerPointEditor extends BlockingPanel implements AutoCloseable {
    private final PowerPointSession session=new PowerPointSession();
    private final PowerPointServices services;
    private PowerPointEditorConfig config;
    private final PowerPointCanvas canvas;
    private final JPanel north=new JPanel(new BorderLayout()),providerBar=new JPanel(new FlowLayout(FlowLayout.LEADING,4,2)),west=new JPanel(new BorderLayout()),east=new JPanel(new BorderLayout());
    private final JLabel status=new JLabel();
    private final JLabel slideCount=new JLabel(),objectType=new JLabel("Selecione um objeto");
    private final JTabbedPane inspectorTabs=new JTabbedPane();
    private final List<AbstractButton> inspectorEditButtons=new ArrayList<>();
    private final DefaultListModel<String> slideNames=new DefaultListModel<>();
    private final JList<String> thumbnails=new JList<>(slideNames);
    private final JTextField titleField=new JTextField();
    private final JComboBox<String> transitionChoice=new JComboBox<>(new String[]{"Corte","Esmaecer","Varrer","Empurrar"});
    private final JSpinner fontSize=new JSpinner(new SpinnerNumberModel(36,1,400,1));
    private final JComboBox<PptAnimation.Start> animationStart=new JComboBox<>(PptAnimation.Start.values());
    private final JSpinner animationDuration=new JSpinner(new SpinnerNumberModel(600,0,60000,100));
    private final JSpinner animationDelay=new JSpinner(new SpinnerNumberModel(0,0,60000,100));
    private final JSpinner animationRepeat=new JSpinner(new SpinnerNumberModel(1,1,100,1));
    private final DefaultListModel<String> animationNames=new DefaultListModel<>();
    private final JList<String> animationList=new JList<>(animationNames);
    private final ExecutorService files=Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"PowerPointEditor-files");thread.setDaemon(true);return thread;});
    private final Map<String,PowerPointProvider> providers=new LinkedHashMap<>();
    private final PowerPointDialogProvider defaultDialogs=new DefaultPowerPointDialogProvider();
    private final PowerPointFileDialogProvider defaultFiles=new DefaultPowerPointFileDialogProvider();
    private final Map<String,AutoCloseable> registrations=new HashMap<>();
    private final Map<String,Action> commands=new LinkedHashMap<>();
    private final javax.swing.Timer animationTimer;
    private final javax.swing.Timer transitionTimer;
    private final List<ScheduledAnimation> scheduled=new ArrayList<>();
    private final Map<String,Double> animationProgress=new HashMap<>();
    private final Map<String,PptAnimation.Effect> activeEffects=new HashMap<>();
    private JComponent ribbon;
    private PptxCodec.ImportResult origin;
    private PptObject copiedObject;
    private Path currentFile;
    private double textPointScale=1280.*12700/9144000;
    private boolean updatingUi,presenting,closed;
    private int nextAnimation;
    private long animationEpoch;
    private long previousAnimationStart;
    private long previousAnimationEnd;
    private Consumer<Throwable> errorHandler=Throwable::printStackTrace;
    private final List<PowerPointMediaProvider.Player> activeMedia=new ArrayList<>();
    private long mediaGeneration;

    private record ScheduledAnimation(PptAnimation animation,long startMs,long endMs){}
    public PowerPointEditor(){this(PowerPointEditorConfig.defaults(),PowerPointServices.defaults());}
    public PowerPointEditor(PowerPointEditorConfig config){this(config,PowerPointServices.defaults());}
    public PowerPointEditor(PowerPointEditorConfig config,PowerPointServices services){
        this.config=Objects.requireNonNull(config);this.services=Objects.requireNonNull(services);
        setLayout(new BorderLayout());
        canvas=services.uiFactory().createCanvas(session,services.renderer());
        canvas.setTextEditorFactory(services.uiFactory()::createTextEditor);
        canvas.setTextSelectionListener(()->{if(ribbon instanceof PowerPointRibbon builtIn)builtIn.refreshFontState();});
        canvas.setEditTextHandler(this::editText);
        add(canvas,BorderLayout.CENTER);
        ribbon=services.uiFactory().createRibbon(this);north.add(ribbon,BorderLayout.CENTER);north.add(providerBar,BorderLayout.SOUTH);add(north,BorderLayout.NORTH);
        thumbnails.setName("powerpoint.thumbnails");thumbnails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        thumbnails.setFixedCellHeight(UIScale.scale(132));thumbnails.setCellRenderer(new ThumbnailRenderer());
        thumbnails.setBackground(UiTokens.background());
        if(!GraphicsEnvironment.isHeadless())thumbnails.setDragEnabled(true);
        thumbnails.setDropMode(DropMode.INSERT);
        thumbnails.setTransferHandler(new TransferHandler(){
            @Override protected Transferable createTransferable(JComponent source){return new StringSelection(Integer.toString(thumbnails.getSelectedIndex()));}
            @Override public int getSourceActions(JComponent source){return MOVE;}
            @Override public boolean canImport(TransferSupport support){return support.isDrop()&&support.isDataFlavorSupported(DataFlavor.stringFlavor)&&!session.isReadOnly();}
            @Override public boolean importData(TransferSupport support){
                if(!canImport(support))return false;
                try{int from=Integer.parseInt((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                    int drop=((JList.DropLocation)support.getDropLocation()).getIndex();
                    int to=Math.min(session.getPresentation().slides().size()-1,drop>from?drop-1:drop);
                    if(from!=to)moveSlide(from,to);return true;
                }catch(Exception error){errorHandler.accept(error);return false;}
            }
        });
        thumbnails.addListSelectionListener(e->{if(!updatingUi&&!e.getValueIsAdjusting()&&thumbnails.getSelectedIndex()>=0)session.selectSlide(thumbnails.getSelectedIndex());});
        thumbnails.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){showSlideMenu(e);}
            @Override public void mouseReleased(MouseEvent e){showSlideMenu(e);}
        });
        west.setName("powerpoint.slides.sidebar");west.setBackground(UiTokens.background());
        west.setBorder(BorderFactory.createMatteBorder(0,0,0,1,UiTokens.border()));
        JPanel slideHeader=new JPanel(new BorderLayout(8,0));slideHeader.setOpaque(false);
        slideHeader.setBorder(BorderFactory.createEmptyBorder(12,14,10,10));
        JLabel slideTitle=sidebarTitle("Slides");slideHeader.add(slideTitle,BorderLayout.WEST);
        slideCount.setForeground(UiTokens.muted());slideHeader.add(slideCount,BorderLayout.EAST);west.add(slideHeader,BorderLayout.NORTH);
        JScrollPane slideScroll=new JScrollPane(thumbnails);slideScroll.setBorder(BorderFactory.createEmptyBorder());
        slideScroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(20));west.add(slideScroll,BorderLayout.CENTER);
        JPanel slideActions=new JPanel(new BorderLayout(6,0));slideActions.setOpaque(false);
        slideActions.setBorder(BorderFactory.createEmptyBorder(10,12,12,12));
        slideActions.add(sidebarButton("+ Novo slide",this::addSlide),BorderLayout.CENTER);
        JButton moreSlides=sidebarButton("⋯",()->slideMenu().show(slideActions,Math.max(0,slideActions.getWidth()-UIScale.scale(155)),0));
        moreSlides.setPreferredSize(UIScale.scale(new Dimension(38,30)));slideActions.add(moreSlides,BorderLayout.EAST);
        west.add(slideActions,BorderLayout.SOUTH);west.setPreferredSize(UIScale.scale(new Dimension(220,100)));add(west,BorderLayout.WEST);
        east.setName("powerpoint.properties.sidebar");east.setBackground(UiTokens.background());
        east.setBorder(BorderFactory.createMatteBorder(0,1,0,0,UiTokens.border()));
        east.add(buildProperties(),BorderLayout.CENTER);east.setPreferredSize(UIScale.scale(new Dimension(270,100)));add(east,BorderLayout.EAST);
        canvas.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){showObjectMenu(e);}
            @Override public void mouseReleased(MouseEvent e){showObjectMenu(e);}
        });
        addComponentListener(new ComponentAdapter(){@Override public void componentResized(ComponentEvent e){adaptSidebars();}});
        status.setOpaque(true);status.setBackground(UIManager.getColor("Panel.background"));
        status.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));add(status,BorderLayout.SOUTH);
        session.setEditValidator(next->{Optional<String> rejection=services.pptx().validateEdit(origin,session.getPresentation(),next);if(rejection.isPresent()){active(PowerPointDialogProvider.class).orElse(defaultDialogs).message(this,"Alteracao indisponivel",rejection.get());return false;}return true;});
        session.addListener(this::refresh);
        animationTimer=new javax.swing.Timer(16,e->tickAnimation());
        transitionTimer=new javax.swing.Timer(16,e->tickTransition());
        canvas.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(presenting&&SwingUtilities.isLeftMouseButton(e))advancePresentation();}});
        bind(KeyEvent.VK_ESCAPE,0,"stop",this::stopPresentation);
        bind(KeyEvent.VK_RIGHT,0,"next",this::advancePresentation);bind(KeyEvent.VK_SPACE,0,"space",this::advancePresentation);
        bind(KeyEvent.VK_ENTER,0,"enter",this::advancePresentation);bind(KeyEvent.VK_LEFT,0,"previous",this::previousPresentation);
        bind(KeyEvent.VK_Z,InputEvent.CTRL_DOWN_MASK,"undo",this::undo);
        bind(KeyEvent.VK_Y,InputEvent.CTRL_DOWN_MASK,"redo",this::redo);
        bind(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK,"save",this::chooseSave);
        bind(KeyEvent.VK_C,InputEvent.CTRL_DOWN_MASK,"copy",this::copySelectedObject);
        bind(KeyEvent.VK_V,InputEvent.CTRL_DOWN_MASK,"paste",this::pasteObject);
        bind(KeyEvent.VK_DELETE,0,"deleteObject",this::removeSelectedObject);
        bind(KeyEvent.VK_F5,0,"present",this::startPresentation);
        addProvider(new WavMediaProvider());addProvider(new AviMjpegMediaProvider());
        applyConfig();refresh();
    }
    private void bind(int key,int modifiers,String id,Runnable action){
        KeyStroke stroke=KeyStroke.getKeyStroke(key,modifiers);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke,id);
        getActionMap().put(id,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){action.run();}});
    }
    private JComponent buildProperties(){
        JPanel panel=new JPanel(new BorderLayout());panel.setOpaque(false);
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12,14,8,10));
        header.add(sidebarTitle("Propriedades"),BorderLayout.WEST);
        JButton hide=sidebarButton("×",()->setPropertiesVisible(false));hide.setToolTipText("Ocultar propriedades");
        header.add(hide,BorderLayout.EAST);panel.add(header,BorderLayout.NORTH);

        JPanel slide=inspectorPage();
        JPanel identity=section("SLIDE");identity.add(field("Título",titleField));
        identity.add(editButton("Aplicar título",()->{if(!updatingUi)renameSlide(titleField.getText());}));
        slide.add(identity);slide.add(Box.createVerticalStrut(10));
        JPanel slideAppearance=section("APARÊNCIA");
        slideAppearance.add(editButton("Cor de fundo",this::chooseBackground));
        slideAppearance.add(Box.createVerticalStrut(8));
        transitionChoice.setName("powerpoint.inspector.transition");
        transitionChoice.addActionListener(e->{if(!updatingUi)setTransition(new String[]{"cut","fade","wipe","push"}[transitionChoice.getSelectedIndex()]);});
        slideAppearance.add(field("Transição",transitionChoice));slide.add(slideAppearance);

        JPanel object=inspectorPage();
        JPanel objectCard=section("OBJETO SELECIONADO");
        objectType.setForeground(UiTokens.muted());objectCard.add(objectType);
        objectCard.add(Box.createVerticalStrut(8));
        objectCard.add(field("Tamanho da fonte",fontSize));
        objectCard.add(editButton("Aplicar fonte",()->setSelectedFontSize((int)fontSize.getValue())));
        objectCard.add(Box.createVerticalStrut(6));
        objectCard.add(editButton("Cor do objeto",this::chooseObjectFill));object.add(objectCard);
        object.add(Box.createVerticalStrut(10));
        JPanel arrange=section("ORGANIZAR");
        arrange.add(editButton("Trazer à frente",this::bringSelectedToFront));
        arrange.add(editButton("Enviar ao fundo",this::sendSelectedToBack));
        arrange.add(editButton("Girar 90°",()->rotateSelectedBy(90)));object.add(arrange);

        JPanel animations=inspectorPage();
        JPanel timeline=section("LINHA DO TEMPO");
        animationList.setName("powerpoint.animations");animationList.setFixedCellHeight(UIScale.scale(38));
        animationList.setSelectionBackground(UiTokens.accent());animationList.setSelectionForeground(UiTokens.onColor(UiTokens.accent()));
        JScrollPane timelineScroll=new JScrollPane(animationList);timelineScroll.setPreferredSize(new Dimension(210,145));
        timelineScroll.setBorder(BorderFactory.createLineBorder(UiTokens.border()));timeline.add(timelineScroll);
        JPanel order=new JPanel(new GridLayout(1,3,5,0));order.setOpaque(false);
        order.add(editButton("↑",()->moveAnimation(-1)));
        order.add(editButton("↓",()->moveAnimation(1)));
        order.add(editButton("Excluir",this::removeAnimation));timeline.add(order);animations.add(timeline);
        animations.add(Box.createVerticalStrut(10));
        JPanel timing=section("TEMPORIZAÇÃO");
        timing.add(field("Início",animationStart));
        timing.add(field("Duração (ms)",animationDuration));
        timing.add(field("Atraso (ms)",animationDelay));
        timing.add(field("Repetições",animationRepeat));
        timing.add(editButton("Aplicar animação",this::applyAnimationSettings));animations.add(timing);

        inspectorTabs.setName("powerpoint.inspector.tabs");
        inspectorTabs.putClientProperty("JTabbedPane.tabType","underlined");
        inspectorTabs.addTab("Slide",inspectorScroll(slide));
        inspectorTabs.addTab("Objeto",inspectorScroll(object));
        inspectorTabs.addTab("Animações",inspectorScroll(animations));
        panel.add(inspectorTabs,BorderLayout.CENTER);
        animationList.addListSelectionListener(e->{if(!e.getValueIsAdjusting())showAnimationSettings();});
        return panel;
    }
    private static JPanel inspectorPage(){JPanel panel=new JPanel();panel.setOpaque(false);panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,18,12));return panel;}
    private static JScrollPane inspectorScroll(JComponent content){JScrollPane scroll=new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getVerticalScrollBar().setUnitIncrement(UIScale.scale(18));
        scroll.getViewport().setBackground(UiTokens.background());return scroll;}
    private static JPanel section(String title){JPanel panel=new RoundedCard();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading=new JLabel(title);heading.setFont(UiTokens.fontBold().deriveFont(UIScale.scale(10f)));
        heading.setForeground(UiTokens.muted());heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);panel.add(Box.createVerticalStrut(10));return panel;}
    private static JComponent field(String label,JComponent input){JPanel row=new JPanel(new BorderLayout(0,5));row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);row.setBorder(BorderFactory.createEmptyBorder(0,0,9,0));
        JLabel caption=new JLabel(label);caption.setForeground(UiTokens.muted());row.add(caption,BorderLayout.NORTH);
        row.add(input,BorderLayout.CENTER);row.setMaximumSize(new Dimension(Integer.MAX_VALUE,UIScale.scale(64)));return row;}
    private JButton editButton(String label,Runnable action){JButton button=sidebarButton(label,action);inspectorEditButtons.add(button);return button;}
    private static JLabel sidebarTitle(String text){JLabel title=new JLabel(text);title.setFont(UiTokens.fontBold().deriveFont(UIScale.scale(15f)));return title;}
    private static JButton sidebarButton(String label,Runnable action){JButton button=new JButton(label);button.putClientProperty("JButton.buttonType","roundRect");
        button.setFocusable(false);button.addActionListener(e->action.run());button.setAlignmentX(Component.LEFT_ALIGNMENT);return button;}
    private static final class RoundedCard extends JPanel {
        RoundedCard(){setOpaque(false);}
        @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UiTokens.surface());g.fill(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,14,14));
            g.setColor(UiTokens.border());g.draw(new RoundRectangle2D.Double(0.5,0.5,getWidth()-2,getHeight()-2,14,14));
        }finally{g.dispose();}super.paintComponent(graphics);}
    }
    private void adaptSidebars(){int width=getWidth();if(width<=0)return;
        int left=UIScale.scale(width<920?170:220),right=UIScale.scale(width<920?230:270);
        int cellWidth=Math.max(UIScale.scale(110),left-UIScale.scale(18));
        if(thumbnails.getFixedCellWidth()!=cellWidth)thumbnails.setFixedCellWidth(cellWidth);
        if(west.getPreferredSize().width!=left||east.getPreferredSize().width!=right){
            west.setPreferredSize(new Dimension(left,100));east.setPreferredSize(new Dimension(right,100));revalidate();}
    }
    @Override public void doLayout(){adaptSidebars();super.doLayout();}
    private void showSlideMenu(MouseEvent event){if(!event.isPopupTrigger())return;
        int index=thumbnails.locationToIndex(event.getPoint());
        if(index<0||!thumbnails.getCellBounds(index,index).contains(event.getPoint()))return;
        thumbnails.setSelectedIndex(index);slideMenu().show(thumbnails,event.getX(),event.getY());
    }
    private JPopupMenu slideMenu(){JPopupMenu menu=new JPopupMenu();
        menu.add(menuHeader("SLIDE "+(session.selectedSlide()+1)));menu.addSeparator();
        menu.add(menuItem("Novo slide",this::addSlide,!session.isReadOnly()));
        menu.add(menuItem("Duplicar slide",this::duplicateSlide,!session.isReadOnly()));
        menu.add(menuItem("Excluir slide",this::removeSlide,!session.isReadOnly()&&getPresentation().slides().size()>1));
        menu.addSeparator();
        int index=session.selectedSlide();
        menu.add(menuItem("Mover para cima",()->moveSlide(index,index-1),!session.isReadOnly()&&index>0));
        menu.add(menuItem("Mover para baixo",()->moveSlide(index,index+1),!session.isReadOnly()&&index<getPresentation().slides().size()-1));
        return menu;
    }
    private void showObjectMenu(MouseEvent event){if(!event.isPopupTrigger()||presenting)return;
        JPopupMenu menu=new JPopupMenu();boolean selected=session.selectedObjectId()!=null,editable=!session.isReadOnly();
        boolean textSelected=canvas.currentSlide().objects().stream().anyMatch(o->o.id().equals(session.selectedObjectId())&&o.kind()==PptObject.Kind.TEXT);
        menu.add(menuHeader(selected?"OBJETO":"SLIDE"));menu.addSeparator();
        menu.add(menuItem("Editar texto",this::editSelectedText,textSelected&&editable));
        menu.add(menuItem("Duplicar",this::duplicateSelectedObject,selected&&editable));
        menu.add(menuItem("Copiar",this::copySelectedObject,selected));
        menu.add(menuItem("Colar",this::pasteObject,editable));
        menu.addSeparator();
        menu.add(menuItem("Trazer à frente",this::bringSelectedToFront,selected&&editable));
        menu.add(menuItem("Enviar ao fundo",this::sendSelectedToBack,selected&&editable));
        menu.add(menuItem("Excluir",this::removeSelectedObject,selected&&editable));
        menu.show(canvas,event.getX(),event.getY());
    }
    private static JMenuItem menuItem(String label,Runnable action,boolean enabled){JMenuItem item=new JMenuItem(label);
        item.setEnabled(enabled);item.addActionListener(e->action.run());
        switch(label){case "Copiar"->item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_DOWN_MASK));
            case "Colar"->item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,InputEvent.CTRL_DOWN_MASK));
            case "Excluir"->item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0));default->{}}
        return item;}
    private static JLabel menuHeader(String title){JLabel label=new JLabel(title);label.setFont(UiTokens.fontBold().deriveFont(UIScale.scale(10f)));
        label.setForeground(UiTokens.muted());label.setBorder(BorderFactory.createEmptyBorder(8,12,5,12));return label;}
    private void refresh(){
        if(closed)return;updatingUi=true;
        try {
            Presentation doc=session.getPresentation();int selected=session.selectedSlide();
            slideNames.clear();for(int i=0;i<doc.slides().size();i++)slideNames.addElement((i+1)+". "+doc.slides().get(i).title());
            thumbnails.setSelectedIndex(selected);titleField.setText(doc.slides().get(selected).title());
            slideCount.setText(doc.slides().size()+(doc.slides().size()==1?" slide":" slides"));
            transitionChoice.setSelectedIndex(switch(doc.slides().get(selected).transition()){
                case "fade"->1;case "wipe"->2;case "push"->3;default->0;});
            String selectedId=session.selectedObjectId();
            PptObject selectedObject=doc.slides().get(selected).objects().stream().filter(object->object.id().equals(selectedId)).findFirst().orElse(null);
            objectType.setText(selectedObject==null?"Selecione um objeto":switch(selectedObject.kind()){
                case ROUND_RECTANGLE->"Retangulo arredondado";case DIAMOND->"Losango";case TABLE->"Tabela";case CONNECTOR->"Conector";case TEXT->"Caixa de texto";case RECTANGLE->"Retângulo";case ELLIPSE->"Elipse";
                case IMAGE->"Imagem";case AUDIO->"Áudio";case VIDEO->"Vídeo";});
            inspectorTabs.setEnabledAt(1,selectedObject!=null);
            if(selectedObject==null&&inspectorTabs.getSelectedIndex()==1)inspectorTabs.setSelectedIndex(0);
            titleField.setEditable(!session.isReadOnly());transitionChoice.setEnabled(!session.isReadOnly());
            fontSize.setEnabled(selectedObject!=null&&!session.isReadOnly());
            animationStart.setEnabled(!session.isReadOnly());animationDuration.setEnabled(!session.isReadOnly());
            animationDelay.setEnabled(!session.isReadOnly());animationRepeat.setEnabled(!session.isReadOnly());
            for(AbstractButton button:inspectorEditButtons)button.setEnabled(!session.isReadOnly());
            if(ribbon instanceof PowerPointRibbon builtIn)builtIn.refreshSelection(doc.slides().get(selected).objects().stream()
                    .filter(object->object.id().equals(selectedId)).map(PptObject::kind).findFirst().orElse(null),session.isReadOnly());
            for(PptObject object:doc.slides().get(selected).objects())if(object.id().equals(selectedId))fontSize.setValue(object.fontSize());
            int animationSelection=animationList.getSelectedIndex();
            animationNames.clear();for(PptAnimation animation:doc.slides().get(selected).animations())animationNames.addElement(animation.effect()+" · "+animation.start());
            if(animationSelection>=0&&animationSelection<animationNames.size())animationList.setSelectedIndex(animationSelection);
            status.setText("Slide "+(selected+1)+" de "+doc.slides().size()+(session.isDirty()?"  •  alterações não salvas":"")
                    +(origin!=null&&!origin.editable()?"  •  PPTX externo protegido":""));
            canvas.repaint();thumbnails.repaint();
        }finally{updatingUi=false;}
    }
    private void applyConfig(){
        session.setHistoryLimit(config.historyLimit());session.setReadOnly(config.readOnly()||(origin!=null&&!origin.editable()));
        canvas.setZoom(config.zoom());
        north.setVisible(config.ribbonVisible()&&!presenting);west.setVisible(config.thumbnailsVisible()&&!presenting);
        east.setVisible(config.propertiesVisible()&&!presenting);status.setVisible(config.statusVisible()&&!presenting);
        revalidate();repaint();
    }
    private void editable(){ensureOpen();if(session.isReadOnly())throw new IllegalStateException("Apresentação protegida ou em modo somente leitura");}
    private void ensureOpen(){if(closed)throw new IllegalStateException("Editor closed");}
    public PowerPointSession getSession(){return session;}
    public Presentation getPresentation(){return session.getPresentation();}
    public PowerPointCanvas getCanvas(){return canvas;}
    public PowerPointEditorConfig getConfig(){return config;}
    public Optional<Path> getCurrentFile(){return Optional.ofNullable(currentFile);}
    public void setErrorHandler(Consumer<Throwable> handler){errorHandler=Objects.requireNonNull(handler);}
    public void setPresentation(Presentation value){ensureOpen();stopPresentation();origin=null;currentFile=null;textPointScale=value.width()*12700./9144000;session.replace(value);applyConfig();}
    public void setConfig(PowerPointEditorConfig value){ensureOpen();config=Objects.requireNonNull(value);applyConfig();}
    public void setRibbon(JComponent value){ensureOpen();north.remove(ribbon);ribbon=Objects.requireNonNull(value);north.add(ribbon,BorderLayout.CENTER);applyConfig();}
    public void setThumbnailsVisible(boolean value){setConfig(new PowerPointEditorConfig(config.readOnly(),config.ribbonVisible(),value,config.propertiesVisible(),config.statusVisible(),config.zoom(),config.locale(),config.historyLimit()));}
    public void setPropertiesVisible(boolean value){setConfig(new PowerPointEditorConfig(config.readOnly(),config.ribbonVisible(),config.thumbnailsVisible(),value,config.statusVisible(),config.zoom(),config.locale(),config.historyLimit()));}
    public boolean isThumbnailsVisible(){return config.thumbnailsVisible();}
    public boolean isPropertiesVisible(){return config.propertiesVisible();}
    public void selectSlide(int index){ensureOpen();canvas.commitEditing();session.selectSlide(index);}
    public void selectObject(String id){ensureOpen();session.selectObject(id);}
    public void undo(){ensureOpen();canvas.commitEditing();session.undo();}public void redo(){ensureOpen();canvas.commitEditing();session.redo();}
    public void addSlide(){editable();int index=session.selectedSlide()+1;session.edit(doc->doc.insertSlide(index,PptSlide.create("Slide "+(doc.slides().size()+1))));if(index<session.getPresentation().slides().size())session.selectSlide(index);}
    public void duplicateSlide(){editable();int index=session.selectedSlide()+1;session.edit(doc->doc.insertSlide(index,doc.slides().get(index-1).duplicate()));if(index<session.getPresentation().slides().size())session.selectSlide(index);}
    public void removeSlide(){editable();int index=session.selectedSlide();session.edit(doc->doc.removeSlide(index));session.selectSlide(Math.min(index,session.getPresentation().slides().size()-1));}
    public void moveSlide(int from,int to){editable();session.edit(doc->doc.moveSlide(from,to));session.selectSlide(to);}
    public void renameSlide(String title){editable();int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).withTitle(title)));}
    public void insertText(String text){editable();addObject(PptObject.text(text,110,90,680,140));}
    public void insertShape(PptObject.Kind kind){editable();addObject(PptObject.shape(kind,180,160,320,200));}
    public void insertMedia(PptObject.Kind kind,String mime,byte[] bytes){editable();
        if(kind==PptObject.Kind.IMAGE&&!List.of("image/png","image/jpeg").contains(mime))throw new IllegalArgumentException("Somente PNG e JPEG são suportados para imagens");
        if(bytes.length>32*1024*1024)throw new IllegalArgumentException("Mídia acima do limite de 32 MB");
        addObject(PptObject.media(kind,mime,bytes,180,160,440,260));}
    private void addObject(PptObject object){int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).addObject(object)));session.selectObject(object.id());}
    public void removeSelectedObject(){editable();String id=session.selectedObjectId();if(id==null)return;int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).removeObject(id)));session.selectObject(null);}
    public void copySelectedObject(){String id=session.selectedObjectId();if(id==null)return;for(PptObject object:canvas.currentSlide().objects())if(object.id().equals(id)){copiedObject=object;return;}}
    public void pasteObject(){editable();if(copiedObject!=null)addObject(copiedObject.duplicate());}
    public void duplicateSelectedObject(){copySelectedObject();pasteObject();}
    public void resizeSelectedObject(double width,double height){editSelectedObject(object->object.geometry(object.x(),object.y(),width,height));}
    public void rotateSelectedObject(double degrees){editSelectedObject(object->object.withRotation(degrees));}
    public void rotateSelectedBy(double degrees){editSelectedObject(object->object.withRotation(object.rotation()+degrees));}
    public void moveSelectedObject(double x,double y){editSelectedObject(object->object.geometry(x,y,object.width(),object.height()));}
    public void alignSelected(String alignment){
        if(!List.of("left","center","right","top","middle","bottom").contains(alignment))throw new IllegalArgumentException(alignment);
        int width=getPresentation().width(),height=getPresentation().height();
        editSelectedObject(object->{double x=switch(alignment){case "left"->0;case "center"->(width-object.width())/2;case "right"->width-object.width();default->object.x();};
            double y=switch(alignment){case "top"->0;case "middle"->(height-object.height())/2;case "bottom"->height-object.height();default->object.y();};
            return object.geometry(x,y,object.width(),object.height());});
    }
    public void distributeObjectsHorizontally(){editable();int index=session.selectedSlide();session.edit(doc->{PptSlide slide=doc.slides().get(index);
        if(slide.objects().size()<3)return doc;List<PptObject> sorted=new ArrayList<>(slide.objects());sorted.sort(Comparator.comparingDouble(PptObject::x));
        double left=sorted.getFirst().x(),right=sorted.getLast().x();double interval=(right-left)/(sorted.size()-1);
        Map<String,PptObject> moved=new HashMap<>();for(int i=0;i<sorted.size();i++){PptObject object=sorted.get(i);moved.put(object.id(),object.geometry(left+i*interval,object.y(),object.width(),object.height()));}
        return doc.withSlide(index,slide.withObjects(slide.objects().stream().map(o->moved.get(o.id())).toList()));});}
    public void applyTheme(String theme){editable();Color background,foreground,shape;
        switch(theme){case "light"->{background=Color.WHITE;foreground=Color.BLACK;shape=new Color(52,116,210);}
            case "dark"->{background=new Color(22,28,40);foreground=Color.WHITE;shape=new Color(68,136,218);}
            case "blue"->{background=new Color(226,238,252);foreground=new Color(15,41,82);shape=new Color(31,91,175);}
            default->throw new IllegalArgumentException(theme);}
        session.edit(doc->{List<PptSlide> slides=new ArrayList<>();for(PptSlide slide:doc.slides()){
            List<PptObject> objects=new ArrayList<>();for(PptObject object:slide.objects()){
                if(object.kind()==PptObject.Kind.TEXT)objects.add(object.withForeground(foreground));
                else if(object.kind()==PptObject.Kind.RECTANGLE||object.kind()==PptObject.Kind.ELLIPSE)objects.add(object.withFill(shape));
                else objects.add(object);
            }slides.add(slide.withBackground(background).withObjects(objects));}
            return new Presentation(doc.width(),doc.height(),slides);});
    }
    private void editSelectedObject(java.util.function.UnaryOperator<PptObject> operation){editable();canvas.commitEditing();String id=session.selectedObjectId();if(id==null)return;int index=session.selectedSlide();
        session.edit(doc->{PptSlide slide=doc.slides().get(index);for(PptObject object:slide.objects())if(object.id().equals(id))return doc.withSlide(index,slide.replaceObject(id,operation.apply(object)));return doc;});}
    public void bringSelectedToFront(){reorderSelected(true);}
    public void sendSelectedToBack(){reorderSelected(false);}
    private void reorderSelected(boolean front){editable();String id=session.selectedObjectId();if(id==null)return;int index=session.selectedSlide();
        session.edit(doc->{PptSlide slide=doc.slides().get(index);List<PptObject> list=new ArrayList<>(slide.objects());
            PptObject found=null;for(PptObject object:list)if(object.id().equals(id)){found=object;break;}if(found==null)return doc;
            list.remove(found);list.add(front?list.size():0,found);return doc.withSlide(index,slide.withObjects(list));});}
    public void editSelectedText(){String id=session.selectedObjectId();if(id==null)return;for(PptObject object:canvas.currentSlide().objects())if(object.id().equals(id)){editText(object);return;}}
    private void editText(PptObject object){canvas.startEditing(object);}
    public void setSelectedFontSize(int size){editable();canvas.formatText(style->style.size(size));}
    public double getTextPointScale(){return textPointScale;}
    public PptText.Style getSelectedTextStyle(){return canvas.selectedTextStyle();}
    public void setSelectedFontSizePoints(double size){editable();if(!Double.isFinite(size)||size<=0)throw new IllegalArgumentException("Invalid font size");double scale=getTextPointScale();canvas.formatText(style->style.size(size*scale));}
    public void formatSelectedText(java.util.function.UnaryOperator<PptText.Style> operation){editable();canvas.formatText(operation);}
    public void alignSelectedText(String alignment){editable();canvas.alignText(alignment);}
    public void chooseTextColor(){editable();chooseColor("Cor do texto",Color.BLACK).ifPresent(color->canvas.formatText(style->style.color(color)));}
    public void insertTable(int rows,int columns){editable();canvas.commitEditing();addObject(PptObject.table(rows,columns,120,160,900,360));}
    public void chooseInsertTable(){active(PowerPointDialogProvider.class).orElse(defaultDialogs).chooseTableSize(this).ifPresent(size->insertTable(size.height,size.width));}
    public void insertConnector(){editable();canvas.commitEditing();addObject(PptObject.connector(180,240,500,240));}
    public void editSelectedTable(java.util.function.UnaryOperator<PptTable> operation){canvas.commitEditing();try{editSelectedObject(object->object.kind()==PptObject.Kind.TABLE?object.withTable(operation.apply(object.visual().table())):object);}catch(IllegalArgumentException error){active(PowerPointDialogProvider.class).orElse(defaultDialogs).message(this,"Seleção da tabela","Selecione um intervalo válido, incluindo as células mescladas por inteiro.");}}
    public void insertTableRow(){editSelectedTable(t->t.insertRow(Math.min(canvas.selectedRow()+1,t.rows().size())));}
    public void insertTableColumn(){editSelectedTable(t->t.insertColumn(Math.min(canvas.selectedColumn()+1,t.columns().size())));}
    public void deleteTableRow(){editSelectedTable(t->t.rows().size()>1?t.deleteRow(Math.min(canvas.selectedRow(),t.rows().size()-1)):t);}
    public void deleteTableColumn(){editSelectedTable(t->t.columns().size()>1?t.deleteColumn(Math.min(canvas.selectedColumn(),t.columns().size()-1)):t);}
    public void splitTableCells(){editSelectedTable(t->t.split(canvas.selectedRow(),canvas.selectedColumn()));}
    public void mergeTableCells(){editSelectedTable(t->t.merge(canvas.selectionTop(),canvas.selectionLeft(),canvas.selectionRows(),canvas.selectionColumns()));}
    public void setTableColumnWidth(double width){if(!Double.isFinite(width)||width<=0)throw new IllegalArgumentException();editSelectedTable(t->{List<Double> widths=new ArrayList<>(t.columns());widths.set(Math.min(canvas.selectedColumn(),widths.size()-1),width);return new PptTable(widths,t.heights(),t.rows());});}
    public void setTableRowHeight(double height){if(!Double.isFinite(height)||height<=0)throw new IllegalArgumentException();editSelectedTable(t->{List<Double> heights=new ArrayList<>(t.heights());heights.set(Math.min(canvas.selectedRow(),heights.size()-1),height);return new PptTable(t.columns(),heights,t.rows());});}
    public void chooseTextLayout(){canvas.commitEditing();PptObject object=canvas.currentSlide().objects().stream().filter(o->o.id().equals(session.selectedObjectId())).findFirst().orElse(null);if(object==null||!object.hasText()&&object.kind()!=PptObject.Kind.TABLE)return;
        PptText text=object.kind()==PptObject.Kind.TABLE?object.visual().table().rows().get(canvas.selectedRow()).get(canvas.selectedColumn()).text():object.styledText();
        active(PowerPointDialogProvider.class).orElse(defaultDialogs).editTextLayout(this,text).ifPresent(value->editSelectedObject(o->{if(o.kind()!=PptObject.Kind.TABLE)return o.withStyledText(value);var table=o.visual().table();return o.withTable(table.cell(canvas.selectedRow(),canvas.selectedColumn(),table.rows().get(canvas.selectedRow()).get(canvas.selectedColumn()).withText(value)));}));
    }
    public void chooseCellBorderColor(){chooseColor("Cor da borda",Color.GRAY).ifPresent(color->editSelectedTable(t->{int r=canvas.selectedRow(),c=canvas.selectedColumn();var cell=t.rows().get(r).get(c);return t.cell(r,c,cell.withBorder(new PptStroke(color,1,"solid","none","none")));}));}
    public void setSelectedCellBorder(double width,String dash){editSelectedTable(t->{int r=canvas.selectedRow(),c=canvas.selectedColumn();var cell=t.rows().get(r).get(c);Color color=cell.top().color();return t.cell(r,c,cell.withBorder(new PptStroke(color.getAlpha()==0?Color.GRAY:color,width,dash,"none","none")));});}
    public void setSelectedStroke(double width,String dash,String head,String tail){editSelectedObject(o->{PptObject value=o.visual()==null?o.withStyledText(o.styledText()):o;Color color=value.visual().stroke().color();return value.withVisual(value.visual().withStroke(new PptStroke(color.getAlpha()==0?Color.BLACK:color,width,dash,head,tail)));});}
    public void chooseStrokeColor(){editable();chooseColor("Cor da linha",Color.BLACK).ifPresent(color->editSelectedObject(o->{PptObject value=o.visual()==null?o.withStyledText(o.styledText()):o;var stroke=value.visual().stroke();return value.withVisual(value.visual().withStroke(new PptStroke(color,Math.max(1,stroke.width()),stroke.dash(),stroke.head(),stroke.tail())));}));}
    public List<String> getImportDiagnostics(){return origin==null?List.of():origin.diagnostics();}
    public void showImportDiagnostics(){active(PowerPointDialogProvider.class).orElse(defaultDialogs).message(this,"Compatibilidade",String.join("<br>",getImportDiagnostics()));}
    public void setTransition(String transition){editable();if(!List.of("cut","fade","wipe","push").contains(transition))throw new IllegalArgumentException(transition);
        int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).withTransition(transition)));}
    public void addAnimation(PptAnimation.Effect effect){editable();String id=session.selectedObjectId();if(id==null)return;int index=session.selectedSlide();
        session.edit(doc->{PptSlide slide=doc.slides().get(index);List<PptAnimation> list=new ArrayList<>(slide.animations());list.add(PptAnimation.create(id,effect));return doc.withSlide(index,slide.withAnimations(list));});}
    private void showAnimationSettings(){int index=animationList.getSelectedIndex();List<PptAnimation> animations=canvas.currentSlide().animations();if(index<0||index>=animations.size())return;
        PptAnimation value=animations.get(index);animationStart.setSelectedItem(value.start());animationDuration.setValue(value.durationMs());animationDelay.setValue(value.delayMs());animationRepeat.setValue(value.repeat());}
    private void applyAnimationSettings(){editable();int selected=animationList.getSelectedIndex();if(selected<0)return;int index=session.selectedSlide();
        session.edit(doc->{PptSlide slide=doc.slides().get(index);List<PptAnimation> list=new ArrayList<>(slide.animations());PptAnimation old=list.get(selected);
            list.set(selected,new PptAnimation(old.id(),old.targetId(),old.effect(),(PptAnimation.Start)animationStart.getSelectedItem(),
                    (int)animationDuration.getValue(),(int)animationDelay.getValue(),(int)animationRepeat.getValue(),old.direction()));
            return doc.withSlide(index,slide.withAnimations(list));});}
    private void moveAnimation(int change){editable();int selected=animationList.getSelectedIndex(),index=session.selectedSlide();int to=selected+change;
        if(selected<0||to<0||to>=canvas.currentSlide().animations().size())return;
        session.edit(doc->{PptSlide slide=doc.slides().get(index);List<PptAnimation> list=new ArrayList<>(slide.animations());Collections.swap(list,selected,to);return doc.withSlide(index,slide.withAnimations(list));});animationList.setSelectedIndex(to);}
    private void removeAnimation(){editable();int selected=animationList.getSelectedIndex(),index=session.selectedSlide();if(selected<0)return;
        session.edit(doc->{PptSlide slide=doc.slides().get(index);List<PptAnimation> list=new ArrayList<>(slide.animations());list.remove(selected);return doc.withSlide(index,slide.withAnimations(list));});}
    private Optional<Color> chooseColor(String title,Color current){return active(PowerPointDialogProvider.class).orElse(defaultDialogs).chooseColor(this,title,current);}
    public void chooseBackground(){editable();Optional<Color> color=chooseColor("Cor de fundo",canvas.currentSlide().background());if(color.isEmpty())return;int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).withBackground(color.get())));}
    public void chooseObjectFill(){editable();canvas.commitEditing();String id=session.selectedObjectId();if(id==null)return;Optional<Color> color=chooseColor("Cor do objeto",Color.BLUE);if(color.isEmpty())return;int index=session.selectedSlide();session.edit(doc->{PptSlide slide=doc.slides().get(index);for(PptObject object:slide.objects())if(object.id().equals(id))return doc.withSlide(index,slide.replaceObject(id,object.kind()==PptObject.Kind.TABLE?object.withTable(object.visual().table().cell(canvas.selectedRow(),canvas.selectedColumn(),object.visual().table().rows().get(canvas.selectedRow()).get(canvas.selectedColumn()).withFill(color.get()))):object.withFill(color.get())));return doc;});}
    public void chooseInsertMedia(PptObject.Kind kind){editable();Optional<Path> choice=active(PowerPointFileDialogProvider.class).orElse(defaultFiles).chooseMedia(this,kind);if(choice.isEmpty())return;
        try{Path path=choice.get();String mime=Files.probeContentType(path);insertMedia(kind,mime==null?"application/octet-stream":mime,Files.readAllBytes(path));}catch(IOException error){errorHandler.accept(error);}}
    public AutoCloseable addProvider(PowerPointProvider provider){ensureOpen();Objects.requireNonNull(provider);if(providers.containsKey(provider.id()))throw new IllegalArgumentException("Provider already registered: "+provider.id());
        Map<String,Action> provided=provider instanceof PowerPointCommandProvider cp?Map.copyOf(cp.commands(this)):Map.of();
        for(String id:provided.keySet())if(commands.containsKey(id))throw new IllegalArgumentException("Command already registered: "+id);
        AutoCloseable registration=provider.attach(this);JComponent toolbar;
        try{toolbar=provider instanceof PowerPointToolbarContributor contributor?contributor.createToolbar(this):null;}
        catch(RuntimeException error){try{registration.close();}catch(Exception closeError){error.addSuppressed(closeError);}throw error;}
        List<String> addedCommands=new ArrayList<>(provided.keySet());commands.putAll(provided);
        if(toolbar!=null){providerBar.add(toolbar);providerBar.revalidate();}
        providers.put(provider.id(),provider);JComponent finalToolbar=toolbar;
        registrations.put(provider.id(),()->{for(String id:addedCommands)commands.remove(id);if(finalToolbar!=null){providerBar.remove(finalToolbar);providerBar.revalidate();providerBar.repaint();}registration.close();});
        return ()->{providers.remove(provider.id());AutoCloseable close=registrations.remove(provider.id());if(close!=null)try{close.close();}catch(Exception error){errorHandler.accept(error);}};}
    public Optional<Action> command(String id){return Optional.ofNullable(commands.get(id));}
    public boolean invokeCommand(String id){Action action=commands.get(id);if(action==null||!action.isEnabled())return false;action.actionPerformed(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,id));return true;}
    private <T extends PowerPointProvider> Optional<T> active(Class<T> type){return providers.values().stream().filter(type::isInstance)
            .map(type::cast).max(Comparator.comparingInt(PowerPointProvider::priority));}
    public Optional<PowerPointMediaProvider> mediaProvider(String mime){return providers.values().stream()
            .filter(p->p instanceof PowerPointMediaProvider m&&m.supports(mime))
            .sorted(Comparator.comparingInt(PowerPointProvider::priority).reversed())
            .map(p->(PowerPointMediaProvider)p).findFirst();}
    public void startPresentation(){ensureOpen();if(presenting)return;presenting=true;canvas.setPresenting(true);applyConfig();beginSlide();canvas.requestFocusInWindow();}
    public void stopPresentation(){if(!presenting)return;presenting=false;animationTimer.stop();transitionTimer.stop();canvas.setTransition(null,"cut",1);scheduled.clear();animationProgress.clear();activeEffects.clear();canvas.setAnimationProgress(Map.of());canvas.setAnimationEffects(Map.of());stopMedia();canvas.setPresenting(false);applyConfig();canvas.requestFocusInWindow();}
    public boolean isPresenting(){return presenting;}
    public void advancePresentation(){if(!presenting)return;List<PptAnimation> list=canvas.currentSlide().animations();
        if(nextAnimation<list.size()){long now=System.currentTimeMillis();
            do{PptAnimation animation=list.get(nextAnimation++);activeEffects.put(animation.targetId(),animation.effect());
                long base=switch(animation.start()){
                    case ON_CLICK->now;
                    case WITH_PREVIOUS->previousAnimationStart==0?now:previousAnimationStart;
                    case AFTER_PREVIOUS->previousAnimationEnd==0?now:previousAnimationEnd;
                };
                long start=base+animation.delayMs();long end=start+(long)animation.durationMs()*animation.repeat();
                scheduled.add(new ScheduledAnimation(animation,start,end));previousAnimationStart=start;previousAnimationEnd=end;
            }while(nextAnimation<list.size()&&list.get(nextAnimation).start()!=PptAnimation.Start.ON_CLICK);
            canvas.setAnimationEffects(activeEffects);animationTimer.start();return;}
        if(session.selectedSlide()<session.getPresentation().slides().size()-1){PptSlide from=canvas.currentSlide();session.selectSlide(session.selectedSlide()+1);beginSlide();beginTransition(from);}else stopPresentation();
    }
    public void previousPresentation(){if(!presenting)return;if(session.selectedSlide()>0){PptSlide from=canvas.currentSlide();session.selectSlide(session.selectedSlide()-1);beginSlide();beginTransition(from);}}
    private void beginTransition(PptSlide from){String kind=canvas.currentSlide().transition();if("cut".equals(kind)){canvas.setTransition(null,"cut",1);return;}
        transitionFromSlide=from;transitionEpoch=System.currentTimeMillis();canvas.setTransition(from,kind,0);transitionTimer.start();}
    private long transitionEpoch;
    private void tickTransition(){double progress=(System.currentTimeMillis()-transitionEpoch)/450.0;
        if(progress>=1){transitionTimer.stop();transitionFromSlide=null;canvas.setTransition(null,"cut",1);}else canvas.setTransition(transitionFromSlide,canvas.currentSlide().transition(),progress);}
    private PptSlide transitionFromSlide;
    private void beginSlide(){animationTimer.stop();scheduled.clear();nextAnimation=0;animationEpoch=System.currentTimeMillis();previousAnimationStart=0;previousAnimationEnd=0;animationProgress.clear();activeEffects.clear();
        for(PptAnimation a:canvas.currentSlide().animations())if(isEntrance(a.effect())){animationProgress.put(a.targetId(),-1.0);activeEffects.put(a.targetId(),a.effect());}
        canvas.setAnimationProgress(animationProgress);canvas.setAnimationEffects(activeEffects);stopMedia();playSlideMedia();}
    private static boolean isEntrance(PptAnimation.Effect effect){return switch(effect){case APPEAR,FADE_IN,FLY_IN,WIPE_IN,SPLIT_IN,ZOOM_IN->true;default->false;};}
    private void tickAnimation(){long now=System.currentTimeMillis();boolean active=false;
        for(ScheduledAnimation event:scheduled){PptAnimation a=event.animation();double progress=event.endMs()==event.startMs()?1.0:(now-event.startMs())/(double)(event.endMs()-event.startMs());
            if(progress<0){active=true;continue;}progress=Math.max(0,Math.min(1,progress));if(progress<1)active=true;
            animationProgress.put(a.targetId(),switch(a.effect()){case DISAPPEAR,FADE_OUT,FLY_OUT,WIPE_OUT,SPLIT_OUT,ZOOM_OUT,TRANSPARENCY->1-progress;default->progress;});}
        canvas.setAnimationProgress(animationProgress);if(!active)animationTimer.stop();}
    private void playSlideMedia(){for(PptObject object:canvas.currentSlide().objects())if(object.kind()==PptObject.Kind.AUDIO||object.kind()==PptObject.Kind.VIDEO){
        mediaProvider(object.mimeType()).ifPresent(provider->{try{
            long generation=mediaGeneration;
            PowerPointMediaProvider.Player player=provider.open(object.mimeType(),object.data(),state->{},frame->SwingUtilities.invokeLater(()->{
                if(presenting&&mediaGeneration==generation)canvas.setMediaFrame(object.id(),frame);
            }));
            activeMedia.add(player);player.play();
        }catch(IOException error){errorHandler.accept(error);}});}}
    private void stopMedia(){mediaGeneration++;for(PowerPointMediaProvider.Player player:List.copyOf(activeMedia)){player.stop();player.close();}activeMedia.clear();canvas.setMediaFrame(null,null);}
    public PowerPointTask<PptxCodec.ImportResult> open(Path path){return open(path,false);}
    public PowerPointTask<PptxCodec.ImportResult> open(Path path,boolean discardChanges){
        ensureOpen();canvas.commitEditing();Objects.requireNonNull(path);if(session.isDirty()&&!discardChanges)throw new IllegalStateException("Unsaved changes");
        long revision=session.revision();PowerPointTask<PptxCodec.ImportResult> task=new PowerPointTask<>();
        task.attach(files.submit(()->{try(InputStream input=Files.newInputStream(path)){
            PptxCodec.ImportResult result=services.pptx().read(input);task.progress(90);
            SwingUtilities.invokeLater(()->{if(task.isCancelled())return;if(session.revision()!=revision){task.fail(new IllegalStateException("Presentation changed while opening"));return;}
                stopPresentation();origin=result;textPointScale=services.pptx().textPointScale(result,result.presentation());currentFile=path;session.replace(result.presentation());applyConfig();task.complete(result);});
        }catch(Throwable error){task.fail(error);}}));return task;
    }
    public PowerPointTask<Path> save(){if(currentFile==null)throw new IllegalStateException("No current file");return save(currentFile);}
    public PowerPointTask<Path> save(Path path){ensureOpen();canvas.commitEditing();Objects.requireNonNull(path);Presentation snapshot=session.getPresentation();long revision=session.revision();PptxCodec.ImportResult source=origin;
        PowerPointTask<Path> task=new PowerPointTask<>();task.attach(files.submit(()->{
            Path parent=path.toAbsolutePath().getParent(),temp=null;
            try{temp=Files.createTempFile(parent,"powerpoint-",".pptx.tmp");
                try(OutputStream out=Files.newOutputStream(temp)){services.pptx().write(snapshot,source,out);}
                PptxCodec.ImportResult savedOrigin=new PptxCodec.ImportResult(snapshot,Files.readAllBytes(temp),
                        source==null||source.editable(),source==null?List.of():source.diagnostics());
                task.progress(90);if(!task.beginCommit()){Files.deleteIfExists(temp);return;}
                Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                SwingUtilities.invokeLater(()->{currentFile=path;origin=savedOrigin;session.markSaved(revision);task.complete(path);});
            }catch(Throwable error){task.fail(error);if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
        }));return task;
    }
    public PowerPointTask<Path> export(Path path,String providerId){ensureOpen();PowerPointProvider provider=providers.get(providerId);
        if(!(provider instanceof PowerPointExportProvider exporter))throw new IllegalArgumentException("Export provider not registered: "+providerId);
        Presentation snapshot=session.getPresentation();PowerPointTask<Path> task=new PowerPointTask<>();task.attach(files.submit(()->{
            Path temp=null;try{temp=Files.createTempFile(path.toAbsolutePath().getParent(),"powerpoint-export-",".tmp");
                try(OutputStream output=Files.newOutputStream(temp)){exporter.export(snapshot,output);}task.progress(90);
                if(!task.beginCommit()){Files.deleteIfExists(temp);return;}Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                task.complete(path);
            }catch(Throwable error){task.fail(error);if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
        }));return task;
    }
    public void chooseOpen(){
        canvas.commitEditing();
        if(session.isDirty()&&!active(PowerPointDialogProvider.class).orElse(defaultDialogs).confirmDiscardChanges(this))return;
        active(PowerPointFileDialogProvider.class).orElse(defaultFiles).chooseOpen(this)
                .ifPresent(path->open(path,true).completion().exceptionally(error->{errorHandler.accept(error);return null;}));
    }
    public void chooseSave(){active(PowerPointFileDialogProvider.class).orElse(defaultFiles).chooseSave(this,currentFile)
            .ifPresent(path->save(path).completion().exceptionally(error->{errorHandler.accept(error);return null;}));}
    @Override public void removeNotify(){stopPresentation();animationTimer.stop();stopMedia();super.removeNotify();}
    @Override public void close(){if(closed)return;stopPresentation();canvas.cancelEditing();closed=true;animationTimer.stop();stopMedia();files.shutdownNow();
        for(AutoCloseable registration:List.copyOf(registrations.values()))try{registration.close();}catch(Exception error){errorHandler.accept(error);}registrations.clear();providers.clear();}
    private final class ThumbnailRenderer extends JPanel implements ListCellRenderer<String> {
        private String label;private int index;private boolean selected;
        ThumbnailRenderer(){setOpaque(false);setPreferredSize(new Dimension(210,UIScale.scale(132)));}
        @Override public Component getListCellRendererComponent(JList<? extends String> list,String value,int index,boolean selected,boolean focus){this.label=value;this.index=index;this.selected=selected;return this;}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int width=getWidth(),cardX=7,cardY=5,cardW=Math.max(30,width-14),cardH=Math.max(40,getHeight()-9);
            g.setColor(selected?(UiTokens.isDarkTheme()?new Color(0x30445F):new Color(0xE8F2FF)):UiTokens.surface());
            g.fill(new RoundRectangle2D.Double(cardX,cardY,cardW,cardH,14,14));
            g.setColor(selected?(UiTokens.isDarkTheme()?new Color(0x7CB4FA):new Color(0x3D86D8)):UiTokens.border());
            g.setStroke(new BasicStroke(selected?1.7f:1f));g.draw(new RoundRectangle2D.Double(cardX+.5,cardY+.5,cardW-1,cardH-1,14,14));
            Presentation doc=session.getPresentation();if(index>=doc.slides().size())return;
            int previewW=Math.max(30,Math.min(cardW-22,190));
            double ratio=doc.height()/(double)doc.width();int previewH=(int)Math.min(82,previewW*ratio);
            previewW=(int)(previewH/ratio);int previewX=(width-previewW)/2,previewY=12;
            g.setColor(new Color(0,0,0,UiTokens.isDarkTheme()?65:25));
            g.fillRoundRect(previewX+2,previewY+3,previewW,previewH,3,3);
            services.renderer().render(g,doc,doc.slides().get(index),new Rectangle2D.Double(previewX,previewY,previewW,previewH),Map.of());
            g.setColor(UiTokens.foreground());g.setFont(UiTokens.fontBold());
            String title=(index+1)+"  "+doc.slides().get(index).title();
            FontMetrics metrics=g.getFontMetrics();int available=cardW-22;
            while(title.length()>4&&metrics.stringWidth(title)>available)title=title.substring(0,title.length()-2)+"…";
            g.drawString(title,cardX+12,cardY+cardH-12);
        }finally{g.dispose();}}
    }
}
