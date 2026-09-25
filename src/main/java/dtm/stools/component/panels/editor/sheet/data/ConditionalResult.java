package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;

public record ConditionalResult(DifferentialStyle style, Integer scaleColor, Double barFraction, Integer barColor, boolean barGradient, IconSetType iconSet, int iconIndex, boolean hideValue) {
    public static final ConditionalResult NONE = new ConditionalResult(null, null, null, null, false, null, -1, false);

    public boolean isEmpty() { return style == null && scaleColor == null && barFraction == null && iconSet == null; }
}
