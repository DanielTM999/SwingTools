package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import org.w3c.dom.*;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

final class ExternalPptxSupport {
    private static final String P="http://schemas.openxmlformats.org/presentationml/2006/main";
    private static final String A="http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String R="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final long EMU_WIDTH=9144000;
    record ReadResult(Presentation presentation,boolean editable,List<String> diagnostics){}
    private ExternalPptxSupport(){}

    static ReadResult read(OpcPackage pkg)throws IOException {
        List<String> slidePaths=slidePaths(pkg);
        if(slidePaths.isEmpty())throw new IOException("PPTX has no slides");
        Document presentationXml=OoxmlXml.parse(pkg.part("ppt/presentation.xml"));
        Element slideSize=OoxmlXml.descendant(presentationXml.getDocumentElement(),"sldSz");
        int width=1280,height=720;double scale=width/(double)EMU_WIDTH;List<String> warnings=new ArrayList<>();
        if(slideSize!=null){double cx=number(slideSize,"cx",EMU_WIDTH),cy=number(slideSize,"cy",5143500);
            if(cx<=0||cy<=0)throw new IOException("Invalid PPTX slide size");height=Math.max(1,(int)Math.round(width*cy/cx));scale=width/cx;}
        List<PptSlide> slides=new ArrayList<>();boolean editable=true;
        for(int i=0;i<slidePaths.size();i++){
            String path=slidePaths.get(i);Document xml=OoxmlXml.parse(pkg.part(path));Element root=xml.getDocumentElement();
            if(OoxmlXml.child(root,"timing")!=null)editable=false;
            Element common=OoxmlXml.child(root,"cSld"),tree=OoxmlXml.child(common,"spTree");
            if(tree==null)throw new IOException("Slide has no shape tree: "+path);
            PptxStyleResolver.Context context=PptxStyleResolver.context(pkg,path);context.resolveFonts(root);
            Color background=PptxVisualXml.fill(OoxmlXml.path(common,"bg","bgPr"),context.background(),context.colors());
            String title=common.hasAttribute("name")?common.getAttribute("name"):"Slide "+(i+1);
            List<PptObject> objects=new ArrayList<>();Map<String,String> relationships=relationships(pkg,path);
            for(Element child:OoxmlXml.children(tree)){
                if("sp".equals(child.getLocalName())||"cxnSp".equals(child.getLocalName())||"graphicFrame".equals(child.getLocalName())&&OoxmlXml.descendant(child,"tbl")!=null){
                    Element nonVisual=OoxmlXml.descendant(child,"cNvPr");String id=objectId(nonVisual,i);
                    if(OoxmlXml.descendant(child,"custGeom")!=null){warnings.add("Slide "+(i+1)+": geometria personalizada não suportada (preservada no arquivo).");continue;}
                    objects.add(PptxVisualXml.shape(child,"external-object-"+i+"-"+nonVisual.getAttribute("id"),scale,context.colors(),context.placeholder(child)).withId(id));
                }else if("pic".equals(child.getLocalName()))objects.add(readPicture(pkg,child,relationships,path,i,width,height,scale));
                else if(!Set.of("nvGrpSpPr","grpSpPr","extLst").contains(child.getLocalName()))warnings.add("Slide "+(i+1)+": recurso não exibido: "+child.getLocalName()+" (preservado no arquivo).");
            }
            String transition="cut";Element transitionNode=OoxmlXml.child(root,"transition");
            if(transitionNode!=null&&!OoxmlXml.children(transitionNode).isEmpty())transition=OoxmlXml.children(transitionNode).getFirst().getLocalName();
            Map<Integer,String> objectIds=new HashMap<>();Map<String,String> links=new HashMap<>();
            for(Element child:OoxmlXml.children(tree)){Element nv=OoxmlXml.descendant(child,"cNvPr");if(nv!=null){int n=(int)number(nv,"id",0);String id=objectId(nv,i);objectIds.put(n,id);links.put("external-object-"+i+"-"+n,id);}}
            for(int j=0;j<objects.size();j++){PptObject o=objects.get(j);if(o.visual()!=null&&o.visual().connector()!=null){var c=o.visual().connector();objects.set(j,o.withVisual(o.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),links.getOrDefault(c.startId(),c.startId()),links.getOrDefault(c.endId(),c.endId()),c.startSite(),c.endSite()))));}}
            slides.add(new PptSlide("external-slide-"+i,title,background,objects,PptxTimingXml.read(root,objectIds),transition));
        }
        List<String> diagnostics=new ArrayList<>(editable?List.of("PPTX externo: texto, tabelas, formas e conectores editáveis; a estrutura dos slides é preservada.")
                :List.of("PPTX externo com linha do tempo não suportada: edição protegida para preservar animações."));
        diagnostics.addAll(warnings);
        return new ReadResult(new Presentation(width,height,slides),editable,diagnostics);
    }
    static void patch(OpcPackage original,Presentation before,Presentation after,OutputStream output)throws IOException {
        ExternalPptxPatcher.write(original,before,after,output);
    }
    private static PptObject readPicture(OpcPackage pkg,Element picture,Map<String,String> relationships,String path,int slide,int width,int height,double scale)throws IOException {
        Element props=OoxmlXml.path(picture,"nvPicPr","cNvPr");if(props==null)throw new IOException("Picture without ID");
        String id=objectId(props,slide);
        Element audio=OoxmlXml.descendant(picture,"audioFile"),video=OoxmlXml.descendant(picture,"videoFile");
        PptObject.Kind kind=audio!=null?PptObject.Kind.AUDIO:video!=null?PptObject.Kind.VIDEO:PptObject.Kind.IMAGE;
        Element source=audio!=null?audio:video!=null?video:OoxmlXml.descendant(picture,"blip");
        String rel=source==null?"":kind==PptObject.Kind.IMAGE?source.getAttributeNS(R,"embed"):source.getAttributeNS(R,"link");
        String target=relationships.get(rel);byte[] data=target==null?new byte[0]:pkg.part(resolve(path,target));
        String extension=target==null?"":target.toLowerCase(Locale.ROOT);
        String mime=switch(kind){
            case IMAGE->extension.endsWith(".png")?"image/png":"image/jpeg";
            case AUDIO->extension.endsWith(".mp3")?"audio/mpeg":"audio/wav";
            case VIDEO->extension.endsWith(".mp4")?"video/mp4":"video/x-msvideo";
            default->"application/octet-stream";
        };
        Element xf=OoxmlXml.path(picture,"spPr","xfrm"),off=OoxmlXml.child(xf,"off"),ext=OoxmlXml.child(xf,"ext");
        double[] bounds={number(off,"x",0)*scale,number(off,"y",0)*scale,Math.max(1,number(ext,"cx",2286000)*scale),Math.max(1,number(ext,"cy",1285875)*scale),number(xf,"rot",0)/60000};
        return new PptObject(id,kind,bounds[0],bounds[1],bounds[2],bounds[3],bounds[4],"",Color.WHITE,Color.BLACK,24,mime,data,new PptVisual("rect",PptStroke.none(),null,null,null,PptxVisualXml.bool(xf,"flipH",false),PptxVisualXml.bool(xf,"flipV",false),0));
    }
    static List<String> slidePaths(OpcPackage pkg)throws IOException {
        if(!pkg.contains("ppt/presentation.xml")||!pkg.contains("ppt/_rels/presentation.xml.rels"))throw new IOException("Missing PPTX presentation relationships");
        Map<String,String> rels=relationships(pkg,"ppt/presentation.xml");
        Element root=OoxmlXml.parse(pkg.part("ppt/presentation.xml")).getDocumentElement();Element ids=OoxmlXml.child(root,"sldIdLst");
        List<String> paths=new ArrayList<>();if(ids==null)return paths;
        for(Element id:OoxmlXml.children(ids,"sldId")){
            String rel=id.getAttributeNS(R,"id"),target=rels.get(rel);if(target==null)throw new IOException("Missing slide relationship: "+rel);
            String path=resolve("ppt/presentation.xml",target);if(!pkg.contains(path))throw new IOException("Missing slide part: "+path);paths.add(path);
        }
        return paths;
    }
    static Map<String,String> relationships(OpcPackage pkg,String source)throws IOException {
        int slash=source.lastIndexOf('/');String path=source.substring(0,slash+1)+"_rels/"+source.substring(slash+1)+".rels";
        if(!pkg.contains(path))return Map.of();
        Element root=OoxmlXml.parse(pkg.part(path)).getDocumentElement();Map<String,String> map=new HashMap<>();
        for(Element child:OoxmlXml.children(root))if("Relationship".equals(child.getLocalName())){
            if("External".equalsIgnoreCase(child.getAttribute("TargetMode")))throw new IOException("External relationship not supported: "+path);
            map.put(child.getAttribute("Id"),child.getAttribute("Target"));
        }
        return map;
    }
    static String resolve(String source,String target)throws IOException {
        boolean absolute=target.startsWith("/");if(absolute)target=target.substring(1);
        String combined=(absolute?"":source.substring(0,source.lastIndexOf('/')+1))+target.replace('\\','/');
        Deque<String> parts=new ArrayDeque<>();for(String segment:combined.split("/")){
            if(segment.isBlank()||segment.equals("."))continue;
            if(segment.equals("..")){if(parts.isEmpty())throw new IOException("Relationship escapes PPTX package");parts.removeLast();}
            else parts.addLast(segment);
        }
        String path=String.join("/",parts);try{OpcPackage.validateName(path);}catch(IllegalArgumentException error){throw new IOException("Invalid PPTX part target",error);}return path;
    }
    static String objectId(Element properties,int slide){
        Element custom=OoxmlXml.descendant(properties,"objectId");return custom!=null?custom.getAttribute("value"):"external-object-"+slide+"-"+properties.getAttribute("id");
    }
    private static double number(Element e,String attribute,double fallback){
        if(e==null||e.getAttribute(attribute).isBlank())return fallback;
        try{return Double.parseDouble(e.getAttribute(attribute));}catch(NumberFormatException error){return fallback;}
    }
}
