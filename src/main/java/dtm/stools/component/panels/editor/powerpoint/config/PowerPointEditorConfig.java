package dtm.stools.component.panels.editor.powerpoint.config;

import java.util.Locale;
import java.util.Objects;

public record PowerPointEditorConfig(boolean readOnly, boolean ribbonVisible, boolean thumbnailsVisible,
                                     boolean propertiesVisible, boolean statusVisible, double zoom,
                                     Locale locale, int historyLimit) {
    public PowerPointEditorConfig {
        Objects.requireNonNull(locale);
        if(!Double.isFinite(zoom)||zoom<0.1||zoom>4||historyLimit<0)throw new IllegalArgumentException("Invalid editor configuration");
    }
    public static PowerPointEditorConfig defaults(){return new PowerPointEditorConfig(false,true,true,true,true,1,Locale.forLanguageTag("pt-BR"),200);}
}
