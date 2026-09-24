package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WordDiagram(String id, WordDiagramLayout layout, List<WordDiagramNode> nodes, int color, float width, float height,
                          String altText, WordPlacement placement) implements WordInlineObject {
    public static final String TYPE = "diagram";
    public WordDiagram {
        WordInlineObject.requireId(id); Objects.requireNonNull(layout);
        nodes = List.copyOf(nodes);
        if (nodes.isEmpty() || nodes.size() > 200) throw new IllegalArgumentException("A diagram needs 1..200 items");
        WordInlineObject.checkSize(width,height);
        if (width < 48 || height < 36) throw new IllegalArgumentException("Diagram is too small");
        color &= 0xffffff;
        altText = altText == null ? "" : altText;
        placement = placement == null ? WordPlacement.INLINE : placement;
    }
    public static WordDiagram of(WordDiagramLayout layout, List<String> lines) {
        List<WordDiagramNode> nodes = new ArrayList<>();
        for (String line : lines) {
            int level = 0; while (level < line.length() && line.charAt(level) == '\t') level++;
            if (!line.isBlank()) nodes.add(new WordDiagramNode(line.strip(),Math.min(level,8)));
        }
        return new WordDiagram(WordIds.next(),layout,nodes,0x4472C4,400,220,layout.displayName(),WordPlacement.INLINE);
    }
    public static WordDiagram parse(WordDiagramLayout layout, String text) { return of(layout,List.of(text.replace("\r","").split("\n"))); }
    public String outline() {
        StringBuilder b = new StringBuilder();
        for (WordDiagramNode n : nodes) { if (!b.isEmpty()) b.append('\n'); b.append("\t".repeat(n.level())).append(n.text()); }
        return b.toString();
    }
    @Override public String type() { return TYPE; }
    @Override public WordDiagram withId(String value) { return new WordDiagram(value,layout,nodes,color,width,height,altText,placement); }
    @Override public WordDiagram resize(float w, float h) { return new WordDiagram(id,layout,nodes,color,w,h,altText,placement); }
    @Override public WordDiagram withPlacement(WordPlacement value) { return new WordDiagram(id,layout,nodes,color,width,height,altText,value); }
    @Override public WordDiagram withAltText(String value) { return new WordDiagram(id,layout,nodes,color,width,height,value,placement); }
    public WordDiagram withLayout(WordDiagramLayout value) { return new WordDiagram(id,value,nodes,color,width,height,altText,placement); }
    public WordDiagram withNodes(List<WordDiagramNode> value) { return new WordDiagram(id,layout,value,color,width,height,altText,placement); }
    public WordDiagram withColor(int value) { return new WordDiagram(id,layout,nodes,value,width,height,altText,placement); }
    @Override public String plainText() { return String.join(" ",nodes.stream().map(WordDiagramNode::text).toList()); }
}
