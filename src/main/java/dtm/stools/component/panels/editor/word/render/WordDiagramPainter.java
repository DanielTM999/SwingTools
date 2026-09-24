package dtm.stools.component.panels.editor.word.render;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.SwingConstants;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public final class WordDiagramPainter implements WordObjectPainter {
    private record Item(String text, List<String> children) {}

    @Override public void paint(Graphics2D graphics, WordInlineObject object, Rectangle2D.Float bounds, WordDocument document) {
        if (!(object instanceof WordDiagram diagram)) return;
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            WordPaintSupport.quality(g); g.clip(new Rectangle2D.Float(bounds.x-1,bounds.y-1,bounds.width+2,bounds.height+2));
            float scale = (float)Math.max(0.85,Math.min(2.5,Math.min(bounds.width/360.0,bounds.height/140.0)));
            Font font = new Font(Font.SANS_SERIF,Font.PLAIN,1).deriveFont(11f*scale), small = font.deriveFont(9f*scale);
            Color base = new Color(diagram.color());
            List<Item> items = items(diagram);
            Rectangle2D.Float r = new Rectangle2D.Float(bounds.x+4,bounds.y+4,bounds.width-8,bounds.height-8);
            switch (diagram.layout()) {
                case BASIC_LIST -> grid(g,items,r,base,font,small,false);
                case VERTICAL_LIST -> vertical(g,items,r,base,font,small);
                case BASIC_PROCESS -> process(g,items,r,base,font,false,scale);
                case CHEVRON_PROCESS -> process(g,items,r,base,font,true,scale);
                case BASIC_CYCLE -> cycle(g,items,r,base,font,scale,false);
                case BASIC_RADIAL -> cycle(g,items,r,base,font,scale,true);
                case HIERARCHY -> hierarchy(g,diagram.nodes(),r,base,font,scale);
                case BASIC_VENN -> venn(g,items,r,base,font);
                case BASIC_MATRIX -> grid(g,items.subList(0,Math.min(4,items.size())),r,base,font,small,true);
                case BASIC_PYRAMID -> pyramid(g,items,r,base,font);
            }
        } finally { g.dispose(); }
    }
    private static List<Item> items(WordDiagram diagram) {
        List<Item> items = new ArrayList<>();
        for (WordDiagramNode node : diagram.nodes()) {
            if (node.level() == 0 || items.isEmpty()) items.add(new Item(node.text(),new ArrayList<>()));
            else items.getLast().children().add(node.text());
        }
        return items;
    }
    private static Color shade(Color base, int index, int count) { return WordPaintSupport.tint(base,count <= 1 ? 0 : 0.45f*index/(count-1)); }
    private static void box(Graphics2D g, Shape shape, Color fill, String text, Font font, Rectangle2D textArea) {
        g.setColor(fill); g.fill(shape);
        g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.2f)); g.draw(shape);
        WordPaintSupport.text(g,text,textArea,font,WordPaintSupport.readable(fill),SwingConstants.CENTER,true);
    }
    private void grid(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font, Font small, boolean matrix) {
        int n = items.size(); if (n == 0) return;
        int columns = matrix ? 2 : (int)Math.ceil(Math.sqrt(n*r.width/Math.max(1,r.height)*0.6)); columns = Math.max(1,Math.min(n,columns));
        int rows = (int)Math.ceil(n/(double)columns);
        float gap = 6, w = (r.width-gap*(columns-1))/columns, h = (r.height-gap*(rows-1))/rows;
        for (int i = 0; i < n; i++) {
            float x = r.x + (i%columns)*(w+gap), y = r.y + (i/columns)*(h+gap);
            Item item = items.get(i);
            Shape s = new RoundRectangle2D.Float(x,y,w,h,8,8);
            String text = item.text() + (item.children().isEmpty() ? "" : "\n" + String.join("\n",item.children().stream().map(c -> "• " + c).toList()));
            box(g,s,shade(base,i,n),text,item.children().isEmpty() ? font : small,new Rectangle2D.Float(x+5,y+4,w-10,h-8));
        }
    }
    private void vertical(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font, Font small) {
        int n = items.size(); if (n == 0) return;
        float gap = 5, h = (r.height-gap*(n-1))/n;
        for (int i = 0; i < n; i++) {
            float y = r.y + i*(h+gap); Item item = items.get(i);
            float head = item.children().isEmpty() ? r.width : r.width*0.35f;
            box(g,new RoundRectangle2D.Float(r.x,y,head,h,6,6),shade(base,i,n),item.text(),font,new Rectangle2D.Float(r.x+4,y+2,head-8,h-4));
            if (!item.children().isEmpty()) {
                Shape detail = new Rectangle2D.Float(r.x+head+2,y,r.width-head-2,h);
                g.setColor(WordPaintSupport.tint(base,0.85f)); g.fill(detail);
                WordPaintSupport.text(g,String.join("\n",item.children().stream().map(c -> "• " + c).toList()),new Rectangle2D.Float(r.x+head+8,y+2,r.width-head-12,h-4),small,new Color(0x1F2937),SwingConstants.LEFT,true);
            }
        }
    }
    private void process(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font, boolean chevron, float scale) {
        int n = items.size(); if (n == 0) return;
        float arrow = chevron ? -r.width*0.03f : 18*scale, w = (r.width-arrow*(n-1))/n, h = Math.min(r.height,w*0.8f), y = r.y+(r.height-h)/2;
        for (int i = 0; i < n; i++) {
            float x = r.x + i*(w+arrow); Item item = items.get(i);
            Shape s;
            if (chevron) {
                float tip = h*0.3f; Path2D.Float p = new Path2D.Float();
                p.moveTo(x,y); p.lineTo(x+w-tip,y); p.lineTo(x+w,y+h/2); p.lineTo(x+w-tip,y+h); p.lineTo(x,y+h);
                if (i > 0) p.lineTo(x+tip,y+h/2); p.closePath(); s = p;
            } else s = new RoundRectangle2D.Float(x,y,w,h,8,8);
            String text = item.text() + (item.children().isEmpty() ? "" : "\n" + String.join("\n",item.children()));
            box(g,s,shade(base,i,n),text,font,new Rectangle2D.Float(x+(chevron?h*0.3f:4),y+4,w-(chevron?h*0.6f:8),h-8));
            if (!chevron && i < n-1) {
                float ax = x+w+arrow*0.2f, aw = arrow*0.6f, cy = y+h/2;
                Path2D.Float p = new Path2D.Float(); p.moveTo(ax,cy-aw*0.35f); p.lineTo(ax+aw*0.55f,cy-aw*0.35f); p.lineTo(ax+aw*0.55f,cy-aw*0.6f); p.lineTo(ax+aw,cy); p.lineTo(ax+aw*0.55f,cy+aw*0.6f); p.lineTo(ax+aw*0.55f,cy+aw*0.35f); p.lineTo(ax,cy+aw*0.35f); p.closePath();
                g.setColor(WordPaintSupport.tint(base,0.5f)); g.fill(p);
            }
        }
    }
    private void cycle(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font, float scale, boolean radial) {
        int n = items.size(); if (n == 0) return;
        float cx = (float)r.getCenterX(), cy = (float)r.getCenterY();
        int around = radial ? n-1 : n;
        float node = Math.min(r.width,r.height)*(around <= 3 ? 0.34f : 0.26f);
        float radius = Math.min(r.width,r.height)/2 - node/2;
        if (radial) {
            g.setColor(WordPaintSupport.tint(base,0.5f)); g.setStroke(new BasicStroke(1.5f*scale));
            for (int i = 0; i < around; i++) { double a = -Math.PI/2 + 2*Math.PI*i/Math.max(1,around); g.draw(new Line2D.Float(cx,cy,cx+(float)Math.cos(a)*radius,cy+(float)Math.sin(a)*radius)); }
            float c = node*1.15f;
            box(g,new Ellipse2D.Float(cx-c/2,cy-c/2,c,c),base,items.getFirst().text(),font,new Rectangle2D.Float(cx-c*0.36f,cy-c*0.36f,c*0.72f,c*0.72f));
        } else {
            g.setColor(WordPaintSupport.tint(base,0.55f)); g.setStroke(new BasicStroke(2.5f*scale));
            for (int i = 0; i < n; i++) {
                double a = -Math.PI/2 + 2*Math.PI*i/n, b = -Math.PI/2 + 2*Math.PI*(i+1)/n, gap = Math.asin(Math.min(1,node/2/radius))*1.15;
                g.draw(new Arc2D.Double(cx-radius,cy-radius,radius*2,radius*2,-Math.toDegrees(a+gap),-Math.toDegrees(b-a-2*gap),Arc2D.OPEN));
            }
        }
        for (int i = 0; i < around; i++) {
            Item item = items.get(radial ? i+1 : i);
            double a = -Math.PI/2 + 2*Math.PI*i/Math.max(1,around);
            float x = cx+(float)Math.cos(a)*radius-node/2, y = cy+(float)Math.sin(a)*radius-node/2;
            box(g,new Ellipse2D.Float(x,y,node,node),shade(base,i+1,around+1),item.text(),font,new Rectangle2D.Float(x+node*0.15f,y+node*0.15f,node*0.7f,node*0.7f));
        }
    }
    private void hierarchy(Graphics2D g, List<WordDiagramNode> nodes, Rectangle2D.Float r, Color base, Font font, float scale) {
        int depth = 0; for (WordDiagramNode n : nodes) depth = Math.max(depth,n.level());
        List<List<Integer>> levels = new ArrayList<>(); for (int i = 0; i <= depth; i++) levels.add(new ArrayList<>());
        int[] parent = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            levels.get(nodes.get(i).level()).add(i); parent[i] = -1;
            for (int k = i-1; k >= 0; k--) if (nodes.get(k).level() < nodes.get(i).level()) { parent[i] = k; break; }
        }
        float gap = 10*scale, h = Math.min(40*scale,(r.height-gap*depth)/(depth+1));
        float[][] centers = new float[nodes.size()][2];
        for (int level = 0; level <= depth; level++) {
            List<Integer> row = levels.get(level); int count = row.size(); if (count == 0) continue;
            float w = Math.min(120*scale,(r.width-gap*(count-1))/count), total = count*w+gap*(count-1), x = r.x+(r.width-total)/2, y = r.y+level*(h+gap*2);
            for (int k = 0; k < count; k++) { int i = row.get(k); centers[i][0] = x+k*(w+gap)+w/2; centers[i][1] = y; }
        }
        g.setColor(WordPaintSupport.tint(base,0.4f)); g.setStroke(new BasicStroke(1.2f*scale));
        for (int i = 0; i < nodes.size(); i++) if (parent[i] >= 0) {
            float px = centers[parent[i]][0], py = centers[parent[i]][1]+h, x = centers[i][0], y = centers[i][1], mid = (py+y)/2;
            Path2D.Float p = new Path2D.Float(); p.moveTo(px,py); p.lineTo(px,mid); p.lineTo(x,mid); p.lineTo(x,y); g.draw(p);
        }
        for (int level = 0; level <= depth; level++) {
            List<Integer> row = levels.get(level); int count = row.size(); if (count == 0) continue;
            float w = Math.min(120*scale,(r.width-gap*(count-1))/count);
            for (int i : row) {
                float x = centers[i][0]-w/2, y = centers[i][1];
                box(g,new RoundRectangle2D.Float(x,y,w,h,6,6),shade(base,level,depth+1),nodes.get(i).text(),font,new Rectangle2D.Float(x+3,y+2,w-6,h-4));
            }
        }
    }
    private void venn(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font) {
        int n = Math.min(items.size(),4); if (n == 0) return;
        float d = Math.min(r.height,r.width/(1+0.6f*(n-1))), total = d+(n-1)*d*0.6f, x0 = (float)r.getCenterX()-total/2, y = (float)r.getCenterY()-d/2;
        for (int i = 0; i < n; i++) {
            Color c = WordPaintSupport.palette(i == 0 ? 0 : i); if (i == 0) c = base;
            g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),130)); g.fill(new Ellipse2D.Float(x0+i*d*0.6f,y,d,d));
        }
        for (int i = 0; i < n; i++) WordPaintSupport.text(g,items.get(i).text(),new Rectangle2D.Float(x0+i*d*0.6f+d*0.18f,y+d*0.3f,d*0.64f,d*0.4f),font,new Color(0x111827),SwingConstants.CENTER,true);
    }
    private void pyramid(Graphics2D g, List<Item> items, Rectangle2D.Float r, Color base, Font font) {
        int n = items.size(); if (n == 0) return;
        float h = r.height/n, cx = (float)r.getCenterX();
        for (int i = 0; i < n; i++) {
            float top = r.y+i*h, bottom = top+h, wt = r.width*i/n, wb = r.width*(i+1)/n;
            Path2D.Float p = new Path2D.Float(); p.moveTo(cx-wt/2,top); p.lineTo(cx+wt/2,top); p.lineTo(cx+wb/2,bottom-1); p.lineTo(cx-wb/2,bottom-1); p.closePath();
            box(g,p,shade(base,i,n),items.get(i).text(),font,new Rectangle2D.Float(cx-wb*0.35f,top+2,wb*0.7f,h-4));
        }
    }
}
