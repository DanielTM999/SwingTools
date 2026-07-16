package dtm.stools.component.panels.editor.code.api;

import java.util.Collections;
import java.util.List;

public record Command(String id, String title, List<Object> arguments) {
    public Command {
        if (arguments == null) arguments = Collections.emptyList();
        else arguments = List.copyOf(arguments);
    }

    public static Command of(String id, String title) {
        return new Command(id, title, Collections.emptyList());
    }
}
