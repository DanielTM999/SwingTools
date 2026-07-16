package dtm.stools.component.panels.charts.render;

public class AreaChartRender extends LineChartRender {

    public AreaChartRender() {
        setFillArea(true);
        setAreaOpacity(0.35f);
        setShowPoints(false);
        setSmooth(true);
        setLineWidth(2.2f);
    }
}
