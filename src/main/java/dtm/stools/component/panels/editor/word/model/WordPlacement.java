package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordPlacement(boolean floating, Anchor horizontalFrom, float x, Anchor verticalFrom, float y, Wrap wrap, int zOrder) {
    public enum Anchor { MARGIN, PAGE, PARAGRAPH }
    public enum Wrap { INLINE, SQUARE, TIGHT, TOP_AND_BOTTOM, BEHIND_TEXT, IN_FRONT_OF_TEXT }
    public static final WordPlacement INLINE = new WordPlacement(false,Anchor.MARGIN,0,Anchor.PARAGRAPH,0,Wrap.INLINE,0);
    public WordPlacement {
        Objects.requireNonNull(horizontalFrom); Objects.requireNonNull(verticalFrom); Objects.requireNonNull(wrap);
        if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 14400 || Math.abs(y) > 14400) throw new IllegalArgumentException("Invalid placement");
        if (horizontalFrom == Anchor.PARAGRAPH) throw new IllegalArgumentException("Horizontal anchor must be margin or page");
        if (!floating) wrap = Wrap.INLINE;
        else if (wrap == Wrap.INLINE) wrap = Wrap.SQUARE;
    }
    public static WordPlacement floating(float x, float y, Wrap wrap) { return new WordPlacement(true,Anchor.MARGIN,x,Anchor.PARAGRAPH,y,wrap,0); }
    public WordPlacement moveTo(float nx, float ny) { return new WordPlacement(floating,horizontalFrom,nx,verticalFrom,ny,wrap,zOrder); }
    public WordPlacement withWrap(Wrap value) {
        if (value == Wrap.INLINE) return INLINE;
        return new WordPlacement(true,horizontalFrom,x,verticalFrom,y,value,zOrder);
    }
    public WordPlacement withZOrder(int value) { return new WordPlacement(floating,horizontalFrom,x,verticalFrom,y,wrap,value); }
    public boolean wrapsText() { return floating && wrap != Wrap.BEHIND_TEXT && wrap != Wrap.IN_FRONT_OF_TEXT; }
}
