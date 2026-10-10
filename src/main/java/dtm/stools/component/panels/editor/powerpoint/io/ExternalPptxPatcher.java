package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.*;
import org.w3c.dom.*;
import java.io.*;
import java.util.*;

final class ExternalPptxPatcher {
    private static final String R="http://schemas.openxmlformats.org/officeDocument/2006/relationships",REL="http://schemas.openxmlformats.org/package/2006/relationships";
    static void write(OpcPackage original,Presentation before,Presentation after,OutputStream output)throws IOException {
        if(before.width()!=after.width()||before.height()!=after.height()||before.slides().size()!=after.slides().size())throw new IOException("A estrutura dos slides externos não pode ser alterada nesta versão.");
        List<String> paths=ExternalPptxSupport.slidePaths(original);Map<String,byte[]> parts=new LinkedHashMap<>();for(String name:original.names())parts.put(name,original.part(name));
        Element size=OoxmlXml.descendant(OoxmlXml.parse(original.part("ppt/presentation.xml")).getDocumentElement(),"sldSz");double scale=before.width()/PptxVisualXml.num(size,"cx",9144000);
        for(int index=0;index<paths.size();index++){
            PptSlide old=before.slides().get(index),next=after.slides().get(index);
            if(!old.id().equals(next.id())||!old.animations().equals(next.animations())||!old.transition().equals(next.transition()))throw new IOException("Alterações de slides, animações ou transições externas não podem ser preservadas nesta versão.");
            if(old==next)continue;String path=paths.get(index);Document xml=OoxmlXml.parse(original.part(path));Element common=OoxmlXml.child(xml.getDocumentElement(),"cSld"),tree=OoxmlXml.child(common,"spTree");
            if(!old.title().equals(next.title()))common.setAttribute("name",next.title());
            if(!old.background().equals(next.background())){
                Element bg=OoxmlXml.child(common,"bg");if(bg==null){bg=xml.createElementNS(PptxVisualXml.P,"p:bg");common.insertBefore(bg,common.getFirstChild());}
                Element bp=OoxmlXml.child(bg,"bgPr");if(bp==null){bp=xml.createElementNS(PptxVisualXml.P,"p:bgPr");bg.appendChild(bp);}replaceFill(bp,PptxVisualXml.fillXml(next.background()));
            }
            Map<String,Element> elements=new LinkedHashMap<>();Map<String,Integer> ids=new HashMap<>();int maxId=1;
            for(Element element:OoxmlXml.children(tree)){Element nv=OoxmlXml.descendant(element,"cNvPr");if(nv==null)continue;int number=(int)PptxVisualXml.num(nv,"id",1);maxId=Math.max(maxId,number);
                String id=ExternalPptxSupport.objectId(nv,index);elements.put(id,element);ids.put(id,number);}
            Map<String,PptObject> oldObjects=new HashMap<>();for(PptObject o:old.objects())oldObjects.put(o.id(),o);
            Set<String> retained=new HashSet<>();for(PptObject o:next.objects()){retained.add(o.id());if(!ids.containsKey(o.id()))ids.put(o.id(),++maxId);}
            for(PptObject o:old.objects())if(!retained.contains(o.id())){Element element=elements.get(o.id());if(element!=null){assertNoReferences(xml,ids.get(o.id()));tree.removeChild(element);}}
            for(PptObject value:next.objects()){
                PptObject previous=oldObjects.get(value.id());Element element=elements.get(value.id());
                if(previous!=null&&element==null)throw new IOException("Objeto de origem ausente: "+value.id());
                if(previous!=null&&same(previous,value))continue;
                if(previous!=null&&(previous.kind()!=value.kind()||!Arrays.equals(previous.data(),value.data())))throw new IOException("Substituição de tipo ou mídia externa não suportada.");
                PptObject encoded=prepared(value,ids);
                Element generated;
                if(value.kind()==PptObject.Kind.IMAGE||value.kind()==PptObject.Kind.AUDIO||value.kind()==PptObject.Kind.VIDEO){
                    if(previous!=null){patchGeometry(element,value,scale);continue;}
                    generated=media(parts,path,value,ids.get(value.id()),scale);
                }else generated=OoxmlXml.parse(PptxVisualXml.xml(encoded,ids.get(value.id()),scale).getBytes(java.nio.charset.StandardCharsets.UTF_8)).getDocumentElement();
                if(previous==null){Element inserted=(Element)xml.importNode(generated,true);rememberId(inserted,value.id());tree.appendChild(inserted);elements.put(value.id(),inserted);}
                else patchObject(element,generated,previous,value,scale);
            }

            List<Element> ordered=new ArrayList<>();for(PptObject value:next.objects())ordered.add(elements.get(value.id()));
            List<Element> slots=OoxmlXml.children(tree).stream().filter(ordered::contains).toList();
            List<Node> markers=new ArrayList<>();for(Element slot:slots){Node marker=xml.createComment("object-order");tree.replaceChild(marker,slot);markers.add(marker);}
            for(int i=0;i<markers.size();i++)tree.replaceChild(ordered.get(i),markers.get(i));
            parts.put(path,OoxmlXml.bytes(xml));
        }
        new OpcPackage(parts).write(output);
    }
    static boolean same(PptObject a,PptObject b){return a.id().equals(b.id())&&a.kind()==b.kind()&&a.x()==b.x()&&a.y()==b.y()&&a.width()==b.width()&&a.height()==b.height()&&a.rotation()==b.rotation()&&a.text().equals(b.text())&&a.fill().equals(b.fill())&&a.foreground().equals(b.foreground())&&a.fontSize()==b.fontSize()&&a.mimeType().equals(b.mimeType())&&Objects.equals(a.visual(),b.visual())&&Arrays.equals(a.data(),b.data());}
    private static PptObject prepared(PptObject object,Map<String,Integer> ids){
        PptObject o=object.visual()==null?object.withStyledText(object.styledText()):object;
        if(o.visual().connector()!=null){var c=o.visual().connector();o=o.withVisual(o.visual().withConnector(new PptVisual.Connector(c.x1(),c.y1(),c.x2(),c.y2(),mapped(ids,c.startId()),mapped(ids,c.endId()),c.startSite(),c.endSite())));}return o;
    }
    private static String mapped(Map<String,Integer> ids,String id){return id!=null&&ids.containsKey(id)?"shape-"+ids.get(id):null;}
    private static void rememberId(Element element,String id){Element nv=OoxmlXml.descendant(element,"cNvPr");Document doc=element.getOwnerDocument();Element list=OoxmlXml.child(nv,"extLst");if(list==null){list=doc.createElementNS(PptxVisualXml.A,"a:extLst");nv.appendChild(list);}
        Element ext=doc.createElementNS(PptxVisualXml.A,"a:ext");ext.setAttribute("uri","{FCD17AD9-16E1-4C85-9A40-A023F1BE9C36}");Element custom=doc.createElementNS("urn:swingtools:presentation:2","st:objectId");custom.setAttribute("value",id);ext.appendChild(custom);list.appendChild(ext);}
    private static void assertNoReferences(Document xml,int id)throws IOException{
        NodeList nodes=xml.getElementsByTagNameNS(PptxVisualXml.P,"spTgt");for(int i=0;i<nodes.getLength();i++)if(((Element)nodes.item(i)).getAttribute("spid").equals(Integer.toString(id)))throw new IOException("Este objeto participa de uma animação não editável.");
        for(String tag:List.of("stCxn","endCxn")){NodeList references=xml.getElementsByTagNameNS(PptxVisualXml.A,tag);for(int i=references.getLength()-1;i>=0;i--){Element e=(Element)references.item(i);if(e.getAttribute("id").equals(Integer.toString(id)))e.getParentNode().removeChild(e);}}
    }
    private static void patchObject(Element original,Element generated,PptObject before,PptObject after,double scale)throws IOException{
        if(before.x()!=after.x()||before.y()!=after.y()||before.width()!=after.width()||before.height()!=after.height()||before.rotation()!=after.rotation()||!Objects.equals(before.visual().connector(),after.visual().connector()))patchGeometry(original,after,scale);
        if(after.kind()==PptObject.Kind.TABLE){if(!Objects.equals(before.visual().table(),after.visual().table()))mergeTable(OoxmlXml.descendant(original,"tbl"),OoxmlXml.descendant(generated,"tbl"));return;}
        Element props=OoxmlXml.child(original,"spPr"),newProps=OoxmlXml.child(generated,"spPr");
        if(!before.fill().equals(after.fill()))copyChildren(props,newProps,Set.of("noFill","solidFill","gradFill","blipFill","pattFill","grpFill"));
        if(!Objects.equals(before.visual().stroke(),after.visual().stroke()))copyChildren(props,newProps,Set.of("ln"));
        if(!Objects.equals(before.visual().text(),after.visual().text()))mergeText(OoxmlXml.child(original,"txBody"),OoxmlXml.child(generated,"txBody"));
        if(after.kind()==PptObject.Kind.CONNECTOR){Element nv=OoxmlXml.path(original,"nvCxnSpPr","cNvCxnSpPr"),newNv=OoxmlXml.path(generated,"nvCxnSpPr","cNvCxnSpPr");copyChildren(nv,newNv,Set.of("stCxn","endCxn"));}
    }
    private static void patchGeometry(Element element,PptObject value,double scale)throws IOException{
        PptObject encoded=prepared(value,Map.of());Element replacement=OoxmlXml.parse(PptxVisualXml.xml(encoded,2,scale).getBytes(java.nio.charset.StandardCharsets.UTF_8)).getDocumentElement();
        Element source=OoxmlXml.child(replacement,"xfrm"),parent=element;
        if(source==null){source=OoxmlXml.path(replacement,"spPr","xfrm");parent=OoxmlXml.child(element,"spPr");}
        if(parent==null)throw new IOException("Geometria não editável.");Element target=OoxmlXml.child(parent,"xfrm");
        if(target==null){parent.insertBefore(parent.getOwnerDocument().importNode(source,true),parent.getFirstChild());return;}
        for(String attr:List.of("rot","flipH","flipV")){if(source.hasAttribute(attr))target.setAttribute(attr,source.getAttribute(attr));else target.removeAttribute(attr);}copyChildren(target,source,Set.of("off","ext"));
    }
    private static void mergeText(Element old,Element next)throws IOException{
        if(old==null||next==null)throw new IOException("Texto de origem não editável.");
        Element bp=OoxmlXml.child(old,"bodyPr"),newBp=OoxmlXml.child(next,"bodyPr");if(bp==null)copyChildren(old,next,Set.of("bodyPr"));else for(String key:List.of("lIns","tIns","rIns","bIns","anchor"))bp.setAttribute(key,newBp.getAttribute(key));
        List<Element> paragraphs=OoxmlXml.children(old,"p"),replacement=OoxmlXml.children(next,"p");
        if(paragraphs.size()!=replacement.size()){
            if(OoxmlXml.descendant(old,"extLst")!=null||OoxmlXml.descendant(old,"fld")!=null)throw new IOException("A estrutura deste texto contém campos ou extensões que precisam ser preservados.");
            copyChildren(old,next,Set.of("p"));return;
        }
        for(int i=0;i<paragraphs.size();i++){
            Element p=paragraphs.get(i),q=replacement.get(i),pp=OoxmlXml.child(p,"pPr"),qp=OoxmlXml.child(q,"pPr");if(pp==null)copyChildren(p,q,Set.of("pPr"));else{pp.setAttribute("algn",qp.getAttribute("algn"));copyChildren(pp,qp,Set.of("lnSpc","spcBef","spcAft"));}List<Element> runs=OoxmlXml.children(p,"r"),newRuns=OoxmlXml.children(q,"r");
            if(runs.size()!=newRuns.size()||OoxmlXml.child(p,"br")!=null||OoxmlXml.child(q,"br")!=null){if(OoxmlXml.descendant(p,"fld")!=null||OoxmlXml.descendant(p,"extLst")!=null)throw new IOException("Este texto contém campos ou extensões não editáveis.");copyChildren(p,q,Set.of("r","br","endParaRPr"));}
            else for(int j=0;j<runs.size();j++){
                Element r=runs.get(j),s=newRuns.get(j),rp=OoxmlXml.child(r,"rPr"),sp=OoxmlXml.child(s,"rPr");copyChildren(r,s,Set.of("t"));
                if(rp==null)copyChildren(r,s,Set.of("rPr"));else{
                    for(String key:List.of("sz","b","i","u"))rp.setAttribute(key,sp.getAttribute(key));copyChildren(rp,sp,Set.of("solidFill","noFill","gradFill","latin","ea","cs"));
                }
            }
        }
    }
    private static void mergeTable(Element old,Element next)throws IOException{
        List<Element> rows=OoxmlXml.children(old,"tr"),newRows=OoxmlXml.children(next,"tr");boolean structure=rows.size()!=newRows.size();
        if(!structure)for(int i=0;i<rows.size();i++)if(OoxmlXml.children(rows.get(i),"tc").size()!=OoxmlXml.children(newRows.get(i),"tc").size())structure=true;
        if(structure){if(OoxmlXml.descendant(old,"extLst")!=null)throw new IOException("A tabela contém extensões que impedem alterações estruturais.");copyChildren(old,next,Set.of("tblGrid","tr"));return;}
        copyChildren(old,next,Set.of("tblGrid"));
        for(int i=0;i<rows.size();i++){Element row=rows.get(i),replacement=newRows.get(i);row.setAttribute("h",replacement.getAttribute("h"));List<Element> cells=OoxmlXml.children(row,"tc"),newCells=OoxmlXml.children(replacement,"tc");for(int j=0;j<cells.size();j++){
            Element cell=cells.get(j),newCell=newCells.get(j);for(String key:List.of("rowSpan","gridSpan","hMerge","vMerge")){if(newCell.hasAttribute(key))cell.setAttribute(key,newCell.getAttribute(key));else cell.removeAttribute(key);}
            mergeText(OoxmlXml.child(cell,"txBody"),OoxmlXml.child(newCell,"txBody"));Element pr=OoxmlXml.child(cell,"tcPr"),newPr=OoxmlXml.child(newCell,"tcPr");
            if(pr==null)copyChildren(cell,newCell,Set.of("tcPr"));else{for(String key:List.of("marL","marR","marT","marB","anchor"))pr.setAttribute(key,newPr.getAttribute(key));copyChildren(pr,newPr,Set.of("solidFill","noFill","gradFill","lnL","lnR","lnT","lnB"));}
        }}
    }
    private static void copyChildren(Element target,Element source,Set<String> names){
        if(target==null||source==null)return;for(Element child:OoxmlXml.children(target))if(names.contains(child.getLocalName()))target.removeChild(child);
        for(Element child:OoxmlXml.children(source))if(names.contains(child.getLocalName())){Node before=null;int order=order(target.getLocalName(),child.getLocalName());for(Element existing:OoxmlXml.children(target))if(order(target.getLocalName(),existing.getLocalName())>order){before=existing;break;}target.insertBefore(target.getOwnerDocument().importNode(child,true),before);}
    }
    private static int order(String parent,String name){
        if("extLst".equals(name))return 100;
        List<String> sequence=switch(parent){
            case "spPr"->List.of("xfrm","prstGeom","custGeom","noFill","solidFill","gradFill","blipFill","pattFill","grpFill","ln","effectLst","effectDag","scene3d","sp3d");
            case "p"->List.of("pPr","r","br","fld","endParaRPr");case "r"->List.of("rPr","t");case "txBody"->List.of("bodyPr","lstStyle","p");
            case "rPr"->List.of("ln","noFill","solidFill","gradFill","effectLst","highlight","uLnTx","uLn","uFillTx","uFill","latin","ea","cs","sym","hlinkClick","hlinkMouseOver","rtl");
            case "pPr"->List.of("lnSpc","spcBef","spcAft","buClrTx","buClr","buSzTx","buSzPct","buSzPts","buFontTx","buFont","buNone","buAutoNum","buChar","buBlip","tabLst","defRPr");
            case "tbl"->List.of("tblPr","tblGrid","tr");case "tc"->List.of("txBody","tcPr");case "tcPr"->List.of("lnL","lnR","lnT","lnB","lnTlToBr","lnBlToTr","cell3D","noFill","solidFill","gradFill","blipFill","pattFill","grpFill","headers");
            case "xfrm"->List.of("off","ext","chOff","chExt");default->List.of();};int index=sequence.indexOf(name);return index<0?90:index;
    }
    private static void replaceFill(Element props,String xml)throws IOException{Element wrapper=OoxmlXml.parse(("<a:x xmlns:a=\""+PptxVisualXml.A+"\">"+xml+"</a:x>").getBytes(java.nio.charset.StandardCharsets.UTF_8)).getDocumentElement();copyChildren(props,wrapper,Set.of("solidFill","noFill","gradFill","blipFill","pattFill"));}
    private static Element media(Map<String,byte[]> parts,String path,PptObject value,int shapeId,double scale)throws IOException{

        double width=9144000*scale;PptObject normalized=value.geometry(value.x()*1280/width,value.y()*1280/width,value.width()*1280/width,value.height()*1280/width);
        ByteArrayOutputStream data=new ByteArrayOutputStream();new PptxCodec().write(new Presentation(1280,720,List.of(PptSlide.create("Media").addObject(normalized))),data);
        OpcPackage generated=OpcPackage.read(data.toByteArray(),OpcPackage.Limits.DEFAULT);
        Element picture=OoxmlXml.child(OoxmlXml.path(OoxmlXml.parse(generated.part("ppt/slides/slide1.xml")).getDocumentElement(),"cSld","spTree"),"pic");
        OoxmlXml.descendant(picture,"cNvPr").setAttribute("id",Integer.toString(shapeId));
        String relPath=path.substring(0,path.lastIndexOf('/')+1)+"_rels/"+path.substring(path.lastIndexOf('/')+1)+".rels";
        Document rels=parts.containsKey(relPath)?OoxmlXml.parse(parts.get(relPath)):OoxmlXml.parse(("<Relationships xmlns=\""+REL+"\"/>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Document types=OoxmlXml.parse(parts.get("[Content_Types].xml"));Element originalTypes=types.getDocumentElement();
        for(Element entry:OoxmlXml.children(OoxmlXml.parse(generated.part("ppt/slides/_rels/slide1.xml.rels")).getDocumentElement())){
            String target=entry.getAttribute("Target");if(!target.startsWith("../media/"))continue;String source="ppt/"+target.substring(3),destination="ppt/media/"+UUID.randomUUID()+"-"+source.substring(source.lastIndexOf('/')+1),relId="st"+UUID.randomUUID().toString().replace("-","");
            parts.put(destination,generated.part(source));Element relationship=rels.createElementNS(REL,"Relationship");relationship.setAttribute("Id",relId);relationship.setAttribute("Type",entry.getAttribute("Type"));relationship.setAttribute("Target","../media/"+destination.substring(destination.lastIndexOf('/')+1));rels.getDocumentElement().appendChild(relationship);
            NodeList all=picture.getElementsByTagName("*");for(int i=0;i<all.getLength();i++){Element e=(Element)all.item(i);for(String key:List.of("embed","link"))if(e.getAttributeNS(R,key).equals(entry.getAttribute("Id")))e.setAttributeNS(R,"r:"+key,relId);}
        }
        for(Element entry:OoxmlXml.children(OoxmlXml.parse(generated.part("[Content_Types].xml")).getDocumentElement()))if("Default".equals(entry.getLocalName())&&OoxmlXml.children(originalTypes).stream().noneMatch(e->e.getAttribute("Extension").equals(entry.getAttribute("Extension"))))originalTypes.appendChild(types.importNode(entry,true));
        parts.put(relPath,OoxmlXml.bytes(rels));parts.put("[Content_Types].xml",OoxmlXml.bytes(types));return picture;
    }
}
