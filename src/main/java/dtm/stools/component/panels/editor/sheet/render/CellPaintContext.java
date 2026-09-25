package dtm.stools.component.panels.editor.sheet.render;

import dtm.stools.component.panels.editor.sheet.data.ConditionalResult;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.awt.Rectangle;

public record CellPaintContext(int sheet, CellAddress address, Rectangle bounds, Rectangle textBounds, CellStyle style, CellValue value, String text,
                               Integer textColor, ConditionalResult conditional, double zoom, boolean showFormulas, boolean errorIndicator, boolean noteIndicator,
                               boolean threadIndicator, boolean hyperlink, boolean checkbox, boolean fillCharacter, boolean dark) {}
