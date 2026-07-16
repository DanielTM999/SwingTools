package dtm.stools.component.panels.editor.code.api;

@FunctionalInterface
public interface CommandHandler {
    void execute(Command command);
}
