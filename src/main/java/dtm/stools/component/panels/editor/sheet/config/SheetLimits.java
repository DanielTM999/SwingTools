package dtm.stools.component.panels.editor.sheet.config;

import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;

public record SheetLimits(int maxRows, int maxColumns, SheetPackage.Limits packageLimits, long maxFileBytes) {
    public static final SheetLimits EXCEL = new SheetLimits(CellAddress.MAX_ROWS, CellAddress.MAX_COLUMNS, SheetPackage.Limits.DEFAULT, 512L * 1024 * 1024);

    public SheetLimits {
        if (maxRows < 1 || maxRows > CellAddress.MAX_ROWS || maxColumns < 1 || maxColumns > CellAddress.MAX_COLUMNS) throw new IllegalArgumentException("Invalid sheet limits");
    }
}
