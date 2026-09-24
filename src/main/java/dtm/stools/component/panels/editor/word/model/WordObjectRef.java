package dtm.stools.component.panels.editor.word.model;

public record WordObjectRef(int offset, WordObjectRun run) {
    public WordInlineObject object() { return run.object(); }
}
