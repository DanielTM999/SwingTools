package dtm.stools.component.panels.editor.sheet.command;

@FunctionalInterface
public interface SheetCommand {
    void apply(SheetTransaction transaction);

    default String label() { return "Editar"; }
}
