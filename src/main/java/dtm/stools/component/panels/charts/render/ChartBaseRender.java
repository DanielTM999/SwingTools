package dtm.stools.component.panels.charts.render;

import dtm.stools.component.panels.charts.animation.ChartAnimator;
import dtm.stools.component.panels.charts.animation.ChartEasing;
import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.data.ChartSeries;
import dtm.stools.component.panels.charts.render.gl.Gl2D;
import dtm.stools.component.panels.charts.render.gl.GlTextRenderer;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.charts.style.ChartPalette;
import dtm.stools.component.panels.charts.style.ChartTheme;
import dtm.stools.component.panels.charts.style.LegendPosition;
import dtm.stools.component.panels.graphics.gl.GL;
import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;
import dtm.stools.component.panels.graphics.gl.GraphicsGlRender;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class ChartBaseRender implements GraphicsGlRender {

    public static class LegendEntry {
        private final String label;
        private final ChartColor color;

        public LegendEntry(String label, ChartColor color) {
            this.label = label == null ? "" : label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public ChartColor getColor() { return color; }
    }

    public static class TooltipLine {
        private final String text;
        private final ChartColor marker;

        public TooltipLine(String text) {
            this(text, null);
        }

        public TooltipLine(String text, ChartColor marker) {
            this.text = text == null ? "" : text;
            this.marker = marker;
        }

        public String getText() { return text; }
        public ChartColor getMarker() { return marker; }
    }

    private final Gl2D g2d = new Gl2D();
    private final GlTextRenderer textRenderer = new GlTextRenderer();
    private final ChartAnimator animator = new ChartAnimator();

    private volatile ChartDataSource dataSource = new ChartDataSource();
    private volatile ChartTheme theme = ChartTheme.dark();
    private volatile ChartPalette palette = ChartPalette.modern();
    private volatile String title;
    private volatile String subtitle;
    private volatile float paddingTop = 14f;
    private volatile float paddingLeft = 16f;
    private volatile float paddingRight = 16f;
    private volatile float paddingBottom = 12f;
    private volatile boolean legendVisible = true;
    private volatile LegendPosition legendPosition = LegendPosition.BOTTOM;
    private volatile boolean tooltipEnabled = true;
    private volatile boolean animationEnabled = true;
    private volatile boolean animateOnDataChange = true;

    private long lastSeenDataVersion = Long.MIN_VALUE;
    private boolean initializedOnce;

    private float tooltipX;
    private float tooltipY;
    private List<TooltipLine> tooltipLines;

    public ChartDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(ChartDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.lastSeenDataVersion = Long.MIN_VALUE;
    }

    public ChartTheme getTheme() {
        return theme;
    }

    public void setTheme(ChartTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme cannot be null");
    }

    public ChartPalette getPalette() {
        return palette;
    }

    public void setPalette(ChartPalette palette) {
        this.palette = Objects.requireNonNull(palette, "palette cannot be null");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setPadding(float padding) {
        setPadding(padding, padding, padding, padding);
    }

    public void setPadding(float top, float left, float bottom, float right) {
        this.paddingTop = top;
        this.paddingLeft = left;
        this.paddingBottom = bottom;
        this.paddingRight = right;
    }

    public float getPaddingTop() { return paddingTop; }
    public float getPaddingLeft() { return paddingLeft; }
    public float getPaddingRight() { return paddingRight; }
    public float getPaddingBottom() { return paddingBottom; }

    public boolean isLegendVisible() {
        return legendVisible;
    }

    public void setLegendVisible(boolean legendVisible) {
        this.legendVisible = legendVisible;
    }

    public LegendPosition getLegendPosition() {
        return legendPosition;
    }

    public void setLegendPosition(LegendPosition legendPosition) {
        this.legendPosition = Objects.requireNonNull(legendPosition, "legendPosition cannot be null");
    }

    public boolean isTooltipEnabled() {
        return tooltipEnabled;
    }

    public void setTooltipEnabled(boolean tooltipEnabled) {
        this.tooltipEnabled = tooltipEnabled;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    public boolean isAnimateOnDataChange() {
        return animateOnDataChange;
    }

    public void setAnimateOnDataChange(boolean animateOnDataChange) {
        this.animateOnDataChange = animateOnDataChange;
    }

    public float getAnimationDuration() {
        return animator.getDurationSeconds();
    }

    public void setAnimationDuration(float seconds) {
        animator.setDurationSeconds(seconds);
    }

    public ChartEasing getEasing() {
        return animator.getEasing();
    }

    public void setEasing(ChartEasing easing) {
        animator.setEasing(easing);
    }

    public void replayAnimation() {
        animator.restart();
    }

    @Override
    public void initialize(GraphicsGlContext context) {
        g2d.init();
        textRenderer.init();
        if (!initializedOnce || animationEnabled) {
            animator.restart();
        }
        initializedOnce = true;
        onInitialize(context);
    }

    @Override
    public void resize(GraphicsGlContext context, int width, int height) {
        GL.glViewport(0, 0, Math.max(1, width), Math.max(1, height));
        onResize(context, width, height);
    }

    @Override
    public void render(GraphicsGlContext context) {
        ChartDataSource ds = dataSource;
        long version = ds.getVersion();
        if (version != lastSeenDataVersion) {
            boolean firstData = lastSeenDataVersion == Long.MIN_VALUE;
            lastSeenDataVersion = version;
            onDataChanged(context);
            if (animateOnDataChange || firstData) {
                animator.restart();
            }
        }

        float progress = animationEnabled ? animator.update(context.getDeltaTime()) : 1f;
        int width = Math.max(1, context.getWidth());
        int height = Math.max(1, context.getHeight());

        ChartColor background = theme.getBackground();
        GL.glClearColor(background.r(), background.g(), background.b(), background.a());
        GL.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        g2d.begin(width, height);
        textRenderer.begin(width, height);
        tooltipLines = null;

        ChartRect area = new ChartRect(paddingLeft, paddingTop,
                width - paddingLeft - paddingRight,
                height - paddingTop - paddingBottom);
        area = drawTitle(context, area);
        area = drawLegend(context, area);

        if (area.width > 12f && area.height > 12f) {
            renderChart(context, area, progress);
        }

        drawTooltip(context);
        g2d.end();
        onAfterRender(context);
    }

    @Override
    public void dispose(GraphicsGlContext context) {
        onDispose(context);
        textRenderer.dispose();
        g2d.dispose();
    }

    protected abstract void renderChart(GraphicsGlContext context, ChartRect area, float progress);

    protected void onInitialize(GraphicsGlContext context) {}

    protected void onResize(GraphicsGlContext context, int width, int height) {}

    protected void onDataChanged(GraphicsGlContext context) {}

    protected void onAfterRender(GraphicsGlContext context) {}

    protected void onDispose(GraphicsGlContext context) {}

    protected final Gl2D graphics() {
        return g2d;
    }

    protected final GlTextRenderer textRenderer() {
        return textRenderer;
    }

    protected final void drawText(String text, Font font, float x, float y, ChartColor color,
                                  GlTextRenderer.HAlign hAlign, GlTextRenderer.VAlign vAlign) {
        g2d.flush();
        textRenderer.drawText(text, font, x, y, color, hAlign, vAlign);
    }

    protected final ChartColor seriesColor(int index, ChartSeries series) {
        ChartColor fixed = series != null ? series.getColor() : null;
        return fixed != null ? fixed : palette.colorAt(index);
    }

    protected final void requestTooltip(float x, float y, List<TooltipLine> lines) {
        if (!tooltipEnabled || lines == null || lines.isEmpty()) return;
        this.tooltipX = x;
        this.tooltipY = y;
        this.tooltipLines = lines;
    }

    protected List<LegendEntry> legendEntries() {
        List<ChartSeries> seriesList = dataSource.getSeriesList();
        List<LegendEntry> entries = new ArrayList<>(seriesList.size());
        for (int i = 0; i < seriesList.size(); i++) {
            entries.add(new LegendEntry(seriesList.get(i).getName(), seriesColor(i, seriesList.get(i))));
        }
        return entries;
    }

    protected static double niceNumber(double range, boolean round) {
        if (range <= 0) return 1;
        double exponent = Math.floor(Math.log10(range));
        double fraction = range / Math.pow(10, exponent);
        double niceFraction;
        if (round) {
            if (fraction < 1.5) niceFraction = 1;
            else if (fraction < 3) niceFraction = 2;
            else if (fraction < 7) niceFraction = 5;
            else niceFraction = 10;
        } else {
            if (fraction <= 1) niceFraction = 1;
            else if (fraction <= 2) niceFraction = 2;
            else if (fraction <= 5) niceFraction = 5;
            else niceFraction = 10;
        }
        return niceFraction * Math.pow(10, exponent);
    }

    protected static String formatValue(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000d) return trimNumber(value / 1_000_000_000d) + "B";
        if (abs >= 1_000_000d) return trimNumber(value / 1_000_000d) + "M";
        if (abs >= 10_000d) return trimNumber(value / 1_000d) + "k";
        return trimNumber(value);
    }

    private static String trimNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.US, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    protected ChartRect drawTitle(GraphicsGlContext context, ChartRect area) {
        float consumed = 0f;
        String titleText = title;
        String subtitleText = subtitle;
        if (titleText != null && !titleText.isEmpty()) {
            drawText(titleText, theme.getTitleFont(), area.x, area.y + consumed,
                    theme.getTitleColor(), GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.TOP);
            consumed += textRenderer.lineHeight(theme.getTitleFont()) + 2f;
        }
        if (subtitleText != null && !subtitleText.isEmpty()) {
            drawText(subtitleText, theme.getSubtitleFont(), area.x, area.y + consumed,
                    theme.getSubtitleColor(), GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.TOP);
            consumed += textRenderer.lineHeight(theme.getSubtitleFont()) + 2f;
        }
        if (consumed > 0f) {
            consumed += 8f;
        }
        return area.inset(0f, consumed, 0f, 0f);
    }

    protected ChartRect drawLegend(GraphicsGlContext context, ChartRect area) {
        if (!legendVisible) return area;
        List<LegendEntry> entries = legendEntries();
        if (entries.isEmpty()) return area;

        Font font = theme.getLegendFont();
        float lineHeight = textRenderer.lineHeight(font);
        float markerRadius = 4f;
        float markerGap = 7f;
        float itemGap = 18f;
        ChartColor textColor = theme.getLegendTextColor();

        LegendPosition position = legendPosition;
        if (position == LegendPosition.TOP || position == LegendPosition.BOTTOM) {
            float rowHeight = lineHeight + 10f;
            float totalWidth = 0f;
            for (LegendEntry entry : entries) {
                totalWidth += markerRadius * 2 + markerGap + textRenderer.measureWidth(entry.getLabel(), font) + itemGap;
            }
            totalWidth -= itemGap;
            float startX = area.x + Math.max(0f, (area.width - totalWidth) * 0.5f);
            float y = position == LegendPosition.TOP ? area.y : area.bottom() - lineHeight;
            float cursor = startX;
            for (LegendEntry entry : entries) {
                if (entry.getColor() != null) {
                    g2d.fillCircle(cursor + markerRadius, y + lineHeight * 0.5f, markerRadius, entry.getColor());
                }
                drawText(entry.getLabel(), font, cursor + markerRadius * 2 + markerGap, y + lineHeight * 0.5f,
                        textColor, GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.MIDDLE);
                cursor += markerRadius * 2 + markerGap + textRenderer.measureWidth(entry.getLabel(), font) + itemGap;
            }
            return position == LegendPosition.TOP
                    ? area.inset(0f, rowHeight, 0f, 0f)
                    : area.inset(0f, 0f, 0f, rowHeight);
        }

        float maxTextWidth = 0f;
        for (LegendEntry entry : entries) {
            maxTextWidth = Math.max(maxTextWidth, textRenderer.measureWidth(entry.getLabel(), font));
        }
        float columnWidth = Math.min(area.width * 0.4f, markerRadius * 2 + markerGap + maxTextWidth + 12f);
        float rowHeight = lineHeight + 6f;
        float totalHeight = rowHeight * entries.size();
        float startY = area.y + Math.max(0f, (area.height - totalHeight) * 0.5f);
        float columnX = position == LegendPosition.LEFT ? area.x : area.right() - columnWidth;

        float cursorY = startY;
        for (LegendEntry entry : entries) {
            if (entry.getColor() != null) {
                g2d.fillCircle(columnX + markerRadius, cursorY + lineHeight * 0.5f, markerRadius, entry.getColor());
            }
            drawText(entry.getLabel(), font, columnX + markerRadius * 2 + markerGap, cursorY + lineHeight * 0.5f,
                    textColor, GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.MIDDLE);
            cursorY += rowHeight;
        }
        return position == LegendPosition.LEFT
                ? area.inset(columnWidth + 10f, 0f, 0f, 0f)
                : area.inset(0f, 0f, columnWidth + 10f, 0f);
    }

    protected void drawTooltip(GraphicsGlContext context) {
        List<TooltipLine> lines = tooltipLines;
        if (lines == null || lines.isEmpty()) return;

        Font font = theme.getTooltipFont();
        float lineHeight = textRenderer.lineHeight(font) + 3f;
        float paddingX = 11f;
        float paddingY = 8f;
        float markerSpace = 0f;
        float maxTextWidth = 0f;
        for (TooltipLine line : lines) {
            maxTextWidth = Math.max(maxTextWidth, textRenderer.measureWidth(line.getText(), font));
            if (line.getMarker() != null) markerSpace = 13f;
        }
        float boxWidth = maxTextWidth + markerSpace + paddingX * 2;
        float boxHeight = lines.size() * lineHeight - 3f + paddingY * 2;

        int width = Math.max(1, context.getWidth());
        int height = Math.max(1, context.getHeight());
        float x = tooltipX + 14f;
        float y = tooltipY + 14f;
        if (x + boxWidth > width - 4f) x = tooltipX - boxWidth - 14f;
        if (y + boxHeight > height - 4f) y = tooltipY - boxHeight - 14f;
        x = Math.max(4f, Math.min(x, width - boxWidth - 4f));
        y = Math.max(4f, Math.min(y, height - boxHeight - 4f));

        float radius = 8f;
        ChartColor border = theme.getTooltipBorderColor();
        if (border != null && border.a() > 0f) {
            g2d.fillRoundRect(x - 1f, y - 1f, boxWidth + 2f, boxHeight + 2f, radius + 1f, border);
        }
        g2d.fillRoundRect(x, y, boxWidth, boxHeight, radius, theme.getTooltipBackground());

        float textX = x + paddingX + markerSpace;
        float cursorY = y + paddingY;
        for (TooltipLine line : lines) {
            if (line.getMarker() != null) {
                g2d.fillCircle(x + paddingX + 4f, cursorY + (lineHeight - 3f) * 0.5f, 4f, line.getMarker());
            }
            drawText(line.getText(), font, textX, cursorY + (lineHeight - 3f) * 0.5f,
                    theme.getTooltipTextColor(), GlTextRenderer.HAlign.LEFT, GlTextRenderer.VAlign.MIDDLE);
            cursorY += lineHeight;
        }
    }

    protected final GraphicsInput input(GraphicsGlContext context) {
        return context.getInput();
    }
}
