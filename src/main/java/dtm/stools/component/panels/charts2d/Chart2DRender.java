package dtm.stools.component.panels.charts2d;

import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.charts.style.ChartTheme;
import dtm.stools.component.panels.charts.style.LegendPosition;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Locale;
import java.util.function.DoubleFunction;

public abstract class Chart2DRender {

    private ChartDataSource dataSource = new ChartDataSource();
    private ChartTheme theme = ChartTheme.dark();
    private String title;
    private float padding = 8f;
    private boolean legendVisible = true;
    private LegendPosition legendPosition = LegendPosition.BOTTOM;
    private DoubleFunction<String> valueFormatter = value -> String.format(Locale.ROOT, "%.0f", value);
    private long revision;
    private int hoverX = -1;
    private int hoverY = -1;

    public ChartDataSource getDataSource() {
        return dataSource;
    }

    public Chart2DRender setDataSource(ChartDataSource dataSource) {
        this.dataSource = dataSource == null ? new ChartDataSource() : dataSource;
        bumpRevision();
        return this;
    }

    public ChartTheme getTheme() {
        return theme;
    }

    public Chart2DRender setTheme(ChartTheme theme) {
        if (theme != null) {
            this.theme = theme;
            bumpRevision();
        }
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Chart2DRender setTitle(String title) {
        this.title = title;
        bumpRevision();
        return this;
    }

    public float getPadding() {
        return padding;
    }

    public Chart2DRender setPadding(float padding) {
        this.padding = padding;
        bumpRevision();
        return this;
    }

    public boolean isLegendVisible() {
        return legendVisible;
    }

    public Chart2DRender setLegendVisible(boolean legendVisible) {
        this.legendVisible = legendVisible;
        bumpRevision();
        return this;
    }

    public LegendPosition getLegendPosition() {
        return legendPosition;
    }

    public Chart2DRender setLegendPosition(LegendPosition legendPosition) {
        if (legendPosition != null) {
            this.legendPosition = legendPosition;
            bumpRevision();
        }
        return this;
    }

    public DoubleFunction<String> getValueFormatter() {
        return valueFormatter;
    }

    public Chart2DRender setValueFormatter(DoubleFunction<String> valueFormatter) {
        if (valueFormatter != null) {
            this.valueFormatter = valueFormatter;
            bumpRevision();
        }
        return this;
    }

    public long getRevision() {
        return revision + (dataSource != null ? dataSource.getVersion() : 0L);
    }

    protected void bumpRevision() {
        revision++;
    }

    public boolean isHovering() {
        return hoverX >= 0 && hoverY >= 0;
    }

    public int getHoverX() {
        return hoverX;
    }

    public int getHoverY() {
        return hoverY;
    }

    public void setHoverPoint(int x, int y) {
        if (hoverX != x || hoverY != y) {
            hoverX = x;
            hoverY = y;
            bumpRevision();
        }
    }

    public void clearHover() {
        setHoverPoint(-1, -1);
    }

    public abstract void paint(Graphics2D g, int width, int height);

    protected String formatValue(double value) {
        return valueFormatter.apply(value);
    }

    protected void paintBackground(Graphics2D g, int width, int height) {
        Color background = toAwt(theme.getBackground());
        if (background != null) {
            g.setColor(background);
            g.fillRect(0, 0, width, height);
        }
    }

    protected int paintTitle(Graphics2D g, int top) {
        if (title == null || title.isBlank()) {
            return top;
        }
        Color color = toAwt(theme.getTitleColor(), Color.GRAY);
        g.setColor(color);
        Font font = theme.getTitleFont();
        if (font != null) {
            g.setFont(font);
        }
        int ascent = g.getFontMetrics().getAscent();
        g.drawString(title, Math.round(padding), top + ascent);
        return top + g.getFontMetrics().getHeight() + 4;
    }

    protected static Color toAwt(ChartColor color) {
        return color == null ? null : color.toAwt();
    }

    protected static Color toAwt(ChartColor color, Color fallback) {
        return color == null ? fallback : color.toAwt();
    }

    protected static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
