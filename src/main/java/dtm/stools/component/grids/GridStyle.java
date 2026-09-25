package dtm.stools.component.grids;

import lombok.With;

import java.awt.Color;
import java.awt.Font;

/** Appearance of a GridView. Null colors and font follow the current look and feel. */
@With
public record GridStyle(Color background, Color foreground, Color headerBackground, Color headerForeground,
                        Color stripeBackground, Color selectionBackground, Color selectionForeground,
                        Color gridColor, Font font, int rowHeight, boolean striped, boolean showGrid) {
    public GridStyle {
        if (rowHeight < 0) throw new IllegalArgumentException("rowHeight must be non-negative");
    }

    public static GridStyle standard() { return new GridStyle(null, null, null, null, null, null, null, null, null, 0, true, false); }
    public static GridStyle compact() { return standard().withRowHeight(22); }
    public static GridStyle plain() { return standard().withStriped(false).withShowGrid(true); }
}
