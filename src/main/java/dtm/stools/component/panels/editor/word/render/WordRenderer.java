package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.api.WordSelection;
import dtm.stools.component.panels.editor.word.layout.WordLayout;
import dtm.stools.component.panels.editor.word.model.WordBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Objects;

public class WordRenderer {
    private volatile WordObjectRegistry registry=WordObjectRegistry.defaults();

    public WordObjectRegistry getObjectRegistry(){return registry;}
    public void setObjectRegistry(WordObjectRegistry value){registry=Objects.requireNonNull(value);}

    public void paintPage(Graphics2D graphics,WordLayout.Page page,WordSelection selection,Color selectionColor) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            WordPaintSupport.quality(g);
            g.setColor(Color.WHITE); g.fill(new java.awt.geom.Rectangle2D.Float(0,0,page.width(),page.height()));
            for(var d:page.decorations()) if(d.kind()==WordLayout.Decoration.Kind.FILL){g.setColor(new Color(d.color()));g.fill(d.shape());}
            for(var o:page.objects()) if(o.behindText()) paintObject(g,page,o);
            for(var line:page.lines()) {
                if(selection!=null&&line.positional()&&selectionColor!=null) {
                    int from=Math.max(line.start(),selection.start()),to=Math.min(line.end(),selection.end());
                    if(from<to) {
                        Shape highlight=line.text().getLogicalHighlightShape(from-line.start(),to-line.start());
                        g.setColor(selectionColor); g.fill(AffineTransform.getTranslateInstance(line.x(),line.baseline()).createTransformedShape(highlight));
                    }
                }
                g.setColor(Color.BLACK);
                if(line.marker()!=null) line.marker().draw(g,line.markerX(),line.baseline());
                line.text().draw(g,line.x(),line.baseline());
            }
            for(var d:page.decorations()) {
                if(d.kind()==WordLayout.Decoration.Kind.FILL) continue;
                g.setColor(new Color(d.color()));
                float width=Math.max(0.25f,d.stroke());
                g.setStroke(switch(d.style()){
                    case DASHED->new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,new float[]{4*width+2,2*width+1},0);
                    case DOTTED->new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{0.1f,2*width+1},0);
                    default->new BasicStroke(width);
                });
                g.draw(d.shape());
                if(d.style()==WordBorder.Style.DOUBLE){var b=d.shape().getBounds2D();g.draw(new java.awt.geom.Rectangle2D.Double(b.getX()+width*2,b.getY()+width*2,Math.max(0,b.getWidth()-width*4),Math.max(0,b.getHeight()-width*4)));}
            }
            for(var o:page.objects()) if(!o.behindText()&&!o.object().textual()) paintObject(g,page,o);
        } finally { g.dispose(); }
    }
    protected void paintObject(Graphics2D g,WordLayout.Page page,WordLayout.ObjectBox box) {
        if(box.object().textual()) return;
        registry.paint(g,box.object(),box.bounds(),page.document());
    }
}
