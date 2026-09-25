package dtm.stools.component.grids;

import lombok.With;

import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

/** Optional cell overrides. Null properties inherit the less specific style. */
@With
public record GridCellStyle(Color background, Color foreground, Font font, Integer alignment, Border border) {
    public static GridCellStyle empty() { return new GridCellStyle(null, null, null, null, null); }

    public GridCellStyle overlay(GridCellStyle newer) {
        if (newer == null) return this;
        return new GridCellStyle(newer.background != null ? newer.background : background,
                newer.foreground != null ? newer.foreground : foreground,
                newer.font != null ? newer.font : font,
                newer.alignment != null ? newer.alignment : alignment,
                newer.border != null ? newer.border : border);
    }
}
