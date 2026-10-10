package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import org.w3c.dom.*;
import java.awt.Color;
import java.util.*;

final class PptxVisualXml {
    static final String A="http://schemas.openxmlformats.org/drawingml/2006/main",P="http://schemas.openxmlformats.org/presentationml/2006/main";
    private PptxVisualXml(){}
    static List<Element> children(Element parent){return parent==null?List.of():OoxmlXml.children(parent);}
    static List<Element> children(Element parent,String name){return parent==null?List.of():OoxmlXml.children(parent,name);}
    static double num(Element e,String key,double fallback){if(e==null||!e.hasAttribute(key))return fallback;try{return Double.parseDouble(e.getAttribute(key));}catch(NumberFormatException ex){return fallback;}}
    static String attr(Element e,String key,String fallback){return e!=null&&e.hasAttribute(key)?e.getAttribute(key):fallback;}
    static boolean bool(Element e,String key,boolean fallback){return "1".equals(attr(e,key,fallback?"1":"0"))||"true".equals(attr(e,key,fallback?"true":"false"));}
    static Color fill(Element e,Color fallback,Map<String,Color> theme){
        if(e==null)return fallback;if(OoxmlXml.child(e,"noFill")!=null)return new Color(0,0,0,0);
        Element solid=OoxmlXml.child(e,"solidFill");if(solid==null)return fallback;
        Element node=children(solid).stream().findFirst().orElse(null);if(node==null)return fallback;
        Color color=fallback;String kind=node.getLocalName(),value=node.getAttribute("val");
        try{if("srgbClr".equals(kind))color=new Color(Integer.parseInt(value,16));
            else if("sysClr".equals(kind))color=new Color(Integer.parseInt(node.getAttribute("lastClr"),16));
            else if("schemeClr".equals(kind))color=theme.getOrDefault(value,fallback);
        }catch(NumberFormatException ignored){}
        double r=color.getRed()/255.,g=color.getGreen()/255.,b=color.getBlue()/255.,alpha=color.getAlpha()/255.;
        for(Element op:children(node)){double v=num(op,"val",100000)/100000.;switch(op.getLocalName()){
            case "alpha"->alpha=v;case "alphaMod"->alpha*=v;case "tint"->{r+=(1-r)*v;g+=(1-g)*v;b+=(1-b)*v;}
            case "shade","lumMod"->{r*=v;g*=v;b*=v;}case "lumOff"->{r+=v;g+=v;b+=v;}default->{}
        }}return new Color(clamp(r),clamp(g),clamp(b),clamp(alpha));
    }
    private static float clamp(double value){return (float)Math.max(0,Math.min(1,value));}
    static PptStroke stroke(Element line,double scale,Map<String,Color> theme){
        if(line==null)return PptStroke.none();Color color=fill(line,Color.BLACK,theme);
        return new PptStroke(color,num(line,"w",12700)*scale,attr(OoxmlXml.child(line,"prstDash"),"val","solid"),
                attr(OoxmlXml.child(line,"headEnd"),"type","none"),attr(OoxmlXml.child(line,"tailEnd"),"type","none"));
    }
    static PptText.Style style(Element props,PptText.Style base,double scale,Map<String,Color> theme){
        if(props==null)return base;String family=attr(OoxmlXml.child(props,"latin"),"typeface",base.family());
        if(family.startsWith("+"))family="Arial";
        return new PptText.Style(family,props.hasAttribute("sz")?num(props,"sz",1800)*127*scale:base.size(),
                bool(props,"b",base.bold()),bool(props,"i",base.italic()),props.hasAttribute("u")?!"none".equals(props.getAttribute("u")):base.underline(),fill(props,base.color(),theme));
    }
    static PptText text(Element body,Element inherited,double scale,Map<String,Color> theme){
        Element bp=OoxmlXml.child(body,"bodyPr"),ibp=OoxmlXml.child(inherited,"bodyPr");
        double l=num(bp,"lIns",num(ibp,"lIns",91440))*scale,t=num(bp,"tIns",num(ibp,"tIns",45720))*scale;
        double r=num(bp,"rIns",num(ibp,"rIns",91440))*scale,b=num(bp,"bIns",num(ibp,"bIns",45720))*scale;
        PptText.Style base=new PptText.Style("Arial",18*12700*scale,false,false,false,Color.BLACK);
        Element inheritedList=OoxmlXml.child(inherited,"lstStyle"),list=OoxmlXml.child(body,"lstStyle");
        base=style(OoxmlXml.path(inheritedList,"defPPr","defRPr"),base,scale,theme);
        base=style(OoxmlXml.path(list,"defPPr","defRPr"),base,scale,theme);
        List<PptText.Paragraph> paragraphs=new ArrayList<>();
        for(Element p:children(body,"p")){
            Element pp=OoxmlXml.child(p,"pPr");int level=(int)num(pp,"lvl",0)+1;
            Element inheritedLevel=OoxmlXml.child(inheritedList,"lvl"+level+"pPr"),levelProps=OoxmlXml.child(list,"lvl"+level+"pPr");
            PptText.Style def=style(OoxmlXml.child(inheritedLevel,"defRPr"),base,scale,theme);
            def=style(OoxmlXml.child(levelProps,"defRPr"),def,scale,theme);def=style(OoxmlXml.child(pp,"defRPr"),def,scale,theme);
            List<PptText.Run> runs=new ArrayList<>();
            for(Element run:children(p)){
                if("r".equals(run.getLocalName())||"fld".equals(run.getLocalName())){Element txt=OoxmlXml.child(run,"t");runs.add(new PptText.Run(txt==null?"":txt.getTextContent(),style(OoxmlXml.child(run,"rPr"),def,scale,theme)));}
                else if("br".equals(run.getLocalName()))runs.add(new PptText.Run("\n",style(OoxmlXml.child(run,"rPr"),def,scale,theme)));
            }
            if(runs.isEmpty())runs.add(new PptText.Run("",style(OoxmlXml.child(p,"endParaRPr"),def,scale,theme)));
            String alignment=attr(pp,"algn",attr(levelProps,"algn",attr(inheritedLevel,"algn",attr(OoxmlXml.child(list,"defPPr"),"algn","l"))));
            double before=spacing(pp,"spcBef",scale,0),after=spacing(pp,"spcAft",scale,0),line=spacing(pp,"lnSpc",scale,1);
            paragraphs.add(new PptText.Paragraph(runs,alignment,before,after,line));
        }
        return new PptText(paragraphs,l,t,r,b,attr(bp,"anchor",attr(ibp,"anchor","t")));
    }
    private static double spacing(Element pp,String name,double scale,double fallback){Element spacing=OoxmlXml.child(pp,name);Element points=OoxmlXml.child(spacing,"spcPts"),pct=OoxmlXml.child(spacing,"spcPct");return points!=null?num(points,"val",0)*127*scale: pct!=null?num(pct,"val",100000)/100000:fallback;}
    static PptObject shape(Element shape,String id,double scale,Map<String,Color> theme,Element inherited){
        boolean connector="cxnSp".equals(shape.getLocalName()),table="graphicFrame".equals(shape.getLocalName());
        Element props=OoxmlXml.child(shape,"spPr"),iprops=OoxmlXml.child(inherited,"spPr");
        Element xf=table?OoxmlXml.child(shape,"xfrm"):OoxmlXml.child(props,"xfrm");if(xf==null)xf=OoxmlXml.child(iprops,"xfrm");
        Element off=OoxmlXml.child(xf,"off"),ext=OoxmlXml.child(xf,"ext");
        double x=num(off,"x",0)*scale,y=num(off,"y",0)*scale,w=num(ext,"cx",2286000)*scale,h=num(ext,"cy",1285875)*scale;
        boolean flipH=bool(xf,"flipH",false),flipV=bool(xf,"flipV",false);
        String preset=attr(OoxmlXml.child(props,"prstGeom"),"prst",attr(OoxmlXml.child(iprops,"prstGeom"),"prst","rect"));
        Color fill=fill(props,fill(iprops,new Color(0,0,0,0),theme),theme);
        Element line=OoxmlXml.child(props,"ln");if(line==null)line=OoxmlXml.child(iprops,"ln");PptStroke stroke=stroke(line,scale,theme);
        Element tx=OoxmlXml.child(shape,"txBody");PptText text=tx==null?null:text(tx,OoxmlXml.child(inherited,"txBody"),scale,theme);
        PptObject.Kind kind=switch(preset){case "ellipse"->PptObject.Kind.ELLIPSE;case "roundRect"->PptObject.Kind.ROUND_RECTANGLE;case "diamond"->PptObject.Kind.DIAMOND;default->PptObject.Kind.RECTANGLE;};
        if(text!=null&&("rect".equals(preset))&&fill.getAlpha()==0&&stroke.width()==0)kind=PptObject.Kind.TEXT;
        PptTable grid=null;PptVisual.Connector link=null;
        if(table){kind=PptObject.Kind.TABLE;grid=table(OoxmlXml.descendant(shape,"tbl"),scale,theme);w=grid.width();h=grid.height();}
        if(connector){kind=PptObject.Kind.CONNECTOR;Element nv=OoxmlXml.path(shape,"nvCxnSpPr","cNvCxnSpPr"),start=OoxmlXml.child(nv,"stCxn"),end=OoxmlXml.child(nv,"endCxn");
            String prefix=id.substring(0,id.lastIndexOf('-')+1);
            link=new PptVisual.Connector(flipH?w:0,flipV?h:0,flipH?0:w,flipV?0:h,start==null?null:prefix+start.getAttribute("id"),end==null?null:prefix+end.getAttribute("id"),(int)num(start,"idx",0),(int)num(end,"idx",0));flipH=false;flipV=false;}
        double radius=Math.min(w,h)*.16;Element adj=OoxmlXml.path(props,"prstGeom","avLst");for(Element gd:children(adj,"gd")){String f=gd.getAttribute("fmla");if(f.startsWith("val "))try{radius=Math.min(w,h)*Double.parseDouble(f.substring(4))/100000;}catch(NumberFormatException ignored){}}
        PptVisual visual=new PptVisual(preset,stroke,text,grid,link,flipH,flipV,radius);
        return new PptObject(id,kind,x,y,Math.max(1,w),Math.max(1,h),num(xf,"rot",0)/60000,text==null?"":text.text(),fill,text==null?Color.BLACK:text.firstStyle().color(),text==null?24:(int)Math.max(1,Math.round(text.firstStyle().size())),"",null,visual);
    }
    static PptTable table(Element tbl,double scale,Map<String,Color> theme){
        List<Double> columns=children(OoxmlXml.child(tbl,"tblGrid"),"gridCol").stream().map(e->num(e,"w",914400)*scale).toList();
        List<Double> heights=new ArrayList<>();List<List<PptTable.Cell>> rows=new ArrayList<>();
        for(Element row:children(tbl,"tr")){heights.add(num(row,"h",457200)*scale);List<PptTable.Cell> cells=new ArrayList<>();
            for(Element tc:children(row,"tc")){
                Element pr=OoxmlXml.child(tc,"tcPr");PptText text=text(OoxmlXml.child(tc,"txBody"),null,scale,theme);
                text=new PptText(text.paragraphs(),num(pr,"marL",91440)*scale,num(pr,"marT",45720)*scale,num(pr,"marR",91440)*scale,num(pr,"marB",45720)*scale,attr(pr,"anchor",text.anchor()));
                cells.add(new PptTable.Cell(text,fill(pr,Color.WHITE,theme),stroke(OoxmlXml.child(pr,"lnT"),scale,theme),stroke(OoxmlXml.child(pr,"lnR"),scale,theme),stroke(OoxmlXml.child(pr,"lnB"),scale,theme),stroke(OoxmlXml.child(pr,"lnL"),scale,theme),(int)num(tc,"rowSpan",1),(int)num(tc,"gridSpan",1),bool(tc,"hMerge",false)||bool(tc,"vMerge",false)));
            }rows.add(cells);
        }return new PptTable(columns,heights,rows);
    }
    static String xml(PptObject o,int id,double scale){
        PptVisual v=o.visual();StringBuilder b=new StringBuilder();String tag=o.kind()==PptObject.Kind.TABLE?"graphicFrame":o.kind()==PptObject.Kind.CONNECTOR?"cxnSp":"sp";
        b.append("<p:").append(tag).append(" xmlns:p=\"").append(P).append("\" xmlns:a=\"").append(A).append("\">");
        String nv=tag.equals("graphicFrame")?"nvGraphicFramePr":tag.equals("cxnSp")?"nvCxnSpPr":"nvSpPr",cnv=tag.equals("graphicFrame")?"cNvGraphicFramePr":tag.equals("cxnSp")?"cNvCxnSpPr":"cNvSpPr";
        b.append("<p:").append(nv).append("><p:cNvPr id=\"").append(id).append("\" name=\"Object ").append(id).append("\"/><p:").append(cnv).append(">");
        if(v.connector()!=null){connection(b,v.connector().startId(),v.connector().startSite(),"stCxn");connection(b,v.connector().endId(),v.connector().endSite(),"endCxn");}
        b.append("</p:").append(cnv).append("><p:nvPr/></p:").append(nv).append('>');
        if(tag.equals("graphicFrame")){transform(b,o,scale,"p",true);b.append("<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/table\">").append(tableXml(v.table(),scale)).append("</a:graphicData></a:graphic>");}
        else {b.append("<p:spPr>");transform(b,o,scale,"a",true);b.append("<a:prstGeom prst=\"").append(escape(v.preset())).append("\"><a:avLst>");
            if("roundRect".equals(v.preset()))b.append("<a:gd name=\"adj\" fmla=\"val ").append(Math.round(v.cornerRadius()/Math.min(o.width(),o.height())*100000)).append("\"/>");
            b.append("</a:avLst></a:prstGeom>").append(fillXml(o.fill())).append(lineXml(v.stroke(),scale,"ln")).append("</p:spPr>");if(v.text()!=null)b.append(textXml(v.text(),scale,"p"));}
        return b.append("</p:").append(tag).append('>').toString();
    }
    private static void connection(StringBuilder b,String id,int site,String tag){if(id!=null&&id.matches(".*-[0-9]+"))b.append("<a:").append(tag).append(" id=\"").append(id.substring(id.lastIndexOf('-')+1)).append("\" idx=\"").append(site).append("\"/>");}
    private static void transform(StringBuilder b,PptObject o,double scale,String ns,boolean rotation){PptVisual v=o.visual();PptVisual.Connector c=v.connector();boolean fh=v.flipH(),fv=v.flipV();if(c!=null){fh=c.x2()<c.x1();fv=c.y2()<c.y1();}
        b.append('<').append(ns).append(":xfrm");if(rotation)b.append(" rot=\"").append(Math.round(o.rotation()*60000)).append("\" flipH=\"").append(fh?1:0).append("\" flipV=\"").append(fv?1:0).append('"');
        b.append("><a:off x=\"").append(Math.round(o.x()/scale)).append("\" y=\"").append(Math.round(o.y()/scale)).append("\"/><a:ext cx=\"").append(Math.round((c==null?o.width():Math.abs(c.x2()-c.x1()))/scale)).append("\" cy=\"").append(Math.round((c==null?o.height():Math.abs(c.y2()-c.y1()))/scale)).append("\"/></").append(ns).append(":xfrm>");}
    static String textXml(PptText text,double scale,String ns){StringBuilder b=new StringBuilder("<"+ns+":txBody><a:bodyPr lIns=\""+Math.round(text.left()/scale)+"\" tIns=\""+Math.round(text.top()/scale)+"\" rIns=\""+Math.round(text.right()/scale)+"\" bIns=\""+Math.round(text.bottom()/scale)+"\" anchor=\""+escape(text.anchor())+"\"/><a:lstStyle/>");
        for(PptText.Paragraph p:text.paragraphs()){
            b.append("<a:p><a:pPr algn=\"").append(escape(p.alignment())).append("\"><a:spcBef><a:spcPts val=\"").append(Math.round(p.before()/scale/127)).append("\"/></a:spcBef><a:spcAft><a:spcPts val=\"").append(Math.round(p.after()/scale/127)).append("\"/></a:spcAft><a:lnSpc><a:")
                    .append(p.lineSpacing()>4?"spcPts":"spcPct").append(" val=\"").append(Math.round(p.lineSpacing()>4?p.lineSpacing()/scale/127:p.lineSpacing()*100000)).append("\"/></a:lnSpc></a:pPr>");
            for(PptText.Run run:p.runs()){PptText.Style s=run.style();String[] chunks=run.text().split("\n",-1);for(int i=0;i<chunks.length;i++){
                if(i>0)b.append("<a:br/>");b.append("<a:r><a:rPr sz=\"").append(Math.round(s.size()/scale/127)).append("\" b=\"").append(s.bold()?1:0).append("\" i=\"").append(s.italic()?1:0).append("\" u=\"").append(s.underline()?"sng":"none").append("\">").append(fillXml(s.color())).append("<a:latin typeface=\"").append(escape(s.family())).append("\"/></a:rPr><a:t xml:space=\"preserve\">").append(escape(chunks[i])).append("</a:t></a:r>");}}
            b.append("</a:p>");}return b.append("</").append(ns).append(":txBody>").toString();}
    static String tableXml(PptTable table,double scale){StringBuilder b=new StringBuilder("<a:tbl><a:tblPr/><a:tblGrid>");for(double w:table.columns())b.append("<a:gridCol w=\"").append(Math.round(w/scale)).append("\"/>");b.append("</a:tblGrid>");
        for(int r=0;r<table.rows().size();r++){b.append("<a:tr h=\"").append(Math.round(table.heights().get(r)/scale)).append("\">");for(int c=0;c<table.columns().size();c++){PptTable.Cell cell=table.rows().get(r).get(c);
            b.append("<a:tc rowSpan=\"").append(cell.rowSpan()).append("\" gridSpan=\"").append(cell.colSpan()).append('"');if(cell.covered()){
                boolean horizontal=false,vertical=false;for(int rr=0;rr<=r;rr++)for(int cc=0;cc<=c;cc++){PptTable.Cell origin=table.rows().get(rr).get(cc);if(!origin.covered()&&rr+origin.rowSpan()>r&&cc+origin.colSpan()>c){horizontal=cc<c;vertical=rr<r;}}
                if(horizontal)b.append(" hMerge=\"1\"");if(vertical)b.append(" vMerge=\"1\"");}
            b.append('>').append(textXml(cell.text(),scale,"a")).append("<a:tcPr marL=\"").append(Math.round(cell.text().left()/scale)).append("\" marR=\"").append(Math.round(cell.text().right()/scale)).append("\" marT=\"").append(Math.round(cell.text().top()/scale)).append("\" marB=\"").append(Math.round(cell.text().bottom()/scale)).append("\" anchor=\"").append(cell.text().anchor()).append("\">")
                    .append(lineXml(cell.left(),scale,"lnL")).append(lineXml(cell.right(),scale,"lnR")).append(lineXml(cell.top(),scale,"lnT")).append(lineXml(cell.bottom(),scale,"lnB")).append(fillXml(cell.fill())).append("</a:tcPr></a:tc>");}b.append("</a:tr>");}return b.append("</a:tbl>").toString();}
    static String lineXml(PptStroke stroke,double scale,String tag){return "<a:"+tag+" w=\""+Math.round(stroke.width()/scale)+"\">"+fillXml(stroke.width()==0?new Color(0,0,0,0):stroke.color())+"<a:prstDash val=\""+escape(stroke.dash())+"\"/><a:headEnd type=\""+escape(stroke.head())+"\"/><a:tailEnd type=\""+escape(stroke.tail())+"\"/></a:"+tag+">";}
    static String fillXml(Color color){return color.getAlpha()==0?"<a:noFill/>":"<a:solidFill><a:srgbClr val=\""+String.format("%06X",color.getRGB()&0xFFFFFF)+"\"><a:alpha val=\""+Math.round(color.getAlpha()/255.*100000)+"\"/></a:srgbClr></a:solidFill>";}
    static String escape(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
