package dtm.stools.component.panels.editor.powerpoint.model;

/** Rendering details independent of legacy plain object fields. */
public record PptVisual(String preset,PptStroke stroke,PptText text,PptTable table,Connector connector,boolean flipH,boolean flipV,double cornerRadius) {
    public record Connector(double x1,double y1,double x2,double y2,String startId,String endId,int startSite,int endSite) {
        public Connector resize(double sx,double sy){return new Connector(x1*sx,y1*sy,x2*sx,y2*sy,startId,endId,startSite,endSite);}
    }
    public PptVisual withText(PptText value){return new PptVisual(preset,stroke,value,table,connector,flipH,flipV,cornerRadius);}
    public PptVisual withTable(PptTable value){return new PptVisual(preset,stroke,text,value,connector,flipH,flipV,cornerRadius);}
    public PptVisual withStroke(PptStroke value){return new PptVisual(preset,value,text,table,connector,flipH,flipV,cornerRadius);}
    public PptVisual withConnector(Connector value){return new PptVisual(preset,stroke,text,table,value,flipH,flipV,cornerRadius);}
    public PptVisual resize(double sx,double sy){return new PptVisual(preset,stroke,text,table==null?null:table.resize(table.width()*sx,table.height()*sy),connector==null?null:connector.resize(sx,sy),flipH,flipV,cornerRadius*Math.min(sx,sy));}
}
