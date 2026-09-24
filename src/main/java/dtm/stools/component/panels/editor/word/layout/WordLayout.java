package dtm.stools.component.panels.editor.word.layout;

import dtm.stools.component.panels.editor.word.model.*;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WordLayout(WordDocument document,List<Page> pages,Map<String,Integer> bookmarkPages) {
    public WordLayout { pages=List.copyOf(pages); bookmarkPages=bookmarkPages==null?Map.of():Map.copyOf(bookmarkPages); }
    public WordLayout(WordDocument document,List<Page> pages) { this(document,pages,Map.of()); }
    public enum Region { BODY, HEADER, FOOTER, NOTE }
    public record Page(int index,float width,float height,List<Line> lines,List<Decoration> decorations,List<ObjectBox> objects,
                       List<CellBox> cells,int number,WordPageSettings settings,WordDocument document) {
        public Page {
            lines=List.copyOf(lines);decorations=decorations==null?List.of():List.copyOf(decorations);
            objects=objects==null?List.of():List.copyOf(objects);cells=cells==null?List.of():List.copyOf(cells);
        }
        public Page(int index,float width,float height,List<Line> lines) { this(index,width,height,lines,List.of(),List.of(),List.of(),index+1,null,null); }
    }
    public record Line(int start,int end,float x,float baseline,TextLayout text,float width,Region region,boolean repeated,TextLayout marker,float markerX,String link) {
        public Line(int start,int end,float x,float baseline,TextLayout text) { this(start,end,x,baseline,text,text.getAdvance(),Region.BODY,false,null,0,null); }
        public float top() { return baseline-text.getAscent(); }
        public float bottom() { return baseline+text.getDescent()+text.getLeading(); }
        public boolean positional() { return region==Region.BODY&&!repeated&&start>=0; }
        public Line translate(float dx,float dy) { return new Line(start,end,x+dx,baseline+dy,text,width,region,repeated,marker,markerX+dx,link); }
        public Line as(Region value,boolean repeat) { return new Line(value==Region.BODY&&!repeat?start:-1,value==Region.BODY&&!repeat?end:-1,x,baseline,text,width,value,repeat,marker,markerX,link); }
    }
    public record ObjectBox(int offset,WordInlineObject object,float x,float y,float width,float height,boolean floating,Region region,boolean repeated) {
        public Rectangle2D.Float bounds() { return new Rectangle2D.Float(x,y,width,height); }
        public ObjectBox translate(float dx,float dy) { return new ObjectBox(offset,object,x+dx,y+dy,width,height,floating,region,repeated); }
        public ObjectBox as(Region value,boolean repeat) { return new ObjectBox(value==Region.BODY&&!repeat?offset:-1,object,x,y,width,height,floating,value,repeat); }
        public boolean interactive() { return region==Region.BODY&&!repeated&&offset>=0; }
        public boolean behindText() { return floating&&object.placement().wrap()==WordPlacement.Wrap.BEHIND_TEXT; }
    }
    public record Decoration(Kind kind,Shape shape,int color,float stroke,WordBorder.Style style) {
        public enum Kind { FILL, STROKE, LABEL_BOX }
        public Decoration translate(float dx,float dy) { return new Decoration(kind,AffineTransform.getTranslateInstance(dx,dy).createTransformedShape(shape),color,stroke,style); }
    }
    public record CellBox(UUID tableId,int row,int cell,int gridColumn,int gridSpan,Rectangle2D.Float bounds,boolean repeated,int depth,float[] columnEdges) {
        public CellBox translate(float dx,float dy) {
            float[] edges=columnEdges.clone();for(int i=0;i<edges.length;i++)edges[i]+=dx;
            return new CellBox(tableId,row,cell,gridColumn,gridSpan,new Rectangle2D.Float(bounds.x+dx,bounds.y+dy,bounds.width,bounds.height),repeated,depth,edges);
        }
        public CellBox repeat() { return new CellBox(tableId,row,cell,gridColumn,gridSpan,bounds,true,depth,columnEdges); }
    }
}
