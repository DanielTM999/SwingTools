package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

@With
@Builder(toBuilder = true)
public record DifferentialStyle(Integer fontColor, Boolean bold, Boolean italic, Boolean underline, Boolean strikethrough, Integer fillColor,
                                Integer borderColor, String numberFormat) {
    public static final DifferentialStyle EMPTY = new DifferentialStyle(null, null, null, null, null, null, null, null);
    public static final DifferentialStyle LIGHT_RED = fill(0xFFFFC7CE, 0xFF9C0006);
    public static final DifferentialStyle YELLOW = fill(0xFFFFEB9C, 0xFF9C5700);
    public static final DifferentialStyle GREEN = fill(0xFFC6EFCE, 0xFF006100);

    public static DifferentialStyle fill(int fill, int font) { return new DifferentialStyle(font, null, null, null, null, fill, null, null); }

    public CellStyle apply(CellStyle base) {
        CellStyle s = base;
        if (fontColor != null) s = s.withFontColor(fontColor);
        if (bold != null) s = s.withBold(bold);
        if (italic != null) s = s.withItalic(italic);
        if (underline != null) s = s.withUnderline(underline ? UnderlineStyle.SINGLE : UnderlineStyle.NONE);
        if (strikethrough != null) s = s.withStrikethrough(strikethrough);
        if (fillColor != null) s = s.withFill(SheetFill.solid(fillColor));
        if (borderColor != null) s = s.withAllBorders(SheetBorder.of(BorderStyle.THIN, borderColor));
        if (numberFormat != null) s = s.withNumberFormat(numberFormat);
        return s;
    }
}
