package dtm.stools.component.panels.editor.word.command;

public interface EditorCommand<D> {
    String label();
    D apply(D document);
}
