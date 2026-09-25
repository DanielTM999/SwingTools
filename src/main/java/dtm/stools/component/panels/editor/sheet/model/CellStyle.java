package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.Objects;

@With
@Builder(toBuilder = true)
public record CellStyle(String fontFamily, double fontSize, boolean bold, boolean italic, UnderlineStyle underline, boolean strikethrough,
                        boolean superscript, boolean subscript, Integer fontColor, SheetFill fill, String numberFormat,
                        HorizontalAlignment horizontal, VerticalAlignment vertical, boolean wrap, boolean shrinkToFit, int rotation, int indent,
                        SheetBorder top, SheetBorder left, SheetBorder bottom, SheetBorder right, SheetBorder diagonal, boolean diagonalUp, boolean diagonalDown,
                        boolean locked, boolean hidden) {
    public static final String DEFAULT_FONT = "Calibri";
    public static final CellStyle DEFAULT = new CellStyle(DEFAULT_FONT, 11, false, false, UnderlineStyle.NONE, false, false, false, null, SheetFill.NONE, "General",
            HorizontalAlignment.GENERAL, VerticalAlignment.BOTTOM, false, false, 0, 0, SheetBorder.NONE, SheetBorder.NONE, SheetBorder.NONE, SheetBorder.NONE, SheetBorder.NONE, false, false, true, false);

    public CellStyle {
        fontFamily = Objects.requireNonNullElse(fontFamily, DEFAULT_FONT);
        if (!(fontSize > 0) || fontSize > 409) fontSize = 11;
        underline = Objects.requireNonNullElse(underline, UnderlineStyle.NONE);
        fill = Objects.requireNonNullElse(fill, SheetFill.NONE);
        numberFormat = numberFormat == null || numberFormat.isBlank() ? "General" : numberFormat;
        horizontal = Objects.requireNonNullElse(horizontal, HorizontalAlignment.GENERAL);
        vertical = Objects.requireNonNullElse(vertical, VerticalAlignment.BOTTOM);
        top = Objects.requireNonNullElse(top, SheetBorder.NONE);
        left = Objects.requireNonNullElse(left, SheetBorder.NONE);
        bottom = Objects.requireNonNullElse(bottom, SheetBorder.NONE);
        right = Objects.requireNonNullElse(right, SheetBorder.NONE);
        diagonal = Objects.requireNonNullElse(diagonal, SheetBorder.NONE);
        if (rotation != 255) rotation = Math.max(-90, Math.min(90, rotation));
        indent = Math.max(0, Math.min(250, indent));
    }

    public boolean hasBorders() { return top.visible() || left.visible() || bottom.visible() || right.visible() || diagonal.visible(); }
    public CellStyle withAllBorders(SheetBorder border) { return withTop(border).withLeft(border).withBottom(border).withRight(border); }
    public boolean isVertical() { return rotation == 255; }
}
