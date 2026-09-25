package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record SheetBorder(BorderStyle style, Integer color) {
    public static final SheetBorder NONE = new SheetBorder(BorderStyle.NONE, null);
    public static final SheetBorder THIN = new SheetBorder(BorderStyle.THIN, null);

    public SheetBorder { Objects.requireNonNull(style); }

    public static SheetBorder of(BorderStyle style) { return style == BorderStyle.NONE ? NONE : new SheetBorder(style, null); }
    public static SheetBorder of(BorderStyle style, Integer color) { return style == BorderStyle.NONE ? NONE : new SheetBorder(style, color); }
    public boolean visible() { return style != BorderStyle.NONE; }
}
