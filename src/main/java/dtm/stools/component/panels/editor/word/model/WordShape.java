package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WordShape(String id, WordShapeType shapeType, float x, float y, float width, float height, Integer fill, Integer stroke,
                        float strokeWidth, String text, float fontSize, int textColor, float rotation, boolean arrowEnd,
                        List<WordShape> children, String altText, WordPlacement placement) implements WordInlineObject {
    public static final String TYPE = "shape";
    public WordShape {
        WordInlineObject.requireId(id); Objects.requireNonNull(shapeType);
        WordInlineObject.checkSize(width,height);
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(strokeWidth) || strokeWidth < 0 || strokeWidth > 50
                || !Float.isFinite(fontSize) || fontSize < 1 || fontSize > 400 || !Float.isFinite(rotation)) throw new IllegalArgumentException("Invalid shape");
        if (fill != null) fill &= 0xffffff;
        if (stroke != null) stroke &= 0xffffff;
        textColor &= 0xffffff;
        rotation = ((rotation % 360) + 360) % 360;
        text = text == null ? "" : text;
        children = children == null ? List.of() : List.copyOf(children);
        if (shapeType == WordShapeType.GROUP && children.isEmpty()) throw new IllegalArgumentException("A group needs shapes");
        altText = altText == null ? "" : altText;
        placement = placement == null ? WordPlacement.INLINE : placement;
    }
    public static WordShape of(WordShapeType type, float width, float height) {
        boolean box = type == WordShapeType.TEXT_BOX, line = type.isLinear();
        return new WordShape(WordIds.next(),type,0,0,width,height,line ? null : box ? 0xffffff : 0x4472C4,box ? 0x404040 : line ? 0x404040 : 0x2F528F,
                line ? 1.5f : 1f,box ? "Caixa de texto" : "",11,box ? 0x111111 : 0xffffff,0,type == WordShapeType.CONNECTOR,List.of(),type.displayName(),
                WordPlacement.floating(0,0,WordPlacement.Wrap.SQUARE));
    }
    public static WordShape group(List<WordShape> shapes) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (WordShape s : shapes) {
            float sx = s.placement().floating() ? s.placement().x() : s.x(), sy = s.placement().floating() ? s.placement().y() : s.y();
            minX = Math.min(minX,sx); minY = Math.min(minY,sy); maxX = Math.max(maxX,sx+s.width()); maxY = Math.max(maxY,sy+s.height());
        }
        List<WordShape> children = new ArrayList<>();
        for (WordShape s : shapes) {
            float sx = s.placement().floating() ? s.placement().x() : s.x(), sy = s.placement().floating() ? s.placement().y() : s.y();
            children.add(s.at(sx-minX,sy-minY).withPlacement(WordPlacement.INLINE));
        }
        return new WordShape(WordIds.next(),WordShapeType.GROUP,0,0,maxX-minX,maxY-minY,null,null,0,"",11,0,0,false,children,"Grupo",
                WordPlacement.floating(minX,minY,WordPlacement.Wrap.SQUARE));
    }
    @Override public String type() { return TYPE; }
    @Override public WordShape withId(String value) { return new WordShape(value,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,altText,placement); }
    @Override public WordShape resize(float w, float h) {
        List<WordShape> scaled = new ArrayList<>();
        float sx = width == 0 ? 1 : w/width, sy = height == 0 ? 1 : h/height;
        for (WordShape c : children) scaled.add(c.at(c.x*sx,c.y*sy).resize(c.width*sx,c.height*sy));
        return new WordShape(id,shapeType,x,y,w,h,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,scaled,altText,placement);
    }
    @Override public WordShape withPlacement(WordPlacement value) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,altText,value); }
    @Override public WordShape withAltText(String value) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,value,placement); }
    @Override public WordShape withRotation(float value) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,value,arrowEnd,children,altText,placement); }
    public WordShape at(float nx, float ny) { return new WordShape(id,shapeType,nx,ny,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,altText,placement); }
    public WordShape withColors(Integer newFill, Integer newStroke, float newStrokeWidth) { return new WordShape(id,shapeType,x,y,width,height,newFill,newStroke,newStrokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,altText,placement); }
    public WordShape withText(String value, float size, int color) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,value,size,color,rotation,arrowEnd,children,altText,placement); }
    public WordShape withShapeType(WordShapeType value) { return new WordShape(id,value,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,children,altText,placement); }
    public WordShape withArrowEnd(boolean value) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,value,children,altText,placement); }
    public WordShape withChildren(List<WordShape> value) { return new WordShape(id,shapeType,x,y,width,height,fill,stroke,strokeWidth,text,fontSize,textColor,rotation,arrowEnd,value,altText,placement); }
    public List<WordShape> ungroup() {
        List<WordShape> result = new ArrayList<>();
        for (WordShape c : children) result.add(c.withPlacement(placement.moveTo(placement.x()+c.x,placement.y()+c.y)).at(0,0).withId(WordIds.next()));
        return result;
    }
    @Override public String plainText() {
        StringBuilder b = new StringBuilder(text);
        for (WordShape c : children) { String t = c.plainText(); if (!t.isBlank()) { if (!b.isEmpty()) b.append(' '); b.append(t); } }
        return b.toString();
    }
}
