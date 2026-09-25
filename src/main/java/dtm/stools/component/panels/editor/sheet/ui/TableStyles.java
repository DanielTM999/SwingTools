package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;

import java.util.ArrayList;
import java.util.List;

public final class TableStyles {
    public enum Family { LIGHT, MEDIUM, DARK }

    private TableStyles() {}

    public static List<String> names() {
        List<String> list = new ArrayList<>();
        for (int k = 1; k <= 21; k++) list.add("TableStyleLight" + k);
        for (int k = 1; k <= 28; k++) list.add("TableStyleMedium" + k);
        for (int k = 1; k <= 11; k++) list.add("TableStyleDark" + k);
        return list;
    }

    static Family family(String name) {
        if (name.startsWith("TableStyleDark")) return Family.DARK;
        if (name.startsWith("TableStyleLight")) return Family.LIGHT;
        return Family.MEDIUM;
    }

    static int number(String name) {
        try { return Integer.parseInt(name.replaceAll("\\D", "")); } catch (NumberFormatException e) { return 2; }
    }

    public static int accent(String name, SheetTheme theme) {
        int n = number(name);
        int slot = (n - 1) % 7;
        return slot == 0 ? 0xFF404040 : theme.accent(slot - 1);
    }

    public static CellStyle apply(CellStyle base, boolean plain, SheetTable t, int row, int column, SheetTheme theme) {
        if (t.style() == null || t.style().equalsIgnoreCase("None")) return base;
        Family family = family(t.style());
        int accent = accent(t.style(), theme);
        boolean header = t.headerRow() && row == t.range().firstRow();
        boolean totals = t.totalsRow() && row == t.range().lastRow();
        boolean firstCol = t.firstColumn() && column == t.range().firstColumn(), lastCol = t.lastColumn() && column == t.range().lastColumn();
        int dataIndex = row - t.range().firstRow() - (t.headerRow() ? 1 : 0);
        int colIndex = column - t.range().firstColumn();
        boolean band = !header && !totals && (t.bandedRows() && dataIndex % 2 == 0 || t.bandedColumns() && colIndex % 2 == 0);
        CellStyle s = base;
        boolean noFill = !base.fill().visible();
        if (header) {
            switch (family) {
                case MEDIUM, DARK -> {
                    if (noFill) s = s.withFill(SheetFill.solid(family == Family.DARK ? 0xFF000000 : accent));
                    if (base.fontColor() == null) s = s.withFontColor(0xFFFFFFFF);
                }
                case LIGHT -> { if (!base.bottom().visible()) s = s.withBottom(SheetBorder.of(BorderStyle.THIN, accent)); }
            }
            s = s.withBold(true);
        } else if (totals) {
            if (!base.top().visible()) s = s.withTop(SheetBorder.of(family == Family.LIGHT ? BorderStyle.THIN : BorderStyle.DOUBLE, accent));
            s = s.withBold(true);
            if (family == Family.DARK && noFill) { s = s.withFill(SheetFill.solid(SheetTheme.tint(accent, -0.5))); if (base.fontColor() == null) s = s.withFontColor(0xFFFFFFFF); }
        } else if (noFill) {
            switch (family) {
                case LIGHT -> { if (band) s = s.withFill(SheetFill.solid(SheetTheme.tint(accent, 0.85))); }
                case MEDIUM -> { if (band) s = s.withFill(SheetFill.solid(SheetTheme.tint(accent, 0.8))); if (!base.bottom().visible()) s = s.withBottom(SheetBorder.of(BorderStyle.THIN, SheetTheme.tint(accent, 0.6))); }
                case DARK -> {
                    s = s.withFill(SheetFill.solid(SheetTheme.tint(accent, band ? -0.25 : 0)));
                    if (base.fontColor() == null) s = s.withFontColor(0xFFFFFFFF);
                }
            }
        }
        if ((firstCol || lastCol) && !header) s = s.withBold(true);
        return s;
    }
}
