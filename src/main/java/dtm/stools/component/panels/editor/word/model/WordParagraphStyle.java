package dtm.stools.component.panels.editor.word.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record WordParagraphStyle(Alignment alignment, float before, float after, float lineSpacing,
                                 float leftIndent, float rightIndent, float firstLineIndent,
                                 boolean pageBreakBefore, int headingLevel, String styleId, WordListRef list,
                                 List<WordTabStop> tabs, boolean keepWithNext, Integer shading, List<String> extras,
                                 LineSpacingRule lineSpacingRule, boolean keepLines, boolean widowControl) {
    public enum LineSpacingRule { AUTO, EXACT, AT_LEAST }
    public enum Alignment { LEFT, CENTER, RIGHT, JUSTIFY }
    public static final WordParagraphStyle DEFAULT = new WordParagraphStyle(Alignment.LEFT,0,8,1.15f,0,0,0,false,0);
    public WordParagraphStyle {
        Objects.requireNonNull(alignment); Objects.requireNonNull(lineSpacingRule);
        if (!Float.isFinite(before) || !Float.isFinite(after) || !Float.isFinite(lineSpacing)
                || !Float.isFinite(leftIndent) || !Float.isFinite(rightIndent) || !Float.isFinite(firstLineIndent)
                || before < 0 || after < 0 || lineSpacing <= 0 || lineSpacing > 14400
                || (lineSpacingRule == LineSpacingRule.AUTO && (lineSpacing < 0.5 || lineSpacing > 10))
                || headingLevel < 0 || headingLevel > 9) throw new IllegalArgumentException("Invalid paragraph style");
        if (styleId != null && styleId.isBlank()) styleId = null;
        tabs = tabs == null ? List.of() : tabs.stream().sorted(Comparator.comparingDouble(WordTabStop::position)).toList();
        if (shading != null) shading &= 0xffffff;
        extras = extras == null ? List.of() : List.copyOf(extras);
    }
    public WordParagraphStyle(Alignment alignment, float before, float after, float lineSpacing,
                              float leftIndent, float rightIndent, float firstLineIndent, boolean pageBreakBefore,
                              int headingLevel, String styleId, WordListRef list, List<WordTabStop> tabs,
                              boolean keepWithNext, Integer shading, List<String> extras) {
        this(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,
                styleId,list,tabs,keepWithNext,shading,extras,LineSpacingRule.AUTO,false,true);
    }
    public WordParagraphStyle(Alignment alignment, float before, float after, float lineSpacing, float leftIndent, float rightIndent,
                              float firstLineIndent, boolean pageBreakBefore, int headingLevel) {
        this(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,null,null,List.of(),false,null,List.of());
    }
    public WordParagraphStyle withAlignment(Alignment value) { return new WordParagraphStyle(value,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withHeadingLevel(int value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,value,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withSpacing(float b, float a, float line) { return new WordParagraphStyle(alignment,b,a,line,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,tabs,keepWithNext,shading,extras,LineSpacingRule.AUTO,keepLines,widowControl); }
    public WordParagraphStyle withIndents(float left, float right, float first) { return new WordParagraphStyle(alignment,before,after,lineSpacing,left,right,first,pageBreakBefore,headingLevel,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withPageBreakBefore(boolean value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,value,headingLevel,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withStyleId(String value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,value,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withList(WordListRef value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,value,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withTabs(List<WordTabStop> value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,value,keepWithNext,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withKeepWithNext(boolean value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,tabs,value,shading,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withShading(Integer value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,tabs,keepWithNext,value,extras,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withExtras(List<String> value) { return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,headingLevel,styleId,list,tabs,keepWithNext,shading,value,lineSpacingRule,keepLines,widowControl); }
    public WordParagraphStyle withLineSpacing(LineSpacingRule rule, float value) {
        return new WordParagraphStyle(alignment,before,after,value,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,
                headingLevel,styleId,list,tabs,keepWithNext,shading,extras,rule,keepLines,widowControl);
    }
    public WordParagraphStyle withKeepLines(boolean value) {
        return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,
                headingLevel,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,value,widowControl);
    }
    public WordParagraphStyle withWidowControl(boolean value) {
        return new WordParagraphStyle(alignment,before,after,lineSpacing,leftIndent,rightIndent,firstLineIndent,pageBreakBefore,
                headingLevel,styleId,list,tabs,keepWithNext,shading,extras,lineSpacingRule,keepLines,value);
    }
    public float lineHeight(float natural, float baseSize, boolean hasObject) {
        return switch (lineSpacingRule) {
            case EXACT -> lineSpacing;
            case AT_LEAST -> Math.max(natural,lineSpacing);
            case AUTO -> hasObject ? natural+(lineSpacing-1)*baseSize*1.2f : natural*lineSpacing;
        };
    }
}
