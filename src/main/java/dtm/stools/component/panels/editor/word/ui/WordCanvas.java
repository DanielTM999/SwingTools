package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.layout.*;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordImageCache;
import dtm.stools.component.panels.editor.word.render.WordRenderer;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import javax.accessibility.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.font.TextHitInfo;
import java.awt.geom.*;
import java.awt.im.InputMethodRequests;
import java.text.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WordCanvas extends JComponent implements Scrollable,AutoCloseable,InputMethodRequests {
    private enum Drag { NONE, TEXT, RESIZE, MOVE, ROTATE, COLUMN }
    private record PageHit(WordLayout.Page page,float x,float y) {}
    private record Boundary(UUID tableId,int index,float x,float top,float bottom,float scale,int depth) {}
    private static final Color SELECTION=new Color(120,170,255,110),HANDLE=new Color(35,95,200);
    private final WordSession session;
    private final WordLayoutEngine engine;
    private final WordRenderer renderer;
    private final ProviderRegistration listener;
    private ExecutorService executor;
    private ExecutorService transientExecutor;
    private Future<?> pending;
    private WordLayout snapshot;
    private long generation;
    private long layoutGeneration,topRevealTicket;
    private Integer topRevealOffset;
    private double zoom=1;
    private WordViewMode viewMode=WordViewMode.PRINT_LAYOUT;
    private boolean closed;
    private boolean layoutDirty;
    private boolean visualPaused;
    private String composition="";
    private Consumer<Throwable> errorHandler=Throwable::printStackTrace;
    private Consumer<WordLayout.ObjectBox> objectHandler=box->{};
    private Consumer<String> linkHandler=link->{};
    private Consumer<WordLayout.Region> regionHandler=region->{};
    private Predicate<Transferable> pasteHandler=t->false;
    private Drag drag=Drag.NONE;
    private int handle=-1;
    private Point dragStart;
    private WordLayout.ObjectBox dragBox;
    private WordLayout.Page dragPage;
    private Rectangle2D.Float ghost;
    private float ghostAngle;
    private Boundary boundary;
    private float boundaryX;

    public WordCanvas(WordSession session,WordLayoutEngine engine,WordRenderer renderer) {
        this.session=session;this.engine=engine;this.renderer=renderer;
        setFocusable(true);setOpaque(true);setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));enableInputMethods(true);setFocusTraversalKeysEnabled(false);
        getAccessibleContext().setAccessibleName("Documento");
        snapshot=engine.layout(session.getDocument());
        listener=session.addListener(event->{
            if(event.change()==WordSession.Change.DOCUMENT||event.change()==WordSession.Change.SELECTION){topRevealOffset=null;topRevealTicket++;}
            if(event.change()==WordSession.Change.DOCUMENT) scheduleLayoutIfShowing();
            repaint(); if(event.change()==WordSession.Change.SELECTION) revealCaret();
            if(accessibleContext!=null) accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_TEXT_PROPERTY,null,event.revision());
        });
        installInput();
        addComponentListener(new ComponentAdapter(){@Override public void componentResized(ComponentEvent e){if(viewMode==WordViewMode.CONTINUOUS)scheduleLayoutIfShowing();}});
    }
    public WordSession getSession(){return session;}
    public WordRenderer getRenderer(){return renderer;}
    public WordLayout getLayoutSnapshot(){return snapshot;}
    public boolean isLayoutCurrent(){return snapshot!=null&&snapshot.document()==session.getDocument();}
    public void setErrorHandler(Consumer<Throwable> value){errorHandler=Objects.requireNonNull(value);}
    public void setObjectHandler(Consumer<WordLayout.ObjectBox> value){objectHandler=Objects.requireNonNull(value);}
    public void setLinkHandler(Consumer<String> value){linkHandler=Objects.requireNonNull(value);}
    public void setRegionHandler(Consumer<WordLayout.Region> value){regionHandler=Objects.requireNonNull(value);}
    public void setPasteHandler(Predicate<Transferable> value){pasteHandler=Objects.requireNonNull(value);}
    public void setZoom(double value){if(!Double.isFinite(value)||value<.25||value>4)throw new IllegalArgumentException();zoom=value;revalidate();repaint();if(viewMode==WordViewMode.CONTINUOUS)scheduleLayoutIfShowing();}
    public double getZoom(){return zoom;}
    public void setViewMode(WordViewMode value){viewMode=Objects.requireNonNull(value);scheduleLayoutIfShowing();}
    private void scheduleLayoutIfShowing(){if(!visualPaused||isShowing())scheduleLayout();else layoutDirty=true;}
    public void scheduleLayout(){
        if(closed)return;
        layoutDirty=false;
        if(pending!=null)pending.cancel(true);
        if(transientExecutor!=null){transientExecutor.shutdownNow();transientExecutor=null;}
        boolean transientWorker=!isShowing();
        if(!transientWorker&&(executor==null||executor.isShutdown()))executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"word-layout");t.setDaemon(true);return t;});
        ExecutorService worker=transientWorker?Executors.newVirtualThreadPerTaskExecutor():executor;
        if(transientWorker)transientExecutor=worker;
        long ticket=++generation;
        var document=session.getDocument();boolean continuous=viewMode==WordViewMode.CONTINUOUS;
        float width=(float)(Math.max(300,getWidth()-48)/scale())-document.pageSettings().left()-document.pageSettings().right();
        pending=worker.submit(()->{
            try {
                WordLayout next=engine.layout(document,continuous,Math.max(120,width));
                SwingUtilities.invokeLater(()->{if(!closed&&ticket==generation){snapshot=next;layoutGeneration=ticket;revalidate();repaint();revealCaret();queueTopReveal();firePropertyChange("layoutSnapshot",null,next);}});
            }catch(CancellationException ignored){}catch(Throwable error){SwingUtilities.invokeLater(()->{if(!closed&&ticket==generation)errorHandler.accept(error);});}
            finally { if(transientWorker)worker.shutdown(); }
        });
    }
    public void pauseVisualWork(){
        visualPaused=true;
        generation++;
        if(pending!=null){pending.cancel(true);pending=null;}
        if(transientExecutor!=null){transientExecutor.shutdownNow();transientExecutor=null;}
        if(executor!=null){executor.shutdownNow();executor=null;}
        layoutDirty=true;
    }
    public void resumeVisualWork(){if(!closed&&isShowing()){visualPaused=false;if(layoutDirty||!isLayoutCurrent())scheduleLayout();}}
    public double scale(){return zoom*96/72;}
    private double pageX(WordLayout.Page p){return Math.max(24,(getWidth()-p.width()*scale())/2);}
    private double pageY(int index){double y=24;for(int i=0;i<index;i++)y+=snapshot.pages().get(i).height()*scale()+24;return y;}
    public Point2D toScreen(WordLayout.Page page,float x,float y){return new Point2D.Double(pageX(page)+x*scale(),pageY(page.index())+y*scale());}
    @Override public Dimension getPreferredSize(){
        if(snapshot==null)return new Dimension(840,1150);
        double width=0,height=24;for(var page:snapshot.pages()){width=Math.max(width,page.width()*scale()+48);height+=page.height()*scale()+24;}
        return new Dimension((int)Math.ceil(width),(int)Math.min(Integer.MAX_VALUE-1,Math.ceil(height)));
    }
    private PageHit pageAt(Point point){
        if(snapshot==null||snapshot.pages().isEmpty())return null;
        WordLayout.Page chosen=snapshot.pages().getLast();
        for(var page:snapshot.pages())if(point.y<pageY(page.index())+page.height()*scale()+12){chosen=page;break;}
        return new PageHit(chosen,(float)((point.x-pageX(chosen))/scale()),(float)((point.y-pageY(chosen.index()))/scale()));
    }

    @Override protected void paintComponent(Graphics graphics){
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setColor(UiTokens.surfaceAlt());g.fillRect(0,0,getWidth(),getHeight());
            if(snapshot==null)return;
            Rectangle clip=g.getClipBounds();boolean current=isLayoutCurrent();
            WordContentSelection selection=session.getContentSelection();
            for(var page:snapshot.pages()){
                double x=pageX(page),y=pageY(page.index());
                if(clip!=null&&!clip.intersects(x,y,page.width()*scale(),page.height()*scale()))continue;
                g.setColor(new Color(0,0,0,25));g.fill(new Rectangle2D.Double(x+3,y+4,page.width()*scale(),page.height()*scale()));
                Graphics2D pg=(Graphics2D)g.create();
                try {
                    pg.translate(x,y);pg.scale(scale(),scale());
                    renderer.paintPage(pg,page,current&&selection instanceof WordSelection s?s:null,SELECTION);
                    if(current) overlays(pg,page,selection);
                } finally {pg.dispose();}
            }
            if(current&&hasFocus()&&selection instanceof WordSelection){
                Rectangle caret=caretBounds();g.setColor(new Color(35,80,170));g.fillRect(caret.x,caret.y,1,caret.height);
                if(!composition.isEmpty()){g.setFont(UiTokens.font());g.drawString(composition,caret.x,caret.y+caret.height);}
            }
            if(!current){g.setColor(UiTokens.muted());g.setFont(UiTokens.fontSmall());g.drawString("Atualizando páginas…",24,18);}
        }finally{g.dispose();}
    }
    private void overlays(Graphics2D g,WordLayout.Page page,WordContentSelection selection){
        float px=(float)(1/scale());
        if(selection instanceof WordCellSelection cells){
            g.setColor(SELECTION);
            for(var cell:page.cells())if(!cell.repeated()&&cell.tableId().equals(cells.tableId())&&cell.row()>=cells.firstRow()&&cell.row()<=cells.lastRow()
                    &&cell.gridColumn()+cell.gridSpan()-1>=cells.firstColumn()&&cell.gridColumn()<=cells.lastColumn())g.fill(cell.bounds());
        }
        if(selection instanceof WordObjectSelection sel){
            for(var box:page.objects()) if(box.interactive()&&box.offset()==sel.offset()){
                Rectangle2D.Float r=box.bounds();
                g.setColor(HANDLE);g.setStroke(new BasicStroke(px*1.2f));g.draw(r);
                if(!session.isReadOnly()&&box.object().resizable())for(Point2D h:handles(r)){float s=7*px;Rectangle2D.Float hr=new Rectangle2D.Float((float)h.getX()-s/2,(float)h.getY()-s/2,s,s);g.setColor(Color.WHITE);g.fill(hr);g.setColor(HANDLE);g.draw(hr);}
                if(!session.isReadOnly()&&rotatable(box.object())){Point2D top=rotationHandle(r,px);g.draw(new Line2D.Double(r.getCenterX(),r.y,top.getX(),top.getY()));float s=8*px;g.setColor(Color.WHITE);g.fill(new Ellipse2D.Double(top.getX()-s/2,top.getY()-s/2,s,s));g.setColor(HANDLE);g.draw(new Ellipse2D.Double(top.getX()-s/2,top.getY()-s/2,s,s));}
            }
        }
        if(ghost!=null&&dragPage==page){
            Graphics2D c=(Graphics2D)g.create();
            try{
                c.setColor(HANDLE);c.setStroke(new BasicStroke(px,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{4*px,3*px},0));
                if(drag==Drag.ROTATE)c.rotate(Math.toRadians(ghostAngle),ghost.getCenterX(),ghost.getCenterY());
                c.draw(ghost);
            }finally{c.dispose();}
        }
        if(drag==Drag.COLUMN&&boundary!=null&&dragPage==page){g.setColor(HANDLE);g.setStroke(new BasicStroke(px,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{4*px,3*px},0));g.draw(new Line2D.Float(boundaryX,boundary.top(),boundaryX,boundary.bottom()));}
    }
    private static boolean rotatable(WordInlineObject o){return o instanceof WordImage||o instanceof WordShape;}
    private static Point2D[] handles(Rectangle2D.Float r){
        return new Point2D[]{new Point2D.Float(r.x,r.y),new Point2D.Float((float)r.getCenterX(),r.y),new Point2D.Float(r.x+r.width,r.y),new Point2D.Float(r.x+r.width,(float)r.getCenterY()),
                new Point2D.Float(r.x+r.width,r.y+r.height),new Point2D.Float((float)r.getCenterX(),r.y+r.height),new Point2D.Float(r.x,r.y+r.height),new Point2D.Float(r.x,(float)r.getCenterY())};
    }
    private static Point2D rotationHandle(Rectangle2D.Float r,float px){return new Point2D.Double(r.getCenterX(),r.y-18*px);}

    public int hitTest(Point point){
        if(!isLayoutCurrent())return session.getSelection().caret();
        PageHit hit=pageAt(point);if(hit==null)return 0;
        WordLayout.Line best=null;double bestScore=Double.MAX_VALUE;
        for(var line:hit.page().lines()){
            if(!line.positional())continue;
            double dy=hit.y()<line.top()?line.top()-hit.y():hit.y()>line.bottom()?hit.y()-line.bottom():0;
            float left=line.x(),right=line.x()+Math.max(line.text().getAdvance(),4);
            double dx=hit.x()<left-2?left-hit.x():hit.x()>right+2?hit.x()-right:0;
            double score=dy*4+dx;
            if(score<bestScore){bestScore=score;best=line;}
        }
        if(best==null){
            WordLayout.Page page=hit.page();
            for(int i=page.index();i>=0;i--){var lines=snapshot.pages().get(i).lines().stream().filter(WordLayout.Line::positional).toList();if(!lines.isEmpty())return lines.getLast().end();}
            return 0;
        }
        int local=best.text().hitTestChar(hit.x()-best.x(),hit.y()-best.baseline()).getInsertionIndex();
        int offset=Math.max(best.start(),Math.min(best.end(),best.start()+local));
        var document=session.getDocument();
        if(!document.isBoundary(offset))offset=document.previousBoundary(offset);
        return offset;
    }
    public Rectangle caretBounds(){return boundsAt(session.getSelection().caret());}
    public Rectangle boundsAt(int offset){
        if(snapshot!=null){
            WordLayout.Line found=null;WordLayout.Page foundPage=null;
            outer:for(var page:snapshot.pages())for(var line:page.lines()){
                if(!line.positional()||offset<line.start()||offset>line.end())continue;
                found=line;foundPage=page;
                if(offset<line.end()||line.start()==line.end())break outer;
            }
            if(found!=null){
                float x=found.text().getCaretInfo(TextHitInfo.leading(Math.min(offset-found.start(),found.text().getCharacterCount())))[0];
                return new Rectangle((int)(pageX(foundPage)+(found.x()+x)*scale()),(int)(pageY(foundPage.index())+found.top()*scale()),2,Math.max(12,(int)((found.bottom()-found.top())*scale())));
            }
        }
        return new Rectangle(24,24,2,16);
    }
    public void revealCaret(){
        if(!isLayoutCurrent()||topRevealOffset!=null)return;
        if(session.getContentSelection() instanceof WordObjectSelection o){WordLayout.ObjectBox box=box(o.offset());if(box!=null){scrollRectToVisible(screen(box));return;}}
        scrollRectToVisible(caretBounds());
    }
    /** Reveals a document offset near the viewport top, after pending pagination completes. */
    public void revealOffsetAtTop(int offset){
        session.getDocument().checkRange(offset,offset);
        topRevealOffset=offset;topRevealTicket++;
        queueTopReveal();
    }
    private void queueTopReveal(){
        if(closed||topRevealOffset==null||!isLayoutCurrent()||layoutGeneration!=generation)return;
        long ticket=topRevealTicket,layoutTicket=generation;
        SwingUtilities.invokeLater(()->{
            if(closed||ticket!=topRevealTicket||topRevealOffset==null||layoutTicket!=generation||!isLayoutCurrent())return;
            if(getParent() instanceof JViewport viewport){
                // Revalidation is deferred in Swing; use the new view size before clamping the destination.
                viewport.doLayout();
                Rectangle target=boundsAt(topRevealOffset);
                Dimension extent=viewport.getExtentSize();
                Point position=viewport.getViewPosition();
                int x=position.x;
                if(target.x<x)x=target.x;
                else if(target.x+target.width>x+extent.width)x=target.x+target.width-extent.width;
                x=Math.max(0,Math.min(x,getWidth()-extent.width));
                int y=Math.max(0,Math.min(target.y-16,getHeight()-extent.height));
                viewport.setViewPosition(new Point(x,y));
            }
            topRevealOffset=null;
        });
    }
    private WordLayout.ObjectBox box(int offset){
        if(snapshot==null)return null;
        for(var page:snapshot.pages())for(var b:page.objects())if(b.interactive()&&b.offset()==offset){dragPageFor(page,b);return b;}
        return null;
    }
    private WordLayout.Page pageOfBox;
    private void dragPageFor(WordLayout.Page page,WordLayout.ObjectBox b){pageOfBox=page;}
    private Rectangle screen(WordLayout.ObjectBox box){
        WordLayout.Page page=pageOfBox;if(page==null)return new Rectangle();
        return new Rectangle((int)(pageX(page)+box.x()*scale()),(int)(pageY(page.index())+box.y()*scale()),(int)Math.ceil(box.width()*scale()),(int)Math.ceil(box.height()*scale()));
    }
    private WordLayout.ObjectBox objectAt(PageHit hit){
        List<WordLayout.ObjectBox> objects=hit.page().objects();
        for(int pass=0;pass<2;pass++)for(int i=objects.size()-1;i>=0;i--){
            var b=objects.get(i);if(!b.interactive())continue;
            boolean front=!b.behindText();if(pass==0!=front)continue;
            float pad=b.object().textual()?0:1;
            if(new Rectangle2D.Float(b.x()-pad,b.y()-pad,b.width()+2*pad,Math.max(b.height(),2)+2*pad).contains(hit.x(),hit.y()))return b;
        }
        return null;
    }
    private int handleAt(PageHit hit){
        if(!(session.getContentSelection() instanceof WordObjectSelection sel)||session.isReadOnly())return -1;
        for(var b:hit.page().objects()) if(b.interactive()&&b.offset()==sel.offset()){
            float px=(float)(1/scale()),tolerance=6*px;
            if(rotatable(b.object())&&rotationHandle(b.bounds(),px).distance(hit.x(),hit.y())<=tolerance)return 8;
            if(!b.object().resizable())return -1;
            Point2D[] hs=handles(b.bounds());
            for(int i=0;i<hs.length;i++)if(hs[i].distance(hit.x(),hit.y())<=tolerance)return i;
        }
        return -1;
    }
    private Boundary boundaryAt(PageHit hit){
        if(session.isReadOnly())return null;
        float tolerance=(float)(4/scale());Boundary best=null;
        for(var cell:hit.page().cells()){
            if(cell.repeated())continue;
            Rectangle2D.Float r=cell.bounds();
            if(hit.y()<r.y||hit.y()>r.y+r.height)continue;
            float right=r.x+r.width;
            if(Math.abs(hit.x()-right)<=tolerance){
                var table=session.getDocument().findTable(cell.tableId());if(table.isEmpty())continue;
                float[] edges=cell.columnEdges();float scale=(edges[edges.length-1]-edges[0])/Math.max(1,table.get().width());
                float top=Float.MAX_VALUE,bottom=0;for(var c:hit.page().cells())if(c.tableId().equals(cell.tableId())){top=Math.min(top,c.bounds().y);bottom=Math.max(bottom,c.bounds().y+c.bounds().height);}
                Boundary candidate=new Boundary(cell.tableId(),cell.gridColumn()+cell.gridSpan()-1,right,top,bottom,scale,cell.depth());
                if(best==null||candidate.depth()>best.depth())best=candidate;
            }
        }
        return best;
    }

    public void copy(){
        WordContentSelection s=session.getContentSelection();var doc=session.getDocument();
        if(s.range().isEmpty())return;
        WordTransferable transferable;
        if(s instanceof WordCellSelection cells){
            WordTable table=doc.findTable(cells.tableId()).orElseThrow();
            WordTable sub=WordTableEditing.subTable(table,cells.firstRow(),cells.firstColumn(),cells.lastRow(),cells.lastColumn());
            transferable=new WordTransferable(new WordDocument(List.of(sub),doc.pageSettings(),doc.parts()),WordTableEditing.toTabular(table,cells.firstRow(),cells.firstColumn(),cells.lastRow(),cells.lastColumn()),null);
        } else if(s instanceof WordObjectSelection o&&doc.objectAt(o.offset())!=null&&doc.objectAt(o.offset()).object() instanceof WordImage image){
            Image picture=doc.resources().get(image.resourceId()).flatMap(WordImageCache::image).orElse(null);
            transferable=new WordTransferable(doc.fragment(o.offset(),o.offset()+1),image.altText(),picture);
        } else transferable=new WordTransferable(doc.fragment(s.range().start(),s.range().end()));
        getToolkit().getSystemClipboard().setContents(transferable,null);
    }
    public void cut(){if(session.isReadOnly()||session.getSelection().isEmpty())return;copy();session.deleteSelection(false);}
    public void paste(){
        if(session.isReadOnly())return;
        try {
            Transferable content=getToolkit().getSystemClipboard().getContents(null);
            if(content!=null)paste(content);
        } catch(Exception e){errorHandler.accept(e);}
    }
    public void paste(Transferable content)throws Exception{
        if(content.isDataFlavorSupported(WordTransferable.DOCUMENT)){
            WordDocument fragment=(WordDocument)content.getTransferData(WordTransferable.DOCUMENT);
            if(session.getContentSelection() instanceof WordCellSelection&&fragment.blocks().stream().anyMatch(b->b instanceof WordTable)&&content.isDataFlavorSupported(DataFlavor.stringFlavor)){pasteTabular((String)content.getTransferData(DataFlavor.stringFlavor));return;}
            session.replaceSelection(fragment);return;
        }
        if(pasteHandler.test(content))return;
        if(content.isDataFlavorSupported(DataFlavor.stringFlavor)){
            String text=(String)content.getTransferData(DataFlavor.stringFlavor);
            boolean inTable=session.getContentSelection() instanceof WordCellSelection||text.indexOf('\t')>=0&&session.getDocument().tableAt(session.getSelection().caret()).isPresent();
            if(inTable){pasteTabular(text);return;}
            session.replaceSelection(text);
        }
    }
    private void pasteTabular(String text){
        var doc=session.getDocument();int row,column;UUID table;
        if(session.getContentSelection() instanceof WordCellSelection cells){table=cells.tableId();row=cells.firstRow();column=cells.firstColumn();}
        else{var loc=doc.tableAt(session.getSelection().caret()).orElseThrow();table=loc.table().id();row=loc.row();column=loc.gridColumn();}
        List<List<String>> values=WordTableEditing.parseTabular(text);
        WordTextStyle style=session.getInsertionStyle();int r=row,c=column;UUID id=table;
        session.executeWithSelection("Colar células",d->d.updateTable(id,t->WordTableEditing.fill(t,r,c,values,style)),d->d.findTable(id).map(t->(WordContentSelection)cellsSelection(d,id,r,c,Math.min(t.rows().size()-1,r+values.size()-1),Math.min(t.gridColumns()-1,c+values.stream().mapToInt(List::size).max().orElse(1)-1))).orElse(session.getSelection()));
    }
    private static WordContentSelection cellsSelection(WordDocument d,UUID id,int r0,int c0,int r1,int c1){
        int[] a=d.cellRange(id,r0,d.findTable(id).orElseThrow().rows().get(r0).cellAt(c0));
        WordTable t=d.findTable(id).orElseThrow();int[] b=d.cellRange(id,r1,Math.max(0,t.rows().get(r1).cellAt(c1)));
        return WordSession.normalize(d,a[0],b[1]);
    }
    public void moveToCell(boolean forward){
        var doc=session.getDocument();var loc=doc.tableAt(session.getSelection().caret());
        if(loc.isEmpty())return;
        WordTable table=loc.get().table();int row=loc.get().row(),cell=loc.get().cell();
        while(true){
            if(forward){cell++;if(cell>=table.rows().get(row).cells().size()){cell=0;row++;}}
            else{cell--;if(cell<0){row--;if(row<0)return;cell=table.rows().get(row).cells().size()-1;}}
            if(row>=table.rows().size()){
                if(session.isReadOnly())return;
                UUID id=table.id();int last=table.rows().size()-1;
                session.executeWithSelection("Inserir linha",d->d.updateTable(id,t->WordTableEditing.insertRow(t,last,true)),d->{int start=d.cellRange(id,last+1,0)[0];return new WordSelection(start,start);});
                return;
            }
            if(table.rows().get(row).cells().get(cell).verticalMerge()!=WordTableCell.Merge.CONTINUE)break;
        }
        int[] range=doc.cellRange(table.id(),row,cell);
        session.setSelection(range[0],range[1]);
    }
    private void installInput(){
        addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                if(!SwingUtilities.isLeftMouseButton(e))return;requestFocusInWindow();
                if(!isLayoutCurrent())return;
                PageHit hit=pageAt(e.getPoint());if(hit==null)return;
                dragStart=e.getPoint();dragPage=hit.page();
                int h=handleAt(hit);
                if(h>=0){
                    WordObjectSelection sel=(WordObjectSelection)session.getContentSelection();dragBox=box(sel.offset());
                    if(dragBox!=null){drag=h==8?Drag.ROTATE:Drag.RESIZE;handle=h;ghost=dragBox.bounds();ghostAngle=dragBox.object().rotation();return;}
                }
                Boundary b=boundaryAt(hit);
                if(b!=null){drag=Drag.COLUMN;boundary=b;boundaryX=b.x();return;}
                WordLayout.ObjectBox object=objectAt(hit);
                if(object!=null&&!object.object().textual()){
                    session.selectObject(object.offset());
                    if(e.getClickCount()>=2){objectHandler.accept(object);return;}
                    if(object.floating()&&!session.isReadOnly()){drag=Drag.MOVE;dragBox=object;ghost=object.bounds();}
                    return;
                }
                if(object!=null&&object.object() instanceof WordFormField){session.setSelection(object.offset()+1,object.offset()+1);objectHandler.accept(object);return;}
                for(var line:hit.page().lines()){
                    if(line.positional()||line.link()==null)continue;
                    if(hit.y()>=line.top()&&hit.y()<=line.bottom()&&hit.x()>=line.x()&&hit.x()<=line.x()+line.text().getAdvance()){linkHandler.accept(line.link());return;}
                }
                if(e.getClickCount()>=2){
                    for(var line:hit.page().lines())if((line.region()==WordLayout.Region.HEADER||line.region()==WordLayout.Region.FOOTER)&&hit.y()>=line.top()-4&&hit.y()<=line.bottom()+4){regionHandler.accept(line.region());return;}
                    WordPageSettings s=hit.page().settings();
                    if(s!=null&&(hit.y()<s.top()||hit.y()>hit.page().height()-s.bottom())){regionHandler.accept(hit.y()<s.top()?WordLayout.Region.HEADER:WordLayout.Region.FOOTER);return;}
                }
                int offset=hitTest(e.getPoint());
                if(e.isControlDown()){
                    var doc=session.getDocument();String link=offset<doc.length()?doc.styleAt(offset).link():null;
                    if(link==null&&offset>0)link=doc.styleAt(offset-1).link();
                    if(link!=null){linkHandler.accept(link);return;}
                }
                drag=Drag.TEXT;
                session.setSelection(e.isShiftDown()?session.getSelection().anchor():offset,offset);
                if(e.getClickCount()==2){String text=session.getDocument().text();BreakIterator words=BreakIterator.getWordInstance();words.setText(text);
                    int start=words.preceding(Math.min(text.length(),offset+1)),end=words.following(Math.min(offset,Math.max(0,text.length()-1)));
                    start=Math.max(0,start);end=end<0?text.length():end;
                    if(session.getDocument().sameContainer(start,end)&&session.getDocument().isBoundary(start)&&session.getDocument().isBoundary(end))session.setSelection(start,end);}
                if(e.getClickCount()>=3){var doc=session.getDocument();int i=doc.paragraphIndex(offset);session.setSelection(doc.paragraphStart(i),doc.paragraphEnd(i));}
            }
            @Override public void mouseReleased(MouseEvent e){
                try{commitDrag(e);}catch(RuntimeException error){errorHandler.accept(error);}
                finally{drag=Drag.NONE;ghost=null;dragBox=null;boundary=null;handle=-1;repaint();}
            }
        });
        addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                if((e.getModifiersEx()&InputEvent.BUTTON1_DOWN_MASK)==0||dragStart==null)return;
                float dx=(float)((e.getX()-dragStart.x)/scale()),dy=(float)((e.getY()-dragStart.y)/scale());
                switch(drag){
                    case TEXT -> {if(isLayoutCurrent())session.setSelection(session.getSelection().anchor(),hitTest(e.getPoint()));}
                    case MOVE -> {Rectangle2D.Float o=dragBox.bounds();ghost=new Rectangle2D.Float(o.x+dx,o.y+dy,o.width,o.height);repaint();}
                    case RESIZE -> {ghost=resized(dragBox.bounds(),handle,dx,dy,e.isShiftDown()||dragBox.object().lockAspectRatio());repaint();}
                    case ROTATE -> {PageHit hit=pageAt(e.getPoint());if(hit!=null){Rectangle2D.Float o=dragBox.bounds();double a=Math.toDegrees(Math.atan2(hit.x()-o.getCenterX(),-(hit.y()-o.getCenterY())));if(e.isShiftDown())a=Math.round(a/15)*15;ghostAngle=(float)((a%360+360)%360);repaint();}}
                    case COLUMN -> {boundaryX=boundary.x()+dx;repaint();}
                    default -> {}
                }
            }
            @Override public void mouseMoved(MouseEvent e){
                if(!isLayoutCurrent())return;
                PageHit hit=pageAt(e.getPoint());if(hit==null)return;
                int h=handleAt(hit);
                if(h>=0){setCursor(Cursor.getPredefinedCursor(switch(h){case 0->Cursor.NW_RESIZE_CURSOR;case 1->Cursor.N_RESIZE_CURSOR;case 2->Cursor.NE_RESIZE_CURSOR;case 3->Cursor.E_RESIZE_CURSOR;case 4->Cursor.SE_RESIZE_CURSOR;case 5->Cursor.S_RESIZE_CURSOR;case 6->Cursor.SW_RESIZE_CURSOR;case 7->Cursor.W_RESIZE_CURSOR;default->Cursor.HAND_CURSOR;}));return;}
                if(boundaryAt(hit)!=null){setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));return;}
                WordLayout.ObjectBox object=objectAt(hit);
                if(object!=null&&!object.object().textual()){setCursor(Cursor.getPredefinedCursor(object.floating()?Cursor.MOVE_CURSOR:Cursor.DEFAULT_CURSOR));setToolTipText(object.object().altText().isBlank()?null:object.object().altText());return;}
                String tip=null;
                for(var line:hit.page().lines())if(!line.positional()&&line.link()!=null&&hit.y()>=line.top()&&hit.y()<=line.bottom()){setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setToolTipText("Ir para o título");return;}
                int offset=hitTest(e.getPoint());var doc=session.getDocument();
                if(offset<doc.length()&&doc.styleAt(offset).link()!=null)tip="Ctrl+clique para abrir "+doc.styleAt(offset).link();
                setToolTipText(tip);setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            }
        });
        addFocusListener(new FocusAdapter(){@Override public void focusGained(FocusEvent e){repaint();}@Override public void focusLost(FocusEvent e){repaint();}});
        bind("copy",KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_DOWN_MASK),this::copy);
        bind("cut",KeyStroke.getKeyStroke(KeyEvent.VK_X,InputEvent.CTRL_DOWN_MASK),this::cut);
        bind("paste",KeyStroke.getKeyStroke(KeyEvent.VK_V,InputEvent.CTRL_DOWN_MASK),this::paste);
        bind("selectAll",KeyStroke.getKeyStroke(KeyEvent.VK_A,InputEvent.CTRL_DOWN_MASK),()->session.setSelection(0,session.getDocument().length()));
        bind("undo",KeyStroke.getKeyStroke(KeyEvent.VK_Z,InputEvent.CTRL_DOWN_MASK),()->{if(session.canUndo())session.undo();});
        bind("redo",KeyStroke.getKeyStroke(KeyEvent.VK_Y,InputEvent.CTRL_DOWN_MASK),()->{if(session.canRedo())session.redo();});
        bind("bold",KeyStroke.getKeyStroke(KeyEvent.VK_B,InputEvent.CTRL_DOWN_MASK),()->{if(!session.isReadOnly())session.formatSelection(s->s.withBold(!s.bold()));});
        bind("italic",KeyStroke.getKeyStroke(KeyEvent.VK_I,InputEvent.CTRL_DOWN_MASK),()->{if(!session.isReadOnly())session.formatSelection(s->s.withItalic(!s.italic()));});
        bind("underline",KeyStroke.getKeyStroke(KeyEvent.VK_U,InputEvent.CTRL_DOWN_MASK),()->{if(!session.isReadOnly())session.formatSelection(s->s.withUnderline(!s.underline()));});
        bind("lineBreak",KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.SHIFT_DOWN_MASK),()->{if(!session.isReadOnly())session.insertObject(WordBreak.of(WordBreak.Kind.LINE));});
        bind("pageBreak",KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK),()->{if(!session.isReadOnly())session.insertObject(WordBreak.of(WordBreak.Kind.PAGE));});
        addKeyListener(new KeyAdapter(){
            private char highSurrogate;
            @Override public void keyTyped(KeyEvent e){
                if(session.isReadOnly() || e.isControlDown()&&!e.isAltDown() || e.isMetaDown())return;
                char c=e.getKeyChar();if(Character.isISOControl(c)||c==KeyEvent.CHAR_UNDEFINED)return;
                if(Character.isHighSurrogate(c)){highSurrogate=c;return;}
                String text=Character.isLowSurrogate(c)&&highSurrogate!=0?new String(new char[]{highSurrogate,c}):String.valueOf(c);highSurrogate=0;
                if(Character.isLowSurrogate(c)&&text.length()==1)return;
                try{session.replaceSelection(text);}catch(RuntimeException error){errorHandler.accept(error);}e.consume();
            }
            @Override public void keyPressed(KeyEvent e){
                try{handleKey(e);}catch(RuntimeException error){errorHandler.accept(error);e.consume();}
            }
        });
        addInputMethodListener(new InputMethodListener(){
            @Override public void inputMethodTextChanged(InputMethodEvent event){
                AttributedCharacterIterator iterator=event.getText();StringBuilder value=new StringBuilder();
                if(iterator!=null)for(char c=iterator.first();c!=AttributedCharacterIterator.DONE;c=iterator.next())value.append(c);
                int committed=Math.min(event.getCommittedCharacterCount(),value.length());
                if(!session.isReadOnly()&&committed>0)session.replaceSelection(value.substring(0,committed));
                composition=session.isReadOnly()?"":value.substring(committed);event.consume();repaint();
            }
            @Override public void caretPositionChanged(InputMethodEvent event){event.consume();}
        });
    }
    private void handleKey(KeyEvent e){
        var doc=session.getDocument();var content=session.getContentSelection();var selection=session.getSelection();int caret=selection.caret(),next=caret;
        switch(e.getKeyCode()){
            case KeyEvent.VK_TAB -> {
                if(e.isControlDown())return;
                if(doc.tableAt(caret).isPresent()||content instanceof WordCellSelection){moveToCell(!e.isShiftDown());e.consume();return;}
                if(!session.isReadOnly())session.replaceSelection("\t");e.consume();return;
            }
            case KeyEvent.VK_ESCAPE -> {if(content instanceof WordObjectSelection o){session.setSelection(o.offset()+1,o.offset()+1);e.consume();}return;}
            case KeyEvent.VK_LEFT -> next=content instanceof WordObjectSelection o?o.offset():!e.isShiftDown()&&!selection.isEmpty()?selection.start():doc.previousBoundary(caret);
            case KeyEvent.VK_RIGHT -> next=content instanceof WordObjectSelection o?o.offset()+1:!e.isShiftDown()&&!selection.isEmpty()?selection.end():doc.nextBoundary(caret);
            case KeyEvent.VK_HOME -> next=e.isControlDown()?0:doc.paragraphStart(doc.paragraphIndex(caret));
            case KeyEvent.VK_END -> next=e.isControlDown()?doc.length():doc.paragraphEnd(doc.paragraphIndex(caret));
            case KeyEvent.VK_UP,KeyEvent.VK_DOWN -> {Rectangle b=caretBounds();next=hitTest(new Point(b.x,b.y+(e.getKeyCode()==KeyEvent.VK_UP?-b.height/2-2:b.height+b.height/2+2)));}
            case KeyEvent.VK_PAGE_UP,KeyEvent.VK_PAGE_DOWN -> {Rectangle b=caretBounds();int h=getVisibleRect().height;next=hitTest(new Point(b.x,b.y+(e.getKeyCode()==KeyEvent.VK_PAGE_UP?-h:h)));}
            case KeyEvent.VK_BACK_SPACE,KeyEvent.VK_DELETE -> {
                if(!session.isReadOnly()){
                    boolean forward=e.getKeyCode()==KeyEvent.VK_DELETE;
                    if(selection.isEmpty()){
                        int from=forward?caret:doc.previousBoundary(caret),to=forward?doc.nextBoundary(caret):caret;
                        if(from==to){e.consume();return;}
                        if(!doc.sameContainer(from,to)){e.consume();return;}
                        session.setSelection(from,to);
                    }
                    session.deleteSelection(forward);
                }e.consume();return;
            }
            case KeyEvent.VK_ENTER -> {if(e.isShiftDown()||e.isControlDown())return;if(!session.isReadOnly())session.insertParagraphBreak();e.consume();return;}
            default -> {return;}
        }
        session.setSelection(e.isShiftDown()?selection.anchor():next,next);e.consume();
    }
    private static Rectangle2D.Float resized(Rectangle2D.Float o,int handle,float dx,float dy,boolean keepAspect){
        float x1=o.x,y1=o.y,x2=o.x+o.width,y2=o.y+o.height;
        switch(handle){case 0->{x1+=dx;y1+=dy;}case 1->y1+=dy;case 2->{x2+=dx;y1+=dy;}case 3->x2+=dx;case 4->{x2+=dx;y2+=dy;}case 5->y2+=dy;case 6->{x1+=dx;y2+=dy;}case 7->x1+=dx;default->{}}
        float w=Math.max(4,x2-x1),h=Math.max(4,y2-y1);
        boolean corner=handle%2==0;
        if(keepAspect&&corner&&o.width>0&&o.height>0){float ratio=o.width/o.height;if(w/h>ratio)w=h*ratio;else h=w/ratio;}
        float nx=handle==0||handle==6||handle==7?o.x+o.width-w:o.x,ny=handle==0||handle==1||handle==2?o.y+o.height-h:o.y;
        return new Rectangle2D.Float(nx,ny,w,h);
    }
    private void commitDrag(MouseEvent e){
        if(session.isReadOnly())return;
        switch(drag){
            case RESIZE -> {
                if(ghost==null||dragBox==null||ghost.equals(dragBox.bounds()))return;
                WordInlineObject object=session.getDocument().objectAt(dragBox.offset()).object();
                float w=Math.min(14400,ghost.width),h=Math.min(14400,ghost.height);
                WordInlineObject resized=object.resize(w,h);
                if(object.placement().floating()&&(ghost.x!=dragBox.x()||ghost.y!=dragBox.y()))resized=resized.withPlacement(object.placement().moveTo(object.placement().x()+ghost.x-dragBox.x(),object.placement().y()+ghost.y-dragBox.y()));
                session.replaceObject(dragBox.offset(),resized,"Redimensionar objeto");
            }
            case MOVE -> {
                if(ghost==null||dragBox==null)return;
                float dx=ghost.x-dragBox.x(),dy=ghost.y-dragBox.y();
                if(Math.abs(dx)<0.5f&&Math.abs(dy)<0.5f)return;
                WordInlineObject object=session.getDocument().objectAt(dragBox.offset()).object();
                session.replaceObject(dragBox.offset(),object.withPlacement(object.placement().moveTo(object.placement().x()+dx,object.placement().y()+dy)),"Mover objeto");
            }
            case ROTATE -> {
                if(dragBox==null||Math.abs(ghostAngle-dragBox.object().rotation())<0.5f)return;
                WordInlineObject object=session.getDocument().objectAt(dragBox.offset()).object();
                session.replaceObject(dragBox.offset(),object.withRotation(ghostAngle),"Girar objeto");
            }
            case COLUMN -> {
                if(boundary==null||Math.abs(boundaryX-boundary.x())<0.5f)return;
                float delta=(boundaryX-boundary.x())/Math.max(0.01f,boundary.scale());int index=boundary.index();UUID id=boundary.tableId();
                session.execute("Largura da coluna",d->d.updateTable(id,t->WordTableEditing.resizeBoundary(t,index,delta)));
            }
            default -> {}
        }
    }
    public void bind(String id,KeyStroke key,Runnable runnable){
        getInputMap(WHEN_FOCUSED).put(key,id);getActionMap().put(id,new AbstractAction(){@Override public void actionPerformed(ActionEvent e){try{runnable.run();}catch(RuntimeException error){errorHandler.accept(error);}}});
    }
    @Override public Dimension getPreferredScrollableViewportSize(){return new Dimension(850,650);}
    @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 24;}
    @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(24,visible.height-48);}
    @Override public boolean getScrollableTracksViewportWidth(){return viewMode==WordViewMode.CONTINUOUS || getParent()!=null&&getParent().getWidth()>getPreferredSize().width;}
    @Override public boolean getScrollableTracksViewportHeight(){return false;}
    @Override public InputMethodRequests getInputMethodRequests(){return this;}
    @Override public Rectangle getTextLocation(TextHitInfo offset){Rectangle r=caretBounds();if(isShowing()){Point p=getLocationOnScreen();r.translate(p.x,p.y);}return r;}
    @Override public TextHitInfo getLocationOffset(int x,int y){if(isShowing()){Point p=getLocationOnScreen();return TextHitInfo.leading(hitTest(new Point(x-p.x,y-p.y)));}return null;}
    @Override public int getInsertPositionOffset(){return session.getSelection().caret();}
    @Override public AttributedCharacterIterator getCommittedText(int begin,int end,AttributedCharacterIterator.Attribute[] attributes){return new AttributedString(session.getDocument().text().substring(begin,end)).getIterator();}
    @Override public int getCommittedTextLength(){return session.getDocument().length();}
    @Override public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes){return null;}
    @Override public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes){var s=session.getSelection();return new AttributedString(session.getDocument().text().substring(s.start(),s.end())).getIterator();}
    @Override public AccessibleContext getAccessibleContext(){if(accessibleContext==null)accessibleContext=new AccessibleWordCanvas();return accessibleContext;}
    protected class AccessibleWordCanvas extends AccessibleJComponent implements AccessibleText {
        @Override public AccessibleRole getAccessibleRole(){return AccessibleRole.TEXT;}
        @Override public AccessibleText getAccessibleText(){return this;}
        @Override public String getAccessibleDescription(){
            var selected=session.getSelectedObject();
            if(selected.isPresent())return "Objeto selecionado: "+selected.get().type()+(selected.get().altText().isBlank()?"":" — "+selected.get().altText());
            return session.getContentSelection() instanceof WordCellSelection c?"Células selecionadas: "+c.rowCount()+" x "+c.columnCount():"Documento";
        }
        public int getIndexAtPoint(Point p){return hitTest(p);}
        public Rectangle getCharacterBounds(int index){return index<0||index>=getCharCount()?null:boundsAt(index);}
        public int getCharCount(){return session.getDocument().length();}
        public int getCaretPosition(){return session.getSelection().caret();}
        public String getAtIndex(int part,int index){return textAt(part,index,0);}
        public String getAfterIndex(int part,int index){return textAt(part,index,1);}
        public String getBeforeIndex(int part,int index){return textAt(part,index,-1);}
        public javax.swing.text.AttributeSet getCharacterAttribute(int index){return null;}
        public int getSelectionStart(){return session.getSelection().start();}
        public int getSelectionEnd(){return session.getSelection().end();}
        public String getSelectedText(){return session.getDocument().text().substring(getSelectionStart(),getSelectionEnd());}
        private String textAt(int part,int index,int direction){
            String text=session.getDocument().text();if(index<0||index>=text.length())return null;
            BreakIterator iterator=switch(part){case AccessibleText.WORD->BreakIterator.getWordInstance();case AccessibleText.SENTENCE->BreakIterator.getSentenceInstance();default->BreakIterator.getCharacterInstance();};
            iterator.setText(text);int end=iterator.following(index),start=iterator.previous();
            if(direction>0){start=end;end=iterator.following(start);}else if(direction<0){end=start;start=iterator.preceding(end);}
            return start<0||end<0?null:text.substring(start,end);
        }
    }
    @Override public void close(){if(closed)return;closed=true;pauseVisualWork();listener.close();}
}
