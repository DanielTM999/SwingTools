package dtm.stools.component.panels.editor.code.api;

/**
 * Callback invoked by the editor when a {@link Command} carried by a
 * {@link CodeAction} or other feature needs to run.
 * The host application decides how to execute it.
 */
@FunctionalInterface
public interface CommandHandler {
    void execute(Command command);
}
