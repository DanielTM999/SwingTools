package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record SheetFill(FillPattern pattern, Integer foreground, Integer background, Integer gradientEnd, double gradientAngle) {
    public static final SheetFill NONE = new SheetFill(FillPattern.NONE, null, null, null, 0);

    public SheetFill { Objects.requireNonNull(pattern); }

    public static SheetFill solid(int argb) { return new SheetFill(FillPattern.SOLID, argb, null, null, 0); }
    public static SheetFill pattern(FillPattern pattern, Integer foreground, Integer background) { return new SheetFill(pattern, foreground, background, null, 0); }
    public static SheetFill gradient(int from, int to, double angle) { return new SheetFill(FillPattern.SOLID, from, null, to, angle); }

    public boolean visible() { return pattern != FillPattern.NONE; }
    public boolean isGradient() { return gradientEnd != null; }
    public Integer primaryColor() { return pattern == FillPattern.NONE ? null : foreground != null ? foreground : background; }
}
