package dtm.stools.component.panels.editor.word.config;

import dtm.stools.component.panels.editor.word.api.WordViewMode;
import java.util.Locale;
import java.util.Objects;

public record WordEditorConfig(boolean readOnly,boolean ribbonVisible,boolean navigationVisible,
                               boolean statusVisible,double zoom,WordViewMode viewMode,Locale locale,int historyLimit) {
    public static WordEditorConfig defaults() { return new WordEditorConfig(false,true,false,true,1,WordViewMode.PRINT_LAYOUT,Locale.forLanguageTag("pt-BR"),200); }
    public WordEditorConfig {
        Objects.requireNonNull(viewMode); Objects.requireNonNull(locale);
        if(!Double.isFinite(zoom) || zoom<0.25 || zoom>4 || historyLimit<0) throw new IllegalArgumentException("Invalid editor configuration");
    }
}
