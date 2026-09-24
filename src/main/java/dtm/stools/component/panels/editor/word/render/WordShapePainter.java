package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.SwingConstants;
import java.awt.*;
import java.awt.geom.*;

public final class WordShapePainter implements WordObjectPainter {
    @Override public void paint(Graphics2D graphics, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        if (!(object instanceof WordShape shape)) return;
        Graphics2D g = (Graphics2D)graphics.create();
        try { WordPaintSupport.quality(g); paintShape(g,shape,bounds); } finally { g.dispose(); }
    }
    static void paintShape(Graphics2D g, WordShape shape, Rectangle2D.Float bounds) {
        if (shape.shapeType() == WordShapeType.GROUP) {
            float sx = shape.width() == 0 ? 1 : bounds.width/shape.width(), sy = shape.height() == 0 ? 1 : bounds.height/shape.height();
            for (WordShape child : shape.children()) {
                Rectangle2D.Float r = new Rectangle2D.Float(bounds.x+child.x()*sx,bounds.y+child.y()*sy,child.width()*sx,child.height()*sy);
                Graphics2D c = (Graphics2D)g.create();
                try { if (child.rotation() != 0) c.rotate(Math.toRadians(child.rotation()),r.getCenterX(),r.getCenterY()); paintShape(c,child,r); } finally { c.dispose(); }
            }
            return;
        }
        Shape outline = geometry(shape.shapeType(),bounds);
        float scale = shape.width() <= 0 ? 1 : bounds.width/shape.width();
        if (shape.fill() != null && !shape.shapeType().isLinear()) { g.setColor(new Color(shape.fill())); g.fill(outline); }
        if (shape.stroke() != null && shape.strokeWidth() > 0) {
            g.setColor(new Color(shape.stroke())); g.setStroke(new BasicStroke(Math.max(0.3f,shape.strokeWidth()*scale),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.draw(outline);
            if (shape.arrowEnd() && shape.shapeType().isLinear()) arrow(g,shape.shapeType(),bounds,Math.max(4,shape.strokeWidth()*scale*4));
        }
        if (!shape.text().isBlank()) {
            float inset = 5*scale;
            Rectangle2D.Float area = new Rectangle2D.Float(bounds.x+inset,bounds.y+inset,Math.max(4,bounds.width-2*inset),Math.max(4,bounds.height-2*inset));
            boolean box = shape.shapeType() == WordShapeType.TEXT_BOX;
            WordPaintSupport.text(g,shape.text(),area,new Font(Font.SANS_SERIF,Font.PLAIN,1).deriveFont(Math.max(4,shape.fontSize()*scale)),new Color(shape.textColor()),box ? SwingConstants.LEFT : SwingConstants.CENTER,!box);
        }
    }
    private static void arrow(Graphics2D g, WordShapeType type, Rectangle2D.Float b, float size) {
        double angle = type == WordShapeType.ELBOW_CONNECTOR ? 0 : Math.atan2(b.height,b.width);
        float x = b.x+b.width, y = b.y+b.height;
        Path2D.Float head = new Path2D.Float();
        head.moveTo(x,y);
        head.lineTo(x-size*Math.cos(angle-0.4),y-size*Math.sin(angle-0.4));
        head.lineTo(x-size*Math.cos(angle+0.4),y-size*Math.sin(angle+0.4));
        head.closePath(); g.fill(head);
    }
    public static Shape geometry(WordShapeType type, Rectangle2D.Float r) {
        float x = r.x, y = r.y, w = r.width, h = r.height;
        return switch (type) {
            case ROUNDED_RECTANGLE -> new RoundRectangle2D.Float(x,y,w,h,Math.min(w,h)*0.33f,Math.min(w,h)*0.33f);
            case ELLIPSE -> new Ellipse2D.Float(x,y,w,h);
            case TRIANGLE -> polygon(new float[]{x+w/2,x+w,x},new float[]{y,y+h,y+h});
            case DIAMOND -> polygon(new float[]{x+w/2,x+w,x+w/2,x},new float[]{y,y+h/2,y+h,y+h/2});
            case PENTAGON -> regular(5,r,-Math.PI/2);
            case HEXAGON -> polygon(new float[]{x+w*0.25f,x+w*0.75f,x+w,x+w*0.75f,x+w*0.25f,x},new float[]{y,y,y+h/2,y+h,y+h,y+h/2});
            case RIGHT_ARROW -> polygon(new float[]{x,x+w*0.62f,x+w*0.62f,x+w,x+w*0.62f,x+w*0.62f,x},new float[]{y+h*0.25f,y+h*0.25f,y,y+h/2,y+h,y+h*0.75f,y+h*0.75f});
            case STAR -> star(r);
            case LINE, CONNECTOR -> new Line2D.Float(x,y,x+w,y+h);
            case ELBOW_CONNECTOR -> { Path2D.Float p = new Path2D.Float(); p.moveTo(x,y); p.lineTo(x+w/2,y); p.lineTo(x+w/2,y+h); p.lineTo(x+w,y+h); yield p; }
            default -> new Rectangle2D.Float(x,y,w,h);
        };
    }
    private static Shape polygon(float[] xs, float[] ys) {
        Path2D.Float p = new Path2D.Float(); p.moveTo(xs[0],ys[0]);
        for (int i = 1; i < xs.length; i++) p.lineTo(xs[i],ys[i]);
        p.closePath(); return p;
    }
    private static Shape regular(int sides, Rectangle2D.Float r, double start) {
        float[] xs = new float[sides], ys = new float[sides];
        for (int i = 0; i < sides; i++) { double a = start + 2*Math.PI*i/sides; xs[i] = (float)(r.getCenterX()+Math.cos(a)*r.width/2); ys[i] = (float)(r.getCenterY()+Math.sin(a)*r.height/2); }
        return polygon(xs,ys);
    }
    private static Shape star(Rectangle2D.Float r) {
        float[] xs = new float[10], ys = new float[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI/2 + Math.PI*i/5; float k = i % 2 == 0 ? 1 : 0.42f;
            xs[i] = (float)(r.getCenterX()+Math.cos(a)*r.width/2*k); ys[i] = (float)(r.getCenterY()+Math.sin(a)*r.height/2*k);
        }
        return polygon(xs,ys);
    }
}
