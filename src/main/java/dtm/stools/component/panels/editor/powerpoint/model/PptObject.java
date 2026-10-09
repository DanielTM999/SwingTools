package dtm.stools.component.panels.editor.powerpoint.model;

import java.awt.Color;
import java.util.Objects;
import java.util.UUID;

/** One editable item on a slide. Binary payloads are copied at the API boundary. */
public final class PptObject {
    public enum Kind { TEXT, RECTANGLE, ELLIPSE, ROUND_RECTANGLE, DIAMOND, TABLE, CONNECTOR, IMAGE, AUDIO, VIDEO }
    private final String id;
    private final Kind kind;
    private final double x, y, width, height, rotation;
    private final String text, mimeType;
    private final Color fill, foreground;
    private final int fontSize;
    private final byte[] data;
    private final PptVisual visual;

    public PptObject(String id, Kind kind, double x, double y, double width, double height,
                     double rotation, String text, Color fill, Color foreground, int fontSize,
                     String mimeType, byte[] data) {
        this(id,kind,x,y,width,height,rotation,text,fill,foreground,fontSize,mimeType,data,null);
    }
    public PptObject(String id, Kind kind, double x, double y, double width, double height,
                     double rotation, String text, Color fill, Color foreground, int fontSize,
                     String mimeType, byte[] data, PptVisual visual) {
        this.id = Objects.requireNonNull(id); this.kind = Objects.requireNonNull(kind);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width) || !Double.isFinite(height)
                || !Double.isFinite(rotation) || width <= 0 || height <= 0 || fontSize < 1)
            throw new IllegalArgumentException("Invalid object geometry or font size");
        this.x=x; this.y=y; this.width=width; this.height=height; this.rotation=rotation;
        this.text=Objects.requireNonNullElse(text, ""); this.fill=Objects.requireNonNullElse(fill, Color.WHITE);
        this.foreground=Objects.requireNonNullElse(foreground, Color.BLACK); this.fontSize=fontSize;
        this.visual=visual;
        this.mimeType=Objects.requireNonNullElse(mimeType, ""); this.data=data == null ? new byte[0] : data.clone();
    }
    public static PptObject text(String value, double x, double y, double width, double height) {
        return new PptObject(UUID.randomUUID().toString(), Kind.TEXT, x,y,width,height,0,value,
                new Color(255,255,255,0),Color.BLACK,36,"",null);
    }
    public static PptObject shape(Kind kind, double x, double y, double width, double height) {
        if (kind != Kind.RECTANGLE && kind != Kind.ELLIPSE && kind != Kind.ROUND_RECTANGLE && kind != Kind.DIAMOND) throw new IllegalArgumentException("Not a shape");
        PptObject object=new PptObject(UUID.randomUUID().toString(),kind,x,y,width,height,0,"",
                new Color(52,116,210),Color.BLACK,28,"",null);
        return kind==Kind.ROUND_RECTANGLE||kind==Kind.DIAMOND?object.withVisual(new PptVisual(kind==Kind.ROUND_RECTANGLE?"roundRect":"diamond",new PptStroke(Color.BLACK,2,"solid","none","none"),null,null,null,false,false,Math.min(20,Math.min(width,height)/2))):object;
    }
    public static PptObject media(Kind kind, String mime, byte[] bytes, double x, double y, double width, double height) {
        if (kind != Kind.IMAGE && kind != Kind.AUDIO && kind != Kind.VIDEO) throw new IllegalArgumentException("Not media");
        return new PptObject(UUID.randomUUID().toString(),kind,x,y,width,height,0,"",Color.LIGHT_GRAY,
                Color.BLACK,24,mime,bytes);
    }
    public static PptObject table(int rows,int columns,double x,double y,double width,double height){
        return new PptObject(UUID.randomUUID().toString(),Kind.TABLE,x,y,width,height,0,"",Color.WHITE,Color.BLACK,24,"",null,
                new PptVisual("rect",PptStroke.none(),null,PptTable.create(rows,columns,width,height),null,false,false,0));
    }
    public static PptObject connector(double x1,double y1,double x2,double y2){
        double x=Math.min(x1,x2),y=Math.min(y1,y2),w=Math.max(1,Math.abs(x2-x1)),h=Math.max(1,Math.abs(y2-y1));
        return new PptObject(UUID.randomUUID().toString(),Kind.CONNECTOR,x,y,w,h,0,"",Color.WHITE,Color.BLACK,24,"",null,
                new PptVisual("straightConnector1",new PptStroke(Color.BLACK,2,"solid","none","triangle"),null,null,
                new PptVisual.Connector(x1-x,y1-y,x2-x,y2-y,null,null,0,0),false,false,0));
    }
    public PptVisual visual(){return visual;}
    public boolean hasText(){return kind==Kind.TEXT||visual!=null&&visual.text()!=null;}
    public PptText styledText(){return visual!=null&&visual.text()!=null?visual.text():PptText.plain(text,fontSize,foreground);}
    public PptObject withVisual(PptVisual value){return new PptObject(id,kind,x,y,width,height,rotation,value!=null&&value.text()!=null?value.text().text():text,fill,
            value!=null&&value.text()!=null?value.text().firstStyle().color():foreground,value!=null&&value.text()!=null?(int)Math.max(1,Math.round(value.text().firstStyle().size())):fontSize,mimeType,data,value);}
    public PptObject withStyledText(PptText value){return withVisual((visual==null?new PptVisual(kind==Kind.ELLIPSE?"ellipse":kind==Kind.ROUND_RECTANGLE?"roundRect":kind==Kind.DIAMOND?"diamond":"rect",kind==Kind.TEXT?PptStroke.none():new PptStroke(foreground,2,"solid","none","none"),null,null,null,false,false,20):visual).withText(value));}
    public PptObject withTable(PptTable value){return new PptObject(id,kind,x,y,value.width(),value.height(),rotation,text,fill,foreground,fontSize,mimeType,data,visual.withTable(value));}
    public PptObject withId(String value){return new PptObject(value,kind,x,y,width,height,rotation,text,fill,foreground,fontSize,mimeType,data,visual);}
    public String id(){return id;} public Kind kind(){return kind;}
    public double x(){return x;} public double y(){return y;} public double width(){return width;}
    public double height(){return height;} public double rotation(){return rotation;}
    public String text(){return text;} public Color fill(){return fill;} public Color foreground(){return foreground;}
    public int fontSize(){return fontSize;} public String mimeType(){return mimeType;}
    public byte[] data(){return data.clone();}
    public PptObject duplicate(){return new PptObject(UUID.randomUUID().toString(),kind,x+20,y+20,width,height,rotation,text,fill,foreground,fontSize,mimeType,data,visual==null||visual.connector()==null?visual:visual.withConnector(new PptVisual.Connector(visual.connector().x1(),visual.connector().y1(),visual.connector().x2(),visual.connector().y2(),null,null,0,0)));}
    public PptObject geometry(double nx,double ny,double nw,double nh){return new PptObject(id,kind,nx,ny,nw,nh,rotation,text,fill,foreground,fontSize,mimeType,data,visual==null?null:visual.resize(nw/width,nh/height));}
    public PptObject withText(String value){if(visual!=null&&visual.text()!=null)return withStyledText(visual.text().withText(value));return new PptObject(id,kind,x,y,width,height,rotation,value,fill,foreground,fontSize,mimeType,data,visual);}
    public PptObject withFill(Color value){return new PptObject(id,kind,x,y,width,height,rotation,text,value,foreground,fontSize,mimeType,data,visual);}
    public PptObject withForeground(Color value){return new PptObject(id,kind,x,y,width,height,rotation,text,fill,value,fontSize,mimeType,data,visual==null||visual.text()==null?visual:visual.withText(visual.text().mapStyles(s->s.color(value))));}
    public PptObject withFontSize(int value){return new PptObject(id,kind,x,y,width,height,rotation,text,fill,foreground,value,mimeType,data,visual==null||visual.text()==null?visual:visual.withText(visual.text().mapStyles(s->s.size(value))));}
    public PptObject withRotation(double value){return new PptObject(id,kind,x,y,width,height,value,text,fill,foreground,fontSize,mimeType,data,visual);}
}
