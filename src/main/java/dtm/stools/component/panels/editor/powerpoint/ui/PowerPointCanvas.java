package dtm.stools.component.panels.editor.powerpoint.ui;

import dtm.stools.component.panels.editor.powerpoint.api.PowerPointSession;
import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.powerpoint.render.PowerPointRenderer;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

public final class PowerPointCanvas extends JComponent {
    private final PowerPointSession session;
    private final PowerPointRenderer renderer;
    private boolean presenting;
    private double zoom=1;
    private Map<String,Double> progress=Map.of();
    private Map<String,PptAnimation.Effect> activeEffects=Map.of();
    private Map<String,BufferedImage> mediaFrames=Map.of();
    private PptSlide transitionFrom;
    private String transitionKind="cut";
    private double transitionProgress=1;
    private Rectangle2D slideBounds=new Rectangle2D.Double();
    private Point dragStart;
    private PptObject dragObject;
    private PptObject dragPreview;
    private boolean resizing;
    private int endpoint=-1;
    private int selectedRow,selectedColumn,endRow,endColumn;
    public int selectionTop(){return Math.min(selectedRow,endRow);}
    public int selectionLeft(){return Math.min(selectedColumn,endColumn);}
    public int selectionRows(){return Math.abs(selectedRow-endRow)+1;}
    public int selectionColumns(){return Math.abs(selectedColumn-endColumn)+1;}
    private PptInlineTextEditor inline;
    private java.util.function.BiFunction<PptText,Double,PptInlineTextEditor> textEditorFactory=PptInlineTextEditor::new;
    public void setTextEditorFactory(java.util.function.BiFunction<PptText,Double,PptInlineTextEditor> factory){textEditorFactory=java.util.Objects.requireNonNull(factory);}
    private PptObject editingObject;
    private int editingSlide;
    private java.awt.geom.Rectangle2D editingBounds;
    private Runnable textSelectionListener=()->{};
    public void setTextSelectionListener(Runnable listener){textSelectionListener=listener;}
    public PptText.Style selectedTextStyle(){
        if(inline!=null)return inline.currentStyle();PptObject object=currentSlide().objects().stream().filter(o->o.id().equals(session.selectedObjectId())).findFirst().orElse(null);
        if(object==null)return null;if(object.kind()==PptObject.Kind.TABLE)return object.visual().table().rows().get(selectedRow).get(selectedColumn).text().firstStyle();return object.hasText()?object.styledText().firstStyle():null;
    }
    public boolean isEditingText(){return inline!=null;}
    public int selectedRow(){return selectedRow;}
    public int selectedColumn(){return selectedColumn;}
    public void formatText(java.util.function.UnaryOperator<PptText.Style> operation){
        if(inline!=null){inline.format(operation);return;}
        String id=session.selectedObjectId();PptObject o=currentSlide().objects().stream().filter(v->v.id().equals(id)).findFirst().orElse(null);if(o==null||session.isReadOnly())return;
        if(o.kind()==PptObject.Kind.TABLE){var table=o.visual().table();selectedRow=Math.min(selectedRow,table.rows().size()-1);selectedColumn=Math.min(selectedColumn,table.columns().size()-1);var cell=table.rows().get(selectedRow).get(selectedColumn);change(o,o.withTable(table.cell(selectedRow,selectedColumn,cell.withText(cell.text().mapStyles(operation)))));}
        else if(o.hasText())change(o,o.withStyledText(o.styledText().mapStyles(operation)));
    }
    public void alignText(String alignment){
        if(inline!=null){inline.align(alignment);return;}PptObject o=currentSlide().objects().stream().filter(v->v.id().equals(session.selectedObjectId())).findFirst().orElse(null);if(o==null)return;
        PptText t=o.kind()==PptObject.Kind.TABLE?o.visual().table().rows().get(selectedRow).get(selectedColumn).text():o.styledText();
        PptText next=new PptText(t.paragraphs().stream().map(p->new PptText.Paragraph(p.runs(),alignment,p.before(),p.after(),p.lineSpacing())).toList(),t.left(),t.top(),t.right(),t.bottom(),t.anchor());
        if(o.kind()==PptObject.Kind.TABLE){var table=o.visual().table();change(o,o.withTable(table.cell(selectedRow,selectedColumn,table.rows().get(selectedRow).get(selectedColumn).withText(next))));}else change(o,o.withStyledText(next));
    }
    private void change(PptObject before,PptObject after){int index=session.selectedSlide();session.edit(doc->doc.withSlide(index,doc.slides().get(index).replaceObject(before.id(),after)));}
    private boolean editableText(PptObject object){return object.hasText()||java.util.Set.of(PptObject.Kind.TABLE,PptObject.Kind.RECTANGLE,PptObject.Kind.ROUND_RECTANGLE,PptObject.Kind.ELLIPSE,PptObject.Kind.DIAMOND).contains(object.kind());}
    public void startEditing(PptObject object){
        endRow=selectedRow;endColumn=selectedColumn;
        if(session.isReadOnly()||presenting||!editableText(object))return;commitEditing();
        editingObject=object;editingSlide=session.selectedSlide();PptText text;
        if(object.kind()==PptObject.Kind.TABLE){var table=object.visual().table();selectedRow=Math.min(selectedRow,table.rows().size()-1);selectedColumn=Math.min(selectedColumn,table.columns().size()-1);text=table.rows().get(selectedRow).get(selectedColumn).text();editingBounds=renderer.cellBounds(object,selectedRow,selectedColumn);}
        else {text=object.styledText();editingBounds=new Rectangle2D.Double(object.x(),object.y(),object.width(),object.height());}
        double scale=slideBounds.getWidth()/session.getPresentation().width();inline=textEditorFactory.apply(text,Math.max(.01,scale));add(inline);positionInline();
        inline.addCaretListener(event->textSelectionListener.run());
        inline.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"ppt.cancel");inline.getActionMap().put("ppt.cancel",new AbstractAction(){public void actionPerformed(ActionEvent e){cancelEditing();}});
        inline.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK),"ppt.commit");inline.getActionMap().put("ppt.commit",new AbstractAction(){public void actionPerformed(ActionEvent e){commitEditing();}});
        if(object.kind()==PptObject.Kind.TABLE)for(int direction:new int[]{-1,1}){String key="ppt.cell"+direction;inline.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,direction<0?InputEvent.SHIFT_DOWN_MASK:0),key);inline.getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){navigateCell(direction);}});}
        inline.addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){Component next=e.getOppositeComponent();if(next==null)return;SwingUtilities.invokeLater(()->{if(inline!=null&&!SwingUtilities.isDescendingFrom(next,PowerPointCanvas.this)&&!isEditorControl(next))commitEditing();});}});
        inline.requestFocusInWindow();revalidate();repaint();
    }
    private boolean isEditorControl(Component component){Container editor=getParent();return editor!=null&&SwingUtilities.isDescendingFrom(component,editor);}
    private void navigateCell(int direction){PptObject object=editingObject;commitEditing();PptObject current=currentSlide().objects().stream().filter(o->o.id().equals(object.id())).findFirst().orElse(null);if(current==null)return;
        int count=current.visual().table().columns().size(),total=count*current.visual().table().rows().size(),index=selectedRow*count+selectedColumn;
        for(int i=0;i<total;i++){index=Math.floorMod(index+direction,total);selectedRow=index/count;selectedColumn=index%count;if(!current.visual().table().rows().get(selectedRow).get(selectedColumn).covered())break;}startEditing(current);
    }
    public void commitEditing(){
        if(inline==null)return;PptText text=inline.value();PptObject before=editingObject;int index=editingSlide;cancelEditing();
        PptText previous=before.kind()==PptObject.Kind.TABLE?before.visual().table().rows().get(selectedRow).get(selectedColumn).text():before.styledText();if(previous.equals(text))return;
        PptObject after=before.kind()==PptObject.Kind.TABLE?before.withTable(before.visual().table().cell(selectedRow,selectedColumn,before.visual().table().rows().get(selectedRow).get(selectedColumn).withText(text))):before.withStyledText(text);
        session.edit(doc->{PptSlide slide=doc.slides().get(index);if(slide.objects().stream().noneMatch(o->o==before))return doc;return doc.withSlide(index,slide.replaceObject(before.id(),after));});
    }
    public void cancelEditing(){if(inline!=null){remove(inline);inline=null;editingObject=null;revalidate();repaint();}}
    private void positionInline(){if(inline==null)return;double scale=slideBounds.getWidth()/session.getPresentation().width();inline.rescale(scale);
        int x=(int)Math.round(slideBounds.getX()+editingBounds.getX()*scale),y=(int)Math.round(slideBounds.getY()+editingBounds.getY()*scale);inline.setBounds(x,y,Math.max(1,(int)Math.round(editingBounds.getWidth()*scale)),Math.max(1,(int)Math.round(editingBounds.getHeight()*scale)));
        PptText t=editingObject.kind()==PptObject.Kind.TABLE?editingObject.visual().table().rows().get(selectedRow).get(selectedColumn).text():editingObject.styledText();
        double available=editingBounds.getHeight()-t.top()-t.bottom(),height=renderer.textLayout().layout(t,editingBounds.getWidth(),editingBounds.getHeight()).height(),top=t.top();if("ctr".equals(t.anchor()))top+=Math.max(0,(available-height)/2);else if("b".equals(t.anchor()))top+=Math.max(0,available-height);
        inline.setBorder(BorderFactory.createEmptyBorder((int)Math.round(top*scale),(int)Math.round(t.left()*scale),(int)Math.round(t.bottom()*scale),(int)Math.round(t.right()*scale)));
    }
    private void selectCell(PptObject object,Point point,boolean extend){var table=object.visual().table();for(int r=0;r<table.rows().size();r++)for(int c=0;c<table.columns().size();c++)if(!table.rows().get(r).get(c).covered()&&renderer.cellBounds(object,r,c).contains(point)){if(extend){endRow=r;endColumn=c;}else{selectedRow=endRow=r;selectedColumn=endColumn=c;}return;}}

    private Consumer<PptObject> editText=object->{};
    public PowerPointCanvas(PowerPointSession session,PowerPointRenderer renderer){
        this.session=session;this.renderer=renderer;setFocusable(true);setOpaque(true);setLayout(null);
        session.addListener(()->{if(inline!=null&&(session.selectedSlide()!=editingSlide||!editingObject.id().equals(session.selectedObjectId())))commitEditing();
            PptObject selected=currentSlide().objects().stream().filter(o->o.id().equals(session.selectedObjectId())).findFirst().orElse(null);if(selected!=null&&selected.kind()==PptObject.Kind.TABLE){var table=selected.visual().table();selectedRow=Math.min(selectedRow,table.rows().size()-1);endRow=Math.min(endRow,table.rows().size()-1);selectedColumn=Math.min(selectedColumn,table.columns().size()-1);endColumn=Math.min(endColumn,table.columns().size()-1);}
        });
        addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                commitEditing();requestFocusInWindow();if(presenting)return;
                Point logical=logical(e.getPoint());
                PptSlide slide=currentSlide();
                for(int i=slide.objects().size()-1;i>=0;i--){
                    PptObject object=slide.objects().get(i);
                    if(renderer.hit(object,logical,6*session.getPresentation().width()/Math.max(1,slideBounds.getWidth()))){
                        session.selectObject(object.id());dragStart=logical;dragObject=object;
                        if(object.kind()==PptObject.Kind.TABLE)selectCell(object,logical,e.isShiftDown());
                        endpoint=-1;if(object.kind()==PptObject.Kind.CONNECTOR){var c=object.visual().connector();double tolerance=10*session.getPresentation().width()/Math.max(1,slideBounds.getWidth());if(logical.distance(object.x()+c.x1(),object.y()+c.y1())<tolerance)endpoint=0;else if(logical.distance(object.x()+c.x2(),object.y()+c.y2())<tolerance)endpoint=1;}
                        resizing=logical.x>=object.x()+object.width()-18&&logical.y>=object.y()+object.height()-18;
                        if(e.getClickCount()==2&&editableText(object)){dragStart=null;dragObject=null;startEditing(object);}
                        return;
                    }
                }
                session.selectObject(null);dragStart=null;dragObject=null;
            }
            @Override public void mouseReleased(MouseEvent e){
                if(dragObject!=null&&dragPreview!=null&&!session.isReadOnly()){
                    int slideIndex=session.selectedSlide();PptObject moved=endpoint>=0?snapConnector(dragPreview,endpoint):dragPreview;
                    session.edit(doc->doc.withSlide(slideIndex,doc.slides().get(slideIndex).replaceObject(dragObject.id(),moved)));
                }
                dragStart=null;dragObject=null;dragPreview=null;resizing=false;endpoint=-1;repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                if(dragStart==null||dragObject==null||session.isReadOnly())return;
                Point p=logical(e.getPoint());double dx=p.x-dragStart.x,dy=p.y-dragStart.y;
                if(dx==0&&dy==0)return;
                if(endpoint>=0){var c=dragObject.visual().connector();double x1=dragObject.x()+c.x1(),y1=dragObject.y()+c.y1(),x2=dragObject.x()+c.x2(),y2=dragObject.y()+c.y2();if(endpoint==0){x1=p.x;y1=p.y;}else{x2=p.x;y2=p.y;}
                    PptObject line=PptObject.connector(x1,y1,x2,y2);var points=line.visual().connector();dragPreview=line.withId(dragObject.id()).withVisual(line.visual().withStroke(dragObject.visual().stroke()).withConnector(new PptVisual.Connector(points.x1(),points.y1(),points.x2(),points.y2(),endpoint==0?null:c.startId(),endpoint==1?null:c.endId(),c.startSite(),c.endSite())));repaint();return;}
                dragPreview=resizing?dragObject.geometry(dragObject.x(),dragObject.y(),Math.max(20,dragObject.width()+dx),Math.max(20,dragObject.height()+dy))
                        :dragObject.geometry(dragObject.x()+dx,dragObject.y()+dy,dragObject.width(),dragObject.height());
                repaint();
            }
        });
    }
    public void setEditTextHandler(Consumer<PptObject> handler){editText=handler;}
    private PptObject snapConnector(PptObject object,int endpoint){
        var c=object.visual().connector();double x=object.x()+(endpoint==0?c.x1():c.x2()),y=object.y()+(endpoint==0?c.y1():c.y2());double best=12*session.getPresentation().width()/Math.max(1,slideBounds.getWidth());PptObject target=null;int site=0;double snappedX=x,snappedY=y;
        for(PptObject candidate:currentSlide().objects())if(!candidate.id().equals(object.id())&&candidate.kind()!=PptObject.Kind.CONNECTOR)for(int i=0;i<4;i++){
            double px=candidate.x()+candidate.width()/2,py=candidate.y()+candidate.height()/2;if(i==0)py=candidate.y();else if(i==1)px=candidate.x();else if(i==2)py=candidate.y()+candidate.height();else px=candidate.x()+candidate.width();
            var point=java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(candidate.rotation()),candidate.x()+candidate.width()/2,candidate.y()+candidate.height()/2).transform(new java.awt.geom.Point2D.Double(px,py),null);double distance=point.distance(x,y);if(distance<best){best=distance;target=candidate;site=i;snappedX=point.getX();snappedY=point.getY();}
        }
        if(target==null)return object;
        double x1=object.x()+c.x1(),y1=object.y()+c.y1(),x2=object.x()+c.x2(),y2=object.y()+c.y2();if(endpoint==0){x1=snappedX;y1=snappedY;}else{x2=snappedX;y2=snappedY;}
        PptObject linked=PptObject.connector(x1,y1,x2,y2);var points=linked.visual().connector();return linked.withId(object.id()).withVisual(linked.visual().withStroke(object.visual().stroke()).withConnector(new PptVisual.Connector(points.x1(),points.y1(),points.x2(),points.y2(),endpoint==0?target.id():c.startId(),endpoint==1?target.id():c.endId(),endpoint==0?site:c.startSite(),endpoint==1?site:c.endSite())));
    }
    public void setPresenting(boolean value){commitEditing();presenting=value;repaint();}
    public void setZoom(double value){zoom=value;repaint();}
    public boolean isPresenting(){return presenting;}
    public void setAnimationProgress(Map<String,Double> value){progress=Map.copyOf(value);repaint();}
    public void setAnimationEffects(Map<String,PptAnimation.Effect> value){activeEffects=Map.copyOf(value);repaint();}
    public void setMediaFrame(String objectId,BufferedImage frame){mediaFrames=frame==null?Map.of():Map.of(objectId,frame);repaint();}
    public void setTransition(PptSlide from,String kind,double progress){transitionFrom=from;transitionKind=kind;transitionProgress=progress;repaint();}
    public PptSlide currentSlide(){return session.getPresentation().slides().get(session.selectedSlide());}
    public Point logical(Point point){
        Presentation doc=session.getPresentation();
        return new Point((int)Math.round((point.x-slideBounds.getX())*doc.width()/Math.max(1,slideBounds.getWidth())),
                (int)Math.round((point.y-slideBounds.getY())*doc.height()/Math.max(1,slideBounds.getHeight())));
    }
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setColor(UiTokens.background());g.fillRect(0,0,getWidth(),getHeight());
            Presentation doc=session.getPresentation();double margin=presenting?0:26;
            double scale=Math.min((getWidth()-2*margin)/(double)doc.width(),(getHeight()-2*margin)/(double)doc.height());
            scale=Math.max(0.01,scale)*(presenting?1:zoom);double w=doc.width()*scale,h=doc.height()*scale;
            slideBounds=new Rectangle2D.Double((getWidth()-w)/2,(getHeight()-h)/2,w,h);
            if(!presenting){g.setColor(new Color(0,0,0,45));g.fill(new Rectangle2D.Double(slideBounds.getX()+4,slideBounds.getY()+5,w,h));}
            PptSlide painted=currentSlide();if(dragPreview!=null)painted=painted.replaceObject(dragPreview.id(),dragPreview);
            if(inline!=null){PptObject hidden=editingObject;if(hidden.kind()==PptObject.Kind.TABLE){var table=hidden.visual().table();var cell=table.rows().get(selectedRow).get(selectedColumn);hidden=hidden.withTable(table.cell(selectedRow,selectedColumn,cell.withText(cell.text().withText(""))));}else hidden=hidden.withStyledText(hidden.styledText().withText(""));painted=painted.replaceObject(hidden.id(),hidden);}
            renderer.render(g,doc,painted,slideBounds,progress,mediaFrames,activeEffects);positionInline();
            if(transitionFrom!=null&&transitionProgress<1){
                Graphics2D old=(Graphics2D)g.create();
                try{
                    double p=Math.max(0,Math.min(1,transitionProgress));
                    switch(transitionKind){
                        case "fade" -> old.setComposite(AlphaComposite.SrcOver.derive((float)(1-p)));
                        case "wipe" -> old.clip(new Rectangle2D.Double(slideBounds.getX()+slideBounds.getWidth()*p,slideBounds.getY(),slideBounds.getWidth()*(1-p),slideBounds.getHeight()));
                        case "push" -> old.translate(-slideBounds.getWidth()*p,0);
                        default -> {}
                    }
                    renderer.render(old,doc,transitionFrom,slideBounds,Map.of());
                }finally{old.dispose();}
            }
            if(!presenting){
                String id=session.selectedObjectId();
                for(PptObject original:currentSlide().objects())if(original.id().equals(id)){
                    PptObject o=dragPreview!=null?dragPreview:original;
                    g.setColor(new Color(32,110,220));g.setStroke(new BasicStroke(2));
                    java.awt.geom.AffineTransform transform=new java.awt.geom.AffineTransform();transform.translate(slideBounds.getX(),slideBounds.getY());transform.scale(scale,scale);transform.rotate(Math.toRadians(o.rotation()),o.x()+o.width()/2,o.y()+o.height()/2);g.draw(transform.createTransformedShape(renderer.objectShape(o)));
                    if(o.kind()==PptObject.Kind.CONNECTOR){var c=o.visual().connector();for(double[] point:new double[][]{{c.x1(),c.y1()},{c.x2(),c.y2()}})g.fill(new Rectangle2D.Double(slideBounds.getX()+(o.x()+point[0])*scale-4,slideBounds.getY()+(o.y()+point[1])*scale-4,8,8));}
                    if(o.kind()==PptObject.Kind.TABLE){Rectangle2D first=renderer.cellBounds(o,selectionTop(),selectionLeft()),last=renderer.cellBounds(o,Math.min(o.visual().table().rows().size()-1,selectionTop()+selectionRows()-1),Math.min(o.visual().table().columns().size()-1,selectionLeft()+selectionColumns()-1));Rectangle2D range=first.createUnion(last);g.setColor(new Color(32,110,220,30));g.fill(new Rectangle2D.Double(slideBounds.getX()+range.getX()*scale,slideBounds.getY()+range.getY()*scale,range.getWidth()*scale,range.getHeight()*scale));}
                    g.fill(new Rectangle2D.Double(slideBounds.getX()+(o.x()+o.width())*scale-5,
                            slideBounds.getY()+(o.y()+o.height())*scale-5,10,10));
                }
            }
        }finally{g.dispose();}
    }
}
