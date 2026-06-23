package dtm.stools.component.panels.editor.code.api;

import java.util.Collections;
import java.util.List;

/**
 * An opaque command that the host application knows how to execute.
 * The editor does not interpret {@code id} or {@code arguments} — it just dispatches
 * them via the registered {@link dtm.stools.component.panels.editor.code.api.CommandHandler}.
 */
public record Command(String id, String title, List<Object> arguments) {
    public Command {
        if (arguments == null) arguments = Collections.emptyList();
        else arguments = List.copyOf(arguments);
    }

    public static Command of(String id, String title) {
        return new Command(id, title, Collections.emptyList());
    }
}
