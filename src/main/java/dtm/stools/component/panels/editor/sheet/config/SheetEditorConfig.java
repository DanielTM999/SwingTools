package dtm.stools.component.panels.editor.sheet.config;

import dtm.stools.component.panels.editor.sheet.api.CalcMode;
import dtm.stools.component.panels.editor.sheet.calc.IterationSettings;
import lombok.Builder;
import lombok.With;

import java.util.Locale;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record SheetEditorConfig(boolean readOnly, boolean ribbonVisible, boolean formulaBarVisible, boolean headersVisible, boolean gridlinesVisible,
                                boolean tabsVisible, boolean statusVisible, double zoom, Locale locale, int historyLimit, CalcMode calcMode,
                                IterationSettings iteration, boolean r1c1, boolean autoComplete, boolean moveAfterEnter, boolean enterMovesDown,
                                SheetLimits limits, long autoRecoverSeconds) {
    public static SheetEditorConfig defaults() {
        return new SheetEditorConfig(false, true, true, true, true, true, true, 1, Locale.forLanguageTag("pt-BR"), 200, CalcMode.AUTOMATIC,
                IterationSettings.DISABLED, false, true, true, true, SheetLimits.EXCEL, 600);
    }

    public SheetEditorConfig {
        locale = Objects.requireNonNullElse(locale, Locale.forLanguageTag("pt-BR"));
        calcMode = Objects.requireNonNullElse(calcMode, CalcMode.AUTOMATIC);
        iteration = Objects.requireNonNullElse(iteration, IterationSettings.DISABLED);
        limits = Objects.requireNonNullElse(limits, SheetLimits.EXCEL);
        if (!Double.isFinite(zoom) || zoom < 0.1 || zoom > 4) throw new IllegalArgumentException("Zoom must be between 10% and 400%");
        if (historyLimit < 0) throw new IllegalArgumentException("Negative history limit");
    }
}
