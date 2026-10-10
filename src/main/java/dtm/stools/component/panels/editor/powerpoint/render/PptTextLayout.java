package dtm.stools.component.panels.editor.powerpoint.render;

import dtm.stools.component.panels.editor.powerpoint.model.PptText;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.*;
import java.util.List;

/** A single text layout for slide rendering, cells and inline editor placement. */
public final class PptTextLayout {
    public record Line(TextLayout text,float x,float baseline){}
    public record Layout(List<Line> lines,double height,Rectangle2D area) {
        public void paint(Graphics2D graphics,double x,double y){Graphics2D g=(Graphics2D)graphics.create();try{
            g.translate(x,y);g.clip(area);for(Line line:lines)line.text().draw(g,line.x(),line.baseline());
        }finally{g.dispose();}}
    }
    private record Key(PptText text,double width,double height){}
    private final Map<Key,Layout> cache=new LinkedHashMap<>(32,.75f,true){
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key,Layout> e){return size()>256;}};
    private static final FontRenderContext FRC=new FontRenderContext(null,true,true);
    public synchronized Layout layout(PptText text,double width,double height){return cache.computeIfAbsent(new Key(text,width,height),key->compute(text,width,height));}
    private Layout compute(PptText text,double width,double height){
        double usable=Math.max(1,width-text.left()-text.right());float cursor=0;List<Line> lines=new ArrayList<>();
        for(PptText.Paragraph p:text.paragraphs()){
            cursor+=p.before();String value=p.text();int start=0;
            while(start<=value.length()){
                int end=value.indexOf('\n',start);if(end<0)end=value.length();String part=value.substring(start,end);
                if(part.isEmpty()){
                    PptText.Style style=p.runs().isEmpty()?text.firstStyle():p.runs().getFirst().style();TextLayout blank=new TextLayout(" ",font(style),FRC);
                    cursor+=Math.max(blank.getAscent()+blank.getDescent()+blank.getLeading(),p.lineSpacing()>4?p.lineSpacing():0);
                }else{
                    AttributedString attributed=new AttributedString(part);int offset=0;
                    for(PptText.Run run:p.runs()){
                        int from=Math.max(start,offset),to=Math.min(end,offset+run.text().length());
                        if(to>from){attributed.addAttribute(TextAttribute.FONT,font(run.style()),from-start,to-start);attributed.addAttribute(TextAttribute.FOREGROUND,run.style().color(),from-start,to-start);if(run.style().underline())attributed.addAttribute(TextAttribute.UNDERLINE,TextAttribute.UNDERLINE_ON,from-start,to-start);}
                        offset+=run.text().length();
                    }
                    LineBreakMeasurer measurer=new LineBreakMeasurer(attributed.getIterator(),FRC);
                    while(measurer.getPosition()<part.length()){
                        TextLayout line=measurer.nextLayout((float)usable);if(line==null)break;
                        double natural=line.getAscent()+line.getDescent()+line.getLeading();double spacing=p.lineSpacing()>4?p.lineSpacing():natural*Math.max(.1,p.lineSpacing());
                        float x=(float)text.left();if("ctr".equals(p.alignment()))x+=(usable-line.getAdvance())/2;else if("r".equals(p.alignment()))x+=usable-line.getAdvance();
                        if("just".equals(p.alignment())&&measurer.getPosition()<part.length())line=line.getJustifiedLayout((float)usable);
                        lines.add(new Line(line,x,cursor+line.getAscent()));cursor+=spacing;
                    }
                }
                if(end==value.length())break;start=end+1;
            }cursor+=p.after();
        }
        double available=Math.max(0,height-text.top()-text.bottom());float shift=(float)text.top();
        if("ctr".equals(text.anchor()))shift+=Math.max(0,(available-cursor)/2);else if("b".equals(text.anchor()))shift+=Math.max(0,available-cursor);
        float dy=shift;return new Layout(lines.stream().map(l->new Line(l.text(),l.x(),l.baseline()+dy)).toList(),cursor,new Rectangle2D.Double(text.left(),text.top(),usable,available));
    }
    public static Font font(PptText.Style s){return new Font(s.family(),(s.bold()?Font.BOLD:0)|(s.italic()?Font.ITALIC:0),1).deriveFont((float)s.size());}
}
