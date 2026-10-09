package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;

/** Self-contained PPTX writer and bounded reader. Proprietary editor metadata is isolated in one optional part. */
public class PptxCodec {
    private static final String P="http://schemas.openxmlformats.org/presentationml/2006/main";
    private static final String A="http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String R="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String REL="http://schemas.openxmlformats.org/package/2006/relationships";
    private static final long CX=9144000;
    private final OpcPackage.Limits limits;
    public PptxCodec(){this(OpcPackage.Limits.DEFAULT);}
    public PptxCodec(OpcPackage.Limits limits){this.limits=Objects.requireNonNull(limits);}
    public double textPointScale(ImportResult origin,Presentation presentation){
        double cx=CX;
        if(origin!=null)try{var pkg=OpcPackage.read(origin.originalBytes(),limits);cx=PptxVisualXml.num(OoxmlXml.descendant(OoxmlXml.parse(pkg.part("ppt/presentation.xml")).getDocumentElement(),"sldSz"),"cx",CX);}catch(IOException ignored){}
        return presentation.width()*12700/cx;
    }
    public Optional<String> validateEdit(ImportResult origin,Presentation current,Presentation next){
        if(origin==null)return Optional.empty();
        if(!origin.editable())return Optional.of("Esta apresentação contém recursos protegidos contra edição.");
        try{
            OpcPackage pkg=OpcPackage.read(origin.originalBytes(),limits);if(pkg.contains("ppt/swingtools.xml"))return Optional.empty();
            if(current.width()!=next.width()||current.height()!=next.height()||current.slides().size()!=next.slides().size())return Optional.of("A estrutura e o tamanho dos slides deste PPTX externo não podem ser alterados nesta versão.");
            List<String> paths=ExternalPptxSupport.slidePaths(pkg);
            for(int i=0;i<current.slides().size();i++){
                PptSlide a=current.slides().get(i),b=next.slides().get(i);if(a==b)continue;
                if(!a.id().equals(b.id())||!a.animations().equals(b.animations())||!a.transition().equals(b.transition()))return Optional.of("A ordem dos slides, animações e transições externas precisa ser preservada.");
                Element tree=OoxmlXml.path(OoxmlXml.parse(pkg.part(paths.get(i))).getDocumentElement(),"cSld","spTree");Map<String,Element> source=new HashMap<>();
                for(Element element:OoxmlXml.children(tree)){Element nv=OoxmlXml.descendant(element,"cNvPr");if(nv!=null)source.put(ExternalPptxSupport.objectId(nv,i),element);}
                Map<String,PptObject> previous=new HashMap<>();for(PptObject o:a.objects())previous.put(o.id(),o);
                for(PptObject o:b.objects()){
                    PptObject old=previous.get(o.id());Element element=source.get(o.id());if(old==null||element==null||ExternalPptxPatcher.same(old,o))continue;
                    if(old.kind()!=o.kind()||!Arrays.equals(old.data(),o.data()))return Optional.of("A substituição deste tipo de objeto ou mídia externa não está disponível.");
                    if(!Objects.equals(old.visual(),o.visual())&&(OoxmlXml.descendant(element,"fld")!=null||OoxmlXml.descendant(OoxmlXml.child(element,"txBody"),"extLst")!=null))return Optional.of("Este texto contém campos ou extensões que precisam ser preservados.");
                    if(o.kind()==PptObject.Kind.TABLE&&OoxmlXml.descendant(OoxmlXml.descendant(element,"tbl"),"extLst")!=null)return Optional.of("Esta tabela contém extensões não editáveis.");
                }
            }return Optional.empty();
        }catch(IOException error){return Optional.of("Não foi possível verificar a preservação do arquivo: "+error.getMessage());}
    }
    public record ImportResult(Presentation presentation,byte[] originalBytes,boolean editable,List<String> diagnostics) {
        public ImportResult {originalBytes=originalBytes.clone();diagnostics=List.copyOf(diagnostics);}
        @Override public byte[] originalBytes(){return originalBytes.clone();}
    }
    public ImportResult read(InputStream input)throws IOException {
        byte[] bytes=OpcPackage.readBounded(input,limits.compressedBytes());
        OpcPackage packageData=OpcPackage.read(bytes,limits);
        for(String name:packageData.names())if(name.endsWith(".rels")){
            Element relationships=OoxmlXml.parse(packageData.part(name)).getDocumentElement();
            for(Element relationship:OoxmlXml.children(relationships))
                if("External".equalsIgnoreCase(relationship.getAttribute("TargetMode")))
                    throw new IOException("External PPTX relationships are not supported: "+name);
        }
        if(packageData.contains("ppt/swingtools.xml")) {
            try {
                Presentation presentation=readOwn(packageData);
                boolean editable=isKnownPackage(packageData)&&matchesGeneratedPackage(packageData,presentation);
                return new ImportResult(presentation,bytes,editable,editable?List.of():List.of("Partes PPTX desconhecidas foram preservadas; a edição está protegida."));
            }catch(IllegalArgumentException error){throw new IOException("Invalid SwingTools presentation metadata",error);}
        }
        ExternalPptxSupport.ReadResult imported=ExternalPptxSupport.read(packageData);
        return new ImportResult(imported.presentation(),bytes,imported.editable(),imported.diagnostics());
    }
    public void write(Presentation presentation,OutputStream output)throws IOException {write(presentation,output,false);}
    private void write(Presentation presentation,OutputStream output,boolean legacyText)throws IOException {
        Objects.requireNonNull(presentation);Objects.requireNonNull(output);
        Map<String,byte[]> parts=new LinkedHashMap<>();
        parts.put("[Content_Types].xml",bytes(contentTypes(presentation.slides().size())));
        parts.put("_rels/.rels",bytes(rels("rId1",R+"/officeDocument","ppt/presentation.xml")));
        parts.put("ppt/presentation.xml",bytes(presentationXml(presentation)));
        parts.put("ppt/_rels/presentation.xml.rels",bytes(presentationRels(presentation.slides().size())));
        parts.put("ppt/slideMasters/slideMaster1.xml",bytes(slideMasterXml()));
        parts.put("ppt/slideMasters/_rels/slideMaster1.xml.rels",bytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\""+REL+"\"><Relationship Id=\"rId1\" Type=\""+R+"/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/><Relationship Id=\"rId2\" Type=\""+R+"/theme\" Target=\"../theme/theme1.xml\"/></Relationships>"));
        parts.put("ppt/slideLayouts/slideLayout1.xml",bytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:sldLayout xmlns:p=\""+P+"\" xmlns:a=\""+A+"\" type=\"blank\" preserve=\"1\"><p:cSld><p:spTree>"+groupRoot()+"</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"));
        parts.put("ppt/slideLayouts/_rels/slideLayout1.xml.rels",bytes(rels("rId1",R+"/slideMaster","../slideMasters/slideMaster1.xml")));
        parts.put("ppt/theme/theme1.xml",bytes(themeXml()));
        StringBuilder own=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><st:presentation xmlns:st=\"urn:swingtools:presentation:1\" width=\""+presentation.width()+"\" height=\""+presentation.height()+"\">");
        if(!legacyText)own.insert(own.length()-1," version=\"2\"");
        for(int i=0;i<presentation.slides().size();i++){
            PptSlide slide=presentation.slides().get(i);
            parts.put("ppt/slides/slide"+(i+1)+".xml",bytes(slideXml(presentation,slide,i,parts,legacyText)));
            parts.put("ppt/slides/_rels/slide"+(i+1)+".xml.rels",bytes(slideRels(slide,i)));
            own.append("<st:slide id=\"").append(escape(slide.id())).append("\" title=\"").append(escape(slide.title()))
                    .append("\" bg=\"").append(rgb(slide.background())).append("\" transition=\"").append(escape(slide.transition())).append("\">");
            for(PptObject object:slide.objects()){
                own.append("<st:object id=\"").append(escape(object.id())).append("\" kind=\"").append(object.kind())
                        .append("\" x=\"").append(object.x()).append("\" y=\"").append(object.y())
                        .append("\" w=\"").append(object.width()).append("\" h=\"").append(object.height())
                        .append("\" rotation=\"").append(object.rotation()).append("\" fill=\"").append(argb(object.fill()))
                        .append("\" foreground=\"").append(argb(object.foreground())).append("\" fontSize=\"").append(object.fontSize())
                        .append("\" mime=\"").append(escape(object.mimeType())).append("\">")
                        .append("<st:text>").append(escape(object.text())).append("</st:text>");
                if(object.visual()!=null){
                    var connector=object.visual().connector();
                    own.append("<st:visual");if(connector!=null){if(connector.startId()!=null)own.append(" start=\"").append(escape(connector.startId())).append("\"");if(connector.endId()!=null)own.append(" end=\"").append(escape(connector.endId())).append("\"");}
                    own.append('>').append(PptxVisualXml.xml(object,2,presentation.width()/(double)CX)).append("</st:visual>");
                }
                if(object.data().length>0){
                    int shapeId=slide.objects().indexOf(object)+2;
                    String mediaPath=switch(object.kind()){
                        case IMAGE->"ppt/media/image"+(i+1)+"_"+shapeId+"."+(object.mimeType().contains("png")?"png":"jpg");
                        case AUDIO,VIDEO->"ppt/media/media"+(i+1)+"_"+shapeId+"."+mediaExtension(object.mimeType());
                        default->"ppt/media/st"+(i+1)+"_"+slide.objects().indexOf(object)+".bin";
                    };
                    if(!parts.containsKey(mediaPath))parts.put(mediaPath,object.data());
                    own.append("<st:data part=\"").append(mediaPath).append("\"/>");
                }
                own.append("</st:object>");
            }
            for(PptAnimation animation:slide.animations())own.append("<st:animation id=\"").append(escape(animation.id()))
                    .append("\" target=\"").append(escape(animation.targetId())).append("\" effect=\"").append(animation.effect())
                    .append("\" start=\"").append(animation.start()).append("\" duration=\"").append(animation.durationMs())
                    .append("\" delay=\"").append(animation.delayMs()).append("\" repeat=\"").append(animation.repeat())
                    .append("\" direction=\"").append(escape(animation.direction())).append("\"/>");
            own.append("</st:slide>");
        }
        own.append("</st:presentation>");parts.put("ppt/swingtools.xml",bytes(own.toString()));
        new OpcPackage(parts).write(output);
    }
    public void write(Presentation presentation,ImportResult origin,OutputStream output)throws IOException {
        if(origin!=null && presentation.equals(origin.presentation())){output.write(origin.originalBytes());return;}
        if(origin!=null&&!origin.editable())throw new IOException("Este PPTX contém recursos que não podem ser preservados após edição; o salvamento foi bloqueado.");
        if(origin!=null){
            OpcPackage original=OpcPackage.read(origin.originalBytes(),limits);
            if(!original.contains("ppt/swingtools.xml")){
                ExternalPptxSupport.patch(original,origin.presentation(),presentation,output);
                return;
            }
        }
        write(presentation,output);
    }
    private Presentation readOwn(OpcPackage pkg)throws IOException {
        Element root=OoxmlXml.parse(pkg.part("ppt/swingtools.xml")).getDocumentElement();
        int width=integer(root,"width"),height=integer(root,"height");List<PptSlide> slides=new ArrayList<>();
        for(Element element:OoxmlXml.children(root,"slide")){
            List<PptObject> objects=new ArrayList<>();List<PptAnimation> animations=new ArrayList<>();
            for(Element child:OoxmlXml.children(element)){
                if("object".equals(child.getLocalName())){
                    Element data=OoxmlXml.child(child,"data");byte[] bytes=data==null?new byte[0]:pkg.part(data.getAttribute("part"));
                    Element visualNode=OoxmlXml.child(child,"visual");PptVisual visual=null;
                    if(visualNode!=null){Element shape=OoxmlXml.children(visualNode).getFirst();visual=PptxVisualXml.shape(shape,child.getAttribute("id"),width/(double)CX,Map.of(),null).visual();
                        if(visual.connector()!=null){var c=visual.connector();visual=visual.withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),visualNode.hasAttribute("start")?visualNode.getAttribute("start"):null,visualNode.hasAttribute("end")?visualNode.getAttribute("end"):null,c.startSite(),c.endSite()));}}
                    objects.add(new PptObject(child.getAttribute("id"),PptObject.Kind.valueOf(child.getAttribute("kind")),
                            number(child,"x"),number(child,"y"),number(child,"w"),number(child,"h"),number(child,"rotation"),
                            OoxmlXml.child(child,"text")==null?"":OoxmlXml.child(child,"text").getTextContent(),
                            color(child.getAttribute("fill")),color(child.getAttribute("foreground")),integer(child,"fontSize"),child.getAttribute("mime"),bytes,visual));
                }else if("animation".equals(child.getLocalName())){
                    animations.add(new PptAnimation(child.getAttribute("id"),child.getAttribute("target"),
                            PptAnimation.Effect.valueOf(child.getAttribute("effect")),PptAnimation.Start.valueOf(child.getAttribute("start")),
                            integer(child,"duration"),integer(child,"delay"),integer(child,"repeat"),child.getAttribute("direction")));
                }
            }
            slides.add(new PptSlide(element.getAttribute("id"),element.getAttribute("title"),
                    color(element.getAttribute("bg")),objects,animations,element.getAttribute("transition")));
        }
        try{return new Presentation(width,height,slides);}catch(IllegalArgumentException error){throw new IOException("Invalid presentation metadata",error);}
    }
    private static boolean isKnownPackage(OpcPackage pkg){
        for(String name:pkg.names())if(!name.equals("[Content_Types].xml")&&!name.equals("_rels/.rels")
                &&!name.equals("ppt/presentation.xml")&&!name.equals("ppt/_rels/presentation.xml.rels")
                &&!name.equals("ppt/slideMasters/slideMaster1.xml")&&!name.equals("ppt/slideMasters/_rels/slideMaster1.xml.rels")
                &&!name.equals("ppt/slideLayouts/slideLayout1.xml")&&!name.equals("ppt/slideLayouts/_rels/slideLayout1.xml.rels")
                &&!name.equals("ppt/theme/theme1.xml")&&!name.equals("ppt/swingtools.xml")
                &&!name.matches("ppt/slides/slide[0-9]+\\.xml")
                &&!name.matches("ppt/slides/_rels/slide[0-9]+\\.xml\\.rels")
                &&!name.matches("ppt/media/(image[0-9]+_[0-9]+\\.(png|jpg)|media[0-9]+_[0-9]+\\.(wav|avi|mp3|mp4|bin)|poster[0-9]+_[0-9]+\\.png|st[0-9]+_[0-9]+\\.bin)"))return false;
        return true;
    }
    private boolean matchesGeneratedPackage(OpcPackage original,Presentation presentation)throws IOException {
        ByteArrayOutputStream generated=new ByteArrayOutputStream();write(presentation,generated,!OoxmlXml.parse(original.part("ppt/swingtools.xml")).getDocumentElement().hasAttribute("version"));
        OpcPackage expected=OpcPackage.read(generated.toByteArray(),limits);
        if(!original.names().equals(expected.names()))return false;
        for(String name:original.names())if(!Arrays.equals(original.part(name),expected.part(name)))return false;
        return true;
    }
    private Presentation readBasic(OpcPackage pkg)throws IOException {
        if(!pkg.contains("ppt/presentation.xml"))throw new IOException("Not a PPTX presentation");
        Document xml=OoxmlXml.parse(pkg.part("ppt/presentation.xml"));Element size=OoxmlXml.descendant(xml.getDocumentElement(),"sldSz");
        int width=1280,height=720;
        if(size!=null){width=1280;height=(int)Math.round(1280*number(size,"cy")/number(size,"cx"));}
        List<PptSlide> slides=new ArrayList<>();
        for(int i=1;pkg.contains("ppt/slides/slide"+i+".xml");i++){
            Element root=OoxmlXml.parse(pkg.part("ppt/slides/slide"+i+".xml")).getDocumentElement();
            List<PptObject> objects=new ArrayList<>();Element tree=OoxmlXml.descendant(root,"spTree");
            if(tree!=null)for(Element shape:OoxmlXml.children(tree,"sp")){
                String text=OoxmlXml.text(shape);if(text.isBlank())continue;
                Element transform=OoxmlXml.descendant(shape,"xfrm");Element off=transform==null?null:OoxmlXml.child(transform,"off");
                Element ext=transform==null?null:OoxmlXml.child(transform,"ext");
                double x=off==null?80:number(off,"x")*width/CX,y=off==null?80:number(off,"y")*height/slideCy(width,height);
                double w=ext==null?width-160:number(ext,"cx")*width/CX,h=ext==null?100:number(ext,"cy")*height/slideCy(width,height);
                objects.add(PptObject.text(text,x,y,Math.max(1,w),Math.max(1,h)));
            }
            slides.add(new PptSlide(UUID.randomUUID().toString(),"Slide "+i,Color.WHITE,objects,List.of(),"cut"));
        }
        if(slides.isEmpty())slides.add(PptSlide.create("Slide 1"));
        return new Presentation(width,height,slides);
    }
    private String contentTypes(int count){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Default Extension=\"bin\" ContentType=\"application/octet-stream\"/><Default Extension=\"png\" ContentType=\"image/png\"/><Default Extension=\"jpg\" ContentType=\"image/jpeg\"/><Default Extension=\"wav\" ContentType=\"audio/wav\"/><Default Extension=\"avi\" ContentType=\"video/x-msvideo\"/><Default Extension=\"mp3\" ContentType=\"audio/mpeg\"/><Default Extension=\"mp4\" ContentType=\"video/mp4\"/><Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/><Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/><Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/><Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>");
        for(int i=1;i<=count;i++)b.append("<Override PartName=\"/ppt/slides/slide").append(i).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>");
        return b.append("</Types>").toString();
    }
    private String presentationXml(Presentation doc){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:presentation xmlns:p=\""+P+"\" xmlns:a=\""+A+"\" xmlns:r=\""+R+"\"><p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst><p:sldIdLst>");
        for(int i=0;i<doc.slides().size();i++)b.append("<p:sldId id=\"").append(256+i).append("\" r:id=\"rId").append(i+2).append("\"/>");
        return b.append("</p:sldIdLst><p:sldSz cx=\"").append(CX).append("\" cy=\"").append(slideCy(doc.width(),doc.height()))
                .append("\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/></p:presentation>").toString();
    }
    private String presentationRels(int count){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\""+REL+"\"><Relationship Id=\"rId1\" Type=\""+R+"/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>");
        for(int i=1;i<=count;i++)b.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"").append(R).append("/slide\" Target=\"slides/slide").append(i).append(".xml\"/>");
        return b.append("</Relationships>").toString();
    }
    private static String nativeId(PptSlide slide,String objectId){if(objectId==null)return null;for(int i=0;i<slide.objects().size();i++)if(slide.objects().get(i).id().equals(objectId))return "shape-"+(i+2);return null;}
    private String slideXml(Presentation doc,PptSlide slide,int slideIndex,Map<String,byte[]> parts,boolean legacyText){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:sld xmlns:p=\""+P+"\" xmlns:a=\""+A+"\" xmlns:r=\""+R+"\" xmlns:p14=\"http://schemas.microsoft.com/office/powerpoint/2010/main\"><p:cSld><p:bg><p:bgPr><a:solidFill><a:srgbClr val=\""+rgb(slide.background())+"\"/></a:solidFill><a:effectLst/></p:bgPr></p:bg><p:spTree>"+groupRoot());
        int id=2;
        for(PptObject object:slide.objects()){
            long cy=slideCy(doc.width(),doc.height());
            long x=emu(object.x(),doc.width(),CX),y=emu(object.y(),doc.height(),cy);
            long w=emu(object.width(),doc.width(),CX),h=emu(object.height(),doc.height(),cy);
            if(object.visual()!=null && object.kind()!=PptObject.Kind.IMAGE && object.kind()!=PptObject.Kind.AUDIO && object.kind()!=PptObject.Kind.VIDEO){
                PptObject encoded=object;var c=object.visual().connector();
                if(c!=null){String start=nativeId(slide,c.startId()),end=nativeId(slide,c.endId());encoded=object.withVisual(object.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),start,end,c.startSite(),c.endSite())));}
                b.append(PptxVisualXml.xml(encoded,id,doc.width()/(double)CX));
            }else if(object.kind()==PptObject.Kind.IMAGE && object.data().length>0){
                String part="ppt/media/image"+(slideIndex+1)+"_"+id+"."+(object.mimeType().contains("png")?"png":"jpg");
                parts.put(part,object.data());
                b.append("<p:pic><p:nvPicPr><p:cNvPr id=\"").append(id).append("\" name=\"Image ").append(id)
                        .append("\"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed=\"rId").append(id)
                        .append("\"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm rot=\"").append(Math.round(object.rotation()*60000)).append("\"><a:off x=\"").append(x).append("\" y=\"").append(y)
                        .append("\"/><a:ext cx=\"").append(w).append("\" cy=\"").append(h).append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr></p:pic>");
            }else if((object.kind()==PptObject.Kind.AUDIO||object.kind()==PptObject.Kind.VIDEO)&&object.data().length>0){
                String extension=mediaExtension(object.mimeType());
                parts.put("ppt/media/media"+(slideIndex+1)+"_"+id+"."+extension,object.data());
                parts.put("ppt/media/poster"+(slideIndex+1)+"_"+id+".png",poster(object));
                b.append("<p:pic><p:nvPicPr><p:cNvPr id=\"").append(id).append("\" name=\"")
                        .append(object.kind()==PptObject.Kind.AUDIO?"Audio ":"Video ").append(id)
                        .append("\"><a:hlinkClick r:id=\"\" action=\"ppaction://media\"/></p:cNvPr>")
                        .append("<p:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></p:cNvPicPr><p:nvPr><a:")
                        .append(object.kind()==PptObject.Kind.AUDIO?"audioFile":"videoFile")
                        .append(" r:link=\"rId").append(id).append("a\"/><p:extLst><p:ext uri=\"{DAA4B4D4-6D71-4841-9C94-3DE7FCFB9230}\"><p14:media r:embed=\"rId")
                        .append(id).append("m\"/></p:ext></p:extLst></p:nvPr></p:nvPicPr>")
                        .append("<p:blipFill><a:blip r:embed=\"rId").append(id).append("\"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>")
                        .append("<p:spPr><a:xfrm rot=\"").append(Math.round(object.rotation()*60000)).append("\"><a:off x=\"").append(x).append("\" y=\"").append(y)
                        .append("\"/><a:ext cx=\"").append(w).append("\" cy=\"").append(h).append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr></p:pic>");
            }else{
                String shape=switch(object.kind()){case ELLIPSE->"ellipse";case ROUND_RECTANGLE->"roundRect";case DIAMOND->"diamond";default->"rect";};
                b.append("<p:sp><p:nvSpPr><p:cNvPr id=\"").append(id).append("\" name=\"Object ").append(id)
                        .append("\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm rot=\"").append(Math.round(object.rotation()*60000)).append("\"><a:off x=\"").append(x).append("\" y=\"").append(y)
                        .append("\"/><a:ext cx=\"").append(w).append("\" cy=\"").append(h).append("\"/></a:xfrm><a:prstGeom prst=\"")
                        .append(shape).append("\"><a:avLst/></a:prstGeom>");
                if(object.fill().getAlpha()==0)b.append("<a:noFill/>");
                else b.append("<a:solidFill><a:srgbClr val=\"").append(rgb(object.fill())).append("\"/></a:solidFill>");
                b.append("</p:spPr><p:txBody>").append(legacyText?"<a:bodyPr/>":"<a:bodyPr lIns=\""+emu(6,doc.width(),CX)+"\" rIns=\""+emu(6,doc.width(),CX)+"\" tIns=\""+emu(6,doc.width(),CX)+"\" bIns=\""+emu(6,doc.width(),CX)+"\"/>")
                        .append("<a:lstStyle/><a:p><a:r><a:rPr lang=\"pt-BR\" sz=\"")
                        .append(legacyText?object.fontSize()*100:Math.round(object.fontSize()*CX/(double)doc.width()/127)).append("\"><a:solidFill><a:srgbClr val=\"").append(rgb(object.foreground()))
                        .append("\"/></a:solidFill></a:rPr><a:t>").append(escape(object.text()))
                        .append("</a:t></a:r></a:p></p:txBody></p:sp>");
            }
            id++;
        }
        b.append("</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>");
        if(!"cut".equals(slide.transition()))b.append("<p:transition><p:").append(switch(slide.transition()){case "fade"->"fade";case "wipe"->"wipe";case "push"->"push";default->"fade";}).append("/></p:transition>");
        b.append(PptxTimingXml.write(slide));
        return b.append("</p:sld>").toString();
    }
    private String slideRels(PptSlide slide,int slideIndex){
        StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\""+REL+"\"><Relationship Id=\"rId1\" Type=\""+R+"/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>");
        int id=2,index=0;
        for(PptObject object:slide.objects()){
            if(object.kind()==PptObject.Kind.IMAGE && object.data().length>0)b.append("<Relationship Id=\"rId").append(id)
                    .append("\" Type=\"").append(R).append("/image\" Target=\"../media/image")
                    .append(slideIndex+1).append("_").append(id).append(object.mimeType().contains("png")?".png":".jpg").append("\"/>");
            if((object.kind()==PptObject.Kind.AUDIO||object.kind()==PptObject.Kind.VIDEO)&&object.data().length>0){
                String target="../media/media"+(slideIndex+1)+"_"+id+"."+mediaExtension(object.mimeType());
                b.append("<Relationship Id=\"rId").append(id).append("\" Type=\"").append(R)
                        .append("/image\" Target=\"../media/poster").append(slideIndex+1).append("_").append(id).append(".png\"/>");
                b.append("<Relationship Id=\"rId").append(id).append("a\" Type=\"").append(R)
                        .append(object.kind()==PptObject.Kind.AUDIO?"/audio":"/video").append("\" Target=\"").append(target).append("\"/>");
                b.append("<Relationship Id=\"rId").append(id).append("m\" Type=\"http://schemas.microsoft.com/office/2007/relationships/media\" Target=\"")
                        .append(target).append("\"/>");
            }
            id++;index++;
        }
        return b.append("</Relationships>").toString();
    }
    private String slideMasterXml(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:sldMaster xmlns:p=\""+P+"\" xmlns:a=\""+A+"\" xmlns:r=\""+R+"\"><p:cSld><p:spTree>"+groupRoot()+"</p:spTree></p:cSld><p:clrMap accent1=\"accent1\" accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" bg1=\"lt1\" bg2=\"lt2\" folHlink=\"folHlink\" hlink=\"hlink\" tx1=\"dk1\" tx2=\"dk2\"/><p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst><p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles></p:sldMaster>";}
    private String themeXml(){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a:theme xmlns:a=\""+A+"\" name=\"SwingTools\"><a:themeElements><a:clrScheme name=\"SwingTools\"><a:dk1><a:srgbClr val=\"000000\"/></a:dk1><a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1><a:dk2><a:srgbClr val=\"202020\"/></a:dk2><a:lt2><a:srgbClr val=\"F4F4F4\"/></a:lt2><a:accent1><a:srgbClr val=\"3474D2\"/></a:accent1><a:accent2><a:srgbClr val=\"E26D5A\"/></a:accent2><a:accent3><a:srgbClr val=\"55A868\"/></a:accent3><a:accent4><a:srgbClr val=\"8172B3\"/></a:accent4><a:accent5><a:srgbClr val=\"64B5CD\"/></a:accent5><a:accent6><a:srgbClr val=\"D6A94A\"/></a:accent6><a:hlink><a:srgbClr val=\"0563C1\"/></a:hlink><a:folHlink><a:srgbClr val=\"954F72\"/></a:folHlink></a:clrScheme><a:fontScheme name=\"SwingTools\"><a:majorFont><a:latin typeface=\"Arial\"/></a:majorFont><a:minorFont><a:latin typeface=\"Arial\"/></a:minorFont></a:fontScheme><a:fmtScheme name=\"SwingTools\"><a:fillStyleLst><a:solidFill><a:schemeClr val=\"accent1\"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w=\"9525\"><a:solidFill><a:schemeClr val=\"accent1\"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val=\"lt1\"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>";}
    private static String groupRoot(){return "<p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr>";}
    private static String rels(String id,String type,String target){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\""+REL+"\"><Relationship Id=\""+id+"\" Type=\""+type+"\" Target=\""+target+"\"/></Relationships>";}
    private static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    private static String escape(String s){return OoxmlXml.escape(s);}
    private static String rgb(Color c){return String.format("%06X",c.getRGB()&0xFFFFFF);}
    private static String argb(Color c){return String.format("%08X",c.getRGB());}
    private static Color color(String s){return new Color((int)Long.parseLong(s,16),true);}
    private static int integer(Element e,String name){return Integer.parseInt(e.getAttribute(name));}
    private static double number(Element e,String name){return Double.parseDouble(e.getAttribute(name));}
    private static long emu(double value,int size,long emuSize){return Math.round(value*emuSize/size);}
    private static long slideCy(int width,int height){return Math.round(CX*(double)height/width);}
    private static String mediaExtension(String mime){return switch(mime.toLowerCase(Locale.ROOT)){
        case "audio/wav","audio/x-wav","audio/wave","audio/vnd.wave"->"wav";
        case "video/x-msvideo","video/avi"->"avi";
        case "audio/mpeg","audio/mp3"->"mp3";
        case "video/mp4"->"mp4";
        default->"bin";
    };}
    private static byte[] poster(PptObject object){
        PptObject.Kind kind=object.kind();
        if(kind==PptObject.Kind.VIDEO&&"avi".equals(mediaExtension(object.mimeType())))try{
            BufferedImage first=dtm.stools.component.panels.editor.powerpoint.provider.AviMjpegMediaProvider.firstFrame(object.data());
            ByteArrayOutputStream preview=new ByteArrayOutputStream();ImageIO.write(first,"png",preview);return preview.toByteArray();
        }catch(IOException ignored){}
        BufferedImage image=new BufferedImage(320,180,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{g.setColor(new Color(33,39,52));g.fillRect(0,0,320,180);g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,64));
            g.drawString(kind==PptObject.Kind.AUDIO?"♪":"▶",125,112);}finally{g.dispose();}
        try{ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
        catch(IOException error){throw new UncheckedIOException(error);}
    }
}
