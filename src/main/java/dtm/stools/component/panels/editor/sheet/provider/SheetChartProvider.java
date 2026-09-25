package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.render.ChartData;

import java.awt.Graphics2D;
import java.awt.Rectangle;

public interface SheetChartProvider extends SheetProvider {
    boolean supports(SheetChart chart);
    void paint(Graphics2D g, SheetChart chart, ChartData data, Rectangle bounds);
}
