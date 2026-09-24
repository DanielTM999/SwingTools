package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record WordTableCell(UUID id, List<WordBlock> blocks, int gridSpan, Merge verticalMerge, Integer fill,
                            VerticalAlign verticalAlign, WordBorder border, List<String> extras) {
    public enum Merge { NONE, RESTART, CONTINUE }
    public enum VerticalAlign { TOP, CENTER, BOTTOM }
    public WordTableCell {
        Objects.requireNonNull(id); Objects.requireNonNull(verticalMerge); Objects.requireNonNull(verticalAlign);
        List<WordBlock> copy = new ArrayList<>(Objects.requireNonNull(blocks));
        copy.forEach(Objects::requireNonNull);
        if (copy.isEmpty() || !(copy.getLast() instanceof WordParagraph)) copy.add(WordParagraph.of(""));
        blocks = List.copyOf(copy);
        if (gridSpan < 1 || gridSpan > 63) throw new IllegalArgumentException("Invalid grid span");
        if (fill != null) fill &= 0xffffff;
        extras = extras == null ? List.of() : List.copyOf(extras);
    }
    public static WordTableCell of(String text) { return of(List.of(WordParagraph.of(text))); }
    public static WordTableCell of(List<? extends WordBlock> blocks) {
        return new WordTableCell(UUID.randomUUID(),new ArrayList<WordBlock>(blocks),1,Merge.NONE,null,VerticalAlign.TOP,null,List.of());
    }
    public WordTableCell withBlocks(List<? extends WordBlock> value) { return new WordTableCell(id,new ArrayList<WordBlock>(value),gridSpan,verticalMerge,fill,verticalAlign,border,extras); }
    public WordTableCell withGridSpan(int value) { return new WordTableCell(id,blocks,value,verticalMerge,fill,verticalAlign,border,extras); }
    public WordTableCell withVerticalMerge(Merge value) { return new WordTableCell(id,blocks,gridSpan,value,fill,verticalAlign,border,extras); }
    public WordTableCell withFill(Integer value) { return new WordTableCell(id,blocks,gridSpan,verticalMerge,value,verticalAlign,border,extras); }
    public WordTableCell withVerticalAlign(VerticalAlign value) { return new WordTableCell(id,blocks,gridSpan,verticalMerge,fill,value,border,extras); }
    public WordTableCell withBorder(WordBorder value) { return new WordTableCell(id,blocks,gridSpan,verticalMerge,fill,verticalAlign,value,extras); }
    public WordTableCell withId(UUID value) { return new WordTableCell(value,blocks,gridSpan,verticalMerge,fill,verticalAlign,border,extras); }
    public String plainText() {
        StringBuilder b = new StringBuilder();
        for (WordBlock block : blocks) { if (!b.isEmpty()) b.append('\n'); b.append(block.plainText()); }
        return b.toString();
    }
}
