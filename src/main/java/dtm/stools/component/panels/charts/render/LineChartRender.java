package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.data.ChartDataPoint;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;

import java.util.ArrayList;
import java.util.List;

public class LineChartRender extends CartesianChartRender {

    private volatile float lineWidth = 2.6f;
    private volatile boolean smooth = true;
    private volatile int smoothSubdivisions = 14;
    private volatile boolean showPoints = true;
    private volatile float pointRadius = 3.6f;
    private volatile boolean fillArea = true;
    private volatile float areaOpacity = 0.16f;
    private volatile boolean crosshairVisible = true;

    public float getLineWidth() { return lineWidth; }
    public void setLineWidth(float lineWidth) { this.lineWidth = Math.max(0.5f, lineWidth); }

    public boolean isSmooth() { return smooth; }
    public void setSmooth(boolean smooth) { this.smooth = smooth; }

    public int getSmoothSubdivisions() { return smoothSubdivisions; }
    public void setSmoothSubdivisions(int smoothSubdivisions) {
        this.smoothSubdivisions = Math.max(2, Math.min(48, smoothSubdivisions));
    }

    public boolean isShowPoints() { return showPoints; }
    public void setShowPoints(boolean showPoints) { this.showPoints = showPoints; }

    public float getPointRadius() { return pointRadius; }
    public void setPointRadius(float pointRadius) { this.pointRadius = Math.max(1f, pointRadius); }

    public boolean isFillArea() { return fillArea; }
    public void setFillArea(boolean fillArea) { this.fillArea = fillArea; }

    public float getAreaOpacity() { return areaOpacity; }
    public void setAreaOpacity(float areaOpacity) { this.areaOpacity = Math.max(0f, Math.min(1f, areaOpacity)); }

    public boolean isCrosshairVisible() { return crosshairVisible; }
    public void setCrosshairVisible(boolean crosshairVisible) { this.crosshairVisible = crosshairVisible; }

    @Override
    protected float categoryLabelX(int index, int count) {
        return pointX(index, count);
    }

    @Override
    protected void renderPlot(GraphicsGlContext context, ChartRect plot, float progress) {
        List<ChartSeries> seriesList = getDataSource().getSeriesList();
        int categoryCount = getDataSource().maxPointCount();
        if (categoryCount == 0) return;

        float revealX = plot.x + plot.width * progress;
        int hoverIndex = hoverIndex(context, plot, categoryCount, revealX);

        for (int s = 0; s < seriesList.size(); s++) {
            ChartSeries series = seriesList.get(s);
            List<ChartDataPoint> points = series.getPoints();
            int n = points.size();
            if (n == 0) continue;

            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = pointX(i, categoryCount);
                ys[i] = valueToY(points.get(i).getValue());
            }

            float[][] curve = smooth && n > 2 ? catmullRom(xs, ys, smoothSubdivisions) : new float[][]{xs, ys};
            float[][] visible = clipByX(curve[0], curve[1], revealX);
            if (visible == null) continue;

            ChartColor color = seriesColor(s, series);
            if (fillArea && areaOpacity > 0f) {
                fillAreaUnder(visible[0], visible[1], plot.bottom(), color);
            }
            graphics().strokePolyline(visible[0], visible[1], visible[0].length, lineWidth, color);

            if (showPoints) {
                ChartColor holeColor = getTheme().getBackground();
                for (int i = 0; i < n; i++) {
                    if (xs[i] > revealX + 0.5f) break;
                    boolean hovered = i == hoverIndex;
                    float radius = hovered ? pointRadius + 1.6f : pointRadius;
                    if (hovered) {
                        graphics().fillCircle(xs[i], ys[i], radius * 2.4f, color.withAlpha(0.22f));
                    }
                    graphics().fillCircle(xs[i], ys[i], radius, color);
                    graphics().fillCircle(xs[i], ys[i], Math.max(1f, radius - 1.9f), holeColor);
                }
            }
        }

