package dtm.stools.component.panels.charts;

import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.render.ChartBaseRender;
import dtm.stools.component.panels.charts.style.ChartPalette;
import dtm.stools.component.panels.charts.style.ChartTheme;
import dtm.stools.component.panels.graphics.RenderThreadingMode;
import dtm.stools.component.panels.graphics.gl.GraphicsGlPanel;

import java.awt.BorderLayout;
import java.util.Objects;

public class ChartsPanel<R extends ChartBaseRender> extends BlockingPanel {

    private final GraphicsGlPanel graphicsGlPanel;
    private volatile R render;

    public ChartsPanel(R render) {
        super(new BorderLayout());
        this.render = Objects.requireNonNull(render, "render cannot be null");
        this.graphicsGlPanel = new GraphicsGlPanel(render);
        graphicsGlPanel.setFPS(60);
        add(graphicsGlPanel, BorderLayout.CENTER);
    }

    public R getRender() {
        return render;
    }

    public void setRender(R render) {
        Objects.requireNonNull(render, "render cannot be null");
        this.render = render;
        graphicsGlPanel.setRenderer(render);
    }

    public ChartDataSource getDataSource() {
        return render.getDataSource();
    }

    public void setDataSource(ChartDataSource dataSource) {
        render.setDataSource(dataSource);
    }

    public ChartTheme getChartTheme() {
        return render.getTheme();
    }

    public void setChartTheme(ChartTheme theme) {
        render.setTheme(theme);
    }

    public ChartPalette getChartPalette() {
        return render.getPalette();
    }

    public void setChartPalette(ChartPalette palette) {
        render.setPalette(palette);
    }

    public void replayAnimation() {
        render.replayAnimation();
    }

    public void init() {
        graphicsGlPanel.init();
    }

    public void dispose() {
        graphicsGlPanel.dispose();
    }

    public boolean isChartReady() {
        return graphicsGlPanel.isReady();
    }

    public boolean isChartContextCreated() {
        return graphicsGlPanel.isContextCreated();
    }

    public boolean isRendering() {
        return graphicsGlPanel.isRendering();
    }

    public boolean isDisposed() {
        return graphicsGlPanel.isDisposed();
    }

    public void setRenderMode(RenderThreadingMode renderMode) {
        graphicsGlPanel.setRenderMode(renderMode);
    }

    public RenderThreadingMode getRenderMode() {
        return graphicsGlPanel.getRenderMode();
    }

    public void setFPS(int fps) {
        graphicsGlPanel.setFPS(fps);
    }

    public int getFPS() {
        return graphicsGlPanel.getFPS();
    }

    public int getCurrentFPS() {
        return graphicsGlPanel.getCurrentFPS();
    }

    public void setVsync(boolean vsync) {
        graphicsGlPanel.setVsync(vsync);
    }

    public boolean isVsync() {
        return graphicsGlPanel.isVsync();
    }
}
