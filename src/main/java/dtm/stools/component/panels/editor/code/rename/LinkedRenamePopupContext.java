package dtm.stools.component.panels.editor.code.rename;

import javax.swing.JComponent;
import java.util.Map;
import java.util.function.BiConsumer;

public record LinkedRenamePopupContext(
        JComponent editor,
        RenameSession session,
        Map<String, Boolean> optionValues,
        BiConsumer<String, Boolean> optionSetter,
        Runnable refocusEditor,
        String hint
) {

    public LinkedRenamePopupContext {
        optionValues = optionValues == null ? Map.of() : Map.copyOf(optionValues);
    }

    public boolean optionValue(String id, boolean defaultValue) {
        Boolean value = optionValues.get(id);
        return value == null ? defaultValue : value;
    }

    public void setOption(String id, boolean value) {
        if (optionSetter != null) optionSetter.accept(id, value);
    }

    public void refocus() {
        if (refocusEditor != null) refocusEditor.run();
    }
}
