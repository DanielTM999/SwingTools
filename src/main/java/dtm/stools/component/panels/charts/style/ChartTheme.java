package dtm.stools.component.panels.charts.style;

import java.awt.Font;

public class ChartTheme {

    private ChartColor background;
    private ChartColor plotBackground;
    private ChartColor gridColor;
    private ChartColor axisTextColor;
    private ChartColor titleColor;
    private ChartColor subtitleColor;
    private ChartColor legendTextColor;
    private ChartColor crosshairColor;
    private ChartColor tooltipBackground;
    private ChartColor tooltipBorderColor;
    private ChartColor tooltipTextColor;

    private Font titleFont;
    private Font subtitleFont;
    private Font labelFont;
    private Font legendFont;
    private Font tooltipFont;

    public ChartTheme() {
        Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        this.titleFont = base.deriveFont(Font.BOLD, 17f);
        this.subtitleFont = base.deriveFont(Font.PLAIN, 12f);
        this.labelFont = base.deriveFont(Font.PLAIN, 11f);
        this.legendFont = base.deriveFont(Font.PLAIN, 12f);
        this.tooltipFont = base.deriveFont(Font.PLAIN, 12f);
    }

    public static ChartTheme light() {
        ChartTheme theme = new ChartTheme();
        theme.background = ChartColor.hex("#FFFFFF");
        theme.plotBackground = null;
        theme.gridColor = ChartColor.hex("#E2E8F0");
        theme.axisTextColor = ChartColor.hex("#64748B");
        theme.titleColor = ChartColor.hex("#0F172A");
        theme.subtitleColor = ChartColor.hex("#64748B");
        theme.legendTextColor = ChartColor.hex("#475569");
        theme.crosshairColor = ChartColor.hex("#94A3B8").withAlpha(0.6f);
        theme.tooltipBackground = ChartColor.hex("#0F172A").withAlpha(0.94f);
        theme.tooltipBorderColor = ChartColor.hex("#334155").withAlpha(0.6f);
        theme.tooltipTextColor = ChartColor.hex("#F1F5F9");
        return theme;
    }

    public static ChartTheme dark() {
        ChartTheme theme = new ChartTheme();
        theme.background = ChartColor.hex("#12141A");
        theme.plotBackground = null;
        theme.gridColor = ChartColor.hex("#262B36");
        theme.axisTextColor = ChartColor.hex("#8B93A3");
        theme.titleColor = ChartColor.hex("#F1F5F9");
        theme.subtitleColor = ChartColor.hex("#8B93A3");
        theme.legendTextColor = ChartColor.hex("#B6BDCB");
        theme.crosshairColor = ChartColor.hex("#4B5563").withAlpha(0.8f);
        theme.tooltipBackground = ChartColor.hex("#1D222D").withAlpha(0.96f);
        theme.tooltipBorderColor = ChartColor.hex("#3A4150").withAlpha(0.9f);
        theme.tooltipTextColor = ChartColor.hex("#E5E9F0");
        return theme;
    }

    public ChartColor getBackground() { return background; }
    public ChartTheme setBackground(ChartColor background) { this.background = background; return this; }

    public ChartColor getPlotBackground() { return plotBackground; }
    public ChartTheme setPlotBackground(ChartColor plotBackground) { this.plotBackground = plotBackground; return this; }

    public ChartColor getGridColor() { return gridColor; }
    public ChartTheme setGridColor(ChartColor gridColor) { this.gridColor = gridColor; return this; }

    public ChartColor getAxisTextColor() { return axisTextColor; }
    public ChartTheme setAxisTextColor(ChartColor axisTextColor) { this.axisTextColor = axisTextColor; return this; }

    public ChartColor getTitleColor() { return titleColor; }
    public ChartTheme setTitleColor(ChartColor titleColor) { this.titleColor = titleColor; return this; }

    public ChartColor getSubtitleColor() { return subtitleColor; }
    public ChartTheme setSubtitleColor(ChartColor subtitleColor) { this.subtitleColor = subtitleColor; return this; }

    public ChartColor getLegendTextColor() { return legendTextColor; }
    public ChartTheme setLegendTextColor(ChartColor legendTextColor) { this.legendTextColor = legendTextColor; return this; }

    public ChartColor getCrosshairColor() { return crosshairColor; }
    public ChartTheme setCrosshairColor(ChartColor crosshairColor) { this.crosshairColor = crosshairColor; return this; }

    public ChartColor getTooltipBackground() { return tooltipBackground; }
    public ChartTheme setTooltipBackground(ChartColor tooltipBackground) { this.tooltipBackground = tooltipBackground; return this; }

    public ChartColor getTooltipBorderColor() { return tooltipBorderColor; }
    public ChartTheme setTooltipBorderColor(ChartColor tooltipBorderColor) { this.tooltipBorderColor = tooltipBorderColor; return this; }

    public ChartColor getTooltipTextColor() { return tooltipTextColor; }
    public ChartTheme setTooltipTextColor(ChartColor tooltipTextColor) { this.tooltipTextColor = tooltipTextColor; return this; }

    public Font getTitleFont() { return titleFont; }
    public ChartTheme setTitleFont(Font titleFont) { this.titleFont = titleFont; return this; }

    public Font getSubtitleFont() { return subtitleFont; }
    public ChartTheme setSubtitleFont(Font subtitleFont) { this.subtitleFont = subtitleFont; return this; }

    public Font getLabelFont() { return labelFont; }
    public ChartTheme setLabelFont(Font labelFont) { this.labelFont = labelFont; return this; }

    public Font getLegendFont() { return legendFont; }
    public ChartTheme setLegendFont(Font legendFont) { this.legendFont = legendFont; return this; }

    public Font getTooltipFont() { return tooltipFont; }
    public ChartTheme setTooltipFont(Font tooltipFont) { this.tooltipFont = tooltipFont; return this; }
}
