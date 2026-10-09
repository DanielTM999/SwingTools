package dtm.stools.component.panels.editor.powerpoint.io;

import dtm.stools.component.panels.editor.powerpoint.model.*;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import org.w3c.dom.Element;
import java.util.*;

/** PresentationML timing tree for the editor's supported visual effects and embedded media. */
final class PptxTimingXml {
    private PptxTimingXml(){}
    static String write(PptSlide slide){
        if(slide.animations().isEmpty()&&slide.objects().stream().noneMatch(o->isMedia(o)&&o.data().length>0))return "";
        Map<String,Integer> shapeIds=new HashMap<>();
        for(int i=0;i<slide.objects().size();i++)shapeIds.put(slide.objects().get(i).id(),i+2);
        StringBuilder b=new StringBuilder("<p:timing><p:tnLst><p:par><p:cTn id=\"1\" dur=\"indefinite\" restart=\"never\" nodeType=\"tmRoot\"><p:childTnLst><p:seq concurrent=\"1\" nextAc=\"seek\"><p:cTn id=\"2\" dur=\"indefinite\" nodeType=\"mainSeq\"><p:childTnLst>");
        int nodeId=3;
        for(PptAnimation animation:slide.animations()){
            Integer shape=shapeIds.get(animation.targetId());if(shape==null)continue;
            int outer=nodeId++,inner=nodeId++;
            String nodeType=switch(animation.start()){case ON_CLICK->"clickEffect";case WITH_PREVIOUS->"withEffect";case AFTER_PREVIOUS->"afterEffect";};
            String presetClass=isEntrance(animation.effect())?"entr":isExit(animation.effect())?"exit":isPath(animation.effect())?"path":"emph";
            b.append("<p:par><p:cTn id=\"").append(outer).append("\" fill=\"hold\" presetID=\"").append(presetId(animation.effect()))
                    .append("\" presetClass=\"").append(presetClass).append("\" presetSubtype=\"0\" nodeType=\"").append(nodeType)
                    .append("\"><p:stCondLst><p:cond delay=\"").append(animation.delayMs()).append("\"/></p:stCondLst><p:childTnLst>");
            String behavior="<p:cBhvr><p:cTn id=\""+inner+"\" dur=\""+Math.max(1,animation.durationMs())+"\""
                    +(animation.repeat()>1?" repeatCount=\""+(long)animation.repeat()*1000+"\"":"")
                    +" fill=\"hold\"/><p:tgtEl><p:spTgt spid=\""+shape+"\"/></p:tgtEl>";
            switch(animation.effect()){
                case APPEAR,DISAPPEAR,FADE_IN,FADE_OUT,FLY_IN,FLY_OUT,WIPE_IN,WIPE_OUT,SPLIT_IN,SPLIT_OUT,ZOOM_IN,ZOOM_OUT ->
                        b.append("<p:animEffect transition=\"").append(isExit(animation.effect())?"out":"in").append("\" filter=\"")
                                .append(filter(animation.effect())).append("\">").append(behavior).append("</p:cBhvr></p:animEffect>");
                case PULSE,GROW_SHRINK -> b.append("<p:animScale>").append(behavior).append("</p:cBhvr><p:by x=\"125000\" y=\"125000\"/></p:animScale>");
                case SPIN -> b.append("<p:animRot by=\"21600000\">").append(behavior)
                        .append("<p:attrNameLst><p:attrName>r</p:attrName></p:attrNameLst></p:cBhvr></p:animRot>");
                case COLOR -> b.append("<p:animClr clrSpc=\"rgb\">").append(behavior)
                        .append("<p:attrNameLst><p:attrName>fillcolor</p:attrName></p:attrNameLst></p:cBhvr><p:to><a:srgbClr val=\"FFD349\"/></p:to></p:animClr>");
                case TRANSPARENCY -> b.append("<p:anim from=\"1\" to=\"0.5\" calcmode=\"lin\" valueType=\"num\">").append(behavior)
                        .append("<p:attrNameLst><p:attrName>style.opacity</p:attrName></p:attrNameLst></p:cBhvr></p:anim>");
                case LINE,ARC,POLYLINE -> b.append("<p:animMotion origin=\"layout\" path=\"")
                        .append(switch(animation.effect()){case LINE->"M 0 0 L 0.1 0 E";case ARC->"M 0 0 C 0.03 -0.1 0.07 -0.1 0.1 0 E";default->"M 0 0 L 0.05 0 L 0.05 -0.1 E";})
                        .append("\">").append(behavior).append("</p:cBhvr></p:animMotion>");
            }
            b.append("</p:childTnLst></p:cTn></p:par>");
        }
        for(PptObject object:slide.objects())if(isMedia(object)&&object.data().length>0){
            int outer=nodeId++,inner=nodeId++;int shape=shapeIds.get(object.id());
            b.append("<p:par><p:cTn id=\"").append(outer).append("\" fill=\"hold\" nodeType=\"withEffect\"><p:childTnLst><p:")
                    .append(object.kind()==PptObject.Kind.AUDIO?"audio":"video").append("><p:cMediaNode vol=\"100000\"><p:cTn id=\"")
                    .append(inner).append("\" dur=\"indefinite\"/><p:tgtEl><p:spTgt spid=\"").append(shape)
                    .append("\"/></p:tgtEl></p:cMediaNode></p:").append(object.kind()==PptObject.Kind.AUDIO?"audio":"video")
                    .append("></p:childTnLst></p:cTn></p:par>");
        }
        return b.append("</p:childTnLst></p:cTn></p:seq></p:childTnLst></p:cTn></p:par></p:tnLst></p:timing>").toString();
    }
    static List<PptAnimation> read(Element slide,Map<Integer,String> objectIds){
        Element timing=OoxmlXml.child(slide,"timing");if(timing==null)return List.of();
        List<PptAnimation> result=new ArrayList<>();
        for(Element node:OoxmlXml.descendants(timing,"cTn")){
            String preset=node.getAttribute("presetID"),nodeType=node.getAttribute("nodeType");
            if(preset.isBlank()||nodeType.isBlank())continue;
            Element parent=node.getParentNode() instanceof Element e?e:null;if(parent==null)continue;
            Element target=OoxmlXml.descendant(parent,"spTgt");if(target==null)continue;
            int shape;try{shape=Integer.parseInt(target.getAttribute("spid"));}catch(NumberFormatException error){continue;}
            String objectId=objectIds.get(shape);if(objectId==null)continue;
            PptAnimation.Effect effect=readEffect(parent,preset);if(effect==null)continue;
            PptAnimation.Start start=switch(nodeType){case "withEffect"->PptAnimation.Start.WITH_PREVIOUS;case "afterEffect"->PptAnimation.Start.AFTER_PREVIOUS;default->PptAnimation.Start.ON_CLICK;};
            Element inner=OoxmlXml.descendant(parent,"animEffect");if(inner==null)inner=OoxmlXml.descendant(parent,"animRot");
            if(inner==null)inner=OoxmlXml.descendant(parent,"animScale");if(inner==null)inner=OoxmlXml.descendant(parent,"animMotion");
            if(inner==null)inner=OoxmlXml.descendant(parent,"animClr");if(inner==null)inner=OoxmlXml.descendant(parent,"anim");
            int duration=600,repeat=1;if(inner!=null){Element innerNode=OoxmlXml.descendant(inner,"cTn");
                if(innerNode!=null){try{duration=Integer.parseInt(innerNode.getAttribute("dur"));}catch(NumberFormatException ignored){}
                    try{repeat=Math.max(1,Integer.parseInt(innerNode.getAttribute("repeatCount"))/1000);}catch(NumberFormatException ignored){}
                }}
            int delay=0;Element condition=OoxmlXml.path(node,"stCondLst","cond");
            if(condition!=null)try{delay=Integer.parseInt(condition.getAttribute("delay"));}catch(NumberFormatException ignored){}
            result.add(new PptAnimation(UUID.randomUUID().toString(),objectId,effect,start,Math.max(0,duration),Math.max(0,delay),repeat,"default"));
        }
        return List.copyOf(result);
    }
    private static PptAnimation.Effect readEffect(Element parent,String preset){
        Element effect=OoxmlXml.descendant(parent,"animEffect");if(effect!=null){
            boolean exit="out".equals(effect.getAttribute("transition"));
            String filter=effect.getAttribute("filter");
            return switch(filter){case "fade"->"1".equals(preset)?exit?PptAnimation.Effect.DISAPPEAR:PptAnimation.Effect.APPEAR:exit?PptAnimation.Effect.FADE_OUT:PptAnimation.Effect.FADE_IN;
                case "fly"->exit?PptAnimation.Effect.FLY_OUT:PptAnimation.Effect.FLY_IN;
                case "wipe"->exit?PptAnimation.Effect.WIPE_OUT:PptAnimation.Effect.WIPE_IN;
                case "split"->exit?PptAnimation.Effect.SPLIT_OUT:PptAnimation.Effect.SPLIT_IN;
                case "zoom"->exit?PptAnimation.Effect.ZOOM_OUT:PptAnimation.Effect.ZOOM_IN;
                default->null;};
        }
        if(OoxmlXml.descendant(parent,"animRot")!=null)return PptAnimation.Effect.SPIN;
        if(OoxmlXml.descendant(parent,"animScale")!=null)return PptAnimation.Effect.GROW_SHRINK;
        if(OoxmlXml.descendant(parent,"animClr")!=null)return PptAnimation.Effect.COLOR;
        if(OoxmlXml.descendant(parent,"animMotion")!=null)return PptAnimation.Effect.LINE;
        if(OoxmlXml.descendant(parent,"anim")!=null)return PptAnimation.Effect.TRANSPARENCY;
        return null;
    }
    private static String filter(PptAnimation.Effect effect){return switch(effect){
        case APPEAR,DISAPPEAR->"fade";case FADE_IN,FADE_OUT->"fade";case FLY_IN,FLY_OUT->"fly";
        case WIPE_IN,WIPE_OUT->"wipe";case SPLIT_IN,SPLIT_OUT->"split";case ZOOM_IN,ZOOM_OUT->"zoom";
        default->"fade";};}
    private static int presetId(PptAnimation.Effect effect){return switch(effect){
        case APPEAR,DISAPPEAR->1;case FLY_IN,FLY_OUT->2;case FADE_IN,FADE_OUT->10;
        case WIPE_IN,WIPE_OUT->22;case ZOOM_IN,ZOOM_OUT->23;case SPLIT_IN,SPLIT_OUT->16;
        case SPIN->8;case PULSE,GROW_SHRINK->26;case COLOR->7;case TRANSPARENCY->9;
        case LINE,ARC,POLYLINE->1;};}
    private static boolean isMedia(PptObject object){return object.kind()==PptObject.Kind.AUDIO||object.kind()==PptObject.Kind.VIDEO;}
    private static boolean isEntrance(PptAnimation.Effect effect){return switch(effect){case APPEAR,FADE_IN,FLY_IN,WIPE_IN,SPLIT_IN,ZOOM_IN->true;default->false;};}
    private static boolean isExit(PptAnimation.Effect effect){return switch(effect){case DISAPPEAR,FADE_OUT,FLY_OUT,WIPE_OUT,SPLIT_OUT,ZOOM_OUT->true;default->false;};}
    private static boolean isPath(PptAnimation.Effect effect){return switch(effect){case LINE,ARC,POLYLINE->true;default->false;};}
}
