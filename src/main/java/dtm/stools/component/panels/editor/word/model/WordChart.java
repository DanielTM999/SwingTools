package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WordChart(String id, WordChartType chartType, String title, List<String> categories, List<WordChartSeries> series,
                        boolean legend, LegendPosition legendPosition, boolean dataLabels, String categoryAxisTitle, String valueAxisTitle,
                        float width, float height, String altText, WordPlacement placement) implements WordInlineObject {
    public static final String TYPE = "chart";
    public enum LegendPosition { RIGHT, BOTTOM, TOP, LEFT }
    public WordChart {
        WordInlineObject.requireId(id); Objects.requireNonNull(chartType); Objects.requireNonNull(legendPosition);
        WordInlineObject.checkSize(width,height);
        if (width < 36 || height < 36) throw new IllegalArgumentException("Chart is too small");
        title = title == null ? "" : title;
        categories = List.copyOf(categories); series = List.copyOf(series);
        if (categories.size() > 4000 || series.size() > 255) throw new IllegalArgumentException("Chart has too much data");
        List<WordChartSeries> padded = new ArrayList<>();
        for (WordChartSeries s : series) {
            List<Double> values = new ArrayList<>(s.values());
            while (values.size() < categories.size()) values.add(Double.NaN);
            while (values.size() > categories.size()) values.removeLast();
            padded.add(s.withValues(values));
        }
        series = List.copyOf(padded);
        categoryAxisTitle = categoryAxisTitle == null ? "" : categoryAxisTitle;
        valueAxisTitle = valueAxisTitle == null ? "" : valueAxisTitle;
        altText = altText == null ? "" : altText;
        placement = placement == null ? WordPlacement.INLINE : placement;
    }
    public static WordChart sample() {
        return new WordChart(WordIds.next(),WordChartType.COLUMN_CLUSTERED,"Vendas por trimestre",List.of("T1","T2","T3","T4"),
                List.of(new WordChartSeries("2025",List.of(42.0,55.0,61.0,70.0),0x4472C4),new WordChartSeries("2026",List.of(48.0,62.0,66.0,81.0),0xED7D31)),
                true,LegendPosition.BOTTOM,false,"","",360,216,"Gráfico",WordPlacement.INLINE);
    }
    @Override public String type() { return TYPE; }
    @Override public WordChart withId(String value) { return new WordChart(value,chartType,title,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    @Override public WordChart resize(float w, float h) { return new WordChart(id,chartType,title,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,w,h,altText,placement); }
    @Override public WordChart withPlacement(WordPlacement value) { return new WordChart(id,chartType,title,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,value); }
    @Override public WordChart withAltText(String value) { return new WordChart(id,chartType,title,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,value,placement); }
    public WordChart withChartType(WordChartType value) { return new WordChart(id,value,title,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    public WordChart withTitle(String value) { return new WordChart(id,chartType,value,categories,series,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    public WordChart withData(List<String> newCategories, List<WordChartSeries> newSeries) { return new WordChart(id,chartType,title,newCategories,newSeries,legend,legendPosition,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    public WordChart withLegend(boolean visible, LegendPosition position) { return new WordChart(id,chartType,title,categories,series,visible,position,dataLabels,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    public WordChart withDataLabels(boolean value) { return new WordChart(id,chartType,title,categories,series,legend,legendPosition,value,categoryAxisTitle,valueAxisTitle,width,height,altText,placement); }
    public WordChart withAxisTitles(String category, String value) { return new WordChart(id,chartType,title,categories,series,legend,legendPosition,dataLabels,category,value,width,height,altText,placement); }
    public WordChart withSeries(int index, WordChartSeries value) { List<WordChartSeries> next = new ArrayList<>(series); next.set(index,value); return withData(categories,next); }
    @Override public String plainText() { return title.isBlank() ? "[Gráfico]" : "[Gráfico: " + title + "]"; }
}