        drawHover(context, plot, seriesList, categoryCount, hoverIndex);
    }

    private int hoverIndex(GraphicsGlContext context, ChartRect plot, int categoryCount, float revealX) {
        if (!isTooltipEnabled() && !crosshairVisible) return -1;
        GraphicsInput input = input(context);
        if (!input.isMouseInside()) return -1;
        float mouseX = input.getMouseX();
        float mouseY = input.getMouseY();
        if (!plot.contains(mouseX, mouseY) || mouseX > revealX) return -1;

        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < categoryCount; i++) {
            float distance = Math.abs(pointX(i, categoryCount) - mouseX);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    protected void drawHover(GraphicsGlContext context, ChartRect plot,
                             List<ChartSeries> seriesList, int categoryCount, int hoverIndex) {
        if (hoverIndex < 0) return;
        float x = pointX(hoverIndex, categoryCount);

        if (crosshairVisible) {
            graphics().strokeLine(x, plot.y, x, plot.bottom(), 1f, getTheme().getCrosshairColor());
        }

        List<TooltipLine> lines = new ArrayList<>();
        List<String> categories = getCategories();
        if (hoverIndex < categories.size()) {
            lines.add(new TooltipLine(categories.get(hoverIndex)));
        }
        for (int s = 0; s < seriesList.size(); s++) {
            ChartSeries series = seriesList.get(s);
            if (hoverIndex >= series.size()) continue;
            double value = series.get(hoverIndex).getValue();
            lines.add(new TooltipLine(series.getName() + ": " + getValueFormatter().apply(value),
                    seriesColor(s, series)));
        }
        GraphicsInput input = input(context);
        requestTooltip(input.getMouseX(), input.getMouseY(), lines);
    }

    private void fillAreaUnder(float[] xs, float[] ys, float baseY, ChartColor color) {
        ChartColor top = color.withAlpha(color.a() * areaOpacity);
        ChartColor bottom = color.withAlpha(0f);
        float highestY = Float.MAX_VALUE;
        for (float y : ys) highestY = Math.min(highestY, y);
        final float minY = highestY;
        final float span = Math.max(1f, baseY - minY);

        for (int i = 0; i < xs.length - 1; i++) {
            ChartColor colorA = top.lerp(bottom, Math.max(0f, Math.min(1f, (ys[i] - minY) / span)));
            ChartColor colorB = top.lerp(bottom, Math.max(0f, Math.min(1f, (ys[i + 1] - minY) / span)));
            graphics().fillQuad(
                    xs[i], ys[i], colorA,
                    xs[i + 1], ys[i + 1], colorB,
                    xs[i + 1], baseY, bottom,
                    xs[i], baseY, bottom);
        }
    }

    private static float[][] clipByX(float[] xs, float[] ys, float limitX) {
        int n = xs.length;
        if (n == 0 || xs[0] > limitX) return null;
        int visible = 0;
        while (visible < n && xs[visible] <= limitX) visible++;
        if (visible >= n) return new float[][]{xs, ys};

        float x1 = xs[visible - 1], y1 = ys[visible - 1];
        float x2 = xs[visible], y2 = ys[visible];
        float t = x2 - x1 < 1e-6f ? 0f : (limitX - x1) / (x2 - x1);
        float[] cx = new float[visible + 1];
        float[] cy = new float[visible + 1];
        System.arraycopy(xs, 0, cx, 0, visible);
        System.arraycopy(ys, 0, cy, 0, visible);
        cx[visible] = limitX;
        cy[visible] = y1 + (y2 - y1) * t;
        return new float[][]{cx, cy};
    }

    private static float[][] catmullRom(float[] xs, float[] ys, int subdivisions) {
        int n = xs.length;
        int outSize = (n - 1) * subdivisions + 1;
        float[] outX = new float[outSize];
        float[] outY = new float[outSize];
        int index = 0;
        for (int i = 0; i < n - 1; i++) {
            float x0 = xs[Math.max(0, i - 1)], y0 = ys[Math.max(0, i - 1)];
            float x1 = xs[i], y1 = ys[i];
            float x2 = xs[i + 1], y2 = ys[i + 1];
            float x3 = xs[Math.min(n - 1, i + 2)], y3 = ys[Math.min(n - 1, i + 2)];
            for (int j = 0; j < subdivisions; j++) {
                float t = (float) j / subdivisions;
                outX[index] = catmull(x0, x1, x2, x3, t);
                outY[index] = catmull(y0, y1, y2, y3, t);
                index++;
            }
        }
        outX[index] = xs[n - 1];
        outY[index] = ys[n - 1];
        return new float[][]{outX, outY};
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}
