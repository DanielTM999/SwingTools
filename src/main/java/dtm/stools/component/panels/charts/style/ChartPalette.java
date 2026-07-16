package dtm.stools.component.panels.charts.style;

import java.util.List;

public final class ChartPalette {

    private final List<ChartColor> colors;

    private ChartPalette(List<ChartColor> colors) {
        if (colors == null || colors.isEmpty()) {
            throw new IllegalArgumentException("palette must have at least one color");
        }
        this.colors = List.copyOf(colors);
    }

    public static ChartPalette of(ChartColor... colors) {
        return new ChartPalette(List.of(colors));
    }

    public static ChartPalette of(List<ChartColor> colors) {
        return new ChartPalette(colors);
    }

    public static ChartPalette modern() {
        return of(
                ChartColor.hex("#6366F1"),
                ChartColor.hex("#14B8A6"),
                ChartColor.hex("#F59E0B"),
                ChartColor.hex("#F43F5E"),
                ChartColor.hex("#0EA5E9"),
                ChartColor.hex("#8B5CF6"),
                ChartColor.hex("#84CC16"),
                ChartColor.hex("#FB923C"),
                ChartColor.hex("#EC4899"),
                ChartColor.hex("#22D3EE")
        );
    }

    public static ChartPalette cool() {
        return of(
                ChartColor.hex("#38BDF8"),
                ChartColor.hex("#818CF8"),
                ChartColor.hex("#34D399"),
                ChartColor.hex("#2DD4BF"),
                ChartColor.hex("#60A5FA"),
                ChartColor.hex("#A78BFA")
        );
    }

    public static ChartPalette warm() {
        return of(
                ChartColor.hex("#F97316"),
                ChartColor.hex("#EF4444"),
                ChartColor.hex("#F59E0B"),
                ChartColor.hex("#FB7185"),
                ChartColor.hex("#FBBF24"),
                ChartColor.hex("#E879F9")
        );
    }

    public ChartColor colorAt(int index) {
        int i = index % colors.size();
        if (i < 0) i += colors.size();
        return colors.get(i);
    }

    public int size() {
        return colors.size();
    }

    public List<ChartColor> getColors() {
        return colors;
    }
}
