package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.WordDialogRequest;
import java.awt.Component;

/** Dedicated, resizable transactional host for the object-specific property editors. */
public final class WordPropertiesActivity<T> extends WordDialogActivity<T> {
    public WordPropertiesActivity(Component owner, WordPropertiesPanel<T> panel, boolean readOnly) {
        this(owner, panel, readOnly, value -> {});
    }
    public WordPropertiesActivity(Component owner, WordPropertiesPanel<T> panel, boolean readOnly, java.util.function.Consumer<T> apply) {
        super(new WordDialogRequest<>(owner, "word.properties.dialog", panel.title() + (readOnly ? " (somente leitura)" : ""),
                "", panel, panel::result, apply, "Aplicar", readOnly, false));
    }
}
