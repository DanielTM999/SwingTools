package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.render.CellPaintContext;

import java.awt.Graphics2D;

public interface SheetCellRendererProvider extends SheetProvider {
    boolean supports(CellPaintContext context);

    boolean paint(Graphics2D g, CellPaintContext context);
}
