package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.word.io.ooxml.*;
import org.w3c.dom.Element;
import java.awt.Color;
import java.io.IOException;
import java.util.*;

final class PptxStyleResolver {
    record Context(Map<String,Color> colors,Color background,Element layout,Element master,Element defaults,String majorFont,String minorFont) {
        void resolveFonts(Element root){if(root==null)return;for(String tag:List.of("latin","ea","cs"))for(Element e:OoxmlXml.descendants(root,tag)){String family=e.getAttribute("typeface");if(family.startsWith("+mj"))e.setAttribute("typeface",majorFont);else if(family.startsWith("+mn"))e.setAttribute("typeface",minorFont);}}
        Element placeholder(Element shape){
            Element ph=OoxmlXml.descendant(shape,"ph");if(ph==null)return null;
            Element local=find(layout,ph),base=find(master,local==null?ph:OoxmlXml.descendant(local,"ph"));
            if(base==null&&local==null)return null;Element inherited=(Element)(base==null?local:base).cloneNode(true);
            if(base!=null&&local!=null){Element targetProps=OoxmlXml.child(inherited,"spPr"),props=OoxmlXml.child(local,"spPr");if(targetProps!=null&&props!=null)overlay(targetProps,props);}
            Element body=OoxmlXml.child(inherited,"txBody");if(body!=null){
                Element list=body.getOwnerDocument().createElementNS(PptxVisualXml.A,"a:lstStyle");if(defaults!=null)overlay(list,defaults);
                String type=PptxVisualXml.attr(ph,"type","obj");Element styles=OoxmlXml.path(master,"txStyles",Set.of("title","ctrTitle").contains(type)?"titleStyle":Set.of("body","subTitle","obj").contains(type)?"bodyStyle":"otherStyle");if(styles!=null)overlay(list,styles);
                Element own=OoxmlXml.child(body,"lstStyle");if(own!=null)overlay(list,own);
                Element localBody=OoxmlXml.child(local,"txBody");if(localBody!=null){Element bp=OoxmlXml.child(body,"bodyPr"),localBp=OoxmlXml.child(localBody,"bodyPr");if(bp!=null&&localBp!=null){var attrs=localBp.getAttributes();for(int i=0;i<attrs.getLength();i++)bp.setAttribute(attrs.item(i).getNodeName(),attrs.item(i).getNodeValue());}Element ls=OoxmlXml.child(localBody,"lstStyle");if(ls!=null)overlay(list,ls);}
                if(own!=null)body.replaceChild(list,own);else body.appendChild(list);
            }resolveFonts(inherited);return inherited;
        }
        private void overlay(Element target,Element source){for(Element child:OoxmlXml.children(source)){Element existing=OoxmlXml.child(target,child.getLocalName());if(existing!=null)target.removeChild(existing);target.appendChild(target.getOwnerDocument().importNode(child,true));}}
        private Element find(Element root,Element ph){
            if(root==null)return null;for(Element candidate:OoxmlXml.children(OoxmlXml.path(root,"cSld","spTree"),"sp")){
                Element other=OoxmlXml.descendant(candidate,"ph");if(other!=null&&(ph.hasAttribute("idx")?ph.getAttribute("idx").equals(other.getAttribute("idx")):PptxVisualXml.attr(ph,"type","obj").equals(PptxVisualXml.attr(other,"type","obj"))))return candidate;
            }String type=PptxVisualXml.attr(ph,"type","obj");for(Element candidate:OoxmlXml.children(OoxmlXml.path(root,"cSld","spTree"),"sp")){Element other=OoxmlXml.descendant(candidate,"ph");if(other!=null&&type.equals(PptxVisualXml.attr(other,"type","obj")))return candidate;}return null;
        }
    }
    static Context context(OpcPackage pkg,String slide)throws IOException {
        String layoutPath=related(pkg,slide,"slideLayout"),masterPath=layoutPath==null?null:related(pkg,layoutPath,"slideMaster"),themePath=masterPath==null?null:related(pkg,masterPath,"theme");
        Element layout=root(pkg,layoutPath),master=root(pkg,masterPath),theme=root(pkg,themePath);Map<String,Color> colors=new HashMap<>();
        if(theme!=null){Element scheme=OoxmlXml.descendant(theme,"clrScheme");for(Element entry:OoxmlXml.children(scheme)){
            Element color=OoxmlXml.children(entry).stream().findFirst().orElse(null);if(color!=null)try{colors.put(entry.getLocalName(),new Color(Integer.parseInt(PptxVisualXml.attr(color,"lastClr",color.getAttribute("val")),16)));}catch(NumberFormatException ignored){}
        }}
        colors.putIfAbsent("dk1",Color.BLACK);colors.putIfAbsent("lt1",Color.WHITE);
        Element map=OoxmlXml.child(master,"clrMap");for(String key:List.of("bg1","bg2","tx1","tx2","accent1","accent2","accent3","accent4","accent5","accent6","hlink","folHlink"))colors.put(key,colors.getOrDefault(PptxVisualXml.attr(map,key,key.equals("bg1")?"lt1":key.equals("tx1")?"dk1":key),Color.BLACK));
        Color background=PptxVisualXml.fill(OoxmlXml.path(master,"cSld","bg","bgPr"),Color.WHITE,colors);background=PptxVisualXml.fill(OoxmlXml.path(layout,"cSld","bg","bgPr"),background,colors);
        String major=PptxVisualXml.attr(OoxmlXml.path(OoxmlXml.descendant(theme,"fontScheme"),"majorFont","latin"),"typeface","Arial"),minor=PptxVisualXml.attr(OoxmlXml.path(OoxmlXml.descendant(theme,"fontScheme"),"minorFont","latin"),"typeface","Arial");
        Element presentation=root(pkg,"ppt/presentation.xml");Context context=new Context(Map.copyOf(colors),background,layout,master,OoxmlXml.child(presentation,"defaultTextStyle"),major,minor);context.resolveFonts(layout);context.resolveFonts(master);return context;
    }
    private static Element root(OpcPackage pkg,String path)throws IOException{return path!=null&&pkg.contains(path)?OoxmlXml.parse(pkg.part(path)).getDocumentElement():null;}
    private static String related(OpcPackage pkg,String source,String suffix)throws IOException{
        String rel=source.substring(0,source.lastIndexOf('/')+1)+"_rels/"+source.substring(source.lastIndexOf('/')+1)+".rels";if(!pkg.contains(rel))return null;
        for(Element e:OoxmlXml.children(OoxmlXml.parse(pkg.part(rel)).getDocumentElement()))if(e.getAttribute("Type").endsWith("/"+suffix))return ExternalPptxSupport.resolve(source,e.getAttribute("Target"));return null;
    }
}
