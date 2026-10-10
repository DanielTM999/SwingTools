package dtm.stools.component.panels.editor.powerpoint.render;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import javax.imageio.ImageIO;
import java.util.LinkedHashMap;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;

/** Renders the same logical slide in editing, thumbnails and presentation. */
public class PowerPointRenderer {
    private final PptTextLayout textLayout=new PptTextLayout();
    private final Map<PptObject,BufferedImage> images=new LinkedHashMap<>(16,.75f,true){
        @Override
        protected boolean removeEldestEntry(Map.Entry<PptObject,BufferedImage> e){return size()>32;}
    };
    public PptTextLayout textLayout(){return textLayout;}

    public void render(Graphics2D graphics, Presentation presentation, PptSlide slide,
                       Rectangle2D target, Map<String, Double> animationProgress) {
        render(graphics,presentation,slide,target,animationProgress,Map.of());
    }
    public void render(Graphics2D graphics, Presentation presentation, PptSlide slide,
                       Rectangle2D target, Map<String, Double> animationProgress,Map<String,BufferedImage> frames) {
        render(graphics,presentation,slide,target,animationProgress,frames,Map.of());
    }
    public void render(Graphics2D graphics, Presentation presentation, PptSlide slide,
                       Rectangle2D target, Map<String, Double> animationProgress,Map<String,BufferedImage> frames,
                       Map<String,PptAnimation.Effect> activeEffects) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(slide.background());g.fill(target);
            g.translate(target.getX(),target.getY());
            g.scale(target.getWidth()/presentation.width(),target.getHeight()/presentation.height());
            Shape oldClip=g.getClip();g.clip(new Rectangle2D.Double(0,0,presentation.width(),presentation.height()));
            for(PptObject object:slide.objects()) {
                Double progress=animationProgress.get(object.id());
                if(progress!=null && progress<0)continue;
                Graphics2D item=(Graphics2D)g.create();
                try {
                    PptAnimation.Effect effect=activeEffects.get(object.id());
                    if(progress!=null&&effect!=null)applyEffect(item,object,effect,Math.max(0,Math.min(1,progress)));
                    item.rotate(Math.toRadians(object.rotation()),object.x()+object.width()/2,object.y()+object.height()/2);
                    if(object.visual()!=null&&(object.visual().flipH()||object.visual().flipV())){
                        item.translate(object.x()+object.width()/2,object.y()+object.height()/2);
                        item.scale(object.visual().flipH()?-1:1,object.visual().flipV()?-1:1);
                        item.translate(-object.x()-object.width()/2,-object.y()-object.height()/2);
                    }
                    Color animatedFill=effect==PptAnimation.Effect.COLOR&&progress!=null
                            ?blend(object.fill(),new Color(255,211,73),Math.sin(Math.PI*Math.max(0,Math.min(1,progress)))):object.fill();
                    paintObject(item,object,frames.get(object.id()),animatedFill);
                } finally {item.dispose();}
            }
            g.setClip(oldClip);
        } finally {g.dispose();}
    }
    private void applyEffect(Graphics2D g,PptObject o,PptAnimation.Effect effect,double p){
        double cx=o.x()+o.width()/2,cy=o.y()+o.height()/2;
        switch(effect){
            case APPEAR,DISAPPEAR -> g.setComposite(AlphaComposite.SrcOver.derive(p>=1?1f:0f));
            case FADE_IN,FADE_OUT,TRANSPARENCY -> g.setComposite(AlphaComposite.SrcOver.derive((float)p));
            case FLY_IN -> g.translate((p-1)*o.width(),0);
            case FLY_OUT -> {g.translate((1-p)*o.width(),0);g.setComposite(AlphaComposite.SrcOver.derive((float)p));}
            case WIPE_IN,WIPE_OUT -> g.clip(new Rectangle2D.Double(o.x(),o.y(),Math.max(0.1,o.width()*p),o.height()));
            case SPLIT_IN,SPLIT_OUT -> g.clip(new Rectangle2D.Double(cx-o.width()*p/2,o.y(),Math.max(0.1,o.width()*p),o.height()));
            case ZOOM_IN,ZOOM_OUT -> {g.translate(cx,cy);g.scale(Math.max(0.01,p),Math.max(0.01,p));g.translate(-cx,-cy);}
            case PULSE,GROW_SHRINK -> {double scale=1+0.22*Math.sin(Math.PI*p);g.translate(cx,cy);g.scale(scale,scale);g.translate(-cx,-cy);}
            case SPIN -> g.rotate(Math.PI*2*p,cx,cy);
            case LINE -> g.translate(120*p,0);
            case ARC -> g.translate(120*p,-80*Math.sin(Math.PI*p));
            case POLYLINE -> g.translate(p<0.5?160*p:80, p<0.5?0:-120*(p-0.5));
            case COLOR -> {}
        }
    }
    private static Color blend(Color a,Color b,double fraction){
        return new Color((int)(a.getRed()*(1-fraction)+b.getRed()*fraction),
                (int)(a.getGreen()*(1-fraction)+b.getGreen()*fraction),
                (int)(a.getBlue()*(1-fraction)+b.getBlue()*fraction),a.getAlpha());
    }
    public Shape objectShape(PptObject o){
        double x=o.x(),y=o.y(),w=o.width(),h=o.height();String preset=o.visual()==null?switch(o.kind()){case ELLIPSE->"ellipse";case ROUND_RECTANGLE->"roundRect";case DIAMOND->"diamond";default->"rect";}:o.visual().preset();
        if(o.kind()==PptObject.Kind.CONNECTOR){var c=o.visual().connector();return new Line2D.Double(x+c.x1(),y+c.y1(),x+c.x2(),y+c.y2());}
        return switch(preset){case "ellipse"->new Ellipse2D.Double(x,y,w,h);case "roundRect"->new RoundRectangle2D.Double(x,y,w,h,o.visual()==null?40:2*o.visual().cornerRadius(),o.visual()==null?40:2*o.visual().cornerRadius());case "diamond"->{Path2D path=new Path2D.Double();path.moveTo(x+w/2,y);path.lineTo(x+w,y+h/2);path.lineTo(x+w/2,y+h);path.lineTo(x,y+h/2);path.closePath();yield path;}default->new Rectangle2D.Double(x,y,w,h);};
    }
    public boolean hit(PptObject o,java.awt.geom.Point2D point,double tolerance){
        java.awt.geom.AffineTransform transform=java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(o.rotation()),o.x()+o.width()/2,o.y()+o.height()/2);
        Shape shape=transform.createTransformedShape(objectShape(o));
        return o.kind()==PptObject.Kind.CONNECTOR?new BasicStroke((float)(Math.max(o.visual().stroke().width(),tolerance)*2)).createStrokedShape(shape).contains(point):shape.contains(point);
    }
    public Rectangle2D cellBounds(PptObject o,int row,int column){
        PptTable t=o.visual().table();double x=o.x(),y=o.y(),w=0,h=0;
        for(int i=0;i<column;i++)x+=t.columns().get(i);for(int i=0;i<row;i++)y+=t.heights().get(i);
        var cell=t.rows().get(row).get(column);for(int i=column;i<Math.min(t.columns().size(),column+cell.colSpan());i++)w+=t.columns().get(i);for(int i=row;i<Math.min(t.rows().size(),row+cell.rowSpan());i++)h+=t.heights().get(i);
        return new Rectangle2D.Double(x,y,w,h);
    }
    private void paintObject(Graphics2D g,PptObject object,BufferedImage frame,Color fill) {
        double x=object.x(),y=object.y(),w=object.width(),h=object.height();
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        switch(object.kind()) {
            case RECTANGLE,ELLIPSE,ROUND_RECTANGLE,DIAMOND,TEXT -> {
                Shape shape=objectShape(object);g.setColor(fill);g.fill(shape);
                PptStroke stroke=object.visual()==null?(object.kind()==PptObject.Kind.TEXT?PptStroke.none():new PptStroke(object.foreground(),2,"solid","none","none")):object.visual().stroke();
                paintStroke(g,shape,stroke);
                if(object.hasText())textLayout.layout(object.styledText(),w,h).paint(g,x,y);
            }
            case TABLE -> {
                var table=object.visual().table();
                for(int r=0;r<table.rows().size();r++)for(int c=0;c<table.columns().size();c++){
                    var cell=table.rows().get(r).get(c);if(cell.covered())continue;Rectangle2D bounds=cellBounds(object,r,c);
                    g.setColor(cell.fill());g.fill(bounds);textLayout.layout(cell.text(),bounds.getWidth(),bounds.getHeight()).paint(g,bounds.getX(),bounds.getY());
                    double bx=bounds.getX(),by=bounds.getY(),bw=bounds.getWidth(),bh=bounds.getHeight();
                    paintStroke(g,new Line2D.Double(bx,by,bx+bw,by),cell.top());paintStroke(g,new Line2D.Double(bx,by,bx,by+bh),cell.left());
                    paintStroke(g,new Line2D.Double(bx+bw,by,bx+bw,by+bh),cell.right());paintStroke(g,new Line2D.Double(bx,by+bh,bx+bw,by+bh),cell.bottom());
                }
            }
            case CONNECTOR -> {var c=object.visual().connector();PptStroke stroke=object.visual().stroke();paintStroke(g,objectShape(object),stroke);
                arrow(g,x+c.x2(),y+c.y2(),Math.atan2(c.y2()-c.y1(),c.x2()-c.x1()),stroke.tail(),stroke);
                arrow(g,x+c.x1(),y+c.y1(),Math.atan2(c.y1()-c.y2(),c.x1()-c.x2()),stroke.head(),stroke);
            }
            case IMAGE -> {
                try {
                    BufferedImage image=images.get(object);if(image==null){image=ImageIO.read(new ByteArrayInputStream(object.data()));if(image!=null)images.put(object,image);}
                    if(image!=null){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(image,(int)x,(int)y,(int)w,(int)h,null);}
                    else paintMediaPlaceholder(g,object,"Imagem indisponível");
                } catch(Exception error){paintMediaPlaceholder(g,object,"Imagem indisponível");}
            }
            case AUDIO, VIDEO -> {if(frame!=null&&object.kind()==PptObject.Kind.VIDEO)g.drawImage(frame,(int)x,(int)y,(int)w,(int)h,null);else paintMediaPlaceholder(g,object,object.kind()==PptObject.Kind.AUDIO?"♪ Áudio":"▶ Vídeo");}
        }
    }
    private void paintStroke(Graphics2D g,Shape shape,PptStroke stroke){
        if(stroke==null||stroke.width()<=0||stroke.color().getAlpha()==0)return;g.setColor(stroke.color());
        float width=(float)stroke.width();float[] dash="solid".equals(stroke.dash())?null:stroke.dash().toLowerCase().contains("dot")?new float[]{width,3*width}:new float[]{5*width,3*width};
        g.setStroke(new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,10,dash,0));g.draw(shape);
    }
    private void arrow(Graphics2D g,double x,double y,double angle,String type,PptStroke stroke){
        if("none".equals(type)||stroke.width()<=0)return;double size=Math.max(5,stroke.width()*4);Graphics2D a=(Graphics2D)g.create();try{
            a.translate(x,y);a.rotate(angle);a.setColor(stroke.color());
            if("oval".equals(type))a.fill(new Ellipse2D.Double(-size,-size/2,size,size));
            else {Path2D p=new Path2D.Double();p.moveTo(0,0);p.lineTo(-size,-size/2);if("diamond".equals(type))p.lineTo(-2*size,0);p.lineTo(-size,size/2);if(!"arrow".equals(type)){p.closePath();a.fill(p);}else{a.setStroke(new BasicStroke((float)stroke.width()));a.draw(p);}}
        }finally{a.dispose();}
    }
    private void paintMediaPlaceholder(Graphics2D g,PptObject object,String label){
        g.setColor(new Color(40,45,54));g.fill(new RoundRectangle2D.Double(object.x(),object.y(),object.width(),object.height(),12,12));
        g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,Math.max(18,object.fontSize())));
        g.drawString(label,(float)(object.x()+12),(float)(object.y()+Math.min(object.height()-8,38)));
    }
}
